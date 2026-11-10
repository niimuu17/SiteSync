package com.example.sitesync.hardware;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.sitesync.BuildConfig;
import com.example.sitesync.data.local.entities.RequiredMaterialItemEntity;
import com.example.sitesync.data.local.entities.SiteChatMessageEntity;
import com.example.sitesync.data.network.MaterialImageResolver;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Multirouted LLM Chat Intelligence Client (Gemini 3.5 / Groq LLaMA 3.3 / Local NLP).
 * Analyzes construction chat transcripts from the previous generation onwards to extract:
 * 1. Required Materials requested by site crew (with Brave-resolved product images).
 * 2. Critical Site Briefings & Alerts the project manager must know.
 */
public class ChatIntelligenceClient {

    private static final String TAG = "ChatIntelligence";
    private final Context context;
    private final OkHttpClient httpClient;
    private final ExecutorService executor;
    private final Handler mainHandler;

    public static class ImportantBriefingNotice implements Serializable {
        public String title;
        public String details;
        public String category; // SAFETY_HAZARD, EQUIPMENT_FAILURE, INSPECTION_AUDIT, WEATHER_DELAY, etc.
        public String severity; // CRITICAL, WARNING, INFO
        public String reportedBy;
        public String suggestedAction;

        public ImportantBriefingNotice(String title, String details, String category,
                                       String severity, String reportedBy, String suggestedAction) {
            this.title = title;
            this.details = details;
            this.category = category;
            this.severity = severity != null ? severity : "INFO";
            this.reportedBy = reportedBy != null ? reportedBy : "Site Crew";
            this.suggestedAction = suggestedAction;
        }
    }

    public static class ChatIntelligenceResult implements Serializable {
        public List<RequiredMaterialItemEntity> requiredMaterials = new ArrayList<>();
        public List<ImportantBriefingNotice> importantBriefings = new ArrayList<>();
        public String modelUsed;
        public int messagesAnalyzed;
    }

    public interface OnIntelligenceCompletedListener {
        void onSuccess(ChatIntelligenceResult result);
        void onError(String errorMessage);
    }

    public ChatIntelligenceClient(Context context) {
        this.context = context != null ? context.getApplicationContext() : null;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(6, TimeUnit.SECONDS)
                .readTimeout(8, TimeUnit.SECONDS)
                .build();
        this.executor = Executors.newFixedThreadPool(4);
        this.mainHandler = Looper.getMainLooper() != null ? new Handler(Looper.getMainLooper()) : null;
    }

    public void analyzeChatMessages(String projectCode, List<SiteChatMessageEntity> messages,
                                   OnIntelligenceCompletedListener listener) {
        executor.execute(() -> {
            try {
                if (messages == null || messages.isEmpty()) {
                    ChatIntelligenceResult emptyResult = new ChatIntelligenceResult();
                    emptyResult.modelUsed = "None (No new messages)";
                    deliverSuccess(listener, emptyResult);
                    return;
                }

                String transcript = buildChatTranscript(messages);
                AtomicBoolean isDelivered = new AtomicBoolean(false);
                CountDownLatch latch = new CountDownLatch(2);

                String groqKey = BuildConfig.GROQ_API_KEY;
                String geminiKey = BuildConfig.GEMINI_API_KEY;

                // Route A: Ultra-Fast Groq LLM (Qwen 3.8 27B / GPT-OSS 120B in ~1.5s)
                if (groqKey != null && !groqKey.trim().isEmpty() && !groqKey.contains("YOUR_")) {
                    executor.execute(() -> {
                        try {
                            ChatIntelligenceResult groqRes = queryGroqIntelligence(projectCode, transcript, groqKey.trim(), messages.size());
                            if (groqRes != null && (!groqRes.requiredMaterials.isEmpty() || !groqRes.importantBriefings.isEmpty())) {
                                if (isDelivered.compareAndSet(false, true)) {
                                    resolveProductImages(groqRes.requiredMaterials);
                                    deliverSuccess(listener, groqRes);
                                }
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "Groq chat intelligence failed: " + e.getMessage());
                        } finally {
                            latch.countDown();
                        }
                    });
                } else {
                    latch.countDown();
                }

                // Route B: Google Gemini 3.5 Flash Lite Concurrent Runner
                if (geminiKey != null && !geminiKey.trim().isEmpty() && !geminiKey.contains("YOUR_")) {
                    executor.execute(() -> {
                        try {
                            ChatIntelligenceResult geminiRes = queryGeminiIntelligence(projectCode, transcript, geminiKey.trim(), messages.size());
                            if (geminiRes != null && (!geminiRes.requiredMaterials.isEmpty() || !geminiRes.importantBriefings.isEmpty())) {
                                if (isDelivered.compareAndSet(false, true)) {
                                    resolveProductImages(geminiRes.requiredMaterials);
                                    deliverSuccess(listener, geminiRes);
                                }
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "Gemini chat intelligence failed: " + e.getMessage());
                        } finally {
                            latch.countDown();
                        }
                    });
                } else {
                    latch.countDown();
                }

                // Fallback guard: delivers local rule-based extraction if cloud queries time out or fail
                executor.execute(() -> {
                    try {
                        latch.await(8, TimeUnit.SECONDS);
                    } catch (InterruptedException ignored) {}

                    if (isDelivered.compareAndSet(false, true)) {
                        Log.i(TAG, "Cloud chat intelligence timed out; using deterministic local extraction");
                        ChatIntelligenceResult fallback = computeLocalExtraction(projectCode, messages);
                        resolveProductImages(fallback.requiredMaterials);
                        deliverSuccess(listener, fallback);
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Chat analysis error: " + e.getMessage(), e);
                ChatIntelligenceResult fallback = computeLocalExtraction(projectCode, messages);
                resolveProductImages(fallback.requiredMaterials);
                deliverSuccess(listener, fallback);
            }
        });
    }

    private String buildChatTranscript(List<SiteChatMessageEntity> messages) {
        StringBuilder sb = new StringBuilder();
        for (SiteChatMessageEntity msg : messages) {
            String role = msg.getSenderRole() != null ? msg.getSenderRole() : "CREW";
            String name = msg.getSenderName() != null ? msg.getSenderName() : "Worker";
            String urgentTag = msg.isUrgent() ? " [URGENT]" : "";
            sb.append("[").append(role).append("] ").append(name).append(urgentTag).append(": ")
              .append(msg.getMessageText()).append("\n");
        }
        return sb.toString();
    }

    private ChatIntelligenceResult queryGeminiIntelligence(String projectCode, String transcript, String apiKey, int count) {
        String[] candidateModels = new String[]{"gemini-3.5-flash-lite", "gemini-3.8-flash"};
        String prompt = buildSystemPrompt(transcript);

        for (String model : candidateModels) {
            try {
                String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;

                JSONObject root = new JSONObject();
                JSONObject contents = new JSONObject();
                JSONObject parts = new JSONObject();
                parts.put("text", prompt);
                contents.put("parts", new JSONArray().put(parts));
                root.put("contents", new JSONArray().put(contents));

                JSONObject genConfig = new JSONObject();
                genConfig.put("temperature", 0.1);
                genConfig.put("response_mime_type", "application/json");
                root.put("generationConfig", genConfig);

                RequestBody body = RequestBody.create(root.toString(), MediaType.parse("application/json"));
                Request request = new Request.Builder().url(url).post(body).build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String jsonResp = response.body().string();
                        JSONObject obj = new JSONObject(jsonResp);
                        String candidateText = obj.getJSONArray("candidates")
                                .getJSONObject(0)
                                .getJSONObject("content")
                                .getJSONArray("parts")
                                .getJSONObject(0)
                                .getString("text");

                        ChatIntelligenceResult result = parseJsonResponse(projectCode, candidateText, count);
                        result.modelUsed = "Google " + model;
                        return result;
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Gemini model " + model + " failed: " + e.getMessage());
            }
        }
        return null;
    }

    private ChatIntelligenceResult queryGroqIntelligence(String projectCode, String transcript, String apiKey, int count) {
        String[] candidateModels = new String[]{"qwen/qwen3.8-27b", "openai/gpt-oss-120b"};

        for (String model : candidateModels) {
            try {
                String url = "https://api.groq.com/openai/v1/chat/completions";
                String prompt = buildSystemPrompt(transcript);

                JSONObject payload = new JSONObject();
                payload.put("model", model);
                payload.put("temperature", 0.1);

                JSONObject responseFormat = new JSONObject();
                responseFormat.put("type", "json_object");
                payload.put("response_format", responseFormat);

                JSONArray messages = new JSONArray();
                JSONObject sysMsg = new JSONObject();
                sysMsg.put("role", "system");
                sysMsg.put("content", "You are an elite Civil Construction Site Intelligence AI in Bangladesh. Extract material needs and critical site updates. Return valid JSON only.");
                messages.put(sysMsg);

                JSONObject userMsg = new JSONObject();
                userMsg.put("role", "user");
                userMsg.put("content", prompt);
                messages.put(userMsg);

                payload.put("messages", messages);

                RequestBody body = RequestBody.create(payload.toString(), MediaType.parse("application/json"));
                Request request = new Request.Builder()
                        .url(url)
                        .header("Authorization", "Bearer " + apiKey)
                        .post(body)
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        JSONObject json = new JSONObject(response.body().string());
                        String content = json.getJSONArray("choices")
                                .getJSONObject(0)
                                .getJSONObject("message")
                                .getString("content");

                        ChatIntelligenceResult result = parseJsonResponse(projectCode, content, count);
                        result.modelUsed = "Groq " + model;
                        return result;
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Groq intelligence error on " + model + ": " + e.getMessage());
            }
        }
        return null;
    }

    private String buildSystemPrompt(String transcript) {
        return "You are an elite Civil Construction Quantity Surveyor and Safety Supervisor in Bangladesh.\n" +
                "Carefully read the following chat conversation between jobsite workers and supervisors:\n\n" +
                "--- CHAT TRANSCRIPT ---\n" +
                transcript + "\n" +
                "--- END TRANSCRIPT ---\n\n" +
                "Analyze and extract TWO categories into strict JSON:\n" +
                "1. 'requiredMaterials': Array of items requested or needed (e.g. '10 bag cement lagbe', '300 kg rod lagbe', '500 cft sand needed').\n" +
                "   Fields:\n" +
                "   - 'itemName': Standard English material name (e.g. 'Shah Cement Special', 'BSRM 16mm Rebar', 'Sylhet Coarse Sand', 'First Class Auto Bricks', 'Concealed PVC Pipe')\n" +
                "   - 'category': One of ['CEMENT', 'STEEL', 'AGGREGATE_SAND', 'BRICK', 'ELECTRICAL', 'PLUMBING', 'TILES', 'GENERAL']\n" +
                "   - 'quantity': float/double number (e.g. 10.0, 300.0)\n" +
                "   - 'unit': standard unit ('BAG', 'KG', 'TON', 'CFT', 'PCS', 'FEET')\n" +
                "   - 'urgency': 'HIGH' | 'MEDIUM' | 'LOW'\n" +
                "   - 'requestedBy': Name of sender\n" +
                "   - 'rawSentence': Original sentence\n" +
                "2. 'importantBriefings': Array of critical site notices the Project Manager MUST know (safety hazards, equipment issues, inspections, weather delays, structural concerns). Exclude trivial everyday chat.\n" +
                "   Fields:\n" +
                "   - 'title': Brief header\n" +
                "   - 'details': Detailed summary\n" +
                "   - 'category': One of ['SAFETY_HAZARD', 'EQUIPMENT_FAILURE', 'INSPECTION_AUDIT', 'WEATHER_DELAY', 'PROGRESS_BLOCKER', 'GENERAL_CRITICAL']\n" +
                "   - 'severity': 'CRITICAL' | 'WARNING' | 'INFO'\n" +
                "   - 'reportedBy': Name of reporter\n" +
                "   - 'suggestedAction': Concrete action for manager\n\n" +
                "Output JSON strictly with structure: { \"requiredMaterials\": [...], \"importantBriefings\": [...] }";
    }

    public static ChatIntelligenceResult parseJsonResponse(String projectCode, String jsonStr, int count) {
        ChatIntelligenceResult result = new ChatIntelligenceResult();
        result.messagesAnalyzed = count;
        try {
            JSONObject root = new JSONObject(jsonStr);

            if (root.has("requiredMaterials")) {
                JSONArray mats = root.getJSONArray("requiredMaterials");
                for (int i = 0; i < mats.length(); i++) {
                    JSONObject m = mats.getJSONObject(i);
                    RequiredMaterialItemEntity item = new RequiredMaterialItemEntity(
                            UUID.randomUUID().toString(),
                            projectCode,
                            m.optString("itemName", "Construction Material"),
                            m.optString("category", "GENERAL"),
                            m.optDouble("quantity", 1.0),
                            m.optString("unit", "UNIT"),
                            m.optString("urgency", "MEDIUM"),
                            m.optString("requestedBy", "Site Worker"),
                            m.optString("rawSentence", ""),
                            ""
                    );
                    result.requiredMaterials.add(item);
                }
            }

            if (root.has("importantBriefings")) {
                JSONArray briefs = root.getJSONArray("importantBriefings");
                for (int i = 0; i < briefs.length(); i++) {
                    JSONObject b = briefs.getJSONObject(i);
                    ImportantBriefingNotice notice = new ImportantBriefingNotice(
                            b.optString("title", "Site Notice"),
                            b.optString("details", ""),
                            b.optString("category", "GENERAL_CRITICAL"),
                            b.optString("severity", "WARNING"),
                            b.optString("reportedBy", "Site Supervisor"),
                            b.optString("suggestedAction", "Review with site engineer")
                    );
                    result.importantBriefings.add(notice);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "JSON parsing error: " + e.getMessage(), e);
        }
        return result;
    }

    /**
     * Resilient offline rule-based parser tuned for Bangladesh construction vernacular.
     */
    public static ChatIntelligenceResult computeLocalExtraction(String projectCode, List<SiteChatMessageEntity> messages) {
        ChatIntelligenceResult result = new ChatIntelligenceResult();
        result.modelUsed = "SiteSync Local Construction NLP";
        result.messagesAnalyzed = messages.size();

        Pattern qtyMatPattern = Pattern.compile("([0-9]+(?:\\.[0-9]+)?)\\s*(bag|bags|kg|ton|cft|pcs|bosta|বস্তা|ব্যাগ|কেজি|টন)?\\s*(cement|rod|rebar|sand|stone|brick|tiles|pipe|সিমেন্ট|রড|বালু|পাথর|ইট|টাইলস)", Pattern.CASE_INSENSITIVE);

        for (SiteChatMessageEntity msg : messages) {
            String text = msg.getMessageText();
            if (text == null) continue;
            String lower = text.toLowerCase(Locale.US);

            // Material extraction
            Matcher matcher = qtyMatPattern.matcher(text);
            if (matcher.find()) {
                double qty = Double.parseDouble(matcher.group(1));
                String unit = matcher.group(2) != null ? matcher.group(2).toUpperCase() : "";
                String mat = matcher.group(3).toLowerCase();

                String standardName = "Construction Supplies";
                String category = "GENERAL";
                String standardUnit = "UNIT";

                if (mat.contains("cement") || mat.contains("সিমেন্ট")) {
                    standardName = "Shah Cement Special (Composite)";
                    category = "CEMENT";
                    standardUnit = "BAG";
                } else if (mat.contains("rod") || mat.contains("rebar") || mat.contains("রড")) {
                    standardName = "BSRM 500W Deformed Rebar";
                    category = "STEEL";
                    standardUnit = unit.contains("KG") ? "KG" : "TON";
                } else if (mat.contains("sand") || mat.contains("বালু")) {
                    standardName = "Sylhet Coarse Sand (2.5 FM)";
                    category = "AGGREGATE_SAND";
                    standardUnit = "CFT";
                } else if (mat.contains("brick") || mat.contains("ইট")) {
                    standardName = "First Class Gas Burned Bricks";
                    category = "BRICK";
                    standardUnit = "PCS";
                } else if (mat.contains("tile") || mat.contains("টাইলস")) {
                    standardName = "Porcelain Floor Tiles (600x600mm)";
                    category = "TILES";
                    standardUnit = "BOX";
                }

                result.requiredMaterials.add(new RequiredMaterialItemEntity(
                        UUID.randomUUID().toString(),
                        projectCode,
                        standardName,
                        category,
                        qty,
                        standardUnit,
                        msg.isUrgent() ? "HIGH" : "MEDIUM",
                        msg.getSenderName(),
                        text,
                        ""
                ));
            } else if (lower.contains("cement") || lower.contains("rod") || lower.contains("sand") || lower.contains("dorkar") || lower.contains("lagbe")) {
                // Heuristic backup for needed materials
                if (lower.contains("cement")) {
                    result.requiredMaterials.add(new RequiredMaterialItemEntity(
                            UUID.randomUUID().toString(), projectCode, "Shah Cement Special", "CEMENT",
                            50.0, "BAG", "HIGH", msg.getSenderName(), text, ""
                    ));
                } else if (lower.contains("rod") || lower.contains("rebar")) {
                    result.requiredMaterials.add(new RequiredMaterialItemEntity(
                            UUID.randomUUID().toString(), projectCode, "BSRM 16mm Rebar", "STEEL",
                            2.0, "TON", "HIGH", msg.getSenderName(), text, ""
                    ));
                }
            }

            // Critical site notice extraction
            if (msg.isUrgent() || lower.contains("agun") || lower.contains("fire") || lower.contains("hazard") ||
                lower.contains("leak") || lower.contains("crack") || lower.contains("breakdown") ||
                lower.contains("accident") || lower.contains("stop") || lower.contains("inspection")) {

                String severity = "WARNING";
                String cat = "SAFETY_HAZARD";
                String action = "Investigate on-site and confirm mitigation.";

                if (lower.contains("agun") || lower.contains("fire") || lower.contains("accident") || lower.contains("hospital")) {
                    severity = "CRITICAL";
                    cat = "SAFETY_HAZARD";
                    action = "Immediate site evacuation / emergency safety team response!";
                } else if (lower.contains("breakdown") || lower.contains("crane") || lower.contains("generator")) {
                    cat = "EQUIPMENT_FAILURE";
                    action = "Call mechanics / equipment supplier for immediate maintenance.";
                } else if (lower.contains("inspection") || lower.contains("rajuk") || lower.contains("audit")) {
                    cat = "INSPECTION_AUDIT";
                    action = "Ensure structural drawings and safety logs are ready for inspection.";
                }

                result.importantBriefings.add(new ImportantBriefingNotice(
                        "Site Notice from " + msg.getSenderName(),
                        text,
                        cat,
                        severity,
                        msg.getSenderName(),
                        action
                ));
            }
        }

        return result;
    }

    private void resolveProductImages(List<RequiredMaterialItemEntity> items) {
        if (items == null) return;
        for (RequiredMaterialItemEntity item : items) {
            String resolved = MaterialImageResolver.resolveImageForMaterial(item.getItemName(), item.getCategory());
            item.setImageUrl(resolved);
        }
    }

    private void deliverSuccess(OnIntelligenceCompletedListener listener, ChatIntelligenceResult result) {
        if (listener != null) {
            if (mainHandler != null) {
                mainHandler.post(() -> listener.onSuccess(result));
            } else {
                listener.onSuccess(result);
            }
        }
    }

    private void deliverError(OnIntelligenceCompletedListener listener, String err) {
        if (listener != null) {
            if (mainHandler != null) {
                mainHandler.post(() -> listener.onError(err));
            } else {
                listener.onError(err);
            }
        }
    }
}

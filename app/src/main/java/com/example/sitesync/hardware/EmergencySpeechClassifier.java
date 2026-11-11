package com.example.sitesync.hardware;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.sitesync.BuildConfig;

import org.json.JSONObject;

import java.io.Serializable;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Intelligent Emergency Classifier for Spoken Utterances.
 * Analyzes speech (Bengali or English, e.g. "tin tolay agun lagse") via Gemini / Groq / Local Heuristics
 * to distinguish genuine construction site emergencies (fire, fall, collapse, electric shock)
 * from casual everyday chatter.
 */
public class EmergencySpeechClassifier {

    private static final String TAG = "EmergencyClassifier";
    private final Context context;
    private final OkHttpClient httpClient;
    private final ExecutorService executor;
    private final Handler mainHandler;

    public static class EmergencyClassification implements Serializable {
        public boolean isEmergency;
        public boolean isMaterialRequest;
        public String requestedItemName;
        public String emergencyType; // "FIRE", "COLLAPSE", "FALL", "ELECTRICAL", "MEDICAL", "MATERIAL_REQUEST", "NONE"
        public String severity;      // "CRITICAL", "HIGH", "NORMAL", "NONE"
        public String alertHeadline;
        public String summary;
        public String rawSpokenText;
        public String modelUsed;

        public EmergencyClassification(boolean isEmergency, String emergencyType, String severity,
                                       String alertHeadline, String summary, String rawSpokenText, String modelUsed) {
            this.isEmergency = isEmergency;
            this.emergencyType = emergencyType;
            this.severity = severity;
            this.alertHeadline = alertHeadline;
            this.summary = summary;
            this.rawSpokenText = rawSpokenText;
            this.modelUsed = modelUsed;
            this.isMaterialRequest = false;
        }

        public static EmergencyClassification notAnEmergency(String rawText) {
            return new EmergencyClassification(false, "NONE", "NONE",
                    "Casual Chatter", "No site security alert detected.", rawText, "Local Classifier");
        }
    }

    public interface OnClassificationListener {
        void onResult(EmergencyClassification result);
    }

    public EmergencySpeechClassifier(Context context) {
        this.context = context != null ? context.getApplicationContext() : null;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(4, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build();
        this.executor = Executors.newFixedThreadPool(4);
        this.mainHandler = Looper.getMainLooper() != null ? new Handler(Looper.getMainLooper()) : null;
    }

    public void classifySpeech(String spokenText, OnClassificationListener listener) {
        executor.execute(() -> {
            if (spokenText == null || spokenText.trim().isEmpty()) {
                deliverResult(listener, EmergencyClassification.notAnEmergency(""));
                return;
            }

            // Fast-path 0: Immediate life-safety keyword triggers (<1ms)
            EmergencyClassification localCheck = evaluateLocalKeywords(spokenText);
            if (localCheck.isEmergency) {
                deliverResult(listener, localCheck);
                return;
            }

            AtomicBoolean isDelivered = new AtomicBoolean(false);
            CountDownLatch latch = new CountDownLatch(2);

            String groqKey = BuildConfig.GROQ_API_KEY;
            String geminiKey = BuildConfig.GEMINI_API_KEY;

            // Route A: Ultra-Fast Groq Classifier (~300ms)
            if (groqKey != null && !groqKey.trim().isEmpty() && !groqKey.contains("YOUR_")) {
                executor.execute(() -> {
                    try {
                        EmergencyClassification groqRes = queryGroqClassifier(spokenText, groqKey.trim());
                        if (groqRes != null && isDelivered.compareAndSet(false, true)) {
                            deliverResult(listener, groqRes);
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Groq emergency classification error: " + e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                });
            } else {
                latch.countDown();
            }

            // Route B: Google Gemini Classifier Concurrent Runner
            if (geminiKey != null && !geminiKey.trim().isEmpty() && !geminiKey.contains("YOUR_")) {
                executor.execute(() -> {
                    try {
                        EmergencyClassification geminiRes = queryGeminiClassifier(spokenText, geminiKey.trim());
                        if (geminiRes != null && isDelivered.compareAndSet(false, true)) {
                            deliverResult(listener, geminiRes);
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Gemini emergency classification error: " + e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                });
            } else {
                latch.countDown();
            }

            // Fallback guard: delivers localCheck if cloud queries time out or fail
            executor.execute(() -> {
                try {
                    latch.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {}

                if (isDelivered.compareAndSet(false, true)) {
                    deliverResult(listener, localCheck);
                }
            });
        });
    }

    public static EmergencyClassification evaluateLocalKeywords(String text) {
        String lower = text.toLowerCase(Locale.US);

        Pattern firePattern = Pattern.compile("(agun|fire|smoke|dhowa|shikha|blast|explosion|gas cylinder)", Pattern.CASE_INSENSITIVE);
        Pattern fallPattern = Pattern.compile("(pore|fall|fallen|scaffolding|slip|chhad theke)", Pattern.CASE_INSENSITIVE);
        Pattern collapsePattern = Pattern.compile("(dhosh|collapse|bhenge|crack|shuttering)", Pattern.CASE_INSENSITIVE);
        Pattern electricPattern = Pattern.compile("(current|karen|shock|electric|short circuit|wire cut)", Pattern.CASE_INSENSITIVE);
        Pattern medicalPattern = Pattern.compile("(rokto|blood|unconscious|accident|mora|hospital|ambulens)", Pattern.CASE_INSENSITIVE);

        if (firePattern.matcher(lower).find() || text.contains("আগুন") || text.contains("ধোঁয়া") || text.contains("বিস্ফোরণ")) {
            return new EmergencyClassification(true, "FIRE", "CRITICAL",
                    "SITE FIRE EMERGENCY REPORTED",
                    "Spoken Report: \"" + text + "\" - Potential active fire or explosion hazard.",
                    text, "Instant Life-Safety Detector");
        }

        if (collapsePattern.matcher(lower).find() || text.contains("ধস") || text.contains("ভেঙে")) {
            return new EmergencyClassification(true, "COLLAPSE", "CRITICAL",
                    "STRUCTURAL COLLAPSE ALERT",
                    "Spoken Report: \"" + text + "\" - Potential formwork or structural beam collapse.",
                    text, "Instant Life-Safety Detector");
        }

        if (fallPattern.matcher(lower).find() || text.contains("পড়ে")) {
            return new EmergencyClassification(true, "FALL", "CRITICAL",
                    "WORKER FALL / SCAFFOLDING ACCIDENT",
                    "Spoken Report: \"" + text + "\" - Worker fall from elevation detected.",
                    text, "Instant Life-Safety Detector");
        }

        if (electricPattern.matcher(lower).find() || text.contains("কারেন্ট") || text.contains("শর্ট")) {
            return new EmergencyClassification(true, "ELECTRICAL", "CRITICAL",
                    "HIGH VOLTAGE ELECTRICAL HAZARD",
                    "Spoken Report: \"" + text + "\" - Electric shock or severe conduit short circuit.",
                    text, "Instant Life-Safety Detector");
        }

        if (medicalPattern.matcher(lower).find() || text.contains("রক্ত") || text.contains("অজ্ঞান")) {
            return new EmergencyClassification(true, "MEDICAL", "CRITICAL",
                    "CRITICAL MEDICAL EMERGENCY",
                    "Spoken Report: \"" + text + "\" - Immediate first aid / ambulance required.",
                    text, "Instant Life-Safety Detector");
        }

        // Material / Product Requisition Detection (e.g. "3 ta led bulb lagbe", "10 bag cement lagbe", "50 kg rod lagbe")
        Pattern reqPattern = Pattern.compile("(lagbe|dar kar|dorkar|need|bulb|cement|rod|sand|balu|brick|it|pipe|wire|cable|tiles|plaster|bring|kinte|anben)", Pattern.CASE_INSENSITIVE);
        if (reqPattern.matcher(lower).find() || text.contains("লাগবে") || text.contains("দরকার") || text.contains("আনেন") || text.contains("কিনে")) {
            EmergencyClassification matReq = new EmergencyClassification(false, "MATERIAL_REQUEST", "NORMAL",
                    "PRODUCT REQUISITION DISPATCHED",
                    "Material request recorded: \"" + text + "\" - Posted to site crew chat.",
                    text, "Site Material Dispatcher");
            matReq.isMaterialRequest = true;
            matReq.requestedItemName = text;
            return matReq;
        }

        return EmergencyClassification.notAnEmergency(text);
    }

    private EmergencyClassification queryGeminiClassifier(String spokenText, String apiKey) {
        String[] candidateModels = new String[]{"gemini-3.5-flash-lite", "gemini-3.8-flash"};
        String prompt = buildPrompt(spokenText);

        for (String model : candidateModels) {
            try {
                String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;

                JSONObject root = new JSONObject();
                JSONObject contents = new JSONObject();
                JSONObject parts = new JSONObject();
                parts.put("text", prompt);
                contents.put("parts", new org.json.JSONArray().put(parts));
                root.put("contents", new org.json.JSONArray().put(contents));

                JSONObject genConfig = new JSONObject();
                genConfig.put("temperature", 0.0);
                genConfig.put("response_mime_type", "application/json");
                root.put("generationConfig", genConfig);

                RequestBody body = RequestBody.create(root.toString(), MediaType.parse("application/json"));
                Request req = new Request.Builder().url(url).post(body).build();

                try (Response response = httpClient.newCall(req).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        JSONObject obj = new JSONObject(response.body().string());
                        String candidateText = obj.getJSONArray("candidates")
                                .getJSONObject(0)
                                .getJSONObject("content")
                                .getJSONArray("parts")
                                .getJSONObject(0)
                                .getString("text");

                        return parseLlmOutput(candidateText, spokenText, "Google " + model);
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Gemini emergency query note on " + model + ": " + e.getMessage());
            }
        }
        return null;
    }

    private EmergencyClassification queryGroqClassifier(String spokenText, String apiKey) {
        String[] candidateModels = new String[]{"qwen/qwen3.8-27b", "openai/gpt-oss-120b"};
        String prompt = buildPrompt(spokenText);

        for (String model : candidateModels) {
            try {
                String url = "https://api.groq.com/openai/v1/chat/completions";

                JSONObject payload = new JSONObject();
                payload.put("model", model);
                payload.put("temperature", 0.0);

                JSONObject responseFormat = new JSONObject();
                responseFormat.put("type", "json_object");
                payload.put("response_format", responseFormat);

                org.json.JSONArray messages = new org.json.JSONArray();
                JSONObject sysMsg = new JSONObject();
                sysMsg.put("role", "system");
                sysMsg.put("content", "You are an OSHA Construction Safety & Emergency AI Classifier. Return strict JSON only.");
                messages.put(sysMsg);

                JSONObject userMsg = new JSONObject();
                userMsg.put("role", "user");
                userMsg.put("content", prompt);
                messages.put(userMsg);

                payload.put("messages", messages);

                RequestBody body = RequestBody.create(payload.toString(), MediaType.parse("application/json"));
                Request req = new Request.Builder()
                        .url(url)
                        .header("Authorization", "Bearer " + apiKey)
                        .post(body)
                        .build();

                try (Response response = httpClient.newCall(req).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        JSONObject json = new JSONObject(response.body().string());
                        String content = json.getJSONArray("choices")
                                .getJSONObject(0)
                                .getJSONObject("message")
                                .getString("content");

                        return parseLlmOutput(content, spokenText, "Groq " + model);
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Groq emergency query note on " + model + ": " + e.getMessage());
            }
        }
        return null;
    }

    private String buildPrompt(String spokenText) {
        return "You are an elite OSHA Construction Safety AI on a jobsite in Bangladesh.\n" +
                "Evaluate whether this spoken phrase represents a genuine life-safety hazard or emergency:\n" +
                "\"" + spokenText + "\"\n\n" +
                "Return JSON strictly with format:\n" +
                "{\n" +
                "  \"isEmergency\": true | false,\n" +
                "  \"emergencyType\": \"FIRE\" | \"COLLAPSE\" | \"FALL\" | \"ELECTRICAL\" | \"MEDICAL\" | \"NONE\",\n" +
                "  \"severity\": \"CRITICAL\" | \"HIGH\" | \"NONE\",\n" +
                "  \"alertHeadline\": \"Short headline in English\",\n" +
                "  \"summary\": \"Brief explanation of the emergency or why it is casual chatter\"\n" +
                "}";
    }

    public static EmergencyClassification parseLlmOutput(String jsonStr, String spokenText, String modelName) {
        try {
            JSONObject obj = new JSONObject(jsonStr);
            boolean isEmergency = obj.optBoolean("isEmergency", false);
            String type = obj.optString("emergencyType", "NONE");
            String severity = obj.optString("severity", "NONE");
            String headline = obj.optString("alertHeadline", isEmergency ? "SITE EMERGENCY" : "Casual Chatter");
            String summary = obj.optString("summary", spokenText);

            return new EmergencyClassification(isEmergency, type, severity, headline, summary, spokenText, modelName);
        } catch (Exception e) {
            return evaluateLocalKeywords(spokenText);
        }
    }

    private void deliverResult(OnClassificationListener listener, EmergencyClassification result) {
        if (listener != null) {
            if (mainHandler != null) {
                mainHandler.post(() -> listener.onResult(result));
            } else {
                listener.onResult(result);
            }
        }
    }
}

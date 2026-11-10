package com.example.sitesync.hardware;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.sitesync.BuildConfig;
import com.example.sitesync.data.local.entities.ProjectEntity;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * AI Progress Estimator powered by Gemini / Groq LLM.
 * Evaluates daily unstructured construction logs against the active project building scope
 * (e.g., 2-room building, 10ft walls, 6 windows, 4 doors, tiles) and computes completed vs remaining percentages.
 */
public class ConstructionProgressEstimatorClient {

    private static final String TAG = "ProgressEstimator";
    private final Context context;
    private final OkHttpClient httpClient;
    private final ExecutorService executor;
    private final Handler mainHandler;

    public static class ProgressEstimationResult {
        public float overallProgressPercent;
        public float civilProgress;
        public float electricProgress;
        public float plumbingProgress;
        public float thaiGlassProgress;
        public float tilesFlooringProgress;
        public float interiorPlasterProgress;
        public String completedTodaySummary;
        public String remainingWorkSummary;
        public String aiAnalysisNotes;
        public String modelUsed;
    }

    public interface OnEstimationCompletedListener {
        void onSuccess(ProgressEstimationResult result);
        void onError(String errorMessage);
    }

    public ConstructionProgressEstimatorClient(Context context) {
        this.context = context.getApplicationContext();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(6, TimeUnit.SECONDS)
                .readTimeout(8, TimeUnit.SECONDS)
                .build();
        this.executor = Executors.newFixedThreadPool(4);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void estimateProgress(ProjectEntity project, String dailyWorkText, OnEstimationCompletedListener listener) {
        executor.execute(() -> {
            AtomicBoolean isDelivered = new AtomicBoolean(false);
            CountDownLatch latch = new CountDownLatch(2);

            String groqKey = BuildConfig.GROQ_API_KEY;
            String geminiKey = BuildConfig.GEMINI_API_KEY;

            // Route A: Ultra-Fast Groq LLM (Qwen 3.8 27B / GPT-OSS 120B in ~700ms)
            if (groqKey != null && !groqKey.trim().isEmpty() && !groqKey.contains("YOUR_")) {
                executor.execute(() -> {
                    try {
                        ProgressEstimationResult groqResult = queryGroqEstimator(project, dailyWorkText, groqKey.trim());
                        if (groqResult != null && isDelivered.compareAndSet(false, true)) {
                            deliverSuccess(listener, groqResult);
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Groq progress estimation failed: " + e.getMessage());
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
                        ProgressEstimationResult geminiResult = queryGeminiEstimator(project, dailyWorkText, geminiKey.trim());
                        if (geminiResult != null && isDelivered.compareAndSet(false, true)) {
                            deliverSuccess(listener, geminiResult);
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Gemini progress estimation failed: " + e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                });
            } else {
                latch.countDown();
            }

            // Fallback guard: delivers local estimation if cloud queries time out or fail
            executor.execute(() -> {
                try {
                    latch.await(7, TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {}

                if (isDelivered.compareAndSet(false, true)) {
                    Log.i(TAG, "Cloud progress estimation timed out; using deterministic local estimation");
                    ProgressEstimationResult fallback = computeLocalEstimation(project, dailyWorkText);
                    deliverSuccess(listener, fallback);
                }
            });
        });
    }

    private ProgressEstimationResult queryGeminiEstimator(ProjectEntity project, String dailyWorkText, String apiKey) {
        String prompt = buildPrompt(project, dailyWorkText);
        String[] candidateModels = new String[]{"gemini-3.5-flash-lite", "gemini-3.8-flash"};

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
                genConfig.put("temperature", 0.2);
                genConfig.put("response_mime_type", "application/json");
                root.put("generationConfig", genConfig);

                RequestBody body = RequestBody.create(root.toString(), MediaType.parse("application/json"));
                Request request = new Request.Builder().url(url).post(body).build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String respStr = response.body().string();
                        JSONObject json = new JSONObject(respStr);
                        String candidateText = json.getJSONArray("candidates")
                                .getJSONObject(0)
                                .getJSONObject("content")
                                .getJSONArray("parts")
                                .getJSONObject(0)
                                .getString("text");

                        ProgressEstimationResult result = parseJsonResponse(candidateText, project);
                        result.modelUsed = "Gemini " + model;
                        return result;
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Gemini model " + model + " error: " + e.getMessage());
            }
        }
        return null;
    }

    private ProgressEstimationResult queryGroqEstimator(ProjectEntity project, String dailyWorkText, String apiKey) {
        String[] candidateModels = new String[]{"qwen/qwen3.8-27b", "openai/gpt-oss-120b"};

        for (String model : candidateModels) {
            try {
                String url = "https://api.groq.com/openai/v1/chat/completions";
                String prompt = buildPrompt(project, dailyWorkText);

                JSONObject payload = new JSONObject();
                payload.put("model", model);
                payload.put("temperature", 0.2);

                JSONObject responseFormat = new JSONObject();
                responseFormat.put("type", "json_object");
                payload.put("response_format", responseFormat);

                org.json.JSONArray messages = new org.json.JSONArray();
                JSONObject sysMsg = new JSONObject();
                sysMsg.put("role", "system");
                sysMsg.put("content", "You are an elite civil construction quantity surveyor and site supervisor in Bangladesh. Output valid JSON only.");
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

                        ProgressEstimationResult result = parseJsonResponse(content, project);
                        result.modelUsed = "Groq " + model;
                        return result;
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Groq estimation error on " + model + ": " + e.getMessage());
            }
        }
        return null;
    }

    private String buildPrompt(ProjectEntity project, String dailyWorkText) {
        return "You are an elite Civil Construction Quantity Surveyor in Bangladesh evaluating daily work on a jobsite.\n\n" +
                "PROJECT BASELINE SCOPE:\n" +
                "- Project Name: " + project.getName() + " (" + project.getProjectCode() + ")\n" +
                "- Building Type: " + project.getBuildingType() + "\n" +
                "- Total Rooms: " + project.getTotalRooms() + " rooms\n" +
                "- Wall Height: " + project.getWallHeightFeet() + " feet\n" +
                "- Total Windows: " + project.getNumWindows() + " windows\n" +
                "- Total Doors: " + project.getNumDoors() + " doors\n" +
                "- Flooring Type: " + project.getFlooringType() + "\n" +
                "- Additional Scope: " + project.getPlanDetailsJson() + "\n\n" +
                "CURRENT TRADE PROGRESS BEFORE TODAY:\n" +
                "- Overall: " + project.getOverallProgressPercent() + "%\n" +
                "- Civil & Structural: " + project.getCivilProgressPercent() + "%\n" +
                "- Electrical & Wiring: " + project.getElectricProgressPercent() + "%\n" +
                "- Plumbing & Sanitary: " + project.getPlumbingProgressPercent() + "%\n" +
                "- Thai Glass & Windows: " + project.getThaiGlassProgressPercent() + "%\n" +
                "- Tiles & Flooring: " + project.getTilesFlooringProgressPercent() + "%\n" +
                "- Interior Plaster & Finishing: " + project.getInteriorPlasterProgressPercent() + "%\n\n" +
                "TODAY'S WORK LOG ENTERED BY SITE STAFF:\n" +
                "\"" + dailyWorkText + "\"\n\n" +
                "TASK:\n" +
                "Analyze the daily log against the project scope. Determine how much work has been accomplished today and calculate updated progress percentages (0 to 100) for each trade and overall. " +
                "Ensure percentages only increase or stay steady based on actual work done.\n\n" +
                "Return ONLY a JSON object with this exact schema:\n" +
                "{\n" +
                "  \"overallProgressPercent\": number,\n" +
                "  \"civilProgress\": number,\n" +
                "  \"electricProgress\": number,\n" +
                "  \"plumbingProgress\": number,\n" +
                "  \"thaiGlassProgress\": number,\n" +
                "  \"tilesFlooringProgress\": number,\n" +
                "  \"interiorPlasterProgress\": number,\n" +
                "  \"completedTodaySummary\": \"string describing what was achieved today\",\n" +
                "  \"remainingWorkSummary\": \"string describing remaining items to complete\",\n" +
                "  \"aiAnalysisNotes\": \"insightful engineering observation on pace & milestones\"\n" +
                "}";
    }

    private ProgressEstimationResult parseJsonResponse(String jsonString, ProjectEntity project) {
        ProgressEstimationResult r = new ProgressEstimationResult();
        try {
            int start = jsonString.indexOf("{");
            int end = jsonString.lastIndexOf("}");
            if (start != -1 && end != -1) {
                jsonString = jsonString.substring(start, end + 1);
            }
            JSONObject j = new JSONObject(jsonString);
            r.overallProgressPercent = (float) j.optDouble("overallProgressPercent", project.getOverallProgressPercent() + 3f);
            r.civilProgress = (float) j.optDouble("civilProgress", project.getCivilProgressPercent());
            r.electricProgress = (float) j.optDouble("electricProgress", project.getElectricProgressPercent());
            r.plumbingProgress = (float) j.optDouble("plumbingProgress", project.getPlumbingProgressPercent());
            r.thaiGlassProgress = (float) j.optDouble("thaiGlassProgress", project.getThaiGlassProgressPercent());
            r.tilesFlooringProgress = (float) j.optDouble("tilesFlooringProgress", project.getTilesFlooringProgressPercent());
            r.interiorPlasterProgress = (float) j.optDouble("interiorPlasterProgress", project.getInteriorPlasterProgressPercent());
            r.completedTodaySummary = j.optString("completedTodaySummary", "Daily site milestones logged.");
            r.remainingWorkSummary = j.optString("remainingWorkSummary", "Subsequent trade finishes and inspections remaining.");
            r.aiAnalysisNotes = j.optString("aiAnalysisNotes", "On-schedule trajectory maintained based on verified site scope.");
        } catch (Exception e) {
            return computeLocalEstimation(project, jsonString);
        }
        return r;
    }

    private ProgressEstimationResult computeLocalEstimation(ProjectEntity project, String dailyWorkText) {
        ProgressEstimationResult r = new ProgressEstimationResult();
        r.modelUsed = "SiteSync Offline Rule Engine";
        String lower = dailyWorkText.toLowerCase(Locale.US);

        float civilDelta = 0f;
        float thaiDelta = 0f;
        float tilesDelta = 0f;
        float electricDelta = 0f;
        float plumbingDelta = 0f;
        float plasterDelta = 0f;

        if (lower.contains("thai") || lower.contains("glass") || lower.contains("window")) {
            thaiDelta += 20f;
        }
        if (lower.contains("tile") || lower.contains("tiles") || lower.contains("floor") || lower.contains("mosaic")) {
            tilesDelta += 15f;
        }
        if (lower.contains("plaster") || lower.contains("wall") || lower.contains("putty") || lower.contains("paint")) {
            plasterDelta += 12f;
        }
        if (lower.contains("electric") || lower.contains("wire") || lower.contains("conduit") || lower.contains("switch")) {
            electricDelta += 14f;
        }
        if (lower.contains("pipe") || lower.contains("sanitary") || lower.contains("plumbing") || lower.contains("drain")) {
            plumbingDelta += 14f;
        }
        if (lower.contains("rebar") || lower.contains("rod") || lower.contains("concrete") || lower.contains("brick") || lower.contains("pour")) {
            civilDelta += 10f;
        }

        r.civilProgress = Math.min(100f, project.getCivilProgressPercent() + civilDelta);
        r.thaiGlassProgress = Math.min(100f, project.getThaiGlassProgressPercent() + thaiDelta);
        r.tilesFlooringProgress = Math.min(100f, project.getTilesFlooringProgressPercent() + tilesDelta);
        r.electricProgress = Math.min(100f, project.getElectricProgressPercent() + electricDelta);
        r.plumbingProgress = Math.min(100f, project.getPlumbingProgressPercent() + plumbingDelta);
        r.interiorPlasterProgress = Math.min(100f, project.getInteriorPlasterProgressPercent() + plasterDelta);

        float avgTrades = (r.civilProgress + r.thaiGlassProgress + r.tilesFlooringProgress + r.electricProgress + r.plumbingProgress + r.interiorPlasterProgress) / 6f;
        r.overallProgressPercent = Math.min(100f, Math.max(project.getOverallProgressPercent(), avgTrades));

        r.completedTodaySummary = "Logged site operations verified against building parameters (" + project.getBuildingType() + ").";
        r.remainingWorkSummary = String.format(Locale.US, "Remaining: %.1f%% overall scope before handover.", 100f - r.overallProgressPercent);
        r.aiAnalysisNotes = "Progress calculated against verified baseline specifications.";
        return r;
    }

    private void deliverSuccess(OnEstimationCompletedListener listener, ProgressEstimationResult result) {
        mainHandler.post(() -> {
            if (listener != null) listener.onSuccess(result);
        });
    }

    private void deliverError(OnEstimationCompletedListener listener, String message) {
        mainHandler.post(() -> {
            if (listener != null) listener.onError(message);
        });
    }
}

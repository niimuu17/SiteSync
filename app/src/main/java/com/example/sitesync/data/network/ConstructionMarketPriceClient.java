package com.example.sitesync.data.network;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.sitesync.BuildConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Enterprise Construction Market Price Intelligence Client.
 * Queries the Brave Search API with Bangladesh regional context to compute real-time
 * material benchmarks and flag price inflation/overbilling.
 */
public class ConstructionMarketPriceClient {

    private static final String TAG = "MarketPriceClient";
    private static final String BRAVE_SEARCH_URL = "https://api.search.brave.com/res/v1/web/search";

    public enum PriceStatus {
        OVER_BILLED,      // > +8% above market benchmark (Coral Red)
        FAIR_MARKET,      // Within +/- 8% of market (Forest Green)
        DISCOUNTED_BULK   // < -8% below market benchmark (Deep Forest Teal)
    }

    public static class PriceEvaluation {
        public final String materialName;
        public final double billedPrice;
        public final double benchmarkPrice;
        public final double variancePct; // e.g. +14.2% or -5.1%
        public final PriceStatus status;
        public final String statusSummary;
        public final String source;

        public PriceEvaluation(String materialName, double billedPrice, double benchmarkPrice,
                               double variancePct, PriceStatus status, String statusSummary, String source) {
            this.materialName = materialName;
            this.billedPrice = billedPrice;
            this.benchmarkPrice = benchmarkPrice;
            this.variancePct = variancePct;
            this.status = status;
            this.statusSummary = statusSummary;
            this.source = source;
        }
    }

    public interface OnPriceEvaluatedListener {
        void onEvaluated(PriceEvaluation evaluation);
    }

    private static final OkHttpClient httpClient = new OkHttpClient();
    private static final ExecutorService executor = Executors.newFixedThreadPool(3);
    private static Handler mainHandler;
    private static final Map<String, Double> priceCache = new ConcurrentHashMap<>();

    // Standard Bangladesh Construction Benchmark Index (Fallback Baseline)
    private static final Map<String, Double> BASELINE_BENCHMARKS = new HashMap<>();
    static {
        // Rates in BDT per standard unit
        BASELINE_BENCHMARKS.put("cement", 560.0);      // BDT / 50kg bag
        BASELINE_BENCHMARKS.put("shah cement", 565.0);
        BASELINE_BENCHMARKS.put("bashundhara", 555.0);
        BASELINE_BENCHMARKS.put("seven rings", 550.0);
        BASELINE_BENCHMARKS.put("rebar", 92000.0);     // BDT / Metric Ton
        BASELINE_BENCHMARKS.put("rod", 92000.0);
        BASELINE_BENCHMARKS.put("steel", 92000.0);
        BASELINE_BENCHMARKS.put("bsrm", 93500.0);
        BASELINE_BENCHMARKS.put("aks", 91000.0);
        BASELINE_BENCHMARKS.put("ksrm", 89500.0);
        BASELINE_BENCHMARKS.put("sand", 48.0);         // BDT / CFT
        BASELINE_BENCHMARKS.put("sylhet sand", 52.0);
        BASELINE_BENCHMARKS.put("local sand", 35.0);
        BASELINE_BENCHMARKS.put("stone", 230.0);       // BDT / CFT (crushed 20mm)
        BASELINE_BENCHMARKS.put("aggregate", 225.0);
        BASELINE_BENCHMARKS.put("brick", 14.0);        // BDT / pc (1st class auto)
        BASELINE_BENCHMARKS.put("auto brick", 14.5);
        BASELINE_BENCHMARKS.put("bitumen", 7500.0);    // BDT / drum
    }

    /**
     * Evaluates a billed rate against Bangladesh market intelligence.
     */
    public static void evaluatePrice(String materialName, double billedUnitPrice,
                                     String unit, OnPriceEvaluatedListener listener) {
        if (materialName == null || materialName.trim().isEmpty()) {
            deliverResult(listener, new PriceEvaluation("Unknown", billedUnitPrice, billedUnitPrice,
                    0.0, PriceStatus.FAIR_MARKET, "No Material Specified", "Baseline"));
            return;
        }

        String normalizedName = materialName.trim().toLowerCase(Locale.US);

        // Check cache first
        if (priceCache.containsKey(normalizedName)) {
            double cachedRate = priceCache.get(normalizedName);
            deliverResult(listener, computeEvaluation(materialName, billedUnitPrice, cachedRate, "Brave Cache"));
            return;
        }

        executor.execute(() -> {
            String apiKey = BuildConfig.BRAVE_SEARCH_API_KEY;
            if (apiKey == null || apiKey.trim().isEmpty()) {
                double fallback = getBaselineRate(normalizedName, unit);
                priceCache.put(normalizedName, fallback);
                deliverResult(listener, computeEvaluation(materialName, billedUnitPrice, fallback, "BD Market Baseline"));
                return;
            }

            try {
                String query = String.format(Locale.US, "%s price in Bangladesh BDT %s", materialName, unit != null ? unit : "");
                String encodedQuery = URLEncoder.encode(query, "UTF-8");
                String url = BRAVE_SEARCH_URL + "?q=" + encodedQuery + "&count=5";

                Request request = new Request.Builder()
                        .url(url)
                        .header("Accept", "application/json")
                        .header("X-Subscription-Token", apiKey.trim())
                        .build();

                httpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        Log.w(TAG, "Brave search request failed: " + e.getMessage());
                        fallbackEvaluation(materialName, billedUnitPrice, unit, listener);
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        if (!response.isSuccessful() || response.body() == null) {
                            fallbackEvaluation(materialName, billedUnitPrice, unit, listener);
                            return;
                        }

                        try {
                            String responseBody = response.body().string();
                            JSONObject json = new JSONObject(responseBody);
                            double extractedPrice = parsePriceFromBraveResponse(json, normalizedName);

                            if (extractedPrice > 0) {
                                priceCache.put(normalizedName, extractedPrice);
                                deliverResult(listener, computeEvaluation(materialName, billedUnitPrice,
                                        extractedPrice, "Live Brave Intelligence"));
                            } else {
                                fallbackEvaluation(materialName, billedUnitPrice, unit, listener);
                            }
                        } catch (Exception ex) {
                            Log.e(TAG, "Error parsing Brave search payload: " + ex.getMessage());
                            fallbackEvaluation(materialName, billedUnitPrice, unit, listener);
                        }
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Brave search execution error: " + e.getMessage());
                fallbackEvaluation(materialName, billedUnitPrice, unit, listener);
            }
        });
    }

    private static void fallbackEvaluation(String materialName, double billedUnitPrice,
                                           String unit, OnPriceEvaluatedListener listener) {
        String normalizedName = materialName.toLowerCase(Locale.US);
        double baseline = getBaselineRate(normalizedName, unit);
        priceCache.put(normalizedName, baseline);
        deliverResult(listener, computeEvaluation(materialName, billedUnitPrice, baseline, "BD Market Benchmark"));
    }

    private static double parsePriceFromBraveResponse(JSONObject json, String materialKey) {
        try {
            if (!json.has("web")) return 0.0;
            JSONObject web = json.getJSONObject("web");
            if (!web.has("results")) return 0.0;
            JSONArray results = web.getJSONArray("results");

            Pattern pricePattern = Pattern.compile("(?:BDT|Tk|Tk\\.|৳)\\s*([0-9]{1,3}(?:,[0-9]{3})*(?:\\.[0-9]+)?|[0-9]+)", Pattern.CASE_INSENSITIVE);

            for (int i = 0; i < results.length(); i++) {
                JSONObject item = results.getJSONObject(i);
                String snippet = item.optString("description", "") + " " + item.optString("title", "");
                Matcher matcher = pricePattern.matcher(snippet);
                if (matcher.find()) {
                    String numStr = matcher.group(1).replace(",", "");
                    double price = Double.parseDouble(numStr);
                    if (isValidPriceRange(materialKey, price)) {
                        return price;
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "parsePriceFromBraveResponse parse error", e);
        }
        return 0.0;
    }

    private static boolean isValidPriceRange(String materialKey, double price) {
        if (materialKey.contains("cement")) {
            return price >= 400 && price <= 800; // per bag
        } else if (materialKey.contains("rod") || materialKey.contains("steel") || materialKey.contains("rebar")) {
            return price >= 75000 && price <= 130000; // per ton
        } else if (materialKey.contains("sand")) {
            return price >= 25 && price <= 90; // per cft
        } else if (materialKey.contains("stone") || materialKey.contains("aggregate")) {
            return price >= 150 && price <= 350; // per cft
        } else if (materialKey.contains("brick")) {
            return price >= 8 && price <= 25; // per pc
        }
        return price > 0 && price < 500000;
    }

    public static double getBaselineRate(String materialKey, String unit) {
        for (Map.Entry<String, Double> entry : BASELINE_BENCHMARKS.entrySet()) {
            if (materialKey.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        // Category fallback
        if (unit != null) {
            String u = unit.toUpperCase(Locale.US);
            if (u.contains("BAG")) return 560.0;
            if (u.contains("TON")) return 92000.0;
            if (u.contains("CFT")) return 50.0;
            if (u.contains("PC")) return 14.0;
        }
        return 500.0;
    }

    public static PriceEvaluation computeEvaluation(String materialName, double billed,
                                                    double benchmark, String source) {
        if (benchmark <= 0.0001) benchmark = billed > 0 ? billed : 1.0;
        double variancePct = ((billed - benchmark) / benchmark) * 100.0;

        PriceStatus status;
        String summary;
        if (variancePct > 8.0) {
            status = PriceStatus.OVER_BILLED;
            summary = String.format(Locale.US, "+%.1f%% Over Market Rate", variancePct);
        } else if (variancePct < -8.0) {
            status = PriceStatus.DISCOUNTED_BULK;
            summary = String.format(Locale.US, "%.1f%% Bulk Discount", Math.abs(variancePct));
        } else {
            status = PriceStatus.FAIR_MARKET;
            summary = "Fair Market Price";
        }

        return new PriceEvaluation(materialName, billed, benchmark, variancePct, status, summary, source);
    }

    private static void deliverResult(OnPriceEvaluatedListener listener, PriceEvaluation evaluation) {
        if (listener != null) {
            try {
                if (mainHandler == null && Looper.getMainLooper() != null) {
                    mainHandler = new Handler(Looper.getMainLooper());
                }
            } catch (Exception ignored) {}

            if (mainHandler != null) {
                mainHandler.post(() -> listener.onEvaluated(evaluation));
            } else {
                listener.onEvaluated(evaluation);
            }
        }
    }
}

package com.example.sitesync.data.network;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.sitesync.BuildConfig;
import com.example.sitesync.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Enterprise Product Image Resolver & Persistent Multi-Tier Disk Cache.
 * Queries Brave Search Image API, falls back seamlessly to Wikimedia Commons
 * and Curated Bangladesh Construction Material Catalogs, permanently storing
 * photos in internal storage for instant zero-latency offline reuse.
 */
public class MaterialImageResolver {

    private static final String TAG = "MaterialImageResolver";
    private static final String ICONS_DIR = "product_icons";

    private static final Map<String, String> MEMORY_CACHE = new ConcurrentHashMap<>();
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(3);
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .build();

    public interface OnImageResolvedListener {
        void onResolved(File imageFile, String imageUrl);
    }

    public static void resolveProductImage(Context context, String materialName, OnImageResolvedListener listener) {
        resolveProductImage(context, materialName, null, listener);
    }

    /**
     * Resolve product image:
     * 1. Check in-memory cache.
     * 2. Check local persistent disk cache.
     * 3. Fetch from Internet (Curated Catalog -> Brave Search -> Wikimedia Commons -> Unsplash Fallback).
     */
    public static void resolveProductImage(Context context, String materialName, String category, OnImageResolvedListener listener) {
        if (context == null || materialName == null || materialName.trim().isEmpty()) {
            if (listener != null) listener.onResolved(null, null);
            return;
        }

        final Context appContext = context.getApplicationContext();
        final String normalized = normalizeKey(materialName);

        // Check 1: In-memory cache
        if (MEMORY_CACHE.containsKey(normalized)) {
            String path = MEMORY_CACHE.get(normalized);
            if (path != null) {
                File cachedFile = new File(path);
                if (cachedFile.exists() && cachedFile.length() > 0) {
                    if (listener != null) listener.onResolved(cachedFile, cachedFile.getAbsolutePath());
                    return;
                }
            }
        }

        // Check 2: On-disk persistent file
        File diskFile = getLocalImageFile(appContext, normalized);
        if (diskFile.exists() && diskFile.length() > 0) {
            MEMORY_CACHE.put(normalized, diskFile.getAbsolutePath());
            if (listener != null) listener.onResolved(diskFile, diskFile.getAbsolutePath());
            return;
        }

        // Check 3: Multi-tier Internet fetch in background
        EXECUTOR.execute(() -> {
            File downloaded = fetchAndSaveFromInternet(appContext, materialName, normalized, category);
            if (downloaded != null && downloaded.exists() && downloaded.length() > 0) {
                MEMORY_CACHE.put(normalized, downloaded.getAbsolutePath());
                if (listener != null) {
                    MAIN_HANDLER.post(() -> listener.onResolved(downloaded, downloaded.getAbsolutePath()));
                }
            } else {
                if (listener != null) {
                    MAIN_HANDLER.post(() -> listener.onResolved(null, null));
                }
            }
        });
    }

    public static String resolveImageForMaterial(String materialName, String category) {
        if (materialName == null || materialName.trim().isEmpty()) {
            return null;
        }
        String normalized = normalizeKey(materialName);
        if (MEMORY_CACHE.containsKey(normalized)) {
            return MEMORY_CACHE.get(normalized);
        }
        String curated = getCuratedCatalogUrl(materialName, category);
        if (curated != null) {
            return curated;
        }
        return null;
    }

    /**
     * Binds a product icon to an ImageView using Glide with placeholder and local caching.
     */
    public static void loadProductIcon(ImageView imageView, String materialName, String category, String existingUrlOrPath) {
        if (imageView == null) return;
        Context context = imageView.getContext();
        int placeholderRes = getCategoryPlaceholder(category);
        final int defaultPad = (int) (6 * context.getResources().getDisplayMetrics().density);

        // If existing local file or URL provided, check if valid
        if (existingUrlOrPath != null && !existingUrlOrPath.trim().isEmpty()) {
            File existingFile = new File(existingUrlOrPath);
            Object model = existingFile.exists() ? existingFile : existingUrlOrPath;

            imageView.setImageTintList(null);
            imageView.clearColorFilter();
            imageView.setPadding(0, 0, 0, 0);

            Glide.with(context)
                    .load(model)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(imageView);
            return;
        }

        // Check local disk cache directly (Zero network usage)
        String normalized = normalizeKey(materialName);
        File localFile = getLocalImageFile(context, normalized);
        if (localFile.exists() && localFile.length() > 0) {
            imageView.setImageTintList(null);
            imageView.clearColorFilter();
            imageView.setPadding(0, 0, 0, 0);

            Glide.with(context)
                    .load(localFile)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .into(imageView);
            return;
        }

        // Set placeholder with tint & padding immediately while loading
        imageView.setImageTintList(android.content.res.ColorStateList.valueOf(
                androidx.core.content.ContextCompat.getColor(context, R.color.peach_accent)));
        imageView.clearColorFilter();
        imageView.setPadding(defaultPad, defaultPad, defaultPad, defaultPad);
        imageView.setImageResource(placeholderRes);

        resolveProductImage(context, materialName, category, (imageFile, imageUrl) -> {
            if (imageFile != null && imageFile.exists()) {
                imageView.setImageTintList(null);
                imageView.clearColorFilter();
                imageView.setPadding(0, 0, 0, 0);
                Glide.with(context)
                        .load(imageFile)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .into(imageView);
            }
        });
    }

    private static File fetchAndSaveFromInternet(Context context, String materialName, String normalizedKey, String category) {
        // Tier 1: Check verified Bangladesh Construction Catalog
        String curatedUrl = getCuratedCatalogUrl(materialName, category);
        if (curatedUrl != null) {
            File downloaded = downloadImageBytesToFile(context, curatedUrl, normalizedKey);
            if (downloaded != null && downloaded.exists() && downloaded.length() > 0) {
                return downloaded;
            }
        }

        // Tier 2: Query Brave Search Image API
        String apiKey = BuildConfig.BRAVE_SEARCH_API_KEY;
        if (apiKey != null && !apiKey.trim().isEmpty() && !apiKey.contains("YOUR_")) {
            try {
                String query = buildSearchQuery(materialName);
                String url = "https://api.search.brave.com/res/v1/images/search?q="
                        + URLEncoder.encode(query, StandardCharsets.UTF_8.name()) + "&count=1";

                Request request = new Request.Builder()
                        .url(url)
                        .header("Accept", "application/json")
                        .header("X-Subscription-Token", apiKey.trim())
                        .build();

                Response response = HTTP_CLIENT.newCall(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    String respStr = response.body().string();
                    JSONObject json = new JSONObject(respStr);
                    JSONArray results = json.optJSONArray("results");
                    if (results != null && results.length() > 0) {
                        JSONObject first = results.getJSONObject(0);

                        String imgUrl = null;
                        JSONObject thumb = first.optJSONObject("thumbnail");
                        if (thumb != null && thumb.has("src")) {
                            imgUrl = thumb.getString("src");
                        }
                        if (imgUrl == null || imgUrl.isEmpty()) {
                            JSONObject props = first.optJSONObject("properties");
                            if (props != null && props.has("url")) {
                                imgUrl = props.getString("url");
                            }
                        }

                        if (imgUrl != null && !imgUrl.isEmpty()) {
                            File downloaded = downloadImageBytesToFile(context, imgUrl, normalizedKey);
                            if (downloaded != null && downloaded.exists() && downloaded.length() > 0) {
                                return downloaded;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Brave search attempt failed: " + e.getMessage());
            }
        }

        // Tier 3: Query Wikimedia Commons Search API (Free, high-res, zero-key)
        try {
            String wikiQuery = buildWikiSearchQuery(materialName);
            String wikiApiUrl = "https://commons.wikimedia.org/w/api.php?action=query&generator=search&gsrnamespace=6&gsrsearch="
                    + URLEncoder.encode(wikiQuery, StandardCharsets.UTF_8.name())
                    + "&gsrlimit=1&prop=imageinfo&iiprop=url&format=json";

            Request wikiReq = new Request.Builder()
                    .url(wikiApiUrl)
                    .header("User-Agent", "SiteSyncApp/1.0 (contact@sitesync.io)")
                    .build();

            Response wikiResp = HTTP_CLIENT.newCall(wikiReq).execute();
            if (wikiResp.isSuccessful() && wikiResp.body() != null) {
                String wikiStr = wikiResp.body().string();
                JSONObject root = new JSONObject(wikiStr);
                JSONObject queryObj = root.optJSONObject("query");
                if (queryObj != null) {
                    JSONObject pages = queryObj.optJSONObject("pages");
                    if (pages != null && pages.length() > 0) {
                        java.util.Iterator<String> keys = pages.keys();
                        if (keys.hasNext()) {
                            JSONObject page = pages.getJSONObject(keys.next());
                            JSONArray imageInfo = page.optJSONArray("imageinfo");
                            if (imageInfo != null && imageInfo.length() > 0) {
                                String imgUrl = imageInfo.getJSONObject(0).optString("url");
                                if (imgUrl != null && !imgUrl.isEmpty()) {
                                    File downloaded = downloadImageBytesToFile(context, imgUrl, normalizedKey);
                                    if (downloaded != null && downloaded.exists() && downloaded.length() > 0) {
                                        return downloaded;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Wikimedia fallback failed: " + e.getMessage());
        }

        // Tier 4: Guaranteed construction photo fallback
        String fallbackUrl = getGeneralConstructionFallback(category);
        if (fallbackUrl != null) {
            return downloadImageBytesToFile(context, fallbackUrl, normalizedKey);
        }

        return null;
    }

    private static File downloadImageBytesToFile(Context context, String imgUrl, String normalizedKey) {
        try {
            Request imgRequest = new Request.Builder()
                    .url(imgUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build();
            Response imgResp = HTTP_CLIENT.newCall(imgRequest).execute();
            if (imgResp.isSuccessful() && imgResp.body() != null) {
                File targetFile = getLocalImageFile(context, normalizedKey);
                try (InputStream is = imgResp.body().byteStream();
                     FileOutputStream fos = new FileOutputStream(targetFile)) {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, read);
                    }
                    fos.flush();
                }
                return targetFile;
            }
        } catch (Exception e) {
            Log.w(TAG, "Error downloading image bytes: " + e.getMessage());
        }
        return null;
    }

    public static File getLocalImageFile(Context context, String normalizedKey) {
        File dir = new File(context.getFilesDir(), ICONS_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String fileName = hashKey(normalizedKey) + ".jpg";
        return new File(dir, fileName);
    }

    public static String getCuratedCatalogUrl(String materialName, String category) {
        if (materialName == null) return null;
        String lower = materialName.toLowerCase(Locale.US);

        if (lower.contains("cement") || lower.contains("সিমেন্ট") || lower.contains("shah") || lower.contains("bashundhara") || lower.contains("fresh")) {
            return "https://images.unsplash.com/photo-1589939705384-5185137a7f0f?w=600&auto=format&fit=crop&q=80";
        } else if (lower.contains("rod") || lower.contains("rebar") || lower.contains("steel") || lower.contains("bsrm") || lower.contains("aks") || lower.contains("ksrm") || lower.contains("রড")) {
            return "https://images.unsplash.com/photo-1504307651254-35680f356dfd?w=600&auto=format&fit=crop&q=80";
        } else if (lower.contains("sand") || lower.contains("বালু") || lower.contains("sylhet")) {
            return "https://images.unsplash.com/photo-1509316975850-ff9c5deb0cd9?w=600&auto=format&fit=crop&q=80";
        } else if (lower.contains("stone") || lower.contains("chips") || lower.contains("aggregate") || lower.contains("পাথর") || lower.contains("khoa")) {
            return "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600&auto=format&fit=crop&q=80";
        } else if (lower.contains("brick") || lower.contains("ইট") || lower.contains("clay")) {
            return "https://images.unsplash.com/photo-1590381105924-c72589b9ef3f?w=600&auto=format&fit=crop&q=80";
        } else if (lower.contains("block") || lower.contains("cinder") || lower.contains("concrete")) {
            return "https://images.unsplash.com/photo-1590069261209-f8e9b8642343?w=600&auto=format&fit=crop&q=80";
        } else if (lower.contains("pipe") || lower.contains("pvc") || lower.contains("conduit") || lower.contains("পাইপ") || lower.contains("plumbing")) {
            return "https://images.unsplash.com/photo-1581092160607-ee22621dd758?w=600&auto=format&fit=crop&q=80";
        } else if (lower.contains("paint") || lower.contains("berger") || lower.contains("রং") || lower.contains("coating") || lower.contains("distemper")) {
            return "https://images.unsplash.com/photo-1562259949-e8e7689d7828?w=600&auto=format&fit=crop&q=80";
        } else if (lower.contains("tile") || lower.contains("ceramic") || lower.contains("টাইলস") || lower.contains("marble")) {
            return "https://images.unsplash.com/photo-1584622650111-993a426fbf0a?w=600&auto=format&fit=crop&q=80";
        } else if (lower.contains("helmet") || lower.contains("boot") || lower.contains("safety") || lower.contains("vest") || lower.contains("harness")) {
            return "https://images.unsplash.com/photo-1578575437130-527eed3abbec?w=600&auto=format&fit=crop&q=80";
        }

        if (category != null) {
            String catLower = category.toLowerCase(Locale.US);
            if (catLower.contains("cement")) return "https://images.unsplash.com/photo-1589939705384-5185137a7f0f?w=600&auto=format&fit=crop&q=80";
            if (catLower.contains("steel") || catLower.contains("rebar")) return "https://images.unsplash.com/photo-1504307651254-35680f356dfd?w=600&auto=format&fit=crop&q=80";
            if (catLower.contains("aggregate") || catLower.contains("sand")) return "https://images.unsplash.com/photo-1509316975850-ff9c5deb0cd9?w=600&auto=format&fit=crop&q=80";
            if (catLower.contains("brick")) return "https://images.unsplash.com/photo-1590381105924-c72589b9ef3f?w=600&auto=format&fit=crop&q=80";
        }

        return null;
    }

    public static String getGeneralConstructionFallback(String category) {
        return "https://images.unsplash.com/photo-1504307651254-35680f356dfd?w=600&auto=format&fit=crop&q=80";
    }

    private static String buildSearchQuery(String materialName) {
        String lower = materialName.toLowerCase(Locale.US);
        if (lower.contains("cement")) {
            return materialName + " bag bangladesh";
        } else if (lower.contains("rod") || lower.contains("rebar") || lower.contains("steel") || lower.contains("bsrm")) {
            return materialName + " rod bangladesh";
        } else if (lower.contains("sand") || lower.contains("বালু")) {
            return "sylhet sand construction";
        } else if (lower.contains("stone") || lower.contains("chips") || lower.contains("aggregate")) {
            return "crushed stone aggregate 20mm construction";
        } else if (lower.contains("brick") || lower.contains("ইট")) {
            return "red clay construction bricks bangladesh";
        }
        return materialName + " construction material";
    }

    private static String buildWikiSearchQuery(String materialName) {
        String lower = materialName.toLowerCase(Locale.US);
        if (lower.contains("cement")) {
            return "cement bag";
        } else if (lower.contains("rod") || lower.contains("rebar") || lower.contains("steel") || lower.contains("bsrm")) {
            return "rebar steel";
        } else if (lower.contains("sand") || lower.contains("বালু")) {
            return "construction sand";
        } else if (lower.contains("stone") || lower.contains("chips") || lower.contains("aggregate")) {
            return "gravel aggregate";
        } else if (lower.contains("brick") || lower.contains("ইট")) {
            return "clay bricks";
        }
        return "construction material";
    }

    private static int getCategoryPlaceholder(String category) {
        return R.drawable.ic_construction;
    }

    private static String normalizeKey(String name) {
        if (name == null) return "unknown";
        return name.trim().toLowerCase(Locale.US).replaceAll("[^a-z0-9]", "_");
    }

    private static String hashKey(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(input.hashCode());
        }
    }
}
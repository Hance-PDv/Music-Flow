package com.mflow.music;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import com.airbnb.lottie.LottieAnimationView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class LyricsFetcher {

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build();

    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static class LyricLine {
        public long timeMs;
        public String text;

        public LyricLine(long timeMs, String text) {
            this.timeMs = timeMs;
            this.text = text;
        }
    }

    public interface OnLyricsLoadedListener {
        void onLyricsLoaded(List<LyricLine> lyricsList);
    }

    public static void fetchLyrics(final Context context, String title, String artist, final long durationMs, final OnLyricsLoadedListener listener, final LottieAnimationView lottieView) {
        showLoadingState(lottieView);

        if (title == null || title.trim().isEmpty()) {
            showNoLyrics(listener, lottieView);
            return;
        }

        final String rawTitle = title.trim();
        boolean isUnknownArtist = (artist == null || artist.trim().isEmpty() || artist.equalsIgnoreCase("<unknown>") || artist.equalsIgnoreCase("unknown"));
        final String rawArtist = isUnknownArtist ? "" : artist.trim();

        String cacheKey = (rawTitle + "_" + rawArtist + "_" + (durationMs / 1000)).replaceAll("[^a-zA-Z0-9]", "_") + ".lrc";
        final File cacheFile = new File(context.getCacheDir(), cacheKey);

        if (cacheFile.exists()) {
            final String cachedLrc = readTextFile(cacheFile);
            final List<LyricLine> parsedList = parseLrc(cachedLrc);
            mainHandler.post(() -> {
                hideLoading(lottieView);
                if (listener != null) {
                    listener.onLyricsLoaded(parsedList);
                }
            });
            return;
        }

        fetchLyricsStrictly(rawTitle, rawArtist, durationMs, false, syncedLyrics -> {
            if (syncedLyrics != null && !syncedLyrics.trim().isEmpty()) {
                saveAndReturn(context, cacheFile, syncedLyrics, listener, lottieView);
            } else {
                mainHandler.post(() -> showNoLyrics(listener, lottieView));
            }
        });
    }

    private interface OnRawLyricsFetchedListener {
        void onFetched(String syncedLyrics);
    }

    private static void fetchLyricsStrictly(final String title, final String artist, final long durationMs, final boolean isRetry, final OnRawLyricsFetchedListener rawListener) {
        final int targetSeconds = (int) (durationMs / 1000);

        if (!artist.isEmpty() && targetSeconds > 0) {
            String getUrl = "https://lrclib.net/api/get?track_name=" + Uri.encode(title) 
                    + "&artist_name=" + Uri.encode(artist) 
                    + "&duration=" + targetSeconds;

            Request getRequest = new Request.Builder()
                    .url(getUrl)
                    .header("User-Agent", "MFlowApp/2.3")
                    .build();

            client.newCall(getRequest).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    if (!isRetry) {
                        fetchLyricsStrictly(title, artist, durationMs, true, rawListener);
                    } else {
                        performSearchFallback(title, artist, targetSeconds, false, rawListener);
                    }
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful() && response.body() != null) {
                        try {
                            String responseData = response.body().string();
                            JSONObject item = new JSONObject(responseData);
                            if (item.has("syncedLyrics") && !item.isNull("syncedLyrics")) {
                                String synced = item.getString("syncedLyrics");
                                if (synced != null && !synced.trim().isEmpty()) {
                                    rawListener.onFetched(synced);
                                    return;
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                    performSearchFallback(title, artist, targetSeconds, false, rawListener);
                }
            });
        } else {
            performSearchFallback(title, artist, targetSeconds, false, rawListener);
        }
    }

    private static void performSearchFallback(final String title, final String artist, final int targetSeconds, final boolean isRetry, final OnRawLyricsFetchedListener rawListener) {
        final String cleanTitle = cleanQuery(title);
        final String cleanArtist = cleanQuery(artist);

        StringBuilder query = new StringBuilder(cleanTitle);
        if (!cleanArtist.isEmpty()) {
            query.append(" ").append(cleanArtist);
        }

        String searchUrl = "https://lrclib.net/api/search?q=" + Uri.encode(query.toString());

        Request request = new Request.Builder()
                .url(searchUrl)
                .header("User-Agent", "MFlowApp/2.3")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (!isRetry) {
                    performSearchFallback(title, artist, targetSeconds, true, rawListener);
                } else {
                    rawListener.onFetched(null);
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseData = response.body().string();
                        JSONArray results = new JSONArray(responseData);

                        String bestSyncedLyrics = null;
                        String normalizedTargetTitle = cleanTitle.toLowerCase();

                        for (int i = 0; i < results.length(); i++) {
                            JSONObject item = results.getJSONObject(i);
                            if (!item.has("syncedLyrics") || item.isNull("syncedLyrics")) continue;

                            String synced = item.getString("syncedLyrics");
                            if (synced == null || synced.trim().isEmpty()) continue;

                            String trackName = item.optString("trackName", "").toLowerCase();
                            if (!trackName.isEmpty() && !normalizedTargetTitle.isEmpty()) {
                                if (!trackName.contains(normalizedTargetTitle) && !normalizedTargetTitle.contains(trackName)) {
                                    continue;
                                }
                            }

                            if (targetSeconds > 0 && item.has("duration") && !item.isNull("duration")) {
                                int apiDuration = (int) Math.round(item.getDouble("duration"));
                                if (apiDuration == targetSeconds) {
                                    bestSyncedLyrics = synced;
                                    break;
                                }
                            }
                        }

                        rawListener.onFetched(bestSyncedLyrics);
                        return;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                rawListener.onFetched(null);
            }
        });
    }

    private static void saveAndReturn(Context context, File cacheFile, String syncedLyrics, final OnLyricsLoadedListener listener, final LottieAnimationView lottieView) {
        try {
            FileOutputStream fos = new FileOutputStream(cacheFile);
            fos.write(syncedLyrics.getBytes("UTF-8"));
            fos.close();
        } catch (Exception ignored) {}

        final List<LyricLine> parsedList = parseLrc(syncedLyrics);
        mainHandler.post(() -> {
            hideLoading(lottieView);
            if (listener != null) {
                listener.onLyricsLoaded(parsedList);
            }
        });
    }

    private static String cleanQuery(String input) {
        if (input == null) return "";
        return input.replaceAll("[^\\p{L}\\p{Nd}\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    public static List<LyricLine> parseLrc(String lrcContent) {
        List<LyricLine> list = new ArrayList<>();
        if (lrcContent == null || lrcContent.trim().isEmpty()) return list;

        String[] lines = lrcContent.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("[") && line.contains("]")) {
                int closeTagIndex = line.indexOf("]");
                String timeStr = line.substring(1, closeTagIndex);
                String text = line.substring(closeTagIndex + 1).trim();
                long timeMs = parseTime(timeStr);
                if (timeMs >= 0 && !text.isEmpty()) {
                    list.add(new LyricLine(timeMs, text));
                }
            }
        }
        return list;
    }

    private static long parseTime(String timeStr) {
        try {
            String[] parts = timeStr.split(":");
            long min = Long.parseLong(parts[0]);
            String[] secParts = parts[1].split("\\.");
            long sec = Long.parseLong(secParts[0]);
            
            long ms = 0;
            if (secParts.length > 1) {
                String msStr = secParts[1];
                if (msStr.length() == 1) ms = Long.parseLong(msStr) * 100;
                else if (msStr.length() == 2) ms = Long.parseLong(msStr) * 10;
                else if (msStr.length() >= 3) ms = Long.parseLong(msStr.substring(0, 3));
            }
            return min * 60000 + sec * 1000 + ms;
        } catch (Exception e) {
            return -1;
        }
    }

    private static String readTextFile(File file) {
        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    private static void showLoadingState(final LottieAnimationView lottieView) {
        mainHandler.post(() -> {
            if (lottieView != null) {
                lottieView.setVisibility(View.VISIBLE);
                lottieView.playAnimation();
            }
        });
    }

    private static void hideLoading(final LottieAnimationView lottieView) {
        if (lottieView != null) {
            lottieView.cancelAnimation();
            lottieView.setVisibility(View.GONE);
        }
    }

    private static void showNoLyrics(final OnLyricsLoadedListener listener, final LottieAnimationView lottieView) {
        hideLoading(lottieView);
        if (listener != null) {
            listener.onLyricsLoaded(new ArrayList<>());
        }
    }
}

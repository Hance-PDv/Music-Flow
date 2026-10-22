package com.mflow.music;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

public class PrefsManager {
    private static final String PREF_NAME = "fav_data";
    private static final String KEY_FAV_LIST = "fav_list_json";

    public static void saveFavorites(Context context, ArrayList<HashMap<String, Object>> favList) {
        if (context == null) return;
        try {
            JSONArray jsonArray = new JSONArray();
            if (favList != null) {
                for (HashMap<String, Object> map : favList) {
                    jsonArray.put(new JSONObject(map));
                }
            }
            SharedPreferences sp = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            sp.edit().putString(KEY_FAV_LIST, jsonArray.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static ArrayList<HashMap<String, Object>> getFavorites(Context context) {
        ArrayList<HashMap<String, Object>> favList = new ArrayList<>();
        if (context == null) return favList;
        try {
            SharedPreferences sp = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String jsonString = sp.getString(KEY_FAV_LIST, "");

            if (jsonString != null && !jsonString.trim().isEmpty()) {
                JSONArray jsonArray = new JSONArray(jsonString);
                boolean hasDeletedFiles = false;

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObj = jsonArray.getJSONObject(i);
                    HashMap<String, Object> map = new HashMap<>();
                    Iterator<String> keys = jsonObj.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        map.put(key, jsonObj.get(key));
                    }

                    String path = map.get("path") != null ? map.get("path").toString() : "";
                    if (!path.isEmpty() && new File(path).exists()) {
                        favList.add(map);
                    } else {
                        hasDeletedFiles = true;
                    }
                }

                if (hasDeletedFiles) {
                    saveFavorites(context, favList);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return favList;
    }

    public static boolean isFavorite(Context context, String path) {
        if (context == null || path == null || path.isEmpty()) return false;
        
        if (!new File(path).exists()) return false;

        try {
            SharedPreferences sp = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String jsonString = sp.getString(KEY_FAV_LIST, "");
            
            return jsonString != null && jsonString.contains("\"path\":\"" + path + "\"");
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String getFavCountString(Context context) {
        ArrayList<HashMap<String, Object>> favList = getFavorites(context);
        return favList.size() + " Tracks";
    }

    public static String getFavTotalDurationString(Context context) {
        ArrayList<HashMap<String, Object>> favList = getFavorites(context);
        long totalDurationMs = 0;

        for (HashMap<String, Object> songMap : favList) {
            if (songMap.containsKey("duration")) {
                try {
                    long duration = Long.parseLong(String.valueOf(songMap.get("duration")));
                    totalDurationMs += duration;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (songMap.containsKey("path")) {
                try {
                    String path = String.valueOf(songMap.get("path"));
                    android.media.MediaMetadataRetriever retriever = new android.media.MediaMetadataRetriever();
                    retriever.setDataSource(path);
                    String timeStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION);
                    if (timeStr != null) {
                        totalDurationMs += Long.parseLong(timeStr);
                    }
                    retriever.release();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        long totalSeconds = totalDurationMs / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format(Locale.US, "%dh %dm %ds", hours, minutes, seconds);
        } else {
            return String.format(Locale.US, "%dm %ds", minutes, seconds);
        }
    }
}

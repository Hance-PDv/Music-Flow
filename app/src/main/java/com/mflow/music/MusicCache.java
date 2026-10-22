package com.mflow.music;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class MusicCache {

    private static final String FILE_NAME = "cached_songs.json";

    public static ArrayList<HashMap<String, Object>> getSongs(Context context) {
        ArrayList<HashMap<String, Object>> cachedList = getSavedSongs(context);
        ArrayList<HashMap<String, Object>> freshList = MusicFetcher.getAudioFiles(context);

        if (cachedList.isEmpty() || freshList.size() != cachedList.size()) {
            saveSongs(context, freshList);
            return freshList;
        }

        for (HashMap<String, Object> song : cachedList) {
            String path = song.get("path") != null ? song.get("path").toString() : "";
            if (!path.isEmpty() && !new File(path).exists()) {
                saveSongs(context, freshList);
                return freshList;
            }
        }

        return cachedList;
    }

    public static void refreshCache(final Context context) {
        if (context == null) return;
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                ArrayList<HashMap<String, Object>> freshList = MusicFetcher.getAudioFiles(context);
                saveSongs(context, freshList);
            }
        }).start();
    }

    public static void saveSongs(Context context, ArrayList<HashMap<String, Object>> list) {
        if (context == null) return;
        
        File file = new File(context.getFilesDir(), FILE_NAME);

        if (list == null || list.isEmpty()) {
            if (file.exists()) {
                file.delete();
            }
            return;
        }

        try {
            JSONArray jsonArray = new JSONArray();
            for (HashMap<String, Object> map : list) {
                JSONObject jsonObject = new JSONObject();
                for (String key : map.keySet()) {
                    jsonObject.put(key, map.get(key));
                }
                jsonArray.put(jsonObject);
            }
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(jsonArray.toString().getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static ArrayList<HashMap<String, Object>> getSavedSongs(Context context) {
        ArrayList<HashMap<String, Object>> list = new ArrayList<>();
        if (context == null) return list;

        File file = new File(context.getFilesDir(), FILE_NAME);
        if (!file.exists()) return list;

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            String jsonString = new String(data, StandardCharsets.UTF_8);

            JSONArray jsonArray = new JSONArray(jsonString);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                HashMap<String, Object> map = new HashMap<>();

                Iterator<String> keys = jsonObject.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    map.put(key, jsonObject.get(key));
                }

                String path = map.get("path") != null ? map.get("path").toString() : "";
                if (!path.isEmpty() && new File(path).exists()) {
                    list.add(map);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }
}

package com.mflow.music;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class PlaylistManager {

    private static final String PREF_NAME = "custom_playlists_data";
    private static final String KEY_PLAYLISTS = "playlists_json";
    private static String selectedPlaylistName = "";

    public static void setSelectedPlaylistName(String name) {
        selectedPlaylistName = name != null ? name : "";
    }

    public static String getSelectedPlaylistName() {
        return selectedPlaylistName;
    }

    public static void createPlaylist(Context context, String playlistName) {
        if (playlistName == null || playlistName.trim().isEmpty()) return;
        try {
            JSONObject root = getPlaylistsJson(context);
            if (!root.has(playlistName.trim())) {
                root.put(playlistName.trim(), new JSONArray());
                savePlaylistsJson(context, root);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void deletePlaylist(Context context, String playlistName) {
        if (playlistName == null || playlistName.trim().isEmpty()) return;
        try {
            JSONObject root = getPlaylistsJson(context);
            if (root.has(playlistName.trim())) {
                root.remove(playlistName.trim());
                savePlaylistsJson(context, root);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static ArrayList<HashMap<String, Object>> getPlaylistsListMap(Context context) {
        ArrayList<HashMap<String, Object>> listMap = new ArrayList<>();
        if (context == null) return listMap;

        JSONObject root = getPlaylistsJson(context);
        Iterator<String> keys = root.keys();

        while (keys.hasNext()) {
            String playlistName = keys.next();
            ArrayList<HashMap<String, Object>> validSongs = getPlaylistSongs(context, playlistName);

            long lastAlbumId = -1L;
            if (!validSongs.isEmpty()) {
                HashMap<String, Object> lastSong = validSongs.get(validSongs.size() - 1);
                lastAlbumId = parseAlbumId(lastSong);
            }

            HashMap<String, Object> map = new HashMap<>();
            map.put("name", playlistName);
            map.put("count", String.valueOf(validSongs.size()));
            map.put("lastAlbumId", lastAlbumId);

            listMap.add(map);
        }

        return listMap;
    }

    public static ArrayList<HashMap<String, Object>> getPlaylistSongs(Context context, String playlistName) {
        ArrayList<HashMap<String, Object>> validSongsList = new ArrayList<>();
        if (context == null || playlistName == null || playlistName.trim().isEmpty()) return validSongsList;

        try {
            JSONObject root = getPlaylistsJson(context);
            if (root.has(playlistName.trim())) {
                JSONArray songsArray = root.getJSONArray(playlistName.trim());
                JSONArray cleanedArray = new JSONArray();
                boolean hasDeletedFiles = false;

                for (int i = 0; i < songsArray.length(); i++) {
                    JSONObject songObj = songsArray.getJSONObject(i);
                    HashMap<String, Object> map = new HashMap<>();
                    
                    Iterator<String> keys = songObj.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        map.put(key, songObj.get(key));
                    }

                    String path = map.get("path") != null ? map.get("path").toString() : "";
                    if (!path.isEmpty() && new File(path).exists()) {
                        validSongsList.add(map);
                        cleanedArray.put(songObj);
                    } else {
                        hasDeletedFiles = true;
                    }
                }

                if (hasDeletedFiles) {
                    root.put(playlistName.trim(), cleanedArray);
                    savePlaylistsJson(context, root);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return validSongsList;
    }

    public static void addSongsToPlaylist(Context context, String playlistName, ArrayList<HashMap<String, Object>> selectedSongs) {
        if (playlistName == null || playlistName.trim().isEmpty() || selectedSongs == null || selectedSongs.isEmpty()) return;
        try {
            JSONObject root = getPlaylistsJson(context);
            JSONArray songsArray = root.optJSONArray(playlistName.trim());
            if (songsArray == null) {
                songsArray = new JSONArray();
            }

            for (HashMap<String, Object> songMap : selectedSongs) {
                String path = songMap.get("path") != null ? songMap.get("path").toString() : "";
                if (!path.isEmpty() && new File(path).exists()) {
                    JSONObject songObj = new JSONObject();
                    for (String key : songMap.keySet()) {
                        songObj.put(key, songMap.get(key));
                    }
                    songsArray.put(songObj);
                }
            }

            root.put(playlistName.trim(), songsArray);
            savePlaylistsJson(context, root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void removeSongFromPlaylist(Context context, String playlistName, HashMap<String, Object> songToRemove) {
        if (playlistName == null || playlistName.trim().isEmpty() || songToRemove == null) return;
        try {
            JSONObject root = getPlaylistsJson(context);
            if (root.has(playlistName.trim())) {
                JSONArray songsArray = root.getJSONArray(playlistName.trim());
                JSONArray updatedArray = new JSONArray();

                String targetPath = songToRemove.get("path") != null ? songToRemove.get("path").toString() : "";

                for (int i = 0; i < songsArray.length(); i++) {
                    JSONObject songObj = songsArray.getJSONObject(i);
                    String currentPath = songObj.has("path") ? songObj.getString("path") : "";

                    if (!targetPath.isEmpty() && !targetPath.equals(currentPath)) {
                        updatedArray.put(songObj);
                    }
                }

                root.put(playlistName.trim(), updatedArray);
                savePlaylistsJson(context, root);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static ArrayList<HashMap<String, Object>> getAvailableSongs(Context context, String playlistName) {
        ArrayList<HashMap<String, Object>> allSongs = MusicCache.getSongs(context);
        ArrayList<HashMap<String, Object>> currentSongs = getPlaylistSongs(context, playlistName);
        ArrayList<HashMap<String, Object>> availableSongs = new ArrayList<>();

        if (allSongs == null || allSongs.isEmpty()) {
            return availableSongs;
        }

        for (HashMap<String, Object> song : allSongs) {
            String path = song.get("path") != null ? song.get("path").toString() : "";
            boolean isAlreadyInPlaylist = false;

            if (!path.isEmpty() && new File(path).exists()) {
                for (HashMap<String, Object> currentSong : currentSongs) {
                    String currentPath = currentSong.get("path") != null ? currentSong.get("path").toString() : "";
                    if (path.equals(currentPath)) {
                        isAlreadyInPlaylist = true;
                        break;
                    }
                }
                if (!isAlreadyInPlaylist) {
                    availableSongs.add(song);
                }
            }
        }

        return availableSongs;
    }

    private static long parseAlbumId(HashMap<String, Object> songMap) {
        if (songMap == null) return -1L;
        if (songMap.containsKey("albumId") && songMap.get("albumId") != null) {
            try { return Long.parseLong(songMap.get("albumId").toString()); } catch (Exception ignored) {}
        }
        if (songMap.containsKey("album_id") && songMap.get("album_id") != null) {
            try { return Long.parseLong(songMap.get("album_id").toString()); } catch (Exception ignored) {}
        }
        return -1L;
    }

    private static JSONObject getPlaylistsJson(Context context) {
        try {
            SharedPreferences sp = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            return new JSONObject(sp.getString(KEY_PLAYLISTS, "{}"));
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    private static void savePlaylistsJson(Context context, JSONObject json) {
        SharedPreferences sp = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_PLAYLISTS, json.toString()).apply();
    }
}

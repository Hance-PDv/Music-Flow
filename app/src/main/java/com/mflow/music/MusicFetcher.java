package com.mflow.music;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class MusicFetcher {

    public static ArrayList<HashMap<String, Object>> getAudioFiles(Context context) {
        ArrayList<HashMap<String, Object>> list = new ArrayList<>();

        if (context == null) return list;

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID
        };

        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0 AND " + MediaStore.Audio.Media.DURATION + " >= 30000";

        Cursor cursor = context.getContentResolver().query(
            uri,
            projection,
            selection,
            null,
            MediaStore.Audio.Media.TITLE + " ASC"
        );

        if (cursor != null) {
            int titleIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
            int dataIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
            int artistIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
            int durationIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
            int albumIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);

            while (cursor.moveToNext()) {
                String path = cursor.getString(dataIdx);

                if (path != null && new File(path).exists()) {
                    HashMap<String, Object> map = new HashMap<>();

                    map.put("title", cursor.getString(titleIdx));
                    map.put("path", path);
                    map.put("artist", cursor.getString(artistIdx));
                    map.put("duration", cursor.getString(durationIdx));
                    map.put("album_id", cursor.getLong(albumIdx));

                    list.add(map);
                }
            }
            cursor.close();
        }

        return list;
    }
}

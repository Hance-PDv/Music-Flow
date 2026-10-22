package com.mflow.music;

import android.content.Context;
import android.content.SharedPreferences;

public class PlaybackStateManager {

    private static final String PREF_NAME = "MFlowPrefs";
    private static final String KEY_LAST_INDEX = "last_index";
    private static final String KEY_LAST_POSITION = "last_position";
    private static final String KEY_REPEAT_MODE = "repeat_mode";

    private final SharedPreferences prefs;

    public PlaybackStateManager(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void savePlaybackState(int index, int position) {
        if (index >= 0) {
            this.prefs.edit()
                .putInt(KEY_LAST_INDEX, index)
                .putInt(KEY_LAST_POSITION, position)
                .apply();
        }
    }

    public int getLastIndex() {
        return this.prefs.getInt(KEY_LAST_INDEX, -1);
    }

    public int getLastPosition() {
        return this.prefs.getInt(KEY_LAST_POSITION, 0);
    }

    public void clearSavedPosition() {
        this.prefs.edit().remove(KEY_LAST_POSITION).apply();
    }

    public void saveRepeatMode(int mode) {
        this.prefs.edit().putInt(KEY_REPEAT_MODE, mode).apply();
    }

    public int getRepeatMode() {
        return this.prefs.getInt(KEY_REPEAT_MODE, 0);
    }

    public boolean isRepeatEnabled() {
        return getRepeatMode() > 0;
    }

    public void clearAll() {
        this.prefs.edit().clear().apply();
    }
}

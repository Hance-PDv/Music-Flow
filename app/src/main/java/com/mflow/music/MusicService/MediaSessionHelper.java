package com.mflow.music;

import android.content.Context;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.SystemClock;

public class MediaSessionHelper {

    private MediaSession mediaSession;
    private final Context context;

    public interface MediaSessionCallback {
        void onPlay();
        void onPause();
        void onSkipToNext();
        void onSkipToPrevious();
        void onSeekTo(long pos);
    }

    public MediaSessionHelper(Context context, final MediaSessionCallback callback) {
        this.context = context;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mediaSession = new MediaSession(context, "MFlow_MediaSession");
            mediaSession.setCallback(new MediaSession.Callback() {
                @Override public void onPlay() { if (callback != null) callback.onPlay(); }
                @Override public void onPause() { if (callback != null) callback.onPause(); }
                @Override public void onSkipToNext() { if (callback != null) callback.onSkipToNext(); }
                @Override public void onSkipToPrevious() { if (callback != null) callback.onSkipToPrevious(); }
                @Override public void onSeekTo(long pos) { if (callback != null) callback.onSeekTo(pos); }
            });
            mediaSession.setActive(true);
        }
    }

    public void updateMetadata(String title, String artist, Bitmap albumArt, long duration) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && mediaSession != null) {
            MediaMetadata.Builder metadataBuilder = new MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, title != null ? title : "Unknown Title")
                .putString(MediaMetadata.METADATA_KEY_ARTIST, artist != null ? artist : "Unknown Artist")
                .putLong(MediaMetadata.METADATA_KEY_DURATION, duration);

            if (albumArt != null) {
                metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, albumArt);
            }
            mediaSession.setMetadata(metadataBuilder.build());
        }
    }

    public void updatePlaybackState(boolean isPlaying, long position) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && mediaSession != null) {
            long actions = PlaybackState.ACTION_PLAY |
                    PlaybackState.ACTION_PAUSE |
                    PlaybackState.ACTION_SKIP_TO_NEXT |
                    PlaybackState.ACTION_SKIP_TO_PREVIOUS |
                    PlaybackState.ACTION_PLAY_PAUSE |
                    PlaybackState.ACTION_SEEK_TO;

            int state = isPlaying ? PlaybackState.STATE_PLAYING : PlaybackState.STATE_PAUSED;
            float speed = isPlaying ? 1.0f : 0.0f;

            PlaybackState.Builder stateBuilder = new PlaybackState.Builder()
                .setActions(actions)
                .setState(state, position, speed, SystemClock.elapsedRealtime());

            mediaSession.setPlaybackState(stateBuilder.build());
        }
    }

    public MediaSession.Token getSessionToken() {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && mediaSession != null) ? mediaSession.getSessionToken() : null;
    }

    public void release() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && mediaSession != null) {
            mediaSession.setActive(false);
            mediaSession.release();
            mediaSession = null;
        }
    }
}

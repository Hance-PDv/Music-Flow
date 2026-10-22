package com.mflow.music;

import android.content.Context;
import android.content.Intent;
import android.os.CountDownTimer;

public class SleepManager {

    private final Context context;
    private final MediaPlayerProvider mediaPlayerProvider;

    private CountDownTimer timer;
    private boolean isActive = false;
    private long remainingMillis = 0;

    public interface MediaPlayerProvider {
        boolean isPlaying();
        long getCurrentPosition();
        long getDuration();
        void pauseMusic();
    }

    public SleepManager(Context context, MediaPlayerProvider mediaPlayerProvider) {
        this.context = context;
        this.mediaPlayerProvider = mediaPlayerProvider;
    }

    public boolean isActive() {
        return isActive;
    }

    public long getRemainingMillis() {
        return remainingMillis;
    }

    public void start(long durationMillis) {
        stopTimerOnly();
        if (durationMillis <= 0) return;
        
        isActive = true;
        remainingMillis = durationMillis;

        timer = new CountDownTimer(durationMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                remainingMillis = millisUntilFinished;
                broadcastTick(millisUntilFinished);
            }

            @Override
            public void onFinish() {
                isActive = false;
                remainingMillis = 0;
                broadcastFinish();

                if (mediaPlayerProvider != null) {
                    mediaPlayerProvider.pauseMusic();
                }
            }
        }.start();
    }

    public void stop() {
        stopTimerOnly();
        isActive = false;
        remainingMillis = 0;
        broadcastFinish();
    }

    private void stopTimerOnly() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private void broadcastTick(long millis) {
        if (context == null) return;
        long totalSeconds = millis / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        String formatted = String.format("%02d:%02d", minutes, seconds);

        Intent intent = new Intent(MusicService.ACTION_SLEEP_TICK);
        intent.setPackage(context.getPackageName());
        intent.putExtra(MusicService.EXTRA_SLEEP_TIME, formatted);
        context.sendBroadcast(intent);
    }

    private void broadcastFinish() {
        if (context == null) return;
        Intent intent = new Intent(MusicService.ACTION_SLEEP_FINISH);
        intent.setPackage(context.getPackageName());
        context.sendBroadcast(intent);
    }
}

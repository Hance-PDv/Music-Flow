package com.mflow.music;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.session.MediaSession;
import android.os.Build;

public class NotificationHelper {

    public static final String CHANNEL_ID = "mflow_live_channel";
    public static final int NOTIFICATION_ID = 101;

    public static final String ACTION_PREV = "ACTION_PREV";
    public static final String ACTION_TOGGLE = "ACTION_TOGGLE_PLAY";
    public static final String ACTION_NEXT = "ACTION_NEXT";
    public static final String ACTION_DISMISS = "ACTION_DISMISS";

    private final Context context;
    private final NotificationManager notificationManager;
    private final PendingIntent contentPendingIntent;
    private final int smallIconRes;

    public NotificationHelper(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();

        Intent openAppIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        this.contentPendingIntent = PendingIntent.getActivity(
            context, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        int iconId = context.getResources().getIdentifier("icon_mus", "drawable", context.getPackageName());
        this.smallIconRes = (iconId != 0) ? iconId : android.R.drawable.ic_media_play;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && notificationManager != null) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "MFlow Playback Controls",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setShowBadge(false);
            channel.setSound(null, null);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public Notification buildNotification(String title, String artist, Bitmap albumArt, boolean isPlaying, MediaSession.Token token) {
        String safeTitle = title != null ? title : "Unknown Title";
        String safeArtist = artist != null ? artist : "Unknown Artist";
        int playPauseIcon = isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Notification.MediaStyle mediaStyle = new Notification.MediaStyle();
            if (token != null) {
                mediaStyle.setMediaSession(token);
            }
            mediaStyle.setShowActionsInCompactView(0, 1, 2);

            Notification.Builder builder = new Notification.Builder(context)
                .setSmallIcon(smallIconRes)
                .setContentTitle(safeTitle)
                .setContentText(safeArtist)
                .setLargeIcon(albumArt)
                .setContentIntent(contentPendingIntent)
                .setDeleteIntent(getPendingIntent(ACTION_DISMISS))
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setOngoing(isPlaying)
                .setStyle(mediaStyle)
                .addAction(android.R.drawable.ic_media_previous, "Previous", getPendingIntent(ACTION_PREV))
                .addAction(playPauseIcon, "PlayPause", getPendingIntent(ACTION_TOGGLE))
                .addAction(android.R.drawable.ic_media_next, "Next", getPendingIntent(ACTION_NEXT));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.setChannelId(CHANNEL_ID);
            }

            return builder.build();
        } else {
            return new Notification.Builder(context)
                .setSmallIcon(smallIconRes)
                .setContentTitle(safeTitle)
                .setContentText(safeArtist)
                .setLargeIcon(albumArt)
                .setContentIntent(contentPendingIntent)
                .setDeleteIntent(getPendingIntent(ACTION_DISMISS))
                .setOngoing(isPlaying)
                .build();
        }
    }

    private PendingIntent getPendingIntent(String action) {
        Intent intent = new Intent(context, MusicService.class);
        intent.setAction(action);
        return PendingIntent.getService(
            context,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );
    }

    public void updateNotification(String title, String artist, Bitmap albumArt, boolean isPlaying, MediaSession.Token token) {
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, buildNotification(title, artist, albumArt, isPlaying, token));
        }
    }
}

package com.mflow.music;

import android.content.ContentUris;
import android.content.Context;
import android.net.Uri;
import android.widget.ImageView;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

public class ImageLoader {

    private static final Uri ALBUM_ART_URI = Uri.parse("content://media/external/audio/albumart");

    private static final RequestOptions REQUEST_OPTIONS = new RequestOptions()
            .override(120, 120)
            .dontAnimate()
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC);

    public static void loadAlbumArt(@Nullable Context context, @Nullable Uri uri, @NonNull ImageView imageView, @DrawableRes int placeholderRes) {
        if (context == null || uri == null) {
            imageView.setImageResource(placeholderRes);
            return;
        }

        Glide.with(imageView)
                .asBitmap()
                .load(uri)
                .apply(REQUEST_OPTIONS)
                .placeholder(placeholderRes)
                .error(placeholderRes)
                .into(imageView);
    }

    public static void loadAlbumArt(@Nullable Context context, long albumId, @NonNull ImageView imageView, @DrawableRes int placeholderRes) {
        Uri uri = (albumId > 0) ? ContentUris.withAppendedId(ALBUM_ART_URI, albumId) : null;
        loadAlbumArt(context, uri, imageView, placeholderRes);
    }

    public static void loadFullAlbumArt(@Nullable Context context, @Nullable Uri uri, @NonNull ImageView imageView, @DrawableRes int placeholderRes) {
        loadAlbumArt(context, uri, imageView, placeholderRes);
    }

    public static void loadFullAlbumArt(@Nullable Context context, long albumId, @NonNull ImageView imageView, @DrawableRes int placeholderRes) {
        loadAlbumArt(context, albumId, imageView, placeholderRes);
    }

    public static void clear(@NonNull ImageView imageView) {
        Glide.with(imageView).clear(imageView);
    }
}

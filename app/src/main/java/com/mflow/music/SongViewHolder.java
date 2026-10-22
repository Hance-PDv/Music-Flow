package com.mflow.music;

import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SongViewHolder {
    public final LinearLayout linearm; // الـ LinearLayout الرئيسي للحاوية
    public final ImageView img_music;
    public final TextView art;
    public final TextView title;

    public SongViewHolder(View view) {
        linearm = view.findViewById(R.id.linearm);
        img_music = view.findViewById(R.id.img_music);
        art = view.findViewById(R.id.art);
        title = view.findViewById(R.id.title);
    }
}

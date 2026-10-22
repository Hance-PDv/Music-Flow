package com.mflow.music;

import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SongViewHolder2 {
    public final LinearLayout linearm2;
    public final ImageView img_music2;
    public final TextView art2;
    public final TextView title2;
    public final CheckBox bottomremove2;

    public SongViewHolder2(View view) {
        linearm2 = view.findViewById(R.id.linearm2);
        img_music2 = view.findViewById(R.id.img_music2);
        art2 = view.findViewById(R.id.art2);
        title2 = view.findViewById(R.id.title2);
        bottomremove2 = view.findViewById(R.id.bottomremove2);
    }
}

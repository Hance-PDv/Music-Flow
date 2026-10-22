package com.mflow.music;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class CustomPermissionHandler {

    public static void requestFromButton(Activity activity) {
        if (activity == null) return;

        String audioPermission;
        String[] permissions;

        if (Build.VERSION.SDK_INT >= 33) {
            audioPermission = "android.permission.READ_MEDIA_AUDIO";
            permissions = new String[]{"android.permission.READ_MEDIA_AUDIO", "android.permission.POST_NOTIFICATIONS"};
        } else {
            audioPermission = "android.permission.READ_EXTERNAL_STORAGE";
            permissions = new String[]{"android.permission.READ_EXTERNAL_STORAGE"};
        }

        if (ContextCompat.checkSelfPermission(activity, audioPermission) != 0) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, audioPermission)) {
                boolean hasRequestedBefore = activity.getSharedPreferences("app_prefs", Activity.MODE_PRIVATE)
                        .getBoolean("permission_requested", false);
                
                if (hasRequestedBefore) {
                    showSamsungStyleDialog(activity);
                    return;
                }
            }

            activity.getSharedPreferences("app_prefs", Activity.MODE_PRIVATE)
                    .edit().putBoolean("permission_requested", true).apply();

            ActivityCompat.requestPermissions(activity, permissions, 1000);
        }
    }

    private static void showSamsungStyleDialog(final Activity activity) {
        final AlertDialog create = new AlertDialog.Builder(activity).create();
        
        LinearLayout linearLayout = new LinearLayout(activity);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(dpToPx(activity, 24), dpToPx(activity, 24), dpToPx(activity, 24), dpToPx(activity, 20));
        
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(Color.parseColor("#3B3D44"));
        gradientDrawable.setCornerRadius(dpToPx(activity, 28));
        linearLayout.setBackground(gradientDrawable);
        
        TextView textView = new TextView(activity);
        textView.setText("To use this feature, you need to allow permission in Settings.");
        textView.setTextColor(Color.WHITE);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16.0f);
        textView.setLineSpacing(0.0f, 1.2f);
        linearLayout.addView(textView);
        
        TextView textView2 = new TextView(activity);
        textView2.setText("Settings");
        textView2.setTextColor(Color.parseColor("#5A9CFF"));
        textView2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16.0f);
        textView2.setGravity(8388613); // Gravity.END
        
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(0, dpToPx(activity, 20), 0, 0);
        textView2.setLayoutParams(layoutParams);
        
        textView2.setOnClickListener(view -> {
            create.dismiss();
            try {
                Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.fromParts("package", activity.getPackageName(), null));
                activity.startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        
        linearLayout.addView(textView2);
        create.setView(linearLayout);
        
        if (create.getWindow() != null) {
            create.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        create.show();
    }

    private static int dpToPx(Activity activity, int i) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, i, activity.getResources().getDisplayMetrics());
    }
}

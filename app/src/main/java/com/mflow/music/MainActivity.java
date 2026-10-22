package com.mflow.music;

import android.animation.*;
import android.app.*;
import android.content.*;
import android.content.Intent;
import android.content.res.*;
import android.graphics.*;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.media.*;
import android.net.*;
import android.net.Uri;
import android.os.*;
import android.text.*;
import android.text.style.*;
import android.util.*;
import android.view.*;
import android.view.View.*;
import android.view.animation.*;
import android.webkit.*;
import android.widget.*;
import androidx.activity.*;
import androidx.annotation.*;
import androidx.annotation.experimental.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.arch.core.*;
import androidx.exifinterface.*;
import androidx.fragment.*;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.runtime.*;
import com.airbnb.lottie.*;
import com.bumptech.glide.*;
import com.bumptech.glide.gifdecoder.*;
import com.mflow.music.databinding.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.*;
import org.json.*;

public class MainActivity extends AppCompatActivity {
	
	private Timer _timer = new Timer();
	
	private MainBinding binding;
	
	private Intent intent = new Intent();
	private TimerTask timer;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		binding = MainBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());
		initialize(_savedInstanceState);
		initializeLogic();
	}
	
	private void initialize(Bundle _savedInstanceState) {
	}
	
	private void initializeLogic() {
		final android.widget.TextView txtM = findViewById(R.id.txt_m);
		final android.view.View fullNameContainer = findViewById(R.id.full_name_container);
		final android.widget.TextView txtSubtitle = findViewById(R.id.txt_subtitle);
		final android.view.View bottomContainer = findViewById(R.id.bottom_container);
		
		if (txtM != null) {
			txtM.setAlpha(0f);
			txtM.setScaleX(6.0f);
			txtM.setScaleY(6.0f);
		}
		if (fullNameContainer != null) {
			fullNameContainer.setVisibility(android.view.View.GONE);
			fullNameContainer.setAlpha(0f);
		}
		if (txtSubtitle != null) {
			txtSubtitle.setAlpha(0f);
			txtSubtitle.setTranslationY(20f);
		}
		if (bottomContainer != null) {
			bottomContainer.setAlpha(0f);
			bottomContainer.setTranslationY(40f);
		}
		
		if (txtM != null) {
			txtM.post(new Runnable() {
				@Override
				public void run() {
					txtM.setPivotX(txtM.getWidth() / 2f);
					txtM.setPivotY(txtM.getHeight() / 2f);
					txtM.animate()
					.alpha(1f)
					.scaleX(1.0f)
					.scaleY(1.0f)
					.setDuration(1300)
					.setInterpolator(new android.view.animation.DecelerateInterpolator(2.2f))
					.withEndAction(new Runnable() {
						@Override
						public void run() {
							txtM.setVisibility(android.view.View.GONE);
							
							if (fullNameContainer != null) {
								fullNameContainer.setVisibility(android.view.View.VISIBLE);
								fullNameContainer.setScaleX(0.85f);
								fullNameContainer.setScaleY(0.85f);
								
								fullNameContainer.animate()
								.alpha(1f)
								.scaleX(1.0f)
								.scaleY(1.0f)
								.setDuration(600)
								.setInterpolator(new android.view.animation.OvershootInterpolator(1.2f))
								.start();
							}
							
							if (txtSubtitle != null) {
								txtSubtitle.animate()
								.alpha(1f)
								.translationY(0f)
								.setDuration(800)
								.setStartDelay(200)
								.setInterpolator(new android.view.animation.DecelerateInterpolator())
								.start();
							}
							
							if (bottomContainer != null) {
								bottomContainer.animate()
								.alpha(1f)
								.translationY(0f)
								.setDuration(900)
								.setStartDelay(400)
								.setInterpolator(new android.view.animation.DecelerateInterpolator(1.5f))
								.start();
							}
						}
					})
					.start();
				}
			});
		}
		
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
			android.view.Window w = getWindow();
			w.clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
			w.addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			w.setStatusBarColor(android.graphics.Color.parseColor("#090A0F"));
			w.setNavigationBarColor(android.graphics.Color.parseColor("#090A0F"));
		}
		
		binding.textview1.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/regular.ttf"), 0);
		binding.txtFlow.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/bold.ttf"), 1);
		binding.txtFlow.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/bold.ttf"), 0);
		binding.txtVersion.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/regular.ttf"), 0);
		timer = new TimerTask() {
			@Override
			public void run() {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						intent.setClass(getApplicationContext(), HomeworkActivity.class);
						startActivity(intent);
						timer.cancel();
						finish();
					}
				});
			}
		};
		_timer.schedule(timer, (int)(3200));
	}
	
}
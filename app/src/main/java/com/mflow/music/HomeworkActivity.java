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
import android.view.View;
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
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.lifecycle.runtime.*;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager.widget.ViewPager.OnAdapterChangeListener;
import androidx.viewpager.widget.ViewPager.OnPageChangeListener;
import com.bumptech.glide.*;
import com.bumptech.glide.gifdecoder.*;
import com.mflow.music.databinding.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;
import org.json.*;

public class HomeworkActivity extends AppCompatActivity {
	
	private HomeworkBinding binding;
	
	private LinkFragmentAdapter link;
	private Intent intent = new Intent();
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		binding = HomeworkBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());
		initialize(_savedInstanceState);
		initializeLogic();
	}
	
	private void initialize(Bundle _savedInstanceState) {
		link = new LinkFragmentAdapter(getApplicationContext(), getSupportFragmentManager());
		
		binding.viewpager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			@Override
			public void onPageScrolled(int _position, float _positionOffset, int _positionOffsetPixels) {
				
			}
			
			@Override
			public void onPageSelected(int _position) {
				if (_position == 0) {
					binding.iconHome.setImageResource(R.drawable.icon_home1);
					binding.iconFav.setImageResource(R.drawable.icon_fav);
					binding.iconLibrary.setImageResource(R.drawable.icon_library);
				}
				if (_position == 1) {
					binding.iconFav.setImageResource(R.drawable.icon_fav1);
					binding.iconHome.setImageResource(R.drawable.icon_home);
					binding.iconLibrary.setImageResource(R.drawable.icon_library);
				}
				if (_position == 2) {
					binding.iconLibrary.setImageResource(R.drawable.icon_library1);
					binding.iconHome.setImageResource(R.drawable.icon_home);
					binding.iconFav.setImageResource(R.drawable.icon_fav);
				}
			}
			
			@Override
			public void onPageScrollStateChanged(int _scrollState) {
				
			}
		});
		
		binding.miniPlayer.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				intent.setClass(getApplicationContext(), FullplayerActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
			}
		});
		
		binding.btnSetting.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				intent.setClass(getApplicationContext(), SettingActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
			}
		});
		
		binding.tabHome.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				binding.viewpager.setCurrentItem((int)0);
				binding.iconHome.setImageResource(R.drawable.icon_home1);
				binding.iconFav.setImageResource(R.drawable.icon_fav);
				binding.iconLibrary.setImageResource(R.drawable.icon_library);
				android.view.animation.ScaleAnimation scaleAnim = new android.view.animation.ScaleAnimation(
				1f, 0.90f, 1f, 0.90f,
				android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f,
				android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f
				);
				scaleAnim.setDuration(120);
				scaleAnim.setRepeatMode(android.view.animation.Animation.REVERSE);
				scaleAnim.setRepeatCount(1);
				((android.view.View)_view).startAnimation(scaleAnim);
				
			}
		});
		
		binding.tabFav.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				binding.viewpager.setCurrentItem((int)1);
				binding.iconFav.setImageResource(R.drawable.icon_fav1);
				binding.iconHome.setImageResource(R.drawable.icon_home);
				binding.iconLibrary.setImageResource(R.drawable.icon_library);
				android.view.animation.ScaleAnimation scaleAnim = new android.view.animation.ScaleAnimation(
				1f, 0.90f, 1f, 0.90f,
				android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f,
				android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f
				);
				scaleAnim.setDuration(120);
				scaleAnim.setRepeatMode(android.view.animation.Animation.REVERSE);
				scaleAnim.setRepeatCount(1);
				((android.view.View)_view).startAnimation(scaleAnim);
				
			}
		});
		
		binding.tabLibrary.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				binding.viewpager.setCurrentItem((int)2);
				binding.iconLibrary.setImageResource(R.drawable.icon_library1);
				binding.iconHome.setImageResource(R.drawable.icon_home);
				binding.iconFav.setImageResource(R.drawable.icon_fav);
				android.view.animation.ScaleAnimation scaleAnim = new android.view.animation.ScaleAnimation(
				1f, 0.90f, 1f, 0.90f,
				android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f,
				android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f
				);
				scaleAnim.setDuration(120);
				scaleAnim.setRepeatMode(android.view.animation.Animation.REVERSE);
				scaleAnim.setRepeatCount(1);
				((android.view.View)_view).startAnimation(scaleAnim);
				
			}
		});
		
		binding.imageview14.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				intent.setClass(getApplicationContext(), SearchActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
			}
		});
	}
	
	private void initializeLogic() {
		CustomPermissionHandler.requestFromButton(this);
		
		// 1. جلب لون الخلفية أو السطح (Surface) مباشرة من ثيم Material 3 الحالي
		android.util.TypedValue typedValue = new android.util.TypedValue();
		getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true);
		int backgroundColor = typedValue.data;
		
		// 2. تطبيق اللون على شريط الحالة وأزرار التنقل لتتطابق تماماً مع الثيم
		getWindow().setStatusBarColor(backgroundColor);
		getWindow().setNavigationBarColor(backgroundColor);
		
		// 3. ضبط لون الأيقونات (فواتح أو غوامق) تلقائياً حسب الوضع الحالي (داكن أو فاتح)
		boolean isDarkTheme = (getResources().getConfiguration().uiMode & 
		android.content.res.Configuration.UI_MODE_NIGHT_MASK) == 
		android.content.res.Configuration.UI_MODE_NIGHT_YES;
		
		androidx.core.view.WindowInsetsControllerCompat controller = 
		androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
		
		if (controller != null) {
			// إذا كان الثيم فاتحاً، تصبح الأيقونات داكنة والعكس صحيح لضمان وضوحها
			controller.setAppearanceLightStatusBars(!isDarkTheme);
			controller.setAppearanceLightNavigationBars(!isDarkTheme);
		}
		
		link.setTabCount(3);
		binding.viewpager.setAdapter(link);
		binding.viewpager.setOffscreenPageLimit((int)3);
		((PagerAdapter)binding.viewpager.getAdapter()).notifyDataSetChanged();
		final android.widget.LinearLayout miniPlayer = findViewById(R.id.mini_player);
		final android.widget.TextView txtSong = findViewById(R.id.txt_song);
		final android.widget.TextView txtArtist = findViewById(R.id.txt_artist);
		final android.widget.ImageView imgCover = findViewById(R.id.img_cover);
		final android.widget.ImageView btnDown = findViewById(R.id.btn_down);
		final android.widget.ImageView btnPlay = findViewById(R.id.btn_play);
		
		if (btnPlay != null) {
			boolean isPlayingInit = com.mflow.music.MusicService.mediaPlayer != null && com.mflow.music.MusicService.mediaPlayer.isPlaying();
			btnPlay.setImageResource(isPlayingInit ? R.drawable.icon_pause : R.drawable.icon_play);
		}
		
		final Runnable updateMiniPlayerLogic = () -> {
			if (HomeworkActivity.this.isFinishing() || (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1 && HomeworkActivity.this.isDestroyed())) {
				return;
			}
			
			int index = com.mflow.music.MusicService.currentIndex;
			java.util.ArrayList<java.util.HashMap<String, Object>> list = com.mflow.music.MusicService.songList;
			
			if (list != null && index >= 0 && index < list.size()) {
				
				if (miniPlayer != null) {
					miniPlayer.setVisibility(android.view.View.VISIBLE);
				}
				
				java.util.HashMap<String, Object> song = list.get(index);
				
				if (txtSong != null) {
					Object titleObj = song.get("title");
					txtSong.setText(titleObj != null ? titleObj.toString() : "");
				}
				
				if (txtArtist != null) {
					Object artistObj = song.get("artist");
					txtArtist.setText(artistObj != null ? artistObj.toString() : "");
				}
				
				if (imgCover != null) {
					long albumId = -1;
					if (song.get("album_id") != null) {
						try {
							albumId = Long.parseLong(song.get("album_id").toString());
						} catch (Exception ignored) {}
					}
					
					com.mflow.music.ImageLoader.loadAlbumArt(
					imgCover.getContext(),
					albumId,
					imgCover,
					R.drawable.default_music_image
					);
				}
				
				if (btnPlay != null) {
					boolean isPlaying = com.mflow.music.MusicService.mediaPlayer != null && com.mflow.music.MusicService.mediaPlayer.isPlaying();
					int targetRes = isPlaying ? R.drawable.icon_pause : R.drawable.icon_play;
					btnPlay.setImageResource(targetRes);
				}
				
			} else {
				if (miniPlayer != null) {
					miniPlayer.setVisibility(android.view.View.GONE);
				}
			}
		};
		
		android.content.IntentFilter filter = new android.content.IntentFilter("MFLOW_UPDATE_PLAYER_UI");
		
		final android.content.BroadcastReceiver miniReceiver = new android.content.BroadcastReceiver() {
			@Override
			public void onReceive(android.content.Context context, android.content.Intent intent) {
				updateMiniPlayerLogic.run();
			}
		};
		
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
			registerReceiver(miniReceiver, filter, android.content.Context.RECEIVER_EXPORTED);
		} else {
			registerReceiver(miniReceiver, filter);
		}
		
		if (btnDown != null) {
			btnDown.setOnClickListener(v -> {
				android.content.Intent dismissIntent = new android.content.Intent(HomeworkActivity.this, com.mflow.music.MusicService.class);
				dismissIntent.setAction(com.mflow.music.NotificationHelper.ACTION_DISMISS);
				startService(dismissIntent);
				
				com.mflow.music.MusicService.currentIndex = -1;
				if (miniPlayer != null) {
					miniPlayer.setVisibility(android.view.View.GONE);
				}
			});
		}
		
		if (btnPlay != null) {
			btnPlay.setOnClickListener(v -> {
				boolean isPlayingNow = com.mflow.music.MusicService.mediaPlayer != null && com.mflow.music.MusicService.mediaPlayer.isPlaying();
				
				btnPlay.setImageResource(isPlayingNow ? R.drawable.icon_play : R.drawable.icon_pause);
				
				android.content.Intent toggleIntent = new android.content.Intent(HomeworkActivity.this, com.mflow.music.MusicService.class);
				toggleIntent.setAction(com.mflow.music.NotificationHelper.ACTION_TOGGLE);
				startService(toggleIntent);
			});
		}
		
		if (miniPlayer != null) {
			miniPlayer.setOnClickListener(v -> {
				android.content.Intent openIntent = new android.content.Intent(HomeworkActivity.this, FullplayerActivity.class);
				startActivity(openIntent);
			});
		}
		
		getWindow().getDecorView().addOnAttachStateChangeListener(new android.view.View.OnAttachStateChangeListener() {
			@Override
			public void onViewAttachedToWindow(android.view.View v) {
				updateMiniPlayerLogic.run();
			}
			@Override
			public void onViewDetachedFromWindow(android.view.View v) {
				try { unregisterReceiver(miniReceiver); } catch (Exception ignored) {}
			}
		});
		
		updateMiniPlayerLogic.run();
		
		binding.txtTitle.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/bold.ttf"), 1);
		binding.txtSong.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/bold.ttf"), 1);
		binding.txtArtist.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/regular.ttf"), 0);
	}
	
	public class LinkFragmentAdapter extends FragmentStatePagerAdapter {
		// This class is deprecated, you should migrate to ViewPager2:
		// https://developer.android.com/reference/androidx/viewpager2/widget/ViewPager2
		Context context;
		int tabCount;
		
		public LinkFragmentAdapter(Context context, FragmentManager manager) {
			super(manager);
			this.context = context;
		}
		
		public void setTabCount(int tabCount) {
			this.tabCount = tabCount;
		}
		
		@Override
		public int getCount() {
			return tabCount;
		}
		
		@Override
		public CharSequence getPageTitle(int _position) {
			return "";
		}
		
		
		@Override
		public Fragment getItem(int _position) {
			if (_position == 0) {
				return new HomeFragmentActivity();
			}
			if (_position == 1) {
				return new FavFragmentActivity();
			}
			if (_position == 2) {
				return new LibraryFragmentActivity();
			}
			return new Fragment();
		}
		
	}
	
}
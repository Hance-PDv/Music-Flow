package com.mflow.music;

import android.animation.*;
import android.app.*;
import android.content.*;
import android.content.res.*;
import android.graphics.*;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.media.*;
import android.net.*;
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
import androidx.lifecycle.runtime.*;
import com.airbnb.lottie.*;
import com.bumptech.glide.*;
import com.bumptech.glide.gifdecoder.*;
import com.mflow.music.databinding.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.*;
import org.json.*;

public class FullplayerActivity extends AppCompatActivity {
	
	private FullplayerBinding binding;
	
	private ArrayList<HashMap<String, Object>> list_play = new ArrayList<>();
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		binding = FullplayerBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());
		initialize(_savedInstanceState);
		initializeLogic();
	}
	
	private void initialize(Bundle _savedInstanceState) {
		
		binding.imgBack.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				finish();
			}
		});
	}
	
	private void initializeLogic() {
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
		
		binding.play.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)90, 0xFF60A5FA));
		binding.titlef.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/bold.ttf"), 1);
		binding.artf.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/regular.ttf"), 0);
		binding.pros.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/regular.ttf"), 0);
		binding.time.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/regular.ttf"), 0);
		binding.text1.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/regular.ttf"), 1);
		final android.widget.ImageView btnPlay = (android.widget.ImageView) findViewById(R.id.play);
		final android.widget.ImageView btnRepeat = (android.widget.ImageView) findViewById(R.id.repeat);
		final android.widget.ImageView btnNext = (android.widget.ImageView) findViewById(R.id.next);
		final android.widget.ImageView btnBack = (android.widget.ImageView) findViewById(R.id.back);
		final android.widget.ImageView btnFave = (android.widget.ImageView) findViewById(R.id.fave); 
		final android.widget.SeekBar seekBar1 = (android.widget.SeekBar) findViewById(R.id.SeekBar1);
		final android.widget.TextView txtPros = (android.widget.TextView) findViewById(R.id.pros);
		final android.widget.TextView txtTime = (android.widget.TextView) findViewById(R.id.time);
		final android.widget.TextView txtTitle = (android.widget.TextView) findViewById(R.id.titlef);
		final android.widget.TextView txtArtist = (android.widget.TextView) findViewById(R.id.artf);
		final android.widget.ImageView imgAlbum = (android.widget.ImageView) findViewById(R.id.mAlbumImageView);
		
		final com.mflow.music.PlaybackStateManager stateManager = new com.mflow.music.PlaybackStateManager(this);
		final boolean[] isUserTracking = {false};
		final int[] lastTrackIndex = {-1};
		final java.util.concurrent.atomic.AtomicBoolean targetPlayingState = new java.util.concurrent.atomic.AtomicBoolean(false);
		final android.os.Handler seekHandler = new android.os.Handler(android.os.Looper.getMainLooper());
		
		final java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("mm:ss", java.util.Locale.getDefault());
		
		int initialRepeatMode = com.mflow.music.MusicService.repeatMode;
		if (initialRepeatMode == -1) {
			initialRepeatMode = stateManager.getRepeatMode();
			com.mflow.music.MusicService.repeatMode = initialRepeatMode;
		}
		
		if (btnRepeat != null) {
			btnRepeat.setImageResource(initialRepeatMode == 1 ? R.drawable.icon_repeat_one : R.drawable.icon_repeat);
		}
		
		final Runnable updateSeekBarRunnable = new Runnable() {
			@Override
			public void run() {
				try {
					if (!isUserTracking[0]) {
						long currentPos = (com.mflow.music.MusicService.mediaPlayer != null && com.mflow.music.MusicService.mediaPlayer.isPlaying())
						? com.mflow.music.MusicService.mediaPlayer.getCurrentPosition() 
						: (com.mflow.music.MusicService.lastSavedPosition > 0 ? com.mflow.music.MusicService.lastSavedPosition : stateManager.getLastPosition());
						
						seekBar1.setProgress((int) currentPos);
						if (txtPros != null) {
							txtPros.setText(timeFormat.format(new java.util.Date(Math.max(0, currentPos))));
						}
						
						if (com.mflow.music.MusicService.mediaPlayer != null && com.mflow.music.MusicService.mediaPlayer.isPlaying()) {
							seekHandler.removeCallbacks(this);
							seekHandler.postDelayed(this, 1000);
						}
					}
				} catch (Exception ignored) {}
			}
		};
		
		final Runnable syncPlayerUI = new Runnable() {
			@Override
			public void run() {
				try {
					int index = com.mflow.music.MusicService.currentIndex;
					
					if (index == -1) {
						index = stateManager.getLastIndex();
						com.mflow.music.MusicService.currentIndex = index;
					}
					
					// الدمج الذكي: فحص قائمة الخدمة النشطة أولاً (لضمان دعم المفضلات والقوائم المخصصة) ثم الكاش العام
					java.util.ArrayList<java.util.HashMap<String, Object>> list = com.mflow.music.MusicService.songList;
					if (list == null || list.isEmpty()) {
						list = com.mflow.music.MusicCache.getSongs(FullplayerActivity.this);
					}
					
					if (list != null && index >= 0 && index < list.size()) {
						java.util.HashMap<String, Object> song = list.get(index);
						
						// مقارنة إضافية عبر مسار الأغنية (path) لضمان عدم حدوث أي اختلاف في البيانات
						if (lastTrackIndex[0] != index) {
							lastTrackIndex[0] = index;
							
							String title = song.get("title") != null ? song.get("title").toString() : "";
							String artist = song.get("artist") != null ? song.get("artist").toString() : "";
							if (txtTitle != null) txtTitle.setText(title);
							if (txtArtist != null) txtArtist.setText(artist);
							
							if (imgAlbum != null) {
								long albumId = -1;
								if (song.get("album_id") != null) {
									try {
										albumId = Long.parseLong(song.get("album_id").toString());
									} catch (Exception ignored) {}
								}
								
								if (albumId > 0) {
									try {
										android.net.Uri sArtworkUri = android.content.ContentUris.withAppendedId(
										android.net.Uri.parse("content://media/external/audio/albumart"), 
										albumId
										);
										android.content.ContentResolver res = getContentResolver();
										java.io.InputStream in = res.openInputStream(sArtworkUri);
										if (in != null) {
											android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeStream(in);
											in.close();
											if (bmp != null) {
												imgAlbum.setImageBitmap(bmp);
											} else {
												imgAlbum.setImageResource(R.drawable.default_music_image);
											}
										} else {
											imgAlbum.setImageResource(R.drawable.default_music_image);
										}
									} catch (Exception e) {
										imgAlbum.setImageResource(R.drawable.default_music_image);
									}
								} else {
									imgAlbum.setImageResource(R.drawable.default_music_image);
								}
							}
							
							long totalDuration = 0;
							if (com.mflow.music.MusicService.mediaPlayer != null) {
								totalDuration = com.mflow.music.MusicService.mediaPlayer.getDuration();
							} else if (song.get("duration") != null) {
								try { totalDuration = Long.parseLong(song.get("duration").toString()); } catch (Exception ignored) {}
							}
							
							if (totalDuration > 0) {
								seekBar1.setMax((int) totalDuration);
								if (txtTime != null) {
									txtTime.setText(timeFormat.format(new java.util.Date(Math.max(0, totalDuration))));
								}
							}
							
							if (btnFave != null) {
								String currentPath = song.get("path") != null ? song.get("path").toString() : "";
								java.util.ArrayList<java.util.HashMap<String, Object>> favList = com.mflow.music.PrefsManager.getFavorites(FullplayerActivity.this);
								boolean isFav = false;
								
								if (!currentPath.isEmpty() && favList != null) {
									for (java.util.HashMap<String, Object> item : favList) {
										if (item.containsKey("path") && currentPath.equals(item.get("path").toString())) {
											isFav = true;
											break;
										}
									}
								}
								btnFave.setImageResource(isFav ? R.drawable.icon_fav1 : R.drawable.icon_fav);
							}
						}
						
						boolean isPlaying = (com.mflow.music.MusicService.mediaPlayer != null && com.mflow.music.MusicService.mediaPlayer.isPlaying()) || targetPlayingState.get();
						
						if (btnPlay != null) {
							btnPlay.setImageResource(isPlaying ? R.drawable.icon_pause : R.drawable.icon_play);
						}
						
						seekHandler.removeCallbacks(updateSeekBarRunnable);
						if (isPlaying) {
							seekHandler.post(updateSeekBarRunnable);
						} else if (!isUserTracking[0]) {
							long savedPos = com.mflow.music.MusicService.lastSavedPosition > 0 
							? com.mflow.music.MusicService.lastSavedPosition 
							: stateManager.getLastPosition();
							seekBar1.setProgress((int) savedPos);
							if (txtPros != null) {
								txtPros.setText(timeFormat.format(new java.util.Date(Math.max(0, savedPos))));
							}
						}
						
						if (btnRepeat != null) {
							btnRepeat.setImageResource(com.mflow.music.MusicService.repeatMode == 1 ? R.drawable.icon_repeat_one : R.drawable.icon_repeat);
						}
					}
				} catch (Exception ignored) {}
			}
		};
		
		try {
			targetPlayingState.set(com.mflow.music.MusicService.mediaPlayer != null && com.mflow.music.MusicService.mediaPlayer.isPlaying());
		} catch (Exception e) {
			targetPlayingState.set(false);
		}
		
		if (btnPlay != null) {
			btnPlay.setOnClickListener(new android.view.View.OnClickListener() {
				@Override
				public void onClick(android.view.View v) {
					boolean isPlayingNow = com.mflow.music.MusicService.mediaPlayer != null && com.mflow.music.MusicService.mediaPlayer.isPlaying();
					btnPlay.setImageResource(isPlayingNow ? R.drawable.icon_play : R.drawable.icon_pause);
					
					android.content.Intent intent = new android.content.Intent(FullplayerActivity.this, com.mflow.music.MusicService.class);
					if (com.mflow.music.MusicService.mediaPlayer == null && com.mflow.music.MusicService.currentIndex != -1) {
						intent.setAction("ACTION_PLAY");
						intent.putExtra("index", com.mflow.music.MusicService.currentIndex);
					} else {
						intent.setAction("ACTION_TOGGLE_PLAY");
					}
					startService(intent);
				}
			});
		}
		
		if (btnNext != null) {
			btnNext.setOnClickListener(new android.view.View.OnClickListener() {
				@Override
				public void onClick(android.view.View v) {
					seekHandler.removeCallbacks(updateSeekBarRunnable);
					android.content.Intent intent = new android.content.Intent(FullplayerActivity.this, com.mflow.music.MusicService.class);
					intent.setAction("ACTION_NEXT");
					startService(intent);
				}
			});
		}
		
		if (btnBack != null) {
			btnBack.setOnClickListener(new android.view.View.OnClickListener() {
				@Override
				public void onClick(android.view.View v) {
					seekHandler.removeCallbacks(updateSeekBarRunnable);
					android.content.Intent intent = new android.content.Intent(FullplayerActivity.this, com.mflow.music.MusicService.class);
					intent.setAction("ACTION_PREV");
					startService(intent);
				}
			});
		}
		
		if (btnRepeat != null) {
			btnRepeat.setOnClickListener(new android.view.View.OnClickListener() {
				@Override
				public void onClick(android.view.View v) {
					android.content.Intent intent = new android.content.Intent(FullplayerActivity.this, com.mflow.music.MusicService.class);
					intent.setAction("ACTION_TOGGLE_REPEAT");
					startService(intent);
				}
			});
		}
		
		if (btnFave != null) {
			btnFave.setOnClickListener(new android.view.View.OnClickListener() {
				@Override
				public void onClick(android.view.View v) {
					java.util.ArrayList<java.util.HashMap<String, Object>> list = com.mflow.music.MusicService.songList;
					if (list == null || list.isEmpty()) {
						list = com.mflow.music.MusicCache.getSongs(FullplayerActivity.this);
					}
					int idx = com.mflow.music.MusicService.currentIndex;
					if (list == null || idx < 0 || idx >= list.size()) {
						android.widget.Toast.makeText(getApplicationContext(), "No song playing", android.widget.Toast.LENGTH_SHORT).show();
						return;
					}
					
					java.util.HashMap<String, Object> song = list.get(idx);
					String currentPath = song.get("path") != null ? song.get("path").toString() : "";
					java.util.ArrayList<java.util.HashMap<String, Object>> favList = com.mflow.music.PrefsManager.getFavorites(FullplayerActivity.this);
					boolean isFav = false;
					
					if (!currentPath.isEmpty() && favList != null) {
						for (java.util.HashMap<String, Object> item : favList) {
							if (item.containsKey("path") && currentPath.equals(item.get("path").toString())) {
								isFav = true;
								break;
							}
						}
					}
					btnFave.setImageResource(!isFav ? R.drawable.icon_fav1 : R.drawable.icon_fav);
					
					android.content.Intent intent = new android.content.Intent(FullplayerActivity.this, com.mflow.music.MusicService.class);
					intent.setAction("ACTION_TOGGLE_FAV");
					startService(intent);
				}
			});
		}
		
		seekBar1.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
				if (fromUser && txtPros != null) {
					txtPros.setText(timeFormat.format(new java.util.Date(Math.max(0, progress))));
				}
			}
			
			@Override
			public void onStartTrackingTouch(android.widget.SeekBar seekBar) {
				isUserTracking[0] = true;
				seekHandler.removeCallbacks(updateSeekBarRunnable);
			}
			
			@Override
			public void onStopTrackingTouch(android.widget.SeekBar seekBar) {
				isUserTracking[0] = false;
				long newPos = seekBar.getProgress();
				com.mflow.music.MusicService.lastSavedPosition = newPos;
				
				if (com.mflow.music.MusicService.mediaPlayer != null) {
					try {
						com.mflow.music.MusicService.mediaPlayer.seekTo((int) newPos);
					} catch (Exception ignored) {}
					if (com.mflow.music.MusicService.mediaPlayer.isPlaying()) {
						seekHandler.removeCallbacks(updateSeekBarRunnable);
						seekHandler.post(updateSeekBarRunnable);
					}
				}
				stateManager.savePlaybackState(com.mflow.music.MusicService.currentIndex, (int) newPos);
			}
		});
		
		android.content.IntentFilter filter = new android.content.IntentFilter("MFLOW_UPDATE_PLAYER_UI");
		
		final android.content.BroadcastReceiver playerReceiver = new android.content.BroadcastReceiver() {
			@Override
			public void onReceive(android.content.Context context, android.content.Intent intent) {
				if (intent != null && intent.hasExtra("is_playing")) {
					targetPlayingState.set(intent.getBooleanExtra("is_playing", false));
				} else {
					try {
						targetPlayingState.set(com.mflow.music.MusicService.mediaPlayer != null && com.mflow.music.MusicService.mediaPlayer.isPlaying());
					} catch (Exception e) {
						targetPlayingState.set(false);
					}
				}
				syncPlayerUI.run();
			}
		};
		
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
			registerReceiver(playerReceiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED);
		} else {
			registerReceiver(playerReceiver, filter);
		}
		
		getWindow().getDecorView().addOnAttachStateChangeListener(new android.view.View.OnAttachStateChangeListener() {
			@Override
			public void onViewAttachedToWindow(android.view.View v) {
				syncPlayerUI.run();
			}
			@Override
			public void onViewDetachedFromWindow(android.view.View v) {
				try { unregisterReceiver(playerReceiver); } catch (Exception ignored) {}
				seekHandler.removeCallbacksAndMessages(null);
			}
		});
		
		syncPlayerUI.run();
		
		final View sleep = findViewById(R.id.sleep);
		final TextView text1 = findViewById(R.id.text1);
		final View decorView = getWindow().getDecorView();
		
		// Handler مستقل ودائم في الشاشة الرئيسية لتحديث text1 وأيقونة النوم باستمرار (حتى لو الديالوج مغلق)
		final android.os.Handler mainActivityHandler = new android.os.Handler(android.os.Looper.getMainLooper());
		final Runnable mainActivityTick = new Runnable() {
			@Override
			public void run() {
				boolean isActive = MusicService.sleepManager != null && MusicService.sleepManager.isActive();
				int iconRes = isActive ? R.drawable.icon_night : R.drawable.icon_night1;
				
				if (sleep != null) {
					if (sleep instanceof android.widget.ImageView) {
						((android.widget.ImageView) sleep).setImageResource(iconRes);
					} else if (sleep instanceof android.widget.ImageButton) {
						((android.widget.ImageButton) sleep).setImageResource(iconRes);
					} else {
						sleep.setBackgroundResource(iconRes);
					}
				}
				
				if (isActive) {
					long rem = MusicService.sleepManager.getRemainingMillis();
					if (rem > 0) {
						long minutesRem = (rem / 1000) / 60;
						long secondsRem = (rem / 1000) % 60;
						if (text1 != null) {
							text1.setText(String.format("%02d:%02d", minutesRem, secondsRem));
						}
					} else {
						if (text1 != null) text1.setText("");
					}
				} else {
					if (text1 != null) text1.setText("");
				}
				
				mainActivityHandler.postDelayed(this, 1000);
			}
		};
		
		// بدء عمل العداد الدائم للشاشة الرئيسية
		mainActivityHandler.post(mainActivityTick);
		
		// إيقاف العداد عند تدمير النشاط لمنع تسريب الذاكرة
		decorView.addOnAttachStateChangeListener(new android.view.View.OnAttachStateChangeListener() {
			@Override
			public void onViewAttachedToWindow(android.view.View v) {}
			@Override
			public void onViewDetachedFromWindow(android.view.View v) {
				mainActivityHandler.removeCallbacks(mainActivityTick);
			}
		});
		
		if (sleep != null) {
			sleep.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					final com.google.android.material.bottomsheet.BottomSheetDialog lyricsDialog = 
					new com.google.android.material.bottomsheet.BottomSheetDialog(FullplayerActivity.this);
					
					final View dialogView = getLayoutInflater().inflate(R.layout.dialog_sleep_timer, null);
					if (dialogView == null) return;
					
					lyricsDialog.setContentView(dialogView);
					lyricsDialog.setCanceledOnTouchOutside(true);
					
					lyricsDialog.setOnShowListener(new android.content.DialogInterface.OnShowListener() {
						@Override
						public void onShow(android.content.DialogInterface dialogInterface) {
							com.google.android.material.bottomsheet.BottomSheetDialog d = 
							(com.google.android.material.bottomsheet.BottomSheetDialog) dialogInterface;
							final android.widget.FrameLayout bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
							if (bottomSheet != null) {
								bottomSheet.setBackgroundResource(android.R.color.transparent);
								
								final com.google.android.material.bottomsheet.BottomSheetBehavior<android.widget.FrameLayout> behavior = 
								com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet);
								
								behavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
								behavior.setSkipCollapsed(true);
								behavior.setDraggable(false);
								
								View btnDismiss = dialogView.findViewById(R.id.btn_dismiss_lyrics);
								if (btnDismiss != null) {
									btnDismiss.setOnTouchListener(new View.OnTouchListener() {
										private float initialTouchY;
										
										@Override
										public boolean onTouch(View view, android.view.MotionEvent event) {
											switch (event.getAction()) {
												case android.view.MotionEvent.ACTION_DOWN:
												initialTouchY = event.getRawY();
												return true;
												
												case android.view.MotionEvent.ACTION_MOVE:
												float deltaY = event.getRawY() - initialTouchY;
												if (deltaY > 0) {
													bottomSheet.setTranslationY(deltaY);
												}
												return true;
												
												case android.view.MotionEvent.ACTION_UP:
												case android.view.MotionEvent.ACTION_CANCEL:
												float movedY = event.getRawY() - initialTouchY;
												if (movedY > bottomSheet.getHeight() * 0.25f) {
													lyricsDialog.dismiss();
												} else {
													bottomSheet.animate()
													.translationY(0)
													.setDuration(200)
													.start();
												}
												return true;
											}
											return false;
										}
									});
								}
							}
						}
					});
					
					final LinearLayout layoutSetup = dialogView.findViewById(R.id.layout_setup);
					final LinearLayout layoutRunningDisplay = dialogView.findViewById(R.id.layout_running_display);
					final LinearLayout layoutSwitchContainer = dialogView.findViewById(R.id.layout_switch_container);
					final LinearLayout layoutSetupButtons = dialogView.findViewById(R.id.layout_setup_buttons);
					final LinearLayout layoutRunningButtons = dialogView.findViewById(R.id.layout_running_buttons);
					
					final TextView tvStopsAt = dialogView.findViewById(R.id.tv_stops_at);
					final TextView tvStopsAtRunning = dialogView.findViewById(R.id.tv_stops_at_running);
					final TextView tvSelectedMinutes = dialogView.findViewById(R.id.tv_selected_minutes);
					final TextView tvDialogCountdown = dialogView.findViewById(R.id.tv_dialog_countdown);
					
					final View sleepSlider = dialogView.findViewById(R.id.sleepSlider);
					final Switch switchFinishSong = dialogView.findViewById(R.id.switch_Finish_current_song);
					
					final android.content.SharedPreferences prefs = getSharedPreferences("sleep_timer_prefs", MODE_PRIVATE);
					boolean isFinishSongSaved = prefs.getBoolean("finish_current_song", false);
					
					if (switchFinishSong != null) {
						switchFinishSong.setChecked(isFinishSongSaved);
						switchFinishSong.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
							@Override
							public void onCheckedChanged(android.widget.CompoundButton buttonView, boolean isChecked) {
								prefs.edit().putBoolean("finish_current_song", isChecked).apply();
							}
						});
					}
					
					View btnCancel = dialogView.findViewById(R.id.btn_cancel);
					View btnStart = dialogView.findViewById(R.id.btn_start);
					View btnStop = dialogView.findViewById(R.id.btn_stop);
					View btnDone = dialogView.findViewById(R.id.btn_done);
					View btnMinus5Card = dialogView.findViewById(R.id.btn_minus_5_card);
					View btnPlus5Card = dialogView.findViewById(R.id.btn_plus_5_card);
					
					final long maxMillis = 60 * 60 * 1000L;
					final long minMillis = 1 * 60 * 1000L;
					final long[] selectedMillis = {15 * 60 * 1000L};
					
					final boolean isSleepActive = MusicService.sleepManager != null && MusicService.sleepManager.isActive();
					
					if (isSleepActive) {
						long savedInitialDuration = prefs.getLong("initial_timer_duration", 0L);
						long currentRem = MusicService.sleepManager.getRemainingMillis();
						if (savedInitialDuration < currentRem || savedInitialDuration == 0L) {
							savedInitialDuration = currentRem;
							prefs.edit().putLong("initial_timer_duration", savedInitialDuration).apply();
						}
					}
					
					// Handler الخاص بالديالوج مع الفحص التلقائي للانتقال عند انتهاء الوقت
					final android.os.Handler dialogHandler = new android.os.Handler(android.os.Looper.getMainLooper());
					final Runnable dialogTickRunnable = new Runnable() {
						@Override
						public void run() {
							boolean active = MusicService.sleepManager != null && MusicService.sleepManager.isActive();
							
							// التحقق التلقائي إذا انتهى الوقت أثناء فتح الديالوج للتحويل لشاشة الضبط
							if (!active) {
								if (layoutSetup != null && layoutSetup.getVisibility() != View.VISIBLE) {
									layoutSetup.setVisibility(View.VISIBLE);
									if (layoutRunningDisplay != null) layoutRunningDisplay.setVisibility(View.GONE);
									if (layoutSwitchContainer != null) layoutSwitchContainer.setVisibility(View.VISIBLE);
									if (layoutSetupButtons != null) layoutSetupButtons.setVisibility(View.VISIBLE);
									if (layoutRunningButtons != null) layoutRunningButtons.setVisibility(View.GONE);
								}
							}
							
							long duration = active ? MusicService.sleepManager.getRemainingMillis() : selectedMillis[0];
							long targetTime = System.currentTimeMillis() + duration;
							java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
							String formatted = sdf.format(new java.util.Date(targetTime));
							
							if (active) {
								long rem = MusicService.sleepManager.getRemainingMillis();
								if (rem < 0) rem = 0;
								long minRem = (rem / 1000) / 60;
								long secRem = (rem / 1000) % 60;
								String timeStr = String.format("%02d:%02d", minRem, secRem);
								
								if (tvDialogCountdown != null) tvDialogCountdown.setText(timeStr);
								if (tvStopsAtRunning != null) tvStopsAtRunning.setText("STOPS AT " + formatted);
							} else {
								if (tvStopsAt != null) tvStopsAt.setText("STOPS AT " + formatted);
							}
							
							dialogHandler.postDelayed(this, 1000);
						}
					};
					
					if (sleepSlider instanceof com.google.android.material.slider.Slider) {
						((com.google.android.material.slider.Slider) sleepSlider).addOnChangeListener(new com.google.android.material.slider.Slider.OnChangeListener() {
							@Override
							public void onValueChange(com.google.android.material.slider.Slider slider, float value, boolean fromUser) {
								int minutes = (int) value;
								selectedMillis[0] = minutes * 60 * 1000L;
								if (tvSelectedMinutes != null) {
									tvSelectedMinutes.setText(minutes + " min");
								}
							}
						});
					}
					
					lyricsDialog.show();
					dialogHandler.post(dialogTickRunnable);
					
					lyricsDialog.setOnDismissListener(new android.content.DialogInterface.OnDismissListener() {
						@Override
						public void onDismiss(android.content.DialogInterface dialogInterface) {
							dialogHandler.removeCallbacks(dialogTickRunnable);
						}
					});
					
					// ضبط حالة العرض الأولية للديالوج
					if (isSleepActive) {
						if (layoutSetup != null) layoutSetup.setVisibility(View.GONE);
						if (layoutRunningDisplay != null) layoutRunningDisplay.setVisibility(View.VISIBLE);
						if (layoutSwitchContainer != null) layoutSwitchContainer.setVisibility(View.GONE);
						if (layoutSetupButtons != null) layoutSetupButtons.setVisibility(View.GONE);
						if (layoutRunningButtons != null) layoutRunningButtons.setVisibility(View.VISIBLE);
					} else {
						if (layoutSetup != null) layoutSetup.setVisibility(View.VISIBLE);
						if (layoutRunningDisplay != null) layoutRunningDisplay.setVisibility(View.GONE);
						if (layoutSwitchContainer != null) layoutSwitchContainer.setVisibility(View.VISIBLE);
						if (layoutSetupButtons != null) layoutSetupButtons.setVisibility(View.VISIBLE);
						if (layoutRunningButtons != null) layoutRunningButtons.setVisibility(View.GONE);
					}
					
					if (btnStart != null) {
						btnStart.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								if (layoutSetup != null) layoutSetup.setVisibility(View.GONE);
								if (layoutRunningDisplay != null) layoutRunningDisplay.setVisibility(View.VISIBLE);
								if (layoutSwitchContainer != null) layoutSwitchContainer.setVisibility(View.GONE);
								if (layoutSetupButtons != null) layoutSetupButtons.setVisibility(View.GONE);
								if (layoutRunningButtons != null) layoutRunningButtons.setVisibility(View.VISIBLE);
								
								prefs.edit().putLong("initial_timer_duration", selectedMillis[0]).apply();
								boolean finishSong = switchFinishSong != null && switchFinishSong.isChecked();
								
								Intent intent = new Intent(FullplayerActivity.this, MusicService.class);
								intent.setAction(MusicService.ACTION_START_SLEEP);
								intent.putExtra("duration", selectedMillis[0]);
								intent.putExtra("finish_current_song", finishSong);
								startService(intent);
								
								dialogHandler.post(dialogTickRunnable);
							}
						});
					}
					
					if (btnPlus5Card != null) {
						btnPlus5Card.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								long currentRemaining = (MusicService.sleepManager != null && MusicService.sleepManager.isActive() && MusicService.sleepManager.getRemainingMillis() > 0)
								? MusicService.sleepManager.getRemainingMillis()
								: selectedMillis[0];
								long newTime = currentRemaining + (5 * 60 * 1000L);
								if (newTime > maxMillis) newTime = maxMillis;
								
								prefs.edit().putLong("initial_timer_duration", newTime).apply();
								boolean finishSong = switchFinishSong != null && switchFinishSong.isChecked();
								
								Intent intent = new Intent(FullplayerActivity.this, MusicService.class);
								intent.setAction(MusicService.ACTION_START_SLEEP);
								intent.putExtra("duration", newTime);
								intent.putExtra("finish_current_song", finishSong);
								startService(intent);
								
								dialogHandler.post(dialogTickRunnable);
							}
						});
					}
					
					if (btnMinus5Card != null) {
						btnMinus5Card.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								long currentRemaining = (MusicService.sleepManager != null && MusicService.sleepManager.isActive() && MusicService.sleepManager.getRemainingMillis() > 0)
								? MusicService.sleepManager.getRemainingMillis()
								: selectedMillis[0];
								long newTime = currentRemaining - (5 * 60 * 1000L);
								if (newTime < minMillis) newTime = minMillis;
								
								prefs.edit().putLong("initial_timer_duration", newTime).apply();
								boolean finishSong = switchFinishSong != null && switchFinishSong.isChecked();
								
								Intent intent = new Intent(FullplayerActivity.this, MusicService.class);
								intent.setAction(MusicService.ACTION_START_SLEEP);
								intent.putExtra("duration", newTime);
								intent.putExtra("finish_current_song", finishSong);
								startService(intent);
								
								dialogHandler.post(dialogTickRunnable);
							}
						});
					}
					
					if (btnStop != null) {
						btnStop.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								Intent intent = new Intent(FullplayerActivity.this, MusicService.class);
								intent.setAction(MusicService.ACTION_STOP_SLEEP);
								startService(intent);
								
								prefs.edit().remove("initial_timer_duration").apply();
								
								if (text1 != null) text1.setText("");
								
								if (sleep instanceof android.widget.ImageView) {
									((android.widget.ImageView) sleep).setImageResource(R.drawable.icon_night1);
								} else if (sleep instanceof android.widget.ImageButton) {
									((android.widget.ImageButton) sleep).setImageResource(R.drawable.icon_night1);
								} else if (sleep != null) {
									sleep.setBackgroundResource(R.drawable.icon_night1);
								}
								
								dialogHandler.removeCallbacks(dialogTickRunnable);
								lyricsDialog.dismiss();
							}
						});
					}
					
					if (btnCancel != null) {
						btnCancel.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								dialogHandler.removeCallbacks(dialogTickRunnable);
								lyricsDialog.dismiss();
							}
						});
					}
					
					if (btnDone != null) {
						btnDone.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								dialogHandler.removeCallbacks(dialogTickRunnable);
								lyricsDialog.dismiss();
							}
						});
					}
				}
			});
		}
		
		android.view.View btnLyrics = findViewById(R.id.btn_lyrics);
		if (btnLyrics != null) {
			btnLyrics.setOnClickListener(new android.view.View.OnClickListener() {
				@Override
				public void onClick(android.view.View v) {
					if (com.mflow.music.MusicService.songList == null || com.mflow.music.MusicService.currentIndex < 0) {
						return;
					}
					
					final com.google.android.material.bottomsheet.BottomSheetDialog lyricsDialog = 
					new com.google.android.material.bottomsheet.BottomSheetDialog(FullplayerActivity.this);
					
					final android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_lyrics, null);
					lyricsDialog.setContentView(dialogView);
					
					lyricsDialog.setCanceledOnTouchOutside(true);
					
					lyricsDialog.setOnShowListener(new android.content.DialogInterface.OnShowListener() {
						@Override
						public void onShow(android.content.DialogInterface dialogInterface) {
							com.google.android.material.bottomsheet.BottomSheetDialog d = 
							(com.google.android.material.bottomsheet.BottomSheetDialog) dialogInterface;
							final android.widget.FrameLayout bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
							if (bottomSheet != null) {
								bottomSheet.setBackgroundResource(android.R.color.transparent);
								
								final com.google.android.material.bottomsheet.BottomSheetBehavior<android.widget.FrameLayout> behavior = 
								com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet);
								
								int displayHeight = getResources().getDisplayMetrics().heightPixels;
								final int targetHeight = (int) (displayHeight * 0.75);
								
								android.view.ViewGroup.LayoutParams layoutParams = bottomSheet.getLayoutParams();
								layoutParams.height = targetHeight;
								bottomSheet.setLayoutParams(layoutParams);
								
								behavior.setPeekHeight(targetHeight);
								behavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
								behavior.setSkipCollapsed(true);
								behavior.setDraggable(false);
								
								android.view.View btnDismiss = dialogView.findViewById(R.id.btn_dismiss_lyrics);
								if (btnDismiss != null) {
									btnDismiss.setOnTouchListener(new android.view.View.OnTouchListener() {
										private float initialTouchY;
										private float initialTranslationY;
										
										@Override
										public boolean onTouch(android.view.View view, android.view.MotionEvent event) {
											switch (event.getAction()) {
												case android.view.MotionEvent.ACTION_DOWN:
												initialTouchY = event.getRawY();
												initialTranslationY = bottomSheet.getTranslationY();
												return true;
												
												case android.view.MotionEvent.ACTION_MOVE:
												float deltaY = event.getRawY() - initialTouchY;
												if (deltaY > 0) {
													bottomSheet.setTranslationY(initialTranslationY + deltaY);
												}
												return true;
												
												case android.view.MotionEvent.ACTION_UP:
												case android.view.MotionEvent.ACTION_CANCEL:
												float movedY = event.getRawY() - initialTouchY;
												if (movedY > targetHeight * 0.25f) {
													lyricsDialog.dismiss();
												} else {
													bottomSheet.animate()
													.translationY(0)
													.setDuration(200)
													.start();
												}
												return true;
											}
											return false;
										}
									});
								}
							}
						}
					});
					
					final android.widget.LinearLayout lyricsContainer = dialogView.findViewById(R.id.lyrics_container);
					final com.airbnb.lottie.LottieAnimationView lottieLoading = dialogView.findViewById(R.id.lottie_loading);
					
					android.graphics.Typeface customBoldFont = null;
					try {
						customBoldFont = android.graphics.Typeface.createFromAsset(getAssets(), "fonts/bold.ttf");
					} catch (Exception ignored) {
						customBoldFont = android.graphics.Typeface.DEFAULT_BOLD;
					}
					
					final android.graphics.Typeface finalFont = customBoldFont;
					
					final class NativeLrcView extends android.view.View {
						private java.util.List<com.mflow.music.LyricsFetcher.LyricLine> lines = new java.util.ArrayList<>();
						private android.text.StaticLayout[] normalLayouts = new android.text.StaticLayout[0];
						private android.text.StaticLayout[] currentLayouts = new android.text.StaticLayout[0];
						private float[] lineYOffsets = new float[0];
						private int currentIndex = -1;
						
						private final android.text.TextPaint normalPaint = new android.text.TextPaint(android.graphics.Paint.ANTI_ALIAS_FLAG);
						private final android.text.TextPaint currentPaint = new android.text.TextPaint(android.graphics.Paint.ANTI_ALIAS_FLAG);
						
						private final android.widget.OverScroller scroller;
						
						private final float lineGap;
						private final float horizontalPadding;
						
						public NativeLrcView(android.content.Context context) {
							super(context);
							scroller = new android.widget.OverScroller(context, new androidx.interpolator.view.animation.FastOutSlowInInterpolator());
							
							float scaledDensity = getResources().getDisplayMetrics().scaledDensity;
							float density = getResources().getDisplayMetrics().density;
							
							setVerticalFadingEdgeEnabled(true);
							setFadingEdgeLength((int) (100 * density));
							
							horizontalPadding = 48 * density;
							lineGap = 32 * density;
							
							// --- جلب ألوان Material 3 التكيفية للتنقل بوضوح تام بين النصوص الحالية وغير الفعالة ---
							// النص المحدد أو الحالي (الذي وصلت إليه الأغنية): يأخذ لون الـ Primary البارز والساطع ليطابق الهوية
							int currentTextColor = com.google.android.material.color.MaterialColors.getColor(
							context, com.google.android.material.R.attr.colorPrimary, android.graphics.Color.WHITE);
							
							// النصوص غير الفعالة: تأخذ لون colorOnSurfaceVariant بوضوح عالٍ وامتناع عن البهتان المزعج
							int normalTextColor = com.google.android.material.color.MaterialColors.getColor(
							context, com.google.android.material.R.attr.colorOnSurfaceVariant, android.graphics.Color.parseColor("#9CA3AF"));
							
							normalPaint.setTextSize(24 * scaledDensity);
							normalPaint.setColor(normalTextColor);
							if (finalFont != null) normalPaint.setTypeface(finalFont);
							
							currentPaint.setTextSize(24 * scaledDensity);
							currentPaint.setColor(currentTextColor);
							if (finalFont != null) currentPaint.setTypeface(finalFont);
						}
						
						@Override
						protected float getTopFadingEdgeStrength() {
							return 1.0f;
						}
						
						@Override
						protected float getBottomFadingEdgeStrength() {
							return 1.0f;
						}
						
						public void setLyrics(java.util.List<com.mflow.music.LyricsFetcher.LyricLine> lyrics) {
							this.lines = lyrics != null ? lyrics : new java.util.ArrayList<com.mflow.music.LyricsFetcher.LyricLine>();
							this.currentIndex = -1;
							prepareLayouts();
							scrollTo(0, 0);
							invalidate();
						}
						
						@Override
						protected void onSizeChanged(int w, int h, int oldw, int oldh) {
							super.onSizeChanged(w, h, oldw, oldh);
							prepareLayouts();
						}
						
						private void prepareLayouts() {
							if (lines.isEmpty() || getWidth() <= 0 || getHeight() <= 0) {
								normalLayouts = new android.text.StaticLayout[0];
								currentLayouts = new android.text.StaticLayout[0];
								lineYOffsets = new float[0];
								return;
							}
							
							int availableWidth = (int) (getWidth() - (horizontalPadding * 2));
							if (availableWidth <= 0) availableWidth = 100;
							
							int size = lines.size();
							normalLayouts = new android.text.StaticLayout[size];
							currentLayouts = new android.text.StaticLayout[size];
							lineYOffsets = new float[size];
							
							float firstLineHeight = 0;
							if (size > 0) {
								android.text.StaticLayout firstLayout = new android.text.StaticLayout(
								lines.get(0).text, currentPaint, availableWidth,
								android.text.Layout.Alignment.ALIGN_CENTER, 1.2f, 0.0f, false);
								firstLineHeight = firstLayout.getHeight();
							}
							
							float currentY = (getHeight() / 2f) - (firstLineHeight / 2f);
							
							for (int i = 0; i < size; i++) {
								String text = lines.get(i).text;
								
								normalLayouts[i] = new android.text.StaticLayout(
								text, normalPaint, availableWidth,
								android.text.Layout.Alignment.ALIGN_CENTER, 1.2f, 0.0f, false);
								
								currentLayouts[i] = new android.text.StaticLayout(
								text, currentPaint, availableWidth,
								android.text.Layout.Alignment.ALIGN_CENTER, 1.2f, 0.0f, false);
								
								lineYOffsets[i] = currentY;
								currentY += normalLayouts[i].getHeight() + lineGap;
							}
						}
						
						public void updateTime(long timeMs) {
							if (lines.isEmpty()) {
								return;
							}
							
							int newIdx = -1;
							for (int i = 0; i < lines.size(); i++) {
								if (timeMs >= lines.get(i).timeMs) {
									newIdx = i;
								} else {
									break;
								}
							}
							
							if (newIdx != currentIndex) {
								currentIndex = newIdx;
								if (currentIndex >= 0 && currentIndex < lineYOffsets.length && getHeight() > 0) {
									android.text.StaticLayout layout = currentLayouts[currentIndex];
									float lineCenter = lineYOffsets[currentIndex] + (layout.getHeight() / 2f);
									int targetY = (int) (lineCenter - (getHeight() / 2f));
									
									scroller.startScroll(0, getScrollY(), 0, targetY - getScrollY(), 400);
									postInvalidateOnAnimation();
								}
							}
						}
						
						@Override
						public void computeScroll() {
							if (scroller.computeScrollOffset()) {
								scrollTo(scroller.getCurrX(), scroller.getCurrY());
								postInvalidateOnAnimation();
							}
						}
						
						@Override
						protected void onDraw(android.graphics.Canvas canvas) {
							super.onDraw(canvas);
							
							int availableWidth = (int) (getWidth() - (horizontalPadding * 2));
							if (availableWidth <= 0) return;
							
							if (lines.isEmpty() || normalLayouts.length == 0) {
								android.text.StaticLayout emptyLayout = new android.text.StaticLayout(
								"No lyrics found", currentPaint, availableWidth,
								android.text.Layout.Alignment.ALIGN_CENTER, 1.0f, 0.0f, false);
								canvas.save();
								float centerY = (getHeight() - emptyLayout.getHeight()) / 2f;
								canvas.translate(horizontalPadding, centerY);
								emptyLayout.draw(canvas);
								canvas.restore();
								return;
							}
							
							for (int i = 0; i < lines.size(); i++) {
								if (i >= normalLayouts.length) break;
								
								boolean isCurrent = (i == currentIndex);
								android.text.StaticLayout layout = isCurrent ? currentLayouts[i] : normalLayouts[i];
								
								canvas.save();
								
								if (isCurrent) {
									float scale = 1.15f; 
									float pivotX = horizontalPadding + (layout.getWidth() / 2f);
									float pivotY = lineYOffsets[i] + (layout.getHeight() / 2f);
									
									canvas.scale(scale, scale, pivotX, pivotY);
								}
								
								canvas.translate(horizontalPadding, lineYOffsets[i]);
								layout.draw(canvas);
								canvas.restore();
							}
						}
						
						@Override
						public boolean onTouchEvent(android.view.MotionEvent event) {
							return false;
						}
					}
					
					final NativeLrcView nativeLrcView = new NativeLrcView(FullplayerActivity.this);
					if (lyricsContainer != null) {
						lyricsContainer.removeAllViews();
						lyricsContainer.addView(nativeLrcView, new android.widget.LinearLayout.LayoutParams(
						android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
						android.widget.LinearLayout.LayoutParams.MATCH_PARENT));
					}
					
					final int[] activeSongIdx = {-1};
					final Runnable loadLyricsForCurrentSong = new Runnable() {
						@Override
						public void run() {
							int currentIdx = com.mflow.music.MusicService.currentIndex;
							if (currentIdx < 0 || currentIdx >= com.mflow.music.MusicService.songList.size()) return;
							
							activeSongIdx[0] = currentIdx;
							java.util.HashMap<String, Object> currentSong = com.mflow.music.MusicService.songList.get(currentIdx);
							String title = currentSong.get("title") != null ? currentSong.get("title").toString() : "";
							String artist = currentSong.get("artist") != null ? currentSong.get("artist").toString() : "";
							
							long duration = 0;
							try {
								if (com.mflow.music.MusicService.mediaPlayer != null) {
									duration = com.mflow.music.MusicService.mediaPlayer.getDuration();
								} else if (currentSong.get("duration") != null) {
									duration = Long.parseLong(currentSong.get("duration").toString());
								}
							} catch (Exception e) {
								duration = 0;
							}
							
							nativeLrcView.setVisibility(android.view.View.INVISIBLE);
							
							com.mflow.music.LyricsFetcher.fetchLyrics(FullplayerActivity.this, title, artist, duration, new com.mflow.music.LyricsFetcher.OnLyricsLoadedListener() {
								@Override
								public void onLyricsLoaded(java.util.List<com.mflow.music.LyricsFetcher.LyricLine> lyricsList) {
									nativeLrcView.setLyrics(lyricsList);
									nativeLrcView.setVisibility(android.view.View.VISIBLE);
								}
							}, lottieLoading);
						}
					};
					
					loadLyricsForCurrentSong.run();
					
					final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
					final Runnable syncTask = new Runnable() {
						@Override
						public void run() {
							try {
								if (lyricsDialog.isShowing()) {
									if (activeSongIdx[0] != com.mflow.music.MusicService.currentIndex) {
										loadLyricsForCurrentSong.run();
									}
									
									if (com.mflow.music.MusicService.mediaPlayer != null) {
										long currentPos = com.mflow.music.MusicService.mediaPlayer.getCurrentPosition();
										nativeLrcView.updateTime(currentPos);
									}
									handler.postDelayed(this, 16);
								}
							} catch (Exception ignored) {}
						}
					};
					handler.post(syncTask);
					
					lyricsDialog.setOnDismissListener(new android.content.DialogInterface.OnDismissListener() {
						@Override
						public void onDismiss(android.content.DialogInterface dialog) {
							handler.removeCallbacks(syncTask);
						}
					});
					
					lyricsDialog.show();
				}
			});
		}
		
	}
	
}
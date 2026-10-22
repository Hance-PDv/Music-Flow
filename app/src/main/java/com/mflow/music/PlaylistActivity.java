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
import androidx.recyclerview.widget.*;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
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

public class PlaylistActivity extends AppCompatActivity {
	
	private PlaylistBinding binding;
	
	private ArrayList<HashMap<String, Object>> list_song = new ArrayList<>();
	
	private Intent intent = new Intent();
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		binding = PlaylistBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());
		initialize(_savedInstanceState);
		initializeLogic();
	}
	
	private void initialize(Bundle _savedInstanceState) {
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
		
		binding.txtPlaylistTitle.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/bold.ttf"), 1);
		binding.txtTrackCount.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/regular.ttf"), 0);
		final TextView txtPlaylistTitle = findViewById(R.id.txtPlaylistTitle);
		final TextView txtTrackCount = findViewById(R.id.txtTrackCount);
		ImageView btnBack = findViewById(R.id.btnBack);
		final androidx.recyclerview.widget.RecyclerView rvPlaylistSongs = findViewById(R.id.rvPlaylistSongs);
		
		final com.google.android.material.card.MaterialCardView btnDeletePlaylist = findViewById(R.id.btnDeletePlaylist);
		final ImageView imageview_delete = findViewById(R.id.imageview_delete);
		final CheckBox checkboxPlaylist = findViewById(R.id.checkboxPlaylist);
		final ImageView btn_remove_playlist = findViewById(R.id.btn_remove_playlist);
		
		if (imageview_delete != null) {
			if (imageview_delete.getTag() == null) {
				imageview_delete.setTag("inactive");
				imageview_delete.setImageResource(R.drawable.icon_delete);
			} else if ("active".equals(imageview_delete.getTag())) {
				imageview_delete.setImageResource(R.drawable.icon_close);
			} else {
				imageview_delete.setImageResource(R.drawable.icon_delete);
			}
		}
		
		if (btnDeletePlaylist != null && imageview_delete != null) {
			btnDeletePlaylist.setOnClickListener(v -> {
				if (list_song == null || list_song.isEmpty()) {
					return;
				}
				
				boolean isActive = "active".equals(imageview_delete.getTag());
				
				if (isActive) {
					imageview_delete.setTag("inactive");
					imageview_delete.setImageResource(R.drawable.icon_delete);
					
					if (btn_remove_playlist != null) {
						btn_remove_playlist.setVisibility(View.GONE);
					}
					
					if (checkboxPlaylist != null) {
						checkboxPlaylist.setChecked(false);
						checkboxPlaylist.setVisibility(View.GONE);
					}
				} else {
					imageview_delete.setTag("active");
					imageview_delete.setImageResource(R.drawable.icon_close);
					
					if (checkboxPlaylist != null) {
						checkboxPlaylist.setVisibility(View.VISIBLE);
					}
				}
				
				if (rvPlaylistSongs != null && rvPlaylistSongs.getAdapter() != null) {
					rvPlaylistSongs.getAdapter().notifyDataSetChanged();
				}
			});
		}
		
		if (checkboxPlaylist != null) {
			checkboxPlaylist.setOnCheckedChangeListener((buttonView, isChecked) -> {
				if (rvPlaylistSongs != null) {
					for (int i = 0; i < rvPlaylistSongs.getChildCount(); i++) {
						android.view.View child = rvPlaylistSongs.getChildAt(i);
						com.mflow.music.SongViewHolder2 childHolder = (com.mflow.music.SongViewHolder2) child.getTag();
						if (childHolder != null && childHolder.bottomremove2 != null) {
							childHolder.bottomremove2.setChecked(isChecked);
						}
					}
				}
			});
		}
		
		if (btn_remove_playlist != null) {
			btn_remove_playlist.setOnClickListener(v -> {
				try {
					String currentPlaylist = PlaylistManager.getSelectedPlaylistName();
					android.content.Context context = v.getContext();
					if (currentPlaylist != null && context != null && rvPlaylistSongs != null) {
						java.util.List<HashMap<String, Object>> songsToRemove = new java.util.ArrayList<>();
						for (int i = 0; i < rvPlaylistSongs.getChildCount(); i++) {
							android.view.View child = rvPlaylistSongs.getChildAt(i);
							com.mflow.music.SongViewHolder2 childHolder = (com.mflow.music.SongViewHolder2) child.getTag();
							if (childHolder != null && childHolder.bottomremove2 != null && childHolder.bottomremove2.isChecked()) {
								int adapterPos = rvPlaylistSongs.getChildAdapterPosition(child);
								if (adapterPos >= 0 && adapterPos < list_song.size()) {
									songsToRemove.add(list_song.get(adapterPos));
								}
							}
						}
						
						for (HashMap<String, Object> song : songsToRemove) {
							PlaylistManager.removeSongFromPlaylist(context, currentPlaylist, song);
						}
						
						java.util.ArrayList<HashMap<String, Object>> updatedList = PlaylistManager.getPlaylistSongs(context, currentPlaylist);
						list_song.clear();
						if (updatedList != null) {
							list_song.addAll(updatedList);
						}
						
						if (imageview_delete != null) {
							imageview_delete.setTag("inactive");
							imageview_delete.setImageResource(R.drawable.icon_delete);
						}
						if (checkboxPlaylist != null) {
							checkboxPlaylist.setChecked(false);
							checkboxPlaylist.setVisibility(View.GONE);
						}
						if (btn_remove_playlist != null) {
							btn_remove_playlist.setVisibility(View.GONE);
						}
						
						if (rvPlaylistSongs.getAdapter() != null) {
							rvPlaylistSongs.getAdapter().notifyDataSetChanged();
						}
						
						if (txtTrackCount != null) {
							txtTrackCount.setText(list_song.size() + " Tracks");
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
		}
		
		if (btnBack != null) {
			btnBack.setOnClickListener(v -> {
				if (imageview_delete != null) {
					imageview_delete.setTag("inactive");
				}
				finish();
			});
		}
		
		String playlistName = PlaylistManager.getSelectedPlaylistName();
		
		if (playlistName != null && !playlistName.trim().isEmpty()) {
			if (txtPlaylistTitle != null) {
				txtPlaylistTitle.setText(playlistName);
			}
			
			list_song.clear();
			list_song.addAll(PlaylistManager.getPlaylistSongs(getApplicationContext(), playlistName));
			
			if (txtTrackCount != null) {
				txtTrackCount.setText(list_song.size() + " Tracks");
			}
			
			if (rvPlaylistSongs != null && rvPlaylistSongs.getAdapter() != null) {
				rvPlaylistSongs.getAdapter().notifyDataSetChanged();
			}
		}
		
		com.google.android.material.card.MaterialCardView btnAddSongs = findViewById(R.id.btnAddSongs);
		
		if (btnAddSongs != null) {
			btnAddSongs.setOnClickListener(v -> {
				final String currentPlaylist = PlaylistManager.getSelectedPlaylistName();
				if (currentPlaylist == null || currentPlaylist.trim().isEmpty()) return;
				
				final ArrayList<HashMap<String, Object>> availableSongs = PlaylistManager.getAvailableSongs(getApplicationContext(), currentPlaylist);
				final ArrayList<HashMap<String, Object>> selectedSongsToAdd = new ArrayList<>();
				
				final com.google.android.material.bottomsheet.BottomSheetDialog addSongsDialog = 
				new com.google.android.material.bottomsheet.BottomSheetDialog(PlaylistActivity.this);
				
				final View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_songs, null);
				addSongsDialog.setContentView(dialogView);
				addSongsDialog.setCanceledOnTouchOutside(true);
				
				addSongsDialog.setOnShowListener(dialogInterface -> {
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
						
						View dragHandle = dialogView.findViewById(R.id.dialogDragHandle);
						if (dragHandle != null) {
							dragHandle.setOnTouchListener(new View.OnTouchListener() {
								private float initialTouchY;
								private float initialTranslationY;
								
								@Override
								public boolean onTouch(View view, android.view.MotionEvent event) {
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
											addSongsDialog.dismiss();
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
				});
				
				final androidx.recyclerview.widget.RecyclerView rvAddSongs = dialogView.findViewById(R.id.rvAddSongs);
				TextView txtEmptyDialog = dialogView.findViewById(R.id.txtEmptyDialog);
				Button btnCancelDialog = dialogView.findViewById(R.id.btnCancelDialog);
				Button btnDoneDialog = dialogView.findViewById(R.id.btnDoneDialog);
				
				final boolean[] selectedState = new boolean[availableSongs.size()];
				
				if (availableSongs.isEmpty()) {
					if (rvAddSongs != null) rvAddSongs.setVisibility(View.GONE);
					if (txtEmptyDialog != null) txtEmptyDialog.setVisibility(View.VISIBLE);
				} else {
					if (rvAddSongs != null) rvAddSongs.setVisibility(View.VISIBLE);
					if (txtEmptyDialog != null) txtEmptyDialog.setVisibility(View.GONE);
					
					rvAddSongs.setAdapter(new androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {
						
						@Override
						public androidx.recyclerview.widget.RecyclerView.ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
							View itemView = getLayoutInflater().inflate(R.layout.item_add_song, parent, false);
							return new androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {};
						}
						
						@Override
						public void onBindViewHolder(androidx.recyclerview.widget.RecyclerView.ViewHolder holder, final int position) {
							View itemView = holder.itemView;
							ImageView imgSongCover = itemView.findViewById(R.id.imgSongCover);
							TextView txtSongTitle = itemView.findViewById(R.id.txtSongTitle);
							TextView txtSongArtist = itemView.findViewById(R.id.txtSongArtist);
							final CheckBox chkSelectSong = itemView.findViewById(R.id.chkSelectSong);
							
							HashMap<String, Object> songMap = availableSongs.get(position);
							
							String title = songMap.containsKey("title") && songMap.get("title") != null ? songMap.get("title").toString() : "Unknown";
							if (txtSongTitle != null) txtSongTitle.setText(title);
							
							String artist = songMap.containsKey("artist") && songMap.get("artist") != null ? songMap.get("artist").toString() : "<unknown>";
							if (txtSongArtist != null) txtSongArtist.setText(artist);
							
							if (imgSongCover != null) {
								long albumId = -1;
								if (songMap.get("album_id") != null) {
									try {
										albumId = Long.parseLong(songMap.get("album_id").toString());
									} catch (Exception ignored) {}
								}
								
								ImageLoader.loadAlbumArt(PlaylistActivity.this, albumId, imgSongCover, R.drawable.default_music_image);
							}
							
							if (chkSelectSong != null) {
								chkSelectSong.setChecked(selectedState[position]);
							}
							
							itemView.setOnClickListener(v1 -> {
								selectedState[position] = !selectedState[position];
								if (chkSelectSong != null) {
									chkSelectSong.setChecked(selectedState[position]);
								}
							});
						}
						
						@Override
						public int getItemCount() {
							return availableSongs.size();
						}
					});
				}
				
				if (btnCancelDialog != null) {
					btnCancelDialog.setOnClickListener(v12 -> addSongsDialog.dismiss());
				}
				
				if (btnDoneDialog != null) {
					btnDoneDialog.setOnClickListener(v13 -> {
						selectedSongsToAdd.clear();
						for (int i = 0; i < availableSongs.size(); i++) {
							if (selectedState[i]) {
								selectedSongsToAdd.add(availableSongs.get(i));
							}
						}
						
						if (!selectedSongsToAdd.isEmpty()) {
							PlaylistManager.addSongsToPlaylist(getApplicationContext(), currentPlaylist, selectedSongsToAdd);
							
							list_song.clear();
							list_song.addAll(PlaylistManager.getPlaylistSongs(getApplicationContext(), currentPlaylist));
							
							if (rvPlaylistSongs != null && rvPlaylistSongs.getAdapter() != null) {
								rvPlaylistSongs.getAdapter().notifyDataSetChanged();
							}
							
							if (txtTrackCount != null) {
								txtTrackCount.setText(list_song.size() + " Tracks");
							}
						}
						
						addSongsDialog.dismiss();
					});
				}
				
				addSongsDialog.show();
			});
		}
		
		binding.rvPlaylistSongs.setAdapter(new RvPlaylistSongsAdapter(list_song));
		binding.rvPlaylistSongs.setLayoutManager(new LinearLayoutManager(this));
	}
	
	public class RvPlaylistSongsAdapter extends RecyclerView.Adapter<RvPlaylistSongsAdapter.ViewHolder> {
		
		ArrayList<HashMap<String, Object>> _data;
		
		public RvPlaylistSongsAdapter(ArrayList<HashMap<String, Object>> _arr) {
			_data = _arr;
		}
		
		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			LayoutInflater _inflater = getLayoutInflater();
			View _v = _inflater.inflate(R.layout.dome, null);
			RecyclerView.LayoutParams _lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			_v.setLayoutParams(_lp);
			return new ViewHolder(_v);
		}
		
		@Override
		public void onBindViewHolder(ViewHolder _holder, final int _position) {
			View _view = _holder.itemView;
			DomeBinding binding = DomeBinding.bind(_view);
			
			com.mflow.music.SongViewHolder2 holder = (com.mflow.music.SongViewHolder2) _view.getTag();
			if (holder == null) {
				holder = new com.mflow.music.SongViewHolder2(_view);
				_view.setTag(holder);
			}
			
			if (_position < 0 || _position >= list_song.size()) {
				return;
			}
			
			final HashMap<String, Object> map = list_song.get(_position);
			
			holder.title2.setText(map.containsKey("title") && map.get("title") != null ? map.get("title").toString() : "Unknown");
			holder.art2.setText(map.containsKey("artist") && map.get("artist") != null ? map.get("artist").toString() : "<unknown>");
			
			long albumId = -1;
			if (map.get("album_id") != null) {
				try {
					albumId = Long.parseLong(map.get("album_id").toString());
				} catch (Exception ignored) {}
			}
			
			com.mflow.music.ImageLoader.loadAlbumArt(
			_view.getContext(),
			albumId,
			holder.img_music2,
			R.drawable.default_music_image
			);
			
			android.util.TypedValue typedValue = new android.util.TypedValue();
			_view.getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true);
			holder.linearm2.setBackgroundResource(typedValue.resourceId);
			
			final int pos = _position;
			final com.mflow.music.SongViewHolder2 finalHolder = holder;
			
			holder.linearm2.setOnClickListener(v -> {
				try {
					android.content.Context context = v.getContext();
					if (context instanceof android.app.Activity) {
						android.app.Activity act = (android.app.Activity) context;
						android.widget.ImageView imgDeleteHeader = act.findViewById(R.id.imageview_delete);
						if (imgDeleteHeader != null && "active".equals(imgDeleteHeader.getTag())) {
							if (finalHolder.bottomremove2 != null) {
								finalHolder.bottomremove2.setChecked(!finalHolder.bottomremove2.isChecked());
							}
							return;
						}
					}
					
					if (pos >= 0 && pos < list_song.size()) {
						com.mflow.music.MusicService.songList = new java.util.ArrayList<>(list_song);
						com.mflow.music.MusicService.currentIndex = pos;
						
						if (context != null) {
							android.content.Intent playIntent = new android.content.Intent(context, com.mflow.music.MusicService.class);
							playIntent.setAction("ACTION_PLAY");
							playIntent.putExtra("index", pos);
							
							if (list_song.get(pos).containsKey("path")) {
								playIntent.putExtra("path", String.valueOf(list_song.get(pos).get("path")));
							}
							
							if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
								context.startForegroundService(playIntent);
							} else {
								context.startService(playIntent);
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
			
			if (_view.getContext() instanceof android.app.Activity) {
				android.app.Activity act = (android.app.Activity) _view.getContext();
				android.widget.ImageView imgDeleteHeader = act.findViewById(R.id.imageview_delete);
				if (imgDeleteHeader != null && "active".equals(imgDeleteHeader.getTag())) {
					holder.bottomremove2.setVisibility(android.view.View.VISIBLE);
				} else {
					holder.bottomremove2.setVisibility(android.view.View.GONE);
					holder.bottomremove2.setChecked(false);
				}
			} else {
				holder.bottomremove2.setVisibility(android.view.View.GONE);
			}
			
			holder.bottomremove2.setOnCheckedChangeListener(null);
			
			holder.bottomremove2.setOnCheckedChangeListener((buttonView, isChecked) -> {
				try {
					android.content.Context context = buttonView.getContext();
					if (context instanceof android.app.Activity) {
						android.app.Activity act = (android.app.Activity) context;
						androidx.recyclerview.widget.RecyclerView rv = act.findViewById(R.id.rvPlaylistSongs);
						android.widget.ImageView btnRemovePlaylist = act.findViewById(R.id.btn_remove_playlist);
						android.widget.CheckBox chkPlaylist = act.findViewById(R.id.checkboxPlaylist);
						
						if (rv != null && btnRemovePlaylist != null) {
							boolean anyChecked = false;
							boolean allChecked = true;
							int visibleCount = 0;
							
							for (int i = 0; i < rv.getChildCount(); i++) {
								android.view.View child = rv.getChildAt(i);
								com.mflow.music.SongViewHolder2 childHolder = (com.mflow.music.SongViewHolder2) child.getTag();
								if (childHolder != null && childHolder.bottomremove2 != null && childHolder.bottomremove2.getVisibility() == android.view.View.VISIBLE) {
									visibleCount++;
									if (childHolder.bottomremove2.isChecked()) {
										anyChecked = true;
									} else {
										allChecked = false;
									}
								}
							}
							
							if (anyChecked) {
								btnRemovePlaylist.setVisibility(android.view.View.VISIBLE);
							} else {
								btnRemovePlaylist.setVisibility(android.view.View.GONE);
							}
							
							if (chkPlaylist != null && visibleCount > 0) {
								final androidx.recyclerview.widget.RecyclerView finalRv = rv;
								chkPlaylist.setOnCheckedChangeListener(null);
								chkPlaylist.setChecked(allChecked);
								chkPlaylist.setOnCheckedChangeListener((buttonView1, isChecked1) -> {
									if (finalRv != null) {
										for (int j = 0; j < finalRv.getChildCount(); j++) {
											android.view.View c = finalRv.getChildAt(j);
											com.mflow.music.SongViewHolder2 ch = (com.mflow.music.SongViewHolder2) c.getTag();
											if (ch != null && ch.bottomremove2 != null) {
												ch.bottomremove2.setChecked(isChecked1);
											}
										}
									}
								});
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
			
			binding.title2.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/bold.ttf"), 1);
			binding.art2.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/regular.ttf"), 0);
		}
		
		@Override
		public int getItemCount() {
			return _data.size();
		}
		
		public class ViewHolder extends RecyclerView.ViewHolder {
			public ViewHolder(View v) {
				super(v);
			}
		}
	}
}
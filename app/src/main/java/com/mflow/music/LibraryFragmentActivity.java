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

public class LibraryFragmentActivity extends Fragment {
	
	private LibraryFragmentBinding binding;
	
	private ArrayList<HashMap<String, Object>> playlist = new ArrayList<>();
	
	private Intent intent = new Intent();
	
	@NonNull
	@Override
	public View onCreateView(@NonNull LayoutInflater _inflater, @Nullable ViewGroup _container, @Nullable Bundle _savedInstanceState) {
		binding = LibraryFragmentBinding.inflate(_inflater, _container, false);
		initialize(_savedInstanceState, binding.getRoot());
		initializeLogic();
		return binding.getRoot();
	}
	
	private void initialize(Bundle _savedInstanceState, View _view) {
		binding.rvPlaylists.setAdapter(new RvPlaylistsAdapter(playlist));
		binding.rvPlaylists.setLayoutManager(new LinearLayoutManager(getContext()));
		binding.textview8.setTypeface(Typeface.createFromAsset(getContext().getAssets(),"fonts/bold.ttf"), 1);
		binding.txtStatPlays.setTypeface(Typeface.createFromAsset(getContext().getAssets(),"fonts/regular.ttf"), 0);
		binding.txtStatListened.setTypeface(Typeface.createFromAsset(getContext().getAssets(),"fonts/regular.ttf"), 0);
		binding.txtStatExplored.setTypeface(Typeface.createFromAsset(getContext().getAssets(),"fonts/regular.ttf"), 0);
		binding.txtStatTopTrack.setTypeface(Typeface.createFromAsset(getContext().getAssets(),"fonts/regular.ttf"), 0);
		binding.txtPlaylistTitle.setTypeface(Typeface.createFromAsset(getContext().getAssets(),"fonts/bold.ttf"), 1);
		binding.vscroll2.setVerticalFadingEdgeEnabled(true);
		binding.vscroll2.setFadingEdgeLength(70);
		
		
		binding.linear10.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				intent.setClass(getContext().getApplicationContext(), StatsActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
			}
		});
	}
	
	private void initializeLogic() {
	}
	
	@Override
	public void onResume() {
		super.onResume();
		View _view = getView();
		
		if (_view != null && getContext() != null) {
			final TextView txtStatPlays = _view.findViewById(R.id.txtStatPlays);
			final TextView txtStatListened = _view.findViewById(R.id.txtStatListened);
			final TextView txtStatExplored = _view.findViewById(R.id.txtStatExplored);
			final TextView txtStatTopTrack = _view.findViewById(R.id.txtStatTopTrack);
			
			final StatsManager statsManager = new StatsManager(getContext());
			
			final Runnable refreshStats = () -> {
				txtStatPlays.setText(String.valueOf(statsManager.getTotalPlays()));
				txtStatListened.setText(statsManager.getFormattedListenedTime());
				txtStatExplored.setText(String.valueOf(statsManager.getExploredCount()));
				
				String topTrack = statsManager.getTopTrackName();
				txtStatTopTrack.setText((topTrack == null || topTrack.equalsIgnoreCase("None") || topTrack.trim().isEmpty()) ? "Unknown" : topTrack);
			};
			
			refreshStats.run();
			
			android.content.BroadcastReceiver statsReceiver = new android.content.BroadcastReceiver() {
				@Override
				public void onReceive(android.content.Context context, android.content.Intent intent) {
					refreshStats.run();
				}
			};
			
			android.content.IntentFilter filter = new android.content.IntentFilter("MFLOW_UPDATE_PLAYER_UI");
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
				getContext().registerReceiver(statsReceiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED);
			} else {
				getContext().registerReceiver(statsReceiver, filter);
			}
		}
		
		final androidx.recyclerview.widget.RecyclerView rvPlaylists = getActivity().findViewById(R.id.rvPlaylists);
		
		final Runnable loadPlaylistsData = () -> {
			playlist.clear();
			ArrayList<HashMap<String, Object>> rawPlaylists = PlaylistManager.getPlaylistsListMap(getContext());
			
			for (HashMap<String, Object> pMap : rawPlaylists) {
				String pName = pMap.containsKey("name") && pMap.get("name") != null ? pMap.get("name").toString() : "";
				ArrayList<HashMap<String, Object>> pSongs = PlaylistManager.getPlaylistSongs(getContext(), pName);
				
				long lastAlbumId = -1L;
				if (pSongs != null && !pSongs.isEmpty()) {
					HashMap<String, Object> lastSong = pSongs.get(pSongs.size() - 1);
					if (lastSong.containsKey("albumId") && lastSong.get("albumId") != null) {
						try {
							lastAlbumId = Long.parseLong(lastSong.get("albumId").toString());
						} catch (Exception ignored) {}
					} else if (lastSong.containsKey("album_id") && lastSong.get("album_id") != null) {
						try {
							lastAlbumId = Long.parseLong(lastSong.get("album_id").toString());
						} catch (Exception ignored) {}
					}
				}
				
				pMap.put("lastAlbumId", lastAlbumId);
				playlist.add(pMap);
			}
		};
		
		loadPlaylistsData.run();
		
		if (rvPlaylists != null && rvPlaylists.getAdapter() != null) {
			rvPlaylists.getAdapter().notifyDataSetChanged();
		}
		
		com.google.android.material.card.MaterialCardView btnCreatePlaylist = getActivity().findViewById(R.id.btnCreatePlaylist);
		if (btnCreatePlaylist != null) {
			btnCreatePlaylist.setOnClickListener(v -> {
				float density = getContext().getResources().getDisplayMetrics().density;
				android.util.TypedValue typedValue = new android.util.TypedValue();
				
				// حاوية MaterialCardView للديالوج بمعايير Material 3
				com.google.android.material.card.MaterialCardView cardContainer = new com.google.android.material.card.MaterialCardView(getContext());
				cardContainer.setLayoutParams(new android.widget.FrameLayout.LayoutParams(
				android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
				android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
				));
				cardContainer.setCardElevation(0f);
				cardContainer.setRadius(28f * density); // حواف دائرية عصرية 28dp
				cardContainer.setStrokeWidth(0);
				
				getContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true);
				cardContainer.setCardBackgroundColor(typedValue.data);
				
				LinearLayout innerLayout = new LinearLayout(getContext());
				innerLayout.setLayoutParams(new com.google.android.material.card.MaterialCardView.LayoutParams(
				com.google.android.material.card.MaterialCardView.LayoutParams.MATCH_PARENT,
				com.google.android.material.card.MaterialCardView.LayoutParams.WRAP_CONTENT
				));
				innerLayout.setOrientation(LinearLayout.VERTICAL);
				innerLayout.setPadding((int)(24 * density), (int)(24 * density), (int)(24 * density), (int)(20 * density));
				
				TextView txtTitle = new TextView(getContext());
				txtTitle.setText("New Playlist");
				txtTitle.setTextSize(18f);
				getContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true);
				txtTitle.setTextColor(typedValue.data);
				txtTitle.setTypeface(null, android.graphics.Typeface.BOLD);
				innerLayout.addView(txtTitle);
				
				final EditText inputName = new EditText(getContext());
				inputName.setHint("Playlist Name");
				getContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurfaceVariant, typedValue, true);
				inputName.setHintTextColor(typedValue.data);
				getContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true);
				inputName.setTextColor(typedValue.data);
				inputName.setTextSize(14f);
				inputName.setPadding((int)(16 * density), (int)(12 * density), (int)(16 * density), (int)(12 * density));
				inputName.setSingleLine(true);
				
				// خلفية حقل الإدخال بتنسيق Material 3 (SurfaceContainerHigh)
				android.graphics.drawable.GradientDrawable inputBg = new android.graphics.drawable.GradientDrawable();
				getContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurfaceContainerHigh, typedValue, true);
				inputBg.setColor(typedValue.data);
				inputBg.setCornerRadius(16f * density);
				inputName.setBackground(inputBg);
				
				LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, 
				LinearLayout.LayoutParams.WRAP_CONTENT
				);
				inputParams.setMargins(0, (int)(16 * density), 0, (int)(20 * density));
				inputName.setLayoutParams(inputParams);
				innerLayout.addView(inputName);
				
				LinearLayout btnLayout = new LinearLayout(getContext());
				btnLayout.setLayoutParams(new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT
				));
				btnLayout.setOrientation(LinearLayout.HORIZONTAL);
				btnLayout.setGravity(android.view.Gravity.END);
				
				TextView btnCancel = new TextView(getContext());
				btnCancel.setText("Cancel");
				getContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurfaceVariant, typedValue, true);
				btnCancel.setTextColor(typedValue.data);
				btnCancel.setTextSize(14f);
				btnCancel.setTypeface(null, android.graphics.Typeface.BOLD);
				int btnPad = (int)(8 * density);
				btnCancel.setPadding(btnPad, btnPad, btnPad, btnPad);
				
				TextView btnCreate = new TextView(getContext());
				btnCreate.setText("Create");
				getContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true);
				btnCreate.setTextColor(typedValue.data);
				btnCreate.setTextSize(14f);
				btnCreate.setTypeface(null, android.graphics.Typeface.BOLD);
				btnCreate.setPadding(btnPad, btnPad, btnPad, btnPad);
				
				btnLayout.addView(btnCancel);
				btnLayout.addView(btnCreate);
				innerLayout.addView(btnLayout);
				
				cardContainer.addView(innerLayout);
				
				// استخدام MaterialAlertDialogBuilder لتوحيد أسلوب الظهور والانيميشن مع باقي الديالوجات
				com.google.android.material.dialog.MaterialAlertDialogBuilder builder = 
				new com.google.android.material.dialog.MaterialAlertDialogBuilder(getContext());
				builder.setView(cardContainer);
				
				final androidx.appcompat.app.AlertDialog createDialog = builder.create();
				if (createDialog.getWindow() != null) {
					createDialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
				}
				
				btnCancel.setOnClickListener(view -> createDialog.dismiss());
				
				btnCreate.setOnClickListener(view -> {
					String name = inputName.getText().toString().trim();
					if (!name.isEmpty()) {
						PlaylistManager.createPlaylist(getContext(), name);
						
						loadPlaylistsData.run();
						
						if (rvPlaylists != null && rvPlaylists.getAdapter() != null) {
							rvPlaylists.getAdapter().notifyDataSetChanged();
						}
					}
					createDialog.dismiss();
				});
				
				createDialog.show();
				
				if (createDialog.getWindow() != null) {
					int dialogWidth = (int) (getContext().getResources().getDisplayMetrics().widthPixels * 0.85);
					createDialog.getWindow().setLayout(dialogWidth, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
				}
			});
		}
		
	}
	public class RvPlaylistsAdapter extends RecyclerView.Adapter<RvPlaylistsAdapter.ViewHolder> {
		
		ArrayList<HashMap<String, Object>> _data;
		
		public RvPlaylistsAdapter(ArrayList<HashMap<String, Object>> _arr) {
			_data = _arr;
		}
		
		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			LayoutInflater _inflater = getActivity().getLayoutInflater();
			View _v = _inflater.inflate(R.layout.listplay, null);
			RecyclerView.LayoutParams _lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			_v.setLayoutParams(_lp);
			return new ViewHolder(_v);
		}
		
		@Override
		public void onBindViewHolder(ViewHolder _holder, final int _position) {
			View _view = _holder.itemView;
			ListplayBinding binding = ListplayBinding.bind(_view);
			
			final View customView = _view;
			
			TextView txtPlaylistName = customView.findViewById(R.id.txtPlaylistName);
			TextView txtSongCount = customView.findViewById(R.id.txtSongCount);
			ImageView imgPlaylist = customView.findViewById(R.id.imgPlaylist);
			View cardDeletePlaylist = customView.findViewById(R.id.cardDeletePlaylist);
			
			final HashMap<String, Object> currentItem = playlist.get((int)_position);
			final String name = currentItem.containsKey("name") && currentItem.get("name") != null ? currentItem.get("name").toString() : "";
			String count = currentItem.containsKey("count") && currentItem.get("count") != null ? currentItem.get("count").toString() : "0";
			
			if (txtPlaylistName != null) {
				txtPlaylistName.setText(name);
			}
			
			if (txtSongCount != null) {
				txtSongCount.setText(count + " Tracks");
			}
			
			if (imgPlaylist != null) {
				long albumId = -1;
				
				if (currentItem.containsKey("last_album_id") && currentItem.get("last_album_id") != null) {
					try {
						albumId = Long.parseLong(currentItem.get("last_album_id").toString());
					} catch (Exception ignored) {}
				} else {
					ArrayList<HashMap<String, Object>> playlistSongs = PlaylistManager.getPlaylistSongs(customView.getContext(), name);
					if (playlistSongs != null && !playlistSongs.isEmpty()) {
						HashMap<String, Object> lastSong = playlistSongs.get(playlistSongs.size() - 1);
						if (lastSong.containsKey("album_id") && lastSong.get("album_id") != null) {
							try {
								albumId = Long.parseLong(lastSong.get("album_id").toString());
							} catch (Exception ignored) {}
						}
					}
				}
				
				if (albumId > 0) {
					ImageLoader.loadAlbumArt(customView.getContext(), albumId, imgPlaylist, R.drawable.ic_add);
				} else {
					imgPlaylist.setImageResource(R.drawable.ic_add);
				}
			}
			
			customView.setClickable(true);
			customView.setFocusable(true);
			
			customView.setOnClickListener(v -> {
				PlaylistManager.setSelectedPlaylistName(name);
				Intent intent = new Intent(customView.getContext(), PlaylistActivity.class);
				intent.putExtra("playlist_name", name);
				customView.getContext().startActivity(intent);
			});
			
			if (cardDeletePlaylist != null) {
				cardDeletePlaylist.setOnClickListener(v -> {
					android.content.Context context = customView.getContext();
					float density = context.getResources().getDisplayMetrics().density;
					android.util.TypedValue typedValue = new android.util.TypedValue();
					
					// حاوية MaterialCardView الرئيسية للديالوج بمعايير Material 3 الحديثة
					com.google.android.material.card.MaterialCardView cardView = new com.google.android.material.card.MaterialCardView(context);
					cardView.setLayoutParams(new android.widget.FrameLayout.LayoutParams(
					android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
					android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
					));
					cardView.setCardElevation(0f);
					cardView.setRadius(28f * density); // حواف دائرية عصرية 28dp مثل الإشعارات
					cardView.setStrokeWidth(0);
					
					// جلب لون السطح الحالي ديناميكياً من الثيم
					context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true);
					cardView.setCardBackgroundColor(typedValue.data);
					
					LinearLayout layout = new LinearLayout(context);
					layout.setLayoutParams(new com.google.android.material.card.MaterialCardView.LayoutParams(
					com.google.android.material.card.MaterialCardView.LayoutParams.MATCH_PARENT,
					com.google.android.material.card.MaterialCardView.LayoutParams.WRAP_CONTENT
					));
					layout.setOrientation(LinearLayout.VERTICAL);
					int padding = (int)(24 * density);
					layout.setPadding(padding, padding, padding, padding);
					
					// العنوان
					TextView txtTitle = new TextView(context);
					txtTitle.setText("Delete Playlist");
					txtTitle.setTextSize(18);
					txtTitle.setTypeface(null, android.graphics.Typeface.BOLD);
					context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true);
					txtTitle.setTextColor(typedValue.data);
					layout.addView(txtTitle);
					
					// الرسالة النصية
					TextView txtMessage = new TextView(context);
					txtMessage.setText("Are you sure you want to delete \"" + name + "\"?");
					txtMessage.setTextSize(14);
					context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurfaceVariant, typedValue, true);
					txtMessage.setTextColor(typedValue.data);
					
					LinearLayout.LayoutParams msgParams = new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT, 
					LinearLayout.LayoutParams.WRAP_CONTENT
					);
					msgParams.setMargins(0, (int)(12 * density), 0, (int)(24 * density));
					txtMessage.setLayoutParams(msgParams);
					layout.addView(txtMessage);
					
					// حاوية الأزرار في الأسفل (Cancel و Delete) محاذاة لليمين
					LinearLayout btnLayout = new LinearLayout(context);
					btnLayout.setLayoutParams(new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT,
					LinearLayout.LayoutParams.WRAP_CONTENT
					));
					btnLayout.setOrientation(LinearLayout.HORIZONTAL);
					btnLayout.setGravity(android.view.Gravity.END);
					
					TextView btnCancel = new TextView(context);
					btnCancel.setText("Cancel");
					context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurfaceVariant, typedValue, true);
					btnCancel.setTextColor(typedValue.data);
					btnCancel.setTextSize(14);
					btnCancel.setTypeface(null, android.graphics.Typeface.BOLD);
					int btnPad = (int)(8 * density);
					btnCancel.setPadding(btnPad, btnPad, btnPad, btnPad);
					
					TextView btnDelete = new TextView(context);
					btnDelete.setText("Delete");
					// استخدام لون الخطأ (Error) إذا توفر، وإلا لون افتراضي
					int errorColor = android.graphics.Color.parseColor("#EF4444");
					try {
						int colorAttr = context.getResources().getIdentifier("colorError", "attr", context.getPackageName());
						if (colorAttr != 0) {
							context.getTheme().resolveAttribute(colorAttr, typedValue, true);
							errorColor = typedValue.data;
						}
					} catch (Exception e) {}
					btnDelete.setTextColor(errorColor);
					btnDelete.setTextSize(14);
					btnDelete.setTypeface(null, android.graphics.Typeface.BOLD);
					btnDelete.setPadding(btnPad, btnPad, btnPad, btnPad);
					
					btnLayout.addView(btnCancel);
					btnLayout.addView(btnDelete);
					layout.addView(btnLayout);
					
					cardView.addView(layout);
					
					com.google.android.material.dialog.MaterialAlertDialogBuilder builder = 
					new com.google.android.material.dialog.MaterialAlertDialogBuilder(context);
					builder.setView(cardView);
					
					final androidx.appcompat.app.AlertDialog deleteDialog = builder.create();
					if (deleteDialog.getWindow() != null) {
						deleteDialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
					}
					
					btnCancel.setOnClickListener(new android.view.View.OnClickListener() {
						@Override
						public void onClick(android.view.View v) {
							deleteDialog.dismiss();
						}
					});
					
					btnDelete.setOnClickListener(new android.view.View.OnClickListener() {
						@Override
						public void onClick(android.view.View v) {
							PlaylistManager.deletePlaylist(context, name);
							
							playlist.clear();
							playlist.addAll(PlaylistManager.getPlaylistsListMap(context));
							
							androidx.recyclerview.widget.RecyclerView localRv = customView.getRootView().findViewById(R.id.rvPlaylists);
							if (localRv != null && localRv.getAdapter() != null) {
								localRv.getAdapter().notifyDataSetChanged();
							}
							
							deleteDialog.dismiss();
						}
					});
					
					deleteDialog.show();
					
					// ضبط عرض الديالوج تماماً مثل إشعارات الإعدادات (85% من الشاشة)
					if (deleteDialog.getWindow() != null) {
						int dialogWidth = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.85);
						deleteDialog.getWindow().setLayout(dialogWidth, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
					}
				});
			}
			
			binding.txtPlaylistName.setTypeface(Typeface.createFromAsset(getContext().getAssets(),"fonts/bold.ttf"), 1);
			binding.txtSongCount.setTypeface(Typeface.createFromAsset(getContext().getAssets(),"fonts/regular.ttf"), 0);
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
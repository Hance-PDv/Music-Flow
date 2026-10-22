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

public class SearchActivity extends AppCompatActivity {
	
	private SearchBinding binding;
	
	private ArrayList<HashMap<String, Object>> list_search = new ArrayList<>();
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		binding = SearchBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());
		initialize(_savedInstanceState);
		initializeLogic();
	}
	
	private void initialize(Bundle _savedInstanceState) {
		
		binding.cardSearchBack.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				
			}
		});
		
		binding.btnSearchBack.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View _view) {
				
				return true;
			}
		});
		
		binding.btnSearchBack.setOnClickListener(new View.OnClickListener() {
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
		
		final EditText etSearchQuery = findViewById(R.id.etSearchQuery);
		final androidx.recyclerview.widget.RecyclerView rvSearchResults = findViewById(R.id.rvSearchResults);
		
		final java.util.ArrayList<java.util.HashMap<String, Object>> originalList = new java.util.ArrayList<>();
		
		originalList.clear();
		originalList.addAll(com.mflow.music.MusicCache.getSongs(getApplicationContext()));
		
		list_search.clear();
		list_search.addAll(originalList);
		
		if (rvSearchResults.getAdapter() != null) {
			rvSearchResults.getAdapter().notifyDataSetChanged();
		}
		
		if (etSearchQuery != null) {
			etSearchQuery.addTextChangedListener(new android.text.TextWatcher() {
				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
				
				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {
					String query = s.toString().trim().toLowerCase();
					list_search.clear();
					
					if (query.isEmpty()) {
						list_search.addAll(originalList);
					} else {
						for (java.util.HashMap<String, Object> song : originalList) {
							String title = song.get("title") != null ? song.get("title").toString().toLowerCase() : "";
							String artist = song.get("artist") != null ? song.get("artist").toString().toLowerCase() : "";
							
							if (title.contains(query) || artist.contains(query)) {
								list_search.add(song);
							}
						}
					}
					
					if (rvSearchResults.getAdapter() != null) {
						rvSearchResults.getAdapter().notifyDataSetChanged();
					}
				}
				
				@Override
				public void afterTextChanged(android.text.Editable s) {}
			});
		}
		
		binding.rvSearchResults.setAdapter(new RvSearchResultsAdapter(list_search));
		binding.rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
		binding.etSearchQuery.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/regular.ttf"), 0);
	}
	
	public class RvSearchResultsAdapter extends RecyclerView.Adapter<RvSearchResultsAdapter.ViewHolder> {
		
		ArrayList<HashMap<String, Object>> _data;
		
		public RvSearchResultsAdapter(ArrayList<HashMap<String, Object>> _arr) {
			_data = _arr;
		}
		
		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			LayoutInflater _inflater = getLayoutInflater();
			View _v = _inflater.inflate(R.layout.music, null);
			RecyclerView.LayoutParams _lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			_v.setLayoutParams(_lp);
			return new ViewHolder(_v);
		}
		
		@Override
		public void onBindViewHolder(ViewHolder _holder, final int _position) {
			View _view = _holder.itemView;
			MusicBinding binding = MusicBinding.bind(_view);
			
			com.mflow.music.SongViewHolder holder = (com.mflow.music.SongViewHolder) _view.getTag();
			if (holder == null) {
				holder = new com.mflow.music.SongViewHolder(_view);
				_view.setTag(holder);
			}
			
			final HashMap<String, Object> map = list_search.get(_position);
			
			holder.title.setText(String.valueOf(map.get("title")));
			holder.art.setText(String.valueOf(map.get("artist")));
			
			long albumId = -1;
			if (map.get("album_id") != null) {
				try {
					albumId = Long.parseLong(String.valueOf(map.get("album_id")));
				} catch (Exception ignored) {}
			}
			
			com.mflow.music.ImageLoader.loadAlbumArt(
			_view.getContext(),
			albumId,
			holder.img_music,
			R.drawable.default_music_image
			);
			
			android.util.TypedValue typedValue = new android.util.TypedValue();
			_view.getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true);
			holder.linearm.setBackgroundResource(typedValue.resourceId);
			
			holder.linearm.setOnClickListener(v -> {
				android.content.Context context = v.getContext();
				
				if (context != null) {
					String targetPath = map.containsKey("path") ? String.valueOf(map.get("path")) : "";
					int realIndex = -1;
					
					if (com.mflow.music.MusicService.songList != null) {
						for (int i = 0; i < com.mflow.music.MusicService.songList.size(); i++) {
							String p = com.mflow.music.MusicService.songList.get(i).get("path") != null 
							? String.valueOf(com.mflow.music.MusicService.songList.get(i).get("path")) : "";
							if (!targetPath.isEmpty() && targetPath.equals(p)) {
								realIndex = i;
								break;
							}
						}
					}
					
					if (realIndex == -1) {
						java.util.ArrayList<java.util.HashMap<String, Object>> fallbackList = com.mflow.music.MusicCache.getSongs(context);
						com.mflow.music.MusicService.songList = new java.util.ArrayList<>(fallbackList);
						for (int i = 0; i < fallbackList.size(); i++) {
							String p = fallbackList.get(i).get("path") != null ? String.valueOf(fallbackList.get(i).get("path")) : "";
							if (!targetPath.isEmpty() && targetPath.equals(p)) {
								realIndex = i;
								break;
							}
						}
					}
					
					if (realIndex != -1) {
						com.mflow.music.MusicService.currentIndex = realIndex;
						
						android.content.Intent playIntent = new android.content.Intent(context, com.mflow.music.MusicService.class);
						playIntent.setAction("ACTION_PLAY");
						playIntent.putExtra("index", realIndex);
						playIntent.putExtra("path", targetPath);
						
						if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
							context.startForegroundService(playIntent);
						} else {
							context.startService(playIntent);
						}
					}
				}
			});
			
			binding.title.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/bold.ttf"), 1);
			binding.art.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/regular.ttf"), 0);
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
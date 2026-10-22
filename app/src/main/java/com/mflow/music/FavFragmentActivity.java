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

public class FavFragmentActivity extends Fragment {
	
	private FavFragmentBinding binding;
	private android.content.BroadcastReceiver favReceiver;
	private double old_size = 0;
	
	private ArrayList<HashMap<String, Object>> list_fav = new ArrayList<>();
	
	@NonNull
	@Override
	public View onCreateView(@NonNull LayoutInflater _inflater, @Nullable ViewGroup _container, @Nullable Bundle _savedInstanceState) {
		binding = FavFragmentBinding.inflate(_inflater, _container, false);
		initialize(_savedInstanceState, binding.getRoot());
		initializeLogic();
		return binding.getRoot();
	}
	
	private void initialize(Bundle _savedInstanceState, View _view) {
		binding.recyclerview1.setVerticalFadingEdgeEnabled(true);
		binding.recyclerview1.setFadingEdgeLength(70);
		
		binding.recyclerview1.setAdapter(new Recyclerview1Adapter(list_fav));
		binding.text.setTypeface(Typeface.createFromAsset(getContext().getAssets(),"fonts/bold.ttf"), 1);
		binding.txtFavCount.setTypeface(Typeface.createFromAsset(getContext().getAssets(),"fonts/regular.ttf"), 0);
		binding.txtFavStatus.setTypeface(Typeface.createFromAsset(getContext().getAssets(),"fonts/regular.ttf"), 0);
	}
	
	private void initializeLogic() {
	}
	
	@Override
	public void onResume() {
		super.onResume();
		old_size = list_fav.size();
		list_fav.clear();
		list_fav.addAll(com.mflow.music.PrefsManager.getFavorites(getContext()));
		
		if (list_fav.size() == 0) {
			binding.NoMusicFoundLayout.setVisibility(View.VISIBLE);
			binding.recyclerview1.setVisibility(View.GONE);
		} else {
			binding.recyclerview1.setVisibility(View.VISIBLE);
			binding.NoMusicFoundLayout.setVisibility(View.GONE);
		}
		final android.widget.TextView txtFavCount = (android.widget.TextView) getView().findViewById(R.id.txtFavCount);
		final android.widget.TextView txtFavStatus = (android.widget.TextView) getView().findViewById(R.id.txtFavStatus);
		
		if (txtFavCount != null) {
			txtFavCount.setText(com.mflow.music.PrefsManager.getFavCountString(getContext()));
		}
		
		if (txtFavStatus != null) {
			txtFavStatus.setText(com.mflow.music.PrefsManager.getFavTotalDurationString(getContext()));
		}
		
		if (!(old_size == list_fav.size())) {
			binding.recyclerview1.setLayoutManager(new LinearLayoutManager(getContext()));
		}
	}
	public class Recyclerview1Adapter extends RecyclerView.Adapter<Recyclerview1Adapter.ViewHolder> {
		
		ArrayList<HashMap<String, Object>> _data;
		
		public Recyclerview1Adapter(ArrayList<HashMap<String, Object>> _arr) {
			_data = _arr;
		}
		
		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			LayoutInflater _inflater = getActivity().getLayoutInflater();
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
			
			HashMap<String, Object> map = list_fav.get(_position);
			
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
			
			final int pos = _position;
			holder.linearm.setOnClickListener(new android.view.View.OnClickListener() {
				@Override
				public void onClick(android.view.View v) {
					android.content.Context context = v.getContext();
					
					if (context != null) {
						// تعيين قائمة المفضلات كقائمة تشغيل حالية وتحديث المؤشر
						com.mflow.music.MusicService.songList = new java.util.ArrayList<>(list_fav);
						com.mflow.music.MusicService.currentIndex = pos;
						
						android.content.Intent playIntent = new android.content.Intent(context, com.mflow.music.MusicService.class);
						playIntent.setAction("ACTION_PLAY");
						playIntent.putExtra("index", pos);
						
						if (list_fav.get(pos).containsKey("path")) {
							playIntent.putExtra("path", String.valueOf(list_fav.get(pos).get("path")));
						}
						
						if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
							context.startForegroundService(playIntent);
						} else {
							context.startService(playIntent);
						}
					}
				}
			});
			
			binding.title.setTypeface(Typeface.createFromAsset(getContext().getAssets(),"fonts/bold.ttf"), 1);
			binding.art.setTypeface(Typeface.createFromAsset(getContext().getAssets(),"fonts/regular.ttf"), 0);
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
package com.mflow.music;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {

    private static final int REQUEST_AUDIO = 100;
    private static final int REQUEST_NOTIFICATIONS = 101;

    private static final String ACTION_PLAYBACK_UPDATE = "com.mflow.music.PLAYBACK_UPDATE";
    private static final String ACTION_SERVICE_COMMAND = "com.mflow.music.SERVICE_COMMAND";

    private static final int BG = Color.rgb(15, 15, 20);
    private static final int SURFACE = Color.rgb(29, 29, 37);
    private static final int ACCENT = Color.rgb(160, 130, 255);
    private static final int WHITE = Color.WHITE;
    private static final int MUTED = Color.rgb(165, 165, 180);

    private LinearLayout root;
    private LinearLayout content;
    private LinearLayout miniPlayer;
    private RecyclerView recycler;
    private SongAdapter adapter;
    private EditText searchInput;

    private final ArrayList<Song> songs = new ArrayList<Song>();
    private final ArrayList<Song> visibleSongs = new ArrayList<Song>();
    private final ArrayList<Song> playbackQueue = new ArrayList<Song>();
    private int playbackQueueIndex = 0;
    private final HashSet<String> favorites = new HashSet<String>();
    private final ArrayList<String> playlistNames = new ArrayList<String>();
    private final HashMap<String, ArrayList<String>> playlists =
            new HashMap<String, ArrayList<String>>();
    private final HashMap<String, Integer> playCounts = new HashMap<String, Integer>();

    private SharedPreferences preferences;
    private Song currentSong;
    private boolean isPlaying = false;
    private boolean playerExpanded = false;
    private String currentPage = "home";
    private String openedPlaylist = null;

    private TextView miniTitle;
    private TextView miniArtist;
    private TextView miniPlayButton;
    private SeekBar playerSeekBar;
    private TextView playerTime;

    private final BroadcastReceiver playbackReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;

            String action = intent.getAction();
            if (ACTION_PLAYBACK_UPDATE.equals(action)) {
                isPlaying = intent.getBooleanExtra("playing", false);
                String uri = intent.getStringExtra("uri");

                boolean trackChanged = false;
                if (uri != null) {
                    Song found = findSong(uri);
                    if (found != null && (currentSong == null || !found.uri.equals(currentSong.uri))) {
                        currentSong = found;
                        int queuePosition = playbackQueue.indexOf(found);
                        if (queuePosition >= 0) playbackQueueIndex = queuePosition;
                        trackChanged = true;
                    }
                }

                updateMiniPlayer();

                if (playerExpanded && currentSong != null) {
                    if (trackChanged) showPlayer();
                    else {
                        refreshPlayerState();
                        int position = intent.getIntExtra("position", 0);
                        int duration = intent.getIntExtra("duration", 0);
                        if (playerSeekBar != null) {
                            playerSeekBar.setMax(Math.max(1, duration));
                            playerSeekBar.setProgress(Math.min(position, playerSeekBar.getMax()));
                        }
                        if (playerTime != null) {
                            playerTime.setText(formatTime(position) + " / " + formatTime(duration));
                        }
                    }
                }
            } else if (ACTION_SERVICE_COMMAND.equals(action)) {
                String command = intent.getStringExtra("command");
                if ("NEXT".equals(command)) {
                    nextSong();
                } else if ("PREVIOUS".equals(command)) {
                    previousSong();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        preferences = getSharedPreferences("MFlow", MODE_PRIVATE);
        loadSavedData();

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_PLAYBACK_UPDATE);
        filter.addAction(ACTION_SERVICE_COMMAND);

        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(playbackReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(playbackReceiver, filter);
        }

        createMainInterface();
        requestAudioPermissionIfNeeded();
        requestNotificationPermissionIfNeeded();
    }

    private void loadSavedData() {
        favorites.clear();
        favorites.addAll(preferences.getStringSet("favorites", new HashSet<String>()));

        playCounts.clear();
        try {
            JSONObject counts = new JSONObject(preferences.getString("play_counts", "{}"));
            Iterator<String> keys = counts.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                playCounts.put(key, counts.optInt(key, 0));
            }
        } catch (Exception ignored) {
        }

        playlistNames.clear();
        playlists.clear();
        try {
            JSONObject saved = new JSONObject(preferences.getString("playlists", "{}"));
            Iterator<String> names = saved.keys();
            while (names.hasNext()) {
                String name = names.next();
                JSONArray array = saved.optJSONArray(name);
                ArrayList<String> entries = new ArrayList<String>();
                if (array != null) {
                    for (int i = 0; i < array.length(); i++) {
                        String value = array.optString(i, null);
                        if (value != null) entries.add(value);
                    }
                }
                playlistNames.add(name);
                playlists.put(name, entries);
            }
        } catch (Exception ignored) {
        }
    }

    private void savePlayCounts() {
        try {
            JSONObject object = new JSONObject();
            for (Map.Entry<String, Integer> entry : playCounts.entrySet()) {
                object.put(entry.getKey(), entry.getValue());
            }
            preferences.edit().putString("play_counts", object.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    private void savePlaylists() {
        try {
            JSONObject object = new JSONObject();
            for (String name : playlistNames) {
                JSONArray array = new JSONArray();
                ArrayList<String> entries = playlists.get(name);
                if (entries != null) {
                    for (String uri : entries) array.put(uri);
                }
                object.put(name, array);
            }
            preferences.edit().putString("playlists", object.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable background(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    private TextView text(String value, int size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }

    private Button button(String value) {
        Button view = new Button(this);
        view.setText(value);
        view.setTextColor(WHITE);
        view.setTextSize(13);
        view.setAllCaps(false);
        view.setBackground(background(SURFACE, 16));
        return view;
    }

    private void createMainInterface() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        setContentView(root);

        LinearLayout header = new LinearLayout(this);
        header.setPadding(dp(20), dp(16), dp(20), dp(8));
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView logo = text("MFlow", 28, WHITE);
        logo.setTypeface(null, Typeface.BOLD);
        header.addView(logo, new LinearLayout.LayoutParams(0, dp(55), 1));

        TextView settings = text("⚙", 25, WHITE);
        settings.setGravity(Gravity.CENTER);
        settings.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showSettings(); }
        });
        header.addView(settings, new LinearLayout.LayoutParams(dp(45), dp(45)));
        root.addView(header);

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(5), dp(18), dp(8));
        root.addView(content, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        createMiniPlayer();
        createNavigation();
        showHome();
    }

    private void createNavigation() {
        LinearLayout nav = new LinearLayout(this);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(6), dp(5), dp(6), dp(5));
        nav.setBackgroundColor(SURFACE);

        String[] pages = {"Home", "Library", "Favorites", "Search"};
        for (final String page : pages) {
            TextView item = text(page, 12, WHITE);
            item.setGravity(Gravity.CENTER);
            item.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    if ("Home".equals(page)) showHome();
                    else if ("Library".equals(page)) showLibrary();
                    else if ("Favorites".equals(page)) showFavorites();
                    else showSearch();
                }
            });
            nav.addView(item, new LinearLayout.LayoutParams(0, dp(48), 1));
        }
        root.addView(nav);
    }

    private void createMiniPlayer() {
        miniPlayer = new LinearLayout(this);
        miniPlayer.setGravity(Gravity.CENTER_VERTICAL);
        miniPlayer.setPadding(dp(14), dp(7), dp(10), dp(7));
        miniPlayer.setBackground(background(SURFACE, 18));
        miniPlayer.setVisibility(View.GONE);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        miniTitle = text("No song", 14, WHITE);
        miniArtist = text("Select a song", 11, MUTED);
        info.addView(miniTitle);
        info.addView(miniArtist);
        miniPlayer.addView(info, new LinearLayout.LayoutParams(0, -2, 1));

        miniPlayButton = text("▶", 23, WHITE);
        miniPlayButton.setGravity(Gravity.CENTER);
        miniPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { togglePlayback(); }
        });
        miniPlayer.addView(miniPlayButton, new LinearLayout.LayoutParams(dp(48), dp(48)));

        miniPlayer.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showPlayer(); }
        });

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, dp(62));
        params.setMargins(dp(12), dp(5), dp(12), dp(7));
        root.addView(miniPlayer, params);
    }

    private void updateMiniPlayer() {
        if (currentSong == null) {
            miniPlayer.setVisibility(View.GONE);
            return;
        }
        miniPlayer.setVisibility(View.VISIBLE);
        miniTitle.setText(currentSong.title);
        miniArtist.setText(currentSong.artist);
        miniPlayButton.setText(isPlaying ? "Ⅱ" : "▶");
    }

    private void clearContent() {
        content.removeAllViews();
        playerExpanded = false;
        recycler = null;
        adapter = null;
    }

    private void addPageTitle(String titleText) {
        TextView title = text(titleText, 24, WHITE);
        title.setTypeface(null, Typeface.BOLD);
        content.addView(title);
    }

    private void showHome() {
        currentPage = "home";
        openedPlaylist = null;
        clearContent();

        TextView greeting = text("Your music", 25, WHITE);
        greeting.setTypeface(null, Typeface.BOLD);
        content.addView(greeting);
        content.addView(text("Discover your personal library", 13, MUTED));

        LinearLayout shortcuts = new LinearLayout(this);
        shortcuts.setPadding(0, dp(15), 0, dp(15));
        String[] names = {"Favorites", "Most Played", "Artists", "Playlists"};

        for (final String name : names) {
            Button item = button(name);
            item.setTextSize(10);
            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(0, dp(62), 1);
            params.setMargins(dp(2), 0, dp(2), 0);
            shortcuts.addView(item, params);
            item.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    if ("Favorites".equals(name)) showFavorites();
                    else if ("Artists".equals(name)) showArtists();
                    else if ("Playlists".equals(name)) showPlaylists();
                    else showMostPlayed();
                }
            });
        }
        content.addView(shortcuts);

        TextView recent = text("Recently added", 19, WHITE);
        recent.setTypeface(null, Typeface.BOLD);
        content.addView(recent);

        ArrayList<Song> recentSongs = new ArrayList<Song>(songs);
        Collections.sort(recentSongs, new Comparator<Song>() {
            @Override public int compare(Song a, Song b) {
                return Long.compare(b.dateAdded, a.dateAdded);
            }
        });
        setVisibleSongs(recentSongs);
        createRecycler();
    }

    private void showLibrary() {
        currentPage = "library";
        openedPlaylist = null;
        clearContent();
        addPageTitle("Music Library");
        content.addView(text(songs.size() + " songs", 13, MUTED));
        setVisibleSongs(songs);
        createRecycler();
    }

    private void showFavorites() {
        currentPage = "favorites";
        openedPlaylist = null;
        clearContent();
        addPageTitle("Favorites");

        ArrayList<Song> result = new ArrayList<Song>();
        for (Song song : songs) {
            if (favorites.contains(song.uri)) result.add(song);
        }
        setVisibleSongs(result);
        createRecycler();
    }

    private void showMostPlayed() {
        currentPage = "most";
        openedPlaylist = null;
        clearContent();
        addPageTitle("Most Played");

        ArrayList<Song> result = new ArrayList<Song>(songs);
        Collections.sort(result, new Comparator<Song>() {
            @Override public int compare(Song a, Song b) {
                return Integer.compare(getPlayCount(b.uri), getPlayCount(a.uri));
            }
        });
        setVisibleSongs(result);
        createRecycler();
    }

    private void showArtists() {
        currentPage = "artists";
        openedPlaylist = null;
        clearContent();
        addPageTitle("Artists");

        HashSet<String> artists = new HashSet<String>();
        for (Song song : songs) artists.add(song.artist);

        ArrayList<String> sorted = new ArrayList<String>(artists);
        Collections.sort(sorted, String.CASE_INSENSITIVE_ORDER);
        for (final String artist : sorted) {
            TextView item = text("♪  " + artist, 16, WHITE);
            item.setPadding(dp(12), dp(12), 0, dp(12));
            content.addView(item);
            item.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { showArtistSongs(artist); }
            });
        }
        if (sorted.isEmpty()) content.addView(text("No artists found.", 14, MUTED));
    }

    private void showArtistSongs(String artist) {
        currentPage = "artist_songs";
        clearContent();
        addPageTitle(artist);
        ArrayList<Song> result = new ArrayList<Song>();
        for (Song song : songs) {
            if (song.artist.equals(artist)) result.add(song);
        }
        setVisibleSongs(result);
        createRecycler();
    }

    private void showSearch() {
        currentPage = "search";
        openedPlaylist = null;
        clearContent();
        addPageTitle("Search");

        searchInput = new EditText(this);
        searchInput.setSingleLine(true);
        searchInput.setHint("Search songs or artists");
        searchInput.setHintTextColor(MUTED);
        searchInput.setTextColor(WHITE);
        searchInput.setPadding(dp(16), 0, dp(16), 0);
        searchInput.setBackground(background(SURFACE, 16));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(50));
        params.setMargins(0, dp(14), 0, dp(10));
        content.addView(searchInput, params);

        setVisibleSongs(songs);
        createRecycler();

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().toLowerCase(Locale.ROOT).trim();
                ArrayList<Song> result = new ArrayList<Song>();
                for (Song song : songs) {
                    if (song.title.toLowerCase(Locale.ROOT).contains(query)
                            || song.artist.toLowerCase(Locale.ROOT).contains(query)) {
                        result.add(song);
                    }
                }
                setVisibleSongs(result);
                if (adapter != null) adapter.notifyDataSetChanged();
            }
            @Override public void afterTextChanged(Editable s) { }
        });
    }

    private void showPlaylists() {
        currentPage = "playlists";
        openedPlaylist = null;
        clearContent();
        addPageTitle("Playlists");

        Button create = button("+ Create Playlist");
        LinearLayout.LayoutParams createParams =
                new LinearLayout.LayoutParams(-1, dp(52));
        createParams.setMargins(0, dp(12), 0, dp(10));
        content.addView(create, createParams);
        create.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { createPlaylistDialog(); }
        });

        for (final String name : new ArrayList<String>(playlistNames)) {
            TextView item = text("♫  " + name + "   ·   "
                    + playlists.get(name).size() + " songs", 15, WHITE);
            item.setPadding(dp(12), dp(15), dp(8), dp(15));
            content.addView(item);
            item.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { showPlaylist(name); }
            });
            item.setOnLongClickListener(new View.OnLongClickListener() {
                @Override public boolean onLongClick(View v) {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle(name)
                            .setItems(new String[]{"Rename", "Delete"}, (dialog, which) -> {
                                if (which == 0) renamePlaylist(name);
                                else deletePlaylist(name);
                            }).show();
                    return true;
                }
            });
        }
        if (playlistNames.isEmpty()) {
            content.addView(text("Your playlists will appear here.", 14, MUTED));
        }
    }

    private void createPlaylistDialog() {
        final EditText input = new EditText(this);
        input.setHint("Playlist name");
        new AlertDialog.Builder(this)
                .setTitle("New Playlist")
                .setView(input)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Create", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (name.length() == 0) return;
                    if (playlists.containsKey(name)) {
                        Toast.makeText(this, "Playlist already exists.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    playlistNames.add(name);
                    playlists.put(name, new ArrayList<String>());
                    savePlaylists();
                    showPlaylists();
                }).show();
    }

    private void renamePlaylist(final String oldName) {
        final EditText input = new EditText(this);
        input.setText(oldName);
        new AlertDialog.Builder(this)
                .setTitle("Rename Playlist")
                .setView(input)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (newName.length() == 0 || playlists.containsKey(newName)) return;
                    ArrayList<String> entries = playlists.remove(oldName);
                    int index = playlistNames.indexOf(oldName);
                    if (index >= 0) playlistNames.set(index, newName);
                    playlists.put(newName, entries);
                    savePlaylists();
                    showPlaylists();
                }).show();
    }

    private void deletePlaylist(String name) {
        playlists.remove(name);
        playlistNames.remove(name);
        savePlaylists();
        showPlaylists();
    }

    private void showPlaylist(String name) {
        currentPage = "playlist";
        openedPlaylist = name;
        clearContent();
        addPageTitle(name);

        Button add = button("+ Add Songs");
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(48));
        params.setMargins(0, dp(10), 0, dp(8));
        content.addView(add, params);
        add.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { addSongsToPlaylist(name); }
        });

        ArrayList<Song> result = new ArrayList<Song>();
        ArrayList<String> entries = playlists.get(name);
        if (entries != null) {
            for (String uri : entries) {
                Song song = findSong(uri);
                if (song != null) result.add(song);
            }
        }
        setVisibleSongs(result);
        createRecycler();
    }

    private void addSongsToPlaylist(final String name) {
        final String[] labels = new String[songs.size()];
        final boolean[] checked = new boolean[songs.size()];
        final ArrayList<String> entries = playlists.get(name);
        for (int i = 0; i < songs.size(); i++) {
            Song song = songs.get(i);
            labels[i] = song.title + " — " + song.artist;
            checked[i] = entries != null && entries.contains(song.uri);
        }

        new AlertDialog.Builder(this)
                .setTitle("Select songs")
                .setMultiChoiceItems(labels, checked, (dialog, which, isChecked) -> {
                    if (entries == null) return;
                    String uri = songs.get(which).uri;
                    if (isChecked) {
                        if (!entries.contains(uri)) entries.add(uri);
                    } else {
                        entries.remove(uri);
                    }
                })
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (dialog, which) -> {
                    savePlaylists();
                    showPlaylist(name);
                }).show();
    }

    private void setVisibleSongs(ArrayList<Song> list) {
        visibleSongs.clear();
        visibleSongs.addAll(list);
    }

    private void createRecycler() {
        recycler = new RecyclerView(this);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setItemAnimator(null);
        adapter = new SongAdapter();
        recycler.setAdapter(adapter);
        content.addView(recycler, new LinearLayout.LayoutParams(-1, 0, 1));
    }

    private class SongAdapter extends RecyclerView.Adapter<SongHolder> {
        @NonNull
        @Override
        public SongHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout item = new LinearLayout(MainActivity.this);
            item.setGravity(Gravity.CENTER_VERTICAL);
            item.setPadding(dp(7), dp(7), dp(7), dp(7));

            TextView artwork = text("♫", 25, ACCENT);
            artwork.setGravity(Gravity.CENTER);
            artwork.setBackground(background(SURFACE, 12));
            item.addView(artwork, new LinearLayout.LayoutParams(dp(53), dp(53)));

            LinearLayout info = new LinearLayout(MainActivity.this);
            info.setOrientation(LinearLayout.VERTICAL);
            info.setPadding(dp(12), 0, dp(5), 0);
            TextView title = text("", 14, WHITE);
            TextView artist = text("", 12, MUTED);
            title.setMaxLines(1);
            artist.setMaxLines(1);
            info.addView(title);
            info.addView(artist);
            item.addView(info, new LinearLayout.LayoutParams(0, -2, 1));

            TextView favorite = text("♡", 25, WHITE);
            favorite.setGravity(Gravity.CENTER);
            item.addView(favorite, new LinearLayout.LayoutParams(dp(42), dp(48)));
            return new SongHolder(item, title, artist, favorite);
        }

        @Override
        public void onBindViewHolder(@NonNull SongHolder holder, int position) {
            final Song song = visibleSongs.get(position);
            holder.title.setText(song.title);
            holder.artist.setText(song.artist);
            holder.favorite.setText(favorites.contains(song.uri) ? "♥" : "♡");

            holder.favorite.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    toggleFavorite(song);
                    if (adapter != null) adapter.notifyDataSetChanged();
                }
            });
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { playSong(song); }
            });
        }

        @Override public int getItemCount() { return visibleSongs.size(); }
    }

    private static class SongHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView artist;
        TextView favorite;

        SongHolder(View view, TextView titleView, TextView artistView, TextView favoriteView) {
            super(view);
            title = titleView;
            artist = artistView;
            favorite = favoriteView;
        }
    }

    private static class Song {
        String title;
        String artist;
        String uri;
        long id;
        long duration;
        long dateAdded;

        Song(String title, String artist, String uri, long id, long duration, long dateAdded) {
            this.title = title;
            this.artist = artist;
            this.uri = uri;
            this.id = id;
            this.duration = duration;
            this.dateAdded = dateAdded;
        }
    }

    private void requestAudioPermissionIfNeeded() {
        String permission = Build.VERSION.SDK_INT >= 33
                ? Manifest.permission.READ_MEDIA_AUDIO
                : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, permission)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{permission}, REQUEST_AUDIO);
        } else {
            loadSongs();
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(this,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_NOTIFICATIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_AUDIO) {
            String permission = Build.VERSION.SDK_INT >= 33
                    ? Manifest.permission.READ_MEDIA_AUDIO
                    : Manifest.permission.READ_EXTERNAL_STORAGE;
            if (ContextCompat.checkSelfPermission(this, permission)
                    == PackageManager.PERMISSION_GRANTED) {
                loadSongs();
            } else {
                Toast.makeText(this, "Audio permission is required to read your music.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void loadSongs() {
        songs.clear();

        Uri collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.IS_MUSIC
        };

        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0 AND "
                + MediaStore.Audio.Media.DURATION + " >= ?";
        String[] selectionArgs = new String[]{"30000"};

        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(
                    collection, projection, selection, selectionArgs,
                    MediaStore.Audio.Media.DATE_ADDED + " DESC");

            if (cursor != null) {
                int idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                int titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                int artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
                int durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
                int dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED);

                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idCol);
                    String title = cursor.getString(titleCol);
                    String artist = cursor.getString(artistCol);
                    long duration = cursor.getLong(durationCol);
                    long dateAdded = cursor.getLong(dateCol);

                    if (title == null || title.trim().length() == 0) title = "Unknown";
                    if (artist == null || artist.trim().length() == 0
                            || "<unknown>".equalsIgnoreCase(artist)) {
                        artist = "Unknown Artist";
                    }

                    Uri songUri = ContentUris.withAppendedId(collection, id);
                    songs.add(new Song(title, artist, songUri.toString(),
                            id, duration, dateAdded));
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Unable to load music: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        } finally {
            if (cursor != null) cursor.close();
        }

        if ("home".equals(currentPage)) showHome();
        else if ("library".equals(currentPage)) showLibrary();
        else showHome();
    }

    private Song findSong(String uri) {
        if (uri == null) return null;
        for (Song song : songs) {
            if (uri.equals(song.uri)) return song;
        }
        return null;
    }

    private void toggleFavorite(Song song) {
        if (favorites.contains(song.uri)) favorites.remove(song.uri);
        else favorites.add(song.uri);

        preferences.edit()
                .putStringSet("favorites", new HashSet<String>(favorites))
                .apply();

        if ("favorites".equals(currentPage)) showFavorites();
        else if ("playlist".equals(currentPage) && openedPlaylist != null) {
            showPlaylist(openedPlaylist);
        }
    }

    private int getPlayCount(String uri) {
        Integer count = playCounts.get(uri);
        return count == null ? 0 : count;
    }

    private void incrementPlayCount(Song song) {
        playCounts.put(song.uri, getPlayCount(song.uri) + 1);
        savePlayCounts();
    }

    private void playSong(Song song) {
        if (song == null) return;

        currentSong = song;
        incrementPlayCount(song);

        ArrayList<String> queueUris = new ArrayList<String>();
        ArrayList<String> queueTitles = new ArrayList<String>();
        ArrayList<String> queueArtists = new ArrayList<String>();

        ArrayList<Song> queue = songs;
        if (openedPlaylist != null && playlists.get(openedPlaylist) != null) {
            queue = new ArrayList<Song>();
            for (String uri : playlists.get(openedPlaylist)) {
                Song item = findSong(uri);
                if (item != null) queue.add(item);
            }
        }

        int queueIndex = 0;
        playbackQueue.clear();
        playbackQueue.addAll(queue);
        for (int i = 0; i < queue.size(); i++) {
            Song item = queue.get(i);
            queueUris.add(item.uri);
            queueTitles.add(item.title);
            queueArtists.add(item.artist);
            if (item.uri.equals(song.uri)) queueIndex = i;
        }
        playbackQueueIndex = queueIndex;

        Intent intent = new Intent(this, PlaybackService.class);
        intent.setAction(PlaybackService.ACTION_PLAY);
        intent.putExtra("uri", song.uri);
        intent.putExtra("title", song.title);
        intent.putExtra("artist", song.artist);
        intent.putStringArrayListExtra("queue_uris", queueUris);
        intent.putStringArrayListExtra("queue_titles", queueTitles);
        intent.putStringArrayListExtra("queue_artists", queueArtists);
        intent.putExtra("queue_index", queueIndex);

        startPlaybackService(intent);
        isPlaying = true;
        updateMiniPlayer();
    }

    private void startPlaybackService(Intent intent) {
        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    private void togglePlayback() {
        Intent intent = new Intent(this, PlaybackService.class);
        intent.setAction(PlaybackService.ACTION_TOGGLE);
        startService(intent);
    }

    private void nextSong() {
        if (playbackQueue.isEmpty()) {
            if (songs.isEmpty()) return;
            playbackQueue.addAll(songs);
            playbackQueueIndex = currentSong == null ? -1 : songs.indexOf(currentSong);
        }
        playbackQueueIndex = (playbackQueueIndex + 1) % playbackQueue.size();
        playSong(playbackQueue.get(playbackQueueIndex));
    }

    private void previousSong() {
        if (playbackQueue.isEmpty()) {
            if (songs.isEmpty()) return;
            playbackQueue.addAll(songs);
            playbackQueueIndex = currentSong == null ? 0 : songs.indexOf(currentSong);
        }
        playbackQueueIndex = (playbackQueueIndex - 1 + playbackQueue.size()) % playbackQueue.size();
        playSong(playbackQueue.get(playbackQueueIndex));
    }

    private void showPlayer() {
        if (currentSong == null) {
            Toast.makeText(this, "Select a song first.", Toast.LENGTH_SHORT).show();
            return;
        }

        playerExpanded = true;
        currentPage = "player";
        clearContent();
        playerExpanded = true;

        TextView close = text("⌄", 32, WHITE);
        close.setGravity(Gravity.CENTER);
        close.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showHome(); }
        });
        content.addView(close, new LinearLayout.LayoutParams(-1, dp(42)));

        TextView artwork = text("♫", 90, ACCENT);
        artwork.setGravity(Gravity.CENTER);
        artwork.setBackground(background(SURFACE, 28));
        LinearLayout.LayoutParams artParams = new LinearLayout.LayoutParams(-1, 0, 1);
        artParams.setMargins(dp(18), dp(10), dp(18), dp(18));
        content.addView(artwork, artParams);

        TextView title = text(currentSong.title, 22, WHITE);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(null, Typeface.BOLD);
        title.setMaxLines(2);
        content.addView(title);

        TextView artist = text(currentSong.artist, 15, MUTED);
        artist.setGravity(Gravity.CENTER);
        content.addView(artist);

        playerTime = text("00:00 / " + formatTime(currentSong.duration), 11, MUTED);
        playerTime.setGravity(Gravity.CENTER);
        content.addView(playerTime);

        playerSeekBar = new SeekBar(this);
        playerSeekBar.setMax((int) Math.min(Integer.MAX_VALUE, currentSong.duration));
        playerSeekBar.setProgress(0);
        content.addView(playerSeekBar);

        playerSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) { }
            @Override public void onStartTrackingTouch(SeekBar bar) { }
            @Override public void onStopTrackingTouch(SeekBar bar) {
                Intent seekIntent = new Intent(MainActivity.this, PlaybackService.class);
                seekIntent.setAction(PlaybackService.ACTION_SEEK);
                seekIntent.putExtra("position", bar.getProgress());
                startService(seekIntent);
            }
        });

        LinearLayout controls = new LinearLayout(this);
        controls.setGravity(Gravity.CENTER);

        TextView previous = control("⏮");
        final TextView play = control(isPlaying ? "Ⅱ" : "▶");
        TextView next = control("⏭");

        previous.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { previousSong(); }
        });
        play.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { togglePlayback(); }
        });
        next.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { nextSong(); }
        });

        controls.addView(previous);
        controls.addView(play);
        controls.addView(next);
        content.addView(controls);

        TextView favorite = text(favorites.contains(currentSong.uri)
                ? "♥  Added to Favorites" : "♡  Add to Favorites", 15, WHITE);
        favorite.setGravity(Gravity.CENTER);
        favorite.setPadding(0, dp(16), 0, dp(16));
        favorite.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Song selected = currentSong;
                toggleFavorite(selected);
                if (playerExpanded) showPlayer();
            }
        });
        content.addView(favorite);

        final Handler handler = new Handler(Looper.getMainLooper());
        final Runnable updater = new Runnable() {
            @Override public void run() {
                if (!playerExpanded || playerSeekBar == null || playerSeekBar.getParent() == null) return;
                Intent query = new Intent(MainActivity.this, PlaybackService.class);
                query.setAction(PlaybackService.ACTION_QUERY);
                startService(query);
                if (playerSeekBar != null) playerSeekBar.setContentDescription(playerTime == null ? "" : playerTime.getText());
                handler.postDelayed(this, 700);
            }
        };
        handler.post(updater);

        // The service broadcasts its current position in playback updates.
        playerSeekBar.setTag(playerTime);
    }

    private void refreshPlayerState() {
        updateMiniPlayer();
        if (content == null) return;
        for (int i = 0; i < content.getChildCount(); i++) {
            View child = content.getChildAt(i);
            if (child instanceof LinearLayout) {
                LinearLayout layout = (LinearLayout) child;
                for (int j = 0; j < layout.getChildCount(); j++) {
                    View control = layout.getChildAt(j);
                    if (control instanceof TextView
                            && ("▶".equals(((TextView) control).getText())
                            || "Ⅱ".equals(((TextView) control).getText()))) {
                        ((TextView) control).setText(isPlaying ? "Ⅱ" : "▶");
                    }
                }
            }
        }
    }

    private TextView control(String value) {
        TextView view = text(value, 30, WHITE);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(14), dp(12), dp(14), dp(12));
        return view;
    }

    private String formatTime(long millis) {
        long totalSeconds = Math.max(0, millis / 1000);
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format(Locale.ROOT, "%02d:%02d", minutes, seconds);
    }

    private void showSettings() {
        currentPage = "settings";
        clearContent();
        addPageTitle("Settings");

        TextView info = text("MFlow\n\nLocal Music Player", 16, MUTED);
        info.setPadding(dp(5), dp(20), dp(5), dp(20));
        content.addView(info);

        Button about = button("About MFlow");
        content.addView(about);
        about.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("MFlow")
                        .setMessage("Local Music Player")
                        .setPositiveButton("OK", null)
                        .show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        try {
            unregisterReceiver(playbackReceiver);
        } catch (Exception ignored) {
        }
        // Playback is intentionally not stopped when the Activity is destroyed.
        super.onDestroy();
    }

    public static class PlaybackService extends Service {

        public static final String ACTION_PLAY = "com.mflow.music.PLAY";
        public static final String ACTION_TOGGLE = "com.mflow.music.TOGGLE";
        public static final String ACTION_PAUSE = "com.mflow.music.PAUSE";
        public static final String ACTION_NEXT = "com.mflow.music.NEXT";
        public static final String ACTION_PREVIOUS = "com.mflow.music.PREVIOUS";
        public static final String ACTION_SEEK = "com.mflow.music.SEEK";
        public static final String ACTION_QUERY = "com.mflow.music.QUERY";

        private static final int NOTIFICATION_ID = 77;
        private static final String CHANNEL_ID = "mflow_playback";

        private MediaPlayer player;
        private String uri;
        private String title;
        private String artist;
        private boolean playing = false;
        private boolean prepared = false;

        private final ArrayList<String> queueUris = new ArrayList<String>();
        private final ArrayList<String> queueTitles = new ArrayList<String>();
        private final ArrayList<String> queueArtists = new ArrayList<String>();
        private int queueIndex = 0;

        @Override
        public void onCreate() {
            super.onCreate();
            createNotificationChannel();

        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            if (intent == null) {
                stopSelf(startId);
                return START_NOT_STICKY;
            }

            String action = intent.getAction();

            if (ACTION_PLAY.equals(action)) {
                uri = intent.getStringExtra("uri");
                title = intent.getStringExtra("title");
                artist = intent.getStringExtra("artist");

                ArrayList<String> uris = intent.getStringArrayListExtra("queue_uris");
                ArrayList<String> titles = intent.getStringArrayListExtra("queue_titles");
                ArrayList<String> artists = intent.getStringArrayListExtra("queue_artists");

                queueUris.clear();
                queueTitles.clear();
                queueArtists.clear();

                if (uris != null) queueUris.addAll(uris);
                if (titles != null) queueTitles.addAll(titles);
                if (artists != null) queueArtists.addAll(artists);
                queueIndex = intent.getIntExtra("queue_index", 0);

                startAsForeground();
                startTrack();
            } else if (ACTION_TOGGLE.equals(action)) {
                if (playing) pause();
                else resume();
            } else if (ACTION_PAUSE.equals(action)) {
                pause();
            } else if (ACTION_NEXT.equals(action)) {
                playNext();
            } else if (ACTION_PREVIOUS.equals(action)) {
                playPrevious();
            } else if (ACTION_SEEK.equals(action)) {
                seekTo(intent.getIntExtra("position", 0));
            } else if (ACTION_QUERY.equals(action)) {
                sendUpdate();
            }

            return START_NOT_STICKY;
        }

        private void startAsForeground() {
            Notification notification = buildNotification();
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(NOTIFICATION_ID, notification,
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }
        }

        private void startTrack() {
            if (uri == null) {
                stopSelf();
                return;
            }

            releasePlayer();
            prepared = false;
            playing = false;
            showNotification();
            sendUpdate();

            try {
                player = new MediaPlayer();
                player.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build());

                player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override public void onPrepared(MediaPlayer mp) {
                        prepared = true;
                        mp.start();
                        playing = true;
                        showNotification();
                        sendUpdate();
                    }
                });

                player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override public void onCompletion(MediaPlayer mp) {
                        playing = false;
                        playNext();
                    }
                });

                player.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                    @Override public boolean onError(MediaPlayer mp, int what, int extra) {
                        playing = false;
                        prepared = false;
                        showNotification();
                        sendUpdate();
                        return true;
                    }
                });

                player.setDataSource(this, Uri.parse(uri));
                player.prepareAsync();
            } catch (Exception e) {
                playing = false;
                prepared = false;
                showNotification();
                sendUpdate();
                stopForeground(false);
                stopSelf();
            }
        }

        private void resume() {
            if (player != null && prepared && !player.isPlaying()) {
                try {
                    player.start();
                    playing = true;
                    showNotification();
                    sendUpdate();
                } catch (Exception ignored) {
                }
            }
        }

        private void pause() {
            if (player != null && prepared && player.isPlaying()) {
                try {
                    player.pause();
                    playing = false;
                    showNotification();
                    sendUpdate();
                } catch (Exception ignored) {
                }
            }
        }

        private void seekTo(int position) {
            if (player == null || !prepared) return;
            try {
                if (Build.VERSION.SDK_INT >= 26) {
                    player.seekTo(position, MediaPlayer.SEEK_CLOSEST);
                } else {
                    player.seekTo(position);
                }
                sendUpdate();
            } catch (Exception ignored) {
            }
        }

        private void playNext() {
            if (queueUris.isEmpty()) {
                stopPlayback();
                return;
            }
            queueIndex = (queueIndex + 1) % queueUris.size();
            loadQueueItem();
        }

        private void playPrevious() {
            if (queueUris.isEmpty()) {
                stopPlayback();
                return;
            }
            queueIndex = (queueIndex - 1 + queueUris.size()) % queueUris.size();
            loadQueueItem();
        }

        private void loadQueueItem() {
            if (queueIndex < 0 || queueIndex >= queueUris.size()) return;
            uri = queueUris.get(queueIndex);
            title = queueIndex < queueTitles.size() ? queueTitles.get(queueIndex) : "Unknown";
            artist = queueIndex < queueArtists.size() ? queueArtists.get(queueIndex) : "Unknown Artist";
            startAsForeground();
            startTrack();
        }

        private void stopPlayback() {
            playing = false;
            prepared = false;
            releasePlayer();
            showNotification();
            sendUpdate();
            stopForeground(false);
            stopSelf();
        }

        private void releasePlayer() {
            if (player != null) {
                try {
                    player.setOnPreparedListener(null);
                    player.setOnCompletionListener(null);
                    player.setOnErrorListener(null);
                    player.reset();
                    player.release();
                } catch (Exception ignored) {
                }
                player = null;
            }
        }

        private void createNotificationChannel() {
            if (Build.VERSION.SDK_INT >= 26) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID, "MFlow Playback", NotificationManager.IMPORTANCE_LOW);
                channel.setDescription("Music playback controls");
                channel.setShowBadge(false);
                NotificationManager manager =
                        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (manager != null) manager.createNotificationChannel(channel);
            }
        }

        private PendingIntent servicePendingIntent(String action, int requestCode) {
            Intent intent = new Intent(this, PlaybackService.class);
            intent.setAction(action);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
            return PendingIntent.getService(this, requestCode, intent, flags);
        }

        private Notification buildNotification() {
            Intent openIntent = new Intent(this, MainActivity.class);
            openIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= 23) pendingFlags |= PendingIntent.FLAG_IMMUTABLE;
            PendingIntent contentIntent = PendingIntent.getActivity(
                    this, 0, openIntent, pendingFlags);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_media_play)
                    .setContentTitle(title == null ? "MFlow" : title)
                    .setContentText(artist == null ? "Music playback" : artist)
                    .setContentIntent(contentIntent)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setOnlyAlertOnce(true)
                    .setOngoing(playing)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .addAction(android.R.drawable.ic_media_previous, "Previous",
                            servicePendingIntent(ACTION_PREVIOUS, 1))
                    .addAction(playing ? android.R.drawable.ic_media_pause
                                    : android.R.drawable.ic_media_play,
                            playing ? "Pause" : "Play",
                            servicePendingIntent(ACTION_TOGGLE, 2))
                    .addAction(android.R.drawable.ic_media_next, "Next",
                            servicePendingIntent(ACTION_NEXT, 3));

            return builder.build();
        }

        private void showNotification() {
            NotificationManager manager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) manager.notify(NOTIFICATION_ID, buildNotification());
        }

        private void sendUpdate() {
            Intent intent = new Intent(ACTION_PLAYBACK_UPDATE);
            intent.setPackage(getPackageName());
            intent.putExtra("playing", playing);
            intent.putExtra("uri", uri);
            int position = 0;
            int duration = 0;
            if (player != null && prepared) {
                try {
                    position = player.getCurrentPosition();
                    duration = player.getDuration();
                } catch (Exception ignored) {
                }
            }
            intent.putExtra("position", position);
            intent.putExtra("duration", duration);
            sendBroadcast(intent);
        }

        @Override
        public void onDestroy() {
            playing = false;
            releasePlayer();
            super.onDestroy();
        }

        @Nullable
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }
    }
}

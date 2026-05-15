package com.example.android.Stoonify;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.util.UnstableApi;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@UnstableApi
public class MainActivity extends AppCompatActivity implements MusicPlayerManager.PlayerListener {

    private List<Song> songsList;
    private SongRecyclerAdapter adapter;
    private MusicPlayerManager playerManager;
    private RecyclerView recyclerView;
    
    private RelativeLayout lowerBar;
    private TextView barSongName;
    private TextView barArtistName;
    private ImageButton barPlayButton;
    private boolean isGridView = false;

    @OptIn(markerClass = UnstableApi.class)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        songsList = new ArrayList<>();
        playerManager = MusicPlayerManager.getInstance(this);
        playerManager.addListener(this);

        recyclerView = findViewById(R.id.recycler_view);
        lowerBar = findViewById(R.id.main_lower_bar);
        barSongName = findViewById(R.id.main_song_name);
        barArtistName = findViewById(R.id.main_artist_name);
        barPlayButton = findViewById(R.id.main_play_button);
        ImageButton barNextButton = findViewById(R.id.main_next_button);

        setupRecyclerView();

        fetchSongs();

        barPlayButton.setOnClickListener(v -> playerManager.togglePlayback());
        barNextButton.setOnClickListener(v -> playerManager.playNext());
        lowerBar.setOnClickListener(v -> navigateToNowPlaying());

        updateBottomBar();
    }

    private void setupRecyclerView() {
        if (isGridView) {
            recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        } else {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
        }
        
        adapter = new SongRecyclerAdapter(this, songsList, isGridView, position -> {
            if (playerManager.getCurrentIndex() == position) {
                navigateToNowPlaying();
            } else {
                playerManager.setPlaylist(songsList);
                playerManager.playSong(position);
                navigateToNowPlaying();
            }
        });
        recyclerView.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_view_list) {
            if (isGridView) {
                isGridView = false;
                setupRecyclerView();
            }
            return true;
        } else if (id == R.id.action_view_grid) {
            if (!isGridView) {
                isGridView = true;
                setupRecyclerView();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void navigateToNowPlaying() {
        Intent intent = new Intent(MainActivity.this, NowPlaying.class);
        startActivity(intent);
    }

    private void updateBottomBar() {
        Song currentSong = playerManager.getCurrentSong();
        if (currentSong != null) {
            lowerBar.setVisibility(View.VISIBLE);
            barSongName.setText(currentSong.getName());
            barArtistName.setText(currentSong.getArtist());
            barPlayButton.setImageResource(playerManager.isPlaying() ? 
                R.drawable.ic_pause_circle_outline_white_64dp : 
                R.drawable.ic_play_circle_outline_white_64dp);
        } else {
            lowerBar.setVisibility(View.GONE);
        }
    }

    private void fetchSongs() {
        SongApiService apiService = RetrofitClient.getClient().create(SongApiService.class);
        Call<List<Song>> call = apiService.getSongs();
        call.enqueue(new Callback<List<Song>>() {
            @Override
            public void onResponse(Call<List<Song>> call, Response<List<Song>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    songsList.clear();
                    songsList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                    playerManager.setPlaylist(songsList);
                }
            }

            @Override
            public void onFailure(Call<List<Song>> call, Throwable t) {
                songsList.add(new Song("Allah Kebeer", "Fayrouz", 256, R.drawable.fayrouz));
                songsList.add(new Song("Habibty", "Amr Diab", 227, R.drawable.amr_diab));
                adapter.notifyDataSetChanged();
                playerManager.setPlaylist(songsList);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateBottomBar();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        playerManager.removeListener(this);
    }

    @Override
    public void onSongChanged(Song song) {
        updateBottomBar();
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onPlaybackStatusChanged(boolean isPlaying) {
        barPlayButton.setImageResource(isPlaying ? 
            R.drawable.ic_pause_circle_outline_white_64dp : 
            R.drawable.ic_play_circle_outline_white_64dp);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onPrepared(int duration) {
        updateBottomBar();
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onCompletion() {
        updateBottomBar();
        adapter.notifyDataSetChanged();
    }
}

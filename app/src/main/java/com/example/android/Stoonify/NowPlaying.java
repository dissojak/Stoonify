package com.example.android.Stoonify;

import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.util.UnstableApi;

import com.bumptech.glide.Glide;

import java.util.Locale;


@UnstableApi public class NowPlaying extends AppCompatActivity implements MusicPlayerManager.PlayerListener {

    private MusicPlayerManager playerManager;
    private SeekBar seekBar;
    private TextView currentDurationTV;
    private TextView totalDurationTV;
    private TextView songNameTV;
    private TextView artistNameTV;
    private ImageView image;
    private ImageButton playButton;
    
    private Handler handler = new Handler();
    private Runnable updateSeekBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_now_playing);

        playerManager = MusicPlayerManager.getInstance();
        playerManager.addListener(this);

        image = findViewById(R.id.now_playing_song_image);
        songNameTV = findViewById(R.id.now_playing_song_name);
        artistNameTV = findViewById(R.id.now_playing_artist_name);
        currentDurationTV = findViewById(R.id.now_playing_current_duration);
        totalDurationTV = findViewById(R.id.now_playing_total_duration);
        seekBar = findViewById(R.id.now_playing_seek_bar);
        playButton = findViewById(R.id.now_playing_play_button);
        ImageButton nextButton = findViewById(R.id.now_playing_next_button);
        ImageButton prevButton = findViewById(R.id.now_playing_prev_button);

        playButton.setOnClickListener(v -> playerManager.togglePlayback());
        nextButton.setOnClickListener(v -> playerManager.playNext());
        prevButton.setOnClickListener(v -> playerManager.playPrevious());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    playerManager.seekTo(progress);
                    currentDurationTV.setText(formatTime(progress));
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                handler.removeCallbacks(updateSeekBar);
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (playerManager.isPlaying()) {
                    startSeekBarUpdate();
                }
            }
        });

        updateUI(playerManager.getCurrentSong());
        if (playerManager.isPlaying()) {
            startSeekBarUpdate();
        }
    }

    private void updateUI(Song song) {
        if (song == null) return;
        songNameTV.setText(song.getName());
        artistNameTV.setText(song.getArtist());
        
        if (song.getImageUrl() != null && !song.getImageUrl().isEmpty()) {
            Glide.with(this).load(song.getImageUrl()).placeholder(R.drawable.music).into(image);
        } else if (song.getImgResId() != -1) {
            image.setImageResource(song.getImgResId());
        }

        playButton.setImageResource(playerManager.isPlaying() ? 
            R.drawable.ic_pause_black_84dp : R.drawable.ic_play_arrow_black_84dp);
        
        int duration = playerManager.getDuration();
        if (duration > 0) {
            seekBar.setMax(duration);
            totalDurationTV.setText(formatTime(duration));
        }
    }

    private void startSeekBarUpdate() {
        if (updateSeekBar == null) {
            updateSeekBar = new Runnable() {
                @Override
                public void run() {
                    int currentPosition = playerManager.getCurrentPosition();
                    seekBar.setProgress(currentPosition);
                    currentDurationTV.setText(formatTime(currentPosition));
                    handler.postDelayed(this, 1000);
                }
            };
        }
        handler.removeCallbacks(updateSeekBar);
        handler.post(updateSeekBar);
    }

    private String formatTime(int milliseconds) {
        int seconds = milliseconds / 1000;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        playerManager.removeListener(this);
        handler.removeCallbacks(updateSeekBar);
    }

    @Override
    public void onSongChanged(Song song) {
        updateUI(song);
    }

    @Override
    public void onPlaybackStatusChanged(boolean isPlaying) {
        playButton.setImageResource(isPlaying ? 
            R.drawable.ic_pause_black_84dp : R.drawable.ic_play_arrow_black_84dp);
        if (isPlaying) {
            startSeekBarUpdate();
        } else {
            handler.removeCallbacks(updateSeekBar);
        }
    }

    @Override
    public void onPrepared(int duration) {
        seekBar.setMax(duration);
        totalDurationTV.setText(formatTime(duration));
        startSeekBarUpdate();
    }

    @Override
    public void onCompletion() {
        playButton.setImageResource(R.drawable.ic_play_arrow_black_84dp);
        seekBar.setProgress(0);
        currentDurationTV.setText(formatTime(0));
        handler.removeCallbacks(updateSeekBar);
    }
}

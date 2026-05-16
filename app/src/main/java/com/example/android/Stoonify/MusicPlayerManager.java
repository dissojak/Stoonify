package com.example.android.Stoonify;

import android.content.Context;
import android.net.Uri;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.datasource.cache.CacheDataSource;
import androidx.media3.datasource.cache.SimpleCache;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.LoadControl;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import java.util.ArrayList;
import java.util.List;





@UnstableApi

// this a sengleton player to ensure not hving 2 songs gonna play at the sane time
public class MusicPlayerManager {
    private static final String TAG = "MusicPlayerManager";
    private static MusicPlayerManager instance;
    private ExoPlayer exoPlayer;
    private List<Song> playlist = new ArrayList<>();
    private int currentIndex = -1;
    private int retryCount = 0;
    private static final int MAX_RETRIES = 1;

    public interface PlayerListener {
        void onSongChanged(Song song);
        void onPlaybackStatusChanged(boolean isPlaying);
        void onPrepared(int duration);
        void onCompletion();
    }

    private List<PlayerListener> listeners = new ArrayList<>();

    private MusicPlayerManager(Context context) {
        SimpleCache cache = MusicApp.getCache(context);
        DefaultHttpDataSource.Factory httpDataSourceFactory = new DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(30000);

        DataSource.Factory cacheDataSourceFactory = new CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(httpDataSourceFactory)
            .setCacheWriteDataSinkFactory(new CacheDataSink.Factory().setCache(cache))
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);

        // Smaller buffers reduce startup latency on local streaming
        LoadControl loadControl = new DefaultLoadControl.Builder()
            .setBufferDurationsMs(10000, 30000, 250, 1000)
            .setPrioritizeTimeOverSizeThresholds(true)
                .build();

        exoPlayer = new ExoPlayer.Builder(context)
                .setMediaSourceFactory(new DefaultMediaSourceFactory(context)
            .setDataSourceFactory(cacheDataSourceFactory))
                .setLoadControl(loadControl)
                .build();

        setupListeners();
    }

    public static synchronized MusicPlayerManager getInstance(Context context) {
        if (instance == null) {
            instance = new MusicPlayerManager(context.getApplicationContext());
        }
        return instance;
    }

    public static MusicPlayerManager getInstance() {
        return instance;
    }

    private void setupListeners() {
        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_READY) {
                    retryCount = 0;
                    for (PlayerListener listener : listeners) {
                        listener.onPrepared((int) exoPlayer.getDuration());
                    }
                } else if (playbackState == Player.STATE_ENDED) {
                    for (PlayerListener listener : listeners) {
                        listener.onCompletion();
                    }
                    playNext();
                }
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                for (PlayerListener listener : listeners) {
                    listener.onPlaybackStatusChanged(isPlaying);
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                if (retryCount < MAX_RETRIES) {
                    retryCount++;
                    reloadCurrent();
                }
            }
        });
    }

    public void setPlaylist(List<Song> songs) {
        this.playlist = songs;
    }

    public void playSong(int index) {
        if (index < 0 || index >= playlist.size()) return;
        currentIndex = index;
        retryCount = 0;
        Song song = playlist.get(currentIndex);
        
        // Crucial: mediaId must be constant for the cache to recognize the file
        MediaItem mediaItem = new MediaItem.Builder()
                .setUri(Uri.parse(song.getAudioUrl()))
                .setMediaId(song.getAudioUrl())
                .build();
        
        exoPlayer.stop();
        exoPlayer.clearMediaItems();
        exoPlayer.setMediaItem(mediaItem);
        exoPlayer.prepare();
        exoPlayer.play();
        
        for (PlayerListener listener : listeners) {
            listener.onSongChanged(song);
        }
    }

    public void togglePlayback() {
        if (exoPlayer.getPlaybackState() == Player.STATE_IDLE) {
            exoPlayer.prepare();
        }
        if (exoPlayer.isPlaying()) {
            exoPlayer.pause();
        } else {
            exoPlayer.play();
        }
    }

    public void playNext() {
        if (playlist.isEmpty()) return;
        int nextIndex = (currentIndex + 1) % playlist.size();
        playSong(nextIndex);
    }

    public void playPrevious() {
        if (playlist.isEmpty()) return;
        int prevIndex = (currentIndex - 1 + playlist.size()) % playlist.size();
        playSong(prevIndex);
    }

    public void seekTo(int position) {
        exoPlayer.seekTo(position);
    }

    public int getCurrentPosition() {
        return (int) exoPlayer.getCurrentPosition();
    }

    public int getDuration() {
        long duration = exoPlayer.getDuration();
        return duration == androidx.media3.common.C.TIME_UNSET ? 0 : (int) duration;
    }

    public boolean isPlaying() {
        return exoPlayer.isPlaying();
    }

    public Song getCurrentSong() {
        if (currentIndex >= 0 && currentIndex < playlist.size()) {
            return playlist.get(currentIndex);
        }
        return null;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public void addListener(PlayerListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(PlayerListener listener) {
        listeners.remove(listener);
    }

    private void reloadCurrent() {
        if (currentIndex < 0 || currentIndex >= playlist.size()) return;
        Song song = playlist.get(currentIndex);
        MediaItem mediaItem = new MediaItem.Builder()
                .setUri(Uri.parse(song.getAudioUrl()))
                .setMediaId(song.getAudioUrl())
                .build();

        exoPlayer.stop();
        exoPlayer.clearMediaItems();
        exoPlayer.setMediaItem(mediaItem);
        exoPlayer.prepare();
        exoPlayer.play();
    }
}

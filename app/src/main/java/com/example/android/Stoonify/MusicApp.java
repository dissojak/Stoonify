package com.example.android.Stoonify;

import android.app.Application;
import android.content.Context;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.database.StandaloneDatabaseProvider;
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor;
import androidx.media3.datasource.cache.SimpleCache;
import java.io.File;

@UnstableApi
public class MusicApp extends Application {
    private static SimpleCache songCache;

    public static SimpleCache getCache(Context context) {
        if (songCache == null) {
            File cacheDir = new File(context.getCacheDir(), "song_cache");
            songCache = new SimpleCache(cacheDir, 
                new LeastRecentlyUsedCacheEvictor(100 * 1024 * 1024), 
                new StandaloneDatabaseProvider(context));
        }
        return songCache;
    }
}

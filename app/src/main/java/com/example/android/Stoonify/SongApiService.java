package com.example.android.Stoonify;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;

public interface SongApiService {
    @GET("songs")
    Call<List<Song>> getSongs();
}

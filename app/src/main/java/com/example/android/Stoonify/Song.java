package com.example.android.Stoonify;

import com.google.gson.annotations.SerializedName;

public class Song {

    @SerializedName("title")
    private String name;
    
    @SerializedName("artist")
    private String artist;
    
    @SerializedName("duration")
    private int duration;
    
    @SerializedName("imageUrl")
    private String imageUrl;
    
    @SerializedName("audioUrl")
    private String audioUrl;

    private final static int NO_IMAGE_PROVIDED = -1;
    private int imgResId = NO_IMAGE_PROVIDED;

    public Song(String name, String artist, int duration, int imgResId) {
        this.name = name;
        this.artist = artist;
        this.duration = duration;
        this.imgResId = imgResId;
    }

    public String getName() {
        return name;
    }

    public String getArtist() {
        return artist;
    }

    public int getDuration() {
        return duration;
    }

    public int getImgResId() {
        return imgResId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public boolean hasImage() {
        return (imgResId != NO_IMAGE_PROVIDED) || (imageUrl != null && !imageUrl.isEmpty());
    }
}

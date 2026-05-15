package com.example.android.Stoonify;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.media3.common.util.UnstableApi;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

@UnstableApi
public class SongRecyclerAdapter extends RecyclerView.Adapter<SongRecyclerAdapter.SongViewHolder> {

    private static final int TYPE_LIST = 0;
    private static final int TYPE_GRID = 1;

    private Context context;
    private List<Song> songs;
    private boolean isGridView;
    private MusicPlayerManager playerManager;
    private OnSongClickListener listener;

    public interface OnSongClickListener {
        void onSongClick(int position);
    }

    public SongRecyclerAdapter(Context context, List<Song> songs, boolean isGridView, OnSongClickListener listener) {
        this.context = context;
        this.songs = songs;
        this.isGridView = isGridView;
        this.listener = listener;
        this.playerManager = MusicPlayerManager.getInstance();
    }

    @Override
    public int getItemViewType(int position) {
        return isGridView ? TYPE_GRID : TYPE_LIST;
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = (viewType == TYPE_GRID) ? R.layout.song_grid_item : R.layout.song_list_item;
        View view = LayoutInflater.from(context).inflate(layoutId, parent, false);
        return new SongViewHolder(view, viewType);
    }

    @OptIn(markerClass = UnstableApi.class)
    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        Song currentSong = songs.get(position);
        
        holder.songName.setText(currentSong.getName());
        holder.artistName.setText(currentSong.getArtist());

        String imageUrl = currentSong.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(context).load(imageUrl).placeholder(R.drawable.music).into(holder.image);
        } else if (currentSong.getImgResId() != -1) {
            holder.image.setImageResource(currentSong.getImgResId());
        } else {
            holder.image.setImageResource(R.drawable.music);
        }

        Song playingSong = playerManager.getCurrentSong();
        if (playingSong != null && playingSong.getName().equals(currentSong.getName())) {
            holder.equalizer.setVisibility(View.VISIBLE);
            AnimationDrawable animation = (AnimationDrawable) holder.equalizer.getDrawable();
            if (playerManager.isPlaying()) animation.start();
            else animation.stop();
        } else {
            holder.equalizer.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> listener.onSongClick(position));
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    static class SongViewHolder extends RecyclerView.ViewHolder {
        ImageView image, equalizer;
        TextView songName, artistName;

        SongViewHolder(View itemView, int viewType) {
            super(itemView);
            if (viewType == TYPE_GRID) {
                image = itemView.findViewById(R.id.grid_item_song_image);
                equalizer = itemView.findViewById(R.id.grid_item_equalizer);
                songName = itemView.findViewById(R.id.grid_item_song_name);
                artistName = itemView.findViewById(R.id.grid_item_artist_name);
            } else {
                image = itemView.findViewById(R.id.list_item_song_image);
                equalizer = itemView.findViewById(R.id.list_item_equalizer);
                songName = itemView.findViewById(R.id.list_item_song_name);
                artistName = itemView.findViewById(R.id.list_item_artist_name);
            }
        }
    }
}

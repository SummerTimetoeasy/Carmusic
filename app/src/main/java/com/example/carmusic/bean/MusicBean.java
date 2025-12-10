package com.example.carmusic.bean;

import java.io.Serializable;

public class MusicBean implements Serializable {
    private String title;
    private String artist;
    private String path;   // 这里的 path 实际上存的是 content:// uri 字符串
    private long duration;
    private long albumResId; // 专辑封面ID

    public MusicBean() {
    }

    public MusicBean(String title, String artist, String path, long duration, long albumResId) {
        this.title = title;
        this.artist = artist;
        this.path = path;
        this.duration = duration;
        this.albumResId = albumResId;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }

    public long getAlbumResId() { return albumResId; }
    public void setAlbumResId(long albumResId) { this.albumResId = albumResId; }
}
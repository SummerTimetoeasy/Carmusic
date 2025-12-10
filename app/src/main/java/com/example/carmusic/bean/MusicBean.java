package com.example.carmusic.bean;

public class MusicBean {
    private String title;
    private String artist;
    private String path;
    private long duration;
    private long albumId; // 新增

    // 必须有这5个参数的构造方法
    public MusicBean(String title, String artist, String path, long duration, long albumId) {
        this.title = title;
        this.artist = artist;
        this.path = path;
        this.duration = duration;
        this.albumId = albumId;
    }

    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public String getPath() { return path; }
    public long getDuration() { return duration; }
    public long getAlbumId() { return albumId; }
}
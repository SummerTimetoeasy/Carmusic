package com.example.carmusic.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import com.example.carmusic.bean.MusicBean;
import java.util.ArrayList;
import java.util.List;

public class MusicService extends Service {
    private MediaPlayer mediaPlayer = new MediaPlayer();
    private List<MusicBean> playlist = new ArrayList<>();
    private int currentPosition = -1;
    private final IBinder binder = new MusicBinder();
    private Runnable onStateChange;

    public class MusicBinder extends Binder {
        public MusicService getService() { return MusicService.this; }
    }

    @Override
    public IBinder onBind(Intent intent) { return binder; }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        mediaPlayer.setOnCompletionListener(mp -> playNext());
    }

    public void setPlaylist(List<MusicBean> list) {
        this.playlist = list;
    }

    public void play(int pos) {
        if (playlist.isEmpty() || pos < 0 || pos >= playlist.size()) return;
        currentPosition = pos;

        try {
            mediaPlayer.reset(); // 重置播放器状态

            String path = playlist.get(pos).getPath();

            // 【关键修复】区分加载方式
            if (path.startsWith("android.resource://")) {
                // 如果是 Raw 内置资源，必须用 Context + Uri 方式加载
                mediaPlayer.setDataSource(getApplicationContext(), android.net.Uri.parse(path));
            } else {
                // 如果是手机本地文件，直接用路径加载
                mediaPlayer.setDataSource(path);
            }

            // 先设置监听器
            mediaPlayer.setOnPreparedListener(mp -> {
                mp.start(); // 【关键】准备好后立即播放
                showNotification(playlist.get(pos));
                if (onStateChange != null) onStateChange.run();
            });

            // 再开始异步准备
            mediaPlayer.prepareAsync();

        } catch (Exception e) {
            e.printStackTrace();
            // 如果出错，打印日志
        }
    }

    public void pause() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            if (onStateChange != null) onStateChange.run();
        }
    }

    public void resume() {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            if (onStateChange != null) onStateChange.run();
        }
    }

    public void playNext() {
        play((currentPosition + 1) % playlist.size());
    }

    public void playPrev() {
        int pos = currentPosition - 1;
        if (pos < 0) pos = playlist.size() - 1;
        play(pos);
    }

    // --- 新增功能：进度控制 ---
    public int getCurrentProgress() {
        if (mediaPlayer != null && (mediaPlayer.isPlaying() || currentPosition != -1)) {
            return mediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    public int getDuration() {
        if (mediaPlayer != null && (mediaPlayer.isPlaying() || currentPosition != -1)) {
            return mediaPlayer.getDuration();
        }
        return 0;
    }

    public void seekTo(int progress) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(progress);
        }
    }
    // -----------------------

    public boolean isPlaying() { return mediaPlayer.isPlaying(); }
    public MusicBean getCurrentMusic() {
        return (currentPosition != -1) ? playlist.get(currentPosition) : null;
    }
    public void setOnStateChange(Runnable action) { this.onStateChange = action; }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(new NotificationChannel("music_ch", "Music", NotificationManager.IMPORTANCE_LOW));
        }
    }

    private void showNotification(MusicBean music) {
        Notification notification = new NotificationCompat.Builder(this, "music_ch")
                .setContentTitle(music.getTitle())
                .setContentText(music.getArtist())
                .setSmallIcon(android.R.drawable.ic_media_play)
                .build();
        startForeground(1, notification);
    }
}
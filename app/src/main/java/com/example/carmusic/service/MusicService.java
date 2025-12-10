package com.example.carmusic.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import com.example.carmusic.bean.MusicBean;
import java.util.ArrayList;
import java.util.List;

public class MusicService extends Service {
    private MediaPlayer mediaPlayer; // 建议不要直接 new，放在 onCreate 初始化
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
        mediaPlayer = new MediaPlayer(); // 在这里初始化更安全
        createNotificationChannel();
        mediaPlayer.setOnCompletionListener(mp -> playNext());
    }

    public void setPlaylist(List<MusicBean> list) {
        this.playlist = list;
    }

    // ✅ 核心修改：统一使用 Uri 加载，解决 setDataSource 崩溃
    public void play(int pos) {
        if (playlist.isEmpty() || pos < 0 || pos >= playlist.size()) return;
        currentPosition = pos;

        try {
            mediaPlayer.reset();

            String path = playlist.get(pos).getPath();
            Uri contentUri = Uri.parse(path); // 将字符串转回 Uri

            // 无论是 android.resource 还是 content://，都用这个方法加载
            mediaPlayer.setDataSource(getApplicationContext(), contentUri);

            mediaPlayer.setOnPreparedListener(mp -> {
                mp.start();
                showNotification(playlist.get(pos));
                if (onStateChange != null) onStateChange.run();
            });

            // 增加错误监听，防止坏文件导致闪退
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                // 这里可以自动切下一首，或者停止
                return true; // 返回 true 表示由于错误已被处理，不会崩溃
            });

            mediaPlayer.prepareAsync();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- 以下代码保持不变 ---
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
        // 加入防衛語句：如果播放列表是空的或為 null，直接返回，不做任何事
        if (playlist == null || playlist.isEmpty()) {
            return;
        }

        // 列表確認有內容後，再執行計算
        play((currentPosition + 1) % playlist.size());
    }

    public void playPrev() {
        int pos = currentPosition - 1;
        if (pos < 0) pos = playlist.size() - 1;
        play(pos);
    }

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

    public boolean isPlaying() { return mediaPlayer != null && mediaPlayer.isPlaying(); }

    public MusicBean getCurrentMusic() {
        return (currentPosition != -1 && currentPosition < playlist.size()) ? playlist.get(currentPosition) : null;
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
                .setOngoing(true) // 设置为常驻通知
                .build();
        startForeground(1, notification);
    }
    private void loadMusic() {
        // ... 读取音乐的代码 ...

        // 在读取结束后打印一下数量
        Log.d("MusicService", "加载完成，共找到歌曲数量: " + playlist.size());
    }
}
package com.example.carmusic.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import com.example.carmusic.bean.MusicBean;
import java.util.ArrayList;
import java.util.List;

public class MusicService extends Service {
    private MediaPlayer mediaPlayer;
    private List<MusicBean> playlist = new ArrayList<>();
    private int currentPosition = -1;
    private final IBinder binder = new MusicBinder();

    // 回调接口
    private Runnable onStateChange;
    private Runnable onPlaylistLoaded;

    public class MusicBinder extends Binder {
        public MusicService getService() { return MusicService.this; }
    }

    @Override
    public IBinder onBind(Intent intent) { return binder; }

    @Override
    public void onCreate() {
        super.onCreate();
        mediaPlayer = new MediaPlayer();
        createNotificationChannel();

        // 监听播放结束，自动下一首
        mediaPlayer.setOnCompletionListener(mp -> playNext());

        // 监听播放错误，防止闪退
        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            Log.e("MusicService", "Play Error, skipping...");
            playNext();
            return true;
        });

        // 启动时自动加载音乐
        loadMusic();
    }

    // 核心：扫描手机音乐
    private void loadMusic() {
        new Thread(() -> {
            playlist.clear();
            Cursor cursor = null;
            try {
                cursor = getContentResolver().query(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        null,
                        MediaStore.Audio.Media.IS_MUSIC + "!=0",
                        null,
                        null
                );

                if (cursor != null) {
                    int idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                    int titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                    int artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
                    int durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
                    int albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);

                    while (cursor.moveToNext()) {
                        long id = cursor.getLong(idCol);
                        String title = cursor.getString(titleCol);
                        String artist = cursor.getString(artistCol);
                        long duration = cursor.getLong(durCol);
                        long albumId = cursor.getLong(albumIdCol);

                        // 转换成 Uri 字符串存储，适配 Android 10+
                        Uri contentUri = ContentUris.withAppendedId(
                                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);

                        playlist.add(new MusicBean(title, artist, contentUri.toString(), duration, albumId));
                    }
                    cursor.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            Log.d("MusicService", "Loaded songs: " + playlist.size());

            // 通知 UI 列表加载完毕
            if (onPlaylistLoaded != null) onPlaylistLoaded.run();

        }).start();
    }

    public void play(int pos) {
        if (playlist.isEmpty() || pos < 0 || pos >= playlist.size()) return;
        currentPosition = pos;

        try {
            mediaPlayer.reset();
            // 解析 Uri 播放
            mediaPlayer.setDataSource(getApplicationContext(), Uri.parse(playlist.get(pos).getPath()));
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(mp -> {
                mp.start();
                showNotification(playlist.get(pos));
                notifyUI(); // 通知界面更新
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void playNext() {
        if (playlist.isEmpty()) return; // 防崩
        play((currentPosition + 1) % playlist.size());
    }

    public void playPrev() {
        if (playlist.isEmpty()) return;
        int pos = currentPosition - 1;
        if (pos < 0) pos = playlist.size() - 1;
        play(pos);
    }

    public void pause() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            notifyUI();
        }
    }

    public void resume() {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            notifyUI();
        }
    }

    // ✅ 之前缺失的 seekTo 方法，已补上
    public void seekTo(int progress) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(progress);
        }
    }

    // --- Getter / Setter ---
    public boolean isPlaying() { return mediaPlayer != null && mediaPlayer.isPlaying(); }
    public int getCurrentProgress() { return mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0; }
    public int getDuration() { return mediaPlayer != null ? mediaPlayer.getDuration() : 0; }
    public List<MusicBean> getPlaylist() { return playlist; }

    public MusicBean getCurrentMusic() {
        if (currentPosition >= 0 && currentPosition < playlist.size()) {
            return playlist.get(currentPosition);
        }
        return null;
    }

    public void setOnStateChange(Runnable action) { this.onStateChange = action; }
    public void setOnPlaylistLoaded(Runnable action) { this.onPlaylistLoaded = action; }

    // 辅助方法：通知 UI 更新
    private void notifyUI() {
        if (onStateChange != null) onStateChange.run();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(new NotificationChannel("music_ch", "Music", NotificationManager.IMPORTANCE_LOW));
            }
        }
    }

    private void showNotification(MusicBean music) {
        // 获取封面图（简单版）
        Notification notification = new NotificationCompat.Builder(this, "music_ch")
                .setContentTitle(music.getTitle())
                .setContentText(music.getArtist())
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setOngoing(true)
                .build();
        startForeground(1, notification);
    }
}
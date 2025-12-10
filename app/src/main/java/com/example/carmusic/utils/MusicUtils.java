package com.example.carmusic.utils;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import com.example.carmusic.R;
import com.example.carmusic.bean.MusicBean;
import java.util.ArrayList;
import java.util.List;

public class MusicUtils {

    public static List<MusicBean> getMusicData(Context context) {
        List<MusicBean> list = new ArrayList<>();
        loadRawMusic(context, list);
        try {
            loadExternalMusic(context, list);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    private static void loadRawMusic(Context context, List<MusicBean> list) {
        // 这里不用改，保持原样
        addRawSong(context, list, R.raw.haiz, "错过的烟火", "内置音乐");
        addRawSong(context, list, R.raw.hongyan, "红颜如霜", "内置音乐");
        addRawSong(context, list, R.raw.zhuiweida, "最伟大的作品", "内置音乐");
    }

    private static void addRawSong(Context context, List<MusicBean> list, int resId, String title, String artist) {
        String path = "android.resource://" + context.getPackageName() + "/" + resId;
        list.add(new MusicBean(title, artist, path, 240000, -1));
    }

    // ✅ 核心修改：使用 ContentUris 获取安全的播放地址
    private static void loadExternalMusic(Context context, List<MusicBean> list) {
        String[] projection = {
                MediaStore.Audio.Media._ID, // 改为获取 ID
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.ALBUM_ID
        };

        // 这里的查询逻辑不变
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection, null, null, MediaStore.Audio.Media.IS_MUSIC);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                // 1. 获取 ID
                long id = cursor.getLong(0);
                String title = cursor.getString(1);
                String artist = cursor.getString(2);
                long duration = cursor.getLong(3);
                long albumId = cursor.getLong(4);

                // 2. 将 ID 转为真机可用的 content:// 格式 Uri
                Uri contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
                String path = contentUri.toString(); // 存入 Bean 的 path 字段

                if (duration > 10000) {
                    if ("<unknown>".equals(artist)) artist = "未知歌手";
                    list.add(new MusicBean(title, artist, path, duration, albumId));
                }
            }
            cursor.close();
        }
    }
}
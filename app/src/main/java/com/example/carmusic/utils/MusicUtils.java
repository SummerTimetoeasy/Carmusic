package com.example.carmusic.utils;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import com.example.carmusic.R;
import com.example.carmusic.bean.MusicBean;
import java.util.ArrayList;
import java.util.List;

public class MusicUtils {

    public static List<MusicBean> getMusicData(Context context) {
        List<MusicBean> list = new ArrayList<>();

        // 1. 加载内置音乐
        loadRawMusic(context, list);

        // 2. 扫描本地音乐
        try {
            loadExternalMusic(context, list);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    private static void loadRawMusic(Context context, List<MusicBean> list) {
        // 这里的 resId 必须对应您 res/raw 下的真实文件
        addRawSong(context, list, R.raw.haiz, "错过的烟火", "内置音乐");
        addRawSong(context, list, R.raw.hongyan, "红颜如霜", "内置音乐");
        addRawSong(context, list, R.raw.zhuiweida, "最伟大的作品", "内置音乐");
    }

    private static void addRawSong(Context context, List<MusicBean> list, int resId, String title, String artist) {
        String path = "android.resource://" + context.getPackageName() + "/" + resId;
        long duration = 240000; // 这里的时长是估算的
        long albumResId = -1;   // 内置音乐没有系统专辑封面 ID

        // 使用更新后的构造方法
        list.add(new MusicBean(title, artist, path, duration, albumResId));
    }

    private static void loadExternalMusic(Context context, List<MusicBean> list) {
        String[] projection = {
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.ALBUM_ID // 必须查这一列
        };

        Cursor cursor = context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection, null, null, MediaStore.Audio.Media.IS_MUSIC);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String title = cursor.getString(0);
                String artist = cursor.getString(1);
                String path = cursor.getString(2);
                long duration = cursor.getLong(3);
                long albumResId = cursor.getLong(4); // 获取专辑 ID

                if (duration > 10000) {
                    if ("<unknown>".equals(artist)) artist = "未知歌手";
                    // 存入 List
                    list.add(new MusicBean(title, artist, path, duration, albumResId));
                }
            }
            cursor.close();
        }
    }
}
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

        // 1. 先加载 RAW 文件夹的内置音乐
        loadRawMusic(context, list);

        // 2. 再扫描手机外部存储的音乐
        // (放在 try-catch 里防止因为没有权限导致整个程序崩溃，
        // 这样即使没权限，至少还能显示 Raw 里的歌)
        try {
            loadExternalMusic(context, list);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    // --- 私有方法：加载 Raw 内置音乐 ---
    private static void loadRawMusic(Context context, List<MusicBean> list) {
        try {
            // 添加您的内置歌曲 (请确保文件名与 res/raw 下的一致)
            addRawSong(context, list, R.raw.haiz, "错过的烟火", "内置音乐");
            addRawSong(context, list, R.raw.hongyan, "红颜如霜", "内置音乐");
            addRawSong(context, list, R.raw.zhuiweida, "最伟大的作品", "内置音乐");
            // addRawSong(context, list, R.raw.qilixiang, "七里香", "周杰伦");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void addRawSong(Context context, List<MusicBean> list, int resId, String title, String artist) {
        // 生成资源 URI 路径
        String path = "android.resource://" + context.getPackageName() + "/" + resId;
        // Raw 资源很难快速获取真实时长，这里统一写死 4分钟，不影响播放
        long duration = 240000;
        // 专辑 ID 对 Raw 无效，传 -1
        long albumId = -1;

        list.add(new MusicBean(title, artist, path, duration, albumId));
    }

    // --- 私有方法：扫描手机存储 ---
    private static void loadExternalMusic(Context context, List<MusicBean> list) {
        String[] projection = {
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.ALBUM_ID
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
                long albumId = cursor.getLong(4);

                if (duration > 10000) { // 过滤短音频
                    if ("<unknown>".equals(artist)) artist = "未知歌手";
                    list.add(new MusicBean(title, artist, path, duration, albumId));
                }
            }
            cursor.close();
        }
    }
}
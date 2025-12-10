package com.example.carmusic;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.carmusic.adapter.MusicAdapter;
import com.example.carmusic.bean.MusicBean;
import com.example.carmusic.service.MusicService;
import com.example.carmusic.utils.MusicUtils;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private MusicService musicService;
    private MusicAdapter adapter;
    private TextView tvTitle, tvArtist;
    private Button btnPlay;
    private SeekBar seekBar;
    private ImageView ivAlbumCover;
    private ObjectAnimator rotateAnimator; // 旋转动画对象
    private boolean isBound = false;
    private boolean isUserTouchingSeekBar = false;

    // 定时器：更新进度条
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            if (isBound && musicService.isPlaying() && !isUserTouchingSeekBar) {
                seekBar.setProgress(musicService.getCurrentProgress());
            }
            handler.postDelayed(this, 1000);
        }
    };

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            musicService = ((MusicService.MusicBinder) service).getService();
            isBound = true;
            checkPermission();
            // 切歌时的回调
            musicService.setOnStateChange(() -> runOnUiThread(() -> updateUI()));
        }
        @Override public void onServiceDisconnected(ComponentName name) { isBound = false; }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initAnimation(); // 初始化动画

        Intent intent = new Intent(this, MusicService.class);
        startService(intent);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
        handler.post(progressRunnable);
    }

    private void initView() {
        RecyclerView rv = findViewById(R.id.rv_list);
        tvTitle = findViewById(R.id.tv_title);
        tvArtist = findViewById(R.id.tv_sub_artist);
        btnPlay = findViewById(R.id.btn_play);
        Button btnNext = findViewById(R.id.btn_next);
        Button btnPrev = findViewById(R.id.btn_prev);
        seekBar = findViewById(R.id.seek_bar);
        ivAlbumCover = findViewById(R.id.iv_album_cover);

        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MusicAdapter();
        rv.setAdapter(adapter);

        adapter.setOnItemClick(pos -> { if (isBound) musicService.play(pos); });

        btnPlay.setOnClickListener(v -> {
            if (!isBound) return;
            if (musicService.isPlaying()) musicService.pause();
            else musicService.resume();
        });
        btnNext.setOnClickListener(v -> { if (isBound) musicService.playNext(); });
        btnPrev.setOnClickListener(v -> { if (isBound) musicService.playPrev(); });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean f) {}
            @Override public void onStartTrackingTouch(SeekBar s) { isUserTouchingSeekBar = true; }
            @Override public void onStopTrackingTouch(SeekBar s) {
                isUserTouchingSeekBar = false;
                if (isBound) musicService.seekTo(s.getProgress());
            }
        });
    }

    // 初始化旋转动画
    private void initAnimation() {
        rotateAnimator = ObjectAnimator.ofFloat(ivAlbumCover, "rotation", 0f, 360f);
        rotateAnimator.setDuration(15000); // 15秒转一圈
        rotateAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        rotateAnimator.setInterpolator(new LinearInterpolator()); // 匀速
    }

    // 更新界面（包含核心的图片加载逻辑）
    private void updateUI() {
        MusicBean current = musicService.getCurrentMusic();
        if (current != null) {
            tvTitle.setText(current.getTitle());
            tvArtist.setText(current.getArtist());
            seekBar.setMax(musicService.getDuration());

            // ========================================================
            // 【核心修复】：直接读取 MP3 文件内置图片，解决不显示问题
            // ========================================================
            Bitmap art = getAlbumArt(current.getPath());
            if (art != null) {
                ivAlbumCover.setImageBitmap(art);
            } else {
                // 确实没有图片，显示默认图
                ivAlbumCover.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        }

        // 控制播放按钮和动画状态
        if (musicService.isPlaying()) {
            btnPlay.setText("暂停");
            if (rotateAnimator.isPaused()) rotateAnimator.resume();
            else if (!rotateAnimator.isRunning()) rotateAnimator.start();
        } else {
            btnPlay.setText("播放");
            rotateAnimator.pause();
        }
    }

    // 【核心工具方法】：从文件路径提取图片
    private Bitmap getAlbumArt(String path) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            if (path.startsWith("android.resource://")) {
                // 如果是 raw 资源路径，需要传入 Context 和 Uri
                retriever.setDataSource(this, android.net.Uri.parse(path));
            } else {
                // 如果是普通文件路径
                retriever.setDataSource(path);
            }

            byte[] embedPic = retriever.getEmbeddedPicture();
            if (embedPic != null) {
                return BitmapFactory.decodeByteArray(embedPic, 0, embedPic.length);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                retriever.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private void checkPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.READ_MEDIA_AUDIO}, 1);
        } else { scan(); }
    }

    @Override
    public void onRequestPermissionsResult(int r, String[] p, int[] g) {
        super.onRequestPermissionsResult(r, p, g);
        if (g.length > 0 && g[0] == PackageManager.PERMISSION_GRANTED) scan();
    }

    private void scan() {
        new Thread(() -> {
            List<MusicBean> list = MusicUtils.getMusicData(this);
            runOnUiThread(() -> {
                adapter.setList(list);
                if (isBound) musicService.setPlaylist(list);
                Toast.makeText(this, "扫描到 " + list.size() + " 首歌", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(progressRunnable);
        if (rotateAnimator != null) rotateAnimator.cancel();
        if (isBound) unbindService(connection);
    }
}
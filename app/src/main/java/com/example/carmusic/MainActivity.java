package com.example.carmusic;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
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

public class MainActivity extends AppCompatActivity {
    private MusicService musicService;
    private MusicAdapter adapter;
    private TextView tvTitle, tvArtist;
    private Button btnPlay;
    private SeekBar seekBar;
    private ImageView ivAlbumCover;
    private ObjectAnimator rotateAnimator;
    private boolean isBound = false;
    private boolean isUserTouchingSeekBar = false;

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

            // 1. 设置回调：当 Service 切歌或播放状态改变时，更新界面
            musicService.setOnStateChange(() -> runOnUiThread(() -> updateUI()));

            // 2. 设置回调：当 Service 扫描完音乐后，更新列表
            musicService.setOnPlaylistLoaded(() -> runOnUiThread(() -> {
                adapter.setList(musicService.getPlaylist()); // 假设你的 Adapter 有 setList 方法
                Toast.makeText(MainActivity.this, "加载歌曲: " + musicService.getPlaylist().size(), Toast.LENGTH_SHORT).show();
            }));

            // 3. 如果 Service 已经有数据（比如屏幕旋转重连），直接显示
            if (!musicService.getPlaylist().isEmpty()) {
                adapter.setList(musicService.getPlaylist());
                updateUI();
            }
        }

        @Override public void onServiceDisconnected(ComponentName name) { isBound = false; }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 检查权限
        checkPermissionAndStart();
    }

    private void checkPermissionAndStart() {
        String permission = (Build.VERSION.SDK_INT >= 33) ?
                Manifest.permission.READ_MEDIA_AUDIO : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, 1);
        } else {
            initApp();
        }
    }

    @Override
    public void onRequestPermissionsResult(int r, String[] p, int[] g) {
        super.onRequestPermissionsResult(r, p, g);
        if (g.length > 0 && g[0] == PackageManager.PERMISSION_GRANTED) {
            initApp();
        } else {
            Toast.makeText(this, "需要权限才能播放音乐", Toast.LENGTH_SHORT).show();
        }
    }

    private void initApp() {
        initView();
        initAnimation();

        Intent intent = new Intent(this, MusicService.class);
        startService(intent); // 启动服务（后台保活）
        bindService(intent, connection, Context.BIND_AUTO_CREATE); // 绑定服务（控制交互）
        handler.post(progressRunnable);
    }

    private void initView() {
        RecyclerView rv = findViewById(R.id.rv_list);
        tvTitle = findViewById(R.id.tv_title);
        tvArtist = findViewById(R.id.tv_sub_artist); // 请确保 XML ID 对应
        btnPlay = findViewById(R.id.btn_play);
        Button btnNext = findViewById(R.id.btn_next);
        Button btnPrev = findViewById(R.id.btn_prev);
        seekBar = findViewById(R.id.seek_bar);
        ivAlbumCover = findViewById(R.id.iv_album_cover);

        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MusicAdapter();
        rv.setAdapter(adapter);

        // 点击列表播放
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

    private void initAnimation() {
        rotateAnimator = ObjectAnimator.ofFloat(ivAlbumCover, "rotation", 0f, 360f);
        rotateAnimator.setDuration(15000);
        rotateAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        rotateAnimator.setInterpolator(new LinearInterpolator());
    }

    // 更新界面（含图片加载）
    private void updateUI() {
        if (musicService == null) return;
        MusicBean current = musicService.getCurrentMusic();

        if (current != null) {
            tvTitle.setText(current.getTitle());
            tvArtist.setText(current.getArtist());
            seekBar.setMax(musicService.getDuration());

            // --- 加载专辑封面 (核心代码) ---
            Uri albumUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    current.getAlbumResId()
            );

            try {
                ivAlbumCover.setImageResource(android.R.drawable.ic_menu_gallery); // 先重置默认图
                if (current.getAlbumResId() > 0) {
                    ivAlbumCover.setImageURI(albumUri);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (musicService.isPlaying()) {
            btnPlay.setText("暂停");
            if (rotateAnimator.isPaused()) rotateAnimator.resume();
            else if (!rotateAnimator.isRunning()) rotateAnimator.start();
        } else {
            btnPlay.setText("播放");
            rotateAnimator.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(progressRunnable);
        if (isBound) unbindService(connection);
    }
}
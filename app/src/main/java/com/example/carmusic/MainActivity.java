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
import com.example.carmusic.utils.MusicUtils;

import java.util.List;

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

            // 绑定成功后，检查权限并开始扫描（保留原本逻辑）
            checkPermission();

            // 设置状态回调
            musicService.setOnStateChange(() -> runOnUiThread(() -> updateUI()));

            // 【修复报错】：删除了 setOnPlaylistLoaded，因为我们用原本的 scan() 方法
        }
        @Override public void onServiceDisconnected(ComponentName name) { isBound = false; }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initAnimation();

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

    private void initAnimation() {
        rotateAnimator = ObjectAnimator.ofFloat(ivAlbumCover, "rotation", 0f, 360f);
        rotateAnimator.setDuration(15000);
        rotateAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        rotateAnimator.setInterpolator(new LinearInterpolator());
    }

    private void updateUI() {
        if (musicService == null) return;
        MusicBean current = musicService.getCurrentMusic();
        if (current != null) {
            tvTitle.setText(current.getTitle());
            tvArtist.setText(current.getArtist());
            seekBar.setMax(musicService.getDuration());

            // --- 优化：使用系统相册ID加载封面 (比原来的 MetadataRetriever 更快更稳) ---
            try {
                // 先重置默认图
                ivAlbumCover.setImageResource(android.R.drawable.ic_menu_gallery);

                if (current.getAlbumResId() > 0) {
                    Uri albumArtUri = Uri.parse("content://media/external/audio/albumart");
                    Uri imgUri = ContentUris.withAppendedId(albumArtUri, current.getAlbumResId());
                    ivAlbumCover.setImageURI(imgUri);
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

    private void checkPermission() {
        String permission = (Build.VERSION.SDK_INT >= 33) ?
                Manifest.permission.READ_MEDIA_AUDIO : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, 1);
        } else {
            scan();
        }
    }

    @Override
    public void onRequestPermissionsResult(int r, String[] p, int[] g) {
        super.onRequestPermissionsResult(r, p, g);
        if (g.length > 0 && g[0] == PackageManager.PERMISSION_GRANTED) scan();
    }

    // 保留原本的扫描逻辑
    private void scan() {
        new Thread(() -> {
            List<MusicBean> list = MusicUtils.getMusicData(this);
            runOnUiThread(() -> {
                adapter.setList(list);
                if (isBound) musicService.setPlaylist(list); // 把数据传给 Service
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
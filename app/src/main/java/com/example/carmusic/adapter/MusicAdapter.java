package com.example.carmusic.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.carmusic.R;
import com.example.carmusic.bean.MusicBean;
import java.util.ArrayList;
import java.util.List;
import android.content.ContentUris;
import android.net.Uri;

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.VH> {
    private List<MusicBean> data = new ArrayList<>();
    private OnItemClick listener;

    public interface OnItemClick { void onClick(int pos); }
    public void setOnItemClick(OnItemClick l) { this.listener = l; }
    public void setList(List<MusicBean> list) { this.data = list; notifyDataSetChanged(); }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_music, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        MusicBean bean = data.get(position);
        holder.title.setText(bean.getTitle());
        holder.artist.setText(bean.getArtist());

        // --- 新增：加载专辑图片逻辑 开始 ---

        // 1. 生成专辑封面的 URI
        Uri albumUri = ContentUris.withAppendedId(
                Uri.parse("content://media/external/audio/albumart"),
                bean.getAlbumResId()
        );

        // 2. 设置图片
        // 注意：holder.albumIcon 要改成你 ViewHolder 里实际定义的 ImageView 名字
        try {
            // 先设置一个默认图 (防止没图的时候显示空白，或者显示上一张的缓存)
            holder.albumIcon.setImageResource(R.drawable.ic_launcher_background); // 改成你自己的默认图资源ID

            // 如果有有效的 ID，尝试设置图片
            if (bean.getAlbumResId() > 0) {
                holder.albumIcon.setImageURI(albumUri);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // --- 新增：加载专辑图片逻辑 结束 ---

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(position);
        });
    }

    @Override public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView title, artist;
        ImageView albumIcon; //  新增

        public VH(View v) {
            super(v);
            title = v.findViewById(R.id.tv_item_title);
            artist = v.findViewById(R.id.tv_item_artist);
            albumIcon = v.findViewById(R.id.iv_item_icon); // 绑定 XML 里新加的图片
        }
    }
}
package com.example.carmusic.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.carmusic.R;
import com.example.carmusic.bean.MusicBean;
import java.util.ArrayList;
import java.util.List;

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
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(position);
        });
    }

    @Override public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView title, artist;
        public VH(View v) {
            super(v);
            title = v.findViewById(R.id.tv_item_title);
            artist = v.findViewById(R.id.tv_item_artist);
        }
    }
}
package com.example.musicplayer;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer.DAO.Audio;

import java.util.ArrayList;

public class MyRecyclerAdapter extends RecyclerView.Adapter<MyRecyclerAdapter.ViewHolder> {
    ArrayList<Audio> audioArrayList;
    Context mContext;
    public MyRecyclerAdapter(Context context, ArrayList<Audio> audioArrayList) {
        mContext= context;
        this.audioArrayList = audioArrayList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.adapter_myrecycler,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        holder.textView.setText(audioArrayList.get(position).getTitle());
        holder.mAudioPos = position;
        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                holder.clickListener = (OnItemClickListener)mContext;
                holder.clickListener.onItemClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return audioArrayList.size();
    }



    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView textView;
        CardView cardView;
        int mAudioPos;
        OnItemClickListener clickListener;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = (TextView) itemView.findViewById(R.id.title);
            imageView = (ImageView)itemView.findViewById(R.id.play_pause);
            cardView = (CardView)itemView.findViewById(R.id.cardview);

        }

    }
}

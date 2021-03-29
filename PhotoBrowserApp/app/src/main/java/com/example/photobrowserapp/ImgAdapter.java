package com.example.photobrowserapp;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ImgAdapter extends RecyclerView.Adapter<ImgAdapter.ViewHolder> {

    private List<MyImageList> imageList;
    private static final String TAG = "ImgAdapter";

    public ImgAdapter(List<MyImageList> imageList) {
        this.imageList = imageList;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView imageView1;
        ImageView imageView2;
        ImageView imageView3;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView1 = (ImageView) itemView.findViewById(R.id.image1);
            imageView2 = (ImageView) itemView.findViewById(R.id.image2);
            imageView3 = (ImageView) itemView.findViewById(R.id.image3);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_pictures, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MyImageList imageRow = imageList.get(position);
        Log.d(TAG, "onBindViewHolder: " + imageRow);
        holder.imageView1.setImageBitmap(imageRow.getImage(1).getImageBitmap());
        holder.imageView2.setImageBitmap(imageRow.getImage(2).getImageBitmap());
        holder.imageView3.setImageBitmap(imageRow.getImage(3).getImageBitmap());
    }

    @Override
    public int getItemCount() {
        return imageList.size();
    }
}

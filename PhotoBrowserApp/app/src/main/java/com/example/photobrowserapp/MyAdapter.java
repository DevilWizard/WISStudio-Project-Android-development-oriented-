package com.example.photobrowserapp;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import java.util.List;

public class MyAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int TYPE_IMAGE = 0;
    public static final int TYPE_FOOTER = 1;
    public static final int LOADING = 1;
    public static final int LOAD_FINISHED = 2;
    private int loadState = 2;//默认已加载完毕
    private List<MyImage> imageList;
    private static final String TAG = "MyAdapter";

    public MyAdapter(List<MyImage> imageList) {
        this.imageList = imageList;
    }

    public static class ImageViewHolder extends RecyclerView.ViewHolder {

        private ImageView imageView;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = (ImageView) itemView.findViewById(R.id.image);
        }
    }

    public static class FooterViewHolder extends RecyclerView.ViewHolder {

        private TextView loadingText;

        public FooterViewHolder(@NonNull View itemView) {
            super(itemView);
            loadingText = (TextView) itemView.findViewById(R.id.loadTips);
        }
    }

    public void setLoadState(int loadState) {
        this.loadState = loadState;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        //区分加载更多的view和显示图片的view
        if (position + 1 == getItemCount()) {
            return TYPE_FOOTER;
        } else {
            return TYPE_IMAGE;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        switch (viewType) {
            case TYPE_IMAGE:
                View view_image = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_view, parent, false);
                return new MyAdapter.ImageViewHolder(view_image);

            case TYPE_FOOTER:
                View view_footer = LayoutInflater.from(parent.getContext()).inflate(R.layout.footerview, parent, false);
                return new MyAdapter.FooterViewHolder(view_footer);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof FooterViewHolder) {
            switch (loadState) {
                case LOADING:
                    ((FooterViewHolder) holder).loadingText.setVisibility(View.VISIBLE);
                    break;
                case LOAD_FINISHED:
                    ((FooterViewHolder) holder).loadingText.setVisibility(View.GONE);
                    break;
            }
        } else {
            MyImage image = imageList.get(position);
            Log.d(TAG, "onBindViewHolder: " + "position: " + position + "url: " + image.getUrl());
            ((ImageViewHolder) holder).imageView.setImageBitmap(image.getImageBitmap());
        }
    }

    @Override
    public int getItemCount() {
        //多了一个footer，需要额外加1
        return imageList.size() + 1;
    }

    @Override
    public void onViewAttachedToWindow(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        ViewGroup.LayoutParams layoutParams = holder.itemView.getLayoutParams();
        if (layoutParams != null && layoutParams instanceof StaggeredGridLayoutManager.LayoutParams) {
            StaggeredGridLayoutManager.LayoutParams params = (StaggeredGridLayoutManager.LayoutParams) layoutParams;
            int position = holder.getLayoutPosition();
            if (getItemViewType(position) == TYPE_FOOTER) {
                params.setFullSpan(true);
            }
        }
    }
}

package com.wisstudio.devilwizard.photobrowserapp.ui;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.wisstudio.devilwizard.photobrowserapp.R;
import com.wisstudio.devilwizard.photobrowserapp.db.PhotoDataBaseManager;
import com.wisstudio.devilwizard.photobrowserapp.util.MyApplication;
import com.wisstudio.devilwizard.photobrowserapp.util.image.MyImage;
import com.wisstudio.devilwizard.photobrowserapp.util.image.load.ImageLoader;
import com.wisstudio.devilwizard.photobrowserapp.util.logutil.MyLog;
import com.wisstudio.devilwizard.photobrowserapp.util.network.HttpCallBackListener;
import com.wisstudio.devilwizard.photobrowserapp.util.network.HttpRequest;
import com.wisstudio.devilwizard.photobrowserapp.util.network.NetWorkState;

import java.util.List;

/**
 * 主页面中RecyclerView的适配器类，包含了显示图片的类{@see #ImageViewHolder}等
 *
 * @author WizardK
 * @version 1.0
 * @date 2021-03-31
 */
public class MyAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "MyAdapter";

    /**
     * 表示该View是图片类型
     */
    public static final int TYPE_IMAGE = 0;

    /**
     * 表示该View是加载更多的视图类型
     */
    public static final int TYPE_FOOTER = 1;

    /**
     * 表示下一页图片正在加载中
     */
    public static final int LOADING = 1;

    /**
     * 表示图片已经加载完毕
     */
    public static final int LOAD_FINISHED = 2;

    /**
     * 默认的加载状态
     */
    private final int defaultLoadState = 2;//默认已加载完毕

    private int loadState;
    private final List<MyImage> imageList;
    private final ImageLoader imageLoader;
    private final PhotoDataBaseManager photoDataBaseManager;

    /**
     * 初始化{@link #imageList}, {@link #loadState}, {@link #imageLoader}, {@link #photoDataBaseManager}
     * @param imageList
     */
    public MyAdapter(List<MyImage> imageList) {
        this.imageList = imageList;
        this.loadState = defaultLoadState;
        imageLoader = ImageLoader.getInstance();
        photoDataBaseManager = MainActivity.photoDBManager;
    }

    /**
     * 将显示“加载更多”的视图设置为{@link #loadState}的状态
     *
     * @param loadState 要设置的加载状态，共有两种状态{@link #LOADING}, {@link #LOAD_FINISHED}
     *
     */
    public void setLoadState(int loadState) {
        this.loadState = loadState;
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
            MyLog.d(TAG, "onBindViewHolder: " + "position: " + position + "url: " + image.getUrl());
            ImageView imageView = ((ImageViewHolder)holder).imageView;
            imageView.setImageResource(R.drawable.default_loading_picture);//未加载的图片默认用纯灰图片填充
            //有时候会出现白图或黑图的情况，未知bug
            String imageUrl = image.getUrl();
            imageView.setTag(imageUrl);

            //点击图片查看原图的监听事件
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final Dialog dialog = new Dialog(MainActivity.getMainActivity(), R.style.fullScreenImageStyle);
                    View dialogView = LayoutInflater.from(MainActivity.getMainActivity()).inflate(R.layout.enlarged_imageview_dialog, null);
                    ProgressBar dialogProgressBar = dialogView.findViewById(R.id.loadingIndicateBar);
                    ImageView enlargedImageView = dialogView.findViewById(R.id.enlargedImage);
                    dialog.setContentView(dialogView);
                    dialog.show();
                    if (NetWorkState.isNetworkConnected(MyApplication.getContext())) {
                        HttpRequest.loadBitmapFromWeb(imageUrl, new HttpCallBackListener<Bitmap>() {
                            @Override
                            public void onFinish(Bitmap response) {
                                MainActivity.getMainActivity().runOnUiThread(() -> {
                                    dialogProgressBar.setVisibility(View.GONE);
                                    enlargedImageView.setImageBitmap(response);
                                });
                            }

                            @Override
                            public void onFailed() {
                                TextView textView = dialogView.findViewById(R.id.photoLoadFailedWarnText);
                                textView.setVisibility(View.VISIBLE);
                            }

                            @Override
                            public void onError(Exception e) {
                                e.printStackTrace();
                            }
                        });
                    } else {
                        dialogProgressBar.setVisibility(View.GONE);
                        TextView textView = dialogView.findViewById(R.id.photoLoadFailedWarnText);
                        textView.setVisibility(View.VISIBLE);
                    }

                    dialogView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.dismiss();
                        }
                    });
                }
            });

            //长按弹出下载和收藏菜单
            imageView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Vibrator vibrator = (Vibrator)(MyApplication.getContext().getSystemService(Context.VIBRATOR_SERVICE));
                    vibrator.vibrate(Vibrator.VIBRATION_EFFECT_SUPPORT_YES);//调用系统硬件级别的震动
                    LongPressPopUpWindow popUpWindow = new LongPressPopUpWindow(MainActivity.getMainActivity(), LongPressPopUpWindow.IMAGEVIEW_POPUP_WINDOW);
                    popUpWindow.showAsDropDown(v, 0, 0);
                    (popUpWindow.getPopUpView()).findViewById(R.id.click_to_download).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            MainActivity.getMainActivity().getImageDownLoader().saveImageToGallery(imageUrl);
                            popUpWindow.dismiss();
                        }
                    });
                    (popUpWindow.getPopUpView()).findViewById(R.id.click_to_star).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            MainActivity.getMainActivity().runOnUiThread(() -> Toast.makeText(MyApplication.getContext(),
                                "收藏成功", Toast.LENGTH_SHORT).show());
                            photoDataBaseManager.setStarredState(imageUrl, PhotoDataBaseManager.STARRED_STATE);
                            popUpWindow.dismiss();
                        }
                    });
                    return true;//返回true可以将长按事件消耗，这样长按后就不会再触发单击事件
                }
            });

            //加载图片的流程
            if (NetWorkState.isNetworkConnected(MyApplication.getContext()) && image != null) {//区分有网和无网的情况
                imageLoader.loadBitmap(imageView, image);
            } else {
                imageLoader.loadPhotoFromFileCache(imageUrl, imageView, photoDataBaseManager);
            }
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
                params.setFullSpan(true);//将“加载更多”视图的宽度设为整个屏幕的宽度
            }
        }
    }

    /**
     * 显示图片的{@link RecyclerView.ViewHolder}类
     */
    public static class ImageViewHolder extends RecyclerView.ViewHolder {

        private final ImageView imageView;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image);
        }
    }

    /**
     * 显示加载更多的{@link RecyclerView.ViewHolder}类
     */
    public static class FooterViewHolder extends RecyclerView.ViewHolder {

        private final TextView loadingText;

        public FooterViewHolder(@NonNull View itemView) {
            super(itemView);
            loadingText = itemView.findViewById(R.id.loadTips);
        }
    }
}

package com.wisstudio.devilwizard.photobrowserapp.util;

import android.graphics.Bitmap;
import android.os.Looper;
import android.widget.ImageView;

import androidx.annotation.NonNull;

/**
 * 用于在Imageview显示图像
 *
 * @author WizardK
 * @date 2021-04-14
 */
public class BitmapDisplayer {

    private ImageView imageView;

    public BitmapDisplayer(@NonNull ImageView imageView) {
        if (imageView == null) {
            throw new IllegalArgumentException("imageView must not be null!");
        }
        this.imageView = imageView;
    }

    /**
     * 设置imageview的bitmap
     *
     * @param bitmap 要显示在imageview的bitmap
     * @return 若操作是在UI线程中完成则返回true，否则返回false即设置失败
     */
    public boolean setImageBitmap(Bitmap bitmap) {
        //UI线程判断
        if (Looper.myLooper() == Looper.getMainLooper()) {
            if (imageView != null) {
                imageView.setImageBitmap(bitmap);
            }
            return true;
        }
        return false;
    }
}

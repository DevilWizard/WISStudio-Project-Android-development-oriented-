package com.wisstudio.devilwizard.photobrowserapp.cache.disk;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.wisstudio.devilwizard.photobrowserapp.util.ImageLoader;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * FileCache类用于将图片缓存至本地
 *
 * @author WizardK
 * @date 2021-04-06
 */
public class FileCache {


    /**
     * 默认的图片缓存路径"/data/data/com.wisstudio.devilwizard.photobrowserapp/cache/imgCache"
     */
    public static final String DEFAULT_CACHE_DIR = "imgCache";
    private static final String TAG = "FileCache";
    /**
     * 图片缓存目录
     */
    private File mCacheDir;

    /**
     * 创建缓存文件目录，默认在手机内部创建，路径为"/data/data/com.wisstudio.devilwizard.photobrowserapp/cache/{@link #DEFAULT_CACHE_DIR}"
     *
     * @param context
     *
     */
    public FileCache(Context context) {

        //andriod 10后不允许直接在根目录直接创建目录，为了前后兼容，默认在手机内部新建缓存目录
        mCacheDir = new File(context.getCacheDir(), DEFAULT_CACHE_DIR);
        if (!mCacheDir.exists()) {
            mCacheDir.mkdirs();//创建目录
        }

    }

    /**
     * 获取图片缓存路径的File{@see java.io.File}对象
     *
     * @param url 图片缓存的路径
     *
     * @return java.io.File
     *
     * @exception
     */
    public File getFile(String url) {
        File f = null;
        String path = ImageLoader.url2path(url);
        try{
            String fileName = URLEncoder.encode(path, "utf-8");
            f = new File(mCacheDir, fileName);
            Log.d(TAG, "getFile: " + f.exists());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return f;
    }

    public Bitmap getBitmapFromFile(String cachePath) {
        return BitmapFactory.decodeFile(cachePath);
    }

    /**
     * 返回图片储存的完整绝对路径
     *
     * @param url 图片的url
     * @return 返回图片储存的完整绝对路径
     */
    public String getFullCachePath(String url) {
        return mCacheDir.getAbsolutePath() + File.separator + ImageLoader.url2path(url);
    }

    /**
     * 清除本地文件缓存
     *
     * @return void
     */
    public void clear() {
        File[] files = mCacheDir.listFiles();
        for (File f : files) {
            f.delete();
        }
    }
}

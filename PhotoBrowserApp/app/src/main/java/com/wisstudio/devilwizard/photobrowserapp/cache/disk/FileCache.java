package com.wisstudio.devilwizard.photobrowserapp.cache.disk;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.wisstudio.devilwizard.photobrowserapp.cache.memory.MemoryCache;
import com.wisstudio.devilwizard.photobrowserapp.util.image.load.ImageLoader;
import com.wisstudio.devilwizard.photobrowserapp.util.logutil.MyLog;

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
    private final File mCacheDir;

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
     * 通过url获取图片缓存路径的{@link File}对象
     *
     * @param url 图片缓存的路径
     *
     * @return java.io.File
     *
     * @exception UnsupportedEncodingException
     */
    public File getFile(String url) {

        File f = null;
        String path = ImageLoader.url2path(url);
        try{
            String fileName = URLEncoder.encode(path, "utf-8");
            f = new File(mCacheDir, fileName);
            MyLog.d(TAG, "getFile: " + f.exists());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return f;
    }

    /**
     * 先从mMemoryCache中寻找那些被清出放在reusableBitmaps里的bitmap，若有则直接读取，
     * 否则通过缓存路径读取缓存图片并返回其{@link Bitmap}，此方法得到的缓存与{@link #getFile(String)}一致
     *
     * @param cachePath 图片的缓存路径（通过{@link #getFullCachePath(String)}得到的缓存路径）
     *
     * @param mMemoryCache 内存缓存实例
     *
     * @return 返回图片的 {@link Bitmap}
     *
     */
    public Bitmap getBitmapFromFile(String cachePath, MemoryCache mMemoryCache) {

        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(cachePath, options);//先解析一遍原来的图，获取其相关options参数
        options.inJustDecodeBounds = false;
        addInBitmapOptions(options, mMemoryCache);

        return BitmapFactory.decodeFile(cachePath, options);//若addInBitmapOptions成功，那么这里decodeFile能直接复用inBitmap
    }

    /**
     * 返回图片储存的完整绝对路径
     *
     * @param url 图片的url
     * @return 返回图片储存的完整绝对路径
     *
     */
    public String getFullCachePath(String url) {
        return mCacheDir.getAbsolutePath() + File.separator + ImageLoader.url2path(url);
    }

    /**
     * 清除本地文件缓存
     */
    public void clear() {

        File[] files = mCacheDir.listFiles();
        for (File f : files) {
            f.delete();
        }

    }

    /**
     * 寻找合适的{@link BitmapFactory.Options#inBitmap}并使用
     *
     * @param options 目标图片的相关{@link BitmapFactory.Options}参数
     *
     * @param mMemoryCache 内存缓存实例，在这里面寻找是否有合适的inBitmap
     */
    private void addInBitmapOptions(BitmapFactory.Options options, MemoryCache mMemoryCache) {

        options.inMutable = true;

        if (mMemoryCache != null) {
            //寻找是否有符合资格的bitmap
            Bitmap inBitmap = mMemoryCache.getBitmapFromReusableSet(options);

            if (inBitmap != null) {
                //若符合复用资格则将其赋给options的inBitmap属性，
                // 这样以后调用decodeFile就能直接复用inBitmap而不用再次分配内存了
                options.inBitmap = inBitmap;
            }
        }
    }
}

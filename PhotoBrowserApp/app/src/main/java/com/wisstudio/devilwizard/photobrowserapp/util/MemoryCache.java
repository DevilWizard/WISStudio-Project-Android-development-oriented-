package com.wisstudio.devilwizard.photobrowserapp.util;

import android.graphics.Bitmap;
import android.util.Log;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MemoryCache类用于内存缓存已加载的图片
 *
 * @author WizardK
 * @date 2021-04-06
 */
public class MemoryCache {


    private static final String TAG = "MemoryCache";
    /**
     * 最大的缓存字节数（B为单位）
     */
    private int maxMemorySize;
    /**
     * MemoryCache当前的缓存大小（B为单位）
     */
    private int currentSize;

    //这里用到了LRU思想，也就是将近期中最少使用的图片缓存清理，这里accessOrder设为true能保证最先移除访问次数最少的图片缓存
    private HashMap<String, Bitmap> mCacheMap = new LinkedHashMap<String, Bitmap>(0, 0.75f, true) {
        //超过缓存最大值maxMemorySize时将最早且最少使用的缓存数据清出
        @Override
        protected boolean removeEldestEntry(Entry<String, Bitmap> eldest) {
            if (currentSize > maxMemorySize) {
                Log.d(TAG, "removeEldestEntry: currentSize is over the max!");
                if (eldest != null) {
                    Bitmap bitmap = eldest.getValue();
                    currentSize -= bitmap.getByteCount();
//                    bitmap.recycle();//将bitmap回收掉
//                    bitmap = null;
                }
                return true;
            }
            return false;
        }
    };

    public MemoryCache(int maxMemorySize) {
        this.maxMemorySize = maxMemorySize;
    }

    /**
     * 从缓存中取出图片
     *
     * @param key 储存在{@link #mCacheMap}中的图片的url地址
     *
     * @return 若该图片仍在缓存中则返回其Bitmap，否则返回null
     */
    public Bitmap get(String key) {
        if (!mCacheMap.containsKey(key)) {
            return null;
        }
        return mCacheMap.get(key);
    }

    /**
     * 缓存图片
     *
     * @param key 储存在{@link #mCacheMap}中的url地址
     *
     * @param value 待储存图片的Bitmap对象
     *
     * @return void
     */
    public void put(String key, Bitmap value) {
        mCacheMap.put(key, value);
        int sizeOfValue = 0;
        if (value != null) {//图片加载成功时才计算缓存
            sizeOfValue = mCacheMap.get(key).getByteCount();//获取当前放入的图片大小
        }
        currentSize += sizeOfValue;
        Log.d(TAG, "currentSize of MemoryCache: "+ currentSize/(1024*1024));
    }

//    private void trimToSize() {
//        if (currentSize > maxMemorySize) {
//            Map.Entry<String, Bitmap> toClear = mCacheMap.
//            Log.d(TAG, "removeEldestEntry: currentSize is over the max!");
//            if (eldest != null) {
//                Bitmap bitmap = eldest.getValue();
//                currentSize -= bitmap.getByteCount();
////                    bitmap.recycle();//将bitmap回收掉
////                    bitmap = null;
//            }
//        }
//    }

    /**
     * 清除所有缓存
     *
     * @param
     *
     * @return void
     *
     * @exception
     */
    public void clearCache() {
        try {
            for (Map.Entry<String, Bitmap> entry : mCacheMap.entrySet()) {
                Bitmap bmp = entry.getValue();
                if (bmp != null) {
                    bmp.recycle();//回收Bitmap对象
                    bmp = null;
                }
            }
            mCacheMap.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

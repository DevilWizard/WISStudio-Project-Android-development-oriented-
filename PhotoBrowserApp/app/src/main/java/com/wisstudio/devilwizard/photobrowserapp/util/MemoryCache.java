package com.wisstudio.devilwizard.photobrowserapp.util;

import android.graphics.Bitmap;

import java.lang.ref.SoftReference;
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
    /**
     * 最大的缓存数
     */
    private static final int MAX_CACHE_CAPACITY = 25;
    //用Map软引用的Bitmap对象, 保证内存空间足够情况下不会被垃圾回收
    private HashMap<String, SoftReference<Bitmap>> mCacheMap = new LinkedHashMap<String, SoftReference<Bitmap>>() {

        //超过缓存最大数MAX_CACHE_CAPACITY时将最早缓存的数据腾出留位
        @Override
        protected boolean removeEldestEntry(Entry<String, SoftReference<Bitmap>> eldest) {
            return size() > MAX_CACHE_CAPACITY;
        }
    };

    /**
     * 从缓存中取出图片
     *
     * @param id 储存在{@link #mCacheMap}中的图片的url地址
     *
     * @return 若该图片仍在缓存中则返回其Bitmap，否则返回null
     */
    public Bitmap get(String id) {
        if (!mCacheMap.containsKey(id)) {
            return null;
        }
        SoftReference<Bitmap> ref = mCacheMap.get(id);
        return ref.get();
    }

    /**
     * 缓存图片
     *
     * @param id 储存在{@link #mCacheMap}中的url地址
     *
     * @param bitmap 待储存图片的Bitmap对象
     *
     * @return void
     */
    public void put(String id, Bitmap bitmap) {
        mCacheMap.put(id, new SoftReference<Bitmap>(bitmap));
    }

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
            for (Map.Entry<String, SoftReference<Bitmap>> entry : mCacheMap.entrySet()) {
                SoftReference<Bitmap> sr = entry.getValue();
                if (sr != null) {
                    Bitmap bmp = sr.get();
                    if (bmp != null) {
                        bmp.recycle();//回收Bitmap对象
                    }
                }
            }
            mCacheMap.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

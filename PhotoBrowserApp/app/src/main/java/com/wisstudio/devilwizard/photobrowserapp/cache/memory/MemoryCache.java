package com.wisstudio.devilwizard.photobrowserapp.cache.memory;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.util.LruCache;

import com.wisstudio.devilwizard.photobrowserapp.util.logutil.MyLog;

import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * MemoryCache类用于内存缓存已加载的图片，所有数据均以KB为单位
 *
 * @author WizardK
 * @date 2021-04-06
 */
public class MemoryCache {


    private static final String TAG = "MemoryCache";

    /**
     * 最大的缓存字节数（KB为单位）
     */
    private final int maxMemorySize;

    /**
     * 用LruCache作内存缓存
     */
    private final LruCache<String, Bitmap> mCacheMap;

    /**
     * 用于保存那些被移出内存缓存{@link #mCacheMap}的Bitmap引用，方便后续在这寻找合适的{@link BitmapFactory.Options#inBitmap}进行复用
     */
    private final Set<SoftReference<Bitmap>> reusableBitmaps;

    /**
     * 初始化内存缓存，并设置缓存大小{@link #maxMemorySize}(以KB为单位)
     *
     * @param maxMemorySizeInByte 内存缓存的最大值(以字节B为单位)
     */
    public MemoryCache(int maxMemorySizeInByte) {
        this.maxMemorySize = maxMemorySizeInByte / 1024;
        MyLog.d(TAG, "MemoryCache: maxMemorySize is " + maxMemorySize/1024 + "MB");
        reusableBitmaps = Collections.synchronizedSet(new HashSet<>());
        mCacheMap = new LruCache<String, Bitmap>(maxMemorySize){
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount() / 1024;//转为KB
            }

            @Override
            protected void entryRemoved(boolean evicted, String key, Bitmap oldValue, Bitmap newValue) {
                reusableBitmaps.add(new SoftReference<Bitmap>(oldValue));//当其被移出时，将其加入reusableBitmaps
            }
        };
    }

    /**
     * 从缓存中取出图片
     *
     * @param key 储存在{@link #mCacheMap}中的图片的url地址
     *
     * @return 若该图片仍在缓存中则返回其Bitmap，否则返回null
     */
    public Bitmap get(String key) {
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

        if (get(key) == null) {
            mCacheMap.put(key, value);
        }
        MyLog.d(TAG, "currentSize of MemoryCache in MB: "+ mCacheMap.size() / 1024);
    }

    /**
     * 通过遍历{@link #reusableBitmaps}来寻找是否有能够被复用的bitmap引用
     *
     * @param options 要加载的目标图片的相关{@link BitmapFactory.Options}参数
     *
     * @return 返回符合条件的bitmap
     */
    public Bitmap getBitmapFromReusableSet(BitmapFactory.Options options) {
        Bitmap bitmap = null;

        if (reusableBitmaps != null && !reusableBitmaps.isEmpty()) {
            synchronized (reusableBitmaps) {
                final Iterator<SoftReference<Bitmap>> iterator = reusableBitmaps.iterator();
                Bitmap item;

                while(iterator.hasNext()) {
                    item = iterator.next().get();

                    if (item != null && item.isMutable()) {
                        //检查item是否符合复用条件
                        if (canUseForInBitmap(item, options)) {
                            MyLog.d(TAG, "getBitmapFromReusableSet: candidate found!");
                            bitmap = item;
                            iterator.remove();//若符合条件则将其从reusableBitmaps移除，防止二次使用
                            break;
                        }
                    } else {
                        iterator.remove();//若此bitmap引用已被清理，则也将其移出
                    }
                }

            }
        }
        return bitmap;
    }

    /**
     * 清除所有缓存
     *
     * @exception
     */
    public void clearCache() {
        mCacheMap.evictAll();//调用LruCache自带方法释放内存缓存
    }

    /**
     * 判断candidate是否符合被复用的条件
     *
     * @param candidate {@link #reusableBitmaps}中待判断是否符合复用资格的bitmap
     * @param targetOptions 要加载的目标图片的相关{@link BitmapFactory.Options}参数
     *
     * @return 符合则返回true，否则返回false
     */
    private boolean canUseForInBitmap(Bitmap candidate, BitmapFactory.Options targetOptions) {

        int width = targetOptions.outWidth / (targetOptions.inSampleSize > 0 ? targetOptions.inSampleSize:1);
        int height = targetOptions.outHeight / (targetOptions.inSampleSize > 0 ? targetOptions.inSampleSize:1);
        int byteCount = width * height * getBytesPerPixel(candidate.getConfig());
        //当目标图片比candidate小时（占用内存更小时），则candidate符合复用资格
        return byteCount <= candidate.getAllocationByteCount();
    }

    /**
     * 根据图片的bitmap储存格式，返回相应的单位像素占用内存大小(Byte/pixel)
     *
     * @return 返回相应的单位像素占用内存大小(Byte/pixel)
     */
    private int getBytesPerPixel(Config config) {
        if (config == Config.ARGB_8888) {
            return 4;
        } else if (config == Config.RGB_565){
            return 2;
        } else if (config == Config.ARGB_4444) {
            return 2;
        } else if (config == Config.ALPHA_8) {
            return 1;
        }

        return 1;
    }
}

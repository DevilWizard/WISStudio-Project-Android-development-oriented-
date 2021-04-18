package com.wisstudio.devilwizard.photobrowserapp.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.ImageView;

import com.wisstudio.devilwizard.photobrowserapp.cache.disk.FileCache;
import com.wisstudio.devilwizard.photobrowserapp.cache.memory.MemoryCache;
import com.wisstudio.devilwizard.photobrowserapp.db.PhotoDataBaseManager;
import com.wisstudio.devilwizard.photobrowserapp.util.NetWork.HttpRequest;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;

/**
 * @author WizardK
 * @date 2021-04-07
 */
public class ImageLoader {

    //使用单例，防止ImageLoader的频繁创建，减少性能消耗
    private volatile static ImageLoader instance;
    private static final String TAG = "ImageLoader";
    protected MemoryCache memoryCache;
    protected FileCache fileCache;
    private ExecutorService mExecutorService;

    /**
     * 记录已经加载图片的ImageView
     */
    private Map<ImageView, String> mImageViews = Collections.synchronizedMap(new WeakHashMap<>());
    
    /**
     * 保存正在加载图片的url
     */
    private List<LoadPhotoTask> taskQueue = new ArrayList<>();

    /**
     *
     * @param memoryCache
     * @param fileCache
     * @param maxThreads 用于异步加载图片线程池的最大线程数
     */
    private ImageLoader(MemoryCache memoryCache, FileCache fileCache, int maxThreads) {
        //防止持有Activity的Context而导致内存泄露
        this.fileCache = fileCache;
        this.memoryCache = memoryCache;
        mExecutorService = PhotoLoadThreadPoolExecutor.newFixedPhotoLoadPool(maxThreads);
    }

    /**
     * 使用双重加锁机制实现单例，保证多线程安全，同时支持懒加载
     * 该方法应该在第一次创建ImageLoader实例时调用，在这之后若需要获取实例应当调用{@link #getInstance()}
     *
     * @param memoryCache
     * @param fileCache
     * @param maxThreads 用于异步加载图片线程池的最大线程数
     *
     * @return com.wisstudio.devilwizard.photobrowserapp.util.ImageLoader
     */
    public static ImageLoader getInstance(MemoryCache memoryCache, FileCache fileCache, int maxThreads) {
        if (instance == null) {
            synchronized (ImageLoader.class) {
                if (instance == null) {
                    instance = new ImageLoader(memoryCache, fileCache, maxThreads);
                }
            }
        }
        return instance;
    }

    /**
     * 返回ImageLoader的单例
     *
     * @return com.wisstudio.devilwizard.photobrowserapp.util.ImageLoader
     *
     * @exception
     */
    public static ImageLoader getInstance() {
        if (instance == null) {
            throw new NullPointerException("instance of ImageLoader should be initiated before this method is called!");
        }
        return instance;
    }


    /**
     * 将图片的url转为储存的目录名
     * 比如url = https://picsum.photos/id/0/5616/3744, 则其储存的目录为“.../056163744”(将id后的数字拼接作为文件名)
     *
     * @param url 图片的url地址
     *
     * @return java.lang.String
     */
    public static String url2path(String url) {
        String rawPath = url.substring(url.lastIndexOf("d") + 1);
        rawPath = rawPath.replace("/","");
        return rawPath;//是否需要储存这些路径，方便插入数据库
    }

    public Bitmap loadPhotoFromFileCache(String url, ImageView imageView, PhotoDataBaseManager manager) {
        String cachePath = manager.getPhotoCachePath(url);
        Bitmap bitmap = fileCache.getBitmapFromFile(cachePath);
        //是否需要优化此显示方案
        BitmapDisplayer displayer = new BitmapDisplayer(imageView);
        imageView.post( () -> displayer.setImageBitmap(bitmap));
        return bitmap;
    }

    /**
     * 根据url加载相应的图片
     *
     * @param imageView 要加载图片的ImageView对象
     *
     * @param image 描述图片信息的MyImage对象
     *
     * @return 先从一级内存缓存中取图片，若有则直接返回，如果没有则异步从文件（二级缓存）中取，如果没有再从网络端获取，最终返回Bitmap对象
     *
     */
    public Bitmap loadBitmap(ImageView imageView, MyImage image) {
        mImageViews.put(imageView, image.getUrl());//先将ImageView记录到Map中,表示该ui已经执行过图片加载了
        Bitmap bitmap = memoryCache.get(image.getUrl());//先从一级缓存中获取图片
        if (bitmap == null) {
            enQueueLoadPhoto(imageView, image);//再从二级缓存或网络中获取
        } else {
            BitmapDisplayer displayer = new BitmapDisplayer(imageView);
            imageView.post(() -> displayer.setImageBitmap(bitmap));
        }
        return bitmap;//有则从一级缓存中返回
    }

    /**
     * 从文件缓存或网络端获取图片，若无网络则直接从本地缓存读取图片
     *
     * @param imageView 要加载图片的ImageView对象
     *
     * @param image 描述图片信息的MyImage对象
     */
    public Bitmap getBitmapByUrl(ImageView imageView, MyImage image) {
        File file = fileCache.getFile(image.getUrl());//获得缓存图片文件
        if (file.exists()) {//如果已经加载过，才读文件，否则从网络请求
            Log.d(TAG, "getBitmapByUrl: 缓存已存在");
            Bitmap bitmap = BitmapFactory.decodeFile(file.getPath());//HttpRequest.decodeFile(file);//获得文件的Bitmap信息
            if (bitmap != null) {
                return bitmap;
            }
        }
//        if (!isNetworkConnected(context)) {//无网络的情况
//            Log.d(TAG, "getBitmapByUrl: 无网络");
//            if (f.exists()) {//如果已经加载过，才读文件，否则从网络请求
//                Log.d(TAG, "getBitmapByUrl: 缓存已存在");
//                Bitmap b = HttpRequest.decodeFile(f);//获得文件的Bitmap信息
//                if (b != null)//不为空表示获得了缓存的文件
//                    return b;
//            }
//        }
        return HttpRequest.loadBitmapFromWeb(imageView, image, file);//从网络获得图片
    }


    /**
     * 将图片加载加入队列
     *
     * @param imageView 要加载图片的ImageView对象
     *
     * @param image 描述图片信息的MyImage对象
     *
     */
    private void enQueueLoadPhoto(ImageView imageView, MyImage image) {
        //如果任务已经存在，则不重新添加
        if (isTaskExisted(image.getUrl())) {
            return;
        }
        LoadPhotoTask task = new LoadPhotoTask(imageView, image);
        synchronized (taskQueue) {//加锁，防止重复添加
            taskQueue.add(task);//将任务添加到队列中
            Log.d(TAG, "enQueueLoadPhoto: task added " + task.url + ", total task : " + taskQueue.size());
        }

        mExecutorService.execute(task);//向线程池中提交任务
    }

    /**
     * 判断下载队列中是否已经存在该任务
     *
     * @param url
     *
     * @return
     */
    private boolean isTaskExisted(String url) {
        if(url == null) {
            return false;
        }
        synchronized (taskQueue) {
            int size = taskQueue.size();
            for(int i=0; i<size; i++) {
                LoadPhotoTask task = taskQueue.get(i);
                if(task != null && task.getUrl().equals(url)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 判断设备是否联网
     *
     * @param context
     *
     * @return 若设备已联网则返回true
     *
     * @exception
     */
    public static boolean isNetworkConnected(Context context) {
        if (context !=null) {
            ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = manager.getActiveNetworkInfo();
            if (networkInfo != null) {
                return networkInfo.isAvailable();
            }
        }
        return false;
    }



    /**
     * 判断该ImageView是否已经加载过图片了（可用于判断是否需要加载图片）
     *
     * @param imageView
     *
     * @param url
     *
     * @return
     */
    public boolean imageViewReused(ImageView imageView, String url) {
        String tag = mImageViews.get(imageView);
        if (tag == null || !tag.equals(url)) {
            return true;
        }
        return false;
    }

    /**
     * 将任务task从队列中移除
     *
     * @param task
     *
     * @return void
     *
     * @exception
     */
    public void removeTask(LoadPhotoTask task) {
        synchronized (taskQueue) {
            taskQueue.remove(task);
        }
    }

    /**
     * 暂停图片的加载
     * @return void
     */
    public void pause() {
        PhotoLoadThreadPoolExecutor.pause();
    }

    /**
     * 恢复图片加载
     * @return void
     */
    public void resume() {
        PhotoLoadThreadPoolExecutor.resume();
    }

    /**
     * 释放资源
     */
    public void release() {
        memoryCache.clearCache();
        memoryCache = null;
        mImageViews.clear();
        mImageViews = null;
        taskQueue.clear();
        taskQueue = null;
        mExecutorService.shutdown();
        mExecutorService = null;
    }



    class LoadPhotoTask implements Runnable{

        private static final String TAG = "LoadPhotoTask";
        private ImageView imageView;
        private MyImage image;
        private String url;


        LoadPhotoTask(ImageView imageView, MyImage image) {
            this.imageView = imageView;
            this.image = image;
            this.url = image.getUrl();
        }

        @Override
        public void run() {
            if (imageViewReused(imageView, url)) {//判断ImageView是否已经被复用
                Log.d(TAG, " imageViewReused !");
                removeTask(this);//如果已经被复用则删除任务
                return;
            }
            Bitmap bmp = getBitmapByUrl(imageView, image);//从缓存文件或者网络端获取图片
            memoryCache.put(url, bmp);// 将图片放入到一级缓存中
            if (!imageViewReused(imageView, url)) {//若ImageView未加载图片则在ui线程中显示图片
                BitmapDisplayer displayer = new BitmapDisplayer(imageView);
                imageView.post(() -> displayer.setImageBitmap(bmp));
            }
            removeTask(this);//从队列中移除任务
        }

        public String getUrl() {
            return url;
        }
    }

}




package com.wisstudio.devilwizard.photobrowserapp.util;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.ImageView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author WizardK
 * @date 2021-04-07
 */
public class ImageLoader {

    //使用单例，减少性能消耗
    private volatile static ImageLoader imageLoader;
    private final Context context;
    private static final String TAG = "ImageLoader";
    private MemoryCache memoryCache;
    private FileCache fileCache;
    private ExecutorService executorService;

    /**
     * 记录已经加载图片的ImageView
     */
    private Map<ImageView, String> mImageViews = Collections.synchronizedMap(new WeakHashMap<>());
    
    /**
     * 保存正在加载图片的url
     */
    private List<LoadPhotoTask> taskQueue = new ArrayList<>();

    /**
     * 默认采用一个大小为5的线程池
     *
     * @param context
     *
     * @param memoryCache 所采用的内存缓存
     *
     * @param fileCache 所采用的文件缓存
     */
    public ImageLoader(Context context, MemoryCache memoryCache, FileCache fileCache) {
        this.context = context;
        this.memoryCache = memoryCache;
        this.fileCache = fileCache;
        executorService = Executors.newFixedThreadPool(10);
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
        Log.d(TAG, "url2path: " + rawPath);
        return rawPath;
    }

    /**
     * 根据url加载相应的图片
     *
     * @param imageView 要加载图片的ImageView对象
     *
     * @param url 图片的url地址
     *
     * @return 先从一级内存缓存中取图片，若有则直接返回，如果没有则异步从文件（二级缓存）中取，如果没有再从网络端获取
     */
    public Bitmap loadBitmap(ImageView imageView, String url) {
        mImageViews.put(imageView, url);//先将ImageView记录到Map中,表示该ui已经执行过图片加载了
        Bitmap bitmap = memoryCache.get(url);//先从一级缓存中获取图片
        if (bitmap == null) {
            enQueueLoadPhoto(url, imageView);//再从二级缓存或网络中获取
        }
        return bitmap;//有则从一级缓存中返回
    }

    /**
     * 将图片加载加入队列
     *
     * @param url 图片的url地址
     *
     * @param imageView 要加载图片的ImageView对象
     *
     */
    private void enQueueLoadPhoto(String url, ImageView imageView) {
        //如果任务已经存在，则不重新添加
        if (isTaskExisted(url)) {
            return;
        }
        LoadPhotoTask task = new LoadPhotoTask(url, imageView);
        synchronized (taskQueue) {//加锁，防止重复添加
            taskQueue.add(task);//将任务添加到队列中
        }
        executorService.execute(task);//向线程池中提交任务
    }

    /**
     * 判断下载队列中是否已经存在该任务
     *
     * @param url
     *
     * @return
     */
    private boolean isTaskExisted(String url) {
        if(url == null)
            return false;
        synchronized (taskQueue) {
            int size = taskQueue.size();
            for(int i=0; i<size; i++) {
                LoadPhotoTask task = taskQueue.get(i);
                if(task != null && task.getUrl().equals(url))
                    return true;
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
     * 从缓存文件或网络端获取图片，若无网络则直接从缓存读取图片
     *
     * @param url
     */
    private Bitmap getBitmapByUrl(String url) {
        File f = fileCache.getFile(url);//获得缓存图片路径
        if (f.exists()) {//如果已经加载过，才读文件，否则从网络请求
            Log.d(TAG, "getBitmapByUrl: 缓存已存在");
            Bitmap b = HttpRequest.decodeFile(f);//获得文件的Bitmap信息
            if (b != null)//不为空表示获得了缓存的文件
                return b;
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
        return HttpRequest.loadBitmapFromWeb(url, f);//从网络获得图片
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
    private boolean imageViewReused(ImageView imageView, String url) {
        String tag = mImageViews.get(imageView);
        if (tag == null || !tag.equals(url))
            return true;
        return false;
    }

    private void removeTask(LoadPhotoTask task) {
        synchronized (taskQueue) {
            taskQueue.remove(task);
        }
    }

    class LoadPhotoTask implements Runnable{

        private String url;
        private ImageView imageView;
        LoadPhotoTask(String url, ImageView imageView) {
            this.url = url;
            this.imageView = imageView;
        }

        @Override
        public void run() {
            if (imageViewReused(imageView, url)) {//判断ImageView是否已经被复用
                removeTask(this);//如果已经被复用则删除任务
                return;
            }
            Bitmap bmp = getBitmapByUrl(url);//从缓存文件或者网络端获取图片
            memoryCache.put(url, bmp);// 将图片放入到一级缓存中
            if (!imageViewReused(imageView, url)) {//若ImageView未加载图片则在ui线程中显示图片
                BitmapDisplayer bd = new BitmapDisplayer(bmp, imageView, url);
                Activity a = (Activity) imageView.getContext();
                a.runOnUiThread(bd);//在UI线程调用bd组件的run方法，实现为ImageView控件加载图片
            }
            removeTask(this);//从队列中移除任务
        }
        public String getUrl() {
            return url;
        }

        /**
         *
         *由UI线程中执行该组件的run方法
         */
        class BitmapDisplayer implements Runnable {
            private Bitmap bitmap;
            private ImageView imageView;
            private String url;
            public BitmapDisplayer(Bitmap b, ImageView imageView, String url) {
                bitmap = b;
                this.imageView = imageView;
                this.url = url;
            }
            public void run() {
                if (imageViewReused(imageView, url))
                    return;
                if (bitmap != null)
                    imageView.setImageBitmap(bitmap);
            }
        }
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
        executorService.shutdown();
        executorService = null;
    }

}




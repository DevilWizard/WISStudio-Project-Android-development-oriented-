package com.wisstudio.devilwizard.photobrowserapp.util.image.load;

import android.graphics.Bitmap;
import android.widget.ImageView;

import com.wisstudio.devilwizard.photobrowserapp.cache.disk.FileCache;
import com.wisstudio.devilwizard.photobrowserapp.cache.memory.MemoryCache;
import com.wisstudio.devilwizard.photobrowserapp.db.PhotoDataBaseManager;
import com.wisstudio.devilwizard.photobrowserapp.ui.MainActivity;
import com.wisstudio.devilwizard.photobrowserapp.util.image.MyImage;
import com.wisstudio.devilwizard.photobrowserapp.util.image.display.BitmapDisplayer;
import com.wisstudio.devilwizard.photobrowserapp.util.logutil.MyLog;
import com.wisstudio.devilwizard.photobrowserapp.util.network.HttpRequest;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * 图片加载的核心类，图片显示的主要操作{@link #loadBitmap(ImageView, MyImage)},
 * {@link #loadPhotoFromFileCache(String, ImageView, PhotoDataBaseManager)}都在这里实现
 * 本类使用了单例设计模式，防止ImageLoader的频繁创建，减少性能消耗
 *
 * @author WizardK
 * @date 2021-04-07
 */
public class ImageLoader {

    /**
     * 本类的唯一实例
     */
    private volatile static ImageLoader instance;

    private static final String TAG = "ImageLoader";

    /**
     * 内存缓存实例
     */
    private final MemoryCache memoryCache;

    /**
     * 文件缓存实例
     */
    private final FileCache fileCache;

    /**
     * 线程池管理器
     */
    private final ExecutorService mExecutorService;

    /**
     * 记录已经加载图片的ImageView
     */
    private final Map<ImageView, String> mImageViews;
    
    /**
     * 保存正在加载图片的url
     */
    private final List<LoadPhotoTask> taskQueue;

    /**
     * 用于在{@link #getInstance(MemoryCache, FileCache, int)}中初始化ImageLoader
     *
     * @param memoryCache 内存缓存实例
     * @param fileCache 文件缓存实例
     * @param maxThreads 用于异步加载图片线程池的最大线程数
     */
    private ImageLoader(MemoryCache memoryCache, FileCache fileCache, int maxThreads) {
        this.fileCache = fileCache;
        this.memoryCache = memoryCache;
        this.mImageViews = new ConcurrentHashMap<>(); //Collections.synchronizedMap(new WeakHashMap<>());
        this.taskQueue = new ArrayList<>();
        mExecutorService = PhotoLoadThreadPoolExecutor.newFixedPhotoLoadPool(maxThreads);
    }

    /**
     * 使用双重加锁机制实现单例，保证多线程安全，同时支持懒加载
     * 该方法应该在第一次创建ImageLoader实例时调用，在这之后若需要获取实例应当调用{@link #getInstance()}
     *
     * @param memoryCache 内存缓存实例
     * @param fileCache 文件缓存实例
     * @param maxThreads 用于异步加载图片线程池的最大线程数
     *
     * @return 返回创建的单例对象@see{@link #instance}
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
     * 返回ImageLoader已创建的单例{@link #instance}
     *
     * @return 返回调用 {@link #getInstance(MemoryCache, FileCache, int)}后创建的单例{@link #instance}
     *
     * @exception NullPointerException
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
     * @return 返回图片储存的目录名，若url="https://picsum.photos/id/0/5616/3744"，则返回"056163744"
     */
    public static String url2path(String url) {
        String rawPath = url.substring(url.lastIndexOf("d") + 1);
        rawPath = rawPath.replace("/","");
        return rawPath;//是否需要储存这些路径，方便插入数据库
    }

    /**
     * 在设备处于无联网状态时，直接通过数据库从文件缓存中加载图片
     *
     * @param url 要加载的图片的网址
     * @param imageView 要加载的图片的{@link ImageView}
     * @param manager
     *
     * @return 返回文件缓存中图片的Bitmap
     */
    public Bitmap loadPhotoFromFileCache(String url, ImageView imageView, PhotoDataBaseManager manager) {
        String cachePath = manager.getPhotoCachePath(url);
        Bitmap bitmap = fileCache.getBitmapFromFile(cachePath, memoryCache);
        //是否需要优化此显示方案
        BitmapDisplayer displayer = new BitmapDisplayer(imageView);
        MainActivity.getMainActivity().runOnUiThread(() -> displayer.setBitmap(bitmap));
        return bitmap;
    }

    /**
     * 根据url加载相应的图片
     *
     * @param imageView 要加载图片的ImageView对象
     *
     * @param image 描述图片信息的MyImage对象
     *
     * @return 先从一级内存缓存中取图片 {@link MemoryCache#get(String)}，若有则直接返回，
     *         如果没有则异步从文件（二级缓存）中取{@link FileCache#getBitmapFromFile(String, MemoryCache)}，
     *         如果没有再从网络端获取{@link HttpRequest#loadBitmapFromWeb(ImageView, MyImage, File)}，最终返回Bitmap对象
     *
     * @see #enQueueLoadPhoto(ImageView, MyImage)
     *
     */
    public Bitmap loadBitmap(ImageView imageView, MyImage image) {
        //mImageViews可能为空
        if (mImageViews == null) {
            MyLog.d(TAG, "loadBitmap: " + "mImageViews is null !");
        }
        if (imageView != null) {
            mImageViews.put(imageView, image.getUrl());//先将ImageView记录到Map中,表示该imageView已经执行过图片加载了
        }

        Bitmap bitmap = memoryCache.get(image.getUrl());//先从一级缓存中获取图片
        if (bitmap == null) {
            enQueueLoadPhoto(imageView, image);//再从二级缓存或网络中获取
        } else {
            BitmapDisplayer displayer = new BitmapDisplayer(imageView);
            MainActivity.getMainActivity().runOnUiThread(() -> displayer.setBitmap(bitmap));
        }
        return bitmap;//有则从一级缓存中返回
    }

    /**
     * 从文件缓存{@link FileCache}或网络端{@link HttpRequest}获取图片
     *
     * @param imageView 要加载图片的ImageView对象
     *
     * @param image 描述图片信息的MyImage对象
     *
     */
    public Bitmap getBitmapByUrl(ImageView imageView, MyImage image) {
        File file = fileCache.getFile(image.getUrl());//获得缓存图片文件
        if (file.exists()) {//如果已经加载过，才读文件，否则从网络请求
            MyLog.d(TAG, "getBitmapByUrl: 缓存已存在");
            Bitmap bitmap = fileCache.getBitmapFromFile(file.getAbsolutePath(), memoryCache);//获得文件的Bitmap信息
            if (bitmap != null) {
                return bitmap;
            }
        }
        return HttpRequest.loadBitmapFromWeb(imageView, image, file);//从网络获得图片
    }

    /**
     * 在加载图片前判断传入的ImageView是否已经加载过其他图片了（可用于判断是否需要加载图片）
     *
     * @param imageView 要判断是否加载过图片的imageView
     *
     * @param url 要加载的图片的url网址
     *
     * @return 若该imageView已加载过图片则返回true，否则返回false
     */
    public boolean isImageViewReused(ImageView imageView, String url) {
        String tag = mImageViews.get(imageView);
        return tag != null && !Objects.equals(tag, url);
    }

    /**
     * 将任务task从队列中移除
     *
     * @param task 要从队列{@link #taskQueue}中移除的任务
     */
    public void removeTask(LoadPhotoTask task) {
        synchronized (taskQueue) {
            taskQueue.remove(task);
        }
    }

    /**
     * 暂停图片的加载
     */
    public void pause() {
        PhotoLoadThreadPoolExecutor.pause();
    }

    /**
     * 恢复图片加载
     */
    public void resume() {
        PhotoLoadThreadPoolExecutor.resume();
    }

    public MemoryCache getMemoryCache() {
        return memoryCache;
    }

    public FileCache getFileCache() {
        return fileCache;
    }

    /**
     * 释放资源
     */
    public void release() {
        memoryCache.clearCache();
        mImageViews.clear();
        taskQueue.clear();
    }

    /**
     * 将图片加入加载队列{@link #taskQueue}，然后通过线程池{@link #mExecutorService}执行加载
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
            MyLog.d(TAG, "enQueueLoadPhoto: task added " + task.url + ", total task : " + taskQueue.size());
        }

        mExecutorService.execute(task);//向线程池中提交任务
    }

    /**
     * 判断任务队列{@link #taskQueue}中是否已经存在网址为url的{@link LoadPhotoTask}任务
     *
     * @param url 要判断的任务的{@link LoadPhotoTask#url}
     *
     * @return 若任务已存在则返回true，否则返回false
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
     * 加载图片的任务类
     */
    class LoadPhotoTask implements Runnable{

        private static final String TAG = "LoadPhotoTask";
        private final ImageView imageView;
        private final MyImage image;
        private final String url;

        LoadPhotoTask(ImageView imageView, MyImage image) {
            this.imageView = imageView;
            this.image = image;
            this.url = image.getUrl();
        }

        @Override
        public void run() {
            if (isImageViewReused(imageView, url)) {//判断ImageView是否已经被复用
                MyLog.d(TAG, " imageViewReused !");
                removeTask(this);//如果已经被复用则删除任务
                return;
            }
            Bitmap bmp = getBitmapByUrl(imageView, image);//从缓存文件或者网络端获取图片
            if (bmp != null) {
                ImageLoader.getInstance().getMemoryCache().put(url, bmp);// 将图片放入到一级缓存中
            }
            if (!isImageViewReused(imageView, url)) {//若ImageView未加载图片则在ui线程中显示图片
                BitmapDisplayer displayer = new BitmapDisplayer(imageView);
                MainActivity.getMainActivity().runOnUiThread(() -> displayer.setBitmap(bmp));
            }
            removeTask(this);//加载完后从队列中移除任务
        }

        public String getUrl() {
            return url;
        }
    }
}

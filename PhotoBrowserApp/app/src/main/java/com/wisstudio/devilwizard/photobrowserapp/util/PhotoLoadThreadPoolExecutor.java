package com.wisstudio.devilwizard.photobrowserapp.util;

import android.util.Log;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 自定义的图片加载线程池，添加了暂停和恢复当前执行任务的功能
 * @author WizardK
 * @date 2021-04-12
 */
public class PhotoLoadThreadPoolExecutor extends ThreadPoolExecutor {

    private static final String TAG = "PhotoLoadThreadPool";
    private static boolean isPause = false;
    private static ReentrantLock lock = new ReentrantLock();
    private static Condition condition = lock.newCondition();

    public PhotoLoadThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    /**
     * 创建一个用于加载图片的固定大小的线程池效果与{@code Executors.newFixedThreadPool(int)}相同
     *
     * @param nThreads 创建的线程池大小
     *
     * @return com.wisstudio.devilwizard.photobrowserapp.util.PhotoLoadThreadPoolExecutor
     *
     * @see java.util.concurrent.Executors
     *
     * @exception
     */
    public static PhotoLoadThreadPoolExecutor newFixedPhotoLoadPool(int nThreads) {
        if (nThreads <= 0) {
            throw new IllegalArgumentException("the pool size must larger than 0 ");
        }
        return new PhotoLoadThreadPoolExecutor(nThreads, nThreads,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>());
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        super.beforeExecute(t, r);
        lock.lock();
        try {
            while (isPause){
                long ms = 10L;
                Log.d(TAG, "beforeExecute: " + "pausing, " + t.getName());
                Thread.sleep(ms);
                condition.await();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 暂停线程池中的任务
     * @param
     *
     * @return void
     */
    public static void pause() {
        lock.lock();
        Log.d(TAG, "pause: ");
        isPause = true;
        lock.unlock();
    }

    /**
     * 继续线程池中的任务
     * @param
     *
     * @return void
     */
    public static void resume() {
        lock.lock();
        Log.d(TAG, "resume: ");
        isPause = false;
        condition.signalAll();
        lock.unlock();
    }

}

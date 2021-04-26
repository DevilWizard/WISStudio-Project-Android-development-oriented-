package com.wisstudio.devilwizard.photobrowserapp.util;

import android.app.Application;
import android.content.Context;


/**
 * 用于保存全局Context对象的类，在应用启动时就会初始化该类
 *
 * @author WizardK
 * @date 2021-04-17
 */
public class MyApplication extends Application {
    private static Context context;
    //这里会有警告“Do not place Android context classes in static fields; this is a memory leak”
    //Google了一下，大部分是说不要直接保存全局Context的引用，因为当Context被引用时并不会被垃圾回收器回收因此可能出现内存泄漏
    //所以大伙给出几个解决办法：1.需要的时候在函数里传入context参数 2.用WeakReference<Context>保存 3.用的时候直接getApplicationContext()

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
    }

    /**
     * 获取应用的全局唯一的Context（通过{@link #getApplicationContext()}获得的context）
     * @return 返回应用的全局Context对象
     */
    public static Context getContext() {
        return context;
    }
}

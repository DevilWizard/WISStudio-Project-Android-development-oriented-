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

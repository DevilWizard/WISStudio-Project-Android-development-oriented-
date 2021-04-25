package com.wisstudio.devilwizard.photobrowserapp.util.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.wisstudio.devilwizard.photobrowserapp.util.MyApplication;

/**
 * 用于监测设备网络状态的类
 * @author WizardK
 * @date 2021-04-21
 */
public class NetWorkState {

    /**
     * 判断设备是否联网
     *
     * @param context 全局的{@link Context}对象，可通过{@link MyApplication#getApplicationContext()}获取
     *
     * @return 若设备已联网则返回true，否则返回false
     *
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

}

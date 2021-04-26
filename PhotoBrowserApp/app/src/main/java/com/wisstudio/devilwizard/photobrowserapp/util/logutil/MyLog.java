package com.wisstudio.devilwizard.photobrowserapp.util.logutil;

import android.util.Log;

/**
 * 自定义日志打印工具，添加屏蔽打印功能
 *
 * @author WizardK
 * @date 2021-04-24
 */
public class MyLog {

    /**
     * @Hide @see android.util.Log#VERBOSE
     */
    public static final int VERBOSE = 1;

    /**
     * @see android.util.Log#DEBUG
     */
    public static final int DEBUG = 2;

    /**
     * @see android.util.Log#INFO
     */
    public static final int INFO = 3;

    /**
     * @see android.util.Log#WARN
     */
    public static final int WARN = 4;

    /**
     * @see android.util.Log#ERROR
     */
    public static final int ERROR = 5;

    /**
     * 此级别为最高级别，会屏蔽所有级别的打印，即无任何打印日志，一般只用于app正式发布时，
     */
    public static final int NOTHING = 6;

    /**
     * 日志打印的最低级别，用于屏蔽level级别以下的打印，
     * 比如若level=DEBUG，则只会打印DEBUG及以上的日志，而VERBOSE级别的日志不会打印
     */
    public static int level = NOTHING;

    /**
     * @see Log#v(String, String)
     */
    public static void v(String tag, String msg) {
        if (level <= VERBOSE) {
            Log.v(tag, msg);
        }
    }

    /**
     * @see Log#d(String, String)
     */
    public static void d(String tag, String msg) {
        if (level <= DEBUG) {
            Log.d(tag, msg);
        }
    }

    /**
     * @see Log#i(String, String)
     */
    public static void i(String tag, String msg) {
        if (level <= INFO) {
            Log.i(tag, msg);
        }
    }

    /**
     * @see Log#w(String, String)
     */
    public static void w(String tag, String msg) {
        if (level <= WARN) {
            Log.w(tag, msg);
        }
    }

    /**
     *  @see Log#e(String, String)
     */
    public static void e(String tag, String msg) {
        if (level <= ERROR) {
            Log.e(tag, msg);
        }
    }
}

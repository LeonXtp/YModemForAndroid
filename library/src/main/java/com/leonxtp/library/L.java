package com.leonxtp.library;

import android.util.Log;

public class L {

    private static final String TAG = "YMODEM";

    private L() {
        /* cannot be instantiated */
        throw new UnsupportedOperationException("cannot be instantiated");
    }

    private static final boolean DEBUGGING = true;

    public static void f(String msg) {
        if (DEBUGGING) {
            L.e(TAG, msg);
        }
    }

    // 下面四个是默认tag的函数  
    public static void i(String msg) {
        if (DEBUGGING)
            L.i(TAG, msg);
    }

    public static void d(String msg) {
        if (DEBUGGING)
            L.d(TAG, msg);
    }

    public static void e(String msg) {
        if (DEBUGGING)
            L.e(TAG, msg);
    }

    public static void v(String msg) {
        if (DEBUGGING)
            L.v(TAG, msg);
    }

    public static void w(String msg) {
        if (DEBUGGING)
            L.w(TAG, msg);
    }

    // 下面是传入自定义tag的函数  
    public static void i(String tag, String msg) {
        if (DEBUGGING)
            Log.i(tag, msg);
    }

    public static void d(String tag, String msg) {
        if (DEBUGGING)
            Log.d(tag, msg);
    }

    public static void e(String tag, String msg) {
        if (DEBUGGING)
            Log.e(tag, msg);
    }

    public static void v(String tag, String msg) {
        if (DEBUGGING)
            Log.v(tag, msg);
    }

    public static void w(String tag, String msg) {
        if (DEBUGGING)
            Log.w(tag, msg);
    }

    public static void e(String tag, String msg, Throwable throwable) {
        if (DEBUGGING)
            Log.e(tag, msg, throwable);
    }

    public static void wtf(String tag, String msg) {
        if (DEBUGGING)
            Log.wtf(tag, msg);
    }

}
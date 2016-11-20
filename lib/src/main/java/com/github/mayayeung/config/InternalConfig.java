package com.github.mayayeung.config;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 此类必须在程序刚刚启动的时候调用init函数注册，程序的运行中就可以通过此类来获取全局的Application
 * 最好的实现是自定义一个Application的子类，然后在此子类的初始化中注册
 *
 * Created by Martin on 2016/2/26.
 */
public class InternalConfig {
    private static final String TAG = InternalConfig.class.getSimpleName();
    private static boolean debug = true;//是否在调试模式下
    private static Application application;// 必须需要显式设置
    private static String pkgName;
    private static ExecutorService es;// 系统全局的线程池，Application启动的时候创建，不需要销毁
    private static ScheduledExecutorService scheduleEs;//定时器线程池
    private static Handler handler;// 主线程的handler，用于方便post一些事情做主线程去做
    private static LocalBroadcastManager localBroadcastManager;
    private static final int MAX_THREAD_NUM = 5;
    private static final int MAX_SCHEDUALED_THREAD_NUM = 2;
    /*
    private static WeakReference<Activity> currentActivity;// 当前正在显示的Activity
    private static AssetReader assetReader;// 有默认值，可以不设置
    private static CityManager cityManager;
    private static ActivityLeavedLongListener activityLeavedLongListener;
    private static UserCityProvider userCityProvider;
    */

    public static void init(Application application) {
        localBroadcastManager = LocalBroadcastManager.getInstance(application);
        es = Executors.newFixedThreadPool(MAX_THREAD_NUM);
        scheduleEs = Executors.newScheduledThreadPool(MAX_SCHEDUALED_THREAD_NUM);
        InternalConfig.application = application;
        handler = new Handler(Looper.getMainLooper());
        pkgName = application.getPackageName();
    }

    public static Application getContext() {
        return application;
    }

    public static String getPackageName() {
        return pkgName;
    }

    public static void setDebug(boolean debug) {
        InternalConfig.debug = debug;
    }

    public static boolean isDebug() {
        return debug;
    }

    public static LocalBroadcastManager getLocalBroadcastManager() {
        return localBroadcastManager;
    }

    public static <T> Future<T> submit(Callable<T> call) {
        if (es != null && !es.isShutdown()) {
            return es.submit(call);
        }
        return null;
    }

    public static void postOnUiThread(Runnable task) {
        handler.post(task);
    }

    public static void postDelayOnUiThread(Runnable task, long delayMillis) {
        handler.postDelayed(task, delayMillis);
    }

    public static void execute(Runnable task) {
        if (es != null && !es.isShutdown()) {
            es.execute(task);
        }
    }

    public static void executeAtFixedRate(Runnable task, long initialDelayMillis, long periodMillis) {
        if (scheduleEs != null && !scheduleEs.isShutdown()) {
            scheduleEs.scheduleAtFixedRate(task, initialDelayMillis, periodMillis, TimeUnit.MILLISECONDS);
        }
    }

    public static SharedPreferences getCommonSharedPreferences() {
        return application.getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);
    }

    public static void shutdown() {
        if (es != null && !es.isShutdown()) {
            es.shutdown();
        }
        if (scheduleEs != null && !scheduleEs.isShutdown()) {
            scheduleEs.shutdown();
        }
    }
}

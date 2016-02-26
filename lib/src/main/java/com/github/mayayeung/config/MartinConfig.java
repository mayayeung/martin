package com.github.mayayeung.config;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;

import com.github.mayayeung.utils.MiscUtils;

import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 此类必须在程序刚刚启动的时候注册，程序的运行中就可以通过此类来获取全局的Application
 * 最好的实现是自定义一个Application的子类，然后在此子类的初始化中注册
 *
 * Created by Martin on 2016/2/26.
 */
public class MartinConfig {
    private static final String TAG = MartinConfig.class.getSimpleName();
    private static final String MARTIN_LAUNCH_STAT = "__martin_launch_stat";
    private static WeakReference<Activity> currentActivity;// 当前正在显示的Activity
    private static boolean debug = true;//是否在调试模式下
    private static AssetReader assetReader;// 有默认值，可以不设置
    private static Application application;// 必须需要显式设置
    private static CityManager cityManager;

    // 系统全局的线程池，Application启动的时候创建，不需要销毁
    private static ExecutorService es;
    private static Handler handler;// 主线程的handler，用于方便post一些事情做主线程去做
    private static ActivityLeavedLongListener activityLeavedLongListener;
    private static UserCityProvider userCityProvider;
    private static LocalBroadcastManager localBroadcastManager;
    private static boolean mainProcess;

    public static void init(Application application) {
        localBroadcastManager = LocalBroadcastManager.getInstance(application);
        // 首先是生成线程池，最多10个线程,最少1个，闲置1分钟后线程退出
        es = Executors.newFixedThreadPool(10);
        assetReader = new DefaultAssetReader(application);
        MartinConfig.application = application;
        // 调用此方法触发保存的动作
        if (isMainProcess()) {
            getFirstLaunchTime();
        }
        handler = new Handler(Looper.getMainLooper());
    }

    public static String getFirstLaunchTime() {
        SharedPreferences prefs = application.getSharedPreferences(MARTIN_LAUNCH_STAT, Application.MODE_PRIVATE);
        String firstTime = prefs.getString("ft", "");
        if (MiscUtils.isEmpty(firstTime)) {
            firstTime = MiscUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss");
            Editor editor = prefs.edit();
            editor.putString("ft", firstTime);
            editor.commit();
        }
        return firstTime;
    }

    public static int addLaunchCount() {
        SharedPreferences prefs = application.getSharedPreferences(MARTIN_LAUNCH_STAT, Application.MODE_PRIVATE);
        int count = prefs.getInt("lc", 0) + 1;
        Editor editor = prefs.edit();
        editor.putInt("lc", count);
        editor.commit();
        return count;
    }

    public static int getLaunchCount() {
        SharedPreferences prefs = application.getSharedPreferences(MARTIN_LAUNCH_STAT, Application.MODE_PRIVATE);
        return prefs.getInt("lc", 0);
    }

    public static long getLastPauseTime() {
        SharedPreferences prefs = application.getSharedPreferences(MARTIN_LAUNCH_STAT, Application.MODE_PRIVATE);
        return prefs.getLong("lastPauseTime", -1L);
    }

    public static void updateLastPauseTime() {
        SharedPreferences prefs = application.getSharedPreferences(MARTIN_LAUNCH_STAT, Application.MODE_PRIVATE);
        Editor editor = prefs.edit();
        editor.putLong("lastPauseTime", System.currentTimeMillis());
        editor.commit();
    }

    public static String getPackageName() {
        Context context = getContext();
        if (context != null) {
            return context.getPackageName();
        }
        return null;
    }

    public static Application getContext() {
        return application;
    }

    static void setDebug(boolean debug) {
        MartinConfig.debug = debug;
    }

    public static boolean isDebug() {
        return debug;
    }

    public static boolean isMainProcess() {
        return mainProcess;
    }

    static void setMainProcess(boolean mainProcess) {
        MartinConfig.mainProcess = mainProcess;
    }


    public static LocalBroadcastManager getLocalBroadcastManager() {
        return localBroadcastManager;
    }

    public static <T> Future<T> submit(Callable<T> call) {
        return es.submit(call);
    }

    public static void postOnUiThread(Runnable task) {
        handler.post(task);
    }

    public static void postDelayOnUiThread(Runnable task, long delay) {
        handler.postDelayed(task, delay);
    }

    public static void execute(Runnable task) {
        es.execute(task);
    }

    public static UserCityProvider getUserCityProvider() {
        return userCityProvider;
    }

    public static void setUserCityProvider(UserCityProvider userCityProvider) {
        MartinConfig.userCityProvider = userCityProvider;
    }


}

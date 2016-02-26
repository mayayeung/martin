package com.github.mayayeung.utils;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import com.github.mayayeung.config.MartinConfig;

import java.io.Closeable;

/**
 * 此类专门负责读取程序的数据和资源 并且处理外部数据和程序内置数据的优先级关系
 *
 * Created by Martin on 2016/2/26.
 */
public class DataUtils {
    private static final String TAG = DataUtils.class.getSimpleName();

    public static DisplayMetrics getCurrentDisplayMetrics() {
        DisplayMetrics dm = new DisplayMetrics();
        WindowManager wm = (WindowManager) MartinConfig.getContext().getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(dm);
        return dm;
    }


    public static void close(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (Exception ex) {
            LogUtils.w(TAG, null, ex);
        }
    }

    public static void close(Cursor cursor) {
        try {
            if (cursor != null) {
                cursor.close();
            }
        } catch (Exception ex) {
            LogUtils.w(TAG, null, ex);
        }
    }

    public static void close(SQLiteDatabase db) {
        try {
            //在这里进行判断，如果系统的版本低于3.0，则不关闭
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB || db == null) {
                return;
            }
            db.releaseReference();
        } catch (Exception ex) {
            LogUtils.w(TAG, null, ex);
        }
    }

}

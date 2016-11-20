package com.github.mayayeung.utils;

import android.util.Log;

import com.github.mayayeung.config.InternalConfig;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Created by Martin on 2016/2/26.
 */
public class LogUtils {
    /**
     * Priority constant for the println method; use Log.v.
     */
    public static final int LEVEL_VERBOSE = 2;

    /**
     * Priority constant for the println method; use Log.d.
     */
    public static final int LEVEL_DEBUG = 3;

    /**
     * Priority constant for the println method; use Log.i.
     */
    public static final int LEVEL_INFO = 4;

    /**
     * Priority constant for the println method; use Log.w.
     */
    public static final int LEVEL_WARN = 5;

    /**
     * Priority constant for the println method; use Log.e.
     */
    public static final int LEVEL_ERROR = 6;
    /**
     * 表示关闭日志.
     */
    public static final int LEVEL_OFF = Integer.MAX_VALUE;

    private static class LevelHolder {
        static volatile int level = InternalConfig.isDebug() ? LEVEL_DEBUG : LEVEL_ERROR;
    }

    public static synchronized void setLevel(int newLevel) {
        LevelHolder.level = newLevel;
    }

    public static int getLevel() {
        return LevelHolder.level;
    }

    /**
     * Send a {@link #VERBOSE} log message.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static int v(String tag, String msg) {
        if (LEVEL_VERBOSE >= getLevel()) {
            return Log.v(tag, msg);
        }
        return 0;
    }

    /**
     * Send a {@link #DEBUG} log message.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static int d(String tag, String msg) {
        if (LEVEL_DEBUG >= getLevel()) {
            return Log.d(tag, msg);
        }
        return 0;
    }

    /**
     * Send an {@link #INFO} log message.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static int i(String tag, String msg) {
        if (LEVEL_INFO >= getLevel()) {
            return Log.i(tag, msg);
        }
        return 0;
    }

    /**
     * Send a {@link #INFO} log message and log the exception.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr  An exception to log
     */
    public static int i(String tag, String msg, Throwable tr) {
        if (LEVEL_INFO >= getLevel()) {
            return Log.i(tag, msg + '\n' + getStackTraceString(tr));
        }
        return 0;
    }

    /**
     * Send a {@link #WARN} log message.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static int w(String tag, String msg) {
        if (LEVEL_WARN >= getLevel()) {
            return Log.w(tag, msg);
        }
        return 0;
    }

    /**
     * Send a {@link #WARN} log message and log the exception.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    public static int w(String tag, String msg, Throwable tr) {
        if (LEVEL_WARN >= getLevel()) {
            return Log.w(tag, msg + '\n' + getStackTraceString(tr));
        }
        return 0;
    }

    /*
     * Send a {@link #WARN} log message and log the exception.
     *
     * @param tag Used to identify the source of a log message. It usually identifies the class or activity where the
     * log call occurs.
     *
     * @param tr An exception to log
     */
    public static int w(String tag, Throwable tr) {
        if (LEVEL_WARN >= getLevel()) {
            return Log.w(tag, getStackTraceString(tr));
        }
        return 0;
    }

    /**
     * Send an {@link #ERROR} log message.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static int e(String tag, String msg) {
        if (LEVEL_ERROR >= getLevel()) {
            return Log.e(tag, msg);
        }
        return 0;
    }

    /**
     * Send a {@link #ERROR} log message and log the exception.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    public static int e(String tag, String msg, Throwable tr) {
        if (LEVEL_ERROR >= getLevel()) {
            return Log.e(tag, msg + '\n' + getStackTraceString(tr));
        }
        return 0;
    }

    /**
     * Handy function to get a loggable stack trace from a Throwable.
     *
     * @param tr An exception to log
     */
    public static String getStackTraceString(Throwable tr) {
        if (tr == null) {
            return "";
        }
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        tr.printStackTrace(pw);
        return sw.toString();
    }

}

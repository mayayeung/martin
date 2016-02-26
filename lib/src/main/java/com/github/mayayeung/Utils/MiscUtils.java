package com.github.mayayeung.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.CallLog;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.DownloadListener;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.github.mayayeung.config.MartinConfig;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Martin on 2016/2/26.
 */
public class MiscUtils {
    private static final String TAG = MiscUtils.class.getSimpleName();

    /**
     * 根据资源路径获取ID值
     * @param context
     * @param fullName  如：“layout/main”
     * @return
     */
    public static int getResourcesIdentifier(Context context, String fullName) {
        return context.getResources().getIdentifier(context.getPackageName() + ":" + fullName, null, null);
    }


    /**
     * 根据jsonObject 得到属性key的值
     * @param jsonObject
     * @param key
     * @return
     */
    public static String optString(JSONObject jsonObject, String key) {
        return optString(jsonObject, key, "");
    }

    public static String optString(JSONObject jsonObject, String key, String fallback) {
        if (jsonObject.isNull(key)) {
            return fallback;
        }
        return jsonObject.optString(key, fallback);
    }

    public static boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }

    /**
     * 获取打电话时长
     * @param phoneNumber   电话号码
     * @param startTime     大概的开始时间
     * @param threshold     最多允许的开始时间误差
     * @return 通话时长，单位为秒
     */
    public static long getCallRecordDuration(String phoneNumber, long startTime, long threshold) {
        Cursor cursor = null;
        try {
            ContentResolver cr = MartinConfig.getContext().getContentResolver();
            cursor = cr.query(CallLog.Calls.CONTENT_URI, new String[]{CallLog.Calls.DATE, CallLog.Calls.DURATION},
                    CallLog.Calls.TYPE + "=" + CallLog.Calls.OUTGOING_TYPE + " and " +
                    CallLog.Calls.NUMBER + "=" + phoneNumber, null, "_id desc");
            long delta = Long.MAX_VALUE;
            long duration = -1L;

            while (cursor.moveToNext()) {
                long date = cursor.getLong(0);
                long time = cursor.getLong(1);
                if (Math.abs(startTime - date) < delta) {
                    delta = Math.abs(startTime - date);
                    duration = time;
                }
            }

            if (delta <= threshold) {
                return duration;
            }
        } catch (SecurityException ex) {
            ex.printStackTrace();
            LogUtils.w(TAG, "打电话权限被拦截");
        } catch (Exception ex) {
            ex.printStackTrace();
        }finally {
            DataUtils.close(cursor);
        }
        return -1L;
    }

    public static class CallLogResult {
        public long duration;
        public long logTime;
        public static final CallLogResult NULL_CURSOR = new CallLogResult(-1, -1);
        public static final CallLogResult EMPTY_CURSOR = new CallLogResult(-2, -2);
        public static final CallLogResult SECURITY_EXCEPTION = new CallLogResult(-3, -3);
        public static final CallLogResult UNKNOWN_EXCEPTION = new CallLogResult(-4, -4);

        public CallLogResult(long duration, long logTime) {
            this.duration = duration;
            this.logTime = logTime;
        }

        public boolean isValid() {
            return this.logTime > 0 && duration >= 0;
        }
    }

    /**
     * @param phoneNumber 电话号码
     * @param startTime   拨打时间
     * @return 电话号码在拨打时间后拨出的最近的一个电话时长
     */
    public static CallLogResult getNearestCallLog(String phoneNumber, long startTime) {
        ContentResolver cr = MartinConfig.getContext().getContentResolver();
        long date, duration;
        try {
            Cursor cursor = cr.query(CallLog.Calls.CONTENT_URI, new String[]{CallLog.Calls.DATE, CallLog.Calls.DURATION},
                    CallLog.Calls.TYPE + " = ?  and " + CallLog.Calls.NUMBER + " = ? and " + CallLog.Calls.DATE + " > ?",
                    new String[]{String.valueOf(CallLog.Calls.OUTGOING_TYPE), phoneNumber, String.valueOf(startTime)},
                    CallLog.Calls.DATE + " asc");
            if (cursor == null) {
                return CallLogResult.NULL_CURSOR;
            }
            if (cursor.getCount() == 0) {
                return CallLogResult.EMPTY_CURSOR;
            }

            cursor.moveToFirst();
            date = cursor.getLong(0);
            duration = cursor.getLong(1);
            return new CallLogResult(duration, date);
        } catch (SecurityException ex) {
            return CallLogResult.SECURITY_EXCEPTION;
        } catch (Exception e) {
            return CallLogResult.UNKNOWN_EXCEPTION;
        }
    }

    public static String getSharedPreferenceValue(String shareName, String key, String defaultValue) {
        SharedPreferences share = MartinConfig.getContext().getSharedPreferences(shareName, Context.MODE_PRIVATE);
        return share.getString(key, defaultValue);
    }

    public static boolean getSharedPreferenceValue(String shareName, String key, boolean defaultValue) {
        SharedPreferences share = MartinConfig.getContext().getSharedPreferences(shareName, Context.MODE_PRIVATE);
        return share.getBoolean(key, defaultValue);
    }

    public static int getSharedPreferenceValue(String shareName, String key, int defaultValue) {
        SharedPreferences share = MartinConfig.getContext().getSharedPreferences(shareName, Context.MODE_PRIVATE);
        return share.getInt(key, defaultValue);
    }

    public static float getSharedPreferenceValue(String shareName, String key, float defaultValue) {
        SharedPreferences share = MartinConfig.getContext().getSharedPreferences(shareName, Context.MODE_PRIVATE);
        return share.getFloat(key, defaultValue);
    }

    public static long getSharedPreferenceValue(String shareName, String key, long defaultValue) {
        SharedPreferences share = MartinConfig.getContext().getSharedPreferences(shareName, Context.MODE_PRIVATE);
        return share.getLong(key, defaultValue);
    }

    public static void setSharedPreferenceValue(String shareName, String key, String value) {
        SharedPreferences share = MartinConfig.getContext().getSharedPreferences(shareName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = share.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public static void setSharedPreferenceValue(String shareName, String key, boolean value) {
        SharedPreferences share = MartinConfig.getContext().getSharedPreferences(shareName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = share.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    public static void setSharedPreferenceValue(String shareName, String key, int value) {
        SharedPreferences share = MartinConfig.getContext().getSharedPreferences(shareName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = share.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    public static void setSharedPreferenceValue(String shareName, String key, float value) {
        SharedPreferences share = MartinConfig.getContext().getSharedPreferences(shareName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = share.edit();
        editor.putFloat(key, value);
        editor.apply();
    }

    public static void setSharedPreferenceValue(String shareName, String key, long value) {
        SharedPreferences share = MartinConfig.getContext().getSharedPreferences(shareName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = share.edit();
        editor.putLong(key, value);
        editor.apply();
    }

    public static boolean isLocationEnabled() {
        return isLocationProviderEnabled(LocationManager.GPS_PROVIDER) || isLocationProviderEnabled(
                LocationManager.NETWORK_PROVIDER);
    }

    public static boolean isLocationProviderEnabled(String provider) {
        LocationManager lm = (LocationManager) MartinConfig.getContext().getSystemService(Context.LOCATION_SERVICE);
        return lm.isProviderEnabled(provider);
    }

    /**
     * ???
     * @param carno
     * @return
     */
    public static boolean isValidCarno(String carno) {
        if (isEmpty(carno)) {
            return false;
        }
        try {
            String normal = "(WJ|[\u0391-\uFFE5])[a-zA-Z0-9]{6}"; // \u0391 = A, \uFFE5 = ￥
            String special = "(WJ|[\u0391-\uFFE5])[a-zA-Z0-9]{5}[\u0391-\uFFE5]"; // \u0391 = A, \uFFE5 = ￥
            return carno.matches(String.format("%s|%s", normal, special));
        } catch (Exception ex) {
            return false;
        }
    }

    public static String getUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static boolean isCellphone(String str) {
        Pattern pattern = Pattern.compile("^1[3-8]{1}\\d{9}");
        Matcher matcher = pattern.matcher(str);
        if (matcher.matches()) {
            return true;
        } else {
            return false;
        }
    }

    public static int getCurrentYear() {
        Calendar cal = Calendar.getInstance();
        return cal.get(Calendar.YEAR);
    }

    public static int getCurrentMonth() {
        Calendar cal = Calendar.getInstance();
        return cal.get(Calendar.MONTH) + 1;
    }

    public static void showMessageToast(final String message) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {

            public void run() {
                Toast.makeText(MartinConfig.getContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static void showMessageDialog(final Activity context, final String message) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {

            public void run() {
                if (context.isFinishing()) {
                    return;
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("消息");
                builder.setMessage(message);
                builder.setPositiveButton("确定", null);
                builder.create().show();
            }
        });
    }

    public static ProgressDialog showProgressDialog(Activity context, String title, String message, ProgressDialog pd) {
        if (context.isFinishing()) {
            return null;
        }
        if (pd == null) {
            pd = new ProgressDialog(context);
        }
        if (MiscUtils.isNotEmpty(title)) {
            pd.setTitle(title);
        }
        if (MiscUtils.isNotEmpty(message)) {
            pd.setMessage(message);
        }
        pd.show();
        return pd;
    }

    public static String getHost(String url) {
        try {
            Uri uri = Uri.parse(url);
            return uri.getHost();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "";
    }

    public static boolean checkSupportMarket(Context context) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("market://details?id=" + context.getPackageName()));
        return isIntentAvailable(context, intent);
    }

    public static boolean isIntentAvailable(Context context, Intent intent) {
        final PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return MiscUtils.isNotEmpty(list);
    }

    public static void viewInMarket(Context context) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("market://details?id=" + context.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception ex) {
            ex.printStackTrace();
            Toast.makeText(context, "当前手机不支持应用市场！", Toast.LENGTH_SHORT).show();
        }
    }

    public static void sendShare(Activity context, String content) {
        Intent it = new Intent(Intent.ACTION_SEND);
        it.putExtra(Intent.EXTRA_TEXT, content);
        it.setType("text/plain");
        Intent intent = Intent.createChooser(it, "请选择分享的方式");
        context.startActivity(intent);
    }

    public static void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static int computeSampleSize(BitmapFactory.Options options, int minSideLength, int maxNumOfPixels) {
        int initialSize = computeInitialSampleSize(options, minSideLength, maxNumOfPixels);
        int roundedSize;
        if (initialSize <= 8) {
            roundedSize = 1;
            while (roundedSize < initialSize) {
                roundedSize <<= 1;
            }
        } else {
            roundedSize = (initialSize + 7) / 8 * 8;
        }
        return roundedSize;
    }

    private static int computeInitialSampleSize(BitmapFactory.Options options, int minSideLength, int maxNumOfPixels) {
        double w = options.outWidth;
        double h = options.outHeight;
        int lowerBound = (maxNumOfPixels == -1) ? 1 : (int) Math.ceil(Math.sqrt(w * h / maxNumOfPixels));
        int upperBound = (minSideLength == -1) ? 128 : (int) Math
                .min(Math.floor(w / minSideLength), Math.floor(h / minSideLength));
        if (upperBound < lowerBound) {
            return lowerBound;
        }
        if ((maxNumOfPixels == -1) && (minSideLength == -1)) {
            return 1;
        } else if (minSideLength == -1) {
            return lowerBound;
        } else {
            return upperBound;
        }
    }

    public static String getDaysDesc(int days) {
        if (days < 0) {
            return "已过期";
        } else if (days == 0) {
            return "今天";
        } else if (days == 1) {
            return "明天";
        } else if (days == 2) {
            return "后天";
        } else {
            return days + "天";
        }
    }


    public static int getFontSizeOfWidth(String content, int width) {
        Paint paint = new Paint();
        int fontSize = MiscUtils.getPxByDip(24);
        paint.setTextSize(fontSize);
        int realWidth = (int) paint.measureText(content);
        if (realWidth > width) {
            while (fontSize > 1) {
                fontSize--;
                paint.setTextSize(fontSize);
                realWidth = (int) paint.measureText(content);
                if (realWidth <= width) {
                    break;
                }
            }
        } else if (realWidth < width) {
            DisplayMetrics dm = new DisplayMetrics();
            WindowManager wm = (WindowManager) MartinConfig.getContext().getSystemService(Context.WINDOW_SERVICE);
            wm.getDefaultDisplay().getMetrics(dm);

            while (fontSize < dm.widthPixels) {
                fontSize++;
                paint.setTextSize(fontSize);
                realWidth = (int) paint.measureText(content);
                if (realWidth >= width) {
                    break;
                }
            }
        }
        return fontSize;
    }

    public static int getFontSizeOfMaxWidth(String content, int maxWidth, int initFontSize) {
        Paint paint = new Paint();
        int fontSize = initFontSize;
        paint.setTextSize(fontSize);
        int realWidth = (int) paint.measureText(content);
        if (realWidth > maxWidth) {
            while (fontSize > 1) {
                fontSize--;
                paint.setTextSize(fontSize);
                realWidth = (int) paint.measureText(content);
                if (realWidth <= maxWidth) {
                    break;
                }
            }
        }
        return fontSize;
    }

    public static Date getDateHeadOfDate(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    public static Date getDateTailOfDate(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTime();
    }

    public static Date getWeekHeadOfDate(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    public static Date getWeekTailOfDate(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTime();
    }

    public static Date getMonthHeadOfDate(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.DATE, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    public static Date getMonthTailOfDate(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.DATE, 1);
        cal.add(Calendar.MONTH, 1);
        cal.add(Calendar.DATE, -1);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTime();
    }

    public static int calculateDays(Date from, Date to) {
        final long ONE_DAY = 86400000L;
        TimeZone timeZone = TimeZone.getDefault();
        long rawOffset = timeZone.getRawOffset();
        int fromDays = (int) ((from.getTime() + rawOffset) / ONE_DAY);
        int toDays = (int) ((to.getTime() + rawOffset) / ONE_DAY);
        return toDays - fromDays;
    }

    public static String getExtensionOfFile(File file) {
        String name = file.getName();
        int index = name.indexOf(".");
        if (index != -1 && index != name.length() - 1) {
            return name.substring(index);
        } else {
            return "";
        }
    }

    public static String addTrail(String input, int maxLength) {
        if (MiscUtils.isEmpty(input)) {
            return input;
        }
        if (input.length() > maxLength) {
            return input.substring(0, maxLength - 1) + "...";
        } else {
            return input;
        }
    }


    public static void goToSite(Context context, String url) {
        Intent it = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        context.startActivity(it);
    }

    public static String getWeekName(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int week = cal.get(Calendar.DAY_OF_WEEK);
        switch (week) {
            case Calendar.SUNDAY:
                return "星期日";
            case Calendar.MONDAY:
                return "星期一";
            case Calendar.TUESDAY:
                return "星期二";
            case Calendar.WEDNESDAY:
                return "星期三";
            case Calendar.THURSDAY:
                return "星期四";
            case Calendar.FRIDAY:
                return "星期五";
            case Calendar.SATURDAY:
                return "星期六";
            default:
                return "星期八";
        }
    }

    public static String getErrorMessage(Throwable th, String defaultMessage) {
        String msg = th.getMessage();
        if (MiscUtils.isEmpty(msg)) {
            return defaultMessage;
        } else {
            return msg;
        }
    }

    public static void doCallPhone(Context context, String phoneNumber) {
        Uri localUri = Uri.parse("tel:" + phoneNumber);
        Intent call = new Intent(Intent.ACTION_CALL, localUri);
        call.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(call);
    }


    public static String format(Date date, String pattern) {
        if (date == null) {
            return "";
        }
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        return sdf.format(date);
    }

    public static String format(long number, String pattern) {
        DecimalFormat df = new DecimalFormat(pattern);
        return df.format(number);
    }

    public static String format(double number, String pattern) {
        if (Double.isNaN(number) || Double.isInfinite(number)) {
            number = 0.0;
        }
        DecimalFormat df = new DecimalFormat(pattern);
        return df.format(number);
    }

    public static String format(Number number, String pattern) {
        DecimalFormat df = new DecimalFormat(pattern);
        return df.format(number);
    }

    public static String formatDate(Date date) {
        return format(date, "yyyy-MM-dd");
    }

    public static Bitmap getBitmapFromDrawable(Drawable drawable, int width, int height) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        } else {
            if (drawable.getIntrinsicHeight() > 0 && drawable.getIntrinsicWidth() > 0) {
                width = drawable.getIntrinsicWidth();
                height = drawable.getIntrinsicHeight();
            }
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, width, height);
            drawable.draw(canvas);
            return bitmap;
        }
    }

    public static Drawable getCoveredDrawable(Drawable input, Drawable cover) {
        int width = input.getIntrinsicWidth();
        int height = input.getIntrinsicHeight();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas cancas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        // paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
        cancas.drawARGB(0, 0, 0, 0);
        input.setBounds(0, 0, width, height);
        input.draw(cancas);
        Bitmap converBitmap = getBitmapFromDrawable(cover, width, height);
        Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        cancas.drawBitmap(converBitmap, rect, rect, paint);
        return new BitmapDrawable(bitmap);
    }

    public static String safeURLEncode(String s, String encoding) {
        if (s == null) {
            return "";
        }
        try {
            return URLEncoder.encode(s, encoding);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return s;
    }

    public static String safeURLDecode(String s, String encoding) {
        if (s == null) {
            return null;
        }
        try {
            return URLDecoder.decode(s, encoding);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return s;
    }

    public static <T> T safeGetList(List<T> list, int index) {
        if (index >= 0 && index < list.size()) {
            return list.get(index);
        } else {
            return null;
        }
    }

    public static boolean isTheSameDay(Date date1, Date date2) {
        if (date1 == null || date2 == null) {
            return false;
        }
        TimeZone timeZone = TimeZone.getDefault();
        long rawOffset = timeZone.getRawOffset();
        return (date1.getTime() + rawOffset) / 86400000L == (date2.getTime() + rawOffset) / 86400000L;
    }

    public static int parseInt(String s, int defaultInt) {
        if (isEmpty(s)) {
            return defaultInt;
        }
        try {
            return Integer.parseInt(s);
        } catch (Exception ex) {
            ex.printStackTrace();
            return defaultInt;
        }
    }

    public static int parseInt(String s) {
        if (isEmpty(s)) {
            return 0;
        }
        try {
            return Integer.parseInt(s);
        } catch (Exception ex) {
            return 0;
        }
    }

    public static boolean parseBoolean(String s, boolean defaultBool) {
        if (isEmpty(s)) {
            return defaultBool;
        }
        try {
            return Boolean.parseBoolean(s);
        } catch (Exception ex) {
            ex.printStackTrace();
            return defaultBool;
        }
    }

    public static double parseDouble(String s, double defaultDouble) {
        try {
            return Double.parseDouble(s);
        } catch (Exception ex) {
            ex.printStackTrace();
            return defaultDouble;
        }
    }

    @Deprecated
    public static int getPxByDip(int dip) {
        return dip > 0 ? getPxByDipReal(dip) : 0;
    }

    public static int getPxByDipReal(int dip) {
        DisplayMetrics dm = MartinConfig.getContext().getResources().getDisplayMetrics();
        return (int) (dip * dm.density + 0.5f);
    }

    public static Object invokeMethod(Object obj, String methodName, Object[] methodArgs, Class<?>... args) {
        Class<?> cls = obj.getClass();
        try {
            Method method = cls.getDeclaredMethod(methodName, args);
            method.setAccessible(true);
            return method.invoke(obj, methodArgs);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static <T> T getValueOfField(Object obj, String fieldName) {
        Class<?> cls = obj.getClass();
        try {
            Field field = cls.getDeclaredField(fieldName);
            field.setAccessible(true);
            return (T) field.get(obj);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static boolean setValueOfField(Object obj, String fieldName, Object value) {
        Class<?> cls = obj.getClass();
        try {
            Field field = cls.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, value);
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }

    public static Bitmap getRoundedCornerBitmap(Bitmap bitmap) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);
        final float roundPx = MiscUtils.getPxByDipReal(12);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return output;
    }

    public static <T> List<T> copy(List<T> list) {
        List<T> copy = new ArrayList<T>();
        if (MiscUtils.isEmpty(list)) {
            return copy;
        }
        for (T t : list) {
            copy.add(t);
        }
        return copy;
    }

    public static Date parseDate(String input, String pattern) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(pattern);
            sdf.setLenient(false);
            return sdf.parse(input);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return new Date();
    }

    public static int parseColor(String color) {
        if (TextUtils.isEmpty(color)) {
            return 0xFF000000;
        }
        try {
            if (color.startsWith("#")) {
                color = color.substring(1);
            }
            return Integer.parseInt(color, 16) | 0xFF000000;
        } catch (NumberFormatException ex) {
            ex.printStackTrace();
        }
        return 0x000000;
    }

    /**
     * 得到当前手机的IMEI。
     */
    public static String getIMEI() {
        TelephonyManager telephonyManager = (TelephonyManager) MartinConfig.getContext()
                .getSystemService(Context.TELEPHONY_SERVICE);
        String imei = telephonyManager.getDeviceId();
        if (TextUtils.isEmpty(imei)) {
            imei = "";
        }
        return imei;
    }

    /**
     * 得到允许的GPS提供者列表，以逗号分隔。
     * <p/>
     * <p/>
     * 移动网络定位：network(@see LocationManager.NETWORK_PROVIDER)
     * GPS卫星定位： gps(@see LocationManager.GPS_PROVIDER)
     *
     * @return 允许的提供者列表
     */
    public static String getAllowedLocationPrividers(Context context) {
        return Settings.System.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
    }

    public static void assertTrue(boolean b, String error) {
        if (b == false) {
            throw new RuntimeException(error);
        }
    }

    /**
     * 返回当前的网络类型，可能是wifi、g2、g3或者none。
     */
    public static String getNetworkType() {
        String netWorkInfo = "unknown";
        if (MiscUtils.isConnectAvailable()) {
            ConnectivityManager manager = (ConnectivityManager) MartinConfig.getContext()
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = manager.getActiveNetworkInfo();
            if (info != null) {
                if (info.getType() == ConnectivityManager.TYPE_WIFI) {
                    netWorkInfo = "wifi";
                } else if (info.getType() == ConnectivityManager.TYPE_MOBILE) {
                    switch (info.getSubtype()) {
                        case TelephonyManager.NETWORK_TYPE_CDMA:
                        case TelephonyManager.NETWORK_TYPE_EDGE:
                        case TelephonyManager.NETWORK_TYPE_GPRS:
                        case TelephonyManager.NETWORK_TYPE_1xRTT:
                        case TelephonyManager.NETWORK_TYPE_UNKNOWN: {
                            netWorkInfo = "g2";
                            break;
                        }
                        case TelephonyManager.NETWORK_TYPE_EVDO_0:
                        case TelephonyManager.NETWORK_TYPE_EVDO_A:
                        case TelephonyManager.NETWORK_TYPE_EVDO_B:
                        case TelephonyManager.NETWORK_TYPE_HSDPA:
                        case TelephonyManager.NETWORK_TYPE_HSPA:
                        case TelephonyManager.NETWORK_TYPE_HSUPA:
                        case TelephonyManager.NETWORK_TYPE_IDEN:
                        case TelephonyManager.NETWORK_TYPE_UMTS: {
                            netWorkInfo = "g3";
                            break;
                        }
                        case TelephonyManager.NETWORK_TYPE_LTE: {
                            netWorkInfo = "g4";
                            break;
                        }
                        default: {
                            netWorkInfo = "g2";
                        }
                    }
                }
            }
        }
        return netWorkInfo;
    }

    public static boolean isConnectAvailable() {
        ConnectivityManager cm = (ConnectivityManager) MartinConfig.getContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return false;
        }
        NetworkInfo info = cm.getActiveNetworkInfo();
        return info != null && info.isAvailable();
    }

    public static boolean isEquals(Object obj1, Object obj2) {
        if (obj1 != null) {
            return obj1.equals(obj2);
        } else if (obj2 != null) {
            return obj2.equals(obj1);
        } else {
            return true;
        }
    }

    public static boolean isNotEquals(Object obj1, Object obj2) {
        return !isEquals(obj1, obj2);
    }

    public static boolean isNotEmpty(String s) {
        return !isEmpty(s);
    }

    public static boolean isNotEmpty(Collection<?> c) {
        return !isEmpty(c);
    }

    public static boolean isNotEmpty(Map<?, ?> map) {
        return !isEmpty(map);
    }

    public static boolean isEmpty(String s) {
        return s == null || s.trim().length() == 0;
    }

    public static boolean isEmptyOrLiterallyNull(String s) {
        return MiscUtils.isEmpty(s) || "null".equals(s);
    }

    public static boolean isEmpty(Collection<?> c) {
        return c == null || c.isEmpty();
    }

    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    /**
     * 程序初始化所有WebView。
     */
    public static void initWebView(Context context) {
        WebView webView = new WebView(context);
        webView.setVerticalScrollbarOverlay(true);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setAppCacheEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setDatabaseEnabled(true);
        webView.getSettings()
                .setDatabasePath(MartinConfig.getContext().getDir("database", Context.MODE_PRIVATE).getPath());
        webView.getSettings()
                .setAppCachePath(MartinConfig.getContext().getDir("cache", Context.MODE_PRIVATE).getPath());
    }

    @SuppressLint("SetJavaScriptEnabled")
    public static void enableHTML5(final WebView webView, boolean netFirst) {
        webView.setVerticalScrollbarOverlay(true);
        int prid = MiscUtils.getResourcesIdentifier(MartinConfig.getContext(), "string/product");
        String pr = MartinConfig.getContext().getResources().getString(prid);
        String appVersion = InfoUtils.getVersionName();
        String userAgent = webView.getSettings().getUserAgentString();
        userAgent += " MuCang(" + pr + ";" + appVersion + ";" + "Android)";
        webView.getSettings().setGeolocationEnabled(true);
        webView.getSettings().setUserAgentString(userAgent);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setAppCacheEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setDatabaseEnabled(true);
        webView.getSettings()
                .setDatabasePath(MartinConfig.getContext().getDir("database", Context.MODE_PRIVATE).getPath());
        webView.getSettings()
                .setAppCachePath(MartinConfig.getContext().getDir("cache", Context.MODE_PRIVATE).getPath());
        webView.getSettings().setSupportZoom(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        if (Build.VERSION.SDK_INT >= 16) {
            webView.getSettings().setDisplayZoomControls(false);
        }
        try {
            webView.getSettings().setDomStorageEnabled(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        webView.getSettings().setAppCacheMaxSize(1024 * 1024 * 50);
        webView.getSettings().setAllowFileAccess(true);
        if (netFirst) {
            webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        } else {
            webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        }
        webView.setDownloadListener(new DownloadListener() {

            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype,
                    long contentLength) {
                MiscUtils.goToSite(webView.getContext(), url);
            }
        });

        // webview同时要实现如下方法
        // @Override
        // public void onExceededDatabaseQuota(String url, String databaseIdentifier, long quota, long
        // estimatedDatabaseSize, long totalQuota, QuotaUpdater quotaUpdater) {
        // quotaUpdater.updateQuota(estimatedDatabaseSize * 2);
        // }
    }

    /**
     * 返回两个时间点的具体描述。
     *
     * @param min 发生的时间
     * @param max 当前的时间
     */
    public static String getBetweenTime(long min, long max) {
        int seconds = (int) ((max - min) / 1000L);
        if (seconds < 30) {
            return "刚刚";
        } else if (seconds < 60) {
            return "30秒前";
        } else if (seconds < 3600) {
            return (seconds / 60) + "分钟前";
        } else {
            // 如果是同一天，则显示时分，否则就显示某一天
            Calendar minCal = Calendar.getInstance();
            Calendar maxCal = Calendar.getInstance();
            minCal.setTimeInMillis(min);
            maxCal.setTimeInMillis(max);
            Date minDate = minCal.getTime();
            // 如果同年月日，则显示时间和分钟就可以了
            if (minCal.get(Calendar.YEAR) == maxCal.get(Calendar.YEAR) && minCal.get(Calendar.MONTH) == maxCal
                    .get(Calendar.MONTH) && minCal.get(Calendar.DATE) == maxCal.get(Calendar.DATE)) {
                return format(minDate, "HH:mm");
            } else if (minCal.get(Calendar.YEAR) == maxCal.get(Calendar.YEAR)) { // 如果同年，则显示日期和时间
                return format(minDate, "MM月dd日");
            } else {
                return format(minDate, "yyyy年MM月dd日");
            }
        }
    }

    /**
     * 当context不是Activity的时候，不能直接启动Activity，需要加上一个FLAG_ACTIVITY_NEW_TASK的flag
     */
    private static void trimIntentWhenStartingNotWithActivity(Context context, Intent intent) {
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
    }

    public static int getStateBarHeight() {
        return Resources.getSystem()
                .getDimensionPixelSize(Resources.getSystem().getIdentifier("status_bar_height", "dimen", "android"));
    }

    public static int compareVersion(String ver1, String ver2) {
        if (isEmpty(ver1) && isNotEmpty(ver2)) {
            return -1;
        } else if (isNotEmpty(ver1) && isEmpty(ver2)) {
            return 1;
        }

        List<String> ver1ItemList = new ArrayList<>(Arrays.asList(ver1.split("\\.")));
        List<String> ver2ItemList = new ArrayList<>(Arrays.asList(ver2.split("\\.")));

        int sizeGap = Math.abs(ver1ItemList.size() - ver2ItemList.size());

        int maxSize = ver1ItemList.size() > ver2ItemList.size() ? ver1ItemList.size() : ver2ItemList.size();

        for (int i = 0; i < sizeGap; i++) {
            if (ver1ItemList.size() < ver2ItemList.size()) {
                ver1ItemList.add("0");
            } else if (ver1ItemList.size() > ver2ItemList.size()) {
                ver2ItemList.add("0");
            }
        }

        for (int i = 0; i < maxSize; i++) {
            try {
                int v1 = Integer.parseInt(ver1ItemList.get(i));
                int v2 = Integer.parseInt(ver2ItemList.get(i));
                if (v1 > v2) {
                    return 1;
                } else if (v1 < v2) {
                    return -1;
                }
            } catch (NumberFormatException e) {
                LogUtils.w("jin", null, e);
            }
        }

        return 0;
    }

    public static void openOrFinish(Activity activity) {
        if (activity == null) {
            return;
        }
        Intent launcherIntent = activity.getApplicationContext().getPackageManager()
                .getLaunchIntentForPackage(activity.getPackageName());
        if (launcherIntent != null) {
            openOrFinish(activity, launcherIntent);
        } else {
            activity.finish();
        }
    }

    public static void openOrFinish(Activity activity, Intent intent) {
        if (activity == null) {
            return;
        }
        ActivityManager activityManager = (ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> infoList = activityManager.getRunningTasks(1);
        ActivityManager.RunningTaskInfo info = infoList.get(0);
        if (info != null && info.numActivities == 1 && intent != null) {
            activity.startActivity(intent);
        }
        activity.finish();
    }

    /**
     * createWebView
     */
    public static WebView a() {
        LogUtils.i("miscutils", "miscUtils.a");
        final WebView webView = new WebView(MartinConfig.getContext());
        enableHTML5(webView, true);
        webView.setWebViewClient(new WebViewClient());
        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype,
                    long contentLength) {
                //如果是下载的话就什么都不做
            }
        });
        return webView;
    }

    /**
     * loadUrl
     */
    public static void b(final WebView webView, final String url) {
        LogUtils.i("miscutils", "miscUtils.b");
        if (webView != null && MiscUtils.isNotEmpty(url)) {
            MartinConfig.postOnUiThread(new Runnable() {
                @Override
                public void run() {
                    webView.loadUrl(url);
                }
            });
        }
    }

    /**
     * destroyWebView
     */
    public static void c(final WebView webView) {
        LogUtils.i("miscutils", "miscUtils.c");
        if (webView != null) {
            MartinConfig.postOnUiThread(new Runnable() {
                @Override
                public void run() {
                    webView.destroy();
                }
            });
        }
    }

    public static String getMIUIVersion() {
        String line = null;
        BufferedReader input = null;
        try {
            Process p = Runtime.getRuntime().exec("getprop ro.miui.ui.version.name");
            input = new BufferedReader(new InputStreamReader(p.getInputStream()), 1024);
            line = input.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            DataUtils.close(input);
        }
        return line;
    }

    public static void setMIUIStatusBarDarkMode(boolean darkMode, Activity activity) {
        Class<? extends Window> clazz = activity.getWindow().getClass();
        try {
            int darkModeFlag = 0;
            Class<?> layoutParams = Class.forName("android.view.MiuiWindowManager$LayoutParams");
            Field field = layoutParams.getField("EXTRA_FLAG_STATUS_BAR_DARK_MODE");
            darkModeFlag = field.getInt(layoutParams);
            Method extraFlagField = clazz.getMethod("setExtraFlags", int.class, int.class);
            extraFlagField.invoke(activity.getWindow(), darkMode ? darkModeFlag : 0, darkModeFlag);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

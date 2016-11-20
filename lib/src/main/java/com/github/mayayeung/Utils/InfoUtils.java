package com.github.mayayeung.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.telephony.TelephonyManager;

import com.github.mayayeung.config.InternalConfig;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Created by Martin on 2016/2/26.
 */
public class InfoUtils {
    private static Bundle applicationInfoMetadata;

    public static String getQudao() {
        return String.valueOf(getApplicationInfoMetadata().getString("qudao"));
    }

    public static String getRenyuan() {
        return String.valueOf(getApplicationInfoMetadata().getString("renyuan"));
    }

    public static String getAppName() {
        try {
            ApplicationInfo info = InternalConfig.getContext().getPackageManager()
                    .getApplicationInfo(InternalConfig.getContext().getPackageName(), PackageManager.GET_META_DATA);
            CharSequence name = info.loadLabel(InternalConfig.getContext().getPackageManager());
            return String.valueOf(name);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "【卡卡移动】";
    }

    public static String getVersionName() {
        // 先取保存的版本号,如果取不到，则取系统的版本号
        String versionName = "1.0.0";
        try {
            versionName = InternalConfig.getContext().getPackageManager()
                    .getPackageInfo(InternalConfig.getContext().getPackageName(), 0).versionName;
        } catch (Exception e) {
            e.printStackTrace();
            versionName = "1.0.0";
        }
        return versionName;
    }

    public static String getSystem() {
        return Build.ID;
    }

    public static String getDeviceName() {
        return Build.MODEL;
    }

    public static String getNetworkName() {
        TelephonyManager phone = (TelephonyManager) InternalConfig.getContext()
                .getSystemService(Context.TELEPHONY_SERVICE);
        String key = phone.getNetworkOperatorName();
        if (key != null) {
            key = key.toUpperCase(Locale.ENGLISH);
        } else {
            return "UNKOWN";
        }
        Map<String, String> map = new HashMap<String, String>();
        map.put("CHINA MOBILE", "M");
        map.put("中国移动", "M");
        map.put("CMCC", "M");
        map.put("CHINA UNICOM", "C");
        map.put("中国联通", "C");
        map.put("CHINA TELECOM", "T");
        map.put("中国电信", "T");
        String net = map.get(key);
        if (net == null) {
            String imsi = phone.getSubscriberId();
            if (imsi != null) {
                if (imsi.startsWith("46000") || imsi.startsWith("46002")) {
                    net = "M";
                } else if (imsi.startsWith("46001")) {
                    net = "C";
                } else if (imsi.startsWith("46003")) {
                    net = "T";
                }
            }
            if (net == null) {
                net = key;
            }
        }
        return net;

    }

    private static Bundle getApplicationInfoMetadata() {
        if (applicationInfoMetadata == null) {
            try {
                ApplicationInfo info = InternalConfig.getContext().getPackageManager()
                        .getApplicationInfo(InternalConfig.getContext().getPackageName(), PackageManager.GET_META_DATA);
                applicationInfoMetadata = info.metaData;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            if (applicationInfoMetadata == null) {
                applicationInfoMetadata = new Bundle();
            }
        }
        return applicationInfoMetadata;
    }
}

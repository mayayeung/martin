package com.github.mayayeung.config;

import android.app.Activity;

/**
 * 应用离开很长时间再回来的监听器
 * Created by Martin on 2016/2/26.
 */
public interface ActivityLeavedLongListener {

    void activityBackNow(Activity activity);
}

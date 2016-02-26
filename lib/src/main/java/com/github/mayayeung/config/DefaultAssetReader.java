package com.github.mayayeung.config;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;

import com.github.mayayeung.utils.DataUtils;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Martin on 2016/2/26.
 */
public class DefaultAssetReader implements AssetReader {
    private Map<String, WeakReference<Drawable>> imageCaches = new HashMap<>();
    private Context context;

    public DefaultAssetReader(Context context) {
        this.context = context;
    }

    @Override
    public Drawable getDrawableOfAsserts(String fullPath) {
        return loadAndCacheIfNeed(fullPath, imageCaches);
    }

    private Drawable loadAndCacheIfNeed(String filePath, Map<String, WeakReference<Drawable>> map) {
        Drawable dr = getCachedDrawable(map, filePath);
        if (dr != null) {
            return dr;
        }
        InputStream is = null;
        try {
            DisplayMetrics dm = DataUtils.getCurrentDisplayMetrics();
            is = context.getAssets().open(filePath);
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inDensity = 160;
            opts.inTargetDensity = dm.densityDpi;
            opts.inScreenDensity = dm.densityDpi;
            Bitmap bitmap = BitmapFactory.decodeStream(is, null, opts);
            if (bitmap == null || bitmap.getWidth() <= 0 || bitmap.getHeight() <= 0) {
                return null;
            }
            int usedWidth = (dm.widthPixels - 40);
            if (bitmap.getWidth() > usedWidth) {
                Matrix matrix = new Matrix();
                float scale = 1.f * usedWidth / bitmap.getWidth();
                matrix.postScale(scale, scale);
                Bitmap newB = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                bitmap = newB;
            }
            dr = new BitmapDrawable(bitmap);
            map.put(filePath, new WeakReference<Drawable>(dr));
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            DataUtils.close(is);
        }
        return dr;
    }

    private Drawable getCachedDrawable(Map<String, WeakReference<Drawable>> map, String key) {
        WeakReference<Drawable> wr = map.get(key);
        if (wr != null) {
            Drawable entry = wr.get();
            if (entry != null) {
                return entry;
            } else {
                map.remove(key);
            }
        }
        return null;
    }
}

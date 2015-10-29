package org.hao.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.Log;

import com.android.volley.toolbox.ImageLoader.ImageCache;

/**
 * Created by Cong Hao on 2015/5/14.
 * Email: hao.cong@qq.com
 */

public class BitmapCache implements ImageCache {
    private Context context;
    private static final String TAG = BitmapCache.class.getName();

    public BitmapCache(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public Bitmap getBitmap(String url) {
        Log.d(TAG, "get cache " + url);
        return MyMemoryCache.getInstance(context).get(url);
    }

    @Override
    public void putBitmap(String url, Bitmap bitmap) {
        Log.d(TAG, "add cache " + url);
        if (!TextUtils.isEmpty(url) && bitmap != null && !bitmap.isRecycled()) {
            MyMemoryCache.getInstance(context).put(url, bitmap);
        }
    }
}

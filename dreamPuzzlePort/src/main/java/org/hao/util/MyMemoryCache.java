package org.hao.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.DisplayMetrics;
import android.util.LruCache;


/**
 * Created by Cong Hao on 2015/6/8.
 * Email: hao.cong@qq.com
 */
public class MyMemoryCache {
	private volatile static MyMemoryCache mInstance;
	private static Context mContext;
	private LruCache<String, Bitmap> mLruCache;
	private static final int SCREENS_OF_MEMORY_CACHE = 4;

	private MyMemoryCache(Context context) {
		this.mContext = context;
		this.mLruCache = new LruCache<String, Bitmap>(getCacheSize(SCREENS_OF_MEMORY_CACHE)) {
			@Override
			protected int sizeOf(String key, Bitmap bitmap) {
				return bitmap.getByteCount();
			}
		};
	}
	public static MyMemoryCache getInstance(Context context) {
		if (mInstance == null) {
			synchronized(MyMemoryCache.class) {
				if (mInstance == null) {
					mInstance = new MyMemoryCache(context);
				}
			}
		}
		return mInstance;
	}
	private int getCacheSize(int intScreens) {
		final int cacheSizeOfMaxMemory = (int)(Runtime.getRuntime().maxMemory() / 8);
		if(intScreens <= 0 ){
			return cacheSizeOfMaxMemory;
		} else {
			final DisplayMetrics displayMetrics = this.mContext.getResources().getDisplayMetrics();
			final int screenWidth = displayMetrics.widthPixels;
			final int screenHeight = displayMetrics.heightPixels;
			// 4 bytes per pixel
			final int screenBytes = screenWidth * screenHeight * 4;
			final int cacheSizeOfScreens = screenBytes * intScreens;

			//获取系统分配给每个应用程序的最大内存，每个应用系统分配32M
//        final int memClass = ((ActivityManager)this.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass();
//        final int cacheSize = 1024 * 1024 * memClass / 8;
			return cacheSizeOfScreens < cacheSizeOfMaxMemory ? cacheSizeOfScreens : cacheSizeOfMaxMemory;
		}
	}
	public Bitmap get(String key) {
		return this.mLruCache.get(key);
	}
	public final Bitmap put(String key, Bitmap bitmap) {
		return this.mLruCache.put(key, bitmap);
	}
	public void evictAll() {
		this.mLruCache.evictAll();
	}
}

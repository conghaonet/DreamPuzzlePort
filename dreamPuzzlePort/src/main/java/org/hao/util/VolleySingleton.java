package org.hao.util;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;

/**
 * Created by Cong Hao on 2015/5/14.
 * Email: hao.cong@qq.com
 */
public class VolleySingleton {
	private volatile static VolleySingleton mInstance;
	private RequestQueue mRequestQueue;
	private ImageLoader mImageLoader;
	private static Context mContext;

	private VolleySingleton(Context context) {
		mContext = context;
		mRequestQueue = getRequestQueue();
		mImageLoader = new ImageLoader(mRequestQueue, new BitmapCache(mContext));
	}

	public static VolleySingleton getInstance(Context context) {
		if (mInstance == null) {
			synchronized (VolleySingleton.class) {
				if (mInstance == null) {
					mInstance = new VolleySingleton(context);
				}
			}
		}
		return mInstance;
	}

	public RequestQueue getRequestQueue() {
		if (mRequestQueue == null) {
			// getApplicationContext() is key, it keeps you from leaking the
			// Activity or BroadcastReceiver if someone passes one in.
			mRequestQueue = Volley.newRequestQueue(mContext.getApplicationContext());
		}
		return mRequestQueue;
	}

	public <T> void addToRequestQueue(Request<T> req) {
		getRequestQueue().add(req);
	}

	public ImageLoader getImageLoader() {
		return mImageLoader;
	}
}
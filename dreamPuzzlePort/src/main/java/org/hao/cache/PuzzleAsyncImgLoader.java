package org.hao.cache;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.Handler;

import org.hao.database.DBHelperCustom;
import org.hao.puzzle54.AppConstants;
import org.hao.puzzle54.AppPrefUtil;
import org.hao.puzzle54.MyApp;
import org.hao.puzzle54.custom.CustomConstants;
import org.hao.puzzle54.custom.CustomPicEntity;

import java.io.BufferedInputStream;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class PuzzleAsyncImgLoader {
	private static final String TAG = PuzzleAsyncImgLoader.class.getName();
	private ExecutorService executorService = Executors.newFixedThreadPool(3); // 固定 3 个线程来执行任务
	private final Handler handler = new Handler();
	private DiskLruCache mDiskLruCache;
//	private Object mDiskCacheLock;
//	private Boolean mDiskCacheStarting;
	private MyApp myApp;
	private boolean isCustom;
	private String packageCode;
	private boolean isInnerPics;
	private AssetManager mAssetManager;

	public PuzzleAsyncImgLoader(String packageCode, Context context, DiskLruCache mDiskLruCache) {
		this.packageCode = packageCode;
		if(AppConstants.CUSTOM_PACKAGE_CODE.equals(this.packageCode)) this.isCustom = true;
		this.myApp = (MyApp)context;
		this.isInnerPics = this.myApp.isInnerPics(this.packageCode);
		if(this.isInnerPics) this.mAssetManager = this.myApp.getAssets();
		this.mDiskLruCache = mDiskLruCache;
//		this.mDiskCacheLock = mDiskCacheLock;
//		this.mDiskCacheStarting = mDiskCacheStarting;
	}
	private String getFullPictureCacheKey(long currentPicIndex) {
		String picName=""+currentPicIndex;
		long lastModified = 0;
		if(this.isCustom) {
			CustomPicEntity currentCustomEntity = DBHelperCustom.getInstance(myApp).getById(currentPicIndex, null);
			picName = currentCustomEntity.getImageName().replaceAll(CustomConstants.EXTENSION_CUSTOM_PICTURE, "");
			lastModified = currentCustomEntity.getImportDateTime().getTime();
		} else if(!this.isInnerPics) {
			lastModified = myApp.getLastModifiedOfZipLastUsedPackage();
		}
		boolean isFullScreenMode = AppPrefUtil.isFullScreenMode(myApp, null);
		String rowXcol = AppPrefUtil.getRows(myApp, null)+"x"+AppPrefUtil.getCols(myApp, null);
		String viableWidthHeight = this.myApp.getAvailablePuzzleWidth()+"x"+this.myApp.getAvailablePuzzleHeight();
		String cacheKey = "v"+myApp.getVersionCode(null)+"_pieces_full_"+String.valueOf(isFullScreenMode)
				+"_"+this.packageCode+"_"+picName+"_"+rowXcol+"_"+viableWidthHeight+"_"+lastModified;
		return cacheKey;
	}
	private String getSubPictureCacheKey(long currentPicIndex, int rowIndex, int colIndex) {
		return getFullPictureCacheKey(currentPicIndex)+"_"+rowIndex+"_"+colIndex;
	}
	public void removeFullPictureFromMemeoryCache(long currentPicIndex) {
		this.myApp.getMemCache().remove(getFullPictureCacheKey(currentPicIndex));
	}
	public Bitmap loadFullBitmap(final long currentPicIndex, final ImageCallback callback) {
		final String cacheKey = getFullPictureCacheKey(currentPicIndex);
		Bitmap cacheBitmap = myApp.getMemCache().get(cacheKey);
		if(cacheBitmap != null && !cacheBitmap.isRecycled()) return cacheBitmap;

		// if bitmap is not in memory cache, get bitmap from sdcard
		executorService.submit(new Runnable() {
			public void run() {
				try {
					Bitmap tempBitmap = getFromDiskCache(cacheKey);
					if(tempBitmap == null) {
						tempBitmap = loadFullBitmapFromSdcard(currentPicIndex);
					}
					final Bitmap bitmap = tempBitmap;
					if(bitmap !=null) {
						addToCache(cacheKey, bitmap);
					}
					handler.post(new Runnable() {
						public void run() {
							callback.imageLoaded(bitmap);
						}
					});
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		});
		return null;
	}
	public Bitmap loadSubBitmap(final long currentPicIndex, final int rowIndex, final int colIndex, final ImageCallback callback) {
		final String subKey = getSubPictureCacheKey(currentPicIndex, rowIndex, colIndex);
		Bitmap cacheSubBitmap = myApp.getMemCache().get(subKey);
		if(cacheSubBitmap !=null && !cacheSubBitmap.isRecycled()) return cacheSubBitmap;
		executorService.submit(new Runnable() {
			public void run() {
				try {
					Bitmap tempBitmap = getFromDiskCache(subKey);
					if(tempBitmap == null) {
						tempBitmap = loadSubBitmapFromSdcard(currentPicIndex, rowIndex, colIndex);
					}
					final Bitmap bitmap = tempBitmap;
					if(bitmap !=null) {
						addToCache(subKey, bitmap);
					}
					handler.post(new Runnable() {
						public void run() {
							callback.imageLoaded(bitmap);
						}
					});
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		});
		return null;
	}
	public void clearCurrentDiskCacheFiles() {
		new Thread() {
			public void run() {
				PuzzleAsyncImgLoader.this.mDiskLruCache.clearCache();
			}
		}.start();
	}
	protected Bitmap loadSubBitmapFromSdcard(long currentPicIndex, int rowIndex, int colIndex) {
		Bitmap subBitmap = null;
		try{
			String fullKey = getFullPictureCacheKey(currentPicIndex);
			Bitmap fullBitmap = this.myApp.getMemCache().get(fullKey);
			if(fullBitmap == null || fullBitmap.isRecycled()) {
				fullBitmap = getFromDiskCache(fullKey);
				if(fullBitmap != null) addToCache(fullKey, fullBitmap);
			}
			if(fullBitmap == null || fullBitmap.isRecycled()) {
				fullBitmap = loadFullBitmapFromSdcard(currentPicIndex);
				if(fullBitmap != null) addToCache(fullKey, fullBitmap);
			}
			if(fullBitmap != null && !fullBitmap.isRecycled()) {
				int pieceWidth = fullBitmap.getWidth() / AppPrefUtil.getCols(myApp, null);
				int pieceHeight = fullBitmap.getHeight() / AppPrefUtil.getRows(myApp, null);
				subBitmap = Bitmap.createBitmap(fullBitmap, colIndex*pieceWidth, rowIndex*pieceHeight, pieceWidth, pieceHeight);
				if(subBitmap != null) return subBitmap;
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return null;
	}
	// get data from sdcard
	protected Bitmap loadFullBitmapFromSdcard(long currentPicIndex) {
		try {
			Bitmap tempBitmap = null;
			Bitmap finalBitmap = null;
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = false;
	        options.inPreferredConfig = Bitmap.Config.RGB_565;
			if(isCustom) {
				CustomPicEntity currentCustomEntity = DBHelperCustom.getInstance(myApp).getById(currentPicIndex, null);
				tempBitmap = BitmapFactory.decodeFile(this.myApp.getCustomPicture(currentCustomEntity.getImageName()).getPath(), options);
			} else {
				BufferedInputStream input = null;
				try {
					if(this.isInnerPics) {
						input = new BufferedInputStream(mAssetManager.open(AppConstants.PICS_FOLDER_IN_ZIP+ File.separator+this.myApp.getInnerPics().get((int)currentPicIndex)));
					} else {
						ZipFile zip = this.myApp.getPackageZipFile(this.packageCode);
						ZipEntry zipEntry = this.myApp.getCurrentZipEntries(this.packageCode).get((int)currentPicIndex);
						input = new BufferedInputStream(zip.getInputStream(zipEntry));
					}
					tempBitmap = BitmapFactory.decodeStream(input, null, options);
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					if(input != null) input.close();
				}
			}
			boolean blnNoCut = false;
			float srcPicWidthHeightRatio = (float)options.outWidth / (float)options.outHeight;
			if(srcPicWidthHeightRatio >= 0.9 && srcPicWidthHeightRatio <= 1.1) blnNoCut = true;

			final float ratioWidthHeight = myApp.getPuzzleRatioWidthHeight();
			int destBitmapHeight;
			int destBitmapWidth;
			if(AppPrefUtil.isFullScreenMode(myApp, null) && !blnNoCut) {
			    destBitmapWidth = this.myApp.getAvailablePuzzleWidth();
			    destBitmapHeight = this.myApp.getAvailablePuzzleHeight();
			} else {
				final float ratioSrcBitmap = (float)tempBitmap.getWidth()/(float)tempBitmap.getHeight();
				if(ratioSrcBitmap > ratioWidthHeight) {
					destBitmapWidth = this.myApp.getAvailablePuzzleWidth();
					destBitmapHeight = (int)((float)destBitmapWidth / ratioSrcBitmap);
				} else {
					destBitmapHeight = this.myApp.getAvailablePuzzleHeight();
					destBitmapWidth = (int)(ratioSrcBitmap * destBitmapHeight);
				}
			}
			destBitmapWidth = destBitmapWidth / AppPrefUtil.getCols(myApp, null) * AppPrefUtil.getCols(myApp, null);
			destBitmapHeight = destBitmapHeight / AppPrefUtil.getRows(myApp, null) * AppPrefUtil.getRows(myApp, null);
			if(destBitmapWidth != tempBitmap.getWidth() || destBitmapHeight != tempBitmap.getHeight()) {
				finalBitmap = ThumbnailUtils.extractThumbnail(tempBitmap, destBitmapWidth, destBitmapHeight, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
				return finalBitmap;
			} else {
				return tempBitmap;
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	private void addToCache(String key, Bitmap bitmap) {
	    // Add to memory cache as before
		if(myApp.getMemCache() != null && myApp.getMemCache().get(key) == null) myApp.getMemCache().put(key, bitmap);
	    // Also add to disk cache
		if (mDiskLruCache != null && mDiskLruCache.get(key) == null) {
			if(bitmap !=null) mDiskLruCache.put(key, bitmap);
		}
//		synchronized (mDiskCacheLock) {
//			if (mDiskLruCache != null && mDiskLruCache.get(key) == null) {
//				if(bitmap !=null) mDiskLruCache.put(key, bitmap);
//			}
//		}
	}
	private Bitmap getFromDiskCache(String key) {
        if (mDiskLruCache != null) {
        	return mDiskLruCache.get(key);
        }
//		synchronized (mDiskCacheLock) {
//			// Wait while disk cache is started from background thread
//			while (mDiskCacheStarting) {
//				try {
//					mDiskCacheLock.wait();
//				} catch (InterruptedException e) {}
//			}
//	        if (mDiskLruCache != null) {
//	        	return mDiskLruCache.get(key);
//	        }
//		}
	    return null;
	}
	// Callback interface is open to the outside world
	public interface ImageCallback {
		// Note that this method is used to set the target object image resources
		public void imageLoaded(Bitmap imageBitmap);
	}
}

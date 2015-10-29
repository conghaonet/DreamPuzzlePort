package org.hao.cache;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A simple disk LRU bitmap cache to illustrate how a disk cache would be used for bitmap caching. A
 * much more robust and efficient disk LRU cache solution can be found in the ICS source code
 * (libcore/luni/src/main/java/libcore/io/DiskLruCache.java) and is preferable to this simple
 * implementation.
 */
public class DiskLruCache {
	private static final String TAG = DiskLruCache.class.getName();
	private static final boolean DEBUG = false;
	
	private static final String CACHE_FILENAME_PREFIX = "cache_";
	private static final int MAX_REMOVALS = 4;
	private static final int INITIAL_CAPACITY = 32;
	private static final float LOAD_FACTOR = 0.75f;

	private final File mCacheDir;
	private int cacheSize = 0;
	private int cacheByteSize = 0;
    private long maxCacheByteSize = 1024 * 1024 * 16; // 16MB default
	private CompressFormat mCompressFormat = CompressFormat.JPEG;
	private int mCompressQuality = 80;

	private final Map<String, String> mLinkedHashMap =
			Collections.synchronizedMap(new LinkedHashMap<String, String>(
					INITIAL_CAPACITY, LOAD_FACTOR, true));

	/**
	 * A filename filter to use to identify the cache filenames which have CACHE_FILENAME_PREFIX
	 * prepended.
	 */
	private static final FilenameFilter cacheFileFilter = new FilenameFilter() {
		@Override
		public boolean accept(File dir, String filename) {
			return filename.startsWith(CACHE_FILENAME_PREFIX);
		}
	};

	/**
	 * Used to fetch an instance of DiskLruCache.
	 *
	 * @param cacheDir
	 * @param maxByteSize
	 * @return
	 */
	public static DiskLruCache openCache(File cacheDir, long maxByteSize) {
		if (!cacheDir.exists()) {
			if(!cacheDir.mkdirs()) {
				Log.e(TAG, "ERROR: Cannot create dir "+cacheDir.toString()+"!!!");
			}
		}

		if (cacheDir.isDirectory() && cacheDir.canWrite()
				&& getUsableSpace(cacheDir) > maxByteSize) {
			Log.i(TAG, "cacheDir :" + cacheDir.toString());
			return new DiskLruCache(cacheDir, maxByteSize);
		}

		return null;
	}

	/**
	 * Constructor that should not be called directly, instead use
	 *  which runs some extra checks before
	 * creating a DiskLruCache instance.
	 *
	 * @param cacheDir
	 * @param maxByteSize
	 */
	private DiskLruCache(File cacheDir, long maxByteSize) {
		mCacheDir = cacheDir;
		maxCacheByteSize = maxByteSize;
	}

	/**
	 * Add a bitmap to the disk cache.
	 *
	 * @param key A unique identifier for the bitmap.
	 * @param data The bitmap to store.
	 */
	public void put(String key, Bitmap data) {
		synchronized (mLinkedHashMap) {
			if (mLinkedHashMap.get(key) == null) {
				try {
					final String file = createFilePath(mCacheDir, key);
					if (writeBitmapToFile(data, file)) {
						put(key, file);
						if (DEBUG) {
							Log.d(TAG, "put - Added cache file, " + file);
						}
						
						flushCache();
					}
				} catch (final FileNotFoundException e) {
					Log.e(TAG, "Error in put: " + e.getMessage());
				} catch (final IOException e) {
					Log.e(TAG, "Error in put: " + e.getMessage());
				}
			}
		}
	}

	private void put(String key, String file) {
		mLinkedHashMap.put(key, file);
		cacheSize = mLinkedHashMap.size();
		cacheByteSize += new File(file).length();
	}

	/**
	 * Flush the cache, removing oldest entries if the total size is over the specified cache size.
	 * Note that this isn't keeping track of stale files in the cache directory that aren't in the
	 * HashMap. If the images and keys in the disk cache change often then they probably won't ever
	 * be removed.
	 */
	private void flushCache() {
		Entry<String, String> eldestEntry;
		File eldestFile;
		long eldestFileSize;
		int count = 0;

        int maxCacheItemSize = 128;
        while (count < MAX_REMOVALS && (cacheSize > maxCacheItemSize || cacheByteSize > maxCacheByteSize)) {
			eldestEntry = mLinkedHashMap.entrySet().iterator().next();
			eldestFile = new File(eldestEntry.getValue());
			eldestFileSize = eldestFile.length();
			mLinkedHashMap.remove(eldestEntry.getKey());
			eldestFile.delete();
			cacheSize = mLinkedHashMap.size();
			cacheByteSize -= eldestFileSize;
			count++;
			if (DEBUG) {
				Log.d(TAG, "flushCache - Removed cache file, " + eldestFile + ", "
						+ eldestFileSize);
			}
		}
	}

	/**
	 * Get an image from the disk cache.
	 *
	 * @param key The unique identifier for the bitmap
	 * @return The bitmap or null if not found
	 */
	public Bitmap get(String key) {
		synchronized (mLinkedHashMap) {
			try {
				final String file = mLinkedHashMap.get(key);
				if (file != null) {
					if (DEBUG) {
						Log.d(TAG, "Disk cache hit");
					}
					return BitmapFactory.decodeFile(file);
				} else {
					final String existingFile = createFilePath(mCacheDir, key);
					if (new File(existingFile).exists()) {
						put(key, existingFile);
						if (DEBUG) {
							Log.d(TAG, "Disk cache hit (existing file)");
						}
						return BitmapFactory.decodeFile(existingFile);
					}
				}
			} catch(Exception e) {
                e.printStackTrace();
            }
			return null;
		}
	}

	/**
	 * Checks if a specific key exist in the cache.
	 *
	 * @param key The unique identifier for the bitmap
	 * @return true if found, false otherwise
	 */
	public boolean containsKey(String key) {
		// See if the key is in our HashMap
		if (mLinkedHashMap.containsKey(key)) {
			return true;
		}

		// Now check if there's an actual file that exists based on the key
		final String existingFile = createFilePath(mCacheDir, key);
		if (new File(existingFile).exists()) {
			// File found, add it to the HashMap for future use
			put(key, existingFile);
			return true;
		}
		return false;
	}

	/**
	 * Removes all disk cache entries from this instance cache dir
	 */
	public void clearCache() {
		DiskLruCache.clearCache(mCacheDir);
	}

	/**
	 * Removes all disk cache entries from the application cache directory in the uniqueName
	 * sub-directory.
	 *
	 * @param context The context to use
	 * @param uniqueName A unique cache directory name to append to the app cache directory
	 */
	public static void clearCache(Context context, String uniqueName) {
		File cacheDir = getDiskCacheDir(context, uniqueName);
		clearCache(cacheDir);
	}

	/**
	 * Removes all disk cache entries from the given directory. This should not be called directly,
	 * call {@link DiskLruCache#clearCache(android.content.Context, String)} or {@link DiskLruCache#clearCache()}
	 * instead.
	 *
	 * @param cacheDir The directory to remove the cache files from
	 */
	private static void clearCache(File cacheDir) {
		final File[] files = cacheDir.listFiles(cacheFileFilter);
        for (File file : files) {
            file.delete();
        }
	}

	/**
	 * Get a usable cache directory (external if available, internal otherwise).
	 *
	 * @param context The context to use
	 * @param uniqueName A unique directory name to append to the cache dir
	 * @return The cache dir
	 */
	public static File getDiskCacheDir(Context context, String uniqueName) {
        String cachePath;
		// Check if media is mounted or storage is built-in, if so, try and use external cache dir
		// otherwise use internal cache dir
        if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) ||
                !Environment.isExternalStorageRemovable()) {
            try{
                cachePath = context.getExternalCacheDir().getPath();
            } catch (Exception e) {
                cachePath = context.getCacheDir().getPath();
            }
        } else {
            cachePath = context.getCacheDir().getPath();
        }
        if(cachePath == null || cachePath.equals("")) {
            cachePath = context.getFilesDir().getPath();
        }
		return new File(cachePath + File.separator + uniqueName);
	}

	/**
	 * Creates a constant cache file path given a target cache directory and an image key.
	 *
	 * @param cacheDir
	 * @param key
	 * @return
	 */
	public static String createFilePath(File cacheDir, String key) {
		try {
			// Use URLEncoder to ensure we have a valid filename, a tad hacky but it will do for
			// this example
			return cacheDir.getAbsolutePath() + File.separator +
					CACHE_FILENAME_PREFIX + URLEncoder.encode(key.replace("*", ""), "UTF-8");
		} catch (final UnsupportedEncodingException e) {
			Log.e(TAG, "createFilePath - " + e);
		}

		return null;
	}

	/**
	 * Create a constant cache file path using the current cache directory and an image key.
	 *
	 * @param key
	 * @return
	 */
	public String createFilePath(String key) {
		return createFilePath(mCacheDir, key);
	}

	/**
	 * Sets the target compression format and quality for images written to the disk cache.
	 *
	 * @param compressFormat
	 * @param quality
	 */
	public void setCompressParams(CompressFormat compressFormat, int quality) {
		mCompressFormat = compressFormat;
		mCompressQuality = quality;
	}

	/**
	 * Writes a bitmap to a file. Call {@link DiskLruCache#setCompressParams(android.graphics.Bitmap.CompressFormat, int)}
	 * first to set the target bitmap compression and format.
	 *
	 * @param bitmap
	 * @param file
	 * @return
	 */
	private boolean writeBitmapToFile(Bitmap bitmap, String file)
			throws IOException, FileNotFoundException {
		if(bitmap == null)
			return false;
		OutputStream out = null;
		try {
			out = new BufferedOutputStream(new FileOutputStream(file), IO_BUFFER_SIZE);
			return bitmap.compress(mCompressFormat, mCompressQuality, out);
		} finally {
			if (out != null) {
				out.close();
			}
		}
	}
	
	/*********************** 替换Utils.java的函数 ***********************/
	private static final int IO_BUFFER_SIZE = 8 * 1024; 
	private static long getUsableSpace(File path) {
		/*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
			 return path.getUsableSpace();
		}*/
		final StatFs stats = new StatFs(path.getPath());
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return stats.getBlockSizeLong() * stats.getAvailableBlocksLong();
        } else {
            //noinspection deprecation
            return (long) stats.getBlockSize() * (long) stats.getAvailableBlocks();
        }
	}
	
	private static boolean isExternalStorageRemovable() {
		/*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
			return Environment.isExternalStorageRemovable();
		}*/
		return true;
	}
	
	private static boolean hasExternalCacheDir() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO;
	}
	
	private static File getExternalCacheDir(Context context) {
		if (hasExternalCacheDir()) {
			return context.getExternalCacheDir(); 
		}
		// Before Froyo we need to construct the external cache dir ourselves
		final String cacheDir = "/Android/data/" + context.getPackageName() + "/cache/";
		return new File(Environment.getExternalStorageDirectory().getPath() + cacheDir);
	}
}

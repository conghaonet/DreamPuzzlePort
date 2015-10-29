package org.hao.cache;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.media.ThumbnailUtils;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.widget.ImageView;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.hao.puzzle54.AppConstants;
import org.hao.puzzle54.MyApp;
import org.hao.puzzle54.MyHttpClient;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;
import java.util.Stack;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * 图片加载类
 * 
 * @author 月月鸟
 */
public class ImageManager2 {

	private static ImageManager2 imageManager;
	private static final int DISK_CACHE_SIZE = 1024 * 1024 * 10; // 10MB
	private static final String DISK_CACHE_SUBDIR = "thumbnails";
	public DiskLruCache mDiskCache;
	private static MyApp myApp;
	private static Bitmap emptyFrame;
	/** 图片加载队列，后进先出 */
	private Stack<ImageRef> mImageQueue = new Stack<ImageRef>();
	/** 图片请求队列，先进先出，用于存放已发送的请求。 */
	private Queue<ImageRef> mRequestQueue = new LinkedList<ImageRef>();
	/** 图片加载线程消息处理器 */
	private Handler mImageLoaderHandler;
	/** 图片加载线程是否就绪 */
	private boolean mImageLoaderIdle = true;
	/** 请求图片 */
	private static final int MSG_REQUEST = 1;
	/** 图片加载完成 */
	private static final int MSG_REPLY = 2;
	/** 中止图片加载线程 */
	private static final int MSG_STOP = 3;
	/** 如果图片是从网络加载，则应用渐显动画，如果从缓存读出则不应用动画 */
	private boolean isFromNet = true;
	private AssetManager mAssetManager;

	/**
	 * 获取单例，只能在UI线程中使用。
	 * 
	 * @param context
	 * @return
	 */
	public static ImageManager2 from(Context context, Bitmap emptyFrame) {
		// 如果不在ui线程中，则抛出异常
		if (Looper.myLooper() != Looper.getMainLooper()) {
			throw new RuntimeException("Cannot instantiate outside UI thread.");
		}
		
		if(ImageManager2.emptyFrame != emptyFrame) {
			if(ImageManager2.emptyFrame != null && !ImageManager2.emptyFrame.isRecycled()) {
				ImageManager2.emptyFrame.recycle();
			}
			ImageManager2.emptyFrame = emptyFrame;
		}
		if (imageManager == null) {
			imageManager = new ImageManager2(context);
		}
		return imageManager;
	}

	/**
	 * 私有构造函数，保证单例模式
	 * 
	 * @param context
	 */
	private ImageManager2(Context context) {
		if(myApp == null) {
			if(context instanceof MyApp) {
				myApp = (MyApp)context;
			} else {
				myApp = (MyApp)context.getApplicationContext();
			}
		}
		File cacheDir = DiskLruCache.getDiskCacheDir(context, DISK_CACHE_SUBDIR);
		mDiskCache = DiskLruCache.openCache(cacheDir, DISK_CACHE_SIZE);
	}

	/**
	 * 存放图片信息
	 */
	class ImageRef {

		/** 图片对应ImageView控件 */
		ImageView imageView;
		/** 图片URL地址 */
		String url;
		/** 图片缓存路径 */
		String filePath;
		/** 默认图资源ID */
		int resId;
		int width = 0;
		int height = 0;
		ZipFile zipFile;
		boolean isInnerPics;

		/**
		 * 构造函数
		 * 
		 * @param imageView
		 * @param url
		 * @param resId
		 * @param filePath
		 */
		ImageRef(ImageView imageView, String url, String filePath, int resId) {
			this.imageView = imageView;
			this.url = url;
			this.filePath = filePath;
			this.resId = resId;
		}

		ImageRef(ImageView imageView, String url, String filePath, int resId, int width, int height, ZipFile zipFile, boolean isInnerPics) {
			this.imageView = imageView;
			this.url = url;
			this.filePath = filePath;
			this.resId = resId;
			this.width = width;
			this.height = height;
			this.zipFile = zipFile;
			this.isInnerPics = isInnerPics;
		}
	}

	/**
	 * 显示图片
	 * 
	 * @param imageView
	 * @param url
	 * @param resId
	 */
	public void displayImage(ImageView imageView, String url, int resId) {
		if (imageView == null) return;
		if (imageView.getTag() != null && imageView.getTag().toString().equals(url)) return;
		if (resId >= 0) {
			if (imageView.getBackground() == null) {
				imageView.setBackgroundResource(resId);
			}
			imageView.setImageDrawable(null);
		}
		if (url == null || url.equals("")) return;
		// 添加url tag
		imageView.setTag(url);
		// 读取map缓存
		Bitmap bitmap = myApp.getMemCache().get(url);
		if (bitmap != null && !bitmap.isRecycled()) {
			setImageBitmap(imageView, bitmap, false);
			return;
		}
//		// 生成文件名
//		String filePath = urlToFilePath(url);
//		if (filePath == null) return;
//		queueImage(new ImageRef(imageView, url, filePath, resId));
		queueImage(new ImageRef(imageView, url, null, resId));
	}

	/**
	 * 显示图片固定大小图片的缩略图，一般用于显示列表的图片，可以大大减小内存使用
	 * 
	 * @param imageView 加载图片的控件
	 * @param url 加载地址
	 * @param resId 默认图片
	 * @param width 指定宽度
	 * @param height 指定高度
	 */
	public void displayImage(ImageView imageView, String url, int resId, int width, int height, ZipFile zipFile, boolean isInnerPics) {
		if(isInnerPics && mAssetManager == null) {
			mAssetManager = imageManager.myApp.getAssets();
		} else if(!isInnerPics && mAssetManager != null) {
			mAssetManager = null;
		}
		if (imageView == null) return;
		if (resId > 0) {//set default image
			if (imageView.getBackground() == null) {
				imageView.setBackgroundResource(resId);
			}
			imageView.setImageDrawable(null);
		} else {
			imageView.setImageBitmap(ImageManager2.emptyFrame);
		}
		if (url == null || url.equals("")) return;
		// 添加url tag
		imageView.setTag(url);
		// 读取map缓存
		long lastModified = 0;
		if(!isInnerPics && zipFile != null) {
			lastModified = myApp.getLastModifiedOfZipLastUsedPackage();
		}
		Bitmap bitmap = myApp.getMemCache().get(ImageManager2.getCacheKey(url, width, height, lastModified));
		if (bitmap != null && !bitmap.isRecycled()) {
			setImageBitmap(imageView, bitmap, false);
			return;
		}
//		// 生成文件名
//		String filePath = urlToFilePath(url);
//		if (filePath == null) return;
//		queueImage(new ImageRef(imageView, url, filePath, resId, width, height, zipFile, isInnerPics));
		queueImage(new ImageRef(imageView, url, null, resId, width, height, zipFile, isInnerPics));
	}
	private static String getCacheKey(String url, int width, int height, long lastModified) {
		return "v"+myApp.getVersionCode(null)+"_"+url + "_" + width + "_" + height+"_"+lastModified;
	}
	/**
	 * 入队，后进先出
	 * 
	 * @param imageRef
	 */
	public void queueImage(ImageRef imageRef) {
		// 删除已有ImageView
		Iterator<ImageRef> iterator = mImageQueue.iterator();
		while (iterator.hasNext()) {
			if (iterator.next().imageView == imageRef.imageView) {
				iterator.remove();
			}
		}
		// 添加请求
		mImageQueue.push(imageRef);
		sendRequest();
	}

	/**
	 * 发送请求
	 */
	private void sendRequest() {
		// 开启图片加载线程
		if (mImageLoaderHandler == null) {
			HandlerThread imageLoader = new HandlerThread("image_loader");
			imageLoader.start();
			mImageLoaderHandler = new ImageLoaderHandler(
					imageLoader.getLooper());
		}
		// 发送请求
		if (mImageLoaderIdle && mImageQueue.size() > 0) {
			ImageRef imageRef = mImageQueue.pop();
			Message message = mImageLoaderHandler.obtainMessage(MSG_REQUEST,
					imageRef);
			mImageLoaderHandler.sendMessage(message);
			mImageLoaderIdle = false;
			mRequestQueue.add(imageRef);
		}
	}

	/**
	 * 图片加载线程
	 */
	class ImageLoaderHandler extends Handler {
		public ImageLoaderHandler(Looper looper) {
			super(looper);
		}
		public void handleMessage(Message msg) {
			if (msg != null) {
				switch (msg.what) {
					case MSG_REQUEST: // 收到请求
						Bitmap bitmap = null;
						Bitmap tBitmap = null;
						ZipEntry zipEntry = null;
						if (msg.obj != null && msg.obj instanceof ImageRef) {
							ImageRef imageRef = (ImageRef) msg.obj;
							String url = imageRef.url;
							if (url != null) {
								// 如果本地url即读取sd相册图片，则直接读取，不用经过DiskCache
								if (!url.toLowerCase(Locale.getDefault()).startsWith("http")) {
									BitmapFactory.Options opt = new BitmapFactory.Options();
									opt.inJustDecodeBounds = true;
									try {
										if(imageRef.isInnerPics) {
											if(mAssetManager == null) mAssetManager = imageManager.myApp.getAssets();
											BufferedInputStream input = new BufferedInputStream(mAssetManager.open(AppConstants.PICS_FOLDER_IN_ZIP + File.separator + url));
											BitmapFactory.decodeStream(input, null, opt);
											input.close();
										} else {
											if (imageRef.zipFile != null) {
												zipEntry = new ZipEntry(url.substring(url.indexOf(File.separator) + 1));
												BufferedInputStream input = new BufferedInputStream(imageRef.zipFile.getInputStream(zipEntry));
												BitmapFactory.decodeStream(input, null, opt);
												input.close();
											} else {
												BitmapFactory.decodeFile(url, opt);
											}
										}
									} catch (Exception e) {
										e.printStackTrace();
									}
									if(opt.outWidth > 0 && opt.outHeight > 0) {
										float intSampleSize = (float) opt.outWidth / (float) (imageRef.width);
										opt.inSampleSize = new BigDecimal(intSampleSize).setScale(0, BigDecimal.ROUND_HALF_UP).intValue();
										opt.inJustDecodeBounds = false;
										opt.inPreferredConfig = Bitmap.Config.RGB_565;
										if(imageRef.isInnerPics) {
											InputStream input = null;
											try {
												if(mAssetManager == null) mAssetManager = imageManager.myApp.getAssets();
												input = mAssetManager.open(AppConstants.PICS_FOLDER_IN_ZIP + File.separator + url);
												tBitmap = BitmapFactory.decodeStream(input, null, opt);
												input.close();
											} catch (IOException e) {
												if (input != null) {
													try {
														input.close();
													} catch (IOException e1) {
														e1.printStackTrace();
													}
												}
											}
										} else {
											if(imageRef.zipFile != null) {
												BufferedInputStream input = null;
												try {
													input = new BufferedInputStream(imageRef.zipFile.getInputStream(zipEntry));
													tBitmap = BitmapFactory.decodeStream(input, null, opt);
													input.close();
												} catch (IOException e) {
													if (input != null) {
														try {
															input.close();
														} catch (IOException e1) {
															e1.printStackTrace();
														}
													}
												} catch (OutOfMemoryError e) {
													if (input != null) {
														try {
															input.close();
														} catch (IOException e1) {
															e1.printStackTrace();
														}
													}
													try {
														input = new BufferedInputStream(imageRef.zipFile.getInputStream(zipEntry));
														tBitmap = BitmapFactory.decodeStream(input, null, opt);
														input.close();
													} catch (Exception e1) {
														e1.printStackTrace();
													}
												} finally {
													if (input != null) {
														try {
															input.close();
														} catch (IOException e1) {
															e1.printStackTrace();
														}
													}
												}
											} else {
												try {
													tBitmap = BitmapFactory.decodeFile(url, opt);
												} catch (Exception e) {
													e.printStackTrace();
												}
											}
										}
									}
									if (tBitmap != null) {
										try {
											if (imageRef.width > 0 && imageRef.height > 0) {
												if (tBitmap.getWidth() != imageRef.width || tBitmap.getHeight() != imageRef.height) {
													bitmap = ThumbnailUtils.extractThumbnail(tBitmap, imageRef.width, imageRef.height, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
												} else {
													bitmap = tBitmap;
												}
												isFromNet = false;
											} else {
												bitmap = tBitmap;
												tBitmap = null;
											}
										} catch (Exception e) {
											e.printStackTrace();
										}
									}
								} else {
									bitmap = mDiskCache.get(url);
								}
							}
							if (bitmap != null) {
								// ToolUtil.log("从disk缓存读取");
								// 写入map缓存
								try {
									if (imageRef.width > 0 && imageRef.height > 0) {
										long lastModified = 0;
										if(!imageRef.isInnerPics && imageRef.zipFile != null) {
											lastModified = myApp.getLastModifiedOfZipLastUsedPackage();
										}
										if (myApp.getMemCache().get(ImageManager2.getCacheKey(url, imageRef.width, imageRef.height, lastModified)) == null)
											myApp.getMemCache().put(ImageManager2.getCacheKey(url, imageRef.width, imageRef.height, lastModified), bitmap);
									} else {
										if (myApp.getMemCache().get(url) == null)
											myApp.getMemCache().put(url, bitmap);
									}
								} catch (Exception e) {
									e.printStackTrace();
								}
							} else {
								try {
									if (url != null && url.toLowerCase(Locale.getDefault()).startsWith("http")) {
										byte[] data = loadByteArrayFromNetwork(url);
										if (data != null) {
											BitmapFactory.Options opt = new BitmapFactory.Options();
											opt.inSampleSize = 1;

											opt.inJustDecodeBounds = true;
											BitmapFactory.decodeByteArray(data, 0,
													data.length, opt);
											int bitmapSize = opt.outHeight * opt.outWidth
													* 4;// pixels*3 if it's RGB and pixels*4
											// if it's ARGB
											if (bitmapSize > 1000 * 1200)
												opt.inSampleSize = 2;
											opt.inJustDecodeBounds = false;
											tBitmap = BitmapFactory.decodeByteArray(data,
													0, data.length, opt);
											if (imageRef.width != 0 && imageRef.height != 0) {
												bitmap = ThumbnailUtils
														.extractThumbnail(
																tBitmap,
																imageRef.width,
																imageRef.height,
																ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
											} else {
												bitmap = tBitmap;
												tBitmap = null;
											}
											if (bitmap != null && url != null) {
												// 写入SD卡
												if (imageRef.width != 0
														&& imageRef.height != 0) {
													mDiskCache.put(url + imageRef.width
															+ imageRef.height, bitmap);
													myApp.getMemCache().put(url + imageRef.width
															+ imageRef.height, bitmap);
												} else {
													mDiskCache.put(url, bitmap);
													myApp.getMemCache().put(url, bitmap);
												}
												isFromNet = true;
											}
										}
									}
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						}
						if (mImageManagerHandler != null) {
							Message message = mImageManagerHandler.obtainMessage(MSG_REPLY, bitmap);
							mImageManagerHandler.sendMessage(message);
						}
						break;
					case MSG_STOP: // 收到终止指令
						Looper.myLooper().quit();
						break;
					default:
						break;
				}
			}
		}
	}

	/** UI线程消息处理器 */
	private Handler mImageManagerHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (msg != null) {
				switch (msg.what) {
				case MSG_REPLY: // 收到应答
					do {
						ImageRef imageRef = mRequestQueue.remove();
						if (imageRef == null) break;
						if (imageRef.imageView == null || imageRef.imageView.getTag() == null || imageRef.url == null) break;
						// 非同一ImageView
						if (!(imageRef.url).equals(imageRef.imageView.getTag())) break;
						if (!(msg.obj instanceof Bitmap) || msg.obj == null) break;
						Bitmap bitmap = (Bitmap) msg.obj;
						setImageBitmap(imageRef.imageView, bitmap, isFromNet);
						isFromNet = false;
					} while (false);
					break;
				}
			}
			// 设置闲置标志
			mImageLoaderIdle = true;
			// 若服务未关闭，则发送下一个请求。
			if (mImageLoaderHandler != null) {
				sendRequest();
			}
		}
	};

	/**
	 * 添加图片显示渐现动画
	 * 
	 */
	private void setImageBitmap(ImageView imageView, Bitmap bitmap, boolean isTran) {
		if (isTran) {
			final TransitionDrawable td = new TransitionDrawable(
					new Drawable[] {
							new ColorDrawable(android.R.color.transparent),
							new BitmapDrawable(ImageManager2.myApp.getResources(),bitmap) });
			td.setCrossFadeEnabled(true);
			imageView.setImageDrawable(td);
			td.startTransition(300);
		} else {
			imageView.setImageBitmap(bitmap);
		}
	}

	/**
	 * 从网络获取图片字节数组
	 * 
	 * @param url
	 * @return
	 */
	private byte[] loadByteArrayFromNetwork(String url) {
		HttpGet method = null;
		try {
			method = new HttpGet(url);
			HttpResponse response = MyHttpClient.getInstance().execute(method);
			HttpEntity entity = response.getEntity();
			return EntityUtils.toByteArray(entity);
		} catch (Exception e) {
			return null;
		} finally {
			if(method != null && !method.isAborted()) method.abort();
		}
	}

	/**
	 * 根据url生成缓存文件完整路径名
	 * 
	 * @param url
	 * @return
	 */
	public String urlToFilePath(String url) {
		// 扩展名位置
		int index = url.lastIndexOf('.');
		if (index == -1) {
			return null;
		}
        //noinspection StringBufferReplaceableByString
        StringBuilder filePath = new StringBuilder();
		// 图片存取路径
		filePath.append(myApp.getCacheDir().toString()).append('/');
		// 图片文件名
		filePath.append(MD5.Md5(url)).append(url.substring(index));
		return filePath.toString();
	}

	/**
	 * Activity#onStop后，ListView不会有残余请求。
	 */
	public void stop() {
		// 清空请求队列
		mImageQueue.clear();
		mAssetManager = null;
	}
}

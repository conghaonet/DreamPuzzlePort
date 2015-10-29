package org.hao.puzzle54;

import android.app.ActivityManager;
import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PathEffect;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.support.v4.util.LruCache;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import com.flurry.android.FlurryAgent;

import org.hao.database.DBHelperCustom;
import org.hao.database.DBHelperMorepuzzles;
import org.hao.database.DBHelperScore;
import org.hao.puzzle54.custom.CustomConstants;
import org.hao.puzzle54.custom.CustomPicEntity;
import org.hao.puzzle54.services.ApkUpdateService;
import org.hao.puzzle54.services.DownloadZipMonitorService;
import org.hao.puzzle54.services.DownloadZipReceiver;
import org.hao.puzzle54.services.MyAdsService;
import org.hao.puzzle54.services.NetworkAvailableReceiver;
import org.hao.puzzle54.services.UpdatePuzzlesXmlService;
import org.hh.puzzle.port54.hall.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class MyApp extends Application {
	private static final String TAG = MyApp.class.getName();
	private Typeface fontArialRounded;
	private LastUsedPackage lastUsedPackage;
	private LruCache<String, Bitmap> mMemoryCache;
	private final Object memoryCacheLock = new Object();
	private HashMap<String, List<String>> customSrcImagesMap;
	private HashSet<String> customSelectedSet;
	private Properties propApkUpdate;
	public int showMainActivityTimes;
	public Hashtable<String, Class> tableUnfinishedServices;
	private int intAvailablePuzzleWidth;
	private int intAvailablePuzzleHeight;
	private List<String> listInnerPics;
	private final Object innerPicsLock = new Object();
	private List<SkinEntity> listSkins;
	private final Object skinsLock = new Object();

	@Override
	public void onCreate() {
		super.onCreate();
		FlurryAgent.setLogEnabled(false);
		FlurryAgent.init(this, getString(R.string.flurry_api_key));

		this.showMainActivityTimes = 0;
		tableUnfinishedServices = new Hashtable<String, Class>();
		tableUnfinishedServices.put(MyAdsService.class.getName(), MyAdsService.class);
		tableUnfinishedServices.put(ApkUpdateService.class.getName(), ApkUpdateService.class);
		tableUnfinishedServices.put(UpdatePuzzlesXmlService.class.getName(), UpdatePuzzlesXmlService.class);
		tableUnfinishedServices.put(DownloadZipMonitorService.class.getName(), DownloadZipMonitorService.class);
		getMemCache();
		InitAppPreference();
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		Editor editor = pref.edit();
		AppPrefUtil.setRunTimesOfApp(this, editor, AppPrefUtil.getRunTimesOfApp(this, pref)+1);
		editor.apply();

		if(!AppPrefUtil.isMergedOldPref(this, pref)) {
			if(mergeOldPref()) {
				setPackageNameInApkUpdateProp();
				editor = pref.edit();
				AppPrefUtil.setMergedOldPref(this, editor, true);
				editor.apply();
			}
		} else {
			if(getPackageNameInApkUpdateProp() == null) setPackageNameInApkUpdateProp();
		}
//		setupNewPackage();
		IntentFilter filterZip = new IntentFilter(this.getPackageName()+DownloadZipReceiver.ACTION_DOWNLOAD_FINISHED);
		DownloadZipReceiver zipReceiver = new DownloadZipReceiver();
		registerReceiver(zipReceiver, filterZip);
		IntentFilter filterNetwork = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
		NetworkAvailableReceiver networkReceiver = new NetworkAvailableReceiver();
		registerReceiver(networkReceiver, filterNetwork);
	}
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        MyHttpClient.shutdown();
    }
 
    @Override
    public void onTerminate() {
        super.onTerminate();
        MyHttpClient.shutdown();
    }
    public Typeface getArialRounded() {
    	if(this.fontArialRounded == null) {
        	fontArialRounded = Typeface.createFromAsset(getAssets(), AppConstants.FONT_FILE_ARIAL_ROUNDED);
    	}
    	return this.fontArialRounded;
    }
    public LruCache<String, Bitmap> getMemCache() {
    	if(this.mMemoryCache == null) {
    		synchronized(this.memoryCacheLock) {
    			if(this.mMemoryCache == null) {
            		final int memClass = ((ActivityManager)this.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass();
//            		memClass = memClass > 64 ? 64 : memClass;
            		final int cacheSize = 1024 * 1024 * memClass / 8;
            		this.mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
            			@Override 
            	        protected int sizeOf(String key, Bitmap bitmap) {
                            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
                                return bitmap.getByteCount();
                            } else {
                                return bitmap.getRowBytes() * bitmap.getHeight();
                            }
            			}
            			/*
            			@Override
                        protected void entryRemoved(boolean evicted, String key, Bitmap oldValue, Bitmap newValue) {
            				Log.d(TAG, "=========entryRemoved evicted="+evicted);
            				if(evicted) {
            					Log.d(TAG, "=========entryRemoved key="+key);
            					if(oldValue != null && !oldValue.isRecycled()) oldValue.recycle();
            					if(newValue != null && !newValue.isRecycled()) newValue.recycle();
            				}
            			}
            			*/
            		};
    			}
    			this.memoryCacheLock.notifyAll();
    		}
    	}
    	return this.mMemoryCache;
    }
    public DisplayMetrics getDisplay() {
	    DisplayMetrics dm = new DisplayMetrics();
	    WindowManager windowMgr = (WindowManager)getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
	    Display display = windowMgr.getDefaultDisplay();
	    display.getMetrics(dm);
    	return dm;
    }
	public DisplayMetrics getRealDisplay() {
		DisplayMetrics dm = new DisplayMetrics();
		WindowManager windowMgr = (WindowManager)getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
		Display display = windowMgr.getDefaultDisplay();
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
			display.getRealMetrics(dm);
		} else {
			display.getMetrics(dm);
		}
		return dm;
	}

	private void InitAppPreference() {
		SharedPreferences tempPref = this.getSharedPreferences(this.getPackageName()+AppConstants.DEFAULT_SHARED_PREFERENCES_SUFFIX, MODE_WORLD_READABLE+MODE_MULTI_PROCESS);
		if(tempPref.getAll().size()==0) {
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
			dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
			tempPref.edit().putString("first_install_time", dateFormat.format(new Date())).apply();
		}
		PreferenceManager.setDefaultValues(this, R.xml.settings, true);
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);//(Context.MODE_PRIVATE);//("settings", Context.MODE_PRIVATE);
		int inPrefVersionCode = AppPrefUtil.getApkVersionCode(this, pref);
		try {
			if(inPrefVersionCode != getVersionCode(null)) {
				Editor editor = pref.edit();
				AppPrefUtil.setApkVersionCode(this, editor, getVersionCode(null));
				editor.apply();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	private boolean mergeOldPref() {
		String oldAppPackageName = getUpadtedPackageNameInApkUpdateProp();
		if(oldAppPackageName != null && !oldAppPackageName.equals(this.getPackageName())) {
			try {
				Context oldAppContext = this.createPackageContext(oldAppPackageName, Context.CONTEXT_IGNORE_SECURITY);
				SharedPreferences oldPref = oldAppContext.getSharedPreferences(oldAppPackageName+AppConstants.DEFAULT_SHARED_PREFERENCES_SUFFIX, MODE_WORLD_READABLE+MODE_MULTI_PROCESS);
				if(AppPrefUtil.getApkVersionCode(this, oldPref) >= getVersionCode(null)) {
					return false;
				}
				SharedPreferences thisPref = PreferenceManager.getDefaultSharedPreferences(this);
				Editor thisEditor = thisPref.edit();
				Map<String, ?> mapOldPref = oldPref.getAll();
                for (String strOldKey : mapOldPref.keySet()) {
                    Object objOldValue = mapOldPref.get(strOldKey);
                    if (objOldValue instanceof String) {
                        thisEditor.putString(strOldKey, (String) objOldValue);
                    } else if (objOldValue instanceof Boolean) {
                        thisEditor.putBoolean(strOldKey, (Boolean) objOldValue);
                    } else if (objOldValue instanceof Integer) {
                        thisEditor.putInt(strOldKey, (Integer) objOldValue);
                    } else if (objOldValue instanceof Long) {
                        thisEditor.putLong(strOldKey, (Long) objOldValue);
                    } else if (objOldValue instanceof Float) {
                        thisEditor.putFloat(strOldKey, (Float) objOldValue);
                    }
                }
				thisEditor.apply();
				AppPrefUtil.setRatedApp(this, thisEditor, false);
				AppPrefUtil.setRunTimesOfApp(this, thisEditor, 1);
				AppPrefUtil.setApkVersionCode(this, thisEditor, getVersionCode(null));
				AppPrefUtil.setOldApp(this, thisEditor, oldAppPackageName);
				thisEditor.apply();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return true;
	}
	public int getVersionCode(String packageName) {
		if(packageName == null || packageName.equals("")) packageName = getPackageName();
		try {
			return getPackageManager().getPackageInfo(packageName,0).versionCode;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
			return 0;
		}
	}
	public String getThisVersionName() {
		try {
			return getPackageManager().getPackageInfo(getPackageName(),0).versionName;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
			return "1.0";
		}
	}
	public String getCustomDB() {
		return getFolderFullPath(CustomConstants.CUSTOM_FOLDER, true) + AppConstants.CUSTOM_DB_NAME;
	}
	public String getMorepuzzlesDB() {
		if(isAdult()) return getPuzzleDataBasePath(true)+AppConstants.MOREPUZZLES_ADULT_DB_NAME;
		else return getPuzzleDataBasePath(true)+AppConstants.MOREPUZZLES_DB_NAME;
	}
	public String getScoreDB() {
		return getPuzzleDataBasePath(true)+DBHelperScore.FILE_NAME;
	}
	private String getApkUpdatePropFilePath() {
		return getPuzzleDataBasePath(true)+AppConstants.PROP_FILE_APKUPDATE;
	}
	private synchronized Properties getApkUpdateProp() {
		if(this.propApkUpdate == null) {
			this.propApkUpdate = new Properties();
			File fileApkUpdate = new File(getApkUpdatePropFilePath());
			if(!fileApkUpdate.exists()) {
				try {
					fileApkUpdate.createNewFile();
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				try {
					this.propApkUpdate.load(new FileInputStream(fileApkUpdate));
					Set<Object> setNames = this.propApkUpdate.keySet();
					if(setNames != null && setNames.size() > 0) {
						Object[] arrayNames = setNames.toArray();
						for (Object propName: arrayNames) {
							if(propName.equals(AppConstants.UPDATED_KEY_IN_PROP_FILE_APKUPDATE)) continue;
							String tempPackage = this.propApkUpdate.getProperty((String)propName, null);
							if(tempPackage != null && getVersionCode(tempPackage) <= 0) {
								this.propApkUpdate.remove(propName);
							}
						}
						if(this.propApkUpdate.keySet().size() < arrayNames.length) {
							this.propApkUpdate.store(new FileOutputStream(fileApkUpdate, false), "");
						}
					}
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return this.propApkUpdate;
	}
	public String getHallPackageNameInApkUpdateProp() {
		return getApkUpdateProp().getProperty(AppConstants.DREAMPUZZLE_HALL_CODE, null);
	}
	public String getPackageNameInApkUpdateProp() {
		return getApkUpdateProp().getProperty(getString(R.string.theme_code), null);
	}
	public void setUpadtedPackageNameInApkUpdateProp() {
		getApkUpdateProp().setProperty(AppConstants.UPDATED_KEY_IN_PROP_FILE_APKUPDATE, getString(R.string.theme_code));
		File fileApkUpdate = new File(getApkUpdatePropFilePath());
		try{
			if(!fileApkUpdate.exists()) fileApkUpdate.createNewFile();
			getApkUpdateProp().store(new FileOutputStream(fileApkUpdate, false), "");
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	public String getUpadtedPackageNameInApkUpdateProp() {
		String updatedPackageName;
		if(getString(R.string.theme_code).equals(AppConstants.DREAMPUZZLE_HALL_CODE)) {
			updatedPackageName = getApkUpdateProp().getProperty(AppConstants.DREAMPUZZLE_HALL_CODE, null);
			if(updatedPackageName == null) {
				String updatedCode = getApkUpdateProp().getProperty(AppConstants.UPDATED_KEY_IN_PROP_FILE_APKUPDATE, null);
				if(updatedCode != null) {
					updatedPackageName = getApkUpdateProp().getProperty(updatedCode, null);
				}
			}
		} else {
			updatedPackageName = getApkUpdateProp().getProperty(getString(R.string.theme_code), null);
		}
		return updatedPackageName;
	}
	public void setPackageNameInApkUpdateProp() {
		if(!getPackageName().equals(getApkUpdateProp().getProperty(getString(R.string.theme_code), null))) {
			getApkUpdateProp().setProperty(getString(R.string.theme_code), getPackageName());
			File fileApkUpdate = new File(getApkUpdatePropFilePath());
			try{
				if(!fileApkUpdate.exists()) fileApkUpdate.createNewFile();
				getApkUpdateProp().store(new FileOutputStream(fileApkUpdate, false), "");
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
	public String getConnectivityNetworkName() {
		ConnectivityManager connManager= (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = connManager.getActiveNetworkInfo();
		if(info != null && info.isAvailable()) {
			return info.getTypeName();
		} return null;
	}
	public boolean isTablet() {
		return (getResources().getConfiguration().screenLayout & 
				Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
	}
	
	public ArrayList<ZipEntry> getCurrentZipEntries(String packageCode) {
		boolean blnResetLastUsedPackage = false;
		if(this.lastUsedPackage == null || !this.lastUsedPackage.getPackageCode().equalsIgnoreCase(packageCode)) {
			blnResetLastUsedPackage = true;
		} else {
			File currentPackageFile = getPackageFile(packageCode);
			if(this.lastUsedPackage.getZipFileLastModified() != currentPackageFile.lastModified()) {
				blnResetLastUsedPackage = true;
			}
		}
		if(blnResetLastUsedPackage) {
			this.lastUsedPackage = new LastUsedPackage(packageCode);
		}
		return this.lastUsedPackage.getListZipEntry();
	}
	public ZipFile getPackageZipFile(String packageCode) {
		ZipFile zipFile = null;
		try {
			zipFile = new ZipFile(getPackageFile(packageCode));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return zipFile;
	}
	public long getLastModifiedOfZipLastUsedPackage() {
		if(this.lastUsedPackage == null) return -1;
		else return this.lastUsedPackage.getZipFileLastModified();
	}
	public Location getLocation() {
		Location location = null;
		try {
		    LocationManager locMan = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		    location = locMan.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		} catch (Exception e) {
			location = null;
		}
	    return location;
	}
	public long getAllowedMaxPicIndex(String packageCode) {
		if(isDebugMode()) return 999999;
		long allowIndex = 0;
		DBHelperScore helperApp = null;
		SQLiteDatabase dbApp = null;
		try {
			helperApp = DBHelperScore.getInstance(this);
			dbApp = helperApp.getReadableDatabase();
			ScoreEntity scoreEntity = helperApp.getMaxPicIndexScore(packageCode, AppPrefUtil.getRows(this, null), AppPrefUtil.getCols(this, null), dbApp);
			if(scoreEntity == null) {
				allowIndex = 0;
			} else {
				allowIndex = scoreEntity.getPicIndex()+1;
			}
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			if(dbApp != null) dbApp.close();
		}
		if(allowIndex < AppConstants.ALLOWED_MIN_PIC_INDEX) allowIndex = AppConstants.ALLOWED_MIN_PIC_INDEX;
		return allowIndex;
	}
	public com.google.android.gms.ads.AdSize getAdSizeForAdmob() {
		if(isTablet()) {
			return com.google.android.gms.ads.AdSize.FULL_BANNER;
		} else {
			return com.google.android.gms.ads.AdSize.BANNER;
		}
	}
	public int getAdViewMeasureHeight() {
		if(isTablet()) {
			return (int)Math.ceil(getDisplay().density * AppConstants.AD_BANNER_TABLET_HEIGHT);
		} else {
			return (int)Math.ceil(getDisplay().density * AppConstants.AD_BANNER_PHONE_HEIGHT);
		}
	}
	public int getAdViewMeasureWidth() {
		if(isTablet()) {
			return (int)Math.ceil(getDisplay().density * AppConstants.AD_BANNER_TABLET_WIDTH);
		} else {
			return (int)Math.ceil(getDisplay().density * AppConstants.AD_BANNER_PHONE_WIDTH);
		}
	}
	public File getCustomCameraFile(Date date) {
		return new File(getFolderFullPath(CustomConstants.CAMERA_FOLDER, true) + AppTools.buildCustomCameraName(date));
	}
	public File getCustomPicture(String customFileName) {
		return new File(this.getFolderFullPath(CustomConstants.PICTURE_FOLDER, true) + customFileName);
	}
	public int getDefaultMoveTimes(int rows, int cols) {
		int pieces = rows * cols;
		int times = AppConstants.DEFAULT_MENU_MOVE_TIMES;
		if(pieces <= 9) times = 1;
		else if(pieces > 9 && pieces <= 20) times = 2;
		else if(pieces > 20 && pieces < 42) times = 3;
		else if(pieces >= 42 && pieces < 64) times = 4;
		else if(pieces >= 64) times = 5;
		return times;
	}
	public int getDefaultEyeTimes(int rows, int cols) {
		int pieces = rows * cols;
		int times = AppConstants.DEFAULT_MENU_EYE_TIMES;
		if(pieces <= 9) times = 1;
		else if(pieces > 9 && pieces <= 20) times = 2;
		else if(pieces > 20 && pieces < 42) times = 3;
		else if(pieces>=42 && pieces<64) return times = 4;
		else if(pieces>=64) return times = 5;
		return times;
	}
	public Intent saveSharePicture(long currentPicIndex, String packageCode) {
		String shareFile = getAppCachePath(null, true)+"share.jpg";
		boolean hasFile = false;
		Intent it = new Intent(Intent.ACTION_SEND);
		it.setType("image/*");
		it.putExtra(Intent.EXTRA_SUBJECT, this.getString(R.string.app_name));
		it.putExtra(Intent.EXTRA_TITLE, this.getString(R.string.app_name));
		it.putExtra(Intent.EXTRA_TEXT, this.getString(R.string.SHARE_TEXT)+AppConstants.RATE_APP_URL+this.getPackageName());
		if(AppConstants.CUSTOM_PACKAGE_CODE.equals(packageCode)) {
			CustomPicEntity currentCustomEntity = DBHelperCustom.getInstance(this).getById(currentPicIndex, null);
			File file = getCustomPicture(currentCustomEntity.getImageName());
			if(file.exists()) {
				try {
					hasFile = AppTools.inputStream2File(new FileInputStream(file), new File(shareFile));
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}
		} else {
			InputStream input = null;
			try {
				if(isInnerPics(packageCode)) {
					input = getAssets().open(AppConstants.PICS_FOLDER_IN_ZIP + File.separator + getInnerPics().get((int) currentPicIndex));
					hasFile = AppTools.inputStream2File(input, new File(shareFile));
				} else {
					ZipFile zip = getPackageZipFile(packageCode);
					ZipEntry zipEntry = getCurrentZipEntries(packageCode).get((int)currentPicIndex);
					input = zip.getInputStream(zipEntry);
					hasFile = AppTools.inputStream2File(input, new File(shareFile));
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if(input != null) try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		if(hasFile) it.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://"+shareFile));
		return it;
	}
	public void autoIncreaseRowCol() {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		String currentDifficulty = AppPrefUtil.getDifficulty(this, pref);
		String[] arrDifficulty = this.getResources().getStringArray(R.array.difficulty_value);
		for(int i=1;i<arrDifficulty.length;i++) {
			if(arrDifficulty[i-1].toLowerCase(Locale.US).equalsIgnoreCase(currentDifficulty)) {
				Editor editor = pref.edit();
				AppPrefUtil.setDifficulty(this, editor, arrDifficulty[i]);
				editor.apply();
				return;
			}
		}
	}
	public String getAppFilesPath(boolean isEndWithFileSeparator) {
		String strPath = null;
		if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
			strPath =  this.getExternalFilesDir(null).getPath();
		} else {
			strPath =  this.getFilesDir().getPath();
		}
		if(isEndWithFileSeparator) return strPath+File.separator;
		else return strPath;
	}
	public String getAppCachePath(String subFolder, boolean isEndWithFileSeparator) {
		String cachePath = null;
		if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			cachePath =  this.getExternalCacheDir().getPath();
		} else {
			cachePath =  this.getCacheDir().getPath();
		}
		if(subFolder != null && !subFolder.trim().equalsIgnoreCase("")) {
			cachePath = cachePath + File.separator + subFolder.trim();
		}
		if(isEndWithFileSeparator) return cachePath+File.separator;
		else return cachePath;
	}
	private String getFolderFullPath(String subPath, boolean isEndWithFileSeparator) {
		if(subPath == null) return null;
		File file = null;
		if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
			file = new File(Environment.getExternalStorageDirectory().getPath() + File.separator + subPath);
		} else {
			file = new File(getAppFilesPath(false) + File.separator + subPath);
		}
		if(!file.exists() || !file.isDirectory()) {
			if(!createFolder(file)) {
				return null;
			}
		}
		if(isEndWithFileSeparator) return file.getPath() + File.separator;
		else return file.getPath();
	}
	private boolean createFolder(File directory) {
		return createFolder(directory, true);
	}
	private boolean createFolder(File directory, boolean isWithNomedia) {
		if(directory == null) return false;
		if(directory.exists() && directory.isDirectory()) {
			return true;
		} else {
			if(directory.mkdirs()) {
				if(isWithNomedia) {
					File fileNomedia = new File(directory.getPath() + File.separator + AppConstants.NOMEDIA_FILENAME);
					try {
						if(fileNomedia.createNewFile()) return true;
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return false;
	}
	public String getPuzzleDataBasePath(boolean isEndWithFileSeparator) {
		return getFolderFullPath(AppConstants.APP_BASE_FOLDER, isEndWithFileSeparator);
	}
	public File getHallApk() {
		return new File(getPuzzleDataBasePath(true)+AppConstants.DREAMPUZZLE_HALL_CODE+AppConstants.EXTENSION_NAME_APK);
	}
	public File getPackageFile(String packageCode) {
		return new File(getPuzzleDataBasePath(true)+packageCode+AppConstants.EXTENSION_NAME_PACKAGE);
	}
	public class LastUsedPackage {
		private ZipFile zipFile;
		private long zipFileLastModified;
		private String packageCode;
		private ArrayList<ZipEntry> listZipEntry;
		public LastUsedPackage(String packageCode) {
			this.packageCode = packageCode;
			this.zipFileLastModified = getPackageFile(packageCode).lastModified();
			this.zipFile = getPackageZipFile(this.packageCode);
			this.listZipEntry = new ArrayList<ZipEntry>(zipFile.size());
			Enumeration<ZipEntry> enumZipEntry = (Enumeration<ZipEntry>) zipFile.entries(); // 获取zip文件中的目录及文件列表
			while (enumZipEntry.hasMoreElements()) {
				ZipEntry entry = enumZipEntry.nextElement();
				if (!entry.isDirectory()) {
					if(entry.getName().startsWith(AppConstants.PICS_FOLDER_IN_ZIP) && entry.getName().toLowerCase(Locale.getDefault()).endsWith(AppConstants.EXTENSION_NAME_PICTURE)) {
						// 如果文件不是目录，则添加到列表中
						listZipEntry.add(entry);
					}
				}
			}
			ComparatorPicNameInZip comparator=new ComparatorPicNameInZip();
			Collections.sort(listZipEntry, comparator);
		}
		public ZipFile getZipFile() {
			return zipFile;
		}
		public long getZipFileLastModified() {
			return zipFileLastModified;
		}
		public String getPackageCode() {
			return packageCode;
		}
		public ArrayList<ZipEntry> getListZipEntry() {
			return listZipEntry;
		}
	}
	public Bitmap getBitmapEmptyFrame(float radius, int width) {
		return getBitmapEmptyFrame(radius, width, width);
	}
	public Bitmap getBitmapEmptyFrame(float radius, int width, int height) {
		radius = radius * getDisplay().density;
		if(this.isTablet()) radius = radius * 2;
		Bitmap bitmapEmptyFrame = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_4444);
        Canvas canvas = new Canvas(bitmapEmptyFrame);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.GRAY);
        float strokeWidth = 3 * getDisplay().density;
        if(this.isTablet()) strokeWidth = strokeWidth * 2;
        paint.setStrokeWidth(strokeWidth);              //线宽  
        paint.setStyle(Style.STROKE); 
        float dash = 5 * getDisplay().density;
        if(this.isTablet()) dash = dash * 2;
        PathEffect effects = new DashPathEffect(new float[] { dash, dash}, 1);
        paint.setPathEffect(effects);
        RectF rectf = new RectF(0F, 0F, width, height);
        canvas.drawRoundRect(rectf, radius, radius, paint);
		return bitmapEmptyFrame;
	}
	public Bitmap getRoundedCornerBitmap(Bitmap bitmap, float radius) {
		radius = radius * getDisplay().density;
		Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Config.ARGB_8888);
		Canvas canvas = new Canvas(output);
		final int color = 0xff424242;
		final Paint paint = new Paint();
		final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
		final RectF rectF = new RectF(rect);
		paint.setAntiAlias(true);
		canvas.drawARGB(0, 0, 0, 0);
		paint.setColor(color);
		canvas.drawRoundRect(rectF, radius, radius, paint);
		paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
		canvas.drawBitmap(bitmap, rect, rect, paint);
		if(bitmap != null && !bitmap.isRecycled()) bitmap.recycle();
		return output;
	}
	
	public boolean isAdult() {
		return Boolean.parseBoolean(getString(R.string.is_adult));
	}
	public boolean hasAds() {
		return Boolean.parseBoolean(getString(R.string.has_ads));
	}
	public boolean isDebugMode() {
		return Boolean.parseBoolean(getString(R.string.is_debug));
	}
	public boolean hasGoogle() {
		return Boolean.parseBoolean(getString(R.string.has_google));
	}
	public HashMap<String, List<String>> getCustomSrcImagesMap() {
		if(this.customSrcImagesMap != null && !this.customSrcImagesMap.isEmpty()) return this.customSrcImagesMap;
		this.customSrcImagesMap = new HashMap<String, List<String>>();
		List<String> listPath = new ArrayList<String>();
		ContentResolver cr = this.getContentResolver();
		String[] proj = {MediaStore.Images.Media.DATA};
		Cursor cur = cr.query(Images.Media.EXTERNAL_CONTENT_URI, proj, null, null, null);
		try {
			int colIndex = cur.getColumnIndex(MediaStore.Images.Media.DATA);
			for (cur.moveToFirst(); !cur.isAfterLast(); cur.moveToNext()) {
				String strPath = cur.getString(colIndex);
				if(strPath!=null && strPath.toLowerCase().endsWith(".jpg") || strPath.toLowerCase().endsWith(".jpeg")) {
					listPath.add(strPath);
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			if(cur != null) cur.close();
		}
		for(String strPath:listPath) {
			String strFolder = AppTools.getFullParentPathEndWithSeparator(strPath);//strPath.substring(0, strPath.lastIndexOf("/"));
			List<String> tempList = this.customSrcImagesMap.get(strFolder);
			if(tempList == null) {
				tempList = new ArrayList<String>();
			}
			tempList.add(strPath);
			this.customSrcImagesMap.put(strFolder, tempList); 
		}
		return this.customSrcImagesMap;
	}
	public void clearCustomSrcImagesMap() {
		if(this.customSrcImagesMap != null) this.customSrcImagesMap.clear();
	}
	public HashSet<String> getCustomSelectedSet() {
		if(this.customSelectedSet == null) this.customSelectedSet = new HashSet<String>();
		return this.customSelectedSet;
	}
	public void clearCustomSelectedSet() {
		if(this.customSelectedSet != null) this.customSelectedSet.clear();
	}
	public void clearAllCustomCollection() {
		clearCustomSrcImagesMap();
		clearCustomSelectedSet();
	}
	public int getSpacingOfPictureGridItem() {
		int spacing = 2;
		if(isTablet()) spacing = spacing * 2;
		int intItemSpacing = (int)(this.getDisplay().density * spacing);
		if(intItemSpacing < spacing) intItemSpacing = spacing;
		return intItemSpacing;
	}
	public String getMainBackgroundImageCacheKey() {
		return "v"+ getVersionCode(null)+"_main_background_image";
	}
	public void setAvailablePuzzleWidthHeight(int width, int height) {
		this.intAvailablePuzzleWidth = width;
		this.intAvailablePuzzleHeight = height;
	}
	public int getAvailablePuzzleWidth() {
		return this.intAvailablePuzzleWidth;
	}
	public int getAvailablePuzzleHeight() {
		return this.intAvailablePuzzleHeight;
	}
	public float getPuzzleRatioWidthHeight() {
		return (float)this.intAvailablePuzzleWidth / (float)this.intAvailablePuzzleHeight;
	}
	public List<String> getInnerPics() {
		if(this.listInnerPics == null) {
			synchronized (this.innerPicsLock) {
				if(this.listInnerPics == null) {
					this.listInnerPics = new ArrayList<String>();
					try {
						String[] picsArray = getAssets().list(AppConstants.PICS_FOLDER_IN_ZIP);
						for(String filename: picsArray) {
							if(filename.toLowerCase().endsWith(".jpg") || filename.toLowerCase().endsWith(".jpeg")) {
								this.listInnerPics.add(filename);
							}
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				this.innerPicsLock.notifyAll();
			}
		}
		return this.listInnerPics;
	}
	public boolean isInnerPics(String packageCode) {
		if(AppConstants.DREAMPUZZLE_HALL_CODE.equals(packageCode) || !getString(R.string.theme_code).equals(packageCode)) return false;
		if(getInnerPics().size() <= 0) return false;
		PicsPackageEntity entity = DBHelperMorepuzzles.getInstance(this).getEntityByCode(packageCode, null);
		if(entity == null || entity.getState().equals(PicsPackageEntity.PackageStates.NOTINSTALL)) {
			return true;
		} else {
			return false;
		}
	}
	public List<SkinEntity> getSkins() {
		if(this.listInnerPics == null) {
			synchronized (this.skinsLock) {
				if(this.listSkins == null) {
					this.listSkins = new ArrayList<SkinEntity>();
					try {
						String[] picsArray = getAssets().list(AppConstants.SKINS_FOLDER_IN_ASSETS);
						for(String filename: picsArray) {
							if(filename.toLowerCase().endsWith(".jpg") || filename.toLowerCase().endsWith(".jpeg")) {
								SkinEntity entity = new SkinEntity();
								String code = filename.substring(0,filename.toLowerCase().lastIndexOf("."));
								entity.setCode(code);
								entity.setAssetsFullPath(AppConstants.SKINS_FOLDER_IN_ASSETS+File.separator+filename);
								this.listSkins.add(entity);
							}
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				this.skinsLock.notifyAll();
			}
		}
		return this.listSkins;
	}
}

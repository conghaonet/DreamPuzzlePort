package org.hao.puzzle54;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

import com.flurry.android.FlurryAgent;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.common.GooglePlayServicesUtil;

import org.hao.puzzle54.services.ApkUpdateReceiver;
import org.hao.puzzle54.services.DownloadApkReceiver;
import org.hao.puzzle54.services.DownloadApkService;
import org.hh.puzzle.port54.hall.R;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActMain extends ActionBarActivity {
	public static final String TAG = ActMain.class.getName();
	private static final int HANDLER_MSG_NEWVERSION_HAS_SHOW_DIALOG = 101;
	private static final int HANDLER_MSG_NEWVERSION_HAS_NO_DIALOG = 102;
	private MyApp myApp;
	private MyHandler mHandler;
	private ImageView imgBgMain;
    private ApkUpdateReceiver apkUpdateReceiver;
	private DownloadApkReceiver downloadApkReceiver;
	private long apkTotalSize;
	private boolean apkDownloadFinished;
	private boolean isExit;
	private InterstitialAd mInterstitial;
    private boolean hasNewApkVersion;
//	private long lastplayScoreId;
//	private String lstPalyPackageCode;
	private boolean blnActivityHasFocus;
	private ActionBar actionBar;
	private void forTesting() {
		
	}
	
	/** Called when the activity is first created. */

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
//			Display display = getWindowManager().getDefaultDisplay();
//			Point point = new Point();
//			display.getRealSize(point);
//			int realScreenHeight = point.y;
		}
		//force display "3 points" menu button
		try {
			ViewConfiguration config = ViewConfiguration.get(this);
			Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
			if(menuKeyField != null) {
				menuKeyField.setAccessible(true);
				menuKeyField.setBoolean(config, false);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		this.myApp = (MyApp)getApplicationContext();
		Toolbar toolbar = (Toolbar)findViewById(R.id.main_mytoolbar);
		setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
//		if(getString(R.string.theme_code).equals(AppConstants.DREAMPUZZLE_HALL_CODE)) {
//			actionBar.setIcon(R.drawable.ic_launcher);
//		} else {
//			actionBar.setDisplayShowHomeEnabled(false);
//		}
		actionBar.setDisplayShowHomeEnabled(false);
		actionBar.setDisplayShowTitleEnabled(true);
		actionBar.setTitle(getString(R.string.app_name)+"  v"+myApp.getThisVersionName());

		this.myApp.showMainActivityTimes = this.myApp.showMainActivityTimes + 1;
		if(this.myApp.showMainActivityTimes == 1) {
			startAnotherApk();
//			deleteOldApp();
		}
		this.mHandler = new MyHandler(this);
		forTesting();
		/*
		if(this.myApp.showMainActivityTimes > 1) {
			this.mInterstitial = new InterstitialAd(this);
			this.mInterstitial.setAdListener(new MyAdListener());
			this.mInterstitial.setAdUnitId(AppPrefUtil.getAdBannerId(this, null));
			AdRequest.Builder adBuilder = new AdRequest.Builder();
//			adBuilder.addTestDevice("4871FC3CAF1B2357D61D20E7A6974E15");
//			adBuilder.addTestDevice("B49F8C051F6B5E739FF3DC17B592ECEE");
			this.mInterstitial.loadAd(adBuilder.build());
		}
		*/

		imgBgMain = (ImageView)findViewById(R.id.mainBgMain);
		Button btnPlay = (Button) findViewById(R.id.mainBtnPlay);
		Button btnContinue = (Button) findViewById(R.id.mainBtnContinue);
		btnPlay.getPaint().setFakeBoldText(true);
		btnContinue.getPaint().setFakeBoldText(true);

		apkUpdateReceiver = new ApkUpdateReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				ActMain.this.mHandler.sendMessage(ActMain.this.mHandler.obtainMessage(ActMain.HANDLER_MSG_NEWVERSION_HAS_SHOW_DIALOG));
			}
		};
		downloadApkReceiver = new DownloadApkReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				ActMain.this.apkTotalSize = (Long)intent.getExtras().get(DownloadApkService.INTENT_EXTRA_LONG_TOTAL_SIZE);
				ActMain.this.apkDownloadFinished = (Boolean)intent.getExtras().get(DownloadApkService.INTENT_EXTRA_BOOLEAN_FINISHED);
			}
		};
		
		btnPlay.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                FlurryAgent.logEvent(FlurryEvents.MAIN_PLAY);
                Intent intent = new Intent();
                intent.setClass(ActMain.this, PackageGridActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                ActMain.this.finish();
            }
        });
		final String lstPalyPackageCode = AppPrefUtil.getLastPlayPackageCode(ActMain.this, null);
		long lastplayScoreId = AppPrefUtil.getLastPlayScoreId(ActMain.this, null);
		if (lstPalyPackageCode == null || lstPalyPackageCode.trim().equalsIgnoreCase("") || lastplayScoreId <= 0) {
			(findViewById(R.id.main_continue_layout)).setVisibility(View.GONE);
		} else {
			btnContinue.setOnClickListener(new Button.OnClickListener() {
				@Override
				public void onClick(View v) {
					FlurryAgent.logEvent(FlurryEvents.MAIN_CONTINUE);
					Intent intent = new Intent();
					intent.setClass(ActMain.this, PuzzleActivity.class);
					Bundle bundle = new Bundle();
					bundle.putString(MyBundleData.PACKAGE_CODE, lstPalyPackageCode);
					bundle.putLong(MyBundleData.PICTURE_INDEX, -1);
					bundle.putBoolean(MyBundleData.PUZZLE_IS_CONTINUE, true);
					intent.putExtras(bundle);
					startActivity(intent);
					overridePendingTransition(R.anim.push_bottom_in, R.anim.push_bottom_out);
					finish();
				}
			});
		}
		if(AppPrefUtil.hasNewApkVersion(ActMain.this, null)) {
			ActMain.this.mHandler.sendMessage(ActMain.this.mHandler.obtainMessage(ActMain.HANDLER_MSG_NEWVERSION_HAS_NO_DIALOG));
		}
		setBackgroundImage();
		registerMyReceivers();
		loadAd();
	}
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		if (hasFocus && !this.blnActivityHasFocus) {
			this.blnActivityHasFocus = hasFocus;
			int intAvailablePuzzleWidth = myApp.getDisplay().widthPixels;
			int intAvailablePuzzleHeight = myApp.getDisplay().heightPixels-this.actionBar.getHeight() - myApp.getAdViewMeasureHeight();
			myApp.setAvailablePuzzleWidthHeight(intAvailablePuzzleWidth, intAvailablePuzzleHeight);

			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && AppPrefUtil.getRunTimesOfApp(this, null) == 1) {
				DisplayMetrics dm = this.myApp.getRealDisplay();
				if((float)dm.heightPixels / (float)dm.widthPixels < 1.6f) {
					SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
					Editor editor = pref.edit();
					AppPrefUtil.setFullScreenMode(this, editor, false);
					editor.apply();
				}
			}
		}
	}
	@Override
	public boolean onMenuOpened(int featureId, Menu menu) {
		if (featureId == Window.FEATURE_ACTION_BAR && menu != null) {
			if (menu.getClass().getSimpleName().equals("MenuBuilder")) {
				try {
					Method m = menu.getClass().getDeclaredMethod("setOptionalIconsVisible", Boolean.TYPE);
					m.setAccessible(true);
                    m.invoke(menu, true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
        }
        return super.onMenuOpened(featureId, menu);
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
	}
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem menuItemUpdate = menu.findItem(R.id.main_menu_update);
		if(this.hasNewApkVersion) {
			menuItemUpdate.setVisible(true);
		} else {
			menuItemUpdate.setVisible(false);
		}
		return super.onPrepareOptionsMenu(menu);
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem menu) {
		switch(menu.getItemId()) {
		case R.id.main_menu_update:
			if(this.apkTotalSize <= 0) {
				showUpdateDialog();
			} else if(this.apkDownloadFinished) {
				Uri uriFile = Uri.fromFile(myApp.getHallApk());
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setDataAndType(uriFile, "application/vnd.android.package-archive");
				startActivity(intent);
				NotificationManager mNotificationManager = (NotificationManager) myApp.getSystemService(Context.NOTIFICATION_SERVICE);
				mNotificationManager.cancel(DownloadApkService.NOTIFICATION_ID);
			}
			break;
		case R.id.main_menu_skin:
			Intent intent = new Intent();
			intent.setClass(this, SkinActivity.class);
			startActivity(intent);
			overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
			finish();
			break;
		case R.id.main_menu_settings:
			openSettings(null);
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(menu);
	}
	private void setBackgroundImage() {
		if(myApp.getMemCache().get(myApp.getMainBackgroundImageCacheKey()) != null) {
			Bitmap bitmap = myApp.getMemCache().get(myApp.getMainBackgroundImageCacheKey());
			if(!bitmap.isRecycled()) {
				this.imgBgMain.setImageBitmap(bitmap);
				return;
			}
		}
		String themeCode = AppPrefUtil.getThemeCode(this, null);
		List<SkinEntity> listSkinEntity = myApp.getSkins();
		SkinEntity skinEntity = null;
		if(themeCode != null) {
			skinEntity = new SkinEntity();
			skinEntity.setCode(themeCode);
			int indexTheme = listSkinEntity.indexOf(skinEntity);
			if(indexTheme > -1) skinEntity = listSkinEntity.get(indexTheme);
			else skinEntity = null;
		}
		if(themeCode == null || skinEntity == null) {
			skinEntity = listSkinEntity.get(0);
			SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
			Editor editor = pref.edit();
			AppPrefUtil.setThemeCode(this, editor, skinEntity.getCode());
			editor.apply();
		}
		InputStream input = null;
		try {
			AssetManager mAssetManager = this.getAssets();
			input = mAssetManager.open(skinEntity.getAssetsFullPath());
			BitmapFactory.Options opts = new BitmapFactory.Options();
			opts.inJustDecodeBounds = true;
			opts.inPreferredConfig = Bitmap.Config.RGB_565;
			BitmapFactory.decodeStream(input, null, opts);
			input.close();
			opts.inSampleSize = opts.outWidth / this.myApp.getDisplay().widthPixels;
			opts.inJustDecodeBounds = false;
			input = mAssetManager.open(skinEntity.getAssetsFullPath());
			Bitmap bitmap = BitmapFactory.decodeStream(input, null, opts);
			myApp.getMemCache().put(myApp.getMainBackgroundImageCacheKey(),bitmap);
			this.imgBgMain.setImageBitmap(myApp.getMemCache().get(myApp.getMainBackgroundImageCacheKey()));
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if(input != null)
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
	}
	public void openSettings(View v) {
		FlurryAgent.logEvent(FlurryEvents.MAIN_SETTINGS);
		Intent intent = new Intent();
		intent.setClass(ActMain.this, SettingsActivity.class);
		startActivity(intent);
		overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
		finish();
	}
	private void startAnotherApk() {
		String strPackageNameInApkUpdateProp;
		if(getString(R.string.theme_code).equals(AppConstants.DREAMPUZZLE_HALL_CODE)) {
			strPackageNameInApkUpdateProp = myApp.getPackageNameInApkUpdateProp();
			if(strPackageNameInApkUpdateProp != null && !strPackageNameInApkUpdateProp.equals(getPackageName())) {
				if(this.myApp.getVersionCode(strPackageNameInApkUpdateProp) > this.myApp.getVersionCode(null)) {
					try {
						openApp(strPackageNameInApkUpdateProp);
					} catch (NameNotFoundException e) {
						e.printStackTrace();
					}
				}
			}
		} else {
			strPackageNameInApkUpdateProp = myApp.getHallPackageNameInApkUpdateProp();
			if(strPackageNameInApkUpdateProp != null && !strPackageNameInApkUpdateProp.equals(getPackageName()) && this.myApp.getVersionCode(strPackageNameInApkUpdateProp) > 0) {
				try {
					openApp(strPackageNameInApkUpdateProp);
				} catch (NameNotFoundException e) {
					e.printStackTrace();
				}
			} else {
				strPackageNameInApkUpdateProp = myApp.getPackageNameInApkUpdateProp();
				if(!strPackageNameInApkUpdateProp.equals(getPackageName()) && this.myApp.getVersionCode(strPackageNameInApkUpdateProp) > this.myApp.getVersionCode(null)) {
					try {
						openApp(strPackageNameInApkUpdateProp);
					} catch (NameNotFoundException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	private void openApp(String packageName) throws NameNotFoundException {
		PackageManager pm = getPackageManager();
		PackageInfo pi = pm.getPackageInfo(packageName, 0);
		Intent resolveIntent = new Intent(Intent.ACTION_MAIN, null);
		resolveIntent.addCategory(Intent.CATEGORY_LAUNCHER);
		resolveIntent.setPackage(pi.packageName);
		List<ResolveInfo> apps = pm.queryIntentActivities(resolveIntent, 0);
		ResolveInfo ri = apps.iterator().next();
		if (ri != null ) {
			String className = ri.activityInfo.name;
			Intent intent = new Intent(Intent.ACTION_MAIN);
			intent.addCategory(Intent.CATEGORY_LAUNCHER);
			ComponentName cn = new ComponentName(packageName, className);
			intent.setComponent(cn);
//			Bundle bundle = new Bundle();
//			bundle.putString("oldpackage", "conghao======================");
//			intent.putExtras(bundle);
			this.isExit = true;
			startActivity(intent);
			this.finish();
		}
	}
	private void deleteOldApp() {
		if(this.isExit) return;
		String oldAppPackageName = AppPrefUtil.getOldApp(this, null);
		if(oldAppPackageName != null) {
	        try {
	        	PackageInfo packageInfo = this.getPackageManager().getPackageInfo(oldAppPackageName, 0);
	        	showUninstallDialog(oldAppPackageName);
	        } catch (NameNotFoundException e) {
	            e.printStackTrace();
	            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
	    		Editor editor = pref.edit();
	            AppPrefUtil.setOldApp(this, editor, null);
	            editor.apply();
	        }
		}
	}
	private void showUninstallDialog(final String oldAppPackageName) {
		Builder dialogUninstall = new AlertDialog.Builder(this);
		dialogUninstall.setCancelable(true);
		dialogUninstall.setTitle(R.string.DIALOG_UNINSTALL_OLD_VERSION_TITLE);
		dialogUninstall.setMessage(R.string.DIALOG_UNINSTALL_OLD_VERSION_MSG);
		dialogUninstall.setPositiveButton(
				R.string.DIALOG_BUTTON_OK,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Uri packageURI = Uri.fromParts("package", oldAppPackageName, null);
						Intent intent = new Intent(Intent.ACTION_DELETE, packageURI);
						startActivity(intent);
					}
				}
			);
		dialogUninstall.show();
	}
	private void registerMyReceivers() {
		IntentFilter filterApkUpdate = new IntentFilter();
		filterApkUpdate.addAction(this.getPackageName()+ApkUpdateReceiver.ACTION_APPSUPDATE);
		registerReceiver(apkUpdateReceiver, filterApkUpdate);
		IntentFilter filterApkDownload = new IntentFilter();
		filterApkDownload.addAction(this.getPackageName()+DownloadApkReceiver.ACTION_DOWNLOAD_APK);
		registerReceiver(downloadApkReceiver, filterApkDownload);
	}
	private void unregisterMyReceivers() {
		try {
			if(this.apkUpdateReceiver != null) unregisterReceiver(this.apkUpdateReceiver);
		} catch(Exception e) {
			e.printStackTrace();
		}
		try {
			if(this.downloadApkReceiver != null) unregisterReceiver(this.downloadApkReceiver);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	static class MyHandler extends Handler {
        WeakReference<ActMain> mActivity;
        MyHandler(ActMain activity) {
        	mActivity = new WeakReference<ActMain>(activity);
        }
        @Override
        public void handleMessage(Message msg) {
        	ActMain theActivity = mActivity.get();
        	if(theActivity == null) return;
        	switch (msg.what) {
			case ActMain.HANDLER_MSG_NEWVERSION_HAS_SHOW_DIALOG:
				theActivity.showApkUpdateButton();
				if(theActivity.apkTotalSize <= 0) theActivity.showUpdateDialog();
				break;
			case ActMain.HANDLER_MSG_NEWVERSION_HAS_NO_DIALOG:
				theActivity.showApkUpdateButton();
				break;
			default:
				break;
            }
        }
	}

	@Override
	public void onResume() {
		super.onResume();
	}
	@Override
	public void onBackPressed() {
		if(!showRateMenu()) {
			showExitMenu();
		}
	}
	@Override
	public void onStart() {
		super.onStart();
		FlurryAgent.onStartSession(this);
		if(this.myApp.showMainActivityTimes == 1) {
    	   	Map<String, String> flurryParams = new HashMap<String, String>();
			flurryParams.put("APK_PACKAGE_NAME", getPackageName());
    	   	flurryParams.put("APK_VERSION_CODE", String.valueOf(this.myApp.getVersionCode(null)));
			flurryParams.put("ANDROID_VERSION", String.valueOf(Build.VERSION.SDK_INT));
			FlurryAgent.logEvent(FlurryEvents.MAIN_START, flurryParams);
       }
	}
	@Override
	public void onStop() {
      super.onStop();
      FlurryAgent.onEndSession(this);
	}
	@Override
	public void onPause() {
		super.onPause();
	}
	@Override
	public void onDestroy() {
		super.onDestroy();
		unregisterMyReceivers();
		System.gc();
		if(isExit) {
//			myApp.mMemoryCache.evictAll();
			MyHttpClient.shutdown();
			System.exit(0);
//			android.os.Process.killProcess(android.os.Process.myPid());
		}
	}
	private void showApkUpdateButton() {
		this.hasNewApkVersion = true;
		supportInvalidateOptionsMenu();
	}
	private void startDownloadApk() {
		Intent apkDownloadIntent = new Intent(this, DownloadApkService.class);
		startService(apkDownloadIntent);
	}
	private void showUpdateDialog() {
		AlertDialog.Builder dialogApkupdate = new AlertDialog.Builder(this);
		dialogApkupdate.setCancelable(true);
		dialogApkupdate.setTitle(R.string.DIALOG_APKUPDATE_TITLE);
		dialogApkupdate.setMessage(R.string.DIALOG_APKUPDATE_MSG);
		dialogApkupdate.setPositiveButton(
				R.string.DIALOG_BUTTON_UPDATE,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if(AppPrefUtil.getApkNewVersionGoogleUri(ActMain.this, null) != null 
								&& !AppPrefUtil.getApkNewVersionGoogleUri(ActMain.this, null).equalsIgnoreCase("")) {
							Uri uri = Uri.parse(AppPrefUtil.getApkNewVersionGoogleUri(ActMain.this, null));
							Intent intent = new Intent(Intent.ACTION_VIEW, uri);
							try {
								Context googlePlayContext = ActMain.this.createPackageContext(GooglePlayServicesUtil.GOOGLE_PLAY_STORE_PACKAGE, Context.CONTEXT_INCLUDE_CODE+Context.CONTEXT_IGNORE_SECURITY);
								Class mAssetBrowserActivity = googlePlayContext.getClassLoader().loadClass(GooglePlayServicesUtil.GOOGLE_PLAY_STORE_PACKAGE + ".AssetBrowserActivity");
								intent.setClassName(googlePlayContext, mAssetBrowserActivity.getName());
								startActivity(intent);
							} catch (Exception e) {
								startDownloadApk();
							}
						} else {
							startDownloadApk();
						}
					}
				}
			);
		dialogApkupdate.setNegativeButton(
				R.string.DIALOG_BUTTON_LATER,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
					}
				}
			);
		if(!this.isFinishing()) dialogApkupdate.show();
	}
	private boolean showRateMenu() {
		if(!this.myApp.hasGoogle()) return false;
		if(this.myApp.showMainActivityTimes <= 1) return false;
		if(AppPrefUtil.isRatedApp(this, null) || AppPrefUtil.getRunTimesOfApp(this, null)<7) return false;
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ActMain.this);
		Editor editor = pref.edit();
		AppPrefUtil.setRatedApp(ActMain.this, editor, true);
		editor.apply();
//		loadAd();
		AlertDialog.Builder dialogRate = new AlertDialog.Builder(this);
		dialogRate.setCancelable(true);
		dialogRate.setMessage(R.string.DIALOG_RATE_MSG);
		dialogRate.setPositiveButton(
				R.string.DIALOG_BUTTON_RATE,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Uri uri = Uri.parse(AppConstants.RATE_APP_URL+getPackageName());
						Intent intent = new Intent(Intent.ACTION_VIEW, uri);
						try {
							Context googlePlayContext = ActMain.this.createPackageContext(GooglePlayServicesUtil.GOOGLE_PLAY_STORE_PACKAGE, Context.CONTEXT_INCLUDE_CODE+Context.CONTEXT_IGNORE_SECURITY);
							Class mAssetBrowserActivity = googlePlayContext.getClassLoader().loadClass(GooglePlayServicesUtil.GOOGLE_PLAY_STORE_PACKAGE + ".AssetBrowserActivity");
							intent.setClassName(googlePlayContext, mAssetBrowserActivity.getName());
						} catch (Exception e) {
                            e.printStackTrace();
						}
						startActivity(intent);
					}
				}
			);
		dialogRate.setNegativeButton(
				R.string.DIALOG_BUTTON_CANCEL,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						ActMain.this.isExit = true;
						displayInterstitial();
					}
				}
			);
		dialogRate.show();
		return true;
	}
	private void showExitMenu() {
//		loadAd();
		AlertDialog.Builder dialogExit = new AlertDialog.Builder(this);
		dialogExit.setCancelable(true);
		dialogExit.setMessage(R.string.DIALOG_EXIT_MSG);
		dialogExit.setPositiveButton(
				R.string.DIALOG_BUTTON_EXIT,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						ActMain.this.isExit = true;
						displayInterstitial();
					}
				});
		if(this.myApp.hasGoogle()) {
			dialogExit.setNeutralButton(
					R.string.DIALOG_BUTTON_MORE_APPS,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							FlurryAgent.logEvent(FlurryEvents.MAIN_MOREAPPS);
							Uri uri = null;
							if(myApp.isAdult()) {
								uri = Uri.parse(AppConstants.MORE_GAMES_URL_ADULT);
							} else {
								uri = Uri.parse(AppConstants.MORE_GAMES_URL);
							}
							Intent intent = new Intent(Intent.ACTION_VIEW, uri);
							try {
								Context googlePlayContext = ActMain.this.createPackageContext(GooglePlayServicesUtil.GOOGLE_PLAY_STORE_PACKAGE, Context.CONTEXT_INCLUDE_CODE+Context.CONTEXT_IGNORE_SECURITY);
								Class mAssetBrowserActivity = googlePlayContext.getClassLoader().loadClass(GooglePlayServicesUtil.GOOGLE_PLAY_STORE_PACKAGE + ".AssetBrowserActivity");
								intent.setClassName(googlePlayContext, mAssetBrowserActivity.getName());
							} catch (Exception e) {
                                e.printStackTrace();
							}
							startActivity(intent);
						}
					});
		}
		dialogExit.setNegativeButton(
				R.string.DIALOG_BUTTON_CANCEL,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
					}
				});
		dialogExit.show();
	}
	private void loadAd() {
		if(AppPrefUtil.getAdInterstitialId(this, null)==null || AppPrefUtil.getAdInterstitialId(this, null).equals("")) return;
		if(this.myApp.showMainActivityTimes > 1 && this.myApp.hasAds()) {
			this.mInterstitial = new InterstitialAd(this);
			this.mInterstitial.setAdListener(new MyAdListener());
			this.mInterstitial.setAdUnitId(AppPrefUtil.getAdInterstitialId(this, null));
			AdRequest adRequest = new AdRequest.Builder().build();
//			adBuilder.addTestDevice("B49F8C051F6B5E739FF3DC17B592ECEE");
			this.mInterstitial.loadAd(adRequest);
		}
	}
	private void displayInterstitial() {
		if (this.mInterstitial != null && this.mInterstitial.isLoaded()) {
			this.mInterstitial.show();
		} else {
			this.finish();		 
		}
	}
	class MyAdListener extends AdListener {
        @Override
        public void onAdLoaded() {
        	Log.d(TAG, "onAdLoaded()");
        }
        @Override
        public void onAdFailedToLoad(int errorCode) {
            String errorReason = "";
            switch(errorCode) {
                case AdRequest.ERROR_CODE_INTERNAL_ERROR:
                    errorReason = "Internal error";
                    break;
                case AdRequest.ERROR_CODE_INVALID_REQUEST:
                    errorReason = "Invalid request";
                    break;
                case AdRequest.ERROR_CODE_NETWORK_ERROR:
                    errorReason = "Network Error";
                    break;
                case AdRequest.ERROR_CODE_NO_FILL:
                    errorReason = "No fill";
                    break;
            }
        	Log.d(TAG, String.format("onAdFailedToLoad(%s)", errorReason));
        }
        @Override
        public void onAdOpened() {
        	Log.d(TAG, "onAdOpened()");
        }
        @Override
        public void onAdClosed() {
        	Log.d(TAG, "onAdClosed()");
        	ActMain.this.finish();
        }
        @Override
        public void onAdLeftApplication() {
        	Log.d(TAG, "onAdLeftApplication()");
			ActMain.this.finish();
        }
    }

}
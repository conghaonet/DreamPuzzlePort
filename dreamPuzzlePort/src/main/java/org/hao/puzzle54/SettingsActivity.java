package org.hao.puzzle54;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.Window;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesUtil;

import org.hao.database.DBHelperScore;
import org.hao.puzzle54.services.DownloadApkReceiver;
import org.hao.puzzle54.services.DownloadApkService;
import org.hh.puzzle.port54.hall.R;

import java.util.Locale;

public class SettingsActivity extends PreferenceActivity implements OnPreferenceChangeListener, OnPreferenceClickListener{
	private static final String TAG = SettingsActivity.class.getName();
	private String skinKey;
	private String difficultyKey;
	private String clearHistoryKey;
	private String clearCacheKey;
	private String updateKey;
	private String aboutRateKey;
	private String aboutVersionKey;
	private String[] arrEntries;
	private String[] arrValues;
	private ListPreference difficultyPref;
	private PreferenceScreen clearCachePref;
	private PreferenceScreen clearHistoryPref;
	private MyApp myApp;
    private DownloadApkReceiver downloadApkReceiver;
	private long apkTotalSize;
	private boolean apkDownloadFinished;

	@Override  
    public void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);

		//从xml文件中添加Preference项
		addPreferencesFromResource(R.xml.settings);
        getPreferenceScreen().removePreference(findPreference(getString(R.string.PREF_INVISIBLE_CATEGORY_KEY)));
		this.myApp = (MyApp)getApplicationContext();
		
		skinKey = getString(R.string.PREF_SKIN_KEY);
		PreferenceScreen skinPref = (PreferenceScreen)findPreference(skinKey);
		skinPref.setOnPreferenceClickListener(this);
		
        arrValues = this.getResources().getStringArray(R.array.difficulty_value);
        arrEntries = new String[arrValues.length];
        String strEntrySuffix = getString(R.string.PREF_DIFFICULTY_ENTRY_SUFFIX);
        for(int i=0;i<arrValues.length;i++) {
        	String[] rowCols = arrValues[i].toLowerCase(Locale.getDefault()).split("x");
        	int intRow = Integer.parseInt(rowCols[0]);
        	int intCol = Integer.parseInt(rowCols[1]);
        	if((intRow * intCol)<10) {
        		arrEntries[i] = arrValues[i]+" ( "+(intRow * intCol)+" "+strEntrySuffix+")";
        	} else {
        		arrEntries[i] = arrValues[i]+" ("+(intRow * intCol)+" "+strEntrySuffix+")";
        	}
        }
        
        difficultyKey = getResources().getString(R.string.PREF_DIFFICULTY_KEY);
        difficultyPref = (ListPreference)findPreference(difficultyKey);
        difficultyPref.setEntries(arrEntries);
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        String difficultyValue = AppPrefUtil.getDifficulty(this, pref);
        for(int i=0;i<arrValues.length;i++) {
        	if(difficultyValue.equalsIgnoreCase(arrValues[i])) {
        		difficultyPref.setValue(arrValues[i]);
        		difficultyPref.setTitle(getString(R.string.PREF_DIFFICULTY_TITLE)+": "+arrEntries[i]);
        		break;
        	}
        }
        difficultyPref.setOnPreferenceChangeListener(this);

        clearHistoryKey = getString(R.string.PREF_USERDATA_CLEAR_HISTORY_KEY);
        clearHistoryPref = (PreferenceScreen)findPreference(clearHistoryKey);
        clearHistoryPref.setOnPreferenceClickListener(this);
        
        clearCacheKey = getString(R.string.PREF_USERDATA_CLEAR_CACHE_KEY);
        clearCachePref = (PreferenceScreen)findPreference(clearCacheKey);
        clearCachePref.setOnPreferenceClickListener(this);

        this.aboutRateKey = getString(R.string.PREF_ABOUT_RATE_KEY);
        PreferenceScreen ratePref = (PreferenceScreen)findPreference(aboutRateKey);
        if(this.myApp.hasGoogle()) {
        	ratePref.setOnPreferenceClickListener(this);
        } else {
        	PreferenceCategory aboutCategory = (PreferenceCategory)findPreference(getString(R.string.PREF_ABOUT_CATEGORY_KEY));
        	aboutCategory.removePreference(findPreference(this.aboutRateKey));
        }
        
        this.aboutVersionKey = getString(R.string.PREF_ABOUT_VERSION_KEY);
        PreferenceScreen versionPref = (PreferenceScreen)findPreference(aboutVersionKey);
        versionPref.setTitle(getString(R.string.app_name)+"  v"+this.myApp.getThisVersionName());
        if(this.myApp.hasGoogle()) {
            versionPref.setOnPreferenceClickListener(this);
        } else {
        	versionPref.setSummary("");
        }
		
        updateKey = getString(R.string.PREF_UPDATE_KEY);
        if(AppPrefUtil.hasNewApkVersion(this, null)) {
            PreferenceScreen updatePref = (PreferenceScreen)findPreference(updateKey);
            updatePref.setOnPreferenceClickListener(this);
        } else {
        	getPreferenceScreen().removePreference(findPreference(getString(R.string.PREF_UPDATE_CATEGORY_KEY)));
        }
        downloadApkReceiver = new DownloadApkReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				SettingsActivity.this.apkTotalSize = (Long)intent.getExtras().get(DownloadApkService.INTENT_EXTRA_LONG_TOTAL_SIZE);
				SettingsActivity.this.apkDownloadFinished = (Boolean)intent.getExtras().get(DownloadApkService.INTENT_EXTRA_BOOLEAN_FINISHED);
			}
		};
       IntentFilter filterApkDownload = new IntentFilter();
       filterApkDownload.addAction(this.getPackageName()+DownloadApkReceiver.ACTION_DOWNLOAD_APK);
       registerReceiver(downloadApkReceiver, filterApkDownload);
	}
	
	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		Log.d(TAG, "preference.getKey()="+preference.getKey());
		if(preference.getKey().equals(this.difficultyKey)) {
			if(!newValue.toString().equalsIgnoreCase(difficultyPref.getValue())) {
				difficultyPref.setValue(newValue.toString());
				for(int i=0;i<arrValues.length;i++) {
		        	if(newValue.toString().equalsIgnoreCase(arrValues[i])) {
		        		difficultyPref.setTitle(getString(R.string.PREF_DIFFICULTY_TITLE)+": "+arrEntries[i]);
		        		break;
		        	}
		        }
			}
		}
		return true;
	}
	@Override
	public boolean onPreferenceClick(Preference preference) {
		if(preference.getKey().equals(this.skinKey)) {
			openThemeActivity();
		} else if(preference.getKey().equals(this.clearHistoryKey)) {
			showClearHistoryDialog();
		} else if(preference.getKey().equals(this.clearCacheKey)) {
			showClearCacheDialog();
		} else if(preference.getKey().equals(this.aboutVersionKey)) {
			doClickVersion();
		} else if(preference.getKey().equals(this.aboutRateKey)) {
			doClickRate();
		} else if(preference.getKey().equals(this.updateKey)) {
			if(this.apkTotalSize <= 0) showUpdateDialog();
			else if(this.apkDownloadFinished) {
				Uri uriFile = Uri.fromFile(myApp.getHallApk());
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setDataAndType(uriFile, "application/vnd.android.package-archive");
				startActivity(intent);
				NotificationManager mNotificationManager = (NotificationManager) myApp.getSystemService(Context.NOTIFICATION_SERVICE);
				mNotificationManager.cancel(DownloadApkService.NOTIFICATION_ID);
			}
		}
		return true;
	}
	private void openThemeActivity() {
		Intent intent = new Intent();
		intent.setClass(this, SkinActivity.class);
		startActivity(intent);
		overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
		finish();
	}
	private void doClickRate() {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		Editor editor = pref.edit();
		AppPrefUtil.setRatedApp(this, editor, true);
		editor.apply();
        Uri rateUri = Uri.parse(AppConstants.RATE_APP_URL+getPackageName());
        Intent rateIntent = new Intent(Intent.ACTION_VIEW, rateUri);
		try {
			Context googlePlayContext = SettingsActivity.this.createPackageContext(GooglePlayServicesUtil.GOOGLE_PLAY_STORE_PACKAGE, Context.CONTEXT_INCLUDE_CODE+Context.CONTEXT_IGNORE_SECURITY);
			Class mAssetBrowserActivity = googlePlayContext.getClassLoader().loadClass(GooglePlayServicesUtil.GOOGLE_PLAY_STORE_PACKAGE + ".AssetBrowserActivity");
			rateIntent.setClassName(googlePlayContext, mAssetBrowserActivity.getName());
		} catch (Exception e) {
            e.printStackTrace();
		}
		startActivity(rateIntent);
	}
	private void doClickVersion() {
		Uri uri = null;
		if(myApp.isAdult()) {
			uri = Uri.parse(AppConstants.MORE_GAMES_URL_ADULT);
		} else {
			uri = Uri.parse(AppConstants.MORE_GAMES_URL);
		}
		Intent intent = new Intent(Intent.ACTION_VIEW, uri);
		try {
			Context googlePlayContext = this.createPackageContext(GooglePlayServicesUtil.GOOGLE_PLAY_STORE_PACKAGE, Context.CONTEXT_INCLUDE_CODE+Context.CONTEXT_IGNORE_SECURITY);
			Class mAssetBrowserActivity = googlePlayContext.getClassLoader().loadClass(GooglePlayServicesUtil.GOOGLE_PLAY_STORE_PACKAGE + ".AssetBrowserActivity");
			intent.setClassName(googlePlayContext, mAssetBrowserActivity.getName());
		} catch (Exception e) {
            e.printStackTrace();
		}
		startActivity(intent);

	}
	private void showUpdateDialog() {
		AlertDialog.Builder updateDialog = new AlertDialog.Builder(this);
		updateDialog.setTitle(R.string.DIALOG_APKUPDATE_TITLE);
		updateDialog.setMessage(R.string.DIALOG_APKUPDATE_MSG);
		updateDialog.setPositiveButton(
				R.string.DIALOG_BUTTON_UPDATE,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if(AppPrefUtil.getApkNewVersionGoogleUri(SettingsActivity.this, null) != null 
								&& !AppPrefUtil.getApkNewVersionGoogleUri(SettingsActivity.this, null).equalsIgnoreCase("")) {
							Uri uri = Uri.parse(AppPrefUtil.getApkNewVersionGoogleUri(SettingsActivity.this, null));
							Intent intent = new Intent(Intent.ACTION_VIEW, uri);
							try {
								Context googlePlayContext = SettingsActivity.this.createPackageContext(GooglePlayServicesUtil.GOOGLE_PLAY_STORE_PACKAGE, Context.CONTEXT_INCLUDE_CODE+Context.CONTEXT_IGNORE_SECURITY);
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
		updateDialog.setNegativeButton(
				R.string.DIALOG_BUTTON_LATER,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
					}
				}
			);
		if(!this.isFinishing()) updateDialog.show();
	}
	private void startDownloadApk() {
		Intent apkDownloadIntent = new Intent(this, DownloadApkService.class);
		startService(apkDownloadIntent);
	}
	private void showClearCacheDialog() {
		AlertDialog.Builder clearDialog = new AlertDialog.Builder(this);
		clearDialog.setTitle(R.string.PREF_USERDATA_CLEAR_CACHE_TITLE);
		clearDialog.setMessage(R.string.PREF_USERDATA_CLEAR_CACHE_DIALOG_MSG);
		clearDialog.setPositiveButton(
				R.string.DIALOG_BUTTON_CLEAR,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						new ClearCacheTask().execute();
					}
				}
			);
		clearDialog.setNegativeButton(
				R.string.DIALOG_BUTTON_CANCEL,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
					}
				}
			);
		clearDialog.show();
	}
	private void showClearHistoryDialog() {
		AlertDialog.Builder clearDialog = new AlertDialog.Builder(this);
		clearDialog.setTitle(R.string.PREF_USERDATA_CLEAR_HISTORY_TITLE);
		clearDialog.setMessage(R.string.PREF_USERDATA_CLEAR_HISTORY_DIALOG_MSG);
		clearDialog.setPositiveButton(
				R.string.DIALOG_BUTTON_CLEAR,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						new ClearHistoryTask().execute();
					}
				}
			);
		clearDialog.setNegativeButton(
				R.string.DIALOG_BUTTON_CANCEL,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
					}
				}
			);
		clearDialog.show();
	}
	@Override
    public void onBackPressed() {
		Intent intent = new Intent();
		intent.setClass(this, ActMain.class);
		startActivity(intent);
		overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
		this.finish();
	}
	@Override
	public void onDestroy() {
		super.onDestroy();
		unregisterMyReceivers();
		System.gc();
	}
	private void unregisterMyReceivers() {
		try {
			if(this.downloadApkReceiver !=null) unregisterReceiver(this.downloadApkReceiver);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	class ClearHistoryTask extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... params) {
			try{
				SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(SettingsActivity.this);
				Editor editor = pref.edit();
				AppPrefUtil.removeLastPlayPackageCode(SettingsActivity.this, editor);
				editor.apply();
				DBHelperScore.getInstance(SettingsActivity.this).deleteAll(null);
			} catch(Exception e) {
				e.printStackTrace();
			}
			return null;
		}
		@Override
	    protected void onPostExecute(Void voida) {
			Toast.makeText(SettingsActivity.this, 
					SettingsActivity.this.getString(R.string.PREF_USERDATA_CLEAR_HISTORY_SUCCESSFUL_MSG)
					, Toast.LENGTH_SHORT).show();
		}
		@Override
        protected void onPreExecute() {
			clearHistoryPref.setEnabled(false);
		}
		
	}
	class ClearCacheTask extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... params) {
            if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) ||
                    !Environment.isExternalStorageRemovable()) {
	            AppTools.deleteFilesByDirectory(SettingsActivity.this.getExternalCacheDir());
	        }
            AppTools.deleteFilesByDirectory(SettingsActivity.this.getCacheDir());
			return null;
		}
		@Override
	    protected void onPostExecute(Void voida) {
			
		}
		@Override
        protected void onPreExecute() {
			clearCachePref.setEnabled(false);
		}
	}
}

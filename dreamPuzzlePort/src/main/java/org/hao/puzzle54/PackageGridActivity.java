package org.hao.puzzle54;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import org.hao.puzzle54.custom.CustomGridActivity;
import org.hao.puzzle54.services.MyAdsReceiver;
import org.hh.puzzle.port54.hall.R;

import java.lang.ref.WeakReference;

public class PackageGridActivity extends ActionBarActivity implements PackageGridActivityCallBack{
	private static final String TAG = PackageGridActivity.class.getName();
	private static final int HANDLER_MSG_AD_CHANGED = 101;
	private FragmentManager fragmentManager;
	private MyApp myApp;
	private AdView adView;
	private FrameLayout adLayout;
	private NewGameFrag fragNewGame;
    private PackagePicsFrag fragPackagePics;
	private MyAdsReceiver myAdsReceiver;
	private MyHandler mHandler;
	private String packageCode;
	private boolean blnActivityHasFocus;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate");
		setContentView(R.layout.package_grid_activity);
		this.myApp = (MyApp)getApplicationContext();
		adLayout = (FrameLayout)findViewById(R.id.ad_layout);
		adLayout.setMinimumHeight(myApp.getAdViewMeasureHeight());
		setBanner();
		this.mHandler = new MyHandler(this);
		myAdsReceiver = new MyAdsReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				PackageGridActivity.this.mHandler.sendMessage(PackageGridActivity.this.mHandler.obtainMessage(PackageGridActivity.HANDLER_MSG_AD_CHANGED));
			}
		};
		fragmentManager = getSupportFragmentManager();
		if(savedInstanceState == null) {
			if(this.getIntent().getExtras() != null && this.getIntent().getExtras().containsKey(MyBundleData.PACKAGE_CODE)) {
				this.packageCode = this.getIntent().getExtras().getString(MyBundleData.PACKAGE_CODE);
				Bundle bundle = new Bundle();
		        bundle.putString(MyBundleData.PACKAGE_CODE, packageCode);
				fragPackagePics = new PackagePicsFrag();
				fragPackagePics.setArguments(bundle);
				FragmentTransaction trans = fragmentManager.beginTransaction();
				trans.replace(R.id.package_grid_container, fragPackagePics).commitAllowingStateLoss();
			} else {
				fragNewGame = new NewGameFrag();
				FragmentTransaction trans = fragmentManager.beginTransaction();
				trans.replace(R.id.package_grid_container, fragNewGame).commitAllowingStateLoss();
			}
		} else {
			this.packageCode = savedInstanceState.getString(MyBundleData.PACKAGE_CODE);
			if(this.packageCode != null) {
				Bundle bundle = new Bundle();
		        bundle.putString(MyBundleData.PACKAGE_CODE, packageCode);
				fragPackagePics = new PackagePicsFrag();
				fragPackagePics.setArguments(bundle);
				FragmentTransaction trans = fragmentManager.beginTransaction();
				trans.replace(R.id.package_grid_container, fragPackagePics).commitAllowingStateLoss();
			} else {
				fragNewGame = new NewGameFrag();
				FragmentTransaction trans = fragmentManager.beginTransaction();
				trans.replace(R.id.package_grid_container, fragNewGame).commitAllowingStateLoss();
			}
		}
		
		IntentFilter filterAdChanged = new IntentFilter();
		filterAdChanged.addAction(this.getPackageName()+MyAdsReceiver.ACTION_AD_CHANGED);
		registerReceiver(myAdsReceiver, filterAdChanged);
	}
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		savedInstanceState.putString(MyBundleData.PACKAGE_CODE, this.packageCode);
        super.onSaveInstanceState(savedInstanceState);
        Log.d(TAG, "onSaveInstanceState");
    }
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		if(hasFocus && !this.blnActivityHasFocus) {
			this.blnActivityHasFocus = hasFocus;
		}
	}
	static class MyHandler extends Handler {
        WeakReference<PackageGridActivity> mActivity;
        MyHandler(PackageGridActivity activity) {
        	mActivity = new WeakReference<PackageGridActivity>(activity);
        }
        @Override
        public void handleMessage(Message msg) {
        	PackageGridActivity theActivity = mActivity.get();
        	if(theActivity == null) return;
        	switch (msg.what) {
			case PackageGridActivity.HANDLER_MSG_AD_CHANGED:
				theActivity.setBanner();
				break;
			default:
				break;
            }
        }
	}

	@Override
    public void onBackPressed() {
		if(fragNewGame == null) {
			fragNewGame = new NewGameFrag();
			FragmentTransaction trans = fragmentManager.beginTransaction();
			trans.setCustomAnimations(R.anim.fade_in, R.anim.fade_out);
			trans.replace(R.id.package_grid_container, fragNewGame);
			trans.commitAllowingStateLoss();
			this.packageCode = null;
		} else {
			if(this.fragmentManager.getBackStackEntryCount()<=0) {
				Intent intent = new Intent();
				intent.setClass(this, ActMain.class);
				startActivity(intent);
				overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
				finish();
			} else {
				this.packageCode = null;
				super.onBackPressed();
			}
		}
	}
	@Override
	public void onResume() {
		super.onResume();
		Log.d(TAG, "onResume");
		if (adView != null) adView.resume();

	}
	@Override
	public void onPause() {
		Log.d(TAG, "onPause");
		if(adView != null) adView.pause();
		super.onPause();
	}
	@Override
	public void onDestroy() {
		Log.d(TAG, "onDestroy");
		if(this.adView != null) this.adView.destroy();
		unregisterMyReceivers();
		super.onDestroy();
	}
	private void unregisterMyReceivers() {
		try {
			if(this.myAdsReceiver != null) unregisterReceiver(this.myAdsReceiver);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	@Override
	public void openMorePuzzlesFrag() {
        MorePuzzlesFrag fragMorePuzzles = new MorePuzzlesFrag();
		FragmentTransaction  trans = fragmentManager.beginTransaction();
		trans.setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out);
		trans.replace(R.id.package_grid_container, fragMorePuzzles);
		trans.addToBackStack(null);
		trans.commitAllowingStateLoss();
	}
	@Override
	public void openNewGameFrag() {
		fragmentManager.popBackStack();
	}
	@Override
	public void openPackagePicsFrag(String packageCode) {
		this.packageCode = packageCode;
		fragPackagePics = new PackagePicsFrag();
		Bundle bundle = new Bundle();
        bundle.putString(MyBundleData.PACKAGE_CODE, this.packageCode);
		fragPackagePics.setArguments(bundle);
		FragmentTransaction  trans = fragmentManager.beginTransaction();
		trans.setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out);
		trans.replace(R.id.package_grid_container, fragPackagePics);
		trans.addToBackStack(null);
		trans.commitAllowingStateLoss();
	}
	@Override
	public void openCustomPicsFrag() {
		Intent intent = new Intent();
		intent.setClass(this, CustomGridActivity.class);
		startActivity(intent);
		overridePendingTransition(R.anim.scale_in, R.anim.scale_out);
		finish();
	}
	@Override
	public void openPuzzlePreview(String packageCode, int picIndex) {
		Intent intent = new Intent();
		intent.setClass(this, PreviewPicActivity.class);
		Bundle bundle = new Bundle();
		bundle.putString(MyBundleData.PACKAGE_CODE, packageCode);
		bundle.putLong(MyBundleData.PICTURE_INDEX, picIndex);
		intent.putExtras(bundle);
		startActivity(intent);
		overridePendingTransition(R.anim.scale_in, R.anim.scale_out);
		finish();
	}
	@Override
	public MyApp getMyApp() {
		return this.myApp;
	}
	private void setBanner() {
		if(!this.myApp.hasAds()) {
			adLayout.setVisibility(View.GONE);
			return;
		}
		if(adView != null) adLayout.removeView(adView);
		try {
			adView = new AdView(this);
			adView.setAdSize(myApp.getAdSizeForAdmob());
			adView.setAdUnitId(AppPrefUtil.getAdBannerId(this, null));
			adView.setBackgroundResource(R.drawable.bg_ad_drawable);
			FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(myApp.getAdViewMeasureWidth(), myApp.getAdViewMeasureHeight());
			params.gravity = Gravity.CENTER;
			adLayout.addView(adView, params);
			AdRequest.Builder builder = new AdRequest.Builder();
			builder.addTestDevice("B3EEABB8EE11C2BE770B684D95219ECB");
			builder.addTestDevice("74EC3A4BB2BCC674BB93E275A8D63A4D"); //htc myTouch 4g
			adView.loadAd(builder.build());
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}

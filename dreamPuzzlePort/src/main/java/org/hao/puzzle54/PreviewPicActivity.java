package org.hao.puzzle54;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import org.hao.database.DBHelperCustom;
import org.hao.database.DBHelperScore;
import org.hao.puzzle54.custom.CustomGridActivity;
import org.hao.puzzle54.custom.CustomPicEntity;
import org.hh.puzzle.port54.hall.R;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipFile;

public class PreviewPicActivity extends ActionBarActivity implements PreviewPicActivityCallBack {
	private static final String TAG = PreviewPicActivity.class.getName();
    private long currentPicIndex;
	private MyApp myApp;
	private long allowedMaxPicIndex;
	private boolean isCustom;
	private AdView adView;
	private FrameLayout adLayout;
	private List<CustomPicEntity> listCustomEntity;
	private String packageCode;
	private ActionBar actionBar;
	private int totalPics;
	private static long DISK_CACHE_SIZE = 1024 * 1024 * 1; //10MB default
	private String strDifficulty;
	private FragmentManager fragmentManager;
	private RecordsFrag recordsFrag;
	private ProgressBar initProgressBar;
	private ViewPager mPager;
	private InitTask initTask;
	private boolean isInnerPics;

	@Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.preview_activity);
        Toolbar toolbar = (Toolbar)findViewById(R.id.preview_mytoolbar);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
		this.myApp = (MyApp)getApplicationContext();
        actionBar.setDisplayShowHomeEnabled(false);
		adLayout = (FrameLayout)findViewById(R.id.ad_layout);
		adLayout.setVisibility(View.INVISIBLE);
        strDifficulty = "["+AppPrefUtil.getRows(this, null)+"x"+AppPrefUtil.getCols(this, null)+"]";
		this.packageCode = this.getIntent().getExtras().getString(MyBundleData.PACKAGE_CODE);
		this.isInnerPics = this.myApp.isInnerPics(this.packageCode);
		this.currentPicIndex = this.getIntent().getExtras().getLong(MyBundleData.PICTURE_INDEX);
		if(AppConstants.CUSTOM_PACKAGE_CODE.equals(this.packageCode)) this.isCustom = true;
		fragmentManager = getSupportFragmentManager();
		if (savedInstanceState != null) {
			recordsFrag = (RecordsFrag)fragmentManager.findFragmentByTag(RecordsFrag.class.getName());
			if(recordsFrag != null) {
				fragmentManager.beginTransaction().remove(recordsFrag).commitAllowingStateLoss();
			}
		}
		mPager = (ViewPager) findViewById(R.id.preview_viewpager);
		mPager.setOnPageChangeListener(new MyPageChangeListener());
		initProgressBar = (ProgressBar)findViewById(R.id.preview_activity_progressbar);
		initTask = new InitTask();
		initTask.execute();

		/*
		if(this.isCustom) {
			this.listCustomEntity = DBHelperCustom.getInstance(this).getAll(null);
			allowedMaxPicIndex = this.listCustomEntity.size()-1;
		} else {
			allowedMaxPicIndex = this.myApp.getAllowedMaxPicIndex(this.packageCode);
		}
		if(isCustom) totalPics = listCustomEntity.size();
		else totalPics = myApp.getCurrentZipEntries(packageCode).size();
        PreviewPicFragAdapter mAdapter = new PreviewPicFragAdapter(fragmentManager, totalPics);
        mPager.setAdapter(mAdapter);
        mPager.setCurrentItem((int) this.currentPicIndex);
        if(this.currentPicIndex == 0) setMyActionBarContent();
        */
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.preview_menu, menu);
        return true;
	}
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		MenuItem menuItemWallpaper = menu.findItem(R.id.preview_menu_wallpaper);
		MenuItem menuItemShare = menu.findItem(R.id.preview_menu_share);
		if(this.currentPicIndex > this.allowedMaxPicIndex) {
			menuItemWallpaper.setVisible(false);
			menuItemShare.setVisible(false);
		} else {
			menuItemWallpaper.setVisible(true);
			menuItemShare.setVisible(true);
		}
		return true;
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem menu) {
		switch(menu.getItemId()) {
		case R.id.preview_menu_wallpaper:
			BufferedInputStream input = null;
			try {
				if(this.isCustom) {
					CustomPicEntity currentCustomEntity = DBHelperCustom.getInstance(myApp).getById(this.listCustomEntity.get((int)this.currentPicIndex).getId(), null);
					input = new BufferedInputStream(new FileInputStream(this.myApp.getCustomPicture(currentCustomEntity.getImageName())));
				} else {
					if(this.isInnerPics) {
						input = new BufferedInputStream(getAssets().open(AppConstants.PICS_FOLDER_IN_ZIP + File.separator + this.myApp.getInnerPics().get((int)this.currentPicIndex)));
					} else {
						input = new BufferedInputStream(this.myApp.getPackageZipFile(packageCode).getInputStream(this.myApp.getCurrentZipEntries(this.packageCode).get((int)this.currentPicIndex)));
					}
				}
				this.myApp.setWallpaper(input);
				Toast.makeText(this, R.string.FINISHED_WALLPAPER_SUCCESSFULLY, Toast.LENGTH_SHORT).show();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if(input != null) {
					try {
						input.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			break;
		case R.id.preview_menu_allrecords:
			if(recordsFrag != null && recordsFrag.isResumed()) return true;
			long picId = this.currentPicIndex;
	        if(this.isCustom) {
	        	picId = this.listCustomEntity.get((int)this.currentPicIndex).getId();
	        }
			FragmentTransaction trans = this.fragmentManager.beginTransaction();
			recordsFrag = new RecordsFrag();
			Bundle bundle = new Bundle();
	        bundle.putString(MyBundleData.PACKAGE_CODE, this.packageCode);
	        bundle.putLong(MyBundleData.PICTURE_INDEX, picId);
	        recordsFrag.setArguments(bundle);
	        trans.setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out);
			trans.add(R.id.records_container, recordsFrag, RecordsFrag.class.getName());
			trans.addToBackStack(null);
			trans.commitAllowingStateLoss();
			break;
		case R.id.preview_menu_share:
			Intent intent = null;
			if(this.isCustom) intent = this.myApp.saveSharePicture(this.listCustomEntity.get((int)this.currentPicIndex).getId(), this.packageCode);
			else intent = this.myApp.saveSharePicture(this.currentPicIndex, this.packageCode);
			if(intent != null) startActivity(Intent.createChooser(intent, getString(R.string.app_name)));
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(menu);
	}
	@Override
	public void openPuzzleActivity() {
		if(this.currentPicIndex > this.allowedMaxPicIndex) return;
		Intent intent = new Intent();
		intent.setClass(this, PuzzleActivity.class);
		Bundle bundle = new Bundle();
		bundle.putString(MyBundleData.PACKAGE_CODE, this.packageCode);
		bundle.putLong(MyBundleData.PICTURE_INDEX, this.currentPicIndex);
		bundle.putBoolean(MyBundleData.PUZZLE_IS_CONTINUE, false);
		intent.putExtras(bundle);
		startActivity(intent);
		overridePendingTransition(R.anim.push_bottom_in, R.anim.push_bottom_out);
		finish();
	}
	@Override
	public MyApp getMyApp() {
		return this.myApp;
	}
	@Override
	public List<CustomPicEntity> getCustomEntityList() {
		return this.listCustomEntity;
	}
	@Override
	public long getAllowedMaxPicIndex() {
		return this.allowedMaxPicIndex;
	}
	@Override
	public boolean isInnerPics() {
		return this.isInnerPics;
	}
	@Override
    public void onBackPressed() {
		if(this.recordsFrag!=null && this.recordsFrag.isResumed()) super.onBackPressed();
		else {
			Intent intent = new Intent();
			if(this.isCustom) {
				intent.setClass(this, CustomGridActivity.class);
			} else {
				Bundle bundle = new Bundle();
				bundle.putString(MyBundleData.PACKAGE_CODE, this.packageCode);
				intent.setClass(this, PackageGridActivity.class);
				intent.putExtras(bundle);
			}
			startActivity(intent);
			overridePendingTransition(R.anim.scale_in, R.anim.scale_out);
			finish();
		}
	}
	@Override
	public void onResume() {
		super.onResume();
		if (adView != null) adView.resume();

	}
	@Override
	public void onPause() {
		if(adView != null) adView.pause();
		super.onPause();
	}
	@Override
	public void onDestroy() {
		if(this.initTask != null) this.initTask.cancel(true);
		if(this.adView != null) this.adView.destroy();
		super.onDestroy();
	}
	public void dismissRecordsFragment(View v) {
		if(this.recordsFrag != null && this.recordsFrag.isResumed()) this.fragmentManager.popBackStack();
	}
	private void setMyActionBarContent() {
		supportInvalidateOptionsMenu();
		actionBar.setSubtitle(strDifficulty+" "+(PreviewPicActivity.this.currentPicIndex+1)+"/"+PreviewPicActivity.this.totalPics);
    	if(PreviewPicActivity.this.currentPicIndex <= PreviewPicActivity.this.allowedMaxPicIndex) {
    		PreviewPicActivity.this.adLayout.setVisibility(View.INVISIBLE);
			ScoreEntity scoreEntity = null;
			if(PreviewPicActivity.this.isCustom) {
				CustomPicEntity customPicEntity = PreviewPicActivity.this.listCustomEntity.get((int)PreviewPicActivity.this.currentPicIndex);
				scoreEntity = DBHelperScore.getInstance(PreviewPicActivity.this.myApp)
						.getScore(PreviewPicActivity.this.packageCode, customPicEntity.getId(), 
								AppPrefUtil.getRows(PreviewPicActivity.this, null), AppPrefUtil.getCols(PreviewPicActivity.this, null), null);
			} else {
				scoreEntity = DBHelperScore.getInstance(PreviewPicActivity.this.myApp)
				.getScore(PreviewPicActivity.this.packageCode, PreviewPicActivity.this.currentPicIndex, 
						AppPrefUtil.getRows(PreviewPicActivity.this, null), AppPrefUtil.getCols(PreviewPicActivity.this, null), null);
				
			}
			if(scoreEntity != null && scoreEntity.getBestTime()>0) {
				actionBar.setTitle(getString(R.string.preview_record_label)+AppTools.formatElapsedTimeSec2String(scoreEntity.getBestTime())); 
			} else {
				actionBar.setTitle(R.string.preview_norecord_label);
			}
    	} else {
    		PreviewPicActivity.this.adLayout.setVisibility(View.VISIBLE);
    		if(PreviewPicActivity.this.adView == null) {
    			setBanner();
    		}
    		actionBar.setTitle(R.string.preview_norecord_label);
    	}
	}
	private void setBanner() {
		if(!this.myApp.hasAds()) {
			adLayout.setVisibility(View.GONE);
			return;
		}
		this.adView = new AdView(this);
		this.adView.setAdSize(myApp.getAdSizeForAdmob());
		this.adView.setAdUnitId(AppPrefUtil.getAdBannerId(this, null));
//		adView.setBackgroundResource(R.drawable.bg_ad_bmp);
		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(myApp.getAdViewMeasureWidth(), myApp.getAdViewMeasureHeight());
		params.gravity = Gravity.CENTER;
		adLayout.addView(adView, params);
		this.adView.loadAd(new AdRequest.Builder().build());
	}
	private class MyPageChangeListener implements OnPageChangeListener {
		@Override
		public void onPageScrollStateChanged(int arg0) {
		}
		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2) {
		}
		@Override
		public void onPageSelected(int arg0) {
			PreviewPicActivity.this.currentPicIndex = arg0;
			setMyActionBarContent();
		}
	}
	class InitTask extends AsyncTask<Void, Void, ZipFile> {
		@Override
		protected void onPreExecute() {
			initProgressBar.bringToFront();
			initProgressBar.setVisibility(View.VISIBLE);
		}
		@Override
		protected ZipFile doInBackground(Void... params) {
			ZipFile zipFile = null;
			if(PreviewPicActivity.this.isCustom) {
				PreviewPicActivity.this.listCustomEntity = DBHelperCustom.getInstance(PreviewPicActivity.this).getAll(null);
				PreviewPicActivity.this.allowedMaxPicIndex = PreviewPicActivity.this.listCustomEntity.size()-1;
			} else {
				PreviewPicActivity.this.allowedMaxPicIndex = PreviewPicActivity.this.myApp.getAllowedMaxPicIndex(PreviewPicActivity.this.packageCode);
				if(!PreviewPicActivity.this.isInnerPics) zipFile= myApp.getPackageZipFile(PreviewPicActivity.this.packageCode);
			}
			if(PreviewPicActivity.this.isCustom) PreviewPicActivity.this.totalPics = PreviewPicActivity.this.listCustomEntity.size();
			else {
				if(PreviewPicActivity.this.isInnerPics) {
					PreviewPicActivity.this.totalPics = myApp.getInnerPics().size();
				} else {
					PreviewPicActivity.this.totalPics = PreviewPicActivity.this.myApp.getCurrentZipEntries(PreviewPicActivity.this.packageCode).size();
				}
			}
			return zipFile;
		}
		@Override
		protected void onPostExecute(ZipFile zipFile) {
			initProgressBar.setVisibility(View.GONE);
			PreviewPicFragAdapter mAdapter = new PreviewPicFragAdapter(fragmentManager, totalPics, PreviewPicActivity.this.packageCode, zipFile, PreviewPicActivity.this.isInnerPics, myApp);
			PreviewPicActivity.this.mPager.setAdapter(mAdapter);
			PreviewPicActivity.this.mPager.setCurrentItem((int) PreviewPicActivity.this.currentPicIndex);
			if(PreviewPicActivity.this.currentPicIndex == 0) setMyActionBarContent();
		}
	}
}

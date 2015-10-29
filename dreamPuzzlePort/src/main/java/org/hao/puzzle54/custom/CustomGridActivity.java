package org.hao.puzzle54.custom;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import org.hao.puzzle54.AppPrefUtil;
import org.hao.puzzle54.MyApp;
import org.hao.puzzle54.MyBundleData;
import org.hao.puzzle54.PackageGridActivity;
import org.hao.puzzle54.PreviewPicActivity;
import org.hh.puzzle.port54.hall.R;

import java.lang.ref.WeakReference;

public class CustomGridActivity extends ActionBarActivity implements CustomGridActivityCallBack{
	private static final String TAG = CustomGridActivity.class.getName();
	protected static final int MENU_STYLE_CUSTOM_FOLDERS = 103;
	protected static final int MENU_STYLE_CUSTOM_EDIT = 105;
	protected static final int CUSTOM_EDIT_REQUEST_CODE_CAMERA = 201;
	protected static final int CUSTOM_EDIT_REQUEST_CODE_ALBUM = 202;
	private int intMenuStyle = R.id.custom_menu_group_standard;
	private FragmentManager fragmentManager;
	private CustomPicsFrag fragCustomPics;
	private CustomFoldersFrag fragCustomFolders;
	private CustomSelectedFolderFrag fragCustomSelectedFolder;
	private CustomEditFrag fragCustomEdit;
	private MyApp myApp;
	private AdView adView;
	private FrameLayout adLayout;
	private ActionBar actionBar;
	private String strSelectedTitle;
	private MyHandler mHandler;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.custom_activity);
        Toolbar toolbar = (Toolbar)findViewById(R.id.custom_mytoolbar);
        setSupportActionBar(toolbar);
		actionBar = getSupportActionBar();
		actionBar.show();
		setCustomSelectedTitle(0);
		this.myApp = (MyApp)getApplicationContext();
		adLayout = (FrameLayout)findViewById(R.id.ad_layout);
		adLayout.setMinimumHeight(myApp.getAdViewMeasureHeight());
		setBanner();
		this.mHandler = new MyHandler(this);
		fragmentManager = getSupportFragmentManager();
		openCustomPics();
	}
	static class MyHandler extends Handler {
        WeakReference<CustomGridActivity> mActivity;
        MyHandler(CustomGridActivity activity) {
        	mActivity = new WeakReference<CustomGridActivity>(activity);
        }
        @Override
        public void handleMessage(Message msg) {
        	CustomGridActivity theActivity = mActivity.get();
        	if(theActivity == null) return;
	        theActivity.intMenuStyle = msg.what;
	        theActivity.supportInvalidateOptionsMenu();
        }
	}
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		menu.setGroupVisible(R.id.custom_menu_group_standard, false);
		menu.setGroupVisible(R.id.custom_menu_group_delete_select_all, false);
		menu.setGroupVisible(R.id.custom_menu_group_delete_deselect_all,false);
		menu.setGroupVisible(R.id.custom_menu_group_save,false);
		menu.setGroupVisible(R.id.custom_menu_group_add_select_all,false);
		menu.setGroupVisible(R.id.custom_menu_group_add_deselect_all,false);
		switch(this.intMenuStyle) {
			case R.id.custom_menu_group_standard:
				menu.setGroupVisible(R.id.custom_menu_group_standard,true);
				actionBar.setDisplayShowTitleEnabled(false);
				actionBar.setDisplayHomeAsUpEnabled(true);
				break;
			case R.id.custom_menu_group_delete_select_all:
				menu.setGroupVisible(R.id.custom_menu_group_delete_select_all, true);
				actionBar.setDisplayHomeAsUpEnabled(true);
				actionBar.setDisplayShowTitleEnabled(true);
				actionBar.setTitle(this.strSelectedTitle);
				break;
			case R.id.custom_menu_group_delete_deselect_all:
				menu.setGroupVisible(R.id.custom_menu_group_delete_deselect_all, true);
				actionBar.setDisplayHomeAsUpEnabled(true);
				actionBar.setDisplayShowTitleEnabled(true);
				actionBar.setTitle(this.strSelectedTitle);
				break;
			case CustomGridActivity.MENU_STYLE_CUSTOM_FOLDERS:
				menu.setGroupVisible(R.id.custom_menu_group_save,true);
				actionBar.setDisplayShowTitleEnabled(true);
				actionBar.setTitle(this.strSelectedTitle);
				actionBar.setDisplayHomeAsUpEnabled(true);
				break;
			case R.id.custom_menu_group_add_select_all:
				menu.setGroupVisible(R.id.custom_menu_group_add_select_all,true);
				actionBar.setDisplayHomeAsUpEnabled(true);
				actionBar.setDisplayShowTitleEnabled(true);
				actionBar.setTitle(this.strSelectedTitle);
				break;
			case R.id.custom_menu_group_add_deselect_all:
				menu.setGroupVisible(R.id.custom_menu_group_add_deselect_all,true);
				actionBar.setDisplayHomeAsUpEnabled(true);
				actionBar.setDisplayShowTitleEnabled(true);
				actionBar.setTitle(this.strSelectedTitle);
				break;
			case CustomGridActivity.MENU_STYLE_CUSTOM_EDIT:
				menu.setGroupVisible(R.id.custom_menu_group_save,true);
				actionBar.setDisplayShowTitleEnabled(false);
				actionBar.setDisplayHomeAsUpEnabled(true);
				break;
			default:
				break;
		}
		return true;
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.custom_menu, menu);
        return true;
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem menu) {
		switch(menu.getItemId()) {
			case android.R.id.home:
				if(R.id.custom_menu_group_standard == this.intMenuStyle) {
					Intent intent = new Intent();
					intent.setClass(this, PackageGridActivity.class);
					startActivity(intent);
					overridePendingTransition(R.anim.scale_in, R.anim.scale_out);
					finish();
				} else {
					this.fragmentManager.popBackStack();
				}
				break;
			case R.id.actionbar_delete_select_all_excute:
				if(fragCustomPics != null && fragCustomPics.isResumed()) fragCustomPics.showDeleteDialog();
				break;
			case R.id.actionbar_delete_deselect_all_excute:
				if(fragCustomPics != null && fragCustomPics.isResumed()) fragCustomPics.showDeleteDialog();
				break;
			case R.id.actionbar_add_batch:
				openCustomFolders();
				break;
			case R.id.actionbar_add_one:
				openCustomEditPic(CustomGridActivity.CUSTOM_EDIT_REQUEST_CODE_ALBUM);
				break;
			case R.id.actionbar_add_camera:
				openCustomEditPic(CustomGridActivity.CUSTOM_EDIT_REQUEST_CODE_CAMERA);
				break;
			case R.id.actionbar_save:
				if(this.intMenuStyle == CustomGridActivity.MENU_STYLE_CUSTOM_FOLDERS
					&& fragCustomFolders != null && fragCustomFolders.isResumed()) {
					fragCustomFolders.importSelectedPics();
				} else if(this.intMenuStyle == CustomGridActivity.MENU_STYLE_CUSTOM_EDIT
						&& fragCustomEdit != null && fragCustomEdit.isResumed()) {
					fragCustomEdit.savePicture();
				}
				break;
			case R.id.actionbar_delete_select_all:
				if(fragCustomPics != null && fragCustomPics.isResumed()) {
					fragCustomPics.selectAll(true);
				}
				break;
			case R.id.actionbar_delete_deselect_all:
				if(fragCustomPics != null && fragCustomPics.isResumed()) {
					fragCustomPics.selectAll(false);
				}
				break;
			case R.id.actionbar_add_select_all:
				if(fragCustomSelectedFolder != null && fragCustomSelectedFolder.isResumed()) {
					fragCustomSelectedFolder.selectAll(true);
				}
				break;
			case R.id.actionbar_add_deselect_all:
				if(fragCustomSelectedFolder != null && fragCustomSelectedFolder.isResumed()) {
					fragCustomSelectedFolder.selectAll(false);
				}
				break;
			default:
				break;
		}
		return super.onOptionsItemSelected(menu);
	}
	@Override
    public void onBackPressed() {
		switch(this.intMenuStyle) {
			case R.id.custom_menu_group_standard:
				Intent intent = new Intent();
				intent.setClass(this, PackageGridActivity.class);
				startActivity(intent);
				overridePendingTransition(R.anim.scale_in, R.anim.scale_out);
				finish();
				break;
			case R.id.custom_menu_group_delete_select_all:
				if(fragCustomPics != null && fragCustomPics.isResumed()) {
					fragCustomPics.set2StandardMode();
				}
				break;
			case R.id.custom_menu_group_delete_deselect_all:
				if(fragCustomPics != null && fragCustomPics.isResumed()) {
					fragCustomPics.set2StandardMode();
				}
				break;
			default:
				super.onBackPressed();
				break;
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
		if(this.adView != null) this.adView.destroy();
		super.onDestroy();
	}
	@Override
	public void openCustomPics() {
		fragCustomPics = new CustomPicsFrag();
		FragmentTransaction trans = fragmentManager.beginTransaction();
		trans.replace(R.id.fragment_container, fragCustomPics);
		trans.commitAllowingStateLoss();
	}
	@Override
	public void openCustomFolders() {
		fragCustomFolders = new CustomFoldersFrag();
		FragmentTransaction trans = fragmentManager.beginTransaction();
		trans.setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out);
		trans.replace(R.id.fragment_container, fragCustomFolders);
		trans.addToBackStack(null);
		trans.commitAllowingStateLoss();
	}
	@Override
	public void openCustomSelectedFolder(final String selectedFolderPath) {
		fragCustomSelectedFolder = new CustomSelectedFolderFrag();
		Bundle bundle = new Bundle();
        bundle.putString(MyBundleData.SELECTED_FOLDER, selectedFolderPath);
        fragCustomSelectedFolder.setArguments(bundle);
		FragmentTransaction trans = fragmentManager.beginTransaction();
		trans.setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out);
		trans.replace(R.id.fragment_container, fragCustomSelectedFolder);
		trans.addToBackStack(null);
		trans.commitAllowingStateLoss();
	}
	@Override
	public void openCustomPreview(final String packageCode, final int picIndex) {
		Intent intent = new Intent();
		intent.setClass(this, PreviewPicActivity.class);
		Bundle bundle = new Bundle();
//		BundleDataPuzzle dataPuzzle = new BundleDataPuzzle();
//		dataPuzzle.setPicIndex(picIndex);
//		dataPuzzle.setPackageCode(packageCode);
//		bundle.putSerializable(BundleDataPuzzle.class.getName(), dataPuzzle);
		bundle.putString(MyBundleData.PACKAGE_CODE, packageCode);
		bundle.putLong(MyBundleData.PICTURE_INDEX, picIndex);
		intent.putExtras(bundle);
		startActivity(intent);
		overridePendingTransition(R.anim.scale_in, R.anim.scale_out);
		finish();
	}
	public void openCustomEditPic(final int requestCode) {
		fragCustomEdit = new CustomEditFrag();
		Bundle bundle = new Bundle();
		bundle.putInt(MyBundleData.CUSTOM_EDIT_REQUEST_CODE, requestCode);
		fragCustomEdit.setArguments(bundle);
		FragmentTransaction trans = fragmentManager.beginTransaction();
		trans.replace(R.id.fragment_container, fragCustomEdit);
		trans.addToBackStack(null);
		trans.commitAllowingStateLoss();
	}
	@Override
	public void refreshActivity(int handlerMsg) {
		this.mHandler.sendMessage(this.mHandler.obtainMessage(handlerMsg));
	}
	@Override
	public int getStatusBarHeight() {
		Rect frame = new Rect();
		getWindow().getDecorView().getWindowVisibleDisplayFrame(frame);
		return frame.top;
	}
	@Override
	public MyApp getMyApp() {
		return this.myApp;
	}
	protected void setCustomSelectedTitle(int selectedNum) {
		this.strSelectedTitle = selectedNum + " " +this.getString(R.string.CUSTOM_SELECTED_TITLE);
	}
	private void setBanner() {
		if(!this.myApp.hasAds()) {
			adLayout.setVisibility(View.GONE);
			return;
		}
		if(adView != null) adLayout.removeView(adView);
		try {
			this.adView = new AdView(this);
			this.adView.setAdSize(myApp.getAdSizeForAdmob());
			this.adView.setAdUnitId(AppPrefUtil.getAdBannerId(this, null));
			this.adView.setBackgroundResource(R.drawable.bg_ad_drawable);
			FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(myApp.getAdViewMeasureWidth(), myApp.getAdViewMeasureHeight());
			params.gravity = Gravity.CENTER;
			adLayout.addView(adView, params);
			AdRequest.Builder builder = new AdRequest.Builder();
			adView.loadAd(builder.build());
//  		if(this.myApp.hasAds()) {
//	    		this.adView.loadAd(new AdRequest.Builder().build());
//		    }
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

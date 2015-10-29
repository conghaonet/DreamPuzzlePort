package org.hao.puzzle54;


import android.content.Context;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import java.util.zip.ZipFile;

public class PreviewPicFragAdapter extends FragmentStatePagerAdapter {
	private int count;
	private String packageCode;
	private ZipFile zipFile;
	private boolean isInnerPics;
	private AssetManager mAssetManager;
	public PreviewPicFragAdapter(FragmentManager fm, int count, String packageCode, ZipFile zipFile, boolean isInnerPics, Context context) {
		super(fm);
		this.count = count;
		this.packageCode = packageCode;
		this.zipFile = zipFile;
		this.isInnerPics = isInnerPics;
		if(this.isInnerPics) {
			mAssetManager = context.getAssets();
		}
	}
	@Override
	public Fragment getItem(int arg0) {
		PreviewPicFrag frag = new PreviewPicFrag();
		frag.setZipFile(this.zipFile);
		frag.isInnerPics(this.isInnerPics);
		frag.setAssetManager(this.mAssetManager);
        Bundle mBundle = new Bundle();
        mBundle.putInt(MyBundleData.PICTURE_INDEX, arg0);
		mBundle.putString(MyBundleData.PACKAGE_CODE, this.packageCode);
        frag.setArguments(mBundle);
		return frag;
	}
	@Override
	public int getCount() {
		return this.count;
	}

}

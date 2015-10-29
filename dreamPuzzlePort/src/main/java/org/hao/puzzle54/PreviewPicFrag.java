package org.hao.puzzle54;

import android.app.Activity;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import org.hh.puzzle.port54.hall.R;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class PreviewPicFrag extends Fragment {
	private static final String TAG = PreviewPicFrag.class.getName();
	private PreviewPicActivityCallBack mCallBack;
	private int mNum;
	private GestureDetector mGesture;
	private BitmapWorkerTask bitmapTask;
	private ProgressBar progressBar;
	private String packageCode;
	private boolean isCustom;
	private ZipFile zipFile;
	private boolean isInnerPics;
	private AssetManager mAssetManager;

	/**
	 * 当该Fragment被添加,显示到Activity时调用该方法
	 * 在此判断显示到的Activity是否已经实现了接口
	 */
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		Log.d(TAG, TAG+"--onAttach");
		if (!(activity instanceof PreviewPicActivityCallBack)) {
			throw new IllegalStateException(PreviewPicFrag.class.getName()+" 所在的Activity必须实现接口："+PreviewPicActivityCallBack.class.getName());
		}
		mCallBack = (PreviewPicActivityCallBack)activity;
	}
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, TAG+"--onCreate");
        this.mNum = getArguments() != null ? getArguments().getInt(MyBundleData.PICTURE_INDEX) : 0;
		this.packageCode = getArguments().getString(MyBundleData.PACKAGE_CODE);
		if(packageCode.equals(AppConstants.CUSTOM_PACKAGE_CODE)) this.isCustom = true;
		else this.isCustom = false;
    }
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.d(TAG, TAG + "--onCreateView");
        View v = inflater.inflate(R.layout.preview_fragment, container, false);
        ImageView imageView = (ImageView)v.findViewById(R.id.preview_fragment_img);
        ImageView imgLock = (ImageView)v.findViewById(R.id.preview_fragment_lock);
        progressBar = (ProgressBar)v.findViewById(R.id.preview_fragment_progressbar);
        imageView.setLongClickable(true);
        if(mNum <= this.mCallBack.getAllowedMaxPicIndex()) {
        	imgLock.setVisibility(View.GONE);
        	mGesture = new GestureDetector(mCallBack.getMyApp(), new MyGestureListener(true));
        } else {
        	imgLock.setVisibility(View.VISIBLE);
        	mGesture = new GestureDetector(mCallBack.getMyApp(), new MyGestureListener(false));
        }
        imageView.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return mGesture.onTouchEvent(event);
			}
		});
        bitmapTask = new BitmapWorkerTask();
        bitmapTask.execute(imageView);
        return v;
	}
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		Log.d(TAG, TAG+"--onActivityCreated");
	}
	@Override
	public void onStart() {
		super.onStart();
		Log.d(TAG, TAG+"--onStart");
	}
	@Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, TAG+"--onResume");
    }
	@Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, TAG+"--onPause");
    }
	@Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, TAG+"--onStop");
    }
	@Override
	public void onDestroyView() {
		if(this.bitmapTask != null) this.bitmapTask.cancel(true);
		Log.d(TAG, TAG+"--onDestroyView");
		super.onDestroyView();
	}
	@Override
	public void onDestroy() {
		Log.d(TAG, TAG + "--onDestroy");
		super.onDestroy();
	}
	/**
	 * 当该FragmentA从它所属的Activity中被删除时调用该方法
	 */
	@Override
	public void onDetach() {
		super.onDetach();
		Log.d(TAG, TAG+"--onDetach");
		if(this.bitmapTask != null) bitmapTask.cancel(true);
	}
	public void setZipFile(ZipFile zipFile) {
		this.zipFile = zipFile;
	}
	public void isInnerPics(boolean isInnerPics) {
		this.isInnerPics = isInnerPics;
	}
	public void setAssetManager(AssetManager mAssetManager) {
		this.mAssetManager = mAssetManager;
	}
	class MyGestureListener extends SimpleOnGestureListener {
    	private boolean isClickable;
    	public MyGestureListener(boolean isClickable) {
    		this.isClickable = isClickable;
    	}
    	@Override
    	public boolean onSingleTapUp(MotionEvent e) {
    		return super.onSingleTapUp(e);
    	}
    	@Override
    	public boolean onSingleTapConfirmed(MotionEvent e) {
    		if(this.isClickable) {
    			mCallBack.openPuzzleActivity();
    		}
			return super.onSingleTapConfirmed(e);        		
    	}
    	@Override  
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)  {
    		Log.d(TAG, "velocityX="+velocityX+" velocityY="+velocityY);
    		return super.onFling(e1, e2, velocityX, velocityY);
    	}
    }
	
	class BitmapWorkerTask extends AsyncTask<ImageView, Void, Bitmap> {
    	WeakReference<ImageView> weakImageView;
		@Override
		protected Bitmap doInBackground(ImageView... params) {
			Bitmap bitmap = null;
			Log.d(TAG, "BitmapWorkerTask.doInBackground() mNum="+PreviewPicFrag.this.mNum);
			this.weakImageView = new WeakReference<ImageView>(params[0]);
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
	        options.inPreferredConfig = Bitmap.Config.RGB_565;
	        if(PreviewPicFrag.this.mNum > PreviewPicFrag.this.mCallBack.getAllowedMaxPicIndex()) {
	        	options.inSampleSize = 16;
	        	options.inJustDecodeBounds = false;
		        BufferedInputStream input = getPicInputStream();
	        	if(input != null) {
		        	bitmap = BitmapFactory.decodeStream(input, null, options);
			        try {
						input.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
	        	}
	        } else {
	        	options.inJustDecodeBounds = true;
		        BufferedInputStream input = getPicInputStream();
	        	if(input != null) {
		        	BitmapFactory.decodeStream(input, null, options);
			        options.inSampleSize = options.outWidth / mCallBack.getMyApp().getDisplay().widthPixels;
			        try {
						input.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
	        	}
	        	options.inJustDecodeBounds =false;
	        	input = getPicInputStream();
		        if(input != null) {
		        	bitmap = BitmapFactory.decodeStream(input, null, options);
			        try {
						input.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
	        	}
	        }
	        if(bitmap != null) {
	        	if(bitmap.getWidth() > mCallBack.getMyApp().getDisplay().widthPixels) {
	        		int scaleHeight = (int)(bitmap.getHeight() * ((float)mCallBack.getMyApp().getDisplay().widthPixels/(float)bitmap.getWidth()));
	        		Bitmap tempBmp = ThumbnailUtils.extractThumbnail(bitmap, mCallBack.getMyApp().getDisplay().widthPixels, scaleHeight, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
	        		bitmap = tempBmp;
	        		tempBmp = null;
	        	}
	        }
			return bitmap;
		}
		@Override
		protected void onPostExecute(Bitmap bitmap) {
			if(weakImageView == null) return;
			ImageView imageView = weakImageView.get();
			if(imageView == null) {
				if(bitmap != null && !bitmap.isRecycled()) {
					bitmap.recycle();
				}
			} else {
				if(bitmap != null && !bitmap.isRecycled()) {
					progressBar.setVisibility(View.GONE);
					imageView.setImageBitmap(bitmap);
				}
			}
		}
		public BufferedInputStream getPicInputStream() {
			BufferedInputStream bis = null;
			if(PreviewPicFrag.this.isCustom) {
				String picFile = mCallBack.getMyApp().getCustomPicture(mCallBack.getCustomEntityList().get(PreviewPicFrag.this.mNum).getImageName()).getPath();
				try {
					bis = new BufferedInputStream(new FileInputStream(picFile));
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			} else {
				if(isInnerPics) {
					try {
						bis = new BufferedInputStream(mAssetManager.open(
								AppConstants.PICS_FOLDER_IN_ZIP+ File.separator
										+mCallBack.getMyApp().getInnerPics().get(PreviewPicFrag.this.mNum)));
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else {
					ZipEntry zipEntry = mCallBack.getMyApp().getCurrentZipEntries(PreviewPicFrag.this.packageCode).get(PreviewPicFrag.this.mNum);
					try {
						bis = new BufferedInputStream(zipFile.getInputStream(zipEntry));
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			return bis;
		}
	}
}

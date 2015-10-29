package org.hao.puzzle54.custom;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.flurry.android.FlurryAgent;

import org.hao.database.DBHelperCustom;
import org.hao.puzzle54.AppConstants;
import org.hao.puzzle54.AppTools;
import org.hao.puzzle54.MyBundleData;
import org.hao.puzzle54.myview.DragImageView;
import org.hh.puzzle.port54.hall.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
	
public class CustomEditFrag extends Fragment {
	private static final String TAG = CustomEditFrag.class.getName();
	private static final float RECT_DEFAULT_RATIO = 0.8f;
	private ImageView previewImageView;
	private int viableCurrentPicHeight;
	private int viableCurrentPicWidth;
	private int intMyRequestCode;
	private Date picMiddleNameDate;
	private File srcImageFile;
	private int srcImageRotate;
	private boolean isDeleteSrcImageFile;
	private ProgressBar progressBar;
	private ResizeImageTask resizeImageTask;
	private LoadImageTask loadImageTask;
	private CustomGridActivityCallBack mCallBack;
	
	private DragImageView myRectView;
	private int maxRectWidth;
	private int maxRectHeight;
	private int intScrImageFileWidth;
	private int intScrImageFileHeight;
	private float ratioSrcImageWidthHeight;
	private int previewBitmapWidth;
	private int previewBitmapHeight;
	private float ratioCurrentWidthHeight;
	private Bitmap previewBitmap;
	private Bitmap rectEmptyFrameBitmap;
	private FrameLayout editLayout;

	/**
	 * 当该Fragment被添加,显示到Activity时调用该方法
	 * 在此判断显示到的Activity是否已经实现了接口
	 */
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		Log.d(TAG, TAG+"--onAttach");
		if (!(activity instanceof CustomGridActivityCallBack)) {
			throw new IllegalStateException("CustomEditFrag所在的Activity必须实现CustomGridActivityCallBack接口");
		}
		mCallBack = (CustomGridActivityCallBack)activity;
	}
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, TAG+"--onCreate");
    }
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.d(TAG, TAG+"--onCreateView");
		View rootView = inflater.inflate(R.layout.custom_edit_fragment, container, false);
		this.intMyRequestCode = 0;
		this.isDeleteSrcImageFile = false;
		if(getArguments().containsKey(MyBundleData.CUSTOM_EDIT_REQUEST_CODE)) {
			this.intMyRequestCode = this.getArguments().getInt(MyBundleData.CUSTOM_EDIT_REQUEST_CODE);
			picMiddleNameDate = Calendar.getInstance().getTime();
			this.editLayout = (FrameLayout)rootView.findViewById(R.id.custom_edit_layout);
			this.previewImageView = (ImageView)rootView.findViewById(R.id.custom_edit_preview_imageview);
			this.myRectView = (DragImageView)rootView.findViewById(R.id.custom_edit_rect_imageview);
			progressBar = (ProgressBar)rootView.findViewById(R.id.progressbar);
			progressBar.setVisibility(View.GONE);
		}
		return rootView;
	}
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		Log.d(TAG, TAG+"--onActivityCreated");
		this.viableCurrentPicHeight = mCallBack.getMyApp().getAvailablePuzzleHeight() - mCallBack.getStatusBarHeight();
		this.viableCurrentPicWidth = this.mCallBack.getMyApp().getDisplay().widthPixels;
		this.ratioCurrentWidthHeight = (float)this.viableCurrentPicWidth / (float)this.viableCurrentPicHeight;
		mCallBack.refreshActivity(CustomGridActivity.MENU_STYLE_CUSTOM_EDIT);
		openActivity();
	}
	@Override
	public void onStart() {
		super.onStart();
		Log.d(TAG, TAG+"--onStart");
		FlurryAgent.onStartSession(this.getActivity());
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
        FlurryAgent.onEndSession(this.getActivity());
    }
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		Log.d(TAG, TAG+"--onDestroyView");
		if(CustomEditFrag.this.previewBitmap != null && !CustomEditFrag.this.previewBitmap.isRecycled()) {
			this.previewImageView = null;
			CustomEditFrag.this.previewBitmap.recycle();
			CustomEditFrag.this.previewBitmap = null;
		}
		if(CustomEditFrag.this.rectEmptyFrameBitmap != null && !CustomEditFrag.this.rectEmptyFrameBitmap.isRecycled()) {
			this.myRectView = null;
			CustomEditFrag.this.rectEmptyFrameBitmap.recycle();
			CustomEditFrag.this.rectEmptyFrameBitmap = null;
		}
		
		deleteSrcImageFile();
		if(this.loadImageTask != null) this.loadImageTask.cancel(true);
		if(this.resizeImageTask != null) this.resizeImageTask.cancel(true);
	}
	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(TAG, TAG+"--onDestroy");
	}
	/**
	 * 当该FragmentA从它所属的Activity中被删除时调用该方法
	 */
	@Override
	public void onDetach() {
		super.onDetach();
		Log.d(TAG, TAG+"--onDetach");
		mCallBack = null;
	}
	private void openActivity() {
		if(this.intMyRequestCode == CustomGridActivity.CUSTOM_EDIT_REQUEST_CODE_ALBUM) {
			try {
				Intent intent = new Intent();
				intent.setType("image/*");
				intent.setAction(Intent.ACTION_GET_CONTENT);
				startActivityForResult(intent, CustomGridActivity.CUSTOM_EDIT_REQUEST_CODE_ALBUM);
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}

		} else if(this.intMyRequestCode == CustomGridActivity.CUSTOM_EDIT_REQUEST_CODE_CAMERA) {
			try {
				this.isDeleteSrcImageFile = true;
				//先验证手机是否有sdcard
				if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
					this.srcImageFile  = mCallBack.getMyApp().getCustomCameraFile(picMiddleNameDate);
					if(!srcImageFile.getParentFile().exists()) srcImageFile.getParentFile().mkdirs();
					Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
					Uri outputFileUri = Uri.fromFile(srcImageFile);
					intent.putExtra(MediaStore.Images.Media.ORIENTATION, 0);
					intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
					startActivityForResult(intent, CustomGridActivity.CUSTOM_EDIT_REQUEST_CODE_CAMERA);
				}
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}
	}
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		srcImageRotate = 0;
		if (resultCode == Activity.RESULT_OK) {
			loadImageTask = new LoadImageTask();
			switch (requestCode) {
			case CustomGridActivity.CUSTOM_EDIT_REQUEST_CODE_ALBUM:
				loadImageTask.execute(data);
				break;
			case CustomGridActivity.CUSTOM_EDIT_REQUEST_CODE_CAMERA:
				loadImageTask.execute();
				break;
			default:
				break;
			}
		} else {
			mCallBack.openCustomPics();
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	private Bitmap getThumbnailWithUri(boolean isResize) throws FileNotFoundException, IOException {
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = false;
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        if(isResize) {	// for preview
    		if(this.ratioSrcImageWidthHeight > this.ratioCurrentWidthHeight) {
    			options.inSampleSize = this.intScrImageFileWidth / this.viableCurrentPicWidth;
    		} else {
    			options.inSampleSize = this.intScrImageFileHeight / this.viableCurrentPicHeight;
    		}
        } else {	//for final puzzle picture
        	if(this.ratioSrcImageWidthHeight > mCallBack.getMyApp().getPuzzleRatioWidthHeight()) {
        		float ratioPreviewHeightRectHeight = (float)this.previewBitmapHeight / (float)this.myRectView.getHeight();
        		if(ratioPreviewHeightRectHeight < 1f) ratioPreviewHeightRectHeight = 1f;
        		int tempSrcImgaeHeight = (int)(this.intScrImageFileHeight / ratioPreviewHeightRectHeight);
        		options.inSampleSize = tempSrcImgaeHeight / mCallBack.getMyApp().getAvailablePuzzleHeight();
        	} else {
        		float ratioPreviewWidthRectWidth = (float)this.previewBitmapWidth / (float)this.myRectView.getWidth();
        		if(ratioPreviewWidthRectWidth < 1f) ratioPreviewWidthRectWidth = 1f;
        		int tempSrcImgaeWidth = (int)(this.intScrImageFileWidth / ratioPreviewWidthRectWidth);
        		options.inSampleSize = tempSrcImgaeWidth / mCallBack.getMyApp().getAvailablePuzzleWidth();
        	}
        }
        
		if(options.inSampleSize < 1) options.inSampleSize = 1;
        try{
    		Bitmap bitmap = BitmapFactory.decodeFile(this.srcImageFile.getPath(), options);
    		if(this.srcImageRotate > 0) {
    			Matrix matrix = new Matrix();
    			matrix.setRotate(this.srcImageRotate);
    			Bitmap tempBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    			bitmap.recycle();
    			bitmap = tempBitmap;
    			tempBitmap = null;
    		}
    		return bitmap;
        } catch(Exception e) {
        	e.printStackTrace();
        	return null;
        }
	}
	protected void savePicture() {
		if (AppTools.getExternalStoragePath() == null)  {
			Toast.makeText(this.mCallBack.getMyApp(), R.string.NO_SDCARD, Toast.LENGTH_SHORT).show();
		} else {
			if(CustomEditFrag.this.previewBitmap != null && !CustomEditFrag.this.previewBitmap.isRecycled()) {
				this.editLayout.setVisibility(View.INVISIBLE);
				this.editLayout.removeView(this.previewImageView);
				this.previewImageView = null;
				CustomEditFrag.this.previewBitmap.recycle();
				CustomEditFrag.this.previewBitmap = null;
			}
			resizeImageTask = new ResizeImageTask();
			resizeImageTask.execute();
		}
	}
	private void deleteSrcImageFile() {
		if(this.isDeleteSrcImageFile && this.srcImageFile != null && this.srcImageFile.exists()) {
			this.srcImageFile.delete();
		}
	}
	class LoadImageTask extends AsyncTask<Intent, Void, Bitmap> {
		@Override
		protected void onPreExecute() {
			CustomEditFrag.this.progressBar.bringToFront();
			CustomEditFrag.this.progressBar.setVisibility(View.VISIBLE);
		}
		@Override
		protected Bitmap doInBackground(Intent... params) {
			CustomEditFrag.this.srcImageRotate = 0;
			Bitmap bitmap = null;
			if(params != null && params.length>0) {
				Cursor cur = null;
				try{
					Intent data = params[0];
					Uri picUri = data.getData();
					String[] proj = { MediaStore.Images.Media.DATA};
					ContentResolver cr = CustomEditFrag.this.mCallBack.getMyApp().getContentResolver();
					cur = cr.query(picUri,proj,null,null,null);
					//因为 proj 只定义获取一个字段（MediaStore.Images.Media.DATA），所以下面的参数（actual_image_column_index）永远为零。
					int actual_image_column_index = cur.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
					cur.moveToFirst();
					String srcImagePath = cur.getString(actual_image_column_index);
					if(srcImagePath == null || srcImagePath.equalsIgnoreCase("")) {
						Bitmap tempBitmap = MediaStore.Images.Media.getBitmap(cr, picUri);
						CustomEditFrag.this.srcImageFile = File.createTempFile("editpic", ".tmp");
						CustomEditFrag.this.isDeleteSrcImageFile = true;
						FileOutputStream outPicture = new FileOutputStream(CustomEditFrag.this.srcImageFile);
						if(tempBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outPicture)){
							outPicture.flush();
							outPicture.close();
						}
						if(!tempBitmap.isRecycled()) tempBitmap.recycle();
					} else {
						CustomEditFrag.this.srcImageFile = new File(srcImagePath);
						CustomEditFrag.this.isDeleteSrcImageFile = false;
					}
				} catch(Exception e) {
					e.printStackTrace();
				} finally {
					if(cur != null) {
						try {
							cur.close();
						} catch(Exception e) {
                            e.printStackTrace();
						}
					}
				}
			}
			if(CustomEditFrag.this.srcImageFile != null && CustomEditFrag.this.srcImageFile.exists() && CustomEditFrag.this.srcImageFile.length()>0) {
				CustomEditFrag.this.srcImageRotate = AppTools.getImageRotate(CustomEditFrag.this.srcImageFile.getPath());
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inJustDecodeBounds = true;
				BitmapFactory.decodeFile(CustomEditFrag.this.srcImageFile.getPath(), options);
				if(options.outWidth > 0 && options.outHeight > 0) {
					if(CustomEditFrag.this.srcImageRotate == 0 || CustomEditFrag.this.srcImageRotate == 180){
						CustomEditFrag.this.intScrImageFileWidth = options.outWidth;
						CustomEditFrag.this.intScrImageFileHeight = options.outHeight;
					} else {
						CustomEditFrag.this.intScrImageFileWidth = options.outHeight;
						CustomEditFrag.this.intScrImageFileHeight = options.outWidth;
					}
					CustomEditFrag.this.ratioSrcImageWidthHeight = (float)CustomEditFrag.this.intScrImageFileWidth / (float)CustomEditFrag.this.intScrImageFileHeight;
					if(CustomEditFrag.this.ratioSrcImageWidthHeight > CustomEditFrag.this.ratioCurrentWidthHeight) {
						CustomEditFrag.this.previewBitmapWidth = CustomEditFrag.this.viableCurrentPicWidth;
						CustomEditFrag.this.previewBitmapHeight = (int)((float)CustomEditFrag.this.previewBitmapWidth / CustomEditFrag.this.ratioSrcImageWidthHeight);
					} else {
						CustomEditFrag.this.previewBitmapHeight = CustomEditFrag.this.viableCurrentPicHeight;
						CustomEditFrag.this.previewBitmapWidth = (int)(CustomEditFrag.this.ratioSrcImageWidthHeight * CustomEditFrag.this.previewBitmapHeight);
					}
					if(CustomEditFrag.this.ratioSrcImageWidthHeight > mCallBack.getMyApp().getPuzzleRatioWidthHeight()) {
						CustomEditFrag.this.maxRectHeight = CustomEditFrag.this.previewBitmapHeight;
						CustomEditFrag.this.maxRectWidth = (int)(mCallBack.getMyApp().getPuzzleRatioWidthHeight() * CustomEditFrag.this.maxRectHeight);
					} else {
						CustomEditFrag.this.maxRectWidth = CustomEditFrag.this.previewBitmapWidth;
						CustomEditFrag.this.maxRectHeight = (int)(CustomEditFrag.this.maxRectWidth / mCallBack.getMyApp().getPuzzleRatioWidthHeight());
					}
				}
			}
			try {
				bitmap = getThumbnailWithUri(true);
				if(bitmap != null && !bitmap.isRecycled() && (bitmap.getWidth()!=CustomEditFrag.this.previewBitmapWidth || bitmap.getHeight()!=CustomEditFrag.this.previewBitmapHeight)) {
					Bitmap tempBitmap = ThumbnailUtils.extractThumbnail(bitmap, CustomEditFrag.this.previewBitmapWidth, CustomEditFrag.this.previewBitmapHeight, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
					bitmap = tempBitmap;
					tempBitmap = null;
				}
				if(bitmap == null) return null;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return bitmap;
		}
		@Override
		protected void onPostExecute(Bitmap bitmap) {
			CustomEditFrag.this.progressBar.setVisibility(View.GONE);
			if(bitmap == null) {
				mCallBack.openCustomPics();
				return;
			}
			CustomEditFrag.this.previewBitmap = bitmap;
			CustomEditFrag.this.previewImageView.setImageBitmap(CustomEditFrag.this.previewBitmap);
			CustomEditFrag.this.rectEmptyFrameBitmap = mCallBack.getMyApp().getBitmapEmptyFrame(0, (int)(CustomEditFrag.this.maxRectWidth * RECT_DEFAULT_RATIO), (int)(CustomEditFrag.this.maxRectHeight * RECT_DEFAULT_RATIO));
			CustomEditFrag.this.myRectView.setImageBitmap(CustomEditFrag.this.rectEmptyFrameBitmap);
			CustomEditFrag.this.myRectView.setMaxWH(CustomEditFrag.this.maxRectWidth, CustomEditFrag.this.maxRectHeight);
			if(AppConstants.CUSTOM_EDITPIC_ZOOM_RECT_TOAST_TIMES > 0) {
				Toast.makeText(CustomEditFrag.this.getActivity(), CustomEditFrag.this.getString(R.string.CUSTOM_EDITPIC_ZOOM_RECT_TOAST), Toast.LENGTH_LONG).show();
				AppConstants.CUSTOM_EDITPIC_ZOOM_RECT_TOAST_TIMES = AppConstants.CUSTOM_EDITPIC_ZOOM_RECT_TOAST_TIMES - 1;
			}
		}
	}
	class ResizeImageTask extends AsyncTask<Void, Void, Void> {
		@Override
		protected void onPreExecute() {
			CustomEditFrag.this.progressBar.bringToFront();
			CustomEditFrag.this.progressBar.setVisibility(View.VISIBLE);
		}
		@Override
		protected Void doInBackground(Void... params) {
			File filePicture = CustomEditFrag.this.mCallBack.getMyApp().getCustomPicture(AppTools.buildCustomPictureName(picMiddleNameDate));
			if(!filePicture.getParentFile().exists()) filePicture.getParentFile().mkdirs();
			try {
				Bitmap bitmap = getThumbnailWithUri(false);
				if(bitmap == null) return null;
				int cutX = (int)((float)CustomEditFrag.this.myRectView.getLeft() / (float)CustomEditFrag.this.previewBitmapWidth * (float)bitmap.getWidth());
				int cutY = (int)((float)CustomEditFrag.this.myRectView.getTop() / (float)CustomEditFrag.this.previewBitmapHeight * (float)bitmap.getHeight());
				int cutWidth = (int)((float)CustomEditFrag.this.myRectView.getWidth() / (float)CustomEditFrag.this.previewBitmapWidth * (float)bitmap.getWidth());
				int cutHeight = (int)((float)CustomEditFrag.this.myRectView.getHeight() / (float)CustomEditFrag.this.previewBitmapHeight * (float)bitmap.getHeight());
				if(cutX < 0) cutX = 0;
				if(cutY < 0) cutY = 0;
				if((cutWidth+cutX) > bitmap.getWidth()) {
					cutWidth = bitmap.getWidth() - cutX;
				}
				if((cutHeight+cutY) > bitmap.getHeight()) {
					cutHeight = bitmap.getHeight() - cutY;
				}
				if(bitmap.getWidth() != cutWidth || bitmap.getHeight() != cutHeight) {
					Bitmap tempBitmap = Bitmap.createBitmap(bitmap, cutX, cutY, cutWidth, cutHeight);
					bitmap.recycle();
					bitmap = tempBitmap;
					tempBitmap = null;
				}
				if(bitmap.getWidth() != mCallBack.getMyApp().getAvailablePuzzleWidth() || bitmap.getHeight() != mCallBack.getMyApp().getAvailablePuzzleHeight()) {
					Bitmap tempBitmap = ThumbnailUtils.extractThumbnail(bitmap, mCallBack.getMyApp().getAvailablePuzzleWidth(), mCallBack.getMyApp().getAvailablePuzzleHeight(), ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
					bitmap = tempBitmap;
					tempBitmap = null;
				}
				FileOutputStream outPicture = new FileOutputStream(filePicture);
				if(bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outPicture)){
					outPicture.flush();
					outPicture.close();
					CustomPicEntity entity = new CustomPicEntity();
					entity.setImageName(filePicture.getName());
					entity.setImportDateTime(Calendar.getInstance().getTime());
					entity.setSrcFilePath(CustomEditFrag.this.srcImageFile.getPath());
					DBHelperCustom.getInstance(CustomEditFrag.this.mCallBack.getMyApp()).insertCustomImage(entity, null);
				}
				if(bitmap != null && !bitmap.isRecycled()) {
					bitmap.recycle();
					bitmap = null;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}
		@Override
		protected void onPostExecute(Void params) {
			CustomEditFrag.this.progressBar.setVisibility(View.GONE);
			mCallBack.openCustomPics();
		}
	}
}

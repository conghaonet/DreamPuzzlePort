package org.hao.puzzle54.custom;

import android.app.Activity;
import android.app.ProgressDialog;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ProgressBar;

import com.flurry.android.FlurryAgent;

import org.hao.cache.CustomImagesFolderAdapter;
import org.hao.database.DBHelperCustom;
import org.hao.puzzle54.AppTools;
import org.hh.puzzle.port54.hall.R;

import java.io.File;
import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
	
public class CustomFoldersFrag extends Fragment {
	private static final String TAG = CustomFoldersFrag.class.getName();
	private ProgressBar progressBar;
	private GridView gridView;
	private CustomImagesFolderAdapter folderAdapter;
	private List<String> dataList = new ArrayList<String>();
	private ProgressDialog importDialog;
	private int viableMaxHeight;
	private int viableMaxWidth;
	private CustomGridActivityCallBack mCallBack;
	private InitialTask initialTask;
	private ImportTask importTask;

	/**
	 * 当该Fragment被添加,显示到Activity时调用该方法
	 * 在此判断显示到的Activity是否已经实现了接口
	 */
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		Log.d(TAG, TAG+"--onAttach");
		if (!(activity instanceof CustomGridActivityCallBack)) {
			throw new IllegalStateException("CustomFoldersFrag所在的Activity必须实现CustomGridActivityCallBack接口");
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
		View rootView = inflater.inflate(R.layout.grid_fragment, container, false);
		this.progressBar = (ProgressBar)rootView.findViewById(R.id.grid_fragment_progress_bar);
		this.progressBar.setVisibility(View.GONE);
		this.gridView = (GridView)rootView.findViewById(R.id.grid_fragment_grid_view);
		this.gridView.setNumColumns(getResources().getInteger(R.integer.custom_folder_grid_columns));
		this.gridView.setVerticalSpacing(this.mCallBack.getMyApp().getSpacingOfPictureGridItem());
		this.gridView.setHorizontalSpacing(this.mCallBack.getMyApp().getSpacingOfPictureGridItem());
		this.folderAdapter = new CustomImagesFolderAdapter(this.mCallBack.getMyApp(), this.dataList);
		this.folderAdapter.setOnItemClickListener(new CustomImagesFolderAdapter.OnItemClickListener() {
			@Override
			public void onItemClick(int position) {
				if(CustomFoldersFrag.this.dataList != null && position < CustomFoldersFrag.this.dataList.size()) {
					String selectedFilePath = CustomFoldersFrag.this.dataList.get(position);
					mCallBack.openCustomSelectedFolder(AppTools.getFullParentPathEndWithSeparator(selectedFilePath));
				}
			}
		});
		this.gridView.setAdapter(folderAdapter);
		this.importDialog = new ProgressDialog(this.getActivity());
		return rootView;
	}
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		Log.d(TAG, TAG+"--onActivityCreated");
		CustomGridActivity mActivity = (CustomGridActivity)this.getActivity();
		mActivity.setCustomSelectedTitle(mCallBack.getMyApp().getCustomSelectedSet().size());
		mCallBack.refreshActivity(CustomGridActivity.MENU_STYLE_CUSTOM_FOLDERS);
		this.viableMaxWidth = this.mCallBack.getMyApp().getAvailablePuzzleWidth();
		this.viableMaxHeight = this.mCallBack.getMyApp().getAvailablePuzzleHeight();
		initialTask = new InitialTask();
		initialTask.execute();
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
		if(this.initialTask != null) this.initialTask.cancel(true);
		if(this.importTask != null) this.importTask.cancel(true);
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
	protected void importSelectedPics() {
		if(this.mCallBack.getMyApp().getCustomSelectedSet()==null || this.mCallBack.getMyApp().getCustomSelectedSet().isEmpty()) return;
		importTask = new ImportTask();
		importTask.execute();
	}
    class InitialTask extends AsyncTask<Void, Void, List<String>> {
    	@Override
		protected void onPreExecute() {
    		progressBar.bringToFront();
			progressBar.setVisibility(View.VISIBLE);
		}
		@Override
		protected List<String> doInBackground(Void... params) {
			HashMap<String, List<String>> folderMap = CustomFoldersFrag.this.mCallBack.getMyApp().getCustomSrcImagesMap();
			Set<String> folderSet = (Set<String>)(new HashSet<String>(folderMap.keySet()).clone());
			List<String> tmpList = new ArrayList<String>();
			List<String> currentImagesSet = DBHelperCustom.getInstance(CustomFoldersFrag.this.mCallBack.getMyApp()).getAllSrcFilePath(null);
			try {
				for(String folderName:folderSet) {
					List<String> imagesList = folderMap.get(folderName);
					imagesList.removeAll(currentImagesSet);
					if(imagesList.isEmpty()) {
						folderMap.remove(folderName);
					} else {
						while(!imagesList.isEmpty()) {
							String firstImagePath = imagesList.get(0);
							BitmapFactory.Options opts = new BitmapFactory.Options();
							opts.inJustDecodeBounds = true;
							BitmapFactory.decodeFile(firstImagePath,opts);
							if(opts.outWidth > 0 && opts.outHeight > 0) {
								tmpList.add(firstImagePath);
								break;
							} else {
								imagesList.remove(firstImagePath);
							}
						}
						if(imagesList.isEmpty()) folderMap.remove(folderName);
						else folderMap.put(folderName, imagesList);
//						folderMap.put(folderName, imagesList);
//						String strImagePath = imagesList.get(0);
//						tmpList.add(strImagePath);
					}
				}
			} catch(Exception e) {
				e.printStackTrace();
			}
			return tmpList;
		}
		@Override
		protected void onPostExecute(List<String> tmpList) {
			progressBar.setVisibility(View.GONE);
			dataList.clear();
			dataList.addAll(tmpList);
			folderAdapter.notifyDataSetChanged();
			if(dataList != null && dataList.size()>0) {
				gridView.setSelection(dataList.size()-1);
			}
		}
    }
    class ImportTask extends AsyncTask<Void, Integer, Void> {
    	@Override
		protected void onPreExecute() {
    		CustomFoldersFrag.this.importDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
    		CustomFoldersFrag.this.importDialog.setTitle(R.string.CUSTOM_FOLDER_IMPORT_DIALOG_TITLE);
    		CustomFoldersFrag.this.importDialog.setMessage(CustomFoldersFrag.this.getString(R.string.CUSTOM_FOLDER_IMPORT_DIALOG_MSG));
    		CustomFoldersFrag.this.importDialog.setIcon(android.R.drawable.ic_menu_save);
    		CustomFoldersFrag.this.importDialog.setProgress(0);
    		CustomFoldersFrag.this.importDialog.setMax(CustomFoldersFrag.this.mCallBack.getMyApp().getCustomSelectedSet().size());
    		CustomFoldersFrag.this.importDialog.setIndeterminate(false);
    		CustomFoldersFrag.this.importDialog.setCancelable(false);
    		CustomFoldersFrag.this.importDialog.show();
		}
		@Override
		protected Void doInBackground(Void... params) {
			Iterator<String> iterator = CustomFoldersFrag.this.mCallBack.getMyApp().getCustomSelectedSet().iterator();
			Date importDateTime = Calendar.getInstance().getTime();
			String pattern = "00";
			while(pattern.length() < String.valueOf(CustomFoldersFrag.this.mCallBack.getMyApp().getCustomSelectedSet().size()).length()) {
				pattern = pattern + "0";
			}
			DecimalFormat decimalFormat = new DecimalFormat(pattern);
			int imgIndex = 0;
			SQLiteDatabase dbCustom = DBHelperCustom.getInstance(CustomFoldersFrag.this.mCallBack.getMyApp()).getWritableDatabase();
			while(iterator.hasNext()) {
				try {
					++imgIndex;
					String srcImageFullPath = iterator.next();
					int imageRotate = AppTools.getImageRotate(srcImageFullPath);
					BitmapFactory.Options options = new BitmapFactory.Options();
					options.inJustDecodeBounds = true;
					Bitmap bitmap = BitmapFactory.decodeFile(srcImageFullPath, options);
					if(options.outHeight<=0 || options.outWidth<=0) {
						publishProgress(imgIndex);
						continue;
					}
					if(imageRotate == 0 || imageRotate == 180)
		        		options.inSampleSize = (options.outHeight/CustomFoldersFrag.this.viableMaxHeight+options.outWidth/CustomFoldersFrag.this.viableMaxWidth)/2;
		        	else options.inSampleSize = (options.outWidth/CustomFoldersFrag.this.viableMaxHeight+options.outHeight/CustomFoldersFrag.this.viableMaxHeight)/2;
					if(options.inSampleSize < 1) options.inSampleSize = 1;
					options.inJustDecodeBounds = false;
			        options.inPreferredConfig = Bitmap.Config.RGB_565;
			        bitmap = BitmapFactory.decodeFile(srcImageFullPath, options);
			        if(imageRotate == 0 || imageRotate == 180) {
			        	if(bitmap.getWidth() != CustomFoldersFrag.this.viableMaxWidth || bitmap.getHeight() != CustomFoldersFrag.this.viableMaxHeight) {
			        		@SuppressWarnings("UnnecessaryLocalVariable") Bitmap tempBitmap = ThumbnailUtils.extractThumbnail(bitmap, CustomFoldersFrag.this.viableMaxWidth, CustomFoldersFrag.this.viableMaxHeight, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
							bitmap = tempBitmap;
			        	}
			        } else {
			        	if(bitmap.getWidth() != CustomFoldersFrag.this.viableMaxHeight || bitmap.getHeight() != CustomFoldersFrag.this.viableMaxWidth) {
			        		@SuppressWarnings("UnnecessaryLocalVariable") Bitmap tempBitmap = ThumbnailUtils.extractThumbnail(bitmap, CustomFoldersFrag.this.viableMaxHeight, CustomFoldersFrag.this.viableMaxWidth, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
							bitmap = tempBitmap;
			        	}
			        }
			        if(imageRotate > 0) {
						Matrix matrix = new Matrix();
						matrix.setRotate(imageRotate);
						Bitmap tempBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
						bitmap.recycle();
						bitmap = tempBitmap;
					}
					String shortDestFileName = AppTools.buildCustomPictureName(importDateTime, decimalFormat.format(imgIndex));
					File filePicture = CustomFoldersFrag.this.mCallBack.getMyApp().getCustomPicture(shortDestFileName);
					if(!filePicture.getParentFile().exists()) filePicture.getParentFile().mkdirs();
					FileOutputStream outPicture = new FileOutputStream(filePicture);
					if(bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outPicture)){
						outPicture.flush();
						outPicture.close();
						CustomPicEntity entity = new CustomPicEntity();
						entity.setImageName(shortDestFileName);
						entity.setImportDateTime(importDateTime);
						entity.setSrcFilePath(srcImageFullPath);
						DBHelperCustom.getInstance(CustomFoldersFrag.this.mCallBack.getMyApp()).insertCustomImage(entity, dbCustom);
					}
					if(!bitmap.isRecycled()) bitmap.recycle();
					publishProgress(imgIndex);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			dbCustom.close();
			if(imgIndex != CustomFoldersFrag.this.mCallBack.getMyApp().getCustomSelectedSet().size()) {
				publishProgress(CustomFoldersFrag.this.mCallBack.getMyApp().getCustomSelectedSet().size());
			}
			return null;
		}
		@Override
		protected void onProgressUpdate(Integer... progresses) {
			CustomFoldersFrag.this.importDialog.setProgress(progresses[0]);
		}
		@Override
		protected void onPostExecute(Void voida) {
			CustomFoldersFrag.this.importDialog.dismiss();
			mCallBack.openCustomPics();
		}
    }
}

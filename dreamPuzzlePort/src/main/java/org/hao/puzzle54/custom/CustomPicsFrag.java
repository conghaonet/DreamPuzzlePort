package org.hao.puzzle54.custom;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.flurry.android.FlurryAgent;

import org.hao.cache.CustomGridItemAdapter;
import org.hao.database.DBHelperCustom;
import org.hao.database.DBHelperScore;
import org.hao.puzzle54.AppConstants;
import org.hh.puzzle.port54.hall.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
	
public class CustomPicsFrag extends Fragment {
	private static final String TAG = CustomPicsFrag.class.getName();
	private ProgressBar progressBar;
	private GridView gridView;
	private CustomGridItemAdapter gridAdapter;
	private boolean isEditMode;
	private List<CustomPicEntity> listSelected;
	private List<CustomPicEntity> listCustomEntity;
	private FrameLayout rootLayout;
	private CustomGridActivityCallBack mCallBack;
	private InitialTask initialTask;
	private ProgressDialog deleteProgressDialog;

	/**
	 * 当该Fragment被添加,显示到Activity时调用该方法
	 * 在此判断显示到的Activity是否已经实现了接口
	 */
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		Log.d(TAG, TAG+"--onAttach");
		if (!(activity instanceof CustomGridActivityCallBack)) {
			throw new IllegalStateException("CustomPicsFrag所在的Activity必须实现CustomGridActivityCallBack接口");
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
		this.mCallBack.getMyApp().clearAllCustomCollection();
		this.progressBar = (ProgressBar)rootView.findViewById(R.id.grid_fragment_progress_bar);
		this.progressBar.setVisibility(View.GONE);
		this.rootLayout = (FrameLayout)rootView.findViewById(R.id.grid_fragment_root_layout);
		gridView = (GridView)rootView.findViewById(R.id.grid_fragment_grid_view);
		gridView.setVerticalSpacing(this.mCallBack.getMyApp().getSpacingOfPictureGridItem());
		gridView.setHorizontalSpacing(this.mCallBack.getMyApp().getSpacingOfPictureGridItem());
		this.deleteProgressDialog = new ProgressDialog(this.getActivity());
		return rootView;
	}
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		Log.d(TAG, TAG+"--onActivityCreated");
		mCallBack.refreshActivity(R.id.custom_menu_group_standard);
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
	private void initListener() {
		gridAdapter.setOnItemLongClickListener(new CustomGridItemAdapter.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(int position) {
				if(!CustomPicsFrag.this.isEditMode) {
					CustomPicsFrag.this.isEditMode = true;
					CustomPicsFrag.this.listSelected.add(CustomPicsFrag.this.listCustomEntity.get(position));
					CustomGridActivity mActivity = (CustomGridActivity)CustomPicsFrag.this.getActivity();
					mActivity.setCustomSelectedTitle(CustomPicsFrag.this.listSelected.size());
					if(CustomPicsFrag.this.listSelected.size() == CustomPicsFrag.this.listCustomEntity.size())
						mCallBack.refreshActivity(R.id.custom_menu_group_delete_deselect_all);
					else mCallBack.refreshActivity(R.id.custom_menu_group_delete_select_all);
					CustomPicsFrag.this.gridAdapter.setEditMode(CustomPicsFrag.this.isEditMode);
					CustomPicsFrag.this.gridAdapter.setSetSelected(CustomPicsFrag.this.listSelected);
					CustomPicsFrag.this.gridAdapter.notifyDataSetChanged();
					return true;
				} else {
					return false;
				}
			}
		});
		gridAdapter.setOnItemClickListener(new CustomGridItemAdapter.OnItemClickListener() {
			@Override
			public void onItemClick(int position) {
				if(CustomPicsFrag.this.isEditMode) {
					CustomPicEntity entity = CustomPicsFrag.this.listCustomEntity.get(position);
					if(CustomPicsFrag.this.listSelected.contains(entity)) {
						CustomPicsFrag.this.listSelected.remove(entity);
					} else {
						CustomPicsFrag.this.listSelected.add(entity);
					}
					CustomGridActivity mActivity = (CustomGridActivity)CustomPicsFrag.this.getActivity();
					mActivity.setCustomSelectedTitle(CustomPicsFrag.this.listSelected.size());
					if(CustomPicsFrag.this.listSelected.size() == CustomPicsFrag.this.listCustomEntity.size())
						mCallBack.refreshActivity(R.id.custom_menu_group_delete_deselect_all);
					else mCallBack.refreshActivity(R.id.custom_menu_group_delete_select_all);
					CustomPicsFrag.this.gridAdapter.setSetSelected(CustomPicsFrag.this.listSelected);
					CustomPicsFrag.this.gridAdapter.notifyDataSetChanged();
					
				} else {
					mCallBack.openCustomPreview(AppConstants.CUSTOM_PACKAGE_CODE, position);
				}
			}
		});
	}
	protected void selectAll(boolean isAll) {
		this.listSelected.clear();
		if(isAll) this.listSelected.addAll(this.listCustomEntity);
		CustomGridActivity mActivity = (CustomGridActivity)getActivity();
		mActivity.setCustomSelectedTitle(CustomPicsFrag.this.listSelected.size());
		if(isAll) mCallBack.refreshActivity(R.id.custom_menu_group_delete_deselect_all);
		else mCallBack.refreshActivity(R.id.custom_menu_group_delete_select_all);
		this.gridAdapter.setSetSelected(this.listSelected);
		this.gridAdapter.notifyDataSetChanged();
	}
	protected void set2StandardMode() {
		mCallBack.refreshActivity(R.id.custom_menu_group_standard);
		this.isEditMode = false;
		this.listSelected.clear();
		this.gridAdapter.setEditMode(this.isEditMode);
		this.gridAdapter.setSetSelected(this.listSelected);
		this.gridAdapter.notifyDataSetChanged();
	}
	protected void showDeleteDialog() {
		if(this.listSelected == null || this.listSelected.isEmpty()) return;
		AlertDialog.Builder dialogDelete = new AlertDialog.Builder(this.getActivity());
		dialogDelete.setCancelable(true);
		dialogDelete.setTitle(R.string.DIALOG_CUSTOM_GRID_DELETE_TITLE);
		dialogDelete.setMessage(R.string.DIALOG_CUSTOM_GRID_DELETE_MSG);
		dialogDelete.setPositiveButton(
				R.string.DIALOG_BUTTON_YES,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if(CustomPicsFrag.this.listSelected != null && !CustomPicsFrag.this.listSelected.isEmpty()) {
							new DeleteTask().execute();
						}
					}
				}
			);
		dialogDelete.setNegativeButton(
				R.string.DIALOG_BUTTON_NO,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
					}
				}
			);
		dialogDelete.show();
	}
	class InitialTask extends AsyncTask<Void, Void, Void> {
		@Override
		protected void onPreExecute() {
			progressBar.bringToFront();
			progressBar.setVisibility(View.VISIBLE);
		}
		@Override
		protected Void doInBackground(Void... params) {
			SQLiteDatabase dbCustom = null;
			SQLiteDatabase dbScore = null;
			try{
				dbCustom = DBHelperCustom.getInstance(CustomPicsFrag.this.mCallBack.getMyApp()).getWritableDatabase();
				dbScore = DBHelperScore.getInstance(CustomPicsFrag.this.mCallBack.getMyApp()).getWritableDatabase();
				CustomPicsFrag.this.listSelected = new ArrayList<CustomPicEntity>();
				CustomPicsFrag.this.listCustomEntity = DBHelperCustom.getInstance(CustomPicsFrag.this.mCallBack.getMyApp()).getAll(dbCustom);
				for(int i=0;i<listCustomEntity.size();i++) {
					CustomPicEntity entity = listCustomEntity.get(i);
					File picFile = CustomPicsFrag.this.mCallBack.getMyApp().getCustomPicture(entity.getImageName());
					if(!picFile.exists()) {
						DBHelperScore.getInstance(CustomPicsFrag.this.mCallBack.getMyApp()).delete(entity.getId(), AppConstants.CUSTOM_PACKAGE_CODE, dbScore);
						DBHelperCustom.getInstance(CustomPicsFrag.this.mCallBack.getMyApp()).deleteCustomImage(entity.getImageName(), dbCustom);
						listCustomEntity.remove(i);
						i=i-1;
					}
				}
			} catch(Exception e) {
				e.printStackTrace();
			} finally {
				if(dbCustom != null) {
					try{
						dbCustom.close();
					} catch(Exception e) {
						e.printStackTrace();
					}
				}
				if(dbScore != null) {
					try{
						dbScore.close();
					} catch(Exception e) {
						e.printStackTrace();
					}
				}
			}
			return null;
		}
		@Override
		protected void onPostExecute(Void params) {
			gridAdapter = new CustomGridItemAdapter(CustomPicsFrag.this.mCallBack.getMyApp(), CustomPicsFrag.this.listCustomEntity);
			gridView.setAdapter(gridAdapter);
			initListener();
			progressBar.setVisibility(View.GONE);
			gridAdapter.notifyDataSetChanged();
			if(CustomPicsFrag.this.listCustomEntity != null && CustomPicsFrag.this.listCustomEntity.size()>0) {
				gridView.setSelection(CustomPicsFrag.this.listCustomEntity.size()-1);
			} else {
				Bitmap bmpIconMore = BitmapFactory.decodeResource(CustomPicsFrag.this.getResources(), R.drawable.icon_more);
				int intIconMoreWidth = mCallBack.getMyApp().getDisplay().widthPixels/3;
				if(intIconMoreWidth > 512) intIconMoreWidth = 512;
				if(bmpIconMore.getWidth() != intIconMoreWidth) {
					Bitmap tempBitmap = Bitmap.createScaledBitmap(bmpIconMore, intIconMoreWidth, intIconMoreWidth, true);
					if(!bmpIconMore.isRecycled()) bmpIconMore.recycle();
					bmpIconMore = tempBitmap;
					tempBitmap = null;
				}
				
				FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER);
				ImageView imgIconMore = new ImageView(CustomPicsFrag.this.getActivity());
				imgIconMore.setImageBitmap(bmpIconMore);
				imgIconMore.setOnClickListener(new ImageButton.OnClickListener() {
					@Override
					public void onClick(View v) {
						mCallBack.openCustomFolders();
					}
				});
				rootLayout.addView(imgIconMore, layoutParams);
			}
		}
	}
	class DeleteTask extends AsyncTask<Void, Integer, Void> {
		@Override
		protected void onPreExecute() {
			CustomPicsFrag.this.deleteProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			CustomPicsFrag.this.deleteProgressDialog.setTitle(R.string.CUSTOM_PICS_DELETE_DIALOG_TITLE);
			CustomPicsFrag.this.deleteProgressDialog.setMessage(CustomPicsFrag.this.getString(R.string.CUSTOM_PICS_DELETE_DIALOG_MSG));
			CustomPicsFrag.this.deleteProgressDialog.setIcon(android.R.drawable.ic_menu_delete);
			CustomPicsFrag.this.deleteProgressDialog.setProgress(0);
    		CustomPicsFrag.this.deleteProgressDialog.setMax(CustomPicsFrag.this.listSelected.size());
    		CustomPicsFrag.this.deleteProgressDialog.setIndeterminate(false);
    		CustomPicsFrag.this.deleteProgressDialog.setCancelable(false);
    		CustomPicsFrag.this.deleteProgressDialog.show();
		}
		@Override
		protected Void doInBackground(Void... avoid) {
			SQLiteDatabase dbCustom = null;
			SQLiteDatabase dbScore = null;
			try{
				dbCustom = DBHelperCustom.getInstance(CustomPicsFrag.this.mCallBack.getMyApp()).getWritableDatabase();
				dbScore = DBHelperScore.getInstance(CustomPicsFrag.this.mCallBack.getMyApp()).getWritableDatabase();
				int progressIndex = 0;
				while(CustomPicsFrag.this.listSelected != null && !CustomPicsFrag.this.listSelected.isEmpty()) {
					++progressIndex;
					publishProgress(progressIndex);
					try{
						CustomPicEntity entity = CustomPicsFrag.this.listSelected.remove(0);
						String strPath = CustomPicsFrag.this.mCallBack.getMyApp().getCustomPicture(entity.getImageName()).getPath();
						File pictureFile = new File(strPath);
						if(pictureFile != null && pictureFile.exists()) {
							pictureFile.delete();
						}
						DBHelperScore.getInstance(CustomPicsFrag.this.mCallBack.getMyApp()).delete(entity.getId(), AppConstants.CUSTOM_PACKAGE_CODE, dbScore);
						DBHelperCustom.getInstance(CustomPicsFrag.this.mCallBack.getMyApp()).deleteCustomImage(entity.getImageName(), dbCustom);
						CustomPicsFrag.this.listCustomEntity.remove(entity);
					} catch(Exception e) {
						e.printStackTrace();
					}
				}
				if(progressIndex != CustomPicsFrag.this.deleteProgressDialog.getMax()) {
					publishProgress(CustomPicsFrag.this.deleteProgressDialog.getMax());
				}
			} catch(Exception e) {
				e.printStackTrace();
			} finally {
				if(dbCustom != null) {
					try{
						dbCustom.close();
					} catch(Exception e) {
						e.printStackTrace();
					}
				}
				if(dbScore != null) {
					try{
						dbScore.close();
					} catch(Exception e) {
						e.printStackTrace();
					}
				}
			}
			return null;
		}
		@Override
		protected void onProgressUpdate(Integer... progresses) {
			CustomPicsFrag.this.deleteProgressDialog.setProgress(progresses[0]);
		}
		@Override
		protected void onPostExecute(Void avoid) {
			CustomPicsFrag.this.deleteProgressDialog.dismiss();
			set2StandardMode();
		}
	}
}

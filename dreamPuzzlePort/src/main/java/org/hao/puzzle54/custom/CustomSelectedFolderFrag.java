package org.hao.puzzle54.custom;

import android.app.Activity;
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

import org.hao.cache.CustomImagesSelectAdapter;
import org.hao.puzzle54.AppTools;
import org.hao.puzzle54.MyBundleData;
import org.hh.puzzle.port54.hall.R;

import java.util.ArrayList;
import java.util.List;
	
public class CustomSelectedFolderFrag extends Fragment {
	private static final String TAG = CustomSelectedFolderFrag.class.getName();
    private ProgressBar progressBar;
	private String strFolder;
	private CustomImagesSelectAdapter imagesAdapter;
	private List<String> dataList;
	private int intSelectedAmountInThisFolder;
	private CustomGridActivityCallBack mCallBack;
	private InitialTask initialTask;
	
	/**
	 * 当该Fragment被添加,显示到Activity时调用该方法
	 * 在此判断显示到的Activity是否已经实现了接口
	 */
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		Log.d(TAG, TAG+"--onAttach");
		if (!(activity instanceof CustomGridActivityCallBack)) {
			throw new IllegalStateException("CustomSelectedFolderFrag所在的Activity必须实现CustomGridActivityCallBack接口");
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
		this.dataList = new ArrayList<String>();
		this.intSelectedAmountInThisFolder = 0;
		this.strFolder = null;
		this.progressBar = (ProgressBar)rootView.findViewById(R.id.grid_fragment_progress_bar);
		if(getArguments().containsKey(MyBundleData.SELECTED_FOLDER)) {
			this.progressBar.setVisibility(View.GONE);
			this.strFolder = this.getArguments().getString(MyBundleData.SELECTED_FOLDER);
            GridView gridView = (GridView) rootView.findViewById(R.id.grid_fragment_grid_view);
			gridView.setVerticalSpacing(this.mCallBack.getMyApp().getSpacingOfPictureGridItem());
			gridView.setHorizontalSpacing(this.mCallBack.getMyApp().getSpacingOfPictureGridItem());
			imagesAdapter = new CustomImagesSelectAdapter(this.mCallBack.getMyApp(), dataList);
			imagesAdapter.setOnItemClickListener(new CustomImagesSelectAdapter.OnItemClickListener() {
				@Override
				public void onItemClick(int position) {
					if(dataList != null && dataList.size()>position) {
						String imagePath = dataList.get(position);
						if(mCallBack.getMyApp().getCustomSelectedSet().contains(imagePath)) {
							mCallBack.getMyApp().getCustomSelectedSet().remove(imagePath);
							--CustomSelectedFolderFrag.this.intSelectedAmountInThisFolder;
						} else {
							mCallBack.getMyApp().getCustomSelectedSet().add(imagePath);
							++CustomSelectedFolderFrag.this.intSelectedAmountInThisFolder;
						}
					}
					CustomGridActivity mActivity = (CustomGridActivity)CustomSelectedFolderFrag.this.getActivity();
					mActivity.setCustomSelectedTitle(CustomSelectedFolderFrag.this.intSelectedAmountInThisFolder);
					if(mCallBack.getMyApp().getCustomSrcImagesMap().get(CustomSelectedFolderFrag.this.strFolder).size() == CustomSelectedFolderFrag.this.intSelectedAmountInThisFolder)
						mCallBack.refreshActivity(R.id.custom_menu_group_add_deselect_all);
					else mCallBack.refreshActivity(R.id.custom_menu_group_add_select_all);
					CustomSelectedFolderFrag.this.imagesAdapter.notifyDataSetChanged();
				}
			});
			gridView.setAdapter(imagesAdapter);
		} else {
			this.progressBar.bringToFront();
			this.progressBar.setVisibility(View.VISIBLE);
		}
		return rootView;
	}
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		Log.d(TAG, TAG+"--onActivityCreated");
        for (String str : mCallBack.getMyApp().getCustomSelectedSet()) {
            if (AppTools.getFullParentPathEndWithSeparator(str).equalsIgnoreCase(this.strFolder)) {
                ++this.intSelectedAmountInThisFolder;
            }
        }
		CustomGridActivity mActivity = (CustomGridActivity)CustomSelectedFolderFrag.this.getActivity();
		mActivity.setCustomSelectedTitle(this.intSelectedAmountInThisFolder);
		if(mCallBack.getMyApp().getCustomSrcImagesMap().get(CustomSelectedFolderFrag.this.strFolder).size() == CustomSelectedFolderFrag.this.intSelectedAmountInThisFolder)
			mCallBack.refreshActivity(R.id.custom_menu_group_add_deselect_all);
		else mCallBack.refreshActivity(R.id.custom_menu_group_add_select_all);
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
	protected void selectAll(boolean isAll) {
		if(isAll) {
			List<String> listThisFolder = mCallBack.getMyApp().getCustomSrcImagesMap().get(this.strFolder);
			if(listThisFolder.size() != this.intSelectedAmountInThisFolder) {
				this.intSelectedAmountInThisFolder = listThisFolder.size();
				mCallBack.getMyApp().getCustomSelectedSet().addAll(listThisFolder);
			}
		} else {
			if(this.intSelectedAmountInThisFolder > 0) {
				List<String> listThisFolder = mCallBack.getMyApp().getCustomSrcImagesMap().get(this.strFolder);
        		this.intSelectedAmountInThisFolder = 0;
        		this.mCallBack.getMyApp().getCustomSelectedSet().removeAll(listThisFolder);
			}
		}
		CustomGridActivity mActivity = (CustomGridActivity)getActivity();
		mActivity.setCustomSelectedTitle(this.intSelectedAmountInThisFolder);
		if(isAll) mCallBack.refreshActivity(R.id.custom_menu_group_add_deselect_all);
		else mCallBack.refreshActivity(R.id.custom_menu_group_add_select_all);
		this.imagesAdapter.notifyDataSetChanged();
	}
	class InitialTask extends AsyncTask<Void, Void, List<String>> {
		@Override
		protected void onPreExecute() {
			progressBar.bringToFront();
			progressBar.setVisibility(View.VISIBLE);
		}
		@Override
		protected List<String> doInBackground(Void... params) {
			return mCallBack.getMyApp().getCustomSrcImagesMap().get(CustomSelectedFolderFrag.this.strFolder);
		}
		@Override
		protected void onPostExecute(List<String> tmpList) {
			progressBar.setVisibility(View.GONE);
			dataList.clear();
			dataList.addAll(tmpList);
			CustomSelectedFolderFrag.this.imagesAdapter.notifyDataSetChanged();
		}
	}
}

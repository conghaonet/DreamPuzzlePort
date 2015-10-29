package org.hao.puzzle54;

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

import org.hao.cache.PackagePicsItemAdapter;
import org.hh.puzzle.port54.hall.R;

import java.io.File;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
	
public class PackagePicsFrag extends Fragment {
	private static final String TAG = PackagePicsFrag.class.getName();
	
	private GridView gridView;
	private ArrayList<String> dataList = new ArrayList<String>();
	private ProgressBar progressBar;
	private PackagePicsItemAdapter gridAdapter;
	private long allowedMaxPicIndex;
	private String packageCode;
	private PackageGridActivityCallBack mCallBack;
	private InitialTask initialTask;
	private boolean isInnerPics;

	/**
	 * 当该Fragment被添加,显示到Activity时调用该方法
	 * 在此判断显示到的Activity是否已经实现了接口
	 */
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		Log.d(TAG, TAG+"--onAttach");
		if (!(activity instanceof PackageGridActivityCallBack)) {
			throw new IllegalStateException("PackagePicsFrag所在的Activity必须实现PackageGridActivityCallBack接口");
		}
		mCallBack = (PackageGridActivityCallBack)activity;
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
		this.progressBar =  (ProgressBar)rootView.findViewById(R.id.grid_fragment_progress_bar);
		this.progressBar.setVisibility(View.GONE);
		if(getArguments().containsKey(MyBundleData.PACKAGE_CODE)) {
			this.packageCode = this.getArguments().getString(MyBundleData.PACKAGE_CODE);
			this.isInnerPics = mCallBack.getMyApp().isInnerPics(packageCode);
			this.allowedMaxPicIndex = this.mCallBack.getMyApp().getAllowedMaxPicIndex(this.packageCode);
			gridView = (GridView)rootView.findViewById(R.id.grid_fragment_grid_view);
			gridView.setVerticalSpacing(this.mCallBack.getMyApp().getSpacingOfPictureGridItem());
			gridView.setHorizontalSpacing(this.mCallBack.getMyApp().getSpacingOfPictureGridItem());
			gridAdapter = new PackagePicsItemAdapter(this.mCallBack.getMyApp(), this.isInnerPics, dataList, this.allowedMaxPicIndex, this.packageCode);
			gridAdapter.setOnItemClickListener(new PackagePicsItemAdapter.OnItemClickListener() {
				@Override
				public void onItemClick(int position) {
					mCallBack.openPuzzlePreview(PackagePicsFrag.this.packageCode, position);
				}
			});
	 		gridView.setAdapter(gridAdapter);
		}
		return rootView;
	}
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		Log.d(TAG, TAG+"--onActivityCreated");
		if(this.packageCode == null) mCallBack.openNewGameFrag();
		else {
			initialTask = new InitialTask();
			initialTask.execute();
		}
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
	class InitialTask extends AsyncTask<Void, Void, ArrayList<String>> {
		@Override
		protected void onPreExecute() {
			progressBar.bringToFront();
			progressBar.setVisibility(View.VISIBLE);
		}		
		@Override
		protected ArrayList<String> doInBackground(Void... params) {
			ArrayList<String> tmpList = new ArrayList<String>();
			try {
				if(PackagePicsFrag.this.packageCode == null) return null;
				if(PackagePicsFrag.this.isInnerPics) {
					tmpList = new ArrayList(mCallBack.getMyApp().getInnerPics());
				} else {
					ArrayList<ZipEntry> listZipEntry = mCallBack.getMyApp().getCurrentZipEntries(PackagePicsFrag.this.packageCode);
					for (ZipEntry aListZipEntry : listZipEntry) {
						tmpList.add(PackagePicsFrag.this.packageCode + File.separator + aListZipEntry.getName());
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return tmpList;
		}
		@Override
	    protected void onPostExecute(ArrayList<String> tmpList) {
			PackagePicsFrag.this.progressBar.setVisibility(View.GONE);
			dataList.clear();
			dataList.addAll(tmpList);
			gridAdapter.notifyDataSetChanged();
			if(dataList != null && dataList.size()>0) {
				long itemSelection = PackagePicsFrag.this.allowedMaxPicIndex-3;
				if(itemSelection < 0) itemSelection = 0;
				gridView.setSelection((int)itemSelection);
			}
        }
	}
    
}

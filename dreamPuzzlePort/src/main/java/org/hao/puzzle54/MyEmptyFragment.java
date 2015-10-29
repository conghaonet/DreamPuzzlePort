package org.hao.puzzle54;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.hh.puzzle.port54.hall.R;
	
public class MyEmptyFragment extends Fragment {
	private static final String TAG = MyEmptyFragment.class.getName();

	/**
	 * 当该Fragment被添加,显示到Activity时调用该方法
	 * 在此判断显示到的Activity是否已经实现了接口
	 */
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		Log.d(TAG, TAG+"--onAttach");
	}
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, TAG+"--onCreate");
    }
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.d(TAG, TAG+"--onCreateView");
		return inflater.inflate(R.layout.grid_fragment, container, false);
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
		super.onDestroyView();
		Log.d(TAG, TAG+"--onDestroyView");
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
	}
	
}

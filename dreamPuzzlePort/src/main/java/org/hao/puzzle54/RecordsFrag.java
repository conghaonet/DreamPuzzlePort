package org.hao.puzzle54;

import android.app.Activity;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import org.hao.database.DBHelperScore;
import org.hh.puzzle.port54.hall.R;

import java.util.List;
	
public class RecordsFrag extends Fragment{
	private static final String TAG = RecordsFrag.class.getName();
	private PreviewPicActivityCallBack mCallBack;
//	private MyApp myApp;
	private String packageCode;
	private long currentPicIndex;
	private TableLayout tableLayout;
	private TextView txtNorecords;
	private InitialTask initialTask;
	
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
        setRetainInstance(true);
        Log.d(TAG, TAG+"--onCreate");
    }
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.d(TAG, TAG+"--onCreateView");
		View rootView = inflater.inflate(R.layout.records_fragment, container, false);
		tableLayout = (TableLayout)rootView.findViewById(R.id.records_tablelayout);
		tableLayout.setVisibility(View.INVISIBLE);
		txtNorecords = (TextView)rootView.findViewById(R.id.records_no_records);
		txtNorecords.setVisibility(View.GONE);
		if(this.getArguments()!= null && this.getArguments().containsKey(MyBundleData.PICTURE_INDEX) && this.getArguments().containsKey(MyBundleData.PACKAGE_CODE)) {
			this.currentPicIndex = getArguments().getLong(MyBundleData.PICTURE_INDEX);
			this.packageCode = getArguments().getString(MyBundleData.PACKAGE_CODE);
		}
		return rootView;
	}
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		Log.d(TAG, TAG+"--onActivityCreated");
		initialTask = new InitialTask();
		initialTask.execute();
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
		if(this.initialTask != null) initialTask.cancel(true);
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
	class InitialTask extends AsyncTask<Void, String, Boolean> {
		private boolean hasRowColor; 
		@Override
		protected Boolean doInBackground(Void... params) {
			List<ScoreEntity> listScores =  null;
			listScores = DBHelperScore.getInstance(RecordsFrag.this.mCallBack.getMyApp()).getScores(RecordsFrag.this.packageCode, RecordsFrag.this.currentPicIndex, null);
			if(listScores != null && !listScores.isEmpty()) {
				while(!listScores.isEmpty()) {
					String strArray[] = new String[3];
					ScoreEntity score = listScores.remove(0);
					strArray[0] = score.getRows()+" x "+score.getCols();
					strArray[1] = AppTools.formatElapsedTimeSec2String(score.getBestTime());
					strArray[2] = ""+score.getStepsOfBestTime();
					publishProgress(strArray);
				}
				return true;
			}			
			return false;
		}
		@Override
		protected void onProgressUpdate(String... params) {
			if(RecordsFrag.this.tableLayout.getVisibility() != View.VISIBLE) RecordsFrag.this.tableLayout.setVisibility(View.VISIBLE);
			Activity theActivity = RecordsFrag.this.getActivity();
			TableRow row = new TableRow(theActivity);
			if(this.hasRowColor) {
				row.setBackgroundColor(Color.argb(0xAA, 0x66, 0x66, 0x66));
			}
			this.hasRowColor = !this.hasRowColor;
			TextView txtDifficulty = new TextView(theActivity);
			TextView txtTime = new TextView(theActivity);
			TextView txtMoves = new TextView(theActivity);
			
			txtDifficulty.setGravity(Gravity.CENTER);
			txtTime.setGravity(Gravity.CENTER);
			txtMoves.setGravity(Gravity.CENTER);
			
			txtDifficulty.setTextAppearance(theActivity, R.style.records_layout_text);
			txtTime.setTextAppearance(theActivity, R.style.records_layout_text);
			txtMoves.setTextAppearance(theActivity, R.style.records_layout_text);
			
			txtDifficulty.setWidth(1);
			txtTime.setWidth(1);
			txtMoves.setWidth(1);
			
			txtDifficulty.setText(params[0]);
			txtTime.setText(params[1]);
			txtMoves.setText(params[2]);
			
			row.addView(txtDifficulty);
			row.addView(txtTime);
			row.addView(txtMoves);
			
			RecordsFrag.this.tableLayout.addView(row);
		}
		@Override
		protected void onPostExecute(Boolean result) {
			if(!result) {
				tableLayout.setVisibility(View.GONE);
				txtNorecords.setVisibility(View.VISIBLE);
			}
		}
	}
}

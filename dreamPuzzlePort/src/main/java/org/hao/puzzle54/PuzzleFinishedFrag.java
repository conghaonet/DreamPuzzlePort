package org.hao.puzzle54;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.hao.database.DBHelperCustom;
import org.hao.database.DBHelperScore;
import org.hao.puzzle54.custom.CustomPicEntity;
import org.hh.puzzle.port54.hall.R;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
	
public class PuzzleFinishedFrag extends Fragment {
	private static final String TAG = PuzzleFinishedFrag.class.getName();
	private PuzzleActivityCallBack mCallBack;

    /**
	 * 当该Fragment被添加,显示到Activity时调用该方法
	 * 在此判断显示到的Activity是否已经实现了接口
	 */
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		Log.d(TAG, TAG+"--onAttach");
		if (!(activity instanceof PuzzleActivityCallBack)) {
			throw new IllegalStateException("PuzzleFinishedFrag所在的Activity必须实现PuzzleActivityCallBack接口");
		}
		mCallBack = (PuzzleActivityCallBack)activity;
	}
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, TAG+"--onCreate");
    }
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.d(TAG, TAG+"--onCreateView");
		View rootView = inflater.inflate(R.layout.puzzle_finished_fragment, container, false);
		if(getArguments().containsKey(MyBundleData.SCORE_ID)) {
            long scoreId = getArguments().getLong(MyBundleData.SCORE_ID);
			final ScoreEntity scoreEntity = DBHelperScore.getInstance(this.mCallBack.getMyApp()).getScore(scoreId, null);
//			FrameLayout rootLayout = (FrameLayout)rootView.findViewById(R.id.finished_root_layout);
			TextView txtDifficulty = (TextView)rootView.findViewById(R.id.finished_difficulty_value);
			TextView txtPlayerTime = (TextView)rootView.findViewById(R.id.finished_player_time_value);
			TextView txtPlayerMoves = (TextView)rootView.findViewById(R.id.finished_player_moves_value);
			TextView txtBestTime = (TextView)rootView.findViewById(R.id.finished_best_time_value);
			TextView txtBestMoves = (TextView)rootView.findViewById(R.id.finished_best_moves_value);
			txtDifficulty.setText(scoreEntity.getRows()+"x"+scoreEntity.getCols());
			txtPlayerTime.setText(AppTools.formatElapsedTimeSec2String(scoreEntity.getElapsedTime()));
			txtPlayerMoves.setText(""+scoreEntity.getSteps());
			txtBestTime.setText(AppTools.formatElapsedTimeSec2String(scoreEntity.getBestTime()));
			txtBestMoves.setText(""+scoreEntity.getStepsOfBestTime());
			if(scoreEntity.getElapsedTime() == scoreEntity.getBestTime() && scoreEntity.getSteps() == scoreEntity.getStepsOfBestTime()) {
				TextView txtPlayerLabel = (TextView)rootView.findViewById(R.id.finished_player_label);
				txtPlayerLabel.setVisibility(View.GONE);
				LinearLayout playerLayout = (LinearLayout)rootView.findViewById(R.id.finished_player_layout);
				playerLayout.setVisibility(View.GONE);
			}
			ImageButton btnNext = (ImageButton)rootView.findViewById(R.id.finished_btn_next);
			ImageButton btnRetry = (ImageButton)rootView.findViewById(R.id.finished_btn_retry);
			ImageButton btnBack2grid = (ImageButton)rootView.findViewById(R.id.finished_btn_grid);
			ImageButton btnShare = (ImageButton)rootView.findViewById(R.id.finished_btn_share);
			ImageButton btnWallpaper = (ImageButton)rootView.findViewById(R.id.finished_btn_wallpaper);
//			rootLayout.setOnClickListener(new OnClickListener(){
//				@Override
//				public void onClick(View v) {
//					mCallBack.gotoNextPuzzle(false);
//				}
//			});
			btnNext.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View v) {
					mCallBack.gotoNextPuzzle(false);
				}
			});
			btnRetry.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View v) {
					mCallBack.gotoNextPuzzle(true);
				}
			});
			btnBack2grid.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View v) {
					mCallBack.openPackageGridActivity();
				}
			});
			btnShare.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View v) {
					Intent intent = PuzzleFinishedFrag.this.mCallBack.getMyApp().saveSharePicture(scoreEntity.getPicIndex(), scoreEntity.getCode());
					if(intent != null) startActivity(Intent.createChooser(intent, getString(R.string.app_name)));
				}
			});
			btnWallpaper.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View v) {
					BufferedInputStream input = null;
					try {
						if(AppConstants.CUSTOM_PACKAGE_CODE.equals(scoreEntity.getCode())) {
							CustomPicEntity currentCustomEntity = DBHelperCustom.getInstance(PuzzleFinishedFrag.this.mCallBack.getMyApp()).getById(scoreEntity.getPicIndex(), null);
							input = new BufferedInputStream(new FileInputStream(PuzzleFinishedFrag.this.mCallBack.getMyApp().getCustomPicture(currentCustomEntity.getImageName())));
						} else {
							if(mCallBack.isInnerPics()) {
								input = new BufferedInputStream(mCallBack.getMyApp().getAssets().open(AppConstants.PICS_FOLDER_IN_ZIP + File.separator + mCallBack.getMyApp().getInnerPics().get((int)scoreEntity.getPicIndex())));
							} else {
								input = new BufferedInputStream(PuzzleFinishedFrag.this.mCallBack.getMyApp().getPackageZipFile(scoreEntity.getCode()).getInputStream(PuzzleFinishedFrag.this.mCallBack.getMyApp().getCurrentZipEntries(scoreEntity.getCode()).get((int)scoreEntity.getPicIndex())));
							}
						}
						PuzzleFinishedFrag.this.mCallBack.getMyApp().setWallpaper(input);
						Toast.makeText(PuzzleFinishedFrag.this.getActivity(), R.string.FINISHED_WALLPAPER_SUCCESSFULLY, Toast.LENGTH_SHORT).show();
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						if(input != null) {
							try {
								input.close();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}
				}
			});
			
		}
		return rootView;
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
		mCallBack = null;
	}
	
}

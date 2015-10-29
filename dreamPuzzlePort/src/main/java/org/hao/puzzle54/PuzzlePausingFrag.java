package org.hao.puzzle54;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import org.hh.puzzle.port54.hall.R;
	
public class PuzzlePausingFrag extends Fragment {
	private static final String TAG = PuzzlePausingFrag.class.getName();
	private boolean musicOn;
	private boolean soundOn;
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
			throw new IllegalStateException("PuzzlePausingFrag所在的Activity必须实现PuzzleActivityCallBack接口");
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
		View rootView = inflater.inflate(R.layout.puzzle_pausing_fragment, container, false);
		final ImageButton btnMusic = (ImageButton)rootView.findViewById(R.id.pausing_btn_music);
		final ImageButton btnSound = (ImageButton)rootView.findViewById(R.id.pausing_btn_sound);
//		ImageButton btnContinue = (ImageButton)rootView.findViewById(R.id.pausing_btn_continue);
		FrameLayout pausingLayout = (FrameLayout)rootView.findViewById(R.id.pausing_root_layout);
		ImageButton btnRetry = (ImageButton)rootView.findViewById(R.id.pausing_btn_retry);
		ImageButton btnBack2Grid = (ImageButton)rootView.findViewById(R.id.pausing_btn_back2grid);
		this.musicOn = AppPrefUtil.isPlayMusic(this.mCallBack.getMyApp(), null);
		this.soundOn = AppPrefUtil.isPlaySound(this.mCallBack.getMyApp(), null);
		if(this.musicOn) btnMusic.setImageResource(R.drawable.btn_music_on);
		else btnMusic.setImageResource(R.drawable.btn_music_off);
		if(this.soundOn) btnSound.setImageResource(R.drawable.btn_sound_on);
		else btnSound.setImageResource(R.drawable.btn_sound_off);
		btnSound.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				PuzzlePausingFrag.this.soundOn = !PuzzlePausingFrag.this.soundOn; 
				if(PuzzlePausingFrag.this.soundOn) btnSound.setImageResource(R.drawable.btn_sound_on);
				else btnSound.setImageResource(R.drawable.btn_sound_off);
				SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(PuzzlePausingFrag.this.mCallBack.getMyApp());
				Editor editor = pref.edit();
				AppPrefUtil.setPlaySound(PuzzlePausingFrag.this.mCallBack.getMyApp(), editor, PuzzlePausingFrag.this.soundOn);
				editor.apply();
				mCallBack.setSoundOn(PuzzlePausingFrag.this.soundOn);
				PuzzlePausingFrag.this.getActivity().onBackPressed();
			}
		});
		btnMusic.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				PuzzlePausingFrag.this.musicOn = !PuzzlePausingFrag.this.musicOn; 
				if(PuzzlePausingFrag.this.musicOn) btnMusic.setImageResource(R.drawable.btn_music_on);
				else btnMusic.setImageResource(R.drawable.btn_music_off);
				SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(PuzzlePausingFrag.this.mCallBack.getMyApp());
				Editor editor = pref.edit();
				AppPrefUtil.setPlayMusic(PuzzlePausingFrag.this.mCallBack.getMyApp(), editor, PuzzlePausingFrag.this.musicOn);
				editor.apply();
				mCallBack.setMusicOn(PuzzlePausingFrag.this.musicOn);
				PuzzlePausingFrag.this.getActivity().onBackPressed();
			}
		});
		pausingLayout.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				PuzzlePausingFrag.this.getActivity().onBackPressed();
			}
		});
		btnRetry.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mCallBack.gotoNextPuzzle(true);
			}
		});
		btnBack2Grid.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mCallBack.openPackageGridActivity();
			}
		});
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
		mCallBack = null;
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

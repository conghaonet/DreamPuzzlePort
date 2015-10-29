package org.hao.puzzle54;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings.SettingNotFoundException;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import org.hao.puzzle54.custom.CustomGridActivity;
import org.hao.puzzle54.services.MyAdsReceiver;
import org.hh.puzzle.port54.hall.R;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class PuzzleActivity extends ActionBarActivity implements PuzzleActivityCallBack {
	private static final String TAG = PuzzleActivity.class.getName();
	private static final int HANDLER_MSG_REFRESH_ACTION_BAR = 101;
	private static final int HANDLER_MSG_REFRESH_TITLE = 102;
	private static final int HANDLER_MSG_REFRESH_SUB_TITLE = 103;
	private static final int HANDLER_MSG_REFRESH_HINT_VIEW = 104;
	private static final int HANDLER_MSG_REFRESH_HINT_MOVE = 105;
	private static final int HANDLER_MSG_AD_CHANGED = 201;
	private FragmentManager fragmentManager;
	private MyApp myApp;
	private AdView adView;
	private FrameLayout adLayout;
	private ActionBar actionBar;
	private MyHandler mHandler;
	private long currentPicIndex;
	private String packageCode;
	private boolean isContinue;
	private boolean isCustom;
	private SoundPool soundPool;
	private HashMap<Integer, Integer> soundPoolMap;
	private boolean musicOn;
	private boolean soundOn;
	private MediaPlayer mp;
    private int currentMusicPosition;
//	private int steps;
	private int intHintViewTimes;
	private int intHintMoveTimes;
	private Timer elapsedTimer;
	private long elapsedTimeSec;
	private PuzzleFrag fragPuzzle;
	private PuzzlePausingFrag fragPausing;
	private PuzzleFinishedFrag fragFinished;
	private boolean blnActivityHasFocus;
	private MyAdsReceiver myAdsReceiver;
	private String strSubTitle;
	private boolean isInnerPics;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.puzzle_activity);
		this.myApp = (MyApp)getApplicationContext();
        setSupportActionBar((Toolbar)findViewById(R.id.puzzle_mytoolbar));
		actionBar = getSupportActionBar();
		actionBar.show();
		actionBar.setDisplayShowTitleEnabled(true);
		actionBar.setTitle(null);
		actionBar.setSubtitle(null);
		actionBar.setDisplayHomeAsUpEnabled(false);
		actionBar.setHomeButtonEnabled(false);
		actionBar.setDisplayShowHomeEnabled(false);
		adLayout = (FrameLayout)findViewById(R.id.ad_layout);
		setBanner();
		this.mHandler = new MyHandler(this);
		myAdsReceiver = new MyAdsReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				PuzzleActivity.this.mHandler.sendMessage(PuzzleActivity.this.mHandler.obtainMessage(PuzzleActivity.HANDLER_MSG_AD_CHANGED));
			}
		};
		fragmentManager = getSupportFragmentManager();
		if (savedInstanceState == null) {
			if(this.getIntent().getExtras().containsKey(MyBundleData.PACKAGE_CODE) 
					&& this.getIntent().getExtras().containsKey(MyBundleData.PICTURE_INDEX)
					&& this.getIntent().getExtras().containsKey(MyBundleData.PUZZLE_IS_CONTINUE)) {
				this.packageCode = this.getIntent().getExtras().getString(MyBundleData.PACKAGE_CODE);
				this.currentPicIndex = this.getIntent().getExtras().getLong(MyBundleData.PICTURE_INDEX);
				this.isContinue = this.getIntent().getExtras().getBoolean(MyBundleData.PUZZLE_IS_CONTINUE);
				this.isInnerPics = this.myApp.isInnerPics(this.packageCode);
			}
		} else {
			this.packageCode = savedInstanceState.getString(MyBundleData.PACKAGE_CODE);
			this.currentPicIndex = savedInstanceState.getLong(MyBundleData.PICTURE_INDEX);
			this.isContinue = savedInstanceState.getBoolean(MyBundleData.PUZZLE_IS_CONTINUE);
			this.isInnerPics = this.myApp.isInnerPics(this.packageCode);
			fragPuzzle = (PuzzleFrag)fragmentManager.findFragmentByTag(PuzzleFrag.class.getName());
			if(fragPuzzle != null) {
				FragmentTransaction trans = fragmentManager.beginTransaction();
				trans.remove(fragPuzzle);
				trans.commit();
			}
		}
		this.blnActivityHasFocus = false;
        this.isCustom = AppConstants.CUSTOM_PACKAGE_CODE.equals(this.packageCode);
		this.musicOn = AppPrefUtil.isPlayMusic(this.myApp, null);
		this.soundOn = AppPrefUtil.isPlaySound(this.myApp, null);
		this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
		this.soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC,100);
		this.soundPoolMap = new HashMap<Integer, Integer>();
		this.soundPoolMap.put(1, soundPool.load(this, R.raw.sound, 1));
		
		upBrightness();
		IntentFilter filterAdChanged = new IntentFilter();
		filterAdChanged.addAction(this.getPackageName()+MyAdsReceiver.ACTION_AD_CHANGED);
		registerReceiver(myAdsReceiver, filterAdChanged);
	}
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		savedInstanceState.putLong(MyBundleData.PICTURE_INDEX, this.currentPicIndex);
		savedInstanceState.putString(MyBundleData.PACKAGE_CODE, this.packageCode);
		savedInstanceState.putBoolean(MyBundleData.PUZZLE_IS_CONTINUE, this.isContinue);
        super.onSaveInstanceState(savedInstanceState);
        Log.d(TAG, "--onSaveInstanceState");
    }
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		if(hasFocus && !this.blnActivityHasFocus) {
			this.blnActivityHasFocus = hasFocus;
			openPuzzle();
		}
	}
	@Override
	public void onStart() {
		super.onStart();
	}
	@Override
	public void onResume() {
		super.onResume();
		if (adView != null) adView.resume();
		if(this.musicOn) {
			playMusic();
		}
	}
	@Override
	public void onPause() {
		if(this.musicOn) stopMusic();
		if(adView != null) adView.pause();
		super.onPause();
	}
	static class MyHandler extends Handler {
        WeakReference<PuzzleActivity> mActivity;
        MyHandler(PuzzleActivity activity) {
        	mActivity = new WeakReference<PuzzleActivity>(activity);
        }
        @Override
        public void handleMessage(Message msg) {
        	PuzzleActivity theActivity = mActivity.get();
        	if(theActivity == null) return;
			switch (msg.what) {
			case HANDLER_MSG_REFRESH_ACTION_BAR:
				theActivity.supportInvalidateOptionsMenu();
				break;
			case HANDLER_MSG_REFRESH_TITLE:
				theActivity.actionBar.setTitle(theActivity.getString(R.string.puzzle_time_label)+" "+AppTools.formatElapsedTimeSec2String(theActivity.elapsedTimeSec));
				break;
			case HANDLER_MSG_REFRESH_SUB_TITLE:
//				theActivity.actionBar.setSubtitle(theActivity.getString(R.string.puzzle_moves_label)+" "+theActivity.steps);
				theActivity.actionBar.setSubtitle(theActivity.strSubTitle);
				break;
			case HANDLER_MSG_REFRESH_HINT_VIEW:
				theActivity.supportInvalidateOptionsMenu();
				break;
			case HANDLER_MSG_REFRESH_HINT_MOVE:
				theActivity.supportInvalidateOptionsMenu();
				break;
			case HANDLER_MSG_AD_CHANGED:
				theActivity.setBanner();
			default:
				break;
			}
        }
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.puzzle_menu, menu);
        return true;
	}
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		MenuItem menuItemMove = menu.findItem(R.id.actionbar_puzzle_move);
		MenuItem menuItemView = menu.findItem(R.id.actionbar_puzzle_view);
		menuItemMove.setTitle(getString(R.string.puzzle_hints_move_label)+this.intHintMoveTimes);
		menuItemView.setTitle(getString(R.string.puzzle_hints_view_label)+this.intHintViewTimes);
		return true;
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem menu) {
		if(this.fragFinished != null && this.fragFinished.isResumed()) return false;
		else if(this.fragPausing != null && this.fragPausing.isResumed()) return false;
		switch(menu.getItemId()) {
		case R.id.actionbar_puzzle_view:
			if(this.intHintViewTimes > 0) {
				if(fragPuzzle != null && fragPuzzle.isResumed()) fragPuzzle.showHintView();
			}
			break;
		case R.id.actionbar_puzzle_move:
			if(this.intHintMoveTimes > 0) {
				if(fragPuzzle != null && fragPuzzle.isResumed()) fragPuzzle.showHintMove();
			}
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(menu);
	}
	@Override
    public void onBackPressed() {
		if(fragFinished != null && fragFinished.isResumed()) {
			openPackageGridActivity();
		} else if(fragPausing != null && fragPausing.isResumed()) {
			fragmentManager.popBackStack();
//			FragmentTransaction trans = fragmentManager.beginTransaction();
//			trans.remove(fragPausing);
//			trans.commitAllowingStateLoss();
		} else {
			openPuzzlePausing();
		}
	}
	@Override
	public void onDestroy() {
		if(this.adView != null) this.adView.destroy();
		unregisterMyReceivers();
		super.onDestroy();
	}
	private void unregisterMyReceivers() {
		try {
			if(this.myAdsReceiver != null) unregisterReceiver(this.myAdsReceiver);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	@Override
	public int getActionBarHeight() {
		return this.actionBar.getHeight();
	}
//	@Override
//	public void refreshActivity(int handlerMsg) {
//		this.mHandler.sendMessage(this.mHandler.obtainMessage(handlerMsg));
//	}
	@Override
	public void setElapsedTimeSec(long elapsedTimeSec) {
		this.elapsedTimeSec = elapsedTimeSec;
		this.mHandler.sendMessage(this.mHandler.obtainMessage(PuzzleActivity.HANDLER_MSG_REFRESH_TITLE));
	}
	@Override
	public long getElapsedTimeSec() {
		return this.elapsedTimeSec;
	}
//	@Override
//	public void setSteps(int steps, boolean isPlaySound) {
//		this.steps = steps;
//		if(isPlaySound) playSound();
//		this.mHandler.sendMessage(this.mHandler.obtainMessage(PuzzleActivity.HANDLER_MSG_REFRESH_SUB_TITLE));
//	}
	@Override
	public void updateActionBarSubTitle(int steps, int correctPiecesSum, int totalPieces, boolean isPlaySound) {
		if(isPlaySound) playSound();
		this.strSubTitle = String.format(getString(R.string.puzzle_sub_title), steps, correctPiecesSum, totalPieces);
		this.mHandler.sendMessage(this.mHandler.obtainMessage(PuzzleActivity.HANDLER_MSG_REFRESH_SUB_TITLE));
	}
	@Override
	public void setHintMoveTimes(int times) {
		this.intHintMoveTimes = times;
		this.mHandler.sendMessage(this.mHandler.obtainMessage(PuzzleActivity.HANDLER_MSG_REFRESH_HINT_MOVE));
	}
	@Override
	public void setHintViewTimes(int times) {
		this.intHintViewTimes = times;
		this.mHandler.sendMessage(this.mHandler.obtainMessage(PuzzleActivity.HANDLER_MSG_REFRESH_HINT_VIEW));
	}
	@Override
	public void setMusicOn(boolean musicOn) {
		this.musicOn = musicOn;
		if(this.musicOn) playMusic();
		else stopMusic();
	}
	@Override
	public void setSoundOn(boolean soundOn) {
		this.soundOn = soundOn;
	}
	@Override
	public void gotoNextPuzzle(boolean isRetry) {
		if(fragmentManager.findFragmentByTag(PuzzleFinishedFrag.class.getName()) != null) {
			fragmentManager.popBackStack();
		}
		if(fragmentManager.findFragmentByTag(PuzzlePausingFrag.class.getName()) != null) {
			fragmentManager.popBackStack();
		}
		if(fragPuzzle != null) {
			fragPuzzle.gotoNextPuzzle(isRetry);
		}
	}
//	@Override
	public void openPuzzle() {
		if(fragPuzzle == null) fragPuzzle = new PuzzleFrag();
		Bundle bundle = new Bundle();
        bundle.putString(MyBundleData.PACKAGE_CODE, this.packageCode);
        bundle.putLong(MyBundleData.PICTURE_INDEX, this.currentPicIndex);
        bundle.putBoolean(MyBundleData.PUZZLE_IS_CONTINUE, this.isContinue);
		fragPuzzle.setArguments(bundle);
		FragmentTransaction trans = fragmentManager.beginTransaction();
		trans.replace(R.id.fragment_container, fragPuzzle, PuzzleFrag.class.getName());
		trans.commitAllowingStateLoss();
	}
	@Override
	public void updateCurrentPicIndex(long currentPicIndex) {
		this.currentPicIndex = currentPicIndex;
	}
	@Override
	public void openPuzzleFinished(long scoreId) {
		if(fragmentManager.findFragmentByTag(PuzzlePausingFrag.class.getName()) != null) {
			fragmentManager.popBackStack();
		}
		fragFinished = new PuzzleFinishedFrag();
		Bundle bundle = new Bundle();
        bundle.putLong(MyBundleData.SCORE_ID, scoreId);
        fragFinished.setArguments(bundle);
		FragmentTransaction trans = fragmentManager.beginTransaction();
		trans.setCustomAnimations(R.anim.push_top_in, R.anim.push_bottom_out, R.anim.push_top_in, R.anim.push_bottom_out);
		trans.add(R.id.fragment_container, fragFinished, PuzzleFinishedFrag.class.getName());
		trans.addToBackStack(null);
		trans.commitAllowingStateLoss();
	}
	public boolean isInnerPics() {
		return this.isInnerPics;
	}
	public void openPuzzlePausing() {
		fragPausing = new PuzzlePausingFrag();
		FragmentTransaction trans = fragmentManager.beginTransaction();
		trans.setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out);
		trans.add(R.id.fragment_container, fragPausing, PuzzlePausingFrag.class.getName());
		trans.addToBackStack(null);
		trans.commitAllowingStateLoss();
	}
	@Override
	public void openPackageGridActivity() {
		Intent intent = new Intent();
		if(this.isCustom) {
			intent.setClass(this, CustomGridActivity.class);
		} else {
			Bundle bundle = new Bundle();
			bundle.putString(MyBundleData.PACKAGE_CODE, this.packageCode);
			intent.setClass(this, PackageGridActivity.class);
			intent.putExtras(bundle);
		}
		startActivity(intent);
		overridePendingTransition(R.anim.push_top_in, R.anim.push_top_out);
		finish();
	}
	@Override
	public void openNewGame() {
		Intent intent = new Intent();
		intent.setClass(this, PackageGridActivity.class);
		startActivity(intent);
		overridePendingTransition(R.anim.push_top_in, R.anim.push_top_out);
		finish();
	}
	@Override
	public MyApp getMyApp() {
		return this.myApp;
	}
	private void upBrightness() {
		final float defaultBrightness = 0.8f;
		float brightness = 0;
		try {
			brightness = android.provider.Settings.System.getInt(getContentResolver(), android.provider.Settings.System.SCREEN_BRIGHTNESS);
		} catch (SettingNotFoundException e) {
			e.printStackTrace();
		}
		if(brightness/255f > defaultBrightness) brightness = brightness/255f;
		else brightness = defaultBrightness;
		WindowManager.LayoutParams lp = getWindow().getAttributes();
		lp.screenBrightness = brightness;
		getWindow().setAttributes(lp);
	}
	private void setBanner() {
		if(adView != null) adLayout.removeView(adView);
		try {
			adView = new AdView(this);
			adView.setAdSize(myApp.getAdSizeForAdmob());
			adView.setAdUnitId(AppPrefUtil.getAdBannerId(this, null));
			adView.setBackgroundResource(R.drawable.bg_ad_drawable);
			FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(myApp.getAdViewMeasureWidth(), myApp.getAdViewMeasureHeight());
			params.gravity = Gravity.CENTER;
			adLayout.addView(adView, params);
			if(this.myApp.hasAds()) {
				AdRequest.Builder builder = new AdRequest.Builder();
//				builder.addTestDevice("96EDE742C567059B15AEE8871B8A9B21");//nexus5
				adView.loadAd(builder.build());
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	@Override
	public void startTimer(boolean blnStart) {
		if(this.elapsedTimer != null) {
			try{
				this.elapsedTimer.cancel();
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		if(blnStart) {
			this.elapsedTimer = new Timer();
			this.elapsedTimer.scheduleAtFixedRate(new ElapsedTimeTimerTask(), 1000, 1000);
		} else {
			this.elapsedTimer = null;
		}
	}

	private void playMusic() {
		new InitialMusic().execute(1);
	}
	private void pauseMusic() {
		new InitialMusic().execute(0);
	}
	private void stopMusic() {
		new InitialMusic().execute(-1);
	}
	private void playSound() {
		if(this.soundOn) {
			if(soundPool == null) {
				this.soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC,100);
			}
			if(soundPoolMap == null) {
				this.soundPoolMap = new HashMap<Integer, Integer>();
				this.soundPoolMap.put(1, soundPool.load(this, R.raw.sound, 1));
			}
			int volume = ((AudioManager)getSystemService(Context.AUDIO_SERVICE)).getStreamVolume(AudioManager.STREAM_MUSIC);
			try{
				this.soundPool.play(soundPoolMap.get(1), volume, volume, 1, 0, 1f);
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}

	class InitialMusic extends AsyncTask<Integer, Void, Void> {
		/**
		 * @param operations -1: stop; 0: pause; 1: play; 
		 */
		@Override
		protected Void doInBackground(Integer... operations) {
			switch(operations[0]) {
			case -1:
				if(PuzzleActivity.this.mp == null) return null;
				try{
					if(PuzzleActivity.this.mp.isPlaying()) {
						PuzzleActivity.this.currentMusicPosition = PuzzleActivity.this.mp.getCurrentPosition();
					}
					PuzzleActivity.this.mp.stop();
					PuzzleActivity.this.mp.release();
					PuzzleActivity.this.mp = null;
				} catch(Exception e) {
					e.printStackTrace();
				}
				break;
			case 0:
				if(PuzzleActivity.this.mp == null) return null;
				try{
					if(PuzzleActivity.this.mp.isPlaying()) {
						PuzzleActivity.this.mp.pause();
						PuzzleActivity.this.currentMusicPosition = PuzzleActivity.this.mp.getCurrentPosition();
					}
				} catch(Exception e) {
					e.printStackTrace();
				}
				break;
			case 1:
				try {
					if(PuzzleActivity.this.mp == null) {
						PuzzleActivity.this.mp = MediaPlayer.create(PuzzleActivity.this, R.raw.music);
						PuzzleActivity.this.mp.setLooping(true);
					}
					if(PuzzleActivity.this.mp.isPlaying()) return null;
					if(PuzzleActivity.this.currentMusicPosition > 0 ) mp.seekTo(PuzzleActivity.this.currentMusicPosition);
					mp.start();
				} catch(Exception e) {
					PuzzleActivity.this.mp = null;
					e.printStackTrace();
				}
				break;
			}
			return null;
		}
		
	}
	class ElapsedTimeTimerTask extends TimerTask {
		@Override
		public void run() {
			final int secondsInDay = 86399;	//???86400??
			if(PuzzleActivity.this.elapsedTimeSec >= secondsInDay) {
				PuzzleActivity.this.elapsedTimeSec = secondsInDay;
				PuzzleActivity.this.startTimer(false);
			} else {
				++PuzzleActivity.this.elapsedTimeSec;
				setElapsedTimeSec(PuzzleActivity.this.elapsedTimeSec);
			}
		}
	}
}

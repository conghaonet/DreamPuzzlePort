package org.hao.puzzle54;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.flurry.android.FlurryAgent;

import org.hao.cache.CacheKeys;
import org.hao.cache.DiskLruCache;
import org.hao.cache.PuzzleAsyncImgLoader;
import org.hao.database.DBHelperCustom;
import org.hao.database.DBHelperMorepuzzles;
import org.hao.database.DBHelperScore;
import org.hao.puzzle54.custom.CustomPicEntity;
import org.hh.puzzle.port54.hall.R;

import java.io.File;
import java.util.Date;
import java.util.List;
	
public class PuzzleFrag extends Fragment implements OnTouchListener {
	private static final String TAG = PuzzleFrag.class.getName();
	private float touchedViewX;
	private float touchedViewY;
	private int steps;
	private int cols;
	private int rows;
	private int intHintViewTimes;
	private int intHintMoveTimes;
	private long currentPicIndex;
	private int pieceWidth;
	private int pieceHeight;
	private double diagonalLengthOfPiece;
	private int touchedPieceIndexTag = -1;
	private boolean isContinue;
	private boolean isFinished;
	private boolean isCustom;
	private boolean isInitialized;
	private RelativeLayout relativeLayoutPieces;
	private RelativeLayout fullImageLayout;
	private ImageView arrRandomPieces[][];
	private Animation fullImageAnimation;
	private boolean moveAnimationIsRunning;
	private ProgressBar mProgressBar;
	private Bitmap wrongBorderBitmap;
	private Bitmap rightBorderBitmap;
	private Integer processedSubCount = 0;
	//for DiskLruCache
	private DiskLruCache mDiskLruCache;
    //	private final Object mDiskCacheLock = new Object();
//	private Boolean mDiskCacheStarting = Boolean.valueOf(true);
	private PuzzleAsyncImgLoader asyncPiecesImgLoader;
	private InitialTask initialTask;
	private String packageCode;
	private List<CustomPicEntity> listCustomEntity;
    private PuzzleActivityCallBack mCallBack;
	private int correctPiecesSum;
	private TextView txtCorrectPromotion;

	/**
	 * 当该Fragment被添加,显示到Activity时调用该方法
	 * 在此判断显示到的Activity是否已经实现了接口
	 */
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		Log.d(TAG, TAG+"--onAttach");
		if (!(activity instanceof PuzzleActivityCallBack)) {
			throw new IllegalStateException("PuzzleFrag所在的Activity必须实现PuzzleActivityCallBack接口");
		}
		mCallBack = (PuzzleActivityCallBack)activity;
	}
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        Log.d(TAG, TAG+"--onCreate");
    }
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		if(!this.isFinished) saveScore();
        super.onSaveInstanceState(savedInstanceState);
        Log.d(TAG, "--onSaveInstanceState");
//        savedInstanceState.putBoolean("MyBoolean", true);
    }

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.d(TAG, TAG+"--onCreateView");
		View rootView = inflater.inflate(R.layout.puzzle_fragment, container, false);
		mProgressBar = (ProgressBar)rootView.findViewById(R.id.progressbar);
		mProgressBar.setVisibility(View.GONE);
		if(getArguments().containsKey(MyBundleData.PACKAGE_CODE) 
				&& getArguments().containsKey(MyBundleData.PICTURE_INDEX)
				&& getArguments().containsKey(MyBundleData.PUZZLE_IS_CONTINUE)) {
			this.packageCode = getArguments().getString(MyBundleData.PACKAGE_CODE);
			this.currentPicIndex = getArguments().getLong(MyBundleData.PICTURE_INDEX);
			this.isContinue = getArguments().getBoolean(MyBundleData.PUZZLE_IS_CONTINUE);
            this.isCustom = AppConstants.CUSTOM_PACKAGE_CODE.equals(this.packageCode);
			this.relativeLayoutPieces = (RelativeLayout)rootView.findViewById(R.id.relativeLayoutPieces);
			this.fullImageLayout = (RelativeLayout)rootView.findViewById(R.id.relativeLayoutFullImagePieces);
			this.txtCorrectPromotion = (TextView)rootView.findViewById(R.id.puzzle_correct_promotion_textview);
			this.txtCorrectPromotion.setVisibility(View.GONE);
		}
		return rootView;
	}
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		Log.d(TAG, TAG+"--onActivityCreated");
		File cacheDir = DiskLruCache.getDiskCacheDir(this.mCallBack.getMyApp(), CacheKeys.PIECES_CACHE_FOLDER);
		initialTask = new InitialTask();
		initialTask.execute(cacheDir);
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
        if(this.isInitialized && !this.isFinished) {
        	mCallBack.startTimer(true);
        }
    }
	@Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, TAG+"--onPause");
        if(this.isInitialized && !this.isFinished) {
        	mCallBack.startTimer(false);
        }
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
		if(!this.isFinished) saveScore();
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
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if(!this.isInitialized) return true;
		if(v.getTag(R.id.piece_index) == null) return true;
		if(this.touchedPieceIndexTag <= -1 || this.touchedPieceIndexTag == (Integer)v.getTag(R.id.piece_index)) {
			if(!this.isFinished) {
				if((this.fullImageAnimation == null || this.fullImageAnimation.hasEnded()) && !this.moveAnimationIsRunning) {
					int[] viewRowCol = AppTools.getRowColIndex((Integer)v.getTag(R.id.piece_index), this.cols);
					if(this.arrRandomPieces[viewRowCol[0]][viewRowCol[1]].getTag(R.id.piece_index) == v.getTag(R.id.piece_index)) return true;
					switch (event.getActionMasked()) {
					case MotionEvent.ACTION_DOWN:
						this.touchedPieceIndexTag = (Integer) v.getTag(R.id.piece_index);
						this.touchedViewX = event.getX(event.getActionIndex());
						this.touchedViewY = event.getY(event.getActionIndex());
						v.bringToFront();
						break;
					case MotionEvent.ACTION_UP:
						setMovedPiece2Target(v);
						break;
					case MotionEvent.ACTION_MOVE:
						float newLeft = event.getRawX() - this.touchedViewX - this.relativeLayoutPieces.getLeft();
						float newTop = event.getRawY() - this.touchedViewY - this.relativeLayoutPieces.getTop() - mCallBack.getActionBarHeight();
						float newRight = newLeft + v.getWidth();
						float newBottom = newTop + v.getHeight();
						if (newLeft < 0) {
							newLeft = 0;
						} else if (newRight > this.relativeLayoutPieces.getWidth()) {
							newRight = this.relativeLayoutPieces.getWidth();
							newLeft = newRight - v.getWidth();
						}
						if (newTop < 0) {
							newTop = 0;
						} else if (newBottom > this.relativeLayoutPieces.getHeight()) {
							newBottom = this.relativeLayoutPieces.getHeight();
							newTop = newBottom - v.getHeight();
						}
						RelativeLayout.LayoutParams relativeParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
						relativeParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
						relativeParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
						relativeParams.leftMargin=(int)newLeft;
						relativeParams.topMargin=(int)newTop;
						v.setLayoutParams(relativeParams);
						break;
					case MotionEvent.ACTION_POINTER_DOWN:
						break;
					case MotionEvent.ACTION_POINTER_UP:
						break;
					default:
						break;
					}
				}
			}
		}
		return true;
	}
	private void setMovedPiece2Target(View touchedView) {
		int rowIndexOfTargetView;
		int colIndexOfTargetView;
		int rowIndexOfTouchedView = -1;
		int colIndexOfTouchedView = -1;
		this.moveAnimationIsRunning = true;
		for(int i=0;i<this.arrRandomPieces.length;i++) {
			for(int j=0;j<this.arrRandomPieces[0].length;j++) {
				if(this.arrRandomPieces[i][j].getTag(R.id.piece_index) == touchedView.getTag(R.id.piece_index)) {
					rowIndexOfTouchedView = i;
					colIndexOfTouchedView = j;
					break;
				}
			}
			if(rowIndexOfTouchedView > -1) break;
		}
		int xYuShu = touchedView.getLeft()%this.pieceWidth;
		int yYuShu = touchedView.getTop()%this.pieceHeight;
		if(xYuShu*2>this.pieceWidth) {
			colIndexOfTargetView = (int) Math.ceil((double)touchedView.getLeft()/(double)this.pieceWidth);
		} else colIndexOfTargetView = (int) Math.floor((double)touchedView.getLeft()/(double)this.pieceWidth);
		if(yYuShu*2>this.pieceHeight) {
			rowIndexOfTargetView = (int) Math.ceil((double)touchedView.getTop()/(double)this.pieceHeight);
		} else rowIndexOfTargetView = (int) Math.floor((double)touchedView.getTop()/(double)this.pieceHeight);
		ImageView targetView = this.arrRandomPieces[rowIndexOfTargetView][colIndexOfTargetView];
		if(touchedView.getTag(R.id.piece_index) != targetView.getTag(R.id.piece_index)
				&& AppTools.getPieceIndexTag(rowIndexOfTargetView, colIndexOfTargetView, this.cols) != (Integer)targetView.getTag(R.id.piece_index)) {

			this.steps++;
			targetView.bringToFront();
			touchedView.bringToFront();
			autoMovePieceAfterTouch(touchedView, colIndexOfTargetView, rowIndexOfTargetView, 0, false);
			autoMovePieceAfterTouch(targetView, colIndexOfTouchedView, rowIndexOfTouchedView, 1, false);
		} else {
			autoMovePieceAfterTouch(touchedView, colIndexOfTouchedView, rowIndexOfTouchedView, 1, true);
		}
	}
	private void autoMovePieceAfterTouch(View view, int toColIndex, int toRowIndex, int startOffset, boolean isReset) {
		long animationMoveX = -(view.getLeft() - this.pieceWidth * toColIndex);
		long animationMoveY = -(view.getTop() - this.pieceHeight * toRowIndex);
		Animation autoMoveAnimation = new TranslateAnimation(0, animationMoveX, 0, animationMoveY);
		autoMoveAnimation.setAnimationListener(new AutoMoveAnimationListener(view, toColIndex, toRowIndex, startOffset));
		autoMoveAnimation.setStartOffset(startOffset);
		long longMoveDuration = getMoveDuration(animationMoveX, animationMoveY);
		if(isReset && longMoveDuration < getResources().getInteger(R.integer.puzzle_piece_move_unit_duration)) longMoveDuration = getResources().getInteger(R.integer.puzzle_piece_move_unit_duration);
		else if(isReset && longMoveDuration > getResources().getInteger(R.integer.puzzle_piece_move_unit_duration)*2) longMoveDuration = getResources().getInteger(R.integer.puzzle_piece_move_unit_duration)*2;
		autoMoveAnimation.setDuration(longMoveDuration);
		view.startAnimation(autoMoveAnimation);
	}

	private boolean checkPuzzleIsFinished() {
		for(int i=0;i<this.arrRandomPieces.length;i++) {
			for(int j=0;j<arrRandomPieces[i].length;j++) {
				int arrIndex = AppTools.getPieceIndexTag(i, j, this.cols);
				if(arrIndex != (Integer) this.arrRandomPieces[i][j].getTag(R.id.piece_index)) {
					return false;
				}
			}
		}
		mCallBack.startTimer(false);
		return true;
	}
	private void loadFile(boolean isRetry) {
		this.rows = AppPrefUtil.getRows(this.mCallBack.getMyApp(), null);
		this.cols = AppPrefUtil.getCols(this.mCallBack.getMyApp(), null);
		this.intHintMoveTimes = this.mCallBack.getMyApp().getDefaultMoveTimes(this.rows, this.cols);
		this.intHintViewTimes = this.mCallBack.getMyApp().getDefaultEyeTimes(this.rows, this.cols);
		
		long elapsedTimeSec = 0;
		this.steps = 0;
		this.isFinished = false;
		this.touchedPieceIndexTag = -1;
		this.processedSubCount = 0;
		String lastPlayRandomPieces = null;
		if(!isRetry) {
			ScoreEntity scoreEntity = null;
			if(this.isCustom) {
				scoreEntity = DBHelperScore.getInstance(this.mCallBack.getMyApp()).getScore(this.packageCode, this.listCustomEntity.get((int)this.currentPicIndex).getId(), this.rows, this.cols, null);
			} else {
				scoreEntity = DBHelperScore.getInstance(this.mCallBack.getMyApp()).getScore(this.packageCode, this.currentPicIndex, this.rows, this.cols, null);
			}
			if(scoreEntity != null && scoreEntity.getPieces() != null && !scoreEntity.isFinished()) {
				elapsedTimeSec = scoreEntity.getElapsedTime();
				this.steps = scoreEntity.getSteps();
				this.intHintMoveTimes = scoreEntity.getMoveTimes();
				this.intHintViewTimes = scoreEntity.getEyeTimes();
				lastPlayRandomPieces = scoreEntity.getPieces();
			}
		}
		if(!isRetry && lastPlayRandomPieces == null) this.asyncPiecesImgLoader.clearCurrentDiskCacheFiles();
		if(this.mProgressBar != null && this.mProgressBar.getVisibility() != View.VISIBLE) this.mProgressBar.setVisibility(View.VISIBLE);
		this.relativeLayoutPieces.setVisibility(View.INVISIBLE);
		mCallBack.setElapsedTimeSec(elapsedTimeSec);
		mCallBack.setHintViewTimes(this.intHintViewTimes);
		mCallBack.setHintMoveTimes(this.intHintMoveTimes);
		prepareImagesForPuzzle(lastPlayRandomPieces);
	}
	private void saveScore() {
		if(!this.isInitialized) return;
		if(this.arrRandomPieces == null) return ;
		ScoreEntity scoreEntity = null;
		long elapsedTimeSec = mCallBack.getElapsedTimeSec();
		if(this.isFinished && elapsedTimeSec == 0) elapsedTimeSec = 1;
		if(this.isCustom) {
			scoreEntity = DBHelperScore.getInstance(this.mCallBack.getMyApp()).getScore(this.packageCode, this.listCustomEntity.get((int)this.currentPicIndex).getId(), this.rows, this.cols, null);
		} else {
			scoreEntity = DBHelperScore.getInstance(this.mCallBack.getMyApp()).getScore(this.packageCode, this.currentPicIndex, this.rows, this.cols, null);
		}
		if(scoreEntity == null) {
			scoreEntity = new ScoreEntity();
			scoreEntity.setCode(this.packageCode);
			if(this.isCustom) scoreEntity.setPicIndex(this.listCustomEntity.get((int)this.currentPicIndex).getId());
			else scoreEntity.setPicIndex(this.currentPicIndex);
			scoreEntity.setRows(this.rows);
			scoreEntity.setCols(this.cols);
			scoreEntity.setElapsedTime(elapsedTimeSec);
			if(this.isFinished) {
				scoreEntity.setBestTime(elapsedTimeSec);
				scoreEntity.setStepsOfBestTime(this.steps);
			}
			scoreEntity.setSteps(this.steps);
			scoreEntity.setEyeTimes(this.intHintViewTimes);
			scoreEntity.setMoveTimes(this.intHintMoveTimes);
			scoreEntity.setPlayDatetime(new Date(System.currentTimeMillis()));
			scoreEntity.setFinished(this.isFinished);
		} else {
			scoreEntity.setElapsedTime(elapsedTimeSec);
			if(this.isFinished) {
				if(scoreEntity.getBestTime() <= 0 || scoreEntity.getBestTime() > elapsedTimeSec) {
					scoreEntity.setBestTime(elapsedTimeSec);
					scoreEntity.setStepsOfBestTime(this.steps);
				} else if(scoreEntity.getBestTime() == elapsedTimeSec && scoreEntity.getStepsOfBestTime() > this.steps) {
					scoreEntity.setStepsOfBestTime(this.steps);
				}
			}
			scoreEntity.setSteps(this.steps);
			scoreEntity.setEyeTimes(this.intHintViewTimes);
			scoreEntity.setMoveTimes(this.intHintMoveTimes);
			scoreEntity.setPlayDatetime(new Date(System.currentTimeMillis()));
			scoreEntity.setFinished(this.isFinished);
		}
		if(!this.isFinished) {
			StringBuilder sbRandomPieces = new StringBuilder();
			for(int i=0;i<this.arrRandomPieces.length;i++) {
				for(int j=0;j<this.arrRandomPieces[i].length;j++) {
					if(i==0 && j==0) sbRandomPieces.append(this.arrRandomPieces[i][j].getTag(R.id.piece_index));
					else sbRandomPieces.append(",").append(this.arrRandomPieces[i][j].getTag(R.id.piece_index));
				}
			}
			scoreEntity.setPieces(sbRandomPieces.toString());
		}
		long scoreId=0;
		if(scoreEntity.getId()<=0) { // Insert Score
			scoreId = DBHelperScore.getInstance(this.mCallBack.getMyApp()).insertScore(scoreEntity, null);
		} else {	// Update Score
			DBHelperScore.getInstance(this.mCallBack.getMyApp()).updateScore(scoreEntity, null);
			scoreId = scoreEntity.getId();
		}
		if(!AppConstants.CUSTOM_PACKAGE_CODE.equals(this.packageCode)) {
			SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this.mCallBack.getMyApp());
			Editor editor = pref.edit();
			AppPrefUtil.setLastPlayScoreId(this.mCallBack.getMyApp(), editor, scoreId);
			AppPrefUtil.setLastPlayPackageCode(this.mCallBack.getMyApp(), editor, this.packageCode);
			editor.apply();
		}
		if(this.isFinished) {
			new ShowFinishedTask().execute(scoreId);
		}
	}

	private void prepareImagesForPuzzle(final String lastPlayRandomPieces) {
		long picId = this.currentPicIndex;
		if(this.isCustom) picId = this.listCustomEntity.get((int)this.currentPicIndex).getId();
		Bitmap tempFullBitmap = asyncPiecesImgLoader.loadFullBitmap(picId,
				new PuzzleAsyncImgLoader.ImageCallback() {
                	// 请参见实现：如果第一次加载url时下面方法会执行
                    public void imageLoaded(Bitmap bitmap) {
                    	if(bitmap != null) {
                        	PuzzleFrag.this.pieceWidth = bitmap.getWidth() / PuzzleFrag.this.cols;
                        	PuzzleFrag.this.pieceHeight = bitmap.getHeight() / PuzzleFrag.this.rows;
		                    diagonalLengthOfPiece = Math.sqrt(Math.pow(PuzzleFrag.this.pieceWidth, 2) + Math.pow(PuzzleFrag.this.pieceHeight, 2));
                        	createBorderBitmap();
                        	setSubPieces(lastPlayRandomPieces);
		                    mCallBack.updateActionBarSubTitle(PuzzleFrag.this.steps, PuzzleFrag.this.correctPiecesSum
				                    , PuzzleFrag.this.rows * PuzzleFrag.this.cols, false);
	                    }
                    }
		});
		if (tempFullBitmap != null) {
			this.pieceWidth = tempFullBitmap.getWidth() / this.cols;
			this.pieceHeight = tempFullBitmap.getHeight() / this.rows;
			diagonalLengthOfPiece = Math.sqrt(Math.pow(PuzzleFrag.this.pieceWidth, 2) + Math.pow(PuzzleFrag.this.pieceHeight, 2));
			createBorderBitmap();
			setSubPieces(lastPlayRandomPieces);
			mCallBack.updateActionBarSubTitle(this.steps, this.correctPiecesSum, this.rows * this.cols, false);
		}
	}
	private void setSubPieces(String strLastPlayRandomPieces) {
		Integer[] arrIntRandomPieces = null;
		if(strLastPlayRandomPieces != null) arrIntRandomPieces = parseIntArray(strLastPlayRandomPieces);
		if(arrIntRandomPieces == null || arrIntRandomPieces.length != (this.rows * this.cols)) {
			arrIntRandomPieces = AppTools.getRandomArray(this.rows * this.cols);
		}
		this.arrRandomPieces = new ImageView[this.rows][this.cols];
		this.correctPiecesSum = 0;
		for(int i=0; i<this.rows; i++) {
			for(int j=0; j<this.cols; j++) {
				Log.d(TAG, "this.getActivity()==null is "+(this.getActivity()==null));
				this.arrRandomPieces[i][j] = new ImageView(this.getActivity());
				int indexInArray = AppTools.getPieceIndexTag(i, j, this.cols); //因为tag的值是从“1”开始的，所以将取出的tag减去“1”才是在数组中对应的索引
				if(indexInArray == arrIntRandomPieces[indexInArray]) {
					this.arrRandomPieces[i][j].setImageBitmap(this.rightBorderBitmap);
					++correctPiecesSum;
				} else {
					this.arrRandomPieces[i][j].setImageBitmap(this.wrongBorderBitmap);
				}
				this.arrRandomPieces[i][j].setTag(R.id.piece_index, arrIntRandomPieces[indexInArray]);
				this.arrRandomPieces[i][j].setOnTouchListener(this);
			}
		}
		setPieces2RelativeLayout();
		for(int i=0; i<this.rows; i++) {
			for(int j=0; j<this.cols; j++) {
				final int ii = i;
				final int jj = j;
				int indexOfCorrect = (Integer)this.arrRandomPieces[ii][jj].getTag(R.id.piece_index);
				int[] rowColIndexOfCorrect = AppTools.getRowColIndex(indexOfCorrect, this.cols);
				long picId = this.currentPicIndex;
				if(this.isCustom) picId = this.listCustomEntity.get((int)this.currentPicIndex).getId();
				Bitmap subBitmap = asyncPiecesImgLoader.loadSubBitmap(picId, rowColIndexOfCorrect[0], rowColIndexOfCorrect[1],
						new PuzzleAsyncImgLoader.ImageCallback() {
		                	// 请参见实现：如果第一次加载url时下面方法会执行
		                    public void imageLoaded(Bitmap bitmap) {
		        				if(bitmap != null) {
		        					setSubImageView(bitmap, ii, jj);
		        				}
		                    }
				});
				if(subBitmap != null) {
					setSubImageView(subBitmap, ii, jj);
				}
			}
		}
	}
	private void setSubImageView(Bitmap subBitmap, int ii, int jj) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            this.arrRandomPieces[ii][jj].setBackground(new BitmapDrawable(getResources(), subBitmap));
        else //noinspection deprecation
			this.arrRandomPieces[ii][jj].setBackgroundDrawable(new BitmapDrawable(getResources(), subBitmap));
		synchronized (this.processedSubCount) {
			++this.processedSubCount;
			if(this.processedSubCount == (this.rows * this.cols)) {
				long picId = this.currentPicIndex;
				if(this.isCustom) picId = this.listCustomEntity.get((int)this.currentPicIndex).getId();
				asyncPiecesImgLoader.removeFullPictureFromMemeoryCache(picId);
				
				if(mCallBack.getElapsedTimeSec() <= 0 && this.steps <= 0) {
					startFullImageAnimation();
				} else {
					this.mProgressBar.setVisibility(View.GONE);
					this.relativeLayoutPieces.setVisibility(View.VISIBLE);
					mCallBack.startTimer(true);
				}
				this.isInitialized = true;
			}
		}
	}
	private boolean startFullImageAnimation() {
		if(this.fullImageAnimation == null || this.fullImageAnimation.hasEnded()) {
			if(this.fullImageLayout.getChildCount() != (this.rows * this.cols)) {
				ImageView[][] arrPreviewSub = new ImageView[this.rows][this.cols];
				for(int i=0; i<this.arrRandomPieces.length; i++) {
					for(int j=0; j<this.arrRandomPieces[i].length; j++) {
						int arrIndex = (Integer)this.arrRandomPieces[i][j].getTag(R.id.piece_index);
						int[] rowColIndex = AppTools.getRowColIndex(arrIndex, this.cols);
						arrPreviewSub[rowColIndex[0]][rowColIndex[1]] = new ImageView(this.getActivity());
						arrPreviewSub[rowColIndex[0]][rowColIndex[1]].setImageDrawable(this.arrRandomPieces[i][j].getBackground());
					}
				}
				for(int i=0; i<arrPreviewSub.length; i++) {
					for(int j=0; j<arrPreviewSub[i].length; j++) {
						RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
						layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
						layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
						layoutParams.leftMargin = this.pieceWidth * j;
						layoutParams.topMargin = this.pieceHeight * i;
						this.fullImageLayout.addView(arrPreviewSub[i][j], layoutParams);
					}
				}
			}
			this.mProgressBar.setVisibility(View.GONE);
			this.fullImageLayout.setVisibility(View.VISIBLE);
			this.fullImageLayout.bringToFront();
			if(this.relativeLayoutPieces.getVisibility() != View.VISIBLE) this.relativeLayoutPieces.setVisibility(View.VISIBLE);

			this.fullImageAnimation = new AlphaAnimation(0f, 1f);
			this.fullImageAnimation.setAnimationListener(new FullImageAnimationListener());
			this.fullImageAnimation.setDuration(getResources().getInteger(R.integer.puzzle_full_image_duration));
			this.fullImageAnimation.setInterpolator(new DecelerateInterpolator());
			this.fullImageAnimation.setRepeatCount(1);
			this.fullImageAnimation.setRepeatMode(Animation.REVERSE);
			this.fullImageLayout.startAnimation(this.fullImageAnimation);
			return true;
		} else {
			return false;
		}
	}

	private void setPieces2RelativeLayout() {
		for(int i=0; i<this.arrRandomPieces.length; i++) {
			for(int j=0; j<this.arrRandomPieces[i].length; j++) {
				RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
				layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
				layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
				layoutParams.leftMargin = this.pieceWidth * j;
				layoutParams.topMargin = this.pieceHeight * i;
				this.relativeLayoutPieces.addView(this.arrRandomPieces[i][j], layoutParams);
			}
		}
	}

	private Integer[] parseIntArray(String lastPieces) {
		if(lastPieces == null) return null;
		String strArray[] = lastPieces.split(",");
		int totalPieces = this.rows * this.cols;
		if(strArray.length != totalPieces) return null;
		Integer intArray[] = new Integer[strArray.length];
		boolean isOldVersion = false;
		try {
			for(int i=0; i<strArray.length; i++) {
				intArray[i] = Integer.parseInt(strArray[i]);
				if(intArray[i] == totalPieces) isOldVersion = true;
				if(intArray[i] < 0 || intArray[i] > totalPieces) return null;
			}
			if(isOldVersion) {
				for(int i=0; i<intArray.length; i++) {
					intArray[i] = intArray[i] -1;
					if(intArray[i] < 0 || intArray[i] >= totalPieces) return null;
				}
			}
			return intArray;
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private void createBorderBitmap() {
		if(this.wrongBorderBitmap != null && this.wrongBorderBitmap.getWidth() == this.pieceWidth && this.wrongBorderBitmap.getHeight()==this.pieceHeight
				&& this.rightBorderBitmap != null && this.rightBorderBitmap.getWidth() == this.pieceWidth && this.rightBorderBitmap.getHeight()==this.pieceHeight) {
			return;
		}
		this.wrongBorderBitmap = Bitmap.createBitmap(this.pieceWidth, this.pieceHeight, Bitmap.Config.ARGB_4444);
		Rect rect = new Rect(0, 0, this.pieceWidth, this.pieceHeight);
        Paint paint = new Paint();
//        paint.setAntiAlias(true);
//        paint.setColor(Color.DKGRAY);
//        float floatStrokeWidth = 2 * this.mCallBack.getMyApp().getDisplay().density;//线宽
//        if(this.mCallBack.getMyApp().isTablet()) floatStrokeWidth = floatStrokeWidth * 2;
//        paint.setStrokeWidth(floatStrokeWidth);
//        paint.setStyle(Style.STROKE);
//        paint.setAlpha(128);
//        new Canvas(wrongBorderBitmap).drawRect(rect, paint);
//		PathEffect effect = new DashPathEffect(new float[] {5,5}, 1);
//		paint.setPathEffect(effect);
//		paint.setColor(Color.WHITE);
//		paint.setAlpha(128);
//		new Canvas(wrongBorderBitmap).drawRect(rect, paint);

		float lengthOfSide = (float)this.pieceWidth / 10f;
		paint.setAntiAlias(true);
		paint.setColor(Color.BLACK);
		paint.setStyle(Style.FILL);
		paint.setAlpha(255);
		Path path1 = new Path();
		path1.moveTo(0,0);
		path1.lineTo(lengthOfSide, 0);
		path1.lineTo(0, lengthOfSide);
		path1.close();
		new Canvas(wrongBorderBitmap).drawPath(path1, paint);
		paint.setColor(Color.WHITE);
		paint.setStyle(Style.FILL);
		paint.setAlpha(255);
		lengthOfSide = lengthOfSide * 3 / 5;
		Path path2 = new Path();
		path2.moveTo(0,0);
		path2.lineTo(lengthOfSide, 0);
		path2.lineTo(0, lengthOfSide);
		path2.close();
		new Canvas(wrongBorderBitmap).drawPath(path2, paint);

		paint.setAlpha(0);
        this.rightBorderBitmap = Bitmap.createBitmap(this.pieceWidth, this.pieceHeight, Bitmap.Config.ARGB_4444);
        new Canvas(rightBorderBitmap).drawRect(rect, paint);
        
	}
	protected void showHintView() {
		if(this.touchedPieceIndexTag > -1) return;
		if(!this.isInitialized) return;
		if(this.isFinished) return;
		if(this.intHintViewTimes > 0 && !this.moveAnimationIsRunning) {
			boolean blnAnimation = startFullImageAnimation();
			if(blnAnimation) {
				mCallBack.setHintViewTimes(--this.intHintViewTimes);
			}
		}
	}
	protected void showHintMove() {
		if(this.touchedPieceIndexTag > -1) return;
		if(!this.isInitialized) return;
		if(this.isFinished) return;
		if(this.intHintMoveTimes <= 0) return;
		if(this.fullImageAnimation != null && !this.fullImageAnimation.hasEnded()) return;
		if(this.moveAnimationIsRunning) return;
		for(int i=0;i<this.arrRandomPieces.length;i++) {
			for(int j=0;j<this.arrRandomPieces[i].length;j++) {
				int arrIndex = AppTools.getPieceIndexTag(i, j, this.cols);
				if(arrIndex != (Integer)arrRandomPieces[i][j].getTag(R.id.piece_index)) {
					this.moveAnimationIsRunning = true;
					int[] destRowColIndex = AppTools.getRowColIndex((Integer)arrRandomPieces[i][j].getTag(R.id.piece_index), this.cols);
					this.steps++;
					this.arrRandomPieces[destRowColIndex[0]][destRowColIndex[1]].bringToFront();
					this.arrRandomPieces[i][j].bringToFront();
					autoMovePieceAfterTouch(this.arrRandomPieces[i][j], destRowColIndex[1], destRowColIndex[0], 0, false);
					autoMovePieceAfterTouch(this.arrRandomPieces[destRowColIndex[0]][destRowColIndex[1]], j, i, getResources().getInteger(R.integer.puzzle_piece_move_unit_duration), false);
					mCallBack.setHintMoveTimes(--this.intHintMoveTimes);
					return;
				}
			}
		}
	}
	private long getMoveDuration(long distanceX, long distanceY) {
		double lengthOfDiagonal = Math.sqrt(Math.pow(distanceX, 2) + Math.pow(distanceY, 2));
		long longDuration = Double.valueOf(lengthOfDiagonal / this.diagonalLengthOfPiece * getResources().getInteger(R.integer.puzzle_piece_move_unit_duration)).longValue();
		if(longDuration < getResources().getInteger(R.integer.puzzle_piece_move_min_duration)) longDuration = getResources().getInteger(R.integer.puzzle_piece_move_min_duration);
		else if(longDuration > getResources().getInteger(R.integer.puzzle_piece_move_max_duration)) longDuration = getResources().getInteger(R.integer.puzzle_piece_move_max_duration);
		return longDuration;
	}
	protected void gotoNextPuzzle(boolean isRetry) {
		if(!this.isInitialized) return;
		if(this.moveAnimationIsRunning) return;
		if(this.fullImageAnimation != null && !this.fullImageAnimation.hasEnded()) return;
		this.isInitialized = false;
		this.isContinue = false;
		if(isRetry) mCallBack.startTimer(false);
		if(!isRetry) {
			this.currentPicIndex = this.currentPicIndex + 1;
		}
		if(isCustom) {
			if(this.currentPicIndex >= this.listCustomEntity.size()) {
				mCallBack.openPackageGridActivity();
				return;
			}
		} else {
			if(mCallBack.isInnerPics()) {
				if(this.currentPicIndex >= this.mCallBack.getMyApp().getInnerPics().size()) {
					this.currentPicIndex = 0;
				}
			} else {
				if(this.currentPicIndex >= this.mCallBack.getMyApp().getCurrentZipEntries(this.packageCode).size()) {
					this.currentPicIndex = 0;
				}
			}
		}
		if(this.currentPicIndex == 0 && !isRetry) {
			this.mCallBack.getMyApp().autoIncreaseRowCol();
		}
		mCallBack.updateCurrentPicIndex(this.currentPicIndex);
		if(this.relativeLayoutPieces != null) this.relativeLayoutPieces.removeAllViews();
		if(this.fullImageLayout != null) this.fullImageLayout.removeAllViews();
		loadFile(isRetry);
		
	}
	class FullImageAnimationListener implements AnimationListener {
		@Override
		public void onAnimationEnd(Animation animation) {
			PuzzleFrag.this.fullImageLayout.clearAnimation();
			PuzzleFrag.this.fullImageLayout.removeAllViews();
			mCallBack.startTimer(true);
		}
		@Override
		public void onAnimationRepeat(Animation animation) {
		}
		@Override
		public void onAnimationStart(Animation animation) {
		}
	}
	private void playCorrectPromotionAnimation(int correctNum) {
		if(correctNum <= 0) return;
		this.txtCorrectPromotion.setText("+"+correctNum);
		this.txtCorrectPromotion.bringToFront();
		this.txtCorrectPromotion.setVisibility(View.VISIBLE);
		Animation scaleAnimation = new ScaleAnimation(0.5f, 2.0f, 0.5f, 2.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
		scaleAnimation.setDuration(getResources().getInteger(R.integer.puzzle_correct_promotion_duration));
		Animation alphaAnimation = new AlphaAnimation(1.0f, 0.0f);
		alphaAnimation.setDuration(getResources().getInteger(R.integer.puzzle_correct_promotion_duration) / 2);
		alphaAnimation.setStartOffset(getResources().getInteger(R.integer.puzzle_correct_promotion_duration) / 2);
		AnimationSet animationSet = new AnimationSet(true);
		animationSet.addAnimation(scaleAnimation);
		animationSet.addAnimation(alphaAnimation);
		animationSet.setFillAfter(false);
		animationSet.setAnimationListener(new CorrectPromotionAnimationListener());
		this.txtCorrectPromotion.startAnimation(animationSet);
	}
	class CorrectPromotionAnimationListener implements AnimationListener {
		@Override
		public void onAnimationStart(Animation animation) {
		}
		@Override
		public void onAnimationEnd(Animation animation) {
			PuzzleFrag.this.txtCorrectPromotion.clearAnimation();
			PuzzleFrag.this.txtCorrectPromotion.setVisibility(View.GONE);
		}
		@Override
		public void onAnimationRepeat(Animation animation) {
		}
	}
	class AutoMoveAnimationListener implements  AnimationListener {
		private View view;
		private int toColIndex;
		private int toRowIndex;
		private int startOffset;
		AutoMoveAnimationListener(View view, int toColIndex, int toRowIndex, int startOffset) {
			this.view = view;
			this.toColIndex = toColIndex;
			this.toRowIndex = toRowIndex;
			this.startOffset = startOffset;
		}
		@Override
		public void onAnimationEnd(Animation animation) {
			view.clearAnimation();
			PuzzleFrag.this.arrRandomPieces[toRowIndex][toColIndex] = (ImageView)view;
			RelativeLayout.LayoutParams relativeParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
			relativeParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
			relativeParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
			relativeParams.leftMargin=PuzzleFrag.this.pieceWidth * toColIndex;
			relativeParams.topMargin=PuzzleFrag.this.pieceHeight * toRowIndex;
			this.view.setLayoutParams(relativeParams);
			if(this.startOffset > 0) {
				PuzzleFrag.this.touchedPieceIndexTag = -1;
				PuzzleFrag.this.moveAnimationIsRunning = false;
			}
			int srcPieceTagIndex = AppTools.getPieceIndexTag(this.toRowIndex, this.toColIndex, PuzzleFrag.this.cols);
			if(srcPieceTagIndex == (Integer)view.getTag(R.id.piece_index)) {
				arrRandomPieces[toRowIndex][toColIndex].setImageBitmap(PuzzleFrag.this.rightBorderBitmap);
				++PuzzleFrag.this.correctPiecesSum;
				playCorrectPromotionAnimation(1);
				mCallBack.updateActionBarSubTitle(PuzzleFrag.this.steps, PuzzleFrag.this.correctPiecesSum, PuzzleFrag.this.rows * PuzzleFrag.this.cols, true);
				PuzzleFrag.this.isFinished = PuzzleFrag.this.checkPuzzleIsFinished();
				if(PuzzleFrag.this.isFinished) {
					saveScore();
				}
			}
		}
		@Override
		public void onAnimationRepeat(Animation animation) {}
		@Override
		public void onAnimationStart(Animation animation) {}
	}
	class ShowFinishedTask extends AsyncTask<Long, Void, Long> {
		@Override
		protected Long doInBackground(Long... params) {
			try {
				java.lang.Thread.sleep(PuzzleFrag.this.getResources().getInteger(R.integer.puzzle_freeze_ui_after_finished));
				return params[0];
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return null;
		}
		@Override
	    protected void onPostExecute(Long scoreId) {
			if(scoreId != null) mCallBack.openPuzzleFinished(scoreId);
		}
	}
	class InitialTask extends AsyncTask<File, Void, Integer> {
		@Override
		protected Integer doInBackground(File... params) {
			int result = 0;
//			synchronized (mDiskCacheLock) {
//				File cacheDir = params[0];
//	            mDiskLruCache = DiskLruCache.openCache(cacheDir, ActPieces.DISK_CACHE_SIZE);
//	            mDiskCacheStarting = false; // Finished initialization
//	            mDiskCacheLock.notifyAll(); // Wake any waiting threads
//			}
			if(PuzzleFrag.this.isCustom) {
				PuzzleFrag.this.listCustomEntity = DBHelperCustom.getInstance(PuzzleFrag.this.mCallBack.getMyApp()).getAll(null);
			}
			if(PuzzleFrag.this.isContinue) {
				String matchedDifficulty = null;
				long lastPlayScoreId = AppPrefUtil.getLastPlayScoreId(PuzzleFrag.this.mCallBack.getMyApp(), null);
				ScoreEntity lastPlayScoreEntity = DBHelperScore.getInstance(PuzzleFrag.this.mCallBack.getMyApp()).getScore(lastPlayScoreId, null);
				if(lastPlayScoreEntity == null) {
					result = 1;
				}
				if(result == 0) {
					if(!PuzzleFrag.this.packageCode.equals(lastPlayScoreEntity.getCode())) {
						result = 1;
					}
				}
				if(result == 0) {
					if(!PuzzleFrag.this.isCustom && !mCallBack.isInnerPics()) {
						PicsPackageEntity packageEntity = DBHelperMorepuzzles.getInstance(PuzzleFrag.this.mCallBack.getMyApp()).getEntityByCode(lastPlayScoreEntity.getCode(), null);
						if(packageEntity == null || 
								(!packageEntity.getState().equalsIgnoreCase(PicsPackageEntity.PackageStates.INSTALLED)
										&& !packageEntity.getState().equalsIgnoreCase(PicsPackageEntity.PackageStates.OLDVERSION))) {
							result = 1;
						}
						if(result == 0) {
							if(PuzzleFrag.this.mCallBack.getMyApp().getPackageZipFile(packageEntity.getCode()) == null ) {
								result = 1;
							}
						}
					}
				}
				if(result == 0) {
					matchedDifficulty = AppPrefUtil.getMatchedDifficulty(PuzzleFrag.this.mCallBack.getMyApp(), lastPlayScoreEntity.getRows(), lastPlayScoreEntity.getCols());
					if(matchedDifficulty == null) {
						result = 1;
					}
				}
				if(result == 0) {
					if(lastPlayScoreEntity.isFinished()) {
						if(lastPlayScoreEntity.isCustom()) {
							CustomPicEntity currentCustomEntity = DBHelperCustom.getInstance(PuzzleFrag.this.mCallBack.getMyApp()).getById(lastPlayScoreEntity.getPicIndex(), null);
							if(currentCustomEntity == null) {
								result = 1;
							} else {
								File file = PuzzleFrag.this.mCallBack.getMyApp().getCustomPicture(currentCustomEntity.getImageName());
								if(!file.exists()) result = 1;
							}
							if(result == 0) {
								SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(PuzzleFrag.this.mCallBack.getMyApp());
								Editor editor = pref.edit();
								AppPrefUtil.setDifficulty(PuzzleFrag.this.mCallBack.getMyApp(), editor, matchedDifficulty);
								editor.apply();
								int tempCurrentPicIndex = PuzzleFrag.this.listCustomEntity.indexOf(currentCustomEntity);
								if((tempCurrentPicIndex+1)>=PuzzleFrag.this.listCustomEntity.size()) {
									PuzzleFrag.this.currentPicIndex = 0;
									PuzzleFrag.this.mCallBack.getMyApp().autoIncreaseRowCol();
								} else {
									PuzzleFrag.this.currentPicIndex = tempCurrentPicIndex + 1;
								}
							}
						} else {
							SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(PuzzleFrag.this.mCallBack.getMyApp());
							Editor editor = pref.edit();
							AppPrefUtil.setDifficulty(PuzzleFrag.this.mCallBack.getMyApp(), editor, matchedDifficulty);
							editor.apply();
							int totalPicsSize;
							if(mCallBack.isInnerPics()) totalPicsSize = PuzzleFrag.this.mCallBack.getMyApp().getInnerPics().size();
							else totalPicsSize = PuzzleFrag.this.mCallBack.getMyApp().getCurrentZipEntries(PuzzleFrag.this.packageCode).size();
							if((lastPlayScoreEntity.getPicIndex()+1) >= totalPicsSize) {
								PuzzleFrag.this.currentPicIndex = 0;
								PuzzleFrag.this.mCallBack.getMyApp().autoIncreaseRowCol();
							} else {
								PuzzleFrag.this.currentPicIndex = lastPlayScoreEntity.getPicIndex()+1;
							}
						}
					} else {
						if(lastPlayScoreEntity.isCustom()) {
							CustomPicEntity currentCustomEntity = DBHelperCustom.getInstance(PuzzleFrag.this.mCallBack.getMyApp()).getById(lastPlayScoreEntity.getPicIndex(), null);
							if(currentCustomEntity == null) {
								result = 1;
							} else {
								File file = PuzzleFrag.this.mCallBack.getMyApp().getCustomPicture(currentCustomEntity.getImageName());
								if(!file.exists()) result = 1;
							}
							if(result == 0) {
								PuzzleFrag.this.currentPicIndex = PuzzleFrag.this.listCustomEntity.indexOf(currentCustomEntity);
							}
						} else {
							PuzzleFrag.this.currentPicIndex = lastPlayScoreEntity.getPicIndex();
							int totalPicsSize;
							if(mCallBack.isInnerPics()) totalPicsSize = PuzzleFrag.this.mCallBack.getMyApp().getInnerPics().size();
							else totalPicsSize = PuzzleFrag.this.mCallBack.getMyApp().getCurrentZipEntries(PuzzleFrag.this.packageCode).size();
							if(PuzzleFrag.this.currentPicIndex >= totalPicsSize) result = 1;
						}
						if(result == 0) {
							SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(PuzzleFrag.this.mCallBack.getMyApp());
							Editor editor = pref.edit();
							AppPrefUtil.setDifficulty(PuzzleFrag.this.mCallBack.getMyApp(), editor, matchedDifficulty);
							editor.apply();
						}
					}
				}
			}
			if(result != 0) {
				SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(PuzzleFrag.this.mCallBack.getMyApp());
				Editor editor = pref.edit();
				AppPrefUtil.setLastPlayScoreId(PuzzleFrag.this.mCallBack.getMyApp(), editor, 0);
				editor.apply();
			}
			File cacheDir = params[0];
            long DISK_CACHE_SIZE = 1024 * 1024 * 5;
            mDiskLruCache = DiskLruCache.openCache(cacheDir, DISK_CACHE_SIZE);
			return result;
		}
		@Override
	    protected void onPostExecute(Integer result) {
			if(result == 0) {
				PuzzleFrag.this.asyncPiecesImgLoader = new PuzzleAsyncImgLoader(PuzzleFrag.this.packageCode, PuzzleFrag.this.mCallBack.getMyApp(), PuzzleFrag.this.mDiskLruCache);
				PuzzleFrag.this.loadFile(false);
			} else {
				mCallBack.openNewGame();
			}
		}
	}
}

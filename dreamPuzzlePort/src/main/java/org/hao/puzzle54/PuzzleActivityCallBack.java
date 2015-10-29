package org.hao.puzzle54;

public interface PuzzleActivityCallBack {
	public int getActionBarHeight();
//	public void refreshActivity(int handlerMsg);
//	public void setSteps(int steps, boolean isPlaySound);
	public void setElapsedTimeSec(long elapsedTimeSec);
	public long getElapsedTimeSec();
	public void setHintMoveTimes(int times);
	public void setHintViewTimes(int times);
	public void startTimer(boolean blnStart);
	public void setMusicOn(boolean musicOn);
	public void setSoundOn(boolean soundOn);
	public void gotoNextPuzzle(boolean isRetry);
//	public void openPuzzle();
	public void openPuzzleFinished(long scoreId);
//	public void openPuzzlePausing();
	public void openPackageGridActivity();
	public void updateCurrentPicIndex(long currentPicIndex);
	public void updateActionBarSubTitle(int steps, int correctPiecesSum, int totalPieces, boolean isPlaySound);
	public MyApp getMyApp();
	public boolean isInnerPics();
	public void openNewGame();
}

package org.hao.puzzle54;

import java.util.Date;

public class ScoreEntity {
	private long id;
	private String code;
	private long picIndex;
	private int rows;
	private int cols;
	private long elapsedTime;
	private long bestTime;
	private int steps;
	private int stepsOfBestTime;
	private int eyeTimes;
	private int moveTimes;
	private Date playDatetime;
	private boolean isFinished;
	private String pieces;
	
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	public long getPicIndex() {
		return picIndex;
	}
	public void setPicIndex(long picIndex) {
		this.picIndex = picIndex;
	}
	public int getRows() {
		return rows;
	}
	public void setRows(int rows) {
		this.rows = rows;
	}
	public int getCols() {
		return cols;
	}
	public void setCols(int cols) {
		this.cols = cols;
	}
	public long getElapsedTime() {
		return elapsedTime;
	}
	public void setElapsedTime(long elapsedTime) {
		this.elapsedTime = elapsedTime;
	}
	public long getBestTime() {
		return bestTime;
	}
	public void setBestTime(long bestTime) {
		this.bestTime = bestTime;
	}
	public int getSteps() {
		return steps;
	}
	public void setSteps(int steps) {
		this.steps = steps;
	}
	public int getStepsOfBestTime() {
		return stepsOfBestTime;
	}
	public void setStepsOfBestTime(int stepsOfBest) {
		this.stepsOfBestTime = stepsOfBest;
	}
	public int getEyeTimes() {
		return eyeTimes;
	}
	public void setEyeTimes(int eyeTimes) {
		this.eyeTimes = eyeTimes;
	}
	public int getMoveTimes() {
		return moveTimes;
	}
	public void setMoveTimes(int moveTimes) {
		this.moveTimes = moveTimes;
	}
	public Date getPlayDatetime() {
		return playDatetime;
	}
	public void setPlayDatetime(Date playDatetime) {
		this.playDatetime = playDatetime;
	}
	public boolean isFinished() {
		return isFinished;
	}
	public void setFinished(boolean isFinished) {
		this.isFinished = isFinished;
	}
	public String getPieces() {
		return pieces;
	}
	public void setPieces(String pieces) {
		this.pieces = pieces;
	}
	public boolean isCustom() {
        return this.code != null && this.code.equals(AppConstants.CUSTOM_PACKAGE_CODE);
	}
}

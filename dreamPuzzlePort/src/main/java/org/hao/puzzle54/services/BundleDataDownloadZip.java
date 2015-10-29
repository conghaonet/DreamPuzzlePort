package org.hao.puzzle54.services;

import java.io.Serializable;

public class BundleDataDownloadZip implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int downloadPercent;
	private String packageCode;
	private String packageState;
	public int getDownloadPercent() {
		return downloadPercent;
	}
	public void setDownloadPercent(int downloadPercent) {
		this.downloadPercent = downloadPercent;
	}
	public String getPackageCode() {
		return packageCode;
	}
	public void setPackageCode(String packageCode) {
		this.packageCode = packageCode;
	}
	public String getPackageState() {
		return packageState;
	}
	public void setPackageState(String packageState) {
		this.packageState = packageState;
	}
	
}

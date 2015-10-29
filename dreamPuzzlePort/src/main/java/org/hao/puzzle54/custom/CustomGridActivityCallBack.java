package org.hao.puzzle54.custom;

import org.hao.puzzle54.MyApp;

public interface CustomGridActivityCallBack {
	public int getStatusBarHeight();
	public void refreshActivity(int handlerMsg);
	public void openCustomPics();
	public void openCustomFolders();
	public void openCustomSelectedFolder(String selectedFolderPath);
	public void openCustomPreview(String packageCode, int picIndex);
	public MyApp getMyApp();
}

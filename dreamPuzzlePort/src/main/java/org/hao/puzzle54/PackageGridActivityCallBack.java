package org.hao.puzzle54;

public interface PackageGridActivityCallBack {
	public void openMorePuzzlesFrag();
	public void openNewGameFrag();
	public void openPackagePicsFrag(String packageCode);
	public void openCustomPicsFrag();
	public void openPuzzlePreview(String packageCode, int picIndex);
	public MyApp getMyApp();
}

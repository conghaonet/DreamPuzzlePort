package org.hao.puzzle54;

import org.hao.puzzle54.custom.CustomPicEntity;

import java.util.List;

public interface PreviewPicActivityCallBack {
	public void openPuzzleActivity();
	public MyApp getMyApp();
	public long getAllowedMaxPicIndex();
	public List<CustomPicEntity> getCustomEntityList();
	public boolean isInnerPics();
}

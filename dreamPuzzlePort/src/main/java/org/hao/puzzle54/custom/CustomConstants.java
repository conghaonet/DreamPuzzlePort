package org.hao.puzzle54.custom;

import org.hao.puzzle54.AppConstants;

import java.io.File;

public abstract class CustomConstants {
	public static final String CUSTOM_FOLDER = AppConstants.APP_BASE_FOLDER + File.separator + "custom";
	public static final String CAMERA_FOLDER = CUSTOM_FOLDER + File.separator + "camera";
	public static final String PICTURE_FOLDER = CUSTOM_FOLDER + File.separator + "pics";
	public static final String CAMERA_NAME_PREFIX="camera_";
	public static final String PICTURE_NAME_PREFIX="custom_";
	public static final String EXTENSION_CUSTOM_PICTURE = ".chr";
}
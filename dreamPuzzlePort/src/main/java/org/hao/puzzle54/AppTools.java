package org.hao.puzzle54;

import android.media.ExifInterface;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;

import org.hao.puzzle54.custom.CustomConstants;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;

public abstract class AppTools {
	public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("00");
	public static final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
	public static List<String> listDownloadingIcon = new ArrayList<String>();
	public static boolean inputStream2File(InputStream ins, File file) {
		boolean blnResult = false;
		BufferedInputStream bis = null;
		BufferedOutputStream bos = null;
		try {
			if(ins == null || file == null || ins.available() <= 0) return blnResult;
			if(file != null && file.exists()) {
				file.delete();
			}
			bis = new BufferedInputStream(ins);
			bos = new BufferedOutputStream(new FileOutputStream(file));
			int bytesRead = 0;
			byte[] buffer = new byte[8192];
			while ((bytesRead = bis.read(buffer, 0, 8192)) != -1) {
				bos.write(buffer, 0, bytesRead);
			}
			bos.flush();
			blnResult = true;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(bos != null) {
				try {
					bos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if(bis != null) {
				try {
					bis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return blnResult;
	}
	public static String buildCustomCameraName(Date date) {
		return CustomConstants.CAMERA_NAME_PREFIX+AppTools.dateTimeFormat.format(date)+AppConstants.EXTENSION_NAME_PICTURE;
	}
	public static String buildCustomPictureName(Date date) {
		return CustomConstants.PICTURE_NAME_PREFIX+AppTools.dateTimeFormat.format(date)+CustomConstants.EXTENSION_CUSTOM_PICTURE;
	}
	public static String buildCustomPictureName(Date date, String subIndex) {
		return CustomConstants.PICTURE_NAME_PREFIX+AppTools.dateTimeFormat.format(date)+"_"+subIndex+CustomConstants.EXTENSION_CUSTOM_PICTURE;
	}

    public static int[] getRowColIndex(int pieceIndexTag, int cols) {
		@SuppressWarnings("UnnecessaryLocalVariable") int intRowColIndex[] = {pieceIndexTag/cols, pieceIndexTag%cols};
		return intRowColIndex;
	}
	public static int getPieceIndexTag(int rowIndex, int colIndex, int cols) {
		return rowIndex * cols + colIndex;
	}
	public static Integer[] getRandomArray(int arrayLength) {
		Integer[] sequence = new Integer[arrayLength];
		for(int i = 0; i < arrayLength; i++){
			sequence[i] = i;
		}
		Random random = new Random();
		for(int i = 0; i < arrayLength; i++){
			int p = random.nextInt(arrayLength);
			int tmp = sequence[i];
			sequence[i] = sequence[p];
			sequence[p] = tmp;
		}
		random = null;
		boolean isRandom = false;
		for(int i = 0; i < arrayLength; i++){
			if (i != sequence[i]) {
				isRandom = true;
				break;
			}
		}
		if(!isRandom) {
			if(arrayLength>=4) {
				int a = sequence[0];
				sequence[0] = sequence[arrayLength-1];
				sequence[arrayLength-1]=a;
				int b = sequence[1];
				sequence[1] = sequence[arrayLength-2];
				sequence[arrayLength-2]=b;
				int c = sequence[0];
				sequence[0] = sequence[1];
				sequence[1]=c;
			}
		}
		return sequence;
	}
	public static String formatElapsedTimeSec2String(long elapsedTimeSec) {
		Calendar ca = Calendar.getInstance();
		ca.setTimeInMillis(elapsedTimeSec * 1000);
		DateFormat df = new SimpleDateFormat("H:mm:ss", Locale.ENGLISH);
		df.setTimeZone(TimeZone.getTimeZone("GMT")); // modify Time Zone.
		String strTime = df.format(ca.getTime());
		if(strTime.startsWith("0:")) strTime = strTime.substring(2);
		return strTime;
	}
	public static String getExternalStoragePath() {
		// 获取SdCard状态
		String sdcardPath = null;
		String state = Environment.getExternalStorageState();
		// 判断SdCard是否存在并且是可用的
		if (state != null && Environment.MEDIA_MOUNTED.equals(state)) {
			if (Environment.getExternalStorageDirectory().canWrite()) {
				sdcardPath = Environment.getExternalStorageDirectory().getPath();
			}
		}
		return sdcardPath;
	}
	public static long getAvailableSpace(String filePath) {
        try {
            StatFs statFs = new StatFs(filePath);
            long blocSize;
            long availaBlock;
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                blocSize = statFs.getBlockSizeLong();
                availaBlock = statFs.getAvailableBlocksLong();
            } else {
                //noinspection deprecation
                blocSize = statFs.getBlockSize();
                //noinspection deprecation
                availaBlock = statFs.getAvailableBlocks();
            }
            return availaBlock * blocSize;
            // 获取BLOCK数量
            // long totalBlocks = statFs.getBlockCount();
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }
	public static String getFullParentPathEndWithSeparator(String fileFullPath) {
		if(fileFullPath == null || !fileFullPath.startsWith("/")) return null;
		else {
			if(fileFullPath.endsWith("/")) fileFullPath = fileFullPath.substring(0, fileFullPath.length()-1);
			return fileFullPath.substring(0, fileFullPath.lastIndexOf("/")+1);
		}
	}
	public static String getShortParentPath(String fileFullPath) {
		fileFullPath = getFullParentPathEndWithSeparator(fileFullPath);
		if(fileFullPath == null || !fileFullPath.contains("/")) return fileFullPath;
		else {
			fileFullPath = fileFullPath.substring(0, fileFullPath.length()-1);
			return fileFullPath.substring(fileFullPath.lastIndexOf("/")+1);
		}
	}
	public static int getImageRotate(String filePath) {
		int imageRotate = 0;
		try{
			ExifInterface exif = new ExifInterface(filePath);
			int tagOrientation = Integer.parseInt(exif.getAttribute(ExifInterface.TAG_ORIENTATION));
			switch(tagOrientation) {
			case ExifInterface.ORIENTATION_ROTATE_90:
				imageRotate = 90;
				break;
			case ExifInterface.ORIENTATION_ROTATE_180:
				imageRotate = 180;
				break;
			case ExifInterface.ORIENTATION_ROTATE_270:
				imageRotate = 270;
				break;
			default:
				break;
			}
		} catch(IOException e) {
			e.printStackTrace();
		}
		return imageRotate;
	}
	/**
	 *
	 * @param directory 如果传入的directory必须是文件夹，如果是文件，将不做处理
	 */
	public static void deleteFilesByDirectory(File directory) {
		if(directory == null || !directory.exists() || !directory.isDirectory()) return;
		for (File file : directory.listFiles()) {
			deleteFiles(file);
		}
	}
	public static void deleteFiles(File file) {
		if(file == null || !file.exists()) return;
		if (file.isFile()) {
			file.delete();
			return;
		}
		if (file.isDirectory()) {
			File[] childFiles = file.listFiles();
			if (childFiles == null || childFiles.length == 0) {
				file.delete();
				return;
			}
            for (File childFile : childFiles) {
                deleteFiles(childFile);
            }
			file.delete();
		}
	}
	public static int getIconScaleWidth(int screenWidth, int cols) {
		return (screenWidth/cols)-(screenWidth/cols)/5;
	}
	
	public static boolean addDownloadingIcon(String packageCode) {
		synchronized (AppTools.listDownloadingIcon) {
			if(AppTools.listDownloadingIcon.indexOf(packageCode) == -1) {
				AppTools.listDownloadingIcon.add(packageCode);
				return true;
			} else {
				return false;
			}
		}
	}
	public static String removeDownloadingIcon(String packageCode) {
		synchronized (AppTools.listDownloadingIcon) {
			if(!AppTools.listDownloadingIcon.isEmpty()) {
				int index = AppTools.listDownloadingIcon.indexOf(packageCode);
				if(index != -1) {
					return AppTools.listDownloadingIcon.remove(index);
				}
			}
			return null;
		}
	}
	public static String getDownloadingIcon() {
		synchronized (AppTools.listDownloadingIcon) {
			if(!AppTools.listDownloadingIcon.isEmpty()) {
				return AppTools.listDownloadingIcon.get(0);
			} else return null;
		}
	}
}

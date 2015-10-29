package org.hao.puzzle54.services;

import android.app.IntentService;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.hao.database.DBHelperMorepuzzles;
import org.hao.puzzle54.AppTools;
import org.hao.puzzle54.MyApp;
import org.hao.puzzle54.MyHttpClient;
import org.hao.puzzle54.PicsPackageEntity;
import org.hao.puzzle54.AppConstants;
import org.hh.puzzle.port54.hall.R;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class DownloadIconService extends IntentService {
	private static final String TAG = DownloadIconService.class.getName();
	private MyApp myApp;
	private static final String TEMP_FILE_EXTENTION=".tmp";
	
	public DownloadIconService() {
		super(TAG);
	}
	@Override
	protected void onHandleIntent(Intent intent) {
		if(this.myApp == null) this.myApp = (MyApp)this.getApplicationContext();
		String packageCode = null;
		while((packageCode = AppTools.getDownloadingIcon()) != null) {
			if(myApp.getConnectivityNetworkName() == null) {
				this.myApp.tableUnfinishedServices.put(this.getClass().getName(), this.getClass());
				return;
			}
			process(packageCode);
			AppTools.removeDownloadingIcon(packageCode);
		}
	}
	private boolean process(String packageCode) {
		int intGridItemWidth = AppTools.getIconScaleWidth(this.myApp.getDisplay().widthPixels, getResources().getInteger(R.integer.pics_grid_columns));
		boolean isSuccessful = false;
		PicsPackageEntity entity = DBHelperMorepuzzles.getInstance(this).getEntityByCode(packageCode, null);
		if(entity != null && entity.getIcon() != null && !entity.getIcon().trim().equalsIgnoreCase("")) {
			File tempFile = downloadIcon(entity.getCode(), entity.getIcon());
			if(!tempFile.exists()) return isSuccessful;
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(tempFile.getPath(), options);
			if(options.outHeight<=0 || options.outWidth<=0) return isSuccessful;
			options.inSampleSize = options.outWidth / intGridItemWidth;
			if(options.inSampleSize < 1) options.inSampleSize = 1;
			options.inJustDecodeBounds = false;
//	        options.inInputShareable = true;
//	        options.inPurgeable = true;
	        options.inPreferredConfig = Bitmap.Config.RGB_565;
			Bitmap iconBitmap = BitmapFactory.decodeFile(tempFile.getPath(), options);
			File iconFile = new File(tempFile.getPath().replaceAll(TEMP_FILE_EXTENTION, AppConstants.EXTENSION_NAME_ICON));
			if(iconFile.exists()) iconFile.delete();
			if(iconBitmap != null) {
				if(iconBitmap.getWidth() != intGridItemWidth) {
					try{
						Bitmap tempBitmap = Bitmap.createScaledBitmap(iconBitmap, intGridItemWidth, intGridItemWidth, true);
				        FileOutputStream outputIcon = new FileOutputStream(iconFile);
				        tempBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputIcon);
						if(!tempBitmap.isRecycled()) tempBitmap.recycle();
						tempBitmap = null;
			        	if(outputIcon != null) outputIcon.close();
			        	tempFile.delete();
					} catch(Exception e) {
						e.printStackTrace();
						return isSuccessful;
					}
				} else {
					tempFile.renameTo(iconFile);
				}
	        	if(!iconBitmap.isRecycled()) iconBitmap.recycle();
	        	iconBitmap = null;
	        	DBHelperMorepuzzles.getInstance(this).updateIconState(entity.getCode(), PicsPackageEntity.IconStates.DOWNLOADED, null);
	        	isSuccessful = true;
				sendBroadcast(new Intent(this.getPackageName()+DownloadIconReceiver.ACTION_DOWNLOAD_ICON));
			}
		}
		return isSuccessful;
	}
	private File downloadIcon(String packageCode, String iconUri) {
		File tempIconFile = new File(this.myApp.getAppFilesPath(true) + packageCode + TEMP_FILE_EXTENTION);
		if(tempIconFile.exists()) tempIconFile.delete();
//			HttpClient httpClient = this.myApp.getHttpClient();
			HttpGet httpGet = new HttpGet(ResourceServerConstants.DREAMPUZZLE_PACKAGES_URL + iconUri);
			byte[] buffer = new byte[8192];
			BufferedInputStream bis = null;
			BufferedOutputStream bos = null;
			try {
				HttpResponse response = MyHttpClient.getInstance().execute(httpGet);
				if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
					bis = new BufferedInputStream(response.getEntity().getContent());
					bos = new BufferedOutputStream(new FileOutputStream(tempIconFile));
					int len = -1;
					while ((len = bis.read(buffer)) != -1) {
						bos.write(buffer, 0, len);
					}
					bos.flush();
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if(httpGet != null && !httpGet.isAborted()) httpGet.abort();
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
		return tempIconFile;
	}
	@Override
	public void onDestroy() {
		super.onDestroy();
	}

}

package org.hao.puzzle54.services;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.hao.database.DBHelperMorepuzzles;
import org.hao.puzzle54.MyApp;
import org.hao.puzzle54.MyHttpClient;
import org.hao.puzzle54.PicsPackageEntity;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

public class DownloadZipService extends IntentService {
	private static final String TAG	= DownloadZipService.class.getName();
	private MyApp myApp;

	public DownloadZipService() {
		super(TAG);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		if(this.myApp == null) this.myApp = (MyApp)this.getApplicationContext();
		Bundle bundle = intent.getExtras();
		String packageCode = bundle.getString("package_code");
		process(packageCode);
	}
	private void process(String packageCode) {
		PicsPackageEntity downlingEntity = DBHelperMorepuzzles.getInstance(this).getEntityByCode(packageCode, null);
		if(downlingEntity.getState().equalsIgnoreCase(PicsPackageEntity.PackageStates.SCHEDULED)) {
			DBHelperMorepuzzles.getInstance(this).updatePackageState(packageCode, PicsPackageEntity.PackageStates.DOWNLOADING, null);
			downlingEntity = DBHelperMorepuzzles.getInstance(this).getEntityByCode(packageCode, null);
		}
		if(downlingEntity.getState().equalsIgnoreCase(PicsPackageEntity.PackageStates.DOWNLOADING)) {
			boolean blnDownloadCompleted = false;
//			File downloadFile = new File(this.myApp.getPuzzleDataBasePath(true) + downlingEntity.getCode() + CommonConstants.EXTENSION_NAME_PACKAGE);
			File downloadFile = myApp.getPackageFile(downlingEntity.getCode());
				blnDownloadCompleted = downloadZip(downloadFile, downlingEntity);
			try {
				java.lang.Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if(blnDownloadCompleted) {
				DBHelperMorepuzzles.getInstance(this).updatePackageState(downlingEntity.getCode(), PicsPackageEntity.PackageStates.INSTALLED, null);
				Intent percentIntent = new Intent(this.getPackageName()+DownloadZipReceiver.ACTION_DOWNLOAD_ZIP);
				Bundle bundle = new Bundle();
				BundleDataDownloadZip bundleData = new BundleDataDownloadZip();
				bundleData.setDownloadPercent(100);
				bundleData.setPackageCode(downlingEntity.getCode());
				bundleData.setPackageState(PicsPackageEntity.PackageStates.INSTALLED);
				bundle.putSerializable(BundleDataDownloadZip.class.getName(), bundleData);
				percentIntent.putExtras(bundle);
				sendBroadcast(percentIntent);
				Intent finishedIntent = new Intent(this.getPackageName()+DownloadZipReceiver.ACTION_DOWNLOAD_FINISHED);
				sendBroadcast(finishedIntent);
			} else {
				DBHelperMorepuzzles.getInstance(this).updatePackageState(downlingEntity.getCode(), PicsPackageEntity.PackageStates.PAUSING, null);
				Intent pausingIntent = new Intent(this.getPackageName()+DownloadZipReceiver.ACTION_DOWNLOAD_ZIP);
				Bundle bundle = new Bundle();
				BundleDataDownloadZip bundleData = new BundleDataDownloadZip();
				bundleData.setDownloadPercent(0);
				bundleData.setPackageCode(downlingEntity.getCode());
				bundleData.setPackageState(PicsPackageEntity.PackageStates.PAUSING);
				bundle.putSerializable(BundleDataDownloadZip.class.getName(), bundleData);
				pausingIntent.putExtras(bundle);
				sendBroadcast(pausingIntent);
			}
		}
	}
	private boolean downloadZip(File downloadFile, PicsPackageEntity downlingEntity) {
		boolean blnDownloadCompleted = false;
		long onlineContentLength = -1;
		String onlineLastModified = "";
		byte[] buffer = new byte[8192];
		BufferedInputStream bis = null;
		BufferedOutputStream bos = null;
		RandomAccessFile raf = null;
//		HttpClient httpClient = this.myApp.getHttpClient();
		HttpGet httpGet = new HttpGet(ResourceServerConstants.DREAMPUZZLE_PACKAGES_URL + downlingEntity.getZip());
		try {
			HttpResponse response = MyHttpClient.getInstance().execute(httpGet);
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				onlineContentLength = response.getEntity().getContentLength();
				onlineLastModified = response.getFirstHeader("Last-Modified").getValue();
				if(onlineContentLength == downlingEntity.getZipSize() && onlineLastModified != null 
						&& downlingEntity.getZipLastModified()!=null && onlineLastModified.equalsIgnoreCase(downlingEntity.getZipLastModified()) && downloadFile.length()>0) {
					if(!httpGet.isAborted()) httpGet.abort();
					httpGet = new HttpGet(ResourceServerConstants.DREAMPUZZLE_PACKAGES_URL + downlingEntity.getZip());
					httpGet.addHeader("Range", "bytes=" + downloadFile.length() + "-");
					response = MyHttpClient.getInstance().execute(httpGet);
					if (response.getStatusLine().getStatusCode() == HttpStatus.SC_PARTIAL_CONTENT) {
						bis = new BufferedInputStream(response.getEntity().getContent());
						raf = new RandomAccessFile(downloadFile, "rw");
						raf.seek(downloadFile.length());
						Intent receiverIntent = new Intent(this.getPackageName()+DownloadZipReceiver.ACTION_DOWNLOAD_ZIP);
						int downloadPercent = (int)((float)downloadFile.length() / (float)onlineContentLength * 100);
						Bundle bundle = new Bundle();
						BundleDataDownloadZip bundleData = new BundleDataDownloadZip();
						bundleData.setDownloadPercent(downloadPercent);
						bundleData.setPackageCode(downlingEntity.getCode());
						bundleData.setPackageState(PicsPackageEntity.PackageStates.DOWNLOADING);
						bundle.putSerializable(BundleDataDownloadZip.class.getName(), bundleData);
						receiverIntent.putExtras(bundle);
						sendBroadcast(receiverIntent);
						
						long startTime = System.currentTimeMillis();
						long currentTime;
						int len = -1;
						while ((len = bis.read(buffer)) != -1) {
							raf.write(buffer, 0, len);
							currentTime = System.currentTimeMillis();
							if((currentTime - startTime)>=1000) {
								downloadPercent = (int)((float)downloadFile.length() / (float)onlineContentLength * 100);
								bundleData.setDownloadPercent(downloadPercent);
								bundleData.setPackageCode(downlingEntity.getCode());
								bundleData.setPackageState(PicsPackageEntity.PackageStates.DOWNLOADING);
								bundle.putSerializable(BundleDataDownloadZip.class.getName(), bundleData);
								receiverIntent.putExtras(bundle);
								sendBroadcast(receiverIntent);
								startTime = currentTime;
							}
						}
						downloadPercent = (int)((float)downloadFile.length() / (float)onlineContentLength * 100);
						bundleData.setDownloadPercent(downloadPercent);
						bundleData.setPackageCode(downlingEntity.getCode());
						bundleData.setPackageState(PicsPackageEntity.PackageStates.DOWNLOADING);
						bundle.putSerializable(BundleDataDownloadZip.class.getName(), bundleData);
						receiverIntent.putExtras(bundle);						
						sendBroadcast(receiverIntent);
					} else {
						if(!httpGet.isAborted()) httpGet.abort();
						httpGet = new HttpGet(ResourceServerConstants.DREAMPUZZLE_PACKAGES_URL + downlingEntity.getZip());
						response = MyHttpClient.getInstance().execute(httpGet);
					}
				}
				//restart download puzzle file
				if(downloadFile.length() != onlineContentLength) {
					if(!downloadFile.exists() || (downloadFile.exists() && downloadFile.delete())) {
						downlingEntity.setZipSize(onlineContentLength);
						downlingEntity.setZipLastModified(onlineLastModified);
						downlingEntity.setDownloadPercent(0);
						DBHelperMorepuzzles.getInstance(this).update(downlingEntity, null);
					}
					bis = new BufferedInputStream(response.getEntity().getContent());
					bos = new BufferedOutputStream(new FileOutputStream(downloadFile));
					Intent receiverIntent = new Intent(this.getPackageName()+DownloadZipReceiver.ACTION_DOWNLOAD_ZIP);
					int downloadPercent = 0;
					Bundle bundle = new Bundle();
					BundleDataDownloadZip bundleData = new BundleDataDownloadZip();
					bundleData.setDownloadPercent(downloadPercent);
					bundleData.setPackageCode(downlingEntity.getCode());
					bundleData.setPackageState(PicsPackageEntity.PackageStates.DOWNLOADING);
					bundle.putSerializable(BundleDataDownloadZip.class.getName(), bundleData);
					receiverIntent.putExtras(bundle);
					sendBroadcast(receiverIntent);
					long startTime = System.currentTimeMillis();
					long currentTime;
					int len = -1;
					buffer = new byte[8192];
					while ((len = bis.read(buffer)) != -1) {
						bos.write(buffer, 0, len);
						currentTime = System.currentTimeMillis();
						if((currentTime - startTime)>=1000) {
							downloadPercent = (int)((float)downloadFile.length() / (float)onlineContentLength * 100);
							bundleData.setDownloadPercent(downloadPercent);
							bundleData.setPackageCode(downlingEntity.getCode());
							bundleData.setPackageState(PicsPackageEntity.PackageStates.DOWNLOADING);
							bundle.putSerializable(BundleDataDownloadZip.class.getName(), bundleData);
							receiverIntent.putExtras(bundle);
							sendBroadcast(receiverIntent);
							startTime = currentTime;
						}
					}
					bos.flush();
					downloadPercent = (int)((float)downloadFile.length() / (float)onlineContentLength * 100);
					bundleData.setDownloadPercent(downloadPercent);
					bundleData.setPackageCode(downlingEntity.getCode());
					bundleData.setPackageState(PicsPackageEntity.PackageStates.DOWNLOADING);
					bundle.putSerializable(BundleDataDownloadZip.class.getName(), bundleData);
					sendBroadcast(receiverIntent);
				}
                blnDownloadCompleted = downloadFile.length() > 0 && downloadFile.length() == onlineContentLength;
			}
			
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			if(httpGet != null && !httpGet.isAborted()) {
				httpGet.abort();
			}
			if(bis != null) {
				try {
					bis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if(bos != null) {
				try {
					bos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if(raf != null) {
				try {
					raf.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		return blnDownloadCompleted;
	}
	@Override
	public void onDestroy() {
		super.onDestroy();
	}
}

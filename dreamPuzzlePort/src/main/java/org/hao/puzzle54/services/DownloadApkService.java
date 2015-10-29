package org.hao.puzzle54.services;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.hao.puzzle54.AppPrefUtil;
import org.hao.puzzle54.InstallMyApkActivity;
import org.hao.puzzle54.MyApp;
import org.hao.puzzle54.MyBundleData;
import org.hao.puzzle54.MyHttpClient;
import org.hh.puzzle.port54.hall.R;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

public class DownloadApkService extends IntentService {
	private static final String TAG = DownloadApkService.class.getName();
	public static final String INTENT_EXTRA_LONG_DOWNLOADED_SIZE="INTENT_EXTRA_LONG_DOWNLOADED_SIZE";
	public static final String INTENT_EXTRA_LONG_TOTAL_SIZE="INTENT_EXTRA_LONG_TOTAL_SIZE";
	public static final String INTENT_EXTRA_BOOLEAN_FINISHED="INTENT_EXTRA_BOOLEAN_FINISHED";
	public static final int NOTIFICATION_ID = 0;
	private NotificationManager mNotificationManager;
	private Notification mNotification;
	private MyApp myApp;
	public DownloadApkService() {
		super(TAG);
	}
	@Override
	protected void onHandleIntent(Intent intent) {
		boolean isSuccessful = false;
		if(this.myApp == null) this.myApp = (MyApp)this.getApplicationContext();
		initialNotification();
		try {
			isSuccessful = downloadApK();
			RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.notification_stop);
			contentView.setImageViewResource(R.id.notify_stop_icon, R.drawable.ic_launcher);
			CharSequence contentTitle = getString(R.string.app_name);
			contentView.setTextViewText(R.id.notify_stop_title, contentTitle);
			CharSequence contentText = null;
			if(isSuccessful) {
				contentText = getString(R.string.notification_download_finished_msg);
				Intent notificationIntent = new Intent(this, InstallMyApkActivity.class);
				Bundle bundle = new Bundle();
				bundle.putString(MyBundleData.FILE_PATH, myApp.getHallApk().getPath());
				notificationIntent.putExtras(bundle);
		        mNotification.contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_ONE_SHOT);
			} else {
				contentText = getString(R.string.notification_download_error_msg);
				this.myApp.tableUnfinishedServices.put(this.getClass().getName(), this.getClass());
			}
			contentView.setTextViewText(R.id.notify_stop_text, contentText);
	        mNotification.contentView = contentView;
	        mNotification.defaults = Notification.DEFAULT_SOUND;
	        mNotificationManager.notify(DownloadApkService.NOTIFICATION_ID, mNotification);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	private void initialNotification() {
        mNotificationManager = (NotificationManager) myApp.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotification = new Notification();
        mNotification.icon = R.drawable.ic_notify_hall;
        mNotification.tickerText = getString(R.string.app_name);
        mNotification.when = System.currentTimeMillis();
        mNotification.flags = Notification.FLAG_AUTO_CANCEL;

        //指定个性化视图  
        RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.notification_downloading);
        CharSequence contentTitle = getString(R.string.app_name);
        CharSequence contentPercent = "0%";
        contentView.setImageViewResource(R.id.notify_downloading_icon, R.drawable.ic_launcher);
        contentView.setTextViewText(R.id.notify_downloading_title, contentTitle);
        contentView.setTextViewText(R.id.notify_downloading_percent, contentPercent);
        contentView.setProgressBar(R.id.notify_downloading_progressbar, 100, 0, false);
        mNotification.contentView = contentView;
        Intent notificationIntent = new Intent();
        mNotification.contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mNotificationManager.notify(DownloadApkService.NOTIFICATION_ID, mNotification);
	}
	private boolean downloadApK() {
		boolean isSuccessful = false;
		String downloadUrl = ResourceServerConstants.APPUPDATE_BASE_URL + AppPrefUtil.getApkNewVersionUri(this, null);
		File apkFile = myApp.getHallApk();
		long onlineContentLength = -1;
		String onlineLastModified = null;
		long expectedContentLength = AppPrefUtil.getApkNewVersionContentLength(this, null);
		String localLastModified = AppPrefUtil.getApkNewVersionLastModified(this, null);
		byte[] buffer = new byte[8192];
		BufferedInputStream bis = null;
		BufferedOutputStream bos = null;
		RandomAccessFile raf = null;
		HttpGet httpGet = new HttpGet(downloadUrl);
		try {
			HttpResponse response = MyHttpClient.getInstance().execute(httpGet);
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				onlineContentLength = response.getEntity().getContentLength();
				Header onlineLastModifiedHeader = response.getFirstHeader("Last-Modified");
				if(onlineLastModifiedHeader != null) onlineLastModified = onlineLastModifiedHeader.getValue();
				boolean isContinueDownload = false;
				if(onlineContentLength == expectedContentLength && apkFile.length() > 0
						&& onlineLastModified != null && localLastModified != null && onlineLastModified.equalsIgnoreCase(localLastModified)) {
					//to do the breakpoint continual transfer
					if(!httpGet.isAborted()) httpGet.abort();
					httpGet = new HttpGet(downloadUrl);
					httpGet.addHeader("Range", "bytes=" + apkFile.length() + "-");
					response = MyHttpClient.getInstance().execute(httpGet);
					Intent receiverIntent = new Intent(this.getPackageName()+DownloadApkReceiver.ACTION_DOWNLOAD_APK);
					receiverIntent.putExtra(DownloadApkService.INTENT_EXTRA_LONG_TOTAL_SIZE, onlineContentLength);
					receiverIntent.putExtra(DownloadApkService.INTENT_EXTRA_BOOLEAN_FINISHED, false);
					if (response.getStatusLine().getStatusCode() == HttpStatus.SC_PARTIAL_CONTENT) {
						bis = new BufferedInputStream(response.getEntity().getContent());
						raf = new RandomAccessFile(apkFile, "rw");
						raf.seek(apkFile.length());
						receiverIntent.putExtra(DownloadApkService.INTENT_EXTRA_LONG_DOWNLOADED_SIZE, apkFile.length());
						sendBroadcast(receiverIntent);
						long startTime = System.currentTimeMillis();
						long currentTime;
						int len = -1;
						while ((len = bis.read(buffer)) != -1) {
							raf.write(buffer, 0, len);
							currentTime = System.currentTimeMillis();
							if((currentTime - startTime) >= 1000 && apkFile.length() != onlineContentLength) {
								int percent = (int)(apkFile.length() * 100 / onlineContentLength);
								RemoteViews contentView = mNotification.contentView;
								CharSequence contentPercent = percent+"%";
								contentView.setTextViewText(R.id.notify_downloading_percent, contentPercent);
								contentView.setProgressBar(R.id.notify_downloading_progressbar, 100, percent, false);
								mNotificationManager.notify(DownloadApkService.NOTIFICATION_ID, mNotification);
								receiverIntent.putExtra(DownloadApkService.INTENT_EXTRA_LONG_DOWNLOADED_SIZE, apkFile.length());
								sendBroadcast(receiverIntent);
								startTime = currentTime;
							}
						}
						raf.close();
						raf = null;
						bis.close();
						bis = null;
					}
					if(apkFile.length() == onlineContentLength) {
						isContinueDownload = true;
						isSuccessful = true;
						java.lang.Thread.sleep(1000);
						receiverIntent.putExtra(DownloadApkService.INTENT_EXTRA_LONG_DOWNLOADED_SIZE, onlineContentLength);
						receiverIntent.putExtra(DownloadApkService.INTENT_EXTRA_BOOLEAN_FINISHED, true);
						sendBroadcast(receiverIntent);
					}
				}
				if(!isContinueDownload) {
					//restart download apk file
					if(httpGet != null && !httpGet.isAborted()) httpGet.abort();
					httpGet = new HttpGet(downloadUrl);
					response = MyHttpClient.getInstance().execute(httpGet);
					SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
					Editor editor = pref.edit();
					AppPrefUtil.setApkNewVersionContentLength(this, editor, onlineContentLength);
					AppPrefUtil.setApkNewVersionLastModified(this, editor, onlineLastModified);
					editor.apply();
					
					if(apkFile.exists()) apkFile.delete();
					bis = new BufferedInputStream(response.getEntity().getContent());
					bos = new BufferedOutputStream(new FileOutputStream(apkFile));
					Intent receiverIntent = new Intent(this.getPackageName()+DownloadApkReceiver.ACTION_DOWNLOAD_APK);
					receiverIntent.putExtra(DownloadApkService.INTENT_EXTRA_LONG_DOWNLOADED_SIZE, 0l);
					receiverIntent.putExtra(DownloadApkService.INTENT_EXTRA_LONG_TOTAL_SIZE, onlineContentLength);
					receiverIntent.putExtra(DownloadApkService.INTENT_EXTRA_BOOLEAN_FINISHED, false);
					sendBroadcast(receiverIntent);
					long startTime = System.currentTimeMillis();
					long currentTime;
					int len = -1;
					while ((len = bis.read(buffer)) != -1) {
						bos.write(buffer, 0, len);
						currentTime = System.currentTimeMillis();
						if((currentTime - startTime) >= 1000 && apkFile.length() != onlineContentLength) {
							int percent = (int)(apkFile.length()  * 100 / onlineContentLength);
							RemoteViews contentView = mNotification.contentView;
							CharSequence contentPercent = percent+"%";
							contentView.setTextViewText(R.id.notify_downloading_percent, contentPercent);
							contentView.setProgressBar(R.id.notify_downloading_progressbar, 100, percent, false);
							mNotificationManager.notify(DownloadApkService.NOTIFICATION_ID, mNotification);
							receiverIntent.putExtra(DownloadApkService.INTENT_EXTRA_LONG_DOWNLOADED_SIZE, apkFile.length());
							sendBroadcast(receiverIntent);
							startTime = currentTime;
						}
					}
					bos.flush();
					bos.close();
					bos = null;
					bis.close();
					bis = null;
					java.lang.Thread.sleep(1000);
					receiverIntent.putExtra(DownloadApkService.INTENT_EXTRA_LONG_DOWNLOADED_SIZE, onlineContentLength);
					receiverIntent.putExtra(DownloadApkService.INTENT_EXTRA_BOOLEAN_FINISHED, true);
					sendBroadcast(receiverIntent);
					isSuccessful = true;
				}
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
		return isSuccessful;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
	}
}

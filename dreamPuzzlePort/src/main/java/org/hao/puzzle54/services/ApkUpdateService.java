package org.hao.puzzle54.services;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Xml;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.hao.puzzle54.AppPrefUtil;
import org.hao.puzzle54.MyApp;
import org.hao.puzzle54.MyHttpClient;
import org.hh.puzzle.port54.hall.R;
import org.xmlpull.v1.XmlPullParser;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class ApkUpdateService  extends IntentService {
	private static final String TAG = ApkUpdateService.class.getName();
	private static final String APPUPDATE_XML_FILENAME="appupdate.xml";
	private MyApp myApp;
	
	public ApkUpdateService() {
		super(TAG);
	}
	@Override
	protected void onHandleIntent(Intent intent) {
		boolean isSuccessful = false;
		if(this.myApp == null) this.myApp = (MyApp)this.getApplicationContext();
//		if(AppPrefUtil.getRunTimesOfApp(this, null) <= 3) return;
		isSuccessful = downloadAppUpdateXml();
		if(isSuccessful) {
			isSuccessful = checkUpdate();
		}
		if(!isSuccessful) {
			this.myApp.tableUnfinishedServices.put(this.getClass().getName(), this.getClass());
		}
	}
	private boolean downloadAppUpdateXml() {
		boolean isSuccessful = false;
		long onlineContentLength = -1;
		String onlineLastModified = null;
		File appupdateFile = new File(myApp.getAppFilesPath(true)+APPUPDATE_XML_FILENAME);
		String localLastModified = AppPrefUtil.getAppsupdateXmlLastModified(this, null);
//			HttpClient httpClient = this.myApp.getHttpClient();
			HttpGet httpGet = new HttpGet(ResourceServerConstants.APPUPDATE_XML_URL);
			byte[] buffer = new byte[4096];
			BufferedInputStream bis = null;
			BufferedOutputStream bos = null;
			try {
				HttpResponse response = MyHttpClient.getInstance().execute(httpGet);
				if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
					onlineContentLength = response.getEntity().getContentLength();
					try {
						onlineLastModified = response.getFirstHeader("Last-Modified").getValue();
					} catch(Exception e) {
                        e.printStackTrace();
                    }
					if(appupdateFile.length() != onlineContentLength || localLastModified==null || onlineLastModified==null || !localLastModified.equalsIgnoreCase(onlineLastModified)) {
						bis = new BufferedInputStream(response.getEntity().getContent());
						bos = new BufferedOutputStream(new FileOutputStream(appupdateFile));
						int len = -1;
						while ((len = bis.read(buffer)) != -1) {
							bos.write(buffer, 0, len);
						}
						bos.flush();
						SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
						Editor editor = pref.edit();
						AppPrefUtil.setAppsupdateXmlLastModified(this, editor, onlineLastModified);
						editor.apply();
					}
					isSuccessful = true;
				}
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
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
			
		return isSuccessful;
	}
	private boolean checkUpdate() {
		boolean isSuccessful = false;
		BufferedInputStream bis = null;
		try {
			File appsupdateFile = new File(this.myApp.getAppFilesPath(true)+APPUPDATE_XML_FILENAME);
			if(!appsupdateFile.exists() || appsupdateFile.length()<=0) return isSuccessful;
			bis = new BufferedInputStream(new FileInputStream(appsupdateFile));
			XmlPullParser parser = Xml.newPullParser();
			parser.setInput(bis, "UTF-8");
			int onlineNewVersionCode = 0;
			String onlineNewVersionUrl = null;
			String onlineNewVersionGoogleUrl = null;
			String onlineNewVersionPackage = null;
			int eventType = parser.getEventType();
			while(eventType!=XmlPullParser.END_DOCUMENT) {
				switch(eventType){
				case XmlPullParser.START_DOCUMENT:
					break;
				case XmlPullParser.START_TAG:
					if(AppUpdateXml.NODE_APP.equals(parser.getName())) {
						if(getString(R.string.theme_code).equals(parser.getAttributeValue("", AppUpdateXml.ATTR_NAME))) {
							onlineNewVersionCode = Integer.parseInt(parser.getAttributeValue("", AppUpdateXml.ATTR_VERSION_CODE));
							onlineNewVersionUrl = parser.getAttributeValue("", AppUpdateXml.ATTR_URL);
							onlineNewVersionGoogleUrl = parser.getAttributeValue("", AppUpdateXml.ATTR_GOOGLE_URL);
							onlineNewVersionPackage = parser.getAttributeValue("", AppUpdateXml.ATTR_APP_PACKAGE);
						}
					}
					break;
				case XmlPullParser.END_TAG:
					break;
				}
				if(onlineNewVersionUrl != null) break;
				eventType = parser.next();
			}
			if(onlineNewVersionCode > 0 && onlineNewVersionUrl != null) {
				boolean isFirstFindNewVersion = false;
				SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
				if(AppPrefUtil.getApkNewVersionCode(this, pref) != onlineNewVersionCode || AppPrefUtil.getApkNewVersionUri(this, pref) == null || !AppPrefUtil.getApkNewVersionUri(this, pref).equals(onlineNewVersionUrl)) {
					isFirstFindNewVersion = true;
					Editor editor = pref.edit();
					AppPrefUtil.setApkNewVersionCode(this, editor, onlineNewVersionCode);
					AppPrefUtil.setApkNewVersionUri(this, editor, onlineNewVersionUrl);
					AppPrefUtil.setApkNewVersionUri(this, editor, onlineNewVersionUrl);
					AppPrefUtil.setApkNewVersionGoogleUri(this, editor, onlineNewVersionGoogleUrl);
					AppPrefUtil.setApkNewVersionPackage(this, editor, onlineNewVersionPackage);
					editor.apply();
				}
				if(AppPrefUtil.hasNewApkVersion(this, null)) {
					if(isFirstFindNewVersion) {
						Intent tempIntent = new Intent(this.getPackageName() + ApkUpdateReceiver.ACTION_APPSUPDATE);
						sendBroadcast(tempIntent);
					}
				} else {
					File apkFile = myApp.getHallApk();
					if(apkFile.exists()) apkFile.delete();
				}
			}
			isSuccessful = true;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(bis != null) {
				try {
					bis.close();
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
	
	private static final class AppUpdateXml {
		private static final String NODE_APP="app";
		private static final String ATTR_NAME="name";
		private static final String ATTR_APP_PACKAGE="app_package";
		private static final String ATTR_VERSION_CODE="version_code";
		private static final String ATTR_URL="url";
		private static final String ATTR_GOOGLE_URL="google_url";
	}
}

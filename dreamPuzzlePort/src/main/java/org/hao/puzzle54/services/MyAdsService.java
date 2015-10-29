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
import org.hao.puzzle54.AppConstants;
import org.xmlpull.v1.XmlPullParser;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class MyAdsService extends IntentService {
	private static final String TAG = MyAdsService.class.getName();
	private static final String ADS_XML_FILENAME="ads.xml";
	private MyApp myApp;
	
	public MyAdsService() {
		super(TAG);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		boolean isSuccessful = false;
		if(this.myApp == null) this.myApp = (MyApp)this.getApplicationContext();
		downloadAdsXmlByVolley();
//		isSuccessful = downloadAdsXml();
//		if(isSuccessful) {
//			isSuccessful = updateAdId();
//		}
//		if(!isSuccessful) {
//			this.myApp.tableUnfinishedServices.put(this.getClass().getName(), this.getClass());
//		}
	}
	private void downloadAdsXmlByVolley() {

	}
	private boolean downloadAdsXml() {
		boolean isSuccessful = false;
		long onlineContentLength = -1;
		String onlineLastModified = null;
		File adsFile = new File(myApp.getAppFilesPath(true)+ADS_XML_FILENAME);
		String localLastModified = AppPrefUtil.getAdsXmlLastModified(this, null);
			HttpGet httpGet = new HttpGet(ResourceServerConstants.ADS_XML_URL);
			byte[] buffer = new byte[2048];
			BufferedInputStream bis = null;
			BufferedOutputStream bos = null;
			try {
				HttpResponse response = MyHttpClient.getInstance().execute(httpGet);
				if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
					onlineContentLength = response.getEntity().getContentLength();
					try {
						onlineLastModified = response.getFirstHeader("Last-Modified").getValue();
					} catch(Exception e){
                        e.printStackTrace();
                    }
					if(adsFile.length() != onlineContentLength || localLastModified==null || onlineLastModified==null || !localLastModified.equalsIgnoreCase(onlineLastModified)) {
						bis = new BufferedInputStream(response.getEntity().getContent());
						bos = new BufferedOutputStream(new FileOutputStream(adsFile));
						int len = -1;
						while ((len = bis.read(buffer)) != -1) {
							bos.write(buffer, 0, len);
						}
						bos.flush();
						SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
						Editor editor = pref.edit();
						AppPrefUtil.setAdsXmlLastModified(this, editor, onlineLastModified);
						editor.apply();
					}
					isSuccessful = true;
				}
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if(!httpGet.isAborted()) httpGet.abort();
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
	private boolean updateAdId() {
		boolean isSuccessful = false;
		String bannerId = null;
		String defaultBannerId = null;
		String interstitialId = null;
		String defaultInterstitialId = null;
		BufferedInputStream bis = null;
		try{
			File adsFile = new File(myApp.getAppFilesPath(true)+ADS_XML_FILENAME);
			if(!adsFile.exists() || adsFile.length()<=0) return isSuccessful;
			bis = new BufferedInputStream(new FileInputStream(adsFile));
			XmlPullParser parser = Xml.newPullParser();
			parser.setInput(bis, "UTF-8");
			int eventType = parser.getEventType();
			while(eventType!=XmlPullParser.END_DOCUMENT && bannerId == null){
				switch(eventType){
				case XmlPullParser.START_DOCUMENT:
					break;
				case XmlPullParser.START_TAG:
					if(AdsXml.NODE_ROOT.equals(parser.getName())) {
						defaultBannerId = parser.getAttributeValue("", AdsXml.ATTR_BANNER_ID);
						defaultInterstitialId = parser.getAttributeValue("", AdsXml.ATTR_INTERSTITIAL_ID);
						String force = parser.getAttributeValue("", AdsXml.ATTR_FORCE);
						if(force != null && Boolean.parseBoolean(force)) {
							bannerId = defaultBannerId;
							interstitialId = defaultInterstitialId;
						}
					} else if(AdsXml.NODE_APP.equals(parser.getName())) {
						if(AppConstants.DREAMPUZZLE_HALL_CODE.equals(parser.getAttributeValue("", AdsXml.ATTR_NAME))) {
							bannerId = parser.getAttributeValue("", AdsXml.ATTR_BANNER_ID);
							interstitialId = parser.getAttributeValue("", AdsXml.ATTR_INTERSTITIAL_ID);
						}
					}
					break;
				case XmlPullParser.END_TAG:
					break;
				}
				if(bannerId != null) break;
				eventType = parser.next();
			}
			if(bannerId == null) bannerId = defaultBannerId;
			if(interstitialId == null) interstitialId = defaultInterstitialId;
			if(bannerId != null) {
				String oldBannerId = AppPrefUtil.getAdBannerId(this, null);
				if(oldBannerId == null || !oldBannerId.equals(bannerId)) {
					SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
					Editor editor = pref.edit();
					AppPrefUtil.setAdBannerId(this, editor, bannerId);
					editor.apply();
					Intent tempIntent = new Intent(this.getPackageName() + MyAdsReceiver.ACTION_AD_CHANGED);
					sendBroadcast(tempIntent);
				}
			}
			if(interstitialId != null) {
				String oldInterstitialId = AppPrefUtil.getAdInterstitialId(this, null);
				if(oldInterstitialId == null || !oldInterstitialId.equals(interstitialId)) {
					SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
					Editor editor = pref.edit();
					AppPrefUtil.setAdInterstitialId(this, editor, interstitialId);
					editor.apply();
				}
			}
			
			isSuccessful = true;
		} catch(Exception e) {
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

	private static final class AdsXml {
		private static final String NODE_ROOT="apps";
		private static final String NODE_APP="app";
		private static final String ATTR_NAME="name";
		private static final String ATTR_FORCE="force";
		private static final String ATTR_BANNER_ID="banner_id";
		private static final String ATTR_INTERSTITIAL_ID="interstitial_id";
	}
}

package org.hao.puzzle54.services;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import android.util.Xml;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.hao.database.DBHelperMorepuzzles;
import org.hao.puzzle54.AppPrefUtil;
import org.hao.puzzle54.AppTools;
import org.hao.puzzle54.MyApp;
import org.hao.puzzle54.MyHttpClient;
import org.hao.puzzle54.PicsPackageEntity;
import org.xmlpull.v1.XmlPullParser;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class UpdatePuzzlesXmlService extends IntentService {
	public static final String TAG = UpdatePuzzlesXmlService.class.getName();
	private static final String PUZZLES_XML_FILENAME="puzzles.xml";
	public static int intNewPackages=0;
	private boolean isNeedToStartDownloadZipMonitor;
	private MyApp myApp;
	
	public UpdatePuzzlesXmlService() {
		super(TAG);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		boolean isSuccessful;
		if(this.myApp == null) this.myApp = (MyApp)this.getApplicationContext();
		try {
			isSuccessful = downloadPuzzlesXml();
			if(isSuccessful) {
				isSuccessful = updateXml();
			}
		} catch (Exception e) {
			isSuccessful = false;
		}
		if(isSuccessful) {
			if(isNeedToStartDownloadZipMonitor) {
				startService(new Intent(this, DownloadZipMonitorService.class));
				sendBroadcast(new Intent(this.getPackageName()+UpdatePuzzlesXmlReceiver.ACTION_UPDATE_ALL_XML));
			} else {
				sendBroadcast(new Intent(this.getPackageName()+UpdatePuzzlesXmlReceiver.ACTION_UPDATE_XML));
			}
		} else {
			this.myApp.tableUnfinishedServices.put(this.getClass().getName(), this.getClass());
		}

	}
	private boolean downloadPuzzlesXml() {
		boolean isSuccessful = false;
		long onlineContentLength = -1;
		String onlineLastModified = null;
		File puzzlesFile = new File(myApp.getAppFilesPath(true)+PUZZLES_XML_FILENAME);
		String localLastModified = AppPrefUtil.getPuzzlesXmlLastModified(this, null);
			HttpGet httpGet;
			if(this.myApp.isAdult()) {
				httpGet = new HttpGet(ResourceServerConstants.PACKAGES_ADULT_XML_URL);
			} else {
				httpGet = new HttpGet(ResourceServerConstants.PACKAGES_KIDS_XML_URL);
			}
			byte[] buffer = new byte[4096];
			BufferedInputStream bis = null;
			BufferedOutputStream bos = null;
			try {
				HttpResponse response = MyHttpClient.getInstance().execute(httpGet);
				if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
					onlineContentLength = response.getEntity().getContentLength();
					try {
						onlineLastModified = response.getFirstHeader("Last-Modified").getValue();
					} catch(Exception e){
                        e.printStackTrace();
                    }
					if(puzzlesFile.length() != onlineContentLength || localLastModified==null || onlineLastModified==null || !localLastModified.equalsIgnoreCase(onlineLastModified)) {
						bis = new BufferedInputStream(response.getEntity().getContent());
						bos = new BufferedOutputStream(new FileOutputStream(puzzlesFile));
						int len = -1;
						while ((len = bis.read(buffer)) != -1) {
							bos.write(buffer, 0, len);
						}
						bos.flush();
						SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
						Editor editor = pref.edit();
						AppPrefUtil.setPuzzlesXmlLastModified(this, editor, onlineLastModified);
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
	private boolean parsePuzzlesXml(List<PicsPackageEntity> list) throws Exception {
		boolean isSuccessful = false;
		BufferedInputStream bis = null;
		File puzzlesFile = new File(myApp.getAppFilesPath(true)+PUZZLES_XML_FILENAME);
		if(!puzzlesFile.exists() || puzzlesFile.length()<=0) return isSuccessful;
		try {
			bis = new BufferedInputStream( new FileInputStream(puzzlesFile));
			XmlPullParser parser = Xml.newPullParser();
			parser.setInput(bis, "UTF-8");
			int eventType = parser.getEventType();
			PicsPackageEntity packageEntity = null;
			boolean blnMatched = false;
			while(eventType!=XmlPullParser.END_DOCUMENT){
				switch(eventType){
				case XmlPullParser.START_DOCUMENT:
					break;
				case XmlPullParser.START_TAG:
					if(MorepuzzlesXML.NODE_PUZZLES.equalsIgnoreCase(parser.getName())) {
					} else if(MorepuzzlesXML.NODE_PACKAGE.equalsIgnoreCase(parser.getName())) {
						packageEntity = new PicsPackageEntity();
						blnMatched = false;
						packageEntity.setName(parser.getAttributeValue("", MorepuzzlesXML.ATTR_NAME));
						packageEntity.setCode(parser.getAttributeValue("", MorepuzzlesXML.ATTR_CODE));
						packageEntity.setIconVersion(Integer.parseInt(parser.getAttributeValue("", MorepuzzlesXML.ATTR_ICON_VERSION)));
						packageEntity.setIcon(parser.getAttributeValue("", MorepuzzlesXML.ATTR_ICON_URL));
					} else if(MorepuzzlesXML.NODE_PACKAGE_ZIP.equalsIgnoreCase(parser.getName())) {
						String strXmlWidth = parser.getAttributeValue("", MorepuzzlesXML.ATTR_WIDTH);
						if(strXmlWidth !=null && strXmlWidth.split(",").length==2) {
							int[] minMaxWidth = {Integer.valueOf(strXmlWidth.split(",")[0]), Integer.valueOf(strXmlWidth.split(",")[1])};
							if(this.myApp.getDisplay().widthPixels >= minMaxWidth[0] && this.myApp.getDisplay().widthPixels <= minMaxWidth[1]) {
								blnMatched = true;
								packageEntity.setZipVersion(Integer.parseInt(parser.getAttributeValue("", MorepuzzlesXML.ATTR_ZIP_VERSION)));
								packageEntity.setZip(parser.getAttributeValue("", MorepuzzlesXML.ATTR_ZIP_URL));
							}
						}
					}
					break;
				case XmlPullParser.END_TAG:
					if(MorepuzzlesXML.NODE_PACKAGE_ZIP.equalsIgnoreCase(parser.getName())) {
						if(packageEntity != null && blnMatched) {
							list.add(packageEntity);
							packageEntity = null;
							blnMatched = false;
							while(true) {
								int intType = parser.next();
								if(intType == XmlPullParser.END_TAG && MorepuzzlesXML.NODE_PACKAGE.equalsIgnoreCase(parser.getName())) {
									break;
								}
							}
						}
					}
					break;
				}
				eventType = parser.next();
			}
			isSuccessful = true;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(bis != null) bis.close();
		}
		return isSuccessful;
	}
	private boolean updateXml() throws Exception {
		boolean isSuccessful = false;
		HashMap<String, PicsPackageEntity> mapMatchedOnlineEntity = new HashMap<String, PicsPackageEntity>();
		List<PicsPackageEntity> listOnline = new ArrayList<PicsPackageEntity>();
		if(parsePuzzlesXml(listOnline)) {
			SQLiteDatabase dbMorepuzzles = DBHelperMorepuzzles.getInstance(this).getWritableDatabase();
			try{
				while(!listOnline.isEmpty()) {
					PicsPackageEntity entityOnline = listOnline.remove(0);
					PicsPackageEntity entityDb = DBHelperMorepuzzles.getInstance(this).getEntityByCode(entityOnline.getCode(), dbMorepuzzles);
					mapMatchedOnlineEntity.put(entityOnline.getCode(), entityOnline);
					if(entityDb != null) {
						boolean isChanged = false;
						boolean isNewIcon = false;
						if(entityDb.getIconVersion() < entityOnline.getIconVersion()) {
							isChanged = true;
							isNewIcon = true;
							entityDb.setIcon(entityOnline.getIcon());
							entityDb.setIconVersion(entityOnline.getIconVersion());
							entityDb.setIconState(PicsPackageEntity.IconStates.OLDVERSION);
						}
						if(entityDb.getZipVersion() < entityOnline.getZipVersion()) {
							isChanged = true;
							++UpdatePuzzlesXmlService.intNewPackages;
							entityDb.setZip(entityOnline.getZip());
							entityDb.setZipVersion(entityOnline.getZipVersion());
							entityDb.setZipSize(0);
							if(entityDb.getState().equalsIgnoreCase(PicsPackageEntity.PackageStates.INSTALLED)) {
								entityDb.setState(PicsPackageEntity.PackageStates.OLDVERSION);
							} else if(entityDb.getState().equalsIgnoreCase(PicsPackageEntity.PackageStates.DOWNLOADING)
									|| entityDb.getState().equalsIgnoreCase(PicsPackageEntity.PackageStates.PAUSING)) {
								entityDb.setState(PicsPackageEntity.PackageStates.SCHEDULED);
							}
						}
						if(!entityOnline.getIcon().equals(entityDb.getIcon())) {
							isChanged = true;
							isNewIcon = true;
							entityDb.setIcon(entityOnline.getIcon());
						}
						if(!entityOnline.getZip().equals(entityDb.getZip())) {
							isChanged = true;
							entityDb.setZip(entityOnline.getZip());
						}
						if(!entityOnline.getName().equals(entityDb.getName())) {
							isChanged = true;
							entityDb.setName(entityOnline.getName());
						}
						if(isChanged) {
							DBHelperMorepuzzles.getInstance(this).update(entityDb, dbMorepuzzles);
							if(isNewIcon) AppTools.addDownloadingIcon(entityOnline.getCode());
						}
					} else {
						entityOnline.setIconState(PicsPackageEntity.IconStates.OLDVERSION);
						entityOnline.setState(PicsPackageEntity.PackageStates.NOTINSTALL);
						++UpdatePuzzlesXmlService.intNewPackages;
						DBHelperMorepuzzles.getInstance(this).insert(entityOnline, dbMorepuzzles);
						AppTools.addDownloadingIcon(entityOnline.getCode());
					}
				}
				if(AppTools.getDownloadingIcon()!=null) startService(new Intent(this, DownloadIconService.class));
				List<PicsPackageEntity> copyAllPuzzles = DBHelperMorepuzzles.getInstance(this).getAllPuzzles(dbMorepuzzles);
				while(copyAllPuzzles != null && !copyAllPuzzles.isEmpty()) {
					PicsPackageEntity tempEntity = copyAllPuzzles.remove(0);
					if(mapMatchedOnlineEntity.get(tempEntity.getCode()) == null) {
						DBHelperMorepuzzles.getInstance(this).updateZipUrlIsNull(tempEntity.getCode(), dbMorepuzzles);
					}
				}
				isSuccessful = true;
			} catch(Exception e) {
				e.printStackTrace();
			} finally {
				if(dbMorepuzzles != null) dbMorepuzzles.close();
			}
		}
		return isSuccessful;
	}	
	@Override
	public void onDestroy() {
		super.onDestroy();
	}
	public static final class MorepuzzlesXML {
		public static final String NODE_PUZZLES = "puzzles";
		public static final String NODE_PACKAGE = "package";
		public static final String NODE_PACKAGE_ZIP = "zip";
		public static final String ATTR_NAME = "name";
		public static final String ATTR_CODE = "code";
		public static final String ATTR_ICON_VERSION = "icon_version";
		public static final String ATTR_ICON_URL = "icon_url";
		public static final String ATTR_ZIP_VERSION = "zip_version";
		public static final String ATTR_WIDTH = "width";
		public static final String ATTR_ZIP_URL = "zip_url";
	}
	
}

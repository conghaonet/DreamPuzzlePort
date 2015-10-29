package org.hao.puzzle54.services;

import android.app.IntentService;
import android.content.Intent;

import org.hao.database.DBHelperMorepuzzles;
import org.hao.puzzle54.MyApp;
import org.hao.puzzle54.PicsPackageEntity;

public class DownloadZipMonitorService extends IntentService {
	private static final String TAG = DownloadZipMonitorService.class.getName();
	
	public DownloadZipMonitorService() {
		super(TAG);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		MyApp myApp = (MyApp)this.getApplicationContext();
		if(myApp.getConnectivityNetworkName() == null) {
			myApp.tableUnfinishedServices.put(this.getClass().getName(), this.getClass());
			return;
		}
		try {
			java.lang.Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		PicsPackageEntity entity = DBHelperMorepuzzles.getInstance(this).getDownloadingEntity(null);
		if(entity == null) {
			entity = DBHelperMorepuzzles.getInstance(this).getFirstScheduledEntity(null);
		}
		if(entity != null) {
			Intent downloadIntent = new Intent();
			downloadIntent.setClass(this, DownloadZipService.class);
			downloadIntent.putExtra("package_code", entity.getCode());
			startService(downloadIntent);
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}
}

package org.hao.puzzle54.services;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class DownloadZipReceiver extends BroadcastReceiver {
	public static final String ACTION_DOWNLOAD_ZIP = DownloadZipReceiver.class.getSimpleName()+"_ACTION_DOWNLOAD_ZIP";
	public static final String ACTION_DOWNLOAD_FINISHED = DownloadZipReceiver.class.getSimpleName()+"_ACTION_DOWNLOAD_FINISHED";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(context.getPackageName()+DownloadZipReceiver.ACTION_DOWNLOAD_FINISHED)) { 
			context.startService(new Intent(context, DownloadZipMonitorService.class));
		}
	}
}

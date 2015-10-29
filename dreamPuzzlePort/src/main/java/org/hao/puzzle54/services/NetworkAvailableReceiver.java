package org.hao.puzzle54.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;

import org.hao.puzzle54.MyApp;

import java.util.Enumeration;

public class NetworkAvailableReceiver extends BroadcastReceiver {
	private String lastNetworkName;
	public static final String ACTION_AVAILABLE = NetworkAvailableReceiver.class.getSimpleName()+"_ACTION_AVAILABLE";
	private MyApp myApp;
	public NetworkAvailableReceiver() {
	}
	@Override
	public void onReceive(Context context, Intent intent) {
        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
        	if(myApp == null) myApp = (MyApp)context.getApplicationContext();
            String currentNetworkName = myApp.getConnectivityNetworkName();
            if(currentNetworkName != null && !currentNetworkName.equals(lastNetworkName)) {
               	Enumeration<String> enumKeys = myApp.tableUnfinishedServices.keys();
               	while(enumKeys.hasMoreElements()) {
               		String key = enumKeys.nextElement();
               		Class tempClass = myApp.tableUnfinishedServices.remove(key);
               		context.startService(new Intent(context, tempClass));
               	}
            }
            lastNetworkName = currentNetworkName;
        }
	}
}

package org.hao.puzzle54.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class UpdatePuzzlesXmlReceiver extends BroadcastReceiver {
	public static final String ACTION_UPDATE_XML = UpdatePuzzlesXmlReceiver.class.getSimpleName()+"_ACTION_UPDATE_XML";
	public static final String ACTION_UPDATE_ALL_XML = UpdatePuzzlesXmlReceiver.class.getSimpleName()+"_ACTION_UPDATE_ALL_XML";
	@Override
	public void onReceive(Context context, Intent intent) {
		
	}

}

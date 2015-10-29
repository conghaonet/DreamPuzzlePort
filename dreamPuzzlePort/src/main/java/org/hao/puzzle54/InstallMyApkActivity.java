package org.hao.puzzle54;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import java.io.File;

public class InstallMyApkActivity extends Activity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if(this.getIntent().getExtras().containsKey(MyBundleData.FILE_PATH)) {
			String filePath = this.getIntent().getExtras().getString(MyBundleData.FILE_PATH);
			File apkFile = new File(filePath);
			if(apkFile != null && apkFile.exists()) {
				try {
					Uri uriHallFile = Uri.fromFile(apkFile);
					Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setDataAndType(uriHallFile, "application/vnd.android.package-archive");
					startActivity(intent);
					MyApp myApp = (MyApp)getApplicationContext();
					myApp.setUpadtedPackageNameInApkUpdateProp();
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		}
		this.finish();
	}
}

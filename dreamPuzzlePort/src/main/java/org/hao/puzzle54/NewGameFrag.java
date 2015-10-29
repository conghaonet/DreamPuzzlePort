package org.hao.puzzle54;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.flurry.android.FlurryAgent;

import org.hao.database.DBHelperMorepuzzles;
import org.hao.database.DBHelperScore;
import org.hao.puzzle54.services.BundleDataDownloadZip;
import org.hao.puzzle54.services.DownloadIconReceiver;
import org.hao.puzzle54.services.DownloadIconService;
import org.hao.puzzle54.services.DownloadZipMonitorService;
import org.hao.puzzle54.services.DownloadZipReceiver;
import org.hao.puzzle54.services.UpdatePuzzlesXmlReceiver;
import org.hao.puzzle54.services.UpdatePuzzlesXmlService;
import org.hh.puzzle.port54.hall.R;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class NewGameFrag extends Fragment {
	private static final String TAG = NewGameFrag.class.getName();
	private static final int HANDLER_MSG_REFRESH_DATA = 101;
	private static final int HANDLER_MSG_REFRESH_ALL_DATA = 102;
	private static final int HANDLER_MSG_NETWORK_ERR = 103;
	private static final int HANDLER_MSG_REFRESH_GRID = 104;
	private static final int RESERVED_GRID_ITEMS = 2;
	private MyHandler mHandler;
	private MyGridAdapter gridAdapter;
	private InitialTask initialTask;
	private List<Integer> listMyEntity;
	private DownloadIconReceiver iconReceiver;
	private DownloadZipReceiver zipReceiver;
	private UpdatePuzzlesXmlReceiver updatePuzzlesXmlReceiver;
	private List<PicsPackageEntity> listAllPuzzles;
	private ProgressBar progressBar;
	private Bitmap iconEmptyFrame;
	private BundleDataDownloadZip bundleDataDownloadZip;
	private PackageGridActivityCallBack mCallBack;
	private int intGridItemWidth;
	
	/**
	 * 当该Fragment被添加,显示到Activity时调用该方法
	 * 在此判断显示到的Activity是否已经实现了接口
	 */
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		Log.d(TAG, TAG+"--onAttach");
		if (!(activity instanceof PackageGridActivityCallBack)) {
			throw new IllegalStateException("NewGameFrag所在的Activity必须实现PackageGridActivityCallBack接口");
		}
		mCallBack = (PackageGridActivityCallBack)activity;
	}
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, TAG+"--onCreate");
        intGridItemWidth = AppTools.getIconScaleWidth(this.mCallBack.getMyApp().getDisplay().widthPixels, getResources().getInteger(R.integer.pics_grid_columns));
    }
	static class MyHandler extends Handler {
		WeakReference<NewGameFrag> mFragment;
		MyHandler(NewGameFrag fragment) {
			mFragment = new WeakReference<NewGameFrag>(fragment);
		}
		@Override
		public void handleMessage(Message msg) {
			NewGameFrag theFragment = mFragment.get();
		    if(theFragment == null) return;
		    switch (msg.what) {
      		case NewGameFrag.HANDLER_MSG_REFRESH_DATA:
      			theFragment.copyAllPuzzles();
      			theFragment.gridAdapter.notifyDataSetChanged();
      			break;
	        case NewGameFrag.HANDLER_MSG_REFRESH_ALL_DATA:
		        theFragment.copyAllPuzzles();
		        theFragment.listMyEntity.clear();
		        theFragment.listMyEntity = new ArrayList<Integer>();
		        for(int i=0;i<theFragment.listAllPuzzles.size();i++) {
			        PicsPackageEntity entity = theFragment.listAllPuzzles.get(i);
			        if(!entity.getState().equalsIgnoreCase(PicsPackageEntity.PackageStates.NOTINSTALL)) {
				        theFragment.listMyEntity.add(i);
			        }
		        }
		        theFragment.gridAdapter.notifyDataSetChanged();
		        break;
  			case NewGameFrag.HANDLER_MSG_NETWORK_ERR:
  				break;
  			case NewGameFrag.HANDLER_MSG_REFRESH_GRID:
  				theFragment.gridAdapter.notifyDataSetChanged();
  				break;	
  			default:
  				break;
		    }
		}
	}	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.d(TAG, TAG+"--onCreateView");
		View rootView = inflater.inflate(R.layout.grid_fragment, container, false);
		this.progressBar = (ProgressBar)rootView.findViewById(R.id.grid_fragment_progress_bar);
		this.progressBar.setVisibility(View.GONE);
		GridView newGameGridView = (GridView) rootView.findViewById(R.id.grid_fragment_grid_view);
		this.gridAdapter = new MyGridAdapter(this.mCallBack.getMyApp());
		newGameGridView.setAdapter(gridAdapter);
		newGameGridView.setOnItemClickListener(new MyItemClickListener());
		newGameGridView.setOnItemLongClickListener(new MyItemLongClickListener());
        mHandler = new MyHandler(this);
		return rootView;
	}
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		Log.d(TAG, TAG+"--onActivityCreated");

		iconReceiver = new DownloadIconReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				NewGameFrag.this.mHandler.sendMessage(NewGameFrag.this.mHandler.obtainMessage(NewGameFrag.HANDLER_MSG_REFRESH_DATA));
			}
		};
		zipReceiver = new DownloadZipReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				Bundle bundle = intent.getExtras();
				bundleDataDownloadZip = (BundleDataDownloadZip)bundle.getSerializable(BundleDataDownloadZip.class.getName());
				NewGameFrag.this.mHandler.sendMessage(NewGameFrag.this.mHandler.obtainMessage(NewGameFrag.HANDLER_MSG_REFRESH_GRID));
			}
		};
		updatePuzzlesXmlReceiver = new UpdatePuzzlesXmlReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if(intent.getAction().equals(NewGameFrag.this.mCallBack.getMyApp().getPackageName()+UpdatePuzzlesXmlReceiver.ACTION_UPDATE_ALL_XML))
					NewGameFrag.this.mHandler.sendMessage(NewGameFrag.this.mHandler.obtainMessage(NewGameFrag.HANDLER_MSG_REFRESH_ALL_DATA));
				else
					NewGameFrag.this.mHandler.sendMessage(NewGameFrag.this.mHandler.obtainMessage(NewGameFrag.HANDLER_MSG_REFRESH_DATA));
			}
		};
		
		initialTask = new InitialTask();
		initialTask.execute();
	}
	@Override
	public void onStart() {
		super.onStart();
		Log.d(TAG, TAG+"--onStart");
		FlurryAgent.onStartSession(this.getActivity());
		IntentFilter filterIcon = new IntentFilter();
		filterIcon.addAction(NewGameFrag.this.mCallBack.getMyApp().getPackageName()+DownloadIconReceiver.ACTION_DOWNLOAD_ICON);
		NewGameFrag.this.getActivity().registerReceiver(iconReceiver, filterIcon);
		IntentFilter filterZip = new IntentFilter();
		filterZip.addAction(NewGameFrag.this.mCallBack.getMyApp().getPackageName()+DownloadZipReceiver.ACTION_DOWNLOAD_ZIP);
		NewGameFrag.this.getActivity().registerReceiver(zipReceiver, filterZip);
		IntentFilter filterUpdatePuzzlesXml = new IntentFilter();
		filterUpdatePuzzlesXml.addAction(NewGameFrag.this.mCallBack.getMyApp().getPackageName()+UpdatePuzzlesXmlReceiver.ACTION_UPDATE_XML);
		filterUpdatePuzzlesXml.addAction(NewGameFrag.this.mCallBack.getMyApp().getPackageName()+UpdatePuzzlesXmlReceiver.ACTION_UPDATE_ALL_XML);
		NewGameFrag.this.getActivity().registerReceiver(updatePuzzlesXmlReceiver, filterUpdatePuzzlesXml);
	}
	@Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, TAG+"--onResume");
    }
	@Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, TAG+"--onPause");
    }
	@Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, TAG+"--onStop");
        this.getActivity().unregisterReceiver(this.iconReceiver);
        this.getActivity().unregisterReceiver(this.zipReceiver);
        this.getActivity().unregisterReceiver(this.updatePuzzlesXmlReceiver);
        FlurryAgent.onEndSession(this.getActivity());
    }
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		Log.d(TAG, TAG+"--onDestroyView");
		if(this.initialTask != null) this.initialTask.cancel(true);
	}
	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(TAG, TAG+"--onDestroy");
	}
	/**
	 * 当该FragmentA从它所属的Activity中被删除时调用该方法
	 */
	@Override
	public void onDetach() {
		super.onDetach();
		Log.d(TAG, TAG+"--onDetach");
		mCallBack = null;
	}

    private synchronized void copyAllPuzzles() {
    	this.listAllPuzzles = DBHelperMorepuzzles.getInstance(this.mCallBack.getMyApp()).getAllPuzzles(null);
	    if(mCallBack.getMyApp().isInnerPics(getString(R.string.theme_code))) {
		    PicsPackageEntity entity = new PicsPackageEntity();
		    entity.setCode(getString(R.string.theme_code));
		    entity.setState(PicsPackageEntity.PackageStates.INNER);
		    entity.setIconState(PicsPackageEntity.IconStates.DOWNLOADED);
		    int innerPackageIndex = this.listAllPuzzles.indexOf(entity);
		    if(innerPackageIndex > -1) {
			    entity = this.listAllPuzzles.get(innerPackageIndex);
			    entity.setState(PicsPackageEntity.PackageStates.INNER);
			    entity.setIconState(PicsPackageEntity.IconStates.DOWNLOADED);
			    this.listAllPuzzles.remove(innerPackageIndex);
		    }
		    this.listAllPuzzles.add(0, entity);
	    }
    }
    
	private void showPlayOptionsMenu(int position) {
		int morepuzzlesIndex = position - RESERVED_GRID_ITEMS;
		CharSequence[] items = null;
		if (morepuzzlesIndex >= 0) {
			PicsPackageEntity entity = listAllPuzzles.get(listMyEntity.get(morepuzzlesIndex));
			if(entity.getZip() == null || entity.getZip().trim().equalsIgnoreCase("")) {
				items = new CharSequence[2];
				items[0] = this.getString(R.string.OPTIONS_NEWGAME_GRID_ITEM_PLAY);
				items[1] = this.getString(R.string.OPTIONS_NEWGAME_GRID_ITEM_DELETE);
			} else {
				items = new CharSequence[3];
				items[0] = this.getString(R.string.OPTIONS_NEWGAME_GRID_ITEM_PLAY);
				items[1] = this.getString(R.string.OPTIONS_NEWGAME_GRID_ITEM_DELETE);
				items[2] = this.getString(R.string.OPTIONS_NEWGAME_GRID_ITEM_REBUILD);
			}
		}
		
		AlertDialog.Builder dialogOptions = new AlertDialog.Builder(getActivity());
		dialogOptions.setTitle(this.getString(R.string.OPTIONS_NEWGAME_GRID_TITLE));
		dialogOptions.setItems(items, new PlayOptionsDialogClickListener(position, items));
		dialogOptions.setPositiveButton(
				R.string.DIALOG_BUTTON_CANCEL,
				new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) {
					}
				}
		);
		dialogOptions.show();
	}
	class PlayOptionsDialogClickListener implements DialogInterface.OnClickListener {
		private int position;
		private CharSequence[] items;
		public PlayOptionsDialogClickListener(int position, CharSequence[] items) {
			this.position = position;
			this.items = items;
		}
		@Override
		public void onClick(DialogInterface dialog, int which) {
			switch (which) {
			case 0:
				openPicsGridActivity(position);
				break;
			case 1:
				showDeleteDialog(position);
				break;
			case 2:
				rebuildAndFix(position);
				break;
			default: break;
			}
		}
	}
	private void openPicsGridActivity(int position) {
		int morepuzzlesIndex = position - RESERVED_GRID_ITEMS;
		PicsPackageEntity entity = listAllPuzzles.get(listMyEntity.get(morepuzzlesIndex));
		if(!PicsPackageEntity.PackageStates.INNER.equals(entity.getState()) && this.mCallBack.getMyApp().getPackageZipFile(entity.getCode()) == null) {
			DBHelperMorepuzzles.getInstance(this.mCallBack.getMyApp()).updatePackageState(entity.getCode(), PicsPackageEntity.PackageStates.SCHEDULED, null);
			this.mHandler.sendMessage(this.mHandler.obtainMessage(NewGameFrag.HANDLER_MSG_REFRESH_DATA));
		} else {
			mCallBack.openPackagePicsFrag(entity.getCode());
		}
	}
	private void showDeleteDialog(final int position) {
		AlertDialog.Builder dialogDelete = new AlertDialog.Builder(getActivity());
		dialogDelete.setTitle(R.string.DIALOG_NEWGAME_GRID_DELETE_TITLE);
		dialogDelete.setMessage(R.string.DIALOG_NEWGAME_GRID_DELETE_MSG);
		dialogDelete.setPositiveButton(
			R.string.DIALOG_BUTTON_YES,
			new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					deletePuzzlePackage(position);
				}
			}
		);
		dialogDelete.setNegativeButton(
			R.string.DIALOG_BUTTON_NO,
			new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
				}
			}
		);
		dialogDelete.show();
	}
	private void deletePuzzlePackage(int position) {
		int morepuzzlesIndex = position - RESERVED_GRID_ITEMS;
		PicsPackageEntity entity = listAllPuzzles.get(listMyEntity.get(morepuzzlesIndex));
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this.mCallBack.getMyApp());
		if(AppPrefUtil.getLastPlayPackageCode(this.mCallBack.getMyApp(), pref) != null && AppPrefUtil.getLastPlayPackageCode(this.mCallBack.getMyApp(), pref).equalsIgnoreCase(entity.getCode())) {
			Editor editor = pref.edit();
			AppPrefUtil.removeLastPlayPackageCode(this.mCallBack.getMyApp(), editor);
			editor.apply();
		}
		File file = this.mCallBack.getMyApp().getPackageFile(entity.getCode());
		if(file != null && file.exists()) file.delete();
		File iconFile = new File(this.mCallBack.getMyApp().getAppFilesPath(true) + entity.getCode() + AppConstants.EXTENSION_NAME_ICON);
		if(iconFile.exists()) iconFile.delete();
		mCallBack.getMyApp().getMemCache().remove(entity.getCode());
		
		entity.setState(PicsPackageEntity.PackageStates.NOTINSTALL);
		entity.setIconState(PicsPackageEntity.IconStates.OLDVERSION);
		entity.setDownloadPercent(0);
		DBHelperMorepuzzles.getInstance(this.mCallBack.getMyApp()).update(entity, null);
		DBHelperScore.getInstance(this.mCallBack.getMyApp()).delete(entity.getCode(), null);
		int indexOfAll = listAllPuzzles.indexOf(entity);
		int indexOfMyEntity = listMyEntity.indexOf(indexOfAll);
		listMyEntity.remove(indexOfMyEntity);
		this.mHandler.sendMessage(this.mHandler.obtainMessage(NewGameFrag.HANDLER_MSG_REFRESH_DATA));
	}
	
	private void rebuildAndFix(int position) {
		int morepuzzlesIndex = position - RESERVED_GRID_ITEMS;
		PicsPackageEntity entity = listAllPuzzles.get(listMyEntity.get(morepuzzlesIndex));
		File file = this.mCallBack.getMyApp().getPackageFile(entity.getCode());
		if(file != null && file.exists()) file.delete();
		File iconFile = new File(this.mCallBack.getMyApp().getAppFilesPath(true) + entity.getCode() + AppConstants.EXTENSION_NAME_ICON);
		if(iconFile.exists()) iconFile.delete();
		mCallBack.getMyApp().getMemCache().remove(entity.getCode());
		entity.setState(PicsPackageEntity.PackageStates.SCHEDULED);
		entity.setIconState(PicsPackageEntity.IconStates.OLDVERSION);
		entity.setDownloadPercent(0);
		DBHelperMorepuzzles.getInstance(this.mCallBack.getMyApp()).update(entity, null);
		this.getActivity().startService(new Intent(this.mCallBack.getMyApp(), DownloadZipMonitorService.class));
		this.mHandler.sendMessage(this.mHandler.obtainMessage(NewGameFrag.HANDLER_MSG_REFRESH_DATA));
	}
	private void showPausingOptionsMenu(int position) {
		CharSequence[] items = new CharSequence[2];
		items[0] = this.getString(R.string.OPTIONS_NEWGAME_GRID_ITEM_CONTINUE_DOWNLOADING);
		items[1] = this.getString(R.string.OPTIONS_NEWGAME_GRID_ITEM_DELETE);
		AlertDialog.Builder dialogOptions = new AlertDialog.Builder(getActivity());
		dialogOptions.setTitle(this.getString(R.string.OPTIONS_NEWGAME_GRID_TITLE));
		dialogOptions.setItems(items, new PausingOptionsDialogClickListener(position, items));
		dialogOptions.setPositiveButton(
				R.string.DIALOG_BUTTON_CANCEL,
				new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) {
					}
				}
		);
		dialogOptions.show();
	}
	class PausingOptionsDialogClickListener implements DialogInterface.OnClickListener {
		private int position;
		private CharSequence[] items;
		public PausingOptionsDialogClickListener(int position, CharSequence[] items) {
			this.position = position;
			this.items = items;
		}
		@Override
		public void onClick(DialogInterface dialog, int which) {
			switch (which) {
			case 0:
				int morepuzzlesIndex = position - RESERVED_GRID_ITEMS;
				PicsPackageEntity entity = listAllPuzzles.get(listMyEntity.get(morepuzzlesIndex));
				DBHelperMorepuzzles.getInstance(NewGameFrag.this.mCallBack.getMyApp()).updatePackageState(entity.getCode(), PicsPackageEntity.PackageStates.SCHEDULED, null);
				NewGameFrag.this.getActivity().startService(new Intent(NewGameFrag.this.mCallBack.getMyApp(), DownloadZipMonitorService.class));
				break;
			case 1:
				showDeleteDialog(position);
				break;
			default: break;
			}
		}
	}
	final static class ViewHolder {
		public RelativeLayout gridItemLayout;
		public RelativeLayout itemRootLayout;
		public ImageView packageImg;
		public ImageView scheduleImg;
		public TextView remindTxt;
		public ProgressBar iconProgressBar;
		public ProgressBar downloadProgressBar;
	}
	
	class MyGridAdapter extends BaseAdapter {
		private LayoutInflater mInflater;
		public MyGridAdapter(Context context) {
			this.mInflater = LayoutInflater.from(context);
		}
		@Override
		public int getCount() {
			if(NewGameFrag.this.listMyEntity == null) return RESERVED_GRID_ITEMS;
			else return NewGameFrag.this.listMyEntity.size()+RESERVED_GRID_ITEMS;
		}
		@Override
		public Object getItem(int position) {
			return null;
		}
		@Override
		public long getItemId(int position) {
			return 0;
		}
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder = null;
			if(convertView == null) {
				holder=new ViewHolder();
				convertView = mInflater.inflate(R.layout.new_game_griditem, null);
				holder.gridItemLayout = (RelativeLayout)convertView.findViewById(R.id.newGameGridItem);
				holder.gridItemLayout.setMinimumHeight(mCallBack.getMyApp().getDisplay().widthPixels/NewGameFrag.this.mCallBack.getMyApp().getResources().getInteger(R.integer.pics_grid_columns));
				holder.itemRootLayout = (RelativeLayout)convertView.findViewById(R.id.newGameItemRoot);
				holder.packageImg = (ImageView)convertView.findViewById(R.id.newgameItemPackageImg);
				holder.remindTxt = (TextView)convertView.findViewById(R.id.newgameItemRemindTxt);
				holder.scheduleImg = (ImageView)convertView.findViewById(R.id.newgameItemScheduleImg);
				holder.iconProgressBar = (ProgressBar)convertView.findViewById(R.id.newgameItemIconProgressBar);
				holder.downloadProgressBar = (ProgressBar)convertView.findViewById(R.id.newgameItemDownloadProgressBar);
				holder.downloadProgressBar.setMax(100);
				
				RelativeLayout.LayoutParams itemRootParams = new RelativeLayout.LayoutParams(intGridItemWidth, intGridItemWidth);
				itemRootParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
				holder.itemRootLayout.setLayoutParams(itemRootParams);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder)convertView.getTag();
			}
			holder.downloadProgressBar.setVisibility(View.GONE);
			holder.scheduleImg.setVisibility(View.GONE);
			holder.iconProgressBar.setVisibility(View.GONE);
			holder.remindTxt.setVisibility(View.GONE);
			holder.packageImg.setBackgroundColor(Color.argb(0, 0, 0, 0));
			if(position < RESERVED_GRID_ITEMS) {
				if(position == 0) {
					holder.packageImg.setImageBitmap(getScaledIconBitmap(R.drawable.icon_more));
					if(UpdatePuzzlesXmlService.intNewPackages > 0) {
						holder.remindTxt.setVisibility(View.VISIBLE);
						holder.remindTxt.setText(String.valueOf(UpdatePuzzlesXmlService.intNewPackages));
					}
				} else if(position == 1) {
//					Bitmap iconCustom = BitmapFactory.decodeResource(getResources(), R.drawable.icon_custom);
//					if(iconCustom.getWidth() != intGridItemWidth) {
//						Bitmap tempBmp = Bitmap.createScaledBitmap(iconCustom, intGridItemWidth, intGridItemWidth, true);
//						if(!iconCustom.isRecycled()) iconCustom.recycle();
//						iconCustom = tempBmp;
//						tempBmp = null;
//					}
					holder.packageImg.setImageBitmap(getScaledIconBitmap(R.drawable.icon_custom));
				}
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                    holder.packageImg.setImageAlpha(255);
                else //noinspection deprecation
                    holder.packageImg.setAlpha(255);
				holder.packageImg.setPadding(0, 0, 0, 0);
			} else if(position >= RESERVED_GRID_ITEMS){
				int morepuzzlesIndex = position - RESERVED_GRID_ITEMS;
				if(morepuzzlesIndex >= listMyEntity.size()) return convertView;
				if(listMyEntity.get(morepuzzlesIndex)>=listAllPuzzles.size()) return convertView;
				PicsPackageEntity entity = listAllPuzzles.get(listMyEntity.get(morepuzzlesIndex));
				if(entity.getIconState().equalsIgnoreCase(PicsPackageEntity.IconStates.DOWNLOADED)) {
					loadBitmap(position, holder.packageImg, entity);
				} else {
	        		holder.packageImg.setImageBitmap(getIconEmptyFrame());
					holder.iconProgressBar.setVisibility(View.VISIBLE);
					holder.iconProgressBar.bringToFront();
		        	if(AppTools.addDownloadingIcon(entity.getCode())) {
		        		NewGameFrag.this.getActivity().startService(new Intent(NewGameFrag.this.mCallBack.getMyApp(), DownloadIconService.class));
		        	}
				}
				if(entity.getState().equalsIgnoreCase(PicsPackageEntity.PackageStates.INSTALLED) 
						|| entity.getState().equalsIgnoreCase(PicsPackageEntity.PackageStates.OLDVERSION)
						|| entity.getState().equalsIgnoreCase(PicsPackageEntity.PackageStates.INNER)) {
					if(bundleDataDownloadZip != null && bundleDataDownloadZip.getPackageCode().equals(entity.getCode())) {
						if(!bundleDataDownloadZip.getPackageState().equals(entity.getState())) {
							NewGameFrag.this.mHandler.sendMessage(NewGameFrag.this.mHandler.obtainMessage(NewGameFrag.HANDLER_MSG_REFRESH_DATA));
						}
					}
					
				} else if(entity.getState().equalsIgnoreCase(PicsPackageEntity.PackageStates.DOWNLOADING)) {
					if(bundleDataDownloadZip != null && bundleDataDownloadZip.getPackageCode().equals(entity.getCode())) {
						if(!bundleDataDownloadZip.getPackageState().equals(entity.getState())) {
							NewGameFrag.this.mHandler.sendMessage(NewGameFrag.this.mHandler.obtainMessage(NewGameFrag.HANDLER_MSG_REFRESH_DATA));
						} else {
							holder.downloadProgressBar.setVisibility(View.VISIBLE);
							holder.downloadProgressBar.setProgress(bundleDataDownloadZip.getDownloadPercent());
							holder.downloadProgressBar.bringToFront();
						}
					}
				} else if(entity.getState().equalsIgnoreCase(PicsPackageEntity.PackageStates.SCHEDULED)) {
					if(bundleDataDownloadZip != null && bundleDataDownloadZip.getPackageCode().equals(entity.getCode())) {
						if(!bundleDataDownloadZip.getPackageState().equals(entity.getState())) {
							NewGameFrag.this.mHandler.sendMessage(NewGameFrag.this.mHandler.obtainMessage(NewGameFrag.HANDLER_MSG_REFRESH_DATA));
						}
					}
					holder.scheduleImg.setVisibility(View.VISIBLE);
					holder.scheduleImg.bringToFront();
				} else if(entity.getState().equalsIgnoreCase(PicsPackageEntity.PackageStates.PAUSING)) {
					if(bundleDataDownloadZip != null && bundleDataDownloadZip.getPackageCode().equals(entity.getCode())) {
						if(!bundleDataDownloadZip.getPackageState().equals(entity.getState())) {
							NewGameFrag.this.mHandler.sendMessage(NewGameFrag.this.mHandler.obtainMessage(NewGameFrag.HANDLER_MSG_REFRESH_DATA));
						}
					}
				}
			}
			return convertView;
		}
	}
	class MyItemClickListener implements OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
			if(progressBar.getVisibility() != View.VISIBLE) {
				switch(arg2) {
				case 0:
					mCallBack.openMorePuzzlesFrag();
					break;
				case 1:
					mCallBack.openCustomPicsFrag();
					break;
				default:
					int morepuzzlesIndex = arg2 - RESERVED_GRID_ITEMS;
					PicsPackageEntity entity = listAllPuzzles.get(listMyEntity.get(morepuzzlesIndex));
					if(entity != null) {
						if(entity.getState().equalsIgnoreCase(PicsPackageEntity.PackageStates.INSTALLED) 
								|| entity.getState().equalsIgnoreCase(PicsPackageEntity.PackageStates.OLDVERSION)
								|| entity.getState().equalsIgnoreCase(PicsPackageEntity.PackageStates.INNER)) {
							openPicsGridActivity(arg2);
						} else if(entity.getState().equalsIgnoreCase(PicsPackageEntity.PackageStates.PAUSING)){
							DBHelperMorepuzzles.getInstance(NewGameFrag.this.mCallBack.getMyApp()).updatePackageState(entity.getCode(), PicsPackageEntity.PackageStates.SCHEDULED, null);
							NewGameFrag.this.getActivity().startService(new Intent(NewGameFrag.this.mCallBack.getMyApp(), DownloadZipMonitorService.class));
						}
					}
					break;
				}
			}
		}
	}
    
	class MyItemLongClickListener implements OnItemLongClickListener {
		@Override
		public boolean onItemLongClick(AdapterView<?> arg0, View arg1,	int arg2, long arg3) {
			if(progressBar.getVisibility() != View.VISIBLE) {
				if(arg2 >= RESERVED_GRID_ITEMS) {
					int morepuzzlesIndex = arg2 - RESERVED_GRID_ITEMS;
					PicsPackageEntity entity = listAllPuzzles.get(listMyEntity.get(morepuzzlesIndex));
					if(entity.getState().equalsIgnoreCase(PicsPackageEntity.PackageStates.INSTALLED) 
							|| entity.getState().equalsIgnoreCase(PicsPackageEntity.PackageStates.OLDVERSION)) {
						showPlayOptionsMenu(arg2);
					} else if(entity.getState().equalsIgnoreCase(PicsPackageEntity.PackageStates.PAUSING)) {
						showPausingOptionsMenu(arg2);
					}
				}
			}
			return true;
		}
	}
	class InitialTask extends AsyncTask<Void, Integer, Void> {
		@Override
		protected void onPreExecute() {
			progressBar.bringToFront();
			progressBar.setVisibility(View.VISIBLE);
		}		
		@Override
		protected Void doInBackground(Void... params) {
			try {
				copyAllPuzzles();
				NewGameFrag.this.listMyEntity = new ArrayList<Integer>();
				for(int i=0;i<listAllPuzzles.size();i++) {
					PicsPackageEntity entity = listAllPuzzles.get(i);
					if(!entity.getState().equalsIgnoreCase(PicsPackageEntity.PackageStates.NOTINSTALL)) {
						listMyEntity.add(i);
					}
				}
			} catch(Exception e) {
				e.printStackTrace();
			}
			return null;
		}
		@Override
	    protected void onPostExecute(Void aVoid) {
			NewGameFrag.this.progressBar.setVisibility(View.GONE);
			NewGameFrag.this.gridAdapter.notifyDataSetChanged();
		}
	}
	class BitmapWorkerTask extends AsyncTask<PicsPackageEntity, Void, Bitmap> {
		private int position;
		private final WeakReference<ImageView> imageViewReference;
		private PicsPackageEntity entity;
		public BitmapWorkerTask(int position, ImageView imageView) {
			this.position = position;
			imageViewReference = new WeakReference<ImageView>(imageView);
		}
		@Override
		protected Bitmap doInBackground(PicsPackageEntity... params) {
			entity = params[0];
			Bitmap iconBitmap = null;
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = false;
			options.inPreferredConfig = Bitmap.Config.RGB_565;
			if(mCallBack.getMyApp().getMemCache().get(entity.getCode())!=null) {
        		return mCallBack.getMyApp().getMemCache().get(entity.getCode());
        	}
			if(mCallBack.getMyApp().isInnerPics(entity.getCode())) {
				InputStream input = null;
				try {
					input  = getResources().getAssets().open(entity.getCode()+AppConstants.EXTENSION_NAME_PICTURE);
					iconBitmap = BitmapFactory.decodeStream(input, null, options);
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					if(input != null) try {
						input.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			} else {
				File iconFile = new File(mCallBack.getMyApp().getAppFilesPath(true)+entity.getCode()+AppConstants.EXTENSION_NAME_ICON);
				iconBitmap = BitmapFactory.decodeFile(iconFile.toString(),options);
			}
	        if(iconBitmap.getWidth() != intGridItemWidth) {
	        	Bitmap tempBmp = Bitmap.createScaledBitmap(iconBitmap, intGridItemWidth, intGridItemWidth, true);
	        	if(!iconBitmap.isRecycled()) iconBitmap.recycle();
	        	iconBitmap = tempBmp;
	        	tempBmp = null;
	        }
	        if(iconBitmap != null && mCallBack.getMyApp().getMemCache().get(entity.getCode()) == null) {
        		mCallBack.getMyApp().getMemCache().put(entity.getCode(), mCallBack.getMyApp().getRoundedCornerBitmap(iconBitmap, AppConstants.DEFAULT_ROUND_RADIUS));
	        }
			return iconBitmap;
		}

		protected void onPostExecute(Bitmap bitmap) {
			if(NewGameFrag.this==null || !NewGameFrag.this.isVisible()) return;
			if (isCancelled()) {
				if(bitmap != null && !bitmap.isRecycled()) bitmap.recycle();
	            bitmap = null;
			}
			if (imageViewReference != null && bitmap != null) {
	            final ImageView imageView = imageViewReference.get();
	            final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
	            if (this == bitmapWorkerTask && imageView != null) {
					if(mCallBack.getMyApp().getMemCache().get(entity.getCode())!=null) {
	            		imageView.setImageBitmap(mCallBack.getMyApp().getMemCache().get(entity.getCode()));
	            		if(entity.getState().equalsIgnoreCase(PicsPackageEntity.PackageStates.INSTALLED)
	                    		|| entity.getState().equalsIgnoreCase(PicsPackageEntity.PackageStates.OLDVERSION)
					            || entity.getState().equalsIgnoreCase(PicsPackageEntity.PackageStates.INNER)) {
                            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                                imageView.setImageAlpha(255);
                            else //noinspection deprecation
                                imageView.setAlpha(255);
	        			} else {
                            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                                imageView.setImageAlpha(100);
                            else //noinspection deprecation
        				        imageView.setAlpha(100);
	        			}
	                    if(AppPrefUtil.getLastPlayPackageCode(NewGameFrag.this.mCallBack.getMyApp(), null) != null && AppPrefUtil.getLastPlayPackageCode(NewGameFrag.this.mCallBack.getMyApp(), null).equalsIgnoreCase(entity.getCode())) {
	                    	imageView.setBackgroundResource(R.drawable.bg_newgame_package);
	        				int  intPadding = (int)(5 * mCallBack.getMyApp().getDisplay().density);
	        				if(intPadding == 0) intPadding = 5;
	        				imageView.setPadding(intPadding, intPadding, intPadding, intPadding);
	        			} else {
	        				imageView.setPadding(0, 0, 0, 0);
	        			}
	            	}
	            }
	        }
	    }
	}
	static class AsyncDrawable extends BitmapDrawable {
	    private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;
	 
	    public AsyncDrawable(Resources res, Bitmap bitmap, BitmapWorkerTask bitmapWorkerTask) {
	        super(res, bitmap);
	        bitmapWorkerTaskReference = new WeakReference<BitmapWorkerTask>(bitmapWorkerTask);
	    }
	 
	    public BitmapWorkerTask getBitmapWorkerTask() {
	        return bitmapWorkerTaskReference.get();
	    }
	}
	public void loadBitmap(int position, ImageView imageView, PicsPackageEntity entity) {
		if(mCallBack.getMyApp().getMemCache().get(entity.getCode()) != null) {
			imageView.setImageBitmap(mCallBack.getMyApp().getMemCache().get(entity.getCode()));
            if(entity.getState().equalsIgnoreCase(PicsPackageEntity.PackageStates.INSTALLED)
            		|| entity.getState().equalsIgnoreCase(PicsPackageEntity.PackageStates.OLDVERSION)
		            || entity.getState().equalsIgnoreCase(PicsPackageEntity.PackageStates.INNER)) {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                    imageView.setImageAlpha(255);
                else //noinspection deprecation
					imageView.setAlpha(255);
			} else {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                    imageView.setImageAlpha(100);
                else //noinspection deprecation
					imageView.setAlpha(100);
			}
          if(AppPrefUtil.getLastPlayPackageCode(this.mCallBack.getMyApp(), null) != null && AppPrefUtil.getLastPlayPackageCode(this.mCallBack.getMyApp(), null).equalsIgnoreCase(entity.getCode())) {
            	imageView.setBackgroundResource(R.drawable.bg_newgame_package);
				int  intPadding = (int)(5 * mCallBack.getMyApp().getDisplay().density);
				if(intPadding == 0) intPadding = 5;
				imageView.setPadding(intPadding, intPadding, intPadding, intPadding);
			} else {
				imageView.setPadding(0, 0, 0, 0);
			}
		} else {
			BitmapFactory.Options options = new BitmapFactory.Options();
	        options.inJustDecodeBounds = true;
			if(mCallBack.getMyApp().isInnerPics(entity.getCode())) {
				InputStream input = null;
				try {
					input = getResources().getAssets().open(entity.getCode()+AppConstants.EXTENSION_NAME_PICTURE);
					BitmapFactory.decodeStream(input, null, options);
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					if(input != null) try {
						input.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			} else {
				File iconFile = new File(mCallBack.getMyApp().getAppFilesPath(true)+entity.getCode()+AppConstants.EXTENSION_NAME_ICON);
				BitmapFactory.decodeFile(iconFile.toString(),options);
			}
	        if(options.outWidth <= 0 || options.outHeight <= 0) {
	        	if(entity.getIcon()!= null && !entity.getIcon().trim().equalsIgnoreCase("")) {
					if(!entity.getIconState().equalsIgnoreCase(PicsPackageEntity.IconStates.OLDVERSION)) {
						DBHelperMorepuzzles.getInstance(this.mCallBack.getMyApp()).updateIconState(entity.getCode(), PicsPackageEntity.IconStates.OLDVERSION, null);
					}
				}
	        	if(AppTools.addDownloadingIcon(entity.getCode())) {
	        		this.mCallBack.getMyApp().startService(new Intent(this.mCallBack.getMyApp(), DownloadIconService.class));
				}
	        	this.mHandler.sendMessage(this.mHandler.obtainMessage(NewGameFrag.HANDLER_MSG_REFRESH_DATA));
	        } else {
	    	    if (cancelPotentialWork(position, imageView)) {
	    	        final BitmapWorkerTask task = new BitmapWorkerTask(position, imageView);
	    	        final AsyncDrawable asyncDrawable = new AsyncDrawable(getResources(), getIconEmptyFrame(), task);
	    	        imageView.setImageDrawable(asyncDrawable);
	    	        task.execute(entity);
	    	    }
	        }
		}
	}
	private static boolean cancelPotentialWork(int position, ImageView imageView) {
	    final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
	    if (bitmapWorkerTask != null) {
	        final int bitmapData = bitmapWorkerTask.position;
	        if (bitmapData != position) {
	            // Cancel previous task
	            bitmapWorkerTask.cancel(true);
	        } else {
	            // The same work is already in progress
	            return false;
	        }
	    }
	    return true;
	}
	private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
		if (imageView != null) {
			final Drawable drawable = imageView.getDrawable();
		    if (drawable instanceof AsyncDrawable) {
		    	final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
		        return asyncDrawable.getBitmapWorkerTask();
		    }
		}
		return null;
	}
	private Bitmap getIconEmptyFrame() {
		if(this.iconEmptyFrame == null || this.iconEmptyFrame.isRecycled()) {
			this.iconEmptyFrame = getScaledIconBitmap(R.drawable.icon_frame);
		}
		return this.iconEmptyFrame;
	}
	private Bitmap getScaledIconBitmap(int redId) {
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inSampleSize = 1;
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeResource(getResources(), redId, options);
		options.inSampleSize = options.outWidth / intGridItemWidth;
		if(options.inSampleSize < 1) options.inSampleSize = 1;
		options.inJustDecodeBounds = false;
		Bitmap iconBitmap = BitmapFactory.decodeResource(getResources(), redId, options);
		if(iconBitmap.getWidth() != this.intGridItemWidth) {
			Bitmap tempBmp = Bitmap.createScaledBitmap(iconBitmap, this.intGridItemWidth, this.intGridItemWidth, true);
			if(!iconBitmap.isRecycled()) iconBitmap.recycle();
			iconBitmap = tempBmp;
			tempBmp = null;
		}
		return iconBitmap;
	}
}

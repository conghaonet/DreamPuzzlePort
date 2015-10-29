package org.hao.puzzle54;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.flurry.android.FlurryAgent;

import org.hao.database.DBHelperMorepuzzles;
import org.hao.puzzle54.services.DownloadIconReceiver;
import org.hao.puzzle54.services.DownloadIconService;
import org.hao.puzzle54.services.DownloadZipMonitorService;
import org.hao.puzzle54.services.UpdatePuzzlesXmlReceiver;
import org.hao.puzzle54.services.UpdatePuzzlesXmlService;
import org.hh.puzzle.port54.hall.R;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.List;
	
public class MorePuzzlesFrag extends Fragment {
	private static final String TAG = MorePuzzlesFrag.class.getName();
	private static final int HANDLER_MSG_REFRESH_DATA = 1;
	private static final int HANDLER_MSG_NETWORK_ERR = 2;
	private InitialTask initialTask;
	private MyGridAdapter gridAdapter;
	private MyHandler mHandler;
	private ProgressBar progressBar;
	private List<PicsPackageEntity> listAllPuzzles;
	private Bitmap iconEmptyFrame;
	private DownloadIconReceiver iconReceiver;
	private UpdatePuzzlesXmlReceiver updatePuzzlesXmlReceiver;
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
			throw new IllegalStateException("MorePuzzlesFrag所在的Activity必须实现PackageGridActivityCallBack接口");
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
		WeakReference<MorePuzzlesFrag> mFragment;
		MyHandler(MorePuzzlesFrag fragment) {
			mFragment = new WeakReference<MorePuzzlesFrag>(fragment);
		}
		@Override
		public void handleMessage(Message msg) {
			MorePuzzlesFrag theFragment = mFragment.get();
		    if(theFragment == null) return;
		    switch (msg.what) {
			case MorePuzzlesFrag.HANDLER_MSG_REFRESH_DATA:
				theFragment.copyAllPuzzles();
				theFragment.gridAdapter.notifyDataSetChanged();
				break;
			case MorePuzzlesFrag.HANDLER_MSG_NETWORK_ERR:
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
		this.progressBar =  (ProgressBar)rootView.findViewById(R.id.grid_fragment_progress_bar);
		this.progressBar.setVisibility(View.GONE);
		GridView morePuzzlesGridView = (GridView)rootView.findViewById(R.id.grid_fragment_grid_view);
		this.gridAdapter = new MyGridAdapter(this.mCallBack.getMyApp());
		morePuzzlesGridView.setAdapter(this.gridAdapter);
		morePuzzlesGridView.setOnItemClickListener(new MyItemClickListener());
		UpdatePuzzlesXmlService.intNewPackages = 0;
		mHandler = new MyHandler(this);
		return rootView;
	}
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		Log.d(TAG, TAG+"--onActivityCreated");
		iconReceiver = new DownloadIconReceiver(){
			@Override
			public void onReceive(Context context, Intent intent) {
				MorePuzzlesFrag.this.mHandler.sendMessage(MorePuzzlesFrag.this.mHandler.obtainMessage(MorePuzzlesFrag.HANDLER_MSG_REFRESH_DATA));
			}
		};
		updatePuzzlesXmlReceiver = new UpdatePuzzlesXmlReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				MorePuzzlesFrag.this.mHandler.sendMessage(MorePuzzlesFrag.this.mHandler.obtainMessage(MorePuzzlesFrag.HANDLER_MSG_REFRESH_DATA));
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
		filterIcon.addAction(MorePuzzlesFrag.this.mCallBack.getMyApp().getPackageName()+DownloadIconReceiver.ACTION_DOWNLOAD_ICON);
		MorePuzzlesFrag.this.getActivity().registerReceiver(iconReceiver, filterIcon);
		IntentFilter filterUpdatePuzzlesXml = new IntentFilter();
		filterUpdatePuzzlesXml.addAction(MorePuzzlesFrag.this.mCallBack.getMyApp().getPackageName()+UpdatePuzzlesXmlReceiver.ACTION_UPDATE_XML);
		MorePuzzlesFrag.this.getActivity().registerReceiver(updatePuzzlesXmlReceiver, filterUpdatePuzzlesXml);
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
    	this.listAllPuzzles = DBHelperMorepuzzles.getInstance(this.mCallBack.getMyApp()).getAllOnlinePuzzles(null);
	    PicsPackageEntity entity = new PicsPackageEntity();
	    entity.setCode(getString(R.string.theme_code));
	    int innerPackageIndex = this.listAllPuzzles.indexOf(entity);
	    if(innerPackageIndex > -1) {
		    entity = this.listAllPuzzles.get(innerPackageIndex);
		    this.listAllPuzzles.remove(innerPackageIndex);
		    this.listAllPuzzles.add(0, entity);
	    }
    }
	final static class ViewHolder {
		public RelativeLayout gridItemLayout;
		public RelativeLayout itemRootLayout;
		public ImageView packageImg;
		public ImageView updateImg;
		public ProgressBar itemProgressBar;
		public TextView packageName;
	}
	class MyGridAdapter extends BaseAdapter {
		private LayoutInflater mInflater;
		public MyGridAdapter(Context context){
			this.mInflater = LayoutInflater.from(context);
		}
		@Override
		public int getCount() {
			if(listAllPuzzles == null) return 0;
			else return listAllPuzzles.size();
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
				convertView = mInflater.inflate(R.layout.more_puzzles_griditem, null);
				holder.gridItemLayout = (RelativeLayout)convertView.findViewById(R.id.morePuzzlesGridItem);
				holder.gridItemLayout.setMinimumHeight(mCallBack.getMyApp().getDisplay().widthPixels/MorePuzzlesFrag.this.mCallBack.getMyApp().getResources().getInteger(R.integer.pics_grid_columns));
				holder.itemRootLayout = (RelativeLayout)convertView.findViewById(R.id.morePuzzlesItemRoot);
				
				holder.packageImg = (ImageView)convertView.findViewById(R.id.morePuzzlesPackageImg);
				holder.updateImg = (ImageView)convertView.findViewById(R.id.morepuzzlesItemUpdateImg);
				holder.itemProgressBar = (ProgressBar)convertView.findViewById(R.id.morepuzzlesItemProgressBar);
				holder.packageName = (TextView)convertView.findViewById(R.id.packageNameTxt);

				RelativeLayout.LayoutParams itemRootParams = new RelativeLayout.LayoutParams(intGridItemWidth, intGridItemWidth);
				itemRootParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
				holder.itemRootLayout.setLayoutParams(itemRootParams);

				convertView.setTag(holder);
			} else {
				holder = (ViewHolder)convertView.getTag();
			}
			if(position >= listAllPuzzles.size()) return convertView;
			PicsPackageEntity entity = listAllPuzzles.get(position);
			holder.updateImg.setVisibility(View.GONE);
			holder.packageName.setText(entity.getName());
			if(entity.getIconState().equalsIgnoreCase(PicsPackageEntity.IconStates.DOWNLOADED)) {
				loadBitmap(position, holder.packageImg, entity);
				holder.itemProgressBar.setVisibility(View.GONE);
				if((mCallBack.getMyApp().isInnerPics(entity.getCode()) && entity.getZipVersion() > 1) || entity.getState().equalsIgnoreCase(PicsPackageEntity.PackageStates.OLDVERSION)) {
					holder.updateImg.setVisibility(View.VISIBLE);
					holder.updateImg.bringToFront();
				}
			} else {
				holder.packageImg.setImageBitmap(getIconEmptyFrame());
				holder.itemProgressBar.setVisibility(View.VISIBLE);
				holder.itemProgressBar.bringToFront();
	        	if(AppTools.addDownloadingIcon(entity.getCode())) {
	        		MorePuzzlesFrag.this.getActivity().startService(new Intent(MorePuzzlesFrag.this.mCallBack.getMyApp(), DownloadIconService.class));
	        	}
			}
			return convertView;
		}
	}
    class MyItemClickListener implements OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
			if(progressBar.getVisibility() != View.VISIBLE) {
				PicsPackageEntity entity = listAllPuzzles.get(arg2);
				if(entity.getIconState().equalsIgnoreCase(PicsPackageEntity.IconStates.DOWNLOADED)) {
					if(entity.getState().equalsIgnoreCase(PicsPackageEntity.PackageStates.NOTINSTALL) 
							|| entity.getState().equalsIgnoreCase(PicsPackageEntity.PackageStates.OLDVERSION)) {
						DBHelperMorepuzzles.getInstance(MorePuzzlesFrag.this.mCallBack.getMyApp()).updatePackageState(entity.getCode(), PicsPackageEntity.PackageStates.SCHEDULED, null);
//						Map<String, String> flurryParams = new HashMap<String, String>();
//						flurryParams.put("CODE", entity.getCode());
//						FlurryAgent.logEvent(FlurryEvents.MOREPUZZLES_PACKAGE, flurryParams);
//						Intent intent = new Intent();
//						intent.putExtra("entity_code", entity.getCode()); 
//						MorePuzzlesFrag.this.getActivity().setResult(1, intent);
//						MorePuzzlesFrag.this.getActivity().finish();
						MorePuzzlesFrag.this.getActivity().startService(new Intent(MorePuzzlesFrag.this.mCallBack.getMyApp(), DownloadZipMonitorService.class));
						mCallBack.openNewGameFrag();
					}
				}
			}
		}
	}
	class InitialTask extends AsyncTask<Void, Void, Void> {
		@Override
		protected void onPreExecute() {
			progressBar.bringToFront();
			progressBar.setVisibility(View.VISIBLE);
		}		
		@Override
		protected Void doInBackground(Void... params) {
			try {
				copyAllPuzzles();
			} catch(Exception e){
				e.printStackTrace();
			}
			return null;
		}
		@Override
	    protected void onPostExecute(Void aVoid) {
			MorePuzzlesFrag.this.progressBar.setVisibility(View.GONE);
			MorePuzzlesFrag.this.gridAdapter.notifyDataSetChanged();
//			MorePuzzlesFrag.this.mHandler.sendMessage(MorePuzzlesFrag.this.mHandler.obtainMessage(MorePuzzlesFrag.HANDLER_MSG_REFRESH_DATA));
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
			if(mCallBack.getMyApp().getMemCache().get(entity.getCode())!=null) {
        		return mCallBack.getMyApp().getMemCache().get(entity.getCode());
        	}
			File iconFile = new File(mCallBack.getMyApp().getAppFilesPath(true)+entity.getCode()+AppConstants.EXTENSION_NAME_ICON);
			BitmapFactory.Options options = new BitmapFactory.Options();
	        options.inJustDecodeBounds = false;
	        options.inPreferredConfig = Bitmap.Config.RGB_565;
	        iconBitmap = BitmapFactory.decodeFile(iconFile.toString(),options);
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
			if(MorePuzzlesFrag.this==null || !MorePuzzlesFrag.this.isVisible()) return;
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
	            		if(entity.getState().equalsIgnoreCase(PicsPackageEntity.PackageStates.NOTINSTALL)
	                    		|| entity.getState().equalsIgnoreCase(PicsPackageEntity.PackageStates.OLDVERSION)) {
                            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                imageView.setImageAlpha(255);
                            } else {
                                //noinspection deprecation
                                imageView.setAlpha(255);
                            }
	        			} else {
                            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                imageView.setImageAlpha(100);
                            } else {
                                //noinspection deprecation
                                imageView.setAlpha(100);
                            }
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
            if(entity.getState().equalsIgnoreCase(PicsPackageEntity.PackageStates.NOTINSTALL)
            		|| entity.getState().equalsIgnoreCase(PicsPackageEntity.PackageStates.OLDVERSION)) {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    imageView.setImageAlpha(255);
                } else {
                    //noinspection deprecation
                    imageView.setAlpha(255);
                }
			} else {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    imageView.setImageAlpha(100);
                } else {
                    //noinspection deprecation
                    imageView.setAlpha(100);
                }
			}
		} else {
			File iconFile = new File(mCallBack.getMyApp().getAppFilesPath(true)+entity.getCode()+AppConstants.EXTENSION_NAME_ICON);
			BitmapFactory.Options options = new BitmapFactory.Options();
	        options.inJustDecodeBounds = true;
	        BitmapFactory.decodeFile(iconFile.toString(),options);
	        if(options.outWidth <= 0 || options.outHeight <= 0) {
	        	if(entity.getIcon()!= null && !entity.getIcon().trim().equalsIgnoreCase("")) {
					if(!entity.getIconState().equalsIgnoreCase(PicsPackageEntity.IconStates.OLDVERSION)) {
						DBHelperMorepuzzles.getInstance(this.mCallBack.getMyApp()).updateIconState(entity.getCode(), PicsPackageEntity.IconStates.OLDVERSION, null);
					}
				}
	        	if(AppTools.addDownloadingIcon(entity.getCode())) {
	        		this.getActivity().startService(new Intent(this.mCallBack.getMyApp(), DownloadIconService.class));
	        	}
	        	this.mHandler.sendMessage(this.mHandler.obtainMessage(MorePuzzlesFrag.HANDLER_MSG_REFRESH_DATA));
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

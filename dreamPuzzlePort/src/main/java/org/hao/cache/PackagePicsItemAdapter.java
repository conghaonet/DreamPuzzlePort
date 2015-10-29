package org.hao.cache;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import org.hao.puzzle54.MyApp;
import org.hh.puzzle.port54.hall.R;

import java.util.ArrayList;
import java.util.zip.ZipFile;

public class PackagePicsItemAdapter extends BaseAdapter{
	private long allowedMaxPicIndex;
	private int thumbnailWidthHeight;
	private ArrayList<String> dataList;
	private ZipFile currentZipFile;
	private Bitmap emptyFrameBitmap;
    private MyApp myApp;
	private boolean isInnerPics;

	public PackagePicsItemAdapter(Context c, boolean isInnerPics, ArrayList<String> dataList, long allowedMaxPicIndex, String packageCode) {
		if(c instanceof MyApp) {
			myApp = (MyApp)c;
		} else {
			myApp = (MyApp)c.getApplicationContext();
		}
		this.allowedMaxPicIndex = allowedMaxPicIndex;
		this.dataList = dataList;
		this.isInnerPics = isInnerPics;
		if(!this.isInnerPics) currentZipFile = myApp.getPackageZipFile(packageCode);
		thumbnailWidthHeight = myApp.getDisplay().widthPixels/myApp.getResources().getInteger(R.integer.pics_grid_columns) - myApp.getSpacingOfPictureGridItem();
		emptyFrameBitmap = myApp.getBitmapEmptyFrame(0, thumbnailWidthHeight);
	}

	@Override
	public int getCount() {
		return dataList.size();
	}

	@Override
	public Object getItem(int position) {
		return dataList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	/**
	 * 存放列表项控件句柄
	 */
	private class ViewHolder {
		public ImageView imageView;
//		public ToggleButton toggleButton;
		public ImageView lockImg;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder viewHolder;
		if (convertView == null) {
			viewHolder = new ViewHolder();
			convertView = LayoutInflater.from(myApp).inflate(
					R.layout.pics_grid_item, parent, false);
			viewHolder.imageView = (ImageView) convertView.findViewById(R.id.image_view);
			viewHolder.lockImg = (ImageView) convertView.findViewById(R.id.lockImg);
			convertView.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}
		String path;
		if (dataList != null && dataList.size() > position)
			path = dataList.get(position);
		else
			path = "empty_frame";
		if (path.contains("empty_frame")) {
			viewHolder.imageView.setImageBitmap(emptyFrameBitmap);
		} else {
			ImageManager2.from(myApp, emptyFrameBitmap).displayImage(viewHolder.imageView,
					path, -1, thumbnailWidthHeight, thumbnailWidthHeight, currentZipFile, this.isInnerPics);
		}
		viewHolder.lockImg.setTag(position);
		if (position <= this.allowedMaxPicIndex) {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                viewHolder.imageView.setImageAlpha(255);
            else //noinspection deprecation
			    viewHolder.imageView.setAlpha(255);
			viewHolder.lockImg.setVisibility(View.GONE);
			viewHolder.imageView.setOnClickListener(new MyItemOnClickListener(position));
			viewHolder.imageView.setOnLongClickListener(new MyItemOnLongClickListener(position));
		} else {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
        		viewHolder.imageView.setImageAlpha(100);
            else //noinspection deprecation
                viewHolder.imageView.setAlpha(100);
			viewHolder.lockImg.setVisibility(View.VISIBLE);
			viewHolder.imageView.setOnClickListener(null);
			viewHolder.imageView.setOnLongClickListener(null);
		}
		return convertView;
	}

	class MyItemOnClickListener implements OnClickListener {
		int position;
		public MyItemOnClickListener(int position) {
			this.position = position;
		}
		@Override
		public void onClick(View v) {
			if (dataList != null && position < dataList.size()) {
				if(mOnItemClickListener != null) mOnItemClickListener.onItemClick(position);
			}
		}
		
	}
	class MyItemOnLongClickListener implements OnLongClickListener {
		int position;
		public MyItemOnLongClickListener(int position) {
			this.position = position;
		}
		@Override
		public boolean onLongClick(View v) {
			if (dataList != null && position < dataList.size()) {
				if(mOnItemLongClickListener != null) mOnItemLongClickListener.onItemLongClick(position);
			}
			return true;
		}
		
	}
	private OnItemClickListener mOnItemClickListener;
	public void setOnItemClickListener(OnItemClickListener l) {
		mOnItemClickListener = l;
	}
	public interface OnItemClickListener {
		public void onItemClick(int position);
	}
	
	private OnItemLongClickListener mOnItemLongClickListener;
	public void setOnItemLongClickListener(OnItemLongClickListener l) {
		mOnItemLongClickListener = l;
	}	
	public interface OnItemLongClickListener {
		public boolean onItemLongClick(int position);
	}
}

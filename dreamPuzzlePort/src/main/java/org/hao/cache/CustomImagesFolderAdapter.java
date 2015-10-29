package org.hao.cache;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.hao.puzzle54.AppTools;
import org.hao.puzzle54.MyApp;
import org.hh.puzzle.port54.hall.R;

import java.util.List;

public class CustomImagesFolderAdapter extends BaseAdapter {
//	private Context mContext;
	private List<String> dataList;
	private Bitmap emptyFrameBitmap;
	private int thumbnailWidthHeight;
	private MyApp myApp;
	public CustomImagesFolderAdapter(Context c, List<String> dataList) {
		if(c instanceof MyApp) {
			myApp = (MyApp)c;
		} else {
			myApp = (MyApp)c.getApplicationContext();
		}
		thumbnailWidthHeight = myApp.getDisplay().widthPixels/myApp.getResources().getInteger(R.integer.custom_folder_grid_columns) - myApp.getSpacingOfPictureGridItem();
		this.dataList = dataList;
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
	private class ViewHolder {
		public ImageView imageView;
		public TextView folderName;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder viewHolder;
		if (convertView == null) {
			viewHolder = new ViewHolder();
			convertView = LayoutInflater.from(myApp).inflate(
					R.layout.custom_images_folder_item, parent, false);
			viewHolder.imageView = (ImageView) convertView.findViewById(R.id.image_folder);
			viewHolder.folderName = (TextView) convertView.findViewById(R.id.folder_name);
			convertView.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}
		String path;
		if (dataList != null && dataList.size() > position) path = dataList.get(position);
		else path = "empty_frame";
		if (path.equalsIgnoreCase("empty_frame")) {
			viewHolder.imageView.setImageBitmap(emptyFrameBitmap);
		} else {
			ImageManager2.from(myApp, emptyFrameBitmap).displayImage(viewHolder.imageView,
					path, -1, thumbnailWidthHeight, thumbnailWidthHeight, null, false);
		}
		viewHolder.folderName.setText(AppTools.getShortParentPath(path));
		viewHolder.imageView.setOnClickListener(new MyItemOnClickListener(position));
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

	private OnItemClickListener mOnItemClickListener;
	public void setOnItemClickListener(OnItemClickListener l) {
		mOnItemClickListener = l;
	}
	public interface OnItemClickListener {
		public void onItemClick(int position);
	}

}

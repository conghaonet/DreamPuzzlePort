package org.hao.cache;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import org.hao.puzzle54.MyApp;
import org.hh.puzzle.port54.hall.R;

import java.util.List;

public class CustomImagesSelectAdapter extends BaseAdapter {
	private MyApp myApp;
	private List<String> dataList;
	private Bitmap emptyFrameBitmap;
	private int thumbnailWidthHeight;
	
	public CustomImagesSelectAdapter(Context c, List<String> dataList) {
		if(c instanceof MyApp) {
			myApp = (MyApp)c;
		} else {
			myApp = (MyApp)c.getApplicationContext();
		}
		thumbnailWidthHeight = myApp.getDisplay().widthPixels/myApp.getResources().getInteger(R.integer.pics_grid_columns) - myApp.getSpacingOfPictureGridItem();
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
//		public TextView txtFileName;
		public ImageView imgSelectOn;
		public ImageView imgSelectOff;
	}
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder viewHolder;
		if (convertView == null) {
			viewHolder = new ViewHolder();
			convertView = LayoutInflater.from(myApp).inflate(
					R.layout.custom_images_select_item, parent, false);
			viewHolder.imageView = (ImageView) convertView.findViewById(R.id.src_image);
//			viewHolder.txtFileName = (TextView) convertView.findViewById(R.id.file_name);
			viewHolder.imgSelectOn = (ImageView) convertView.findViewById(R.id.img_select_on);
			viewHolder.imgSelectOff = (ImageView) convertView.findViewById(R.id.img_select_off);
			convertView.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}
		String path = "empty_frame";
		if (dataList != null && dataList.size() > position) path = dataList.get(position);
//		else path = "empty_frame";
		if (path.equals("empty_frame")) {
			viewHolder.imageView.setImageBitmap(emptyFrameBitmap);
		} else {
			ImageManager2.from(myApp, emptyFrameBitmap).displayImage(viewHolder.imageView,
					path, -1, thumbnailWidthHeight, thumbnailWidthHeight, null, false);
		}
//		viewHolder.txtFileName.setText(path.substring(path.lastIndexOf("/")+1));
		viewHolder.imageView.setOnClickListener(new MyItemOnClickListener(position));
		if(myApp.getCustomSelectedSet().contains(path)) {
			viewHolder.imgSelectOn.setVisibility(View.VISIBLE);
			viewHolder.imgSelectOff.setVisibility(View.INVISIBLE);
		} else {
			viewHolder.imgSelectOn.setVisibility(View.INVISIBLE);
			viewHolder.imgSelectOff.setVisibility(View.VISIBLE);
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

	private OnItemClickListener mOnItemClickListener;
	public void setOnItemClickListener(OnItemClickListener l) {
		mOnItemClickListener = l;
	}
	public interface OnItemClickListener {
		public void onItemClick(int position);
	}
	
}

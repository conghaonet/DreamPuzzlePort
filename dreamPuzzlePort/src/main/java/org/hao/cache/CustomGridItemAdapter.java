package org.hao.cache;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import org.hao.puzzle54.MyApp;
import org.hao.puzzle54.custom.CustomPicEntity;
import org.hh.puzzle.port54.hall.R;

import java.util.List;

public class CustomGridItemAdapter extends BaseAdapter{
	private int thumbnailWidthHeight;
	private Bitmap emptyFrameBitmap;
	private boolean isEditMode;
	private List<CustomPicEntity> listSelected;
	private List<CustomPicEntity> listCustomEntity;
	private MyApp myApp;
	
	public CustomGridItemAdapter(Context c, List<CustomPicEntity> listCustomEntity) {
		if(c instanceof MyApp) {
			myApp = (MyApp)c;
		} else {
			myApp = (MyApp)c.getApplicationContext();
		}
		this.listCustomEntity = listCustomEntity;
		thumbnailWidthHeight = myApp.getDisplay().widthPixels/myApp.getResources().getInteger(R.integer.pics_grid_columns) - myApp.getSpacingOfPictureGridItem();
		emptyFrameBitmap = myApp.getBitmapEmptyFrame(0, thumbnailWidthHeight);
	}

	@Override
	public int getCount() {
		return this.listCustomEntity.size();
	}

	@Override
	public Object getItem(int position) {
		return this.listCustomEntity.get(position);
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
		public ImageView imgDeleteOn;
		public ImageView imgDeleteOff;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder viewHolder;
		if (convertView == null) {
			viewHolder = new ViewHolder();
			convertView = LayoutInflater.from(myApp).inflate(
					R.layout.custom_grid_item, parent, false);
			viewHolder.imageView = (ImageView) convertView.findViewById(R.id.custom_item_image);
			viewHolder.imgDeleteOn = (ImageView) convertView.findViewById(R.id.img_delete_on);
			viewHolder.imgDeleteOff = (ImageView) convertView.findViewById(R.id.img_delete_off);
			convertView.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}
		if(this.listCustomEntity == null || this.listCustomEntity.size() <= position) {
			return convertView;
		}
		CustomPicEntity entity = this.listCustomEntity.get(position);
		String path = null;
		if (this.listCustomEntity != null && this.listCustomEntity.size() > position)
			path = this.myApp.getCustomPicture(entity.getImageName()).getPath();
		else path = "empty_frame";
		if (path.contains("empty_frame")) {
			viewHolder.imageView.setImageBitmap(emptyFrameBitmap);
		} else {
			ImageManager2.from(myApp, emptyFrameBitmap).displayImage(viewHolder.imageView,
					path, -1, thumbnailWidthHeight, thumbnailWidthHeight, null, false);
		}
		if(this.isEditMode) {
			if(this.listSelected.contains(entity)) {
				viewHolder.imgDeleteOn.setVisibility(View.VISIBLE);
				viewHolder.imgDeleteOff.setVisibility(View.INVISIBLE);
			} else {
				viewHolder.imgDeleteOn.setVisibility(View.INVISIBLE);
				viewHolder.imgDeleteOff.setVisibility(View.VISIBLE);
			}
		} else {
			viewHolder.imgDeleteOn.setVisibility(View.INVISIBLE);
			viewHolder.imgDeleteOff.setVisibility(View.INVISIBLE);
		}
		viewHolder.imageView.setOnClickListener(new MyItemOnClickListener(position));
		viewHolder.imageView.setOnLongClickListener(new MyItemOnLongClickListener(position));
		return convertView;
	}

	class MyItemOnClickListener implements OnClickListener {
		int position;
		public MyItemOnClickListener(int position) {
			this.position = position;
		}
		@Override
		public void onClick(View v) {
			if (CustomGridItemAdapter.this.listCustomEntity != null && position < CustomGridItemAdapter.this.listCustomEntity.size()) {
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
			if (CustomGridItemAdapter.this.listCustomEntity != null && position < CustomGridItemAdapter.this.listCustomEntity.size()) {
				if(mOnItemLongClickListener != null) mOnItemLongClickListener.onItemLongClick(position);
			}
			return true;
		}
		
	}
	public void setEditMode(boolean isEditMode) {
		this.isEditMode = isEditMode;
	}

	public void setSetSelected(List<CustomPicEntity> listSelected) {
		this.listSelected = listSelected;
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

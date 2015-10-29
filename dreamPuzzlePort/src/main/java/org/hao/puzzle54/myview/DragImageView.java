package org.hao.puzzle54.myview;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;

/****
 * 这里你要明白几个方法执行的流程： 首先ImageView是继承自View的子类.
 * onLayout方法：是一个回调方法.该方法会在在View中的layout方法中执行，在执行layout方法前面会首先执行setFrame方法.
 * layout方法：
 * setFrame方法：判断我们的View是否发生变化，如果发生变化，那么将最新的l，t，r，b传递给View，然后刷新进行动态更新UI.
 * 并且返回ture.没有变化返回false.
 * 
 * invalidate方法：用于刷新当前控件,
 * 
 * 
 * @author Conghao
 * 
 */
public class DragImageView extends ImageView {
	private static final String TAG = DragImageView.class.getName();
	private static final int DEFAULT_RATIO = 2;
	private int parentLeft = -1;
    private int parentWidth = -1;
    private int parentHeight = -1;// 可见屏幕的宽高度
    private int MAX_W, MAX_H, MIN_W, MIN_H;// 极限值
    private int start_Top = -1;
    private int start_x, start_y, current_x, current_y;// 触摸位置
	private float beforeLenght;

    /**
	 * 模式	NONE：无; DRAG：拖拽; ZOOM:缩放
	 * @author Conghao
	 */
	private enum MODE { NONE, DRAG, ZOOM }
	private MODE mode = MODE.NONE;// 默认模式

	public DragImageView(Context context) {
		super(context);
	}
	
	public DragImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	/***
	 * 设置显示图片
	 */
	@Override
	public void setImageBitmap(Bitmap bm) {
		super.setImageBitmap(bm);
		/** 获取图片宽高 **/
        int bitmap_W = bm.getWidth();
        int bitmap_H = bm.getHeight();

		MAX_W = bitmap_W * DEFAULT_RATIO;
		MAX_H = bitmap_H * DEFAULT_RATIO;

		MIN_W = bitmap_W / DEFAULT_RATIO;
		MIN_H = bitmap_H / DEFAULT_RATIO;
	}
	
	/**
	 *
	 * @param maxWidth	该View的最大宽度
	 * @param maxHeight	该View的最大高度
	 */
	public void setMaxWH(int maxWidth, int maxHeight) {
		MAX_W = maxWidth;
		MAX_H = maxHeight;
	}
	
	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		if (start_Top == -1) {
			start_Top = top;
		}
	}

	/***
	 * touch 事件
	 */
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		/** 处理单点、多点触摸 **/
		switch (event.getAction() & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN:
			onTouchDown(event);
			break;
		// 多点触摸
		case MotionEvent.ACTION_POINTER_DOWN:
			onPointerDown(event);
			break;
		case MotionEvent.ACTION_MOVE:
			onTouchMove(event);
			break;
		case MotionEvent.ACTION_UP:
			LayoutParams layoutParams = new LayoutParams(this.getWidth(), this.getHeight(), Gravity.LEFT|Gravity.TOP);
			layoutParams.leftMargin = this.getLeft();
			layoutParams.topMargin = this.getTop();
			this.setLayoutParams(layoutParams);
			mode = MODE.NONE;
			break;
		// 多点松开
		case MotionEvent.ACTION_POINTER_UP:
			mode = MODE.NONE;
			break;
		default:
			break;
		}
		return true;
	}

	private void setParentRange() {
		if(this.parentWidth == -1) {
			View parentView = (View)this.getParent();
			this.parentWidth = parentView.getWidth();
			this.parentHeight = parentView.getHeight();
			this.parentLeft = parentView.getLeft();
            int parentTop = parentView.getTop();
		}
	}
	
	/** 按下 **/
	void onTouchDown(MotionEvent event) {
		mode = MODE.DRAG;
		setParentRange();
		current_x = (int) event.getRawX();
		current_y = (int) event.getRawY();
		start_x = (int) event.getX();
		start_y = current_y - this.getTop();
	}

	/** 两个手指 只能放大缩小 **/
	void onPointerDown(MotionEvent event) {
		setParentRange();
		if (event.getPointerCount() == 2) {
			mode = MODE.ZOOM;
			beforeLenght = getDistance(event);// 获取两点的距离
		}
	}

	/** 移动的处理 **/
	void onTouchMove(MotionEvent event) {
		int left = 0, top = 0, right = 0, bottom = 0;
		/** 处理拖动 **/
		if (mode == MODE.DRAG) {
			/** 在这里要进行判断处理，防止在drag时候越界 **/
			/** 获取相应的l，t,r ,b **/
			left = current_x - start_x - this.parentLeft;
			right = left + this.getWidth();
			top = current_y - start_y;
			bottom = top + this.getHeight();
			if(left<0) {
				left = 0;
				right = left + this.getWidth();
			}
			if(right > this.parentWidth) {
				right = this.parentWidth;
				left = right - this.getWidth();
			}
			if(top < 0) {
				top = 0;
				bottom = top+this.getHeight();
			}
			if(bottom > this.parentHeight) {
				bottom = this.parentHeight;
				top = bottom - this.getHeight();
			}
			this.setPosition(left, top, right, bottom);
			current_x = (int) event.getRawX();
			current_y = (int) event.getRawY();
		}
		/** 处理缩放 **/
		else if (mode == MODE.ZOOM) {
            float afterLenght = getDistance(event);
			float gapLenght = afterLenght - beforeLenght;// 变化的长度
			if (Math.abs(gapLenght) >= 5f) {
                float scale_temp = afterLenght / beforeLenght;
				this.setScale(scale_temp);
				beforeLenght = afterLenght;
			}
		}
	}

	/** 获取两点的距离 **/
	float getDistance(MotionEvent event) {
		float x = event.getX(0) - event.getX(1);
		float y = event.getY(0) - event.getY(1);
//		return FloatMath.sqrt(x * x + y * y);
		return (float)Math.sqrt(x * x + y * y);
	}

	/** 实现处理拖动 **/
	private void setPosition(int left, int top, int right, int bottom) {
		this.layout(left, top, right, bottom);
	}

	/** 处理缩放 **/
	void setScale(float scale) {
		int disX = (int) (this.getWidth() * Math.abs(1 - scale)) / 4;// 获取缩放水平距离
		int disY = (int) (this.getHeight() * Math.abs(1 - scale)) / 4;// 获取缩放垂直距离
		// 放大
        int current_Top;
        int current_Right;
        int current_Bottom;
        int current_Left;
        if (scale > 1 && this.getWidth() <= MAX_W && this.getHeight() <= MAX_H) {
			if(this.getWidth() == MAX_W || this.getHeight() == MAX_H) {
				current_Left = this.getLeft();
				current_Top = this.getTop();
				current_Right = current_Left + MAX_W;
				current_Bottom = current_Top + MAX_H;
			} else {
				current_Left = this.getLeft() - disX;
				current_Top = this.getTop() - disY;
				current_Right = this.getRight() + disX;
				current_Bottom = this.getBottom() + disY;
				if((current_Right - current_Left) > MAX_W) current_Right = current_Left + MAX_W;
				if((current_Bottom - current_Top) > MAX_H) current_Bottom = current_Top + MAX_H;
			}
			if(current_Left < 0 ) {
				int offsetX = 0 - current_Left;
				current_Left = 0;
				current_Right = current_Right + offsetX;
			}
			if(current_Top < 0) {
				int offsetY = 0- current_Top;
				current_Top = 0;
				current_Bottom = current_Bottom + offsetY;
			}
			if(current_Right > parentWidth) {
				int offsetX = current_Right - parentWidth;
				current_Right = parentWidth;
				current_Left = current_Left - offsetX;
			}
			if(current_Bottom > parentHeight) {
				int offsetY = current_Bottom - parentHeight;
				current_Bottom = parentHeight;
				current_Top = current_Top - offsetY;
			}
			this.setFrame(current_Left, current_Top, current_Right, current_Bottom);
		}
		// 缩小
		else if (scale < 1 && this.getWidth() >= MIN_W && this.getHeight() >= MIN_H) {
			current_Left = this.getLeft() + disX;
			current_Top = this.getTop() + disY;
			current_Right = this.getRight() - disX;
			current_Bottom = this.getBottom() - disY;
			this.setFrame(current_Left, current_Top, current_Right, current_Bottom);
		}
	}
}

package org.hao.puzzle54;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.flurry.android.FlurryAgent;

import org.hh.puzzle.port54.hall.R;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class SkinActivity extends ActionBarActivity {
	private final static String TAG = SkinActivity.class.getName();
	private MyApp myApp;
    private ImageView themeImg;
	private HorizontalScrollView scrollView;
	private LinearLayout linearLayout;
	private List<SkinEntity> listSkinEntity;
	private Bitmap thumbnailBorderBitmap;
	private int thumbnailWidth;
	private int selectedIndex;
	private boolean themeIsChanged;
	private List<ImageView> listThumbnailView;
	private Bitmap fullBitmap;
	private InitTask initTask;
	private Animation scrollViewAlphaAnimation;
	private AssetManager mAssetManager;

	/** Called when the activity is first created. */
	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.skin_activity);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
//			Display display = getWindowManager().getDefaultDisplay();
//			Point point = new Point();
//			display.getRealSize(point);
//			int realScreenHeight = point.y;
		}
		Toolbar toolbar = (Toolbar)findViewById(R.id.theme_mytoolbar);
		setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
		actionBar.setTitle(R.string.SKIN_ACTIVITY_SET_SKIN);
//		actionBar.setHomeAsUpIndicator(R.drawable.btn_actionbar_accept);
		actionBar.setDisplayHomeAsUpEnabled(true);
		mAssetManager = getAssets();
		this.myApp = (MyApp)getApplicationContext();
		themeImg = (ImageView)findViewById(R.id.theme_img);
		themeImg.setOnClickListener(new ThemeImageOnClickListener());
		scrollView = (HorizontalScrollView)findViewById(R.id.theme_scrollview);
		linearLayout = (LinearLayout)findViewById(R.id.theme_linearlayout);
		if(myApp.isTablet()) this.thumbnailWidth = (int)(myApp.getDisplay().widthPixels / 5.5);
		else this.thumbnailWidth = (int)(myApp.getDisplay().widthPixels / 3.5);
		this.listSkinEntity = myApp.getSkins();
		createBorderBitmap();
		setBackgroundImage();
		initTask = new InitTask();
		initTask.execute();
	}
	private void setBackgroundImage() {
		SkinEntity skinEntity = new SkinEntity();
		skinEntity.setCode(AppPrefUtil.getThemeCode(this, null));
		this.selectedIndex = this.listSkinEntity.indexOf(skinEntity);
		if(this.selectedIndex < 0) this.selectedIndex = 0;
		if(myApp.getMemCache().get(myApp.getMainBackgroundImageCacheKey()) != null && !myApp.getMemCache().get(myApp.getMainBackgroundImageCacheKey()).isRecycled()) {
			this.themeImg.setImageBitmap(myApp.getMemCache().get(myApp.getMainBackgroundImageCacheKey()));
		} else {
			InputStream input = null;
			try {
				BitmapFactory.Options opts = new BitmapFactory.Options();
				opts.inJustDecodeBounds = true;
				opts.inPreferredConfig = Bitmap.Config.RGB_565;
				input = this.mAssetManager.open(this.listSkinEntity.get(this.selectedIndex).getAssetsFullPath());
				BitmapFactory.decodeStream(input,null,opts);
				input.close();
				opts.inSampleSize = opts.outWidth / this.myApp.getDisplay().widthPixels;
				opts.inJustDecodeBounds = false;
				input = this.mAssetManager.open(this.listSkinEntity.get(this.selectedIndex).getAssetsFullPath());
				this.fullBitmap = BitmapFactory.decodeStream(input,null,opts);
				this.themeImg.setImageBitmap(this.fullBitmap);
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if(input != null)
					try {
						input.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
			}

		}
		this.listThumbnailView = new ArrayList<ImageView>();
		for(int i=0;i<this.listSkinEntity.size();i++) {
			ImageView subView = new ImageView(this);
			subView.setMaxHeight(this.thumbnailWidth);
			subView.setMaxWidth(this.thumbnailWidth);
			subView.setMinimumHeight(this.thumbnailWidth);
			subView.setMinimumWidth(this.thumbnailWidth);
			subView.setOnClickListener(new ThumbnailOnClickListener(i));
			if(i == SkinActivity.this.selectedIndex) {
				subView.setImageBitmap(SkinActivity.this.thumbnailBorderBitmap);
			}
			LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			this.linearLayout.addView(subView, layoutParams);
			this.listThumbnailView.add(subView);
		}
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.skin_menu, menu);
		return true;
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem menu) {
		switch(menu.getItemId()) {
		case android.R.id.home:
			onBackPressed();
			break;
		case R.id.skin_menu_accept:
			setSkin();
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(menu);
	}
	private void setSkin() {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		String savedThemeCode = AppPrefUtil.getThemeCode(SkinActivity.this, pref);
		SkinEntity entity = SkinActivity.this.listSkinEntity.get(SkinActivity.this.selectedIndex);
		if(savedThemeCode == null || !savedThemeCode.equals(entity.getCode())) {
			Editor editor = pref.edit();
			AppPrefUtil.setThemeCode(SkinActivity.this, editor, entity.getCode());
			editor.apply();
		}
		if(this.fullBitmap != null && !this.fullBitmap.isRecycled()) {
			myApp.getMemCache().put(myApp.getMainBackgroundImageCacheKey(), this.fullBitmap);
		}
		Intent intent = new Intent();
		intent.setClass(this, ActMain.class);
		startActivity(intent);
		overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
		finish();
	}
	class ThumbnailOnClickListener implements ImageView.OnClickListener {
		private int themeIndex;
		public ThumbnailOnClickListener(int themeIndex) {
			this.themeIndex = themeIndex;
		}
		@Override
		public void onClick(View v) {
			if(!SkinActivity.this.themeIsChanged) SkinActivity.this.themeIsChanged = true;
			if(this.themeIndex == SkinActivity.this.selectedIndex) return;
			ImageView lastView = SkinActivity.this.listThumbnailView.get(SkinActivity.this.selectedIndex);
			lastView.setImageBitmap(null);
			ImageView currentView = SkinActivity.this.listThumbnailView.get(this.themeIndex);
			currentView.setImageBitmap(SkinActivity.this.thumbnailBorderBitmap);
			
			SkinActivity.this.selectedIndex = this.themeIndex;
			InputStream input = null;
			BitmapFactory.Options opts = new BitmapFactory.Options();
			opts.inJustDecodeBounds = true;
			opts.inPreferredConfig = Bitmap.Config.RGB_565;
			try {
				input = SkinActivity.this.mAssetManager.open(SkinActivity.this.listSkinEntity.get(SkinActivity.this.selectedIndex).getAssetsFullPath());
				BitmapFactory.decodeStream(input,null,opts);
				input.close();
				opts.inSampleSize = opts.outWidth / SkinActivity.this.myApp.getDisplay().widthPixels;
				opts.inJustDecodeBounds = false;
				if(SkinActivity.this.fullBitmap != null && !SkinActivity.this.fullBitmap.isRecycled()) SkinActivity.this.fullBitmap.recycle();
				input = SkinActivity.this.mAssetManager.open(SkinActivity.this.listSkinEntity.get(SkinActivity.this.selectedIndex).getAssetsFullPath());
				SkinActivity.this.fullBitmap = BitmapFactory.decodeStream(input,null,opts);
				SkinActivity.this.themeImg.setImageBitmap(SkinActivity.this.fullBitmap);
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if(input != null)
					try {
						input.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
			}
		}
	}
	class ThemeImageOnClickListener implements ImageView.OnClickListener {
		@Override
		public void onClick(View v) {
			if(scrollViewAlphaAnimation != null && !scrollViewAlphaAnimation.hasEnded()) return;
			if(scrollView.getVisibility() == View.VISIBLE) {
				scrollViewAlphaAnimation = new AlphaAnimation(1f,0f);
				scrollViewAlphaAnimation.setAnimationListener(new MyAlphaAnimationListener(View.INVISIBLE));
			} else {
				scrollViewAlphaAnimation = new AlphaAnimation(0f, 1f);
				scrollViewAlphaAnimation.setAnimationListener(new MyAlphaAnimationListener(View.VISIBLE));
			}
			scrollViewAlphaAnimation.setDuration(500);
			scrollView.startAnimation(scrollViewAlphaAnimation);
		}
	}
	@Override
	public void onResume() {
		super.onResume();
	}
	@Override
	public void onBackPressed() {
		Intent intent = new Intent();
		intent.setClass(this, SettingsActivity.class);
		startActivity(intent);
		overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
		finish();
	}
	@Override
	public void onStart() {
       super.onStart();
       FlurryAgent.onStartSession(this);
	}
	@Override
	public void onPause() {
		super.onPause();
	}
	@Override
	public void onStop() {
      super.onStop();
      FlurryAgent.onEndSession(this);
	}
	@Override
	public void onDestroy() {
		if(this.initTask != null) initTask.cancel(true);
//		if(this.fullBitmap != null && !this.fullBitmap.isRecycled()) {
//			this.themeImg.setImageBitmap(null);
//			this.fullBitmap.recycle();
//		}
		super.onDestroy();
	}
	private void createBorderBitmap() {
		if(this.thumbnailBorderBitmap != null && !this.thumbnailBorderBitmap.isRecycled()) {
			return;
		}
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.BLACK);
//        float floatStrokeWidth = 5 * this.myApp.getDisplay().density;//线宽
//        if(myApp.isTablet()) {
//        	floatStrokeWidth = floatStrokeWidth * 2;
//        }
        float floatStrokeWidth = this.thumbnailWidth / 10;
        paint.setStrokeWidth(floatStrokeWidth);
        paint.setStyle(Style.STROKE); 
        Rect rect = new Rect(0, 0, this.thumbnailWidth, this.thumbnailWidth);
        paint.setAlpha(0xAA);
        this.thumbnailBorderBitmap = Bitmap.createBitmap(this.thumbnailWidth, this.thumbnailWidth, Bitmap.Config.ARGB_4444);
        new Canvas(thumbnailBorderBitmap).drawRect(rect, paint);
	}	

	class InitTask extends AsyncTask<Void, Integer, Void> {
		private List<Bitmap> listThumbnailBitmap = new ArrayList<Bitmap>();
		@Override
        protected void onPreExecute() {
			SkinActivity.this.themeIsChanged = false;
		}
		@Override
		protected Void doInBackground(Void... params) {
			InputStream input = null;
			for(int i=0;i< listSkinEntity.size();i++) {
				SkinEntity skinEntity = listSkinEntity.get(i);
				BitmapFactory.Options opts = new BitmapFactory.Options();
				opts.inJustDecodeBounds = true;
				opts.inPreferredConfig = Bitmap.Config.RGB_565;
				try {
					input = SkinActivity.this.mAssetManager.open(skinEntity.getAssetsFullPath());
					BitmapFactory.decodeStream(input,null,opts);
					input.close();
					opts.inSampleSize = opts.outWidth / SkinActivity.this.thumbnailWidth;
					opts.inJustDecodeBounds = false;
					input = SkinActivity.this.mAssetManager.open(skinEntity.getAssetsFullPath());
					Bitmap bitmap = BitmapFactory.decodeStream(input,null,opts);
					Bitmap thumbnailBitmap = ThumbnailUtils.extractThumbnail(bitmap,
							SkinActivity.this.thumbnailWidth, SkinActivity.this.thumbnailWidth, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
					this.listThumbnailBitmap.add(thumbnailBitmap);
					this.publishProgress(i);

				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					if(input != null)
						try {
							input.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
				}
			}
			return null;
		}
		@Override
		protected void onProgressUpdate(Integer... progress) {
			ImageView subView = SkinActivity.this.listThumbnailView.get(progress[0]);
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                subView.setBackground(new BitmapDrawable(getResources(), this.listThumbnailBitmap.get(progress[0])));
            else //noinspection deprecation
				subView.setBackgroundDrawable(new BitmapDrawable(getResources(), this.listThumbnailBitmap.get(progress[0])));
		}
		@Override
	    protected void onPostExecute(Void params) {
			if(!SkinActivity.this.themeIsChanged) {
				if((SkinActivity.this.selectedIndex+1) * SkinActivity.this.thumbnailWidth > myApp.getDisplay().widthPixels) {
					int middleLeftPosition = myApp.getDisplay().widthPixels / 2 - SkinActivity.this.thumbnailWidth / 2;
					scrollView.smoothScrollTo(SkinActivity.this.selectedIndex * SkinActivity.this.thumbnailWidth - middleLeftPosition, 0);
				}
			}
		}
	}
	class MyAlphaAnimationListener implements AnimationListener {
		private int visibility;
		public MyAlphaAnimationListener(int visibility) {
			this.visibility = visibility;
		}
		@Override
		public void onAnimationStart(Animation animation) {
		}
		@Override
		public void onAnimationEnd(Animation animation) {
			scrollView.clearAnimation();
			scrollView.setVisibility(this.visibility);
		}
		@Override
		public void onAnimationRepeat(Animation animation) {
		}
	}
}

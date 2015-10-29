package org.hao.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.hao.puzzle54.MyApp;
import org.hao.puzzle54.custom.CustomPicEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DBHelperCustom  extends SQLiteOpenHelper {
	private static final String TAG = DBHelperCustom.class.getName();
	private static final int DB_VERSION = 1;
	private static final String DATE_PATTERN="yyyy-MM-dd HH:mm:ss";
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(DATE_PATTERN, Locale.getDefault());
	private static final String TABLE_NAME_CUSTOM_IMAGE="custom_image";
	private static DBHelperCustom mInstance;
	private static final String CREATE_TABLE_CUSTOM_IMAGE="create table " + TABLE_NAME_CUSTOM_IMAGE
			+" (id integer PRIMARY KEY AUTOINCREMENT, image_name varchar(64) UNIQUE ON CONFLICT REPLACE" 
			+", import_datetime char("+DATE_PATTERN.length()+"), srcfile_path nvarchar(1024)"
			+")";

	private DBHelperCustom(Context context, String dbName) {
		super(context, dbName, null, DB_VERSION);
	}
	public synchronized static DBHelperCustom getInstance(Context context) {
		if (mInstance == null) {
			MyApp myApp = (MyApp)context.getApplicationContext();
			mInstance = new DBHelperCustom(context, myApp.getCustomDB());
		}
		return mInstance;
	}
	

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(CREATE_TABLE_CUSTOM_IMAGE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		
	}
	
	public synchronized long insertCustomImage(CustomPicEntity entity, SQLiteDatabase db) {
		if(db == null) db = getWritableDatabase();
		long rowId = -1;
		try {
			ContentValues values = new ContentValues();
			values.putNull("id");
			values.put("image_name", entity.getImageName());
			values.put("import_datetime", DATE_FORMAT.format(entity.getImportDateTime()));
			values.put("srcfile_path", entity.getSrcFilePath());
			rowId = db.insert(TABLE_NAME_CUSTOM_IMAGE, null, values);
		} catch(Exception e) {
			e.printStackTrace();
		}
		return rowId;
	}
	public synchronized int deleteCustomImage(String imageName, SQLiteDatabase db) {
		if(db == null) db = getWritableDatabase();
		int affectedRows = 0;
		try {
			String[] whereArgs = new String[1];
			whereArgs[0] = imageName;
			affectedRows = db.delete(TABLE_NAME_CUSTOM_IMAGE, "image_name = ?", whereArgs);
		} catch(Exception e) {
			e.printStackTrace();
		}
		return affectedRows;
	}
	public synchronized CustomPicEntity getById(long id, SQLiteDatabase db) {
		String sql = "select * from "+TABLE_NAME_CUSTOM_IMAGE+" where id = " + id;
		return query(sql, db);
	}
	public synchronized CustomPicEntity getByName(String imageName, SQLiteDatabase db) {
		String sql = "select * from "+TABLE_NAME_CUSTOM_IMAGE+" where image_name = '" + imageName+"'";
		return query(sql, db);
	}
	
	public synchronized CustomPicEntity query(String sql, SQLiteDatabase db) {
		if(db == null) db = getReadableDatabase();
		CustomPicEntity entity = new CustomPicEntity();
		Cursor cur = null;
		try{
			cur = db.rawQuery(sql,null);
			if(cur != null && cur.moveToFirst()) {
				entity.setId(cur.getLong(0));
				entity.setImageName(cur.getString(1));
				String strDateTime= cur.getString(2);
				try{
					entity.setImportDateTime(DBHelperCustom.DATE_FORMAT.parse(strDateTime));
				} catch(Exception e) {
                    e.printStackTrace();
				}
				entity.setSrcFilePath(cur.getString(3));
			} else {
				entity = null;
			}
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			if(cur != null) cur.close();
		}
		return entity;
	}
	
	public synchronized List<CustomPicEntity> getAll(SQLiteDatabase db) {
		if(db == null) db = getReadableDatabase();
		List<CustomPicEntity> list = new ArrayList<CustomPicEntity>();
		Cursor cur = null;
		String sql = "select * from "+TABLE_NAME_CUSTOM_IMAGE+" order by id";
		try {
			cur = db.rawQuery(sql, null);
			if(cur != null && cur.moveToFirst()) {
				do {
					CustomPicEntity entity = new CustomPicEntity();
					entity.setId(cur.getLong(0));
					entity.setImageName(cur.getString(1));
					String strDateTime= cur.getString(2);
					try{
						entity.setImportDateTime(DBHelperCustom.DATE_FORMAT.parse(strDateTime));
					} catch(Exception e) {
                        e.printStackTrace();
					}
					entity.setSrcFilePath(cur.getString(3));
			        list.add(entity);
				} while(cur.moveToNext());
			}
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			if(cur != null) cur.close();
		}
		
		return list;
	}
	public synchronized List<String> getAllSrcFilePath(SQLiteDatabase db) {
		if(db == null) db = getReadableDatabase();
		List<String> listSrcFilePath = new ArrayList<String>();
		Cursor cur = null;
		String sql = "select srcfile_path from "+TABLE_NAME_CUSTOM_IMAGE;
		try {
			cur = db.rawQuery(sql, null);
			if(cur != null) listSrcFilePath = new ArrayList<String>(cur.getCount());
			if(cur != null && cur.moveToFirst()) {
				do {
					String srcFilePath = cur.getString(0);
					if(srcFilePath != null && !srcFilePath.equalsIgnoreCase("")) {
						listSrcFilePath.add(srcFilePath);
					}
				} while(cur.moveToNext());
			}
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			if(cur != null) cur.close();
		}
		return listSrcFilePath;
	}

}

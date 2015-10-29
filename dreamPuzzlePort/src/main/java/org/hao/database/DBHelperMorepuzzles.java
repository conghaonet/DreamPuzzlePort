package org.hao.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.hao.puzzle54.MyApp;
import org.hao.puzzle54.PicsPackageEntity;

import java.util.ArrayList;
import java.util.List;

public class DBHelperMorepuzzles extends SQLiteOpenHelper {
	private static final String TAG = DBHelperMorepuzzles.class.getName();
	private static final int DB_VERSION = 1;
	private static final String TABLE_NAME_PACKAGE="package";
	private static DBHelperMorepuzzles mInstance;
//	private static final String CREATE_TABLE_PACKAGE="create table "+TABLE_NAME_PACKAGE
//			+" (id integer PRIMARY KEY AUTOINCREMENT, code varchar(128) UNIQUE, name varchar(128)"
//			+", icon varchar(1024), icon_version integer, icon_state varchar(128)"
//			+", zip varchar(1024), zip_version integer, zip_size integer"
//			+", state varchar(128), update_index integer, zip_lastmodified varchar(64) )";
	private static final String CREATE_TABLE_PACKAGE="create table "+TABLE_NAME_PACKAGE
			+" (id integer PRIMARY KEY AUTOINCREMENT, code varchar(128) UNIQUE, name varchar(128)"
			+", icon varchar(1024), icon_version integer, icon_state varchar(64)"
			+", zip varchar(1024), zip_version integer, zip_size integer"
			+", state varchar(128), update_index integer, zip_lastmodified varchar(64)"
			+", icon_size integer DEFAULT 0, icon_lastmodified varchar(64))";

	private DBHelperMorepuzzles(Context context, String dbName) {
		super(context, dbName, null, DB_VERSION);
	}
	public synchronized static DBHelperMorepuzzles getInstance(Context context) {
		if (mInstance == null) {
			MyApp myApp = (MyApp)context.getApplicationContext();
			mInstance = new DBHelperMorepuzzles(context, myApp.getMorepuzzlesDB());
		}
		return mInstance;
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(CREATE_TABLE_PACKAGE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		/*
		if(oldVersion < 2) db.execSQL("ALTER TABLE " + TABLE_NAME_PACKAGE + " ADD update_index integer DEFAULT 0");
		if(oldVersion < 3) db.execSQL("ALTER TABLE " + TABLE_NAME_PACKAGE + " ADD zip_lastmodified varchar(64)");
		if(oldVersion < 4) {
			db.execSQL("ALTER TABLE " + TABLE_NAME_PACKAGE + " ADD icon_size integer DEFAULT 0");
			db.execSQL("ALTER TABLE " + TABLE_NAME_PACKAGE + " ADD icon_lastmodified varchar(64)");
		}
		*/
	}
	public synchronized long insert(PicsPackageEntity entity, SQLiteDatabase db) {
		if(db == null) db = getWritableDatabase();
		long rowId = -1;
		try {
			ContentValues values = new ContentValues();
			values.putNull("id");
			values.put("code", entity.getCode());
			values.put("name", entity.getName());
			values.put("icon", entity.getIcon());
			values.put("icon_version", entity.getIconVersion());
			values.put("icon_state", entity.getIconState());
			values.put("zip", entity.getZip());
			values.put("zip_version", entity.getZipVersion());
			values.put("zip_size", entity.getZipSize());
			values.put("state", entity.getState());
			values.put("update_index", entity.getUpdateIndex());
			values.put("zip_lastmodified", entity.getZipLastModified());
			values.put("icon_size", entity.getIcongSize());
			values.put("icon_lastmodified", entity.getIconLastModified());
			rowId = db.insert(TABLE_NAME_PACKAGE, null, values);
		} catch(Exception e) {
			e.printStackTrace();
		}
		return rowId;
	}
	public synchronized int delete(String packageCode, int updateIndex, SQLiteDatabase db) {
		if(db == null) db = getWritableDatabase();
		int rows = 0;
		try {
			String[] whereArgs = {packageCode, String.valueOf(updateIndex)};
			rows = db.delete(TABLE_NAME_PACKAGE, "code = ? and update_index = ?", whereArgs);
		} catch(Exception e) {
			e.printStackTrace();
		}
		return rows;
	}

	public synchronized int update(PicsPackageEntity entity, SQLiteDatabase db) {
		if(db == null) db = getWritableDatabase();
		int rows = 0;
		try{
			ContentValues values = new ContentValues();
			values.put("id", entity.getId());
			values.put("code", entity.getCode());
			values.put("name", entity.getName());
			values.put("icon", entity.getIcon());
			values.put("icon_version", entity.getIconVersion());
			values.put("icon_state", entity.getIconState());
			values.put("zip", entity.getZip());
			values.put("zip_version", entity.getZipVersion());
			values.put("zip_size", entity.getZipSize());
			values.put("state", entity.getState());
			values.put("update_index", entity.getUpdateIndex()+1);
			values.put("zip_lastmodified", entity.getZipLastModified());
			values.put("icon_size", entity.getIcongSize());
			values.put("icon_lastmodified", entity.getIconLastModified());
			String[] whereArgs = {entity.getCode(), String.valueOf(entity.getUpdateIndex())};
			rows = db.update(TABLE_NAME_PACKAGE, values, "code = ? and update_index = ?", whereArgs);
		} catch(Exception e) {
			e.printStackTrace();
		}
		return rows;
	}
	private synchronized void update(String sql, SQLiteDatabase db) {
		if(db == null) db = getWritableDatabase();
		db.execSQL(sql);
	}
	public synchronized void updatePackageState(String packageCode, String packageState, SQLiteDatabase db) {
		if(packageCode == null || packageState == null) return;
		String sql = "update "+TABLE_NAME_PACKAGE + " set state='"+packageState+"' where code='"+packageCode+"'";
		update(sql, db);
	}
	public synchronized void updateIconState(String packageCode, String iconState, SQLiteDatabase db) {
		if(packageCode == null || iconState == null) return;
		String sql = "update "+TABLE_NAME_PACKAGE + " set icon_state='"+iconState+"' where code='"+packageCode+"'";
		update(sql, db);
	}
	public synchronized void updateZipUrlIsNull(String packageCode, SQLiteDatabase db) {
		if(packageCode == null) return;
		String sql = "update " + TABLE_NAME_PACKAGE + " set zip = NULL where code='" + packageCode + "'";
		update(sql, db);
	}

	private synchronized PicsPackageEntity getEntity(String sql, SQLiteDatabase db) {
		if(db == null) db = getReadableDatabase();
		PicsPackageEntity entity = null;
		Cursor cur = null;
		try{
			cur = db.rawQuery(sql,null);
			if(cur != null && cur.moveToFirst()) {
				entity = new PicsPackageEntity();
				entity.setId(cur.getLong(0));
		        entity.setCode(cur.getString(1));
		        entity.setName(cur.getString(2));
		        entity.setIcon(cur.getString(3));
		        entity.setIconVersion(cur.getInt(4));
		        entity.setIconState(cur.getString(5));
		        entity.setZip(cur.getString(6));
		        entity.setZipVersion(cur.getInt(7));
		        entity.setZipSize(cur.getLong(8));
		        entity.setState(cur.getString(9));
		        entity.setUpdateIndex(cur.getInt(10));
		        entity.setZipLastModified(cur.getString(11));
		        entity.setIcongSize(cur.getLong(12));
		        entity.setIconLastModified(cur.getString(13));
			}
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			if(cur != null) cur.close();
		}
		return entity;
	}

	public synchronized PicsPackageEntity getEntityById(long id, SQLiteDatabase db) {
		String sql = "select * from "+TABLE_NAME_PACKAGE+" where id = "+id;
		return getEntity(sql, db);
	}

	public synchronized PicsPackageEntity getEntityByCode(String code, SQLiteDatabase db) {
		String sql = "select * from "+TABLE_NAME_PACKAGE+" where code = '"+code+"'";
		return getEntity(sql, db);
	}

	public synchronized PicsPackageEntity getDownloadingEntity(SQLiteDatabase db) {
		String sql = "select * from "+TABLE_NAME_PACKAGE+" where state = '"
				+PicsPackageEntity.PackageStates.DOWNLOADING+"' order by id limit 1";
		return getEntity(sql, db);
	}

	public synchronized PicsPackageEntity getFirstScheduledEntity(SQLiteDatabase db) {
		String sql = "select * from "+TABLE_NAME_PACKAGE+" where state = '"
				+PicsPackageEntity.PackageStates.SCHEDULED+"' order by id limit 1";
		return getEntity(sql, db);
	}
	public synchronized List<PicsPackageEntity> getAllPuzzles(SQLiteDatabase db) {
		if(db == null) db = getReadableDatabase();
		List<PicsPackageEntity> list = new ArrayList<PicsPackageEntity>();
//		PuzzlePackageEntity innerEntity = getEntityByCode(innerPackageCode, db);
//		list.add(innerEntity);
		Cursor cur = null;
		try {
			cur = db.rawQuery("select * from "+TABLE_NAME_PACKAGE+" order by id", null);
			if(cur != null && cur.moveToFirst()){
				do{
					PicsPackageEntity entity = new PicsPackageEntity();
			        entity.setId(cur.getLong(0));
			        entity.setCode(cur.getString(1));
			        entity.setName(cur.getString(2));
			        entity.setIcon(cur.getString(3));
			        entity.setIconVersion(cur.getInt(4));
			        entity.setIconState(cur.getString(5));
			        entity.setZip(cur.getString(6));
			        entity.setZipVersion(cur.getInt(7));
			        entity.setZipSize(cur.getLong(8));
			        entity.setState(cur.getString(9));
			        entity.setUpdateIndex(cur.getInt(10));
			        entity.setZipLastModified(cur.getString(11));
			        entity.setIcongSize(cur.getLong(12));
			        entity.setIconLastModified(cur.getString(13));
			        list.add(entity);
				}while(cur.moveToNext());
			}
		} catch(Exception e) {
//			throw e;
		} finally {
			if(cur != null) cur.close();
		}
		return list;
	}
	public synchronized List<PicsPackageEntity> getAllOnlinePuzzles(SQLiteDatabase db) {
		if(db == null) db = getReadableDatabase();
		List<PicsPackageEntity> list = new ArrayList<PicsPackageEntity>();
//		PuzzlePackageEntity innerEntity = getEntityByCode(innerPackageCode, db);
//		list.add(innerEntity);
		Cursor cur = null;
		try {
			cur = db.rawQuery("select * from "+TABLE_NAME_PACKAGE+" where zip is not NULL order by id", null);
			if(cur != null && cur.moveToFirst()){
				do{
					PicsPackageEntity entity = new PicsPackageEntity();
					entity.setId(cur.getLong(0));
					entity.setCode(cur.getString(1));
					entity.setName(cur.getString(2));
					entity.setIcon(cur.getString(3));
					entity.setIconVersion(cur.getInt(4));
					entity.setIconState(cur.getString(5));
					entity.setZip(cur.getString(6));
					entity.setZipVersion(cur.getInt(7));
					entity.setZipSize(cur.getLong(8));
					entity.setState(cur.getString(9));
					entity.setUpdateIndex(cur.getInt(10));
					entity.setZipLastModified(cur.getString(11));
					entity.setIcongSize(cur.getLong(12));
					entity.setIconLastModified(cur.getString(13));
					list.add(entity);
				}while(cur.moveToNext());
			}
		} catch(Exception e) {
//			throw e;
		} finally {
			if(cur != null) cur.close();
		}
		return list;
	}

}

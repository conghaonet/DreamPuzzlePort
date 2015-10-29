package org.hao.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.hao.puzzle54.MyApp;
import org.hao.puzzle54.ScoreEntity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DBHelperScore  extends SQLiteOpenHelper {
	private static final String TAG = DBHelperScore.class.getName();
	public static final String FILE_NAME=".scores.db";
	private static final int DB_VERSION = 1;
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
	private static final String TABLE_NAME_SCORE="score";
	private static DBHelperScore mInstance;
	private static final String CREATE_TABLE_SCORE="create table "+TABLE_NAME_SCORE
			+" (id integer PRIMARY KEY AUTOINCREMENT, code varchar(128) , pic_index integer " 
			+", rows integer, cols integer, elapsed_time integer, best_time integer, steps integer, steps_of_best_time integer, eye_times integer, move_times integer"
			+", play_datetime text, is_finished boolean, pieces text"
			+")";
	
	private DBHelperScore(Context context, String dbName) {
		super(context, dbName, null, DB_VERSION);
	}
	
	public synchronized static DBHelperScore getInstance(Context context) {
		if (mInstance == null) {
			MyApp myApp = (MyApp)context.getApplicationContext();
			mInstance = new DBHelperScore(context, myApp.getScoreDB());
		}
		return mInstance;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(CREATE_TABLE_SCORE);
	}
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	}
	public synchronized int updateScore(ScoreEntity entity, SQLiteDatabase db) {
		if(db == null) db = getWritableDatabase();
		int rows = 0;
		try {
			ContentValues values = new ContentValues();
			values.put("id", entity.getId());
			values.put("code", entity.getCode());
			values.put("pic_index", entity.getPicIndex());
			values.put("rows", entity.getRows());
			values.put("cols", entity.getCols());
			values.put("elapsed_time", entity.getElapsedTime());
			values.put("best_time", entity.getBestTime());
			values.put("steps", entity.getSteps());
			values.put("steps_of_best_time", entity.getStepsOfBestTime());
			values.put("eye_times", entity.getEyeTimes());
			values.put("move_times", entity.getMoveTimes());
			values.put("play_datetime", DATE_FORMAT.format(entity.getPlayDatetime()));
			values.put("is_finished", entity.isFinished());
			values.put("pieces", entity.getPieces());
			String[] whereArgs = new String[1];
			whereArgs[0] = ""+entity.getId();
			rows = db.update(TABLE_NAME_SCORE, values, "id = ?", whereArgs);
		} catch(Exception e) {
			e.printStackTrace();
		}
		return rows;
	}
	public synchronized long insertScore(ScoreEntity entity, SQLiteDatabase db) {
		if(db == null) db = getWritableDatabase();
		long rowId = -1;
		try {
			// 创建ContentValues对象
			ContentValues values = new ContentValues();
			// 向该对象中插入键值对，其中键是列名，值是希望插入到这一列的值，值必须和数据库当中的数据类型一致
			values.putNull("id");
			values.put("code", entity.getCode());
			values.put("pic_index", entity.getPicIndex());
			values.put("rows", entity.getRows());
			values.put("cols", entity.getCols());
			values.put("elapsed_time", entity.getElapsedTime());
			values.put("best_time", entity.getBestTime());
			values.put("steps", entity.getSteps());
			values.put("steps_of_best_time", entity.getStepsOfBestTime());
			values.put("eye_times", entity.getEyeTimes());
			values.put("move_times", entity.getMoveTimes());
			values.put("play_datetime", DATE_FORMAT.format(entity.getPlayDatetime()));
			values.put("is_finished", entity.isFinished());
			values.put("pieces", entity.getPieces());
			rowId = db.insert(TABLE_NAME_SCORE, null, values);
		} catch(Exception e) {
			e.printStackTrace();
		}
		return rowId;
	}
	public synchronized void delete(String packageCode, SQLiteDatabase db) {
		if(db == null) db = getWritableDatabase();
		try {
			String[] whereArgs = new String[1];
			whereArgs[0] = packageCode;
			db.delete(TABLE_NAME_SCORE, "code = ?", whereArgs);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	public synchronized void delete(long picIndex, String packageCode, SQLiteDatabase db) {
		if(db == null) db = getWritableDatabase();
		try {
			String[] whereArgs = new String[2];
			whereArgs[0] = String.valueOf(picIndex);
			whereArgs[1] = packageCode;
			db.delete(TABLE_NAME_SCORE, "pic_index = ? and code = ?", whereArgs);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	public synchronized void deleteAll(SQLiteDatabase db) {
		if(db == null) db = getWritableDatabase();
		db.execSQL("DELETE FROM " + TABLE_NAME_SCORE);
		db.execSQL("update sqlite_sequence set seq=0 where name='"+TABLE_NAME_SCORE+"'");
	}
	private synchronized ScoreEntity getScore(String sql, SQLiteDatabase db) {
		if(db == null) db = getReadableDatabase();
		ScoreEntity entity = new ScoreEntity();
		Cursor cur = null;
		try{
			cur = db.rawQuery(sql,null);
			if(cur != null && cur.moveToFirst()) {
				entity.setId(cur.getLong(0));
		        entity.setCode(cur.getString(1));
		        entity.setPicIndex(cur.getLong(2));
		        entity.setRows(cur.getInt(3));
		        entity.setCols(cur.getInt(4));
		        entity.setElapsedTime(cur.getLong(5));
		        entity.setBestTime(cur.getLong(6));
		        entity.setSteps(cur.getInt(7));
		        entity.setStepsOfBestTime(cur.getInt(8));
		        entity.setEyeTimes(cur.getInt(9));
		        entity.setMoveTimes(cur.getInt(10));
		        try {
			        String strDateTime = cur.getString(11);
					entity.setPlayDatetime(DBHelperScore.DATE_FORMAT.parse(strDateTime));
				} catch (ParseException e) {
					e.printStackTrace();
				}
		        if(cur.getInt(12) == 1) entity.setFinished(true);
		        else entity.setFinished(false);
		        entity.setPieces(cur.getString(13));
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
	public synchronized ScoreEntity getScore(long id, SQLiteDatabase db) {
		String sql = "select * from "+TABLE_NAME_SCORE+" where id = " + id;
		return getScore(sql, db);
	}
	public synchronized ScoreEntity getScore(String code, long picIndex, int rows, int cols, SQLiteDatabase db) {
		String sql = "select * from "+TABLE_NAME_SCORE+" where code = '"+code
				+"' and pic_index="+picIndex+" and rows="+rows+" and cols="+cols+" limit 1";
		return getScore(sql, db);
	}
	public synchronized ScoreEntity getMaxPicIndexScore(String code, int rows, int cols, SQLiteDatabase db) {
		String sql = "select * from "+TABLE_NAME_SCORE+" where code = '"+code
				+"' and rows>="+rows+" and cols>="+cols+" and best_time>0 order by pic_index desc limit 1";
		return getScore(sql, db);
	}
	public synchronized List<ScoreEntity> getScores(String code, long picIndex, SQLiteDatabase db) {
		if(db == null) db = getReadableDatabase();
		List<ScoreEntity> list = new ArrayList<ScoreEntity>();
		Cursor cur = null;
		String sql = "select * from "+TABLE_NAME_SCORE+" where code = '"+ code 
				+"' and pic_index="+picIndex+" and best_time > 0 order by rows desc, cols desc";
		try {
			cur = db.rawQuery(sql, null);
			if(cur != null && cur.moveToFirst()) {
				do {
					ScoreEntity scoreEntity = new ScoreEntity();
					scoreEntity.setId(cur.getLong(0));
					scoreEntity.setCode(cur.getString(1));
					scoreEntity.setPicIndex(cur.getLong(2));
					scoreEntity.setRows(cur.getInt(3));
					scoreEntity.setCols(cur.getInt(4));
					scoreEntity.setElapsedTime(cur.getLong(5));
					scoreEntity.setBestTime(cur.getLong(6));
					scoreEntity.setSteps(cur.getInt(7));
					scoreEntity.setStepsOfBestTime(cur.getInt(8));
					scoreEntity.setEyeTimes(cur.getInt(9));
					scoreEntity.setMoveTimes(cur.getInt(10));
			        try {
				        String strDateTime = cur.getString(11);
				        scoreEntity.setPlayDatetime(DBHelperScore.DATE_FORMAT.parse(strDateTime));
					} catch (ParseException e) {
						e.printStackTrace();
					}
			        if(cur.getInt(12) == 1) scoreEntity.setFinished(true);
			        else scoreEntity.setFinished(false);
			        
			        list.add(scoreEntity);
					
				} while(cur.moveToNext());
			}
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			if(cur != null) cur.close();
		}
		return list;
	}
}

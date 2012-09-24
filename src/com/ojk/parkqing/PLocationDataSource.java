package com.ojk.parkqing;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class PLocationDataSource {
	
	// Database fields
	private SQLiteDatabase database;
	private MySQLiteHelper dbHelper;
	private String[] allColumns = { MySQLiteHelper.COLUMN_ID, 
			MySQLiteHelper.COLUMN_NAME,
			MySQLiteHelper.COLUMN_LAT,
			MySQLiteHelper.COLUMN_LON
	};
	public PLocationDataSource(Context context) {
		dbHelper = new MySQLiteHelper(context);
	}
	
	public void open() throws SQLException {
		database = dbHelper.getWritableDatabase();
	}
	
	public void close() {
		dbHelper.close();
	}
	
	public PLocation createLocation(String name, double lat, double lon) {
		ContentValues values = new ContentValues();
		values.put(MySQLiteHelper.COLUMN_NAME, name);
		values.put(MySQLiteHelper.COLUMN_LAT, PLocation.coordToInt(lat));
		values.put(MySQLiteHelper.COLUMN_LON, PLocation.coordToInt(lon));
		long insertId = database.insert(MySQLiteHelper.TABLE_LOCATION, null,
		        values);
		Cursor cursor = database.query(MySQLiteHelper.TABLE_LOCATION,
		        allColumns, MySQLiteHelper.COLUMN_ID + " = " + insertId, null,
		        null, null, null);
		    cursor.moveToFirst();
		    PLocation new_pLoc = cursorTo_pLocation(cursor);
		    cursor.close();
		    return new_pLoc;
	}
	
	public void delete_pLocation(PLocation loc) {
	    long id = loc.getId();
	    System.out.println("pLocation deleted with id: " + id);
	    database.delete(MySQLiteHelper.TABLE_LOCATION, MySQLiteHelper.COLUMN_ID
	        + " = " + id, null);
	  }
	
	public List<PLocation> getAllPLocations() {
	    List<PLocation> locs = new ArrayList<PLocation>();

	    Cursor cursor = database.query(MySQLiteHelper.TABLE_LOCATION,
	        allColumns, null, null, null, null, MySQLiteHelper.COLUMN_ID + " DESC");

	    cursor.moveToFirst();
	    while (!cursor.isAfterLast()) {
	      PLocation loc = cursorTo_pLocation(cursor);
	      locs.add(loc);
	      cursor.moveToNext();
	    }
	    // Make sure to close the cursor
	    cursor.close();
	    return locs;
	  }
	
	private PLocation cursorTo_pLocation(Cursor cursor) {
	    PLocation loc = new PLocation(cursor.getLong(0), 
	    		cursor.getString(1),
	    		cursor.getInt(2),
	    		cursor.getInt(3));
	    return loc;
	  }
}

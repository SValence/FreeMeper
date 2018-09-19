package com.valence.freemeper.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

import com.valence.freemeper.tool.AppContext;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseHelper";
    private static final String THUMB_DATABASE_NAME = "thumb_database.db";
    public static final String THUMB_TABLE = "thumb_table";
    private static final int DATABASE_VERSION = 1;
    public static final String FILE_ID = "file_id";
    public static final String FILE_PATH = "file_path";
    public static final String FILE_THUMB_PATH = "file_thumb_path";

    private static SQLiteDatabase database;

    public DatabaseHelper(Context context) {
        super(context, THUMB_DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createTable(db);
    }

    private void createTable(SQLiteDatabase db) {
        String sql = String.format("CREATE TABLE %s(%s INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "%s VARCHAR(512) not null," +
                        "%s VARCHAR(512) not null," +
                        "%s VARCHAR(512) not null)",
                THUMB_TABLE,
                BaseColumns._ID,
                FILE_ID,
                FILE_PATH,
                FILE_THUMB_PATH);
        Log.i(TAG, sql);
        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        dropTable(db);
        createTable(db);
    }

    private void dropTable(SQLiteDatabase db) {
        String sql = String.format("DROP TABLE %s", THUMB_TABLE);
        Log.i(TAG, sql);
        db.execSQL(sql);
    }

    public static void cleanTable(Context context) {
        initDatabase(context);
        String sql = "DELETE FROM " + THUMB_TABLE;
        Log.i(TAG, sql);
        AppContext.getInstance().db.execSQL(sql);
        unInitDatabase();
        Log.w(TAG, String.format("Table %s Has Cleaned", THUMB_TABLE));
    }

    public static Cursor query(SQLiteDatabase db, String sql) {
        Log.i(TAG, sql);
        return db.rawQuery(sql, null);
    }

    public static void insert(Context context, ContentValues c) {
        initDatabase(context);
        String id = (String) c.get(FILE_ID);
        String sql = "SELECT * FROM " + THUMB_TABLE + " WHERE " + FILE_ID + "='" + id + "'";
        Log.i(TAG, sql);
        Cursor cursor = AppContext.getInstance().db.rawQuery(sql, null);
        if (cursor.moveToFirst()) {
            if (updateRaw(AppContext.getInstance().db, c) <= 0)
                Log.e(TAG, "Update Database Error!! " + FILE_ID + " = " + id);
            else Log.i(TAG, "Update Database Success!! " + FILE_ID + " = " + id);
        }
        cursor.close();
        AppContext.getInstance().db.insert(THUMB_TABLE, "", c);
        unInitDatabase();
    }

    private static int updateRaw(SQLiteDatabase db, ContentValues c) {
        String[] args = new String[]{(String) c.get(FILE_ID)};
        return db.update(THUMB_TABLE, c, FILE_ID + "=?", args);
    }

    public static void update() {

    }

    public static void initDatabase(Context context) {
        if (TextUtils.isEmpty(THUMB_DATABASE_NAME)) {
            Log.e(TAG, "Database Name is Null!!!");
            return;
        }
        if (AppContext.getInstance().db != null) {
            Log.e(TAG, "Database Has Initialized!!!");
            return;
        }
        database = new DatabaseHelper(context).getWritableDatabase();
        AppContext.getInstance().db = database;
        Log.i(TAG, "DataBase Init-------" + THUMB_DATABASE_NAME);
    }

    public static void unInitDatabase() {
        if (database != null)
            database.close();
        AppContext.getInstance().db = null;
        database = null;
    }
}

package com.valence.freemeper.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.text.TextUtils;

import com.valence.freemeper.tool.AppContext;

import timber.log.Timber;

public class DatabaseHelper extends SQLiteOpenHelper {

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
        Timber.i(sql);
        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        dropTable(db);
        createTable(db);
    }

    private void dropTable(SQLiteDatabase db) {
        String sql = String.format("DROP TABLE %s", THUMB_TABLE);
        Timber.i(sql);
        db.execSQL(sql);
    }

    public static void cleanTable(Context context) {
        initDatabase(context);
        String sql = "DELETE FROM " + THUMB_TABLE;
        Timber.i(sql);
        database.execSQL(sql);
        unInitDatabase();
        Timber.w("Table %s Has Cleaned", THUMB_TABLE);
    }

    public static Cursor query(SQLiteDatabase db, String sql) {
        Timber.i(sql);
        return db.rawQuery(sql, null);
    }

    public static void insert(Context context, ContentValues c) {
        initDatabase(context);
        String id = (String) c.get(FILE_ID);
        String sql = "SELECT * FROM " + THUMB_TABLE + " WHERE " + FILE_ID + "='" + id + "'";
        Timber.i(sql);
        Cursor cursor = database.rawQuery(sql, null);
        if (cursor.moveToFirst()) {
            if (updateRaw(database, c) <= 0) {
                Timber.d("Update Database Error!! No Record:" + FILE_ID + " = " + id);
                database.insert(THUMB_TABLE, "", c);
            } else Timber.i("Update Database Success!! " + FILE_ID + " = " + id);
        } else {
            database.insert(THUMB_TABLE, "", c);
        }
        cursor.close();
        unInitDatabase();
    }

    public static void inserts(Context context, ContentValues[] cs) {
        initDatabase(context);
        database.beginTransaction();
        for (ContentValues c : cs) {
            database.insert(THUMB_TABLE, "", c);
        }
        database.setTransactionSuccessful();
        database.endTransaction();
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
            Timber.e("Database Name is Null!!!");
            return;
        }
        if (database != null) {
            Timber.e("Database Has Initialized!!!");
            return;
        }
        database = new DatabaseHelper(context).getWritableDatabase();
        AppContext.getInstance().db = database;
        Timber.i("DataBase Init-------%s", THUMB_DATABASE_NAME);
    }

    public static void unInitDatabase() {
        if (database != null)
            database.close();
        AppContext.getInstance().db = null;
        database = null;
    }
}

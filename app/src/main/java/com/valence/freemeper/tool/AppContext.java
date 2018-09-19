package com.valence.freemeper.tool;

import android.app.Application;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.File;

public class AppContext extends Application {
    public static final String TAG = "AppContext";
    public static final String thumbDirRoot;
    private static final String thumbDirPhoto;
    private static final String thumbDirVideo;
    public SQLiteDatabase db;
    private static AppContext instance;
    public static final String CACHE_FILE_PATH = "/DCIM/FreeMeper/.thumbnails/";

    static {
        thumbDirRoot = Environment.getExternalStorageDirectory().getAbsolutePath() + CACHE_FILE_PATH;
        thumbDirPhoto = thumbDirRoot + ".photo/";
        thumbDirVideo = thumbDirRoot + ".video/";
        File file = new File(thumbDirPhoto);
        if (!file.exists()) {
            if (file.mkdirs()) Log.i(TAG, "Create ThumbNail Photo Path Success");
            else Log.e(TAG, "Create ThumbNail Photo Path Failed");
        }
        file = new File(thumbDirVideo);
        if (!file.exists()) {
            if (file.mkdirs()) Log.i(TAG, "Create ThumbNail Video Path Success");
            else Log.e(TAG, "Create ThumbNail Video Path Failed");
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public static AppContext getInstance() {
        return instance;
    }

    public static String getThumbDirPhoto() {
        File f = new File(thumbDirPhoto);
        if (!f.exists()) f.mkdirs();
        return thumbDirPhoto;
    }

    public static String getThumbDirVideo() {
        File f = new File(thumbDirVideo);
        if (!f.exists()) f.mkdirs();
        return thumbDirVideo;
    }

    public static void showToast(String text) {
        Toast.makeText(instance, text, Toast.LENGTH_SHORT).show();
    }
}

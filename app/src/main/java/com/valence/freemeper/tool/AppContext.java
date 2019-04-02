package com.valence.freemeper.tool;

import android.app.Application;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.widget.Toast;

import com.tsinglink.android.library.xtimber.XLogTree;
import com.valence.freemeper.BuildConfig;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import timber.log.Timber;

public class AppContext extends Application {
    public static final String TAG = "AppContext";
    public static final String thumbDirRoot;
    private static final String thumbDirPhoto;
    private static final String thumbDirVideo;
    private static final String thumbDirCameraVideo;
    private static final String savedFilePicture;
    private static final String savedFileVideo;
    private static final String savedFileAudio;
    public SQLiteDatabase db;
    private static AppContext instance;
    public static final String CACHE_FILE_PATH = "/FreeMeper/.thumbnails/";
    public static final String SAVED_FILE_PATH = "/FreeMeper/data/";
    public static final String THUMB_FILE_END = "_thumb.jpg";

    static {
        thumbDirRoot = Environment.getExternalStorageDirectory().getAbsolutePath() + CACHE_FILE_PATH;
        thumbDirPhoto = thumbDirRoot + ".photo/";
        thumbDirVideo = thumbDirRoot + ".video/";
        thumbDirCameraVideo = thumbDirRoot + ".CameraVideo/";
        savedFilePicture = Environment.getExternalStorageDirectory().getAbsolutePath() + SAVED_FILE_PATH + "PICTURE/";
        savedFileVideo = Environment.getExternalStorageDirectory().getAbsolutePath() + SAVED_FILE_PATH + "VIDEO/";
        savedFileAudio = Environment.getExternalStorageDirectory().getAbsolutePath() + SAVED_FILE_PATH + "AUDIO/";
        File file = new File(thumbDirPhoto);
        if (!file.exists()) {
            if (file.mkdirs()) Timber.i("Create ThumbNail Photo Path Success");
            else Timber.e("Create ThumbNail Photo Path Failed");
        }
        file = new File(thumbDirVideo);
        if (!file.exists()) {
            if (file.mkdirs()) Timber.i("Create ThumbNail Video Path Success");
            else Timber.e("Create ThumbNail Video Path Failed");
        }
        file = new File(savedFilePicture);
        if (!file.exists()) {
            if (file.mkdirs()) Timber.i("Create Saved Picture Path Success");
            else Timber.e("Create Saved Picture Path Failed");
        }
        file = new File(savedFileVideo);
        if (!file.exists()) {
            if (file.mkdirs()) Timber.i("Create Saved Video Path Success");
            else Timber.e("Create Saved Video Path Failed");
        }
        file = new File(savedFileAudio);
        if (!file.exists()) {
            if (file.mkdirs()) Timber.i("Create Saved Audio Path Success");
            else Timber.e("Create Saved Audio Path Failed");
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        Timber.plant(new XLogTree(this, BuildConfig.DEBUG));
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }

    public static AppContext getInstance() {
        return instance;
    }

    public static String getThumbDirPhoto() {
        File f = new File(thumbDirPhoto);
        return (!f.exists() && !f.mkdirs()) ? null : thumbDirPhoto;
    }

    public static String getThumbDirVideo() {
        File f = new File(thumbDirVideo);
        return (!f.exists() && !f.mkdirs()) ? null : thumbDirVideo;
    }

    public static String getThumbDirCameraVideo() {
        File f = new File(thumbDirCameraVideo);
        return (!f.exists() && !f.mkdirs()) ? null : thumbDirCameraVideo;
    }

    public static String getSavedFilePicture() {
        File f = new File(savedFilePicture);
        return (!f.exists() && !f.mkdirs()) ? null : savedFilePicture;
    }

    public static String getSavedFileVideo() {
        File f = new File(savedFileVideo);
        return (!f.exists() && !f.mkdirs()) ? null : savedFileVideo;
    }

    public static String getSavedFileAudio() {
        File f = new File(savedFileAudio);
        return (!f.exists() && !f.mkdirs()) ? null : savedFileAudio;
    }

    public static void showToast(String text) {
        Toast.makeText(instance, text, Toast.LENGTH_SHORT).show();
    }

    public static String getNewPicturePath(int picture_num) {
        return getSavedFilePicture() + "IMG_" + new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss", Locale.CHINA).format(new Date()) + "_" + picture_num + ".jpg";
    }

    public static String getNewRecordFilePath() {
        return getSavedFileVideo() + "VID_" + new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss", Locale.CHINA).format(new Date()) + ".mp4";
    }
}

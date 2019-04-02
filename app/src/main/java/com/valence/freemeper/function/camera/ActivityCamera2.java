package com.valence.freemeper.function.camera;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaScannerConnection;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Size;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;

import com.valence.freemeper.R;
import com.valence.freemeper.base.BaseActivity2;
import com.valence.freemeper.cusview.CameraFlashTextureView;
import com.valence.freemeper.cusview.ImageCallbackView;
import com.valence.freemeper.database.DatabaseHelper;
import com.valence.freemeper.tool.AppContext;
import com.valence.freemeper.tool.CommonMethod;
import com.valence.freemeper.tool.GlideApp;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static android.provider.MediaStore.Video.Thumbnails.MINI_KIND;
import static com.valence.freemeper.database.DatabaseHelper.FILE_ID;
import static com.valence.freemeper.database.DatabaseHelper.FILE_PATH;
import static com.valence.freemeper.database.DatabaseHelper.FILE_THUMB_PATH;
import static com.valence.freemeper.database.DatabaseHelper.THUMB_TABLE;

public class ActivityCamera2 extends BaseActivity2 implements View.OnClickListener, SensorEventListener, CameraHelper.OnPicRecCompleteListener {

    private View backView;
    private View backViewFull;
    private TextureView textureView;
    private MediaScannerConnection mediaScanner;
    private CameraHelper helper;

    private ImageCallbackView takePicture;
    private ImageCallbackView viewFiles;
    private ImageCallbackView recordMode;
    private ImageCallbackView pictureMode;
    private ImageCallbackView startRecord;
    private ImageCallbackView stopRecord;
    private ImageCallbackView switchCamera;
    private ImageCallbackView videoSign;

    private Sensor mAccelerometer;
    private SensorManager sensorManager;

    private static final int CAMERA_DIRECTION_UP = 0;
    private static final int CAMERA_DIRECTION_LE = 90;
    private static final int CAMERA_DIRECTION_DO = 180;
    private static final int CAMERA_DIRECTION_RI = 270;
    private int lastDirection;

    private File[] cameraFiles;
    private HashMap<String, String> videoPathMap;
    private CompositeDisposable mComDis = new CompositeDisposable();
    private ArrayList<ImageCallbackView> imageViewList;
    private Disposable disposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_camera);
        findView();
        initData();
        setListener();

        getLifecycle().addObserver(new LifecycleObserver() {
            @OnLifecycleEvent(value = Lifecycle.Event.ON_RESUME)
            public void onResume() {
                reloadCameraFiles("", false);
                if (helper != null) helper.switchCamera(helper.getCurrentCameraId());
            }

            @OnLifecycleEvent(value = Lifecycle.Event.ON_PAUSE)
            public void onPause() {
//                reloadCameraFiles("", false);
                if (helper != null) {
                    if (CameraHelper.isRecording()) helper.closeCamera(true);
                    else helper.closeCamera(false);
                }
            }
        });
    }

    @Override
    public void findView() {
        CameraFlashTextureView flashTextureView = findViewById(R.id.free_camera);
        textureView = flashTextureView.getTextureView();
        backView = flashTextureView.getBackView();
        backViewFull = flashTextureView.getBackViewFull();
        takePicture = findViewById(R.id.free_camera_picture);
        recordMode = findViewById(R.id.free_camera_video);
        pictureMode = findViewById(R.id.free_camera_picture_mode);
        startRecord = findViewById(R.id.free_camera_start_record);
        stopRecord = findViewById(R.id.free_camera_stop_record);
        viewFiles = findViewById(R.id.free_camera_files);
        switchCamera = findViewById(R.id.free_camera_switch);
        videoSign = findViewById(R.id.free_camera_video_sign);

        imageViewList = new ArrayList<>();
        imageViewList.add(takePicture);
        imageViewList.add(recordMode);
        imageViewList.add(pictureMode);
        imageViewList.add(startRecord);
        imageViewList.add(stopRecord);
        imageViewList.add(viewFiles);
        imageViewList.add(switchCamera);
        imageViewList.add(videoSign);
    }

    @Override
    public void initData() {
        lastDirection = 0;
        textureView.setOpaque(true);
        videoPathMap = new HashMap<>();
        mediaScanner = new MediaScannerConnection(this, new MediaScannerConnection.MediaScannerConnectionClient() {
            @Override
            public void onMediaScannerConnected() {
                Timber.i("MediaScannerConnection Connected");
            }

            @Override
            public void onScanCompleted(String path, Uri uri) {
                Timber.i("MediaScannerConnection Scan Completed----path:%s", path);
            }
        });
        mediaScanner.connect();
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager == null) {
            Timber.e("Get Sensor Manager Error");
        } else {
            mAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
    }

    @Override
    public void setListener() {
        takePicture.setOnClickListener(this);
        startRecord.setOnClickListener(this);
        stopRecord.setOnClickListener(this);
        viewFiles.setOnClickListener(this);
        recordMode.setOnClickListener(this);
        pictureMode.setOnClickListener(this);

        for (ImageCallbackView imageView : imageViewList) {
            try {
                Timber.w("ImageCallbackView: " + imageView.getContentDescription() + " Visibility:" + imageView.getVisibility());
                imageView.setVisibilityChangeListener((oldVisibility, newVisibility) -> runOnUiThread(() -> {
                    Timber.w("ImageCallbackView: " + imageView.getContentDescription()
                            + " Visibility Change: Last-" + oldVisibility + " Now-" + newVisibility);
                    if (newVisibility == View.VISIBLE && oldVisibility == View.GONE) {
                        // 由不可见变为可见
                        if (lastDirection == 0) return;
                        imageView.clearAnimation();
                        RotateAnimation rotateAnimation = new RotateAnimation(0, lastDirection,
                                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                        rotateAnimation.setFillAfter(true);
                        rotateAnimation.setDuration(50);
                        rotateAnimation.setRepeatCount(0);
                        imageView.startAnimation(rotateAnimation);
                    } else if (newVisibility == View.GONE && oldVisibility == View.VISIBLE) {
                        // 由可见变为不可见
                        imageView.clearAnimation();
                    }
                }));
            } catch (Exception e) {
                Timber.e("ImageCallbackView.setVisibilityChangeListener(...) Error:%d", imageView.getId());
                e.printStackTrace();
            }
        }

        getLifecycle().addObserver(new LifecycleObserver() {
            @OnLifecycleEvent(value = Lifecycle.Event.ON_DESTROY)
            public void onDestroy() {
                if (disposable != null) {
                    disposable.dispose();
                    disposable = null;
                }
                mComDis.clear();
                mediaScanner.disconnect();
                Timber.i("MediaScannerConnection Disconnect");
            }
        });
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                helper = new CameraHelper.CameraBuilder()
                        .setTextureView(textureView)
                        .setBackView(backView)
                        .setBackViewFull(backViewFull)
                        .setContext(ActivityCamera2.this)
                        .setMediaScanner(mediaScanner)
                        .setCompleteListener(ActivityCamera2.this)
                        .startCamera();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                helper = null;
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        });
        getLifecycle().addObserver(new LifecycleObserver() {
            @OnLifecycleEvent(value = Lifecycle.Event.ON_RESUME)
            public void onResume() {
                sensorManager.registerListener(ActivityCamera2.this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
                Timber.i("ACCELEROMETER Register");
            }

            @OnLifecycleEvent(value = Lifecycle.Event.ON_PAUSE)
            public void onPause() {
                sensorManager.unregisterListener(ActivityCamera2.this, mAccelerometer);
                Timber.i("ACCELEROMETER Unregister");
            }
        });
    }

    public void showResolutionList(View view) {
        view.setClickable(false);
        final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        Completable.fromAction(() -> {
            ArrayList<Size> cameraSTList = helper.getCameraSTList();
            if (cameraSTList == null || cameraSTList.isEmpty())
                throw new NullPointerException(getString(R.string.support_resolution_null));
            dialog.setTitle(R.string.camera_support_resolution);
            CharSequence[] list = new CharSequence[cameraSTList.size()];
            for (int i = 0; i < list.length; i++) {
                list[i] = cameraSTList.get(i).toString();
            }
            dialog.setItems(list, (dialog1, which) -> {
                Size size = cameraSTList.get(which);
                Size currentSize = helper.getPreviewSize();
                if (size.getHeight() == currentSize.getHeight() && size.getWidth() == currentSize.getWidth())
                    return;
                helper.changeCameraResolution(cameraSTList.get(which), helper.getCurrentCameraId());
            });
            dialog.setCancelable(true);
            dialog.show();
        }).subscribeOn(AndroidSchedulers.from(helper.getLooper()))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        dialog.setOnDismissListener(dialog1 -> ActivityCamera2.this.runOnUiThread(() -> view.setClickable(true)));
                    }

                    @Override
                    public void onComplete() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        AppContext.showToast(e.toString());
                    }
                });
    }

    public void showPictureList(View view) {

    }

    public void onSwitchCamera(View view) {
        int count = helper.getCameraCount();
        switch (count) {
            case -1:
                AppContext.showToast(getString(R.string.getCameraCountError));
                return;
            case 0:
                Timber.e("Device Has No Camera");
                return;
            case 1:
                AppContext.showToast(getString(R.string.device_one_camera));
                return;
            case 2:
                view.setClickable(false);
                AlertDialog.Builder dialog = new AlertDialog.Builder(this);
                Completable.fromAction(() -> {
                    dialog.setTitle(R.string.choose_camera);
                    CharSequence[] list = new CharSequence[2];
                    list[0] = getString(R.string.back_camera);
                    list[1] = getString(R.string.front_camera);
                    dialog.setItems(list, (dialog1, which) -> {
                        if (which != helper.getCurrentCameraId())
                            helper.switchCamera(which);
                    });
                    dialog.setCancelable(true);
                    dialog.show();
                }).subscribeOn(AndroidSchedulers.from(helper.getLooper()))
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new CompletableObserver() {
                            @Override
                            public void onSubscribe(Disposable d) {
                                dialog.setOnDismissListener(dialog1 -> ActivityCamera2.this.runOnUiThread(() -> view.setClickable(true)));
                            }

                            @Override
                            public void onComplete() {

                            }

                            @Override
                            public void onError(Throwable e) {
                                AppContext.showToast(e.toString());
                            }
                        });
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.free_camera_video:
                helper.changePreviewMode(true);
                takePicture.setVisibility(View.GONE);
                recordMode.setVisibility(View.GONE);
                stopRecord.setVisibility(View.GONE);
                pictureMode.setVisibility(View.VISIBLE);
                startRecord.setVisibility(View.VISIBLE);
                break;
            case R.id.free_camera_start_record:
                if (!helper.isCameraReady()) {
                    AppContext.showToast("Camera is not Ready");
                    return;
                }
                takePicture.setVisibility(View.GONE);
                recordMode.setVisibility(View.GONE);
                startRecord.setVisibility(View.GONE);
                pictureMode.setVisibility(View.INVISIBLE);
                stopRecord.setVisibility(View.VISIBLE);
                int rotation = getWindowManager().getDefaultDisplay().getRotation();
                helper.startRecord(rotation);
                break;
            case R.id.free_camera_stop_record:
                takePicture.setVisibility(View.GONE);
                recordMode.setVisibility(View.GONE);
                stopRecord.setVisibility(View.GONE);
                pictureMode.setVisibility(View.VISIBLE);
                startRecord.setVisibility(View.VISIBLE);
                helper.stopRecord();
                break;
            case R.id.free_camera_picture_mode:
                helper.changePreviewMode(false);
                pictureMode.setVisibility(View.GONE);
                startRecord.setVisibility(View.GONE);
                stopRecord.setVisibility(View.GONE);
                takePicture.setVisibility(View.VISIBLE);
                recordMode.setVisibility(View.VISIBLE);
                break;
            case R.id.free_camera_picture:
                if (!helper.isCameraReady()) {
                    AppContext.showToast("Camera is not Ready");
                    return;
                }
                int rota = getWindowManager().getDefaultDisplay().getRotation();
                helper.takePicture(rota);
                backView.setVisibility(View.VISIBLE);
                new Handler(getMainLooper()).postDelayed(() -> backView.setVisibility(View.GONE), 100);
                break;
            case R.id.free_camera_files:
                Intent intent = new Intent(ActivityCamera2.this, CameraFileActivity.class);
                CameraFileBucket bucket = new CameraFileBucket();
                bucket.setFiles(cameraFiles);
                bucket.setVideoPathMap(videoPathMap);
                intent.putExtra("camera_file_bucket", bucket);
                startActivity(intent);
                break;
        }
    }

    private void rotateButtons(int direction) {
        if (lastDirection == direction) return;

        int degreeTo = direction - lastDirection % 360;
        if (degreeTo == 270) degreeTo = lastDirection - 90;
        else if (degreeTo == -270) degreeTo = lastDirection + 90;
        else degreeTo += lastDirection;
        long duration = Math.abs(degreeTo - lastDirection) / 90 == 2 ? 600 : 400;
        Animation rotateAnimation;
        for (ImageView view : imageViewList) {
            if (view.getVisibility() != View.VISIBLE) continue;
            rotateAnimation = new RotateAnimation(lastDirection, degreeTo, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            rotateAnimation.setFillAfter(true);
            rotateAnimation.setDuration(duration);
            rotateAnimation.setRepeatCount(0);
            view.startAnimation(rotateAnimation);
        }
        lastDirection = direction;
        helper.setDeviceRotation(degreeTo % 360);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        int sensorType = event.sensor.getType();
        if (sensorType == Sensor.TYPE_ACCELEROMETER) {
            float xValue = event.values[0];// Acceleration minus Gx on the x-axis
            float yValue = event.values[1];//Acceleration minus Gy on the y-axis
            float zValue = event.values[2];//Acceleration minus Gz on the z-axis
            float mGravity = 8.0f;
            int direction;
            if (xValue > mGravity) {
                direction = CAMERA_DIRECTION_LE;
                // 设备左侧朝下
                // sensorAccelerometer.append("\n重力指向设备左边");
            } else if (xValue < -mGravity) {
                direction = CAMERA_DIRECTION_RI;
                // 设备右侧朝下
                // sensorAccelerometer.append("\n重力指向设备右边");
            } else if (yValue > mGravity) {
                direction = CAMERA_DIRECTION_UP;
                // 设备下侧朝下
                // sensorAccelerometer.append("\n重力指向设备下边");
            } else if (yValue < -mGravity) {
                direction = CAMERA_DIRECTION_DO;
                // 设备上侧朝下
                // sensorAccelerometer.append("\n重力指向设备上边");
            } else if (zValue > mGravity) {
//                Timber.i("Screen Upwards... Do Nothing");
                direction = lastDirection;
                // sensorAccelerometer.append("\n屏幕朝上");
            } else if (zValue < -mGravity) {
//                Timber.i("Screen Down... Do Nothing");
                direction = lastDirection;
                // sensorAccelerometer.append("\n屏幕朝下");
            } else {
//                Timber.w("No direction fit!!! xValue:%f; yValue:%f; zValue:%f", xValue, yValue, zValue);
                direction = lastDirection;
            }
            rotateButtons(direction);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onPicRecComplete(int picOrRec, String filePath) {
        if (TextUtils.isEmpty(filePath)) return;
        reloadCameraFiles(filePath, true);
    }

    private void reloadCameraFiles(String path, boolean animation) {
        Disposable subscribe = Single.fromCallable(() -> {
            String filePath = "";
            if (TextUtils.isEmpty(path)) {
                videoPathMap.clear();
                cameraFiles = null;
                String dirPicPath = AppContext.getSavedFilePicture();
                String dirVideoPath = AppContext.getSavedFileVideo();
                ArrayList<File> filesList = new ArrayList<>();
                if (!TextUtils.isEmpty(dirPicPath)) {
                    File dirPic = new File(dirPicPath);
                    if (dirPic.exists() && dirPic.isDirectory()) {
                        File[] pics = dirPic.listFiles(pathname -> pathname.getAbsolutePath().endsWith(".jpg"));
                        if (pics != null && pics.length > 0) filesList.addAll(Arrays.asList(pics));
                    }
                }
                if (!TextUtils.isEmpty(dirVideoPath)) {
                    File dirVideo = new File(dirVideoPath);
                    if (dirVideo.exists() && dirVideo.isDirectory()) {
                        File[] videos = dirVideo.listFiles(pathname -> pathname.getAbsolutePath().endsWith(".mp4"));
                        if (videos != null && videos.length > 0)
                            filesList.addAll(Arrays.asList(videos));
                    }
                }

                File[] files = filesList.toArray(new File[0]);
                Arrays.sort(files, new Comparator<File>() {
                    public int compare(File f1, File f2) {
                        long diff = f1.lastModified() - f2.lastModified();
                        // 如果 diff>0 时 返回 -1, diff>0 时 返回 1  排序按时间递减
                        // 如果 diff>0 时 返回 1, diff>0 时 返回 -1  排序按时间递增
                        if (diff > 0)
                            return -1;
                        else if (diff == 0)
                            return 0;
                        else
                            return 1;
                    }

                    public boolean equals(Object obj) {
                        return true;
                    }
                });
                if (files.length > 0) {
                    cameraFiles = new File[files.length];
                    ArrayList<String> pathList = new ArrayList<>();
                    HashMap<String, String> thumbMap = new HashMap<>();
                    DatabaseHelper.initDatabase(ActivityCamera2.this);
                    SQLiteDatabase db = AppContext.getInstance().db;
                    String sql = "SELECT * FROM " + THUMB_TABLE + " WHERE " + FILE_PATH + " LIKE '" + AppContext.getSavedFileVideo() + "%'";
                    Timber.e(sql);
                    Cursor cursor = DatabaseHelper.query(db, sql);
                    if (cursor == null || !cursor.moveToFirst()) {
                        Timber.e("QUERY DATABASE ERROR: %s", sql);
                    } else {
                        do {
                            String fp = cursor.getString(cursor.getColumnIndex(FILE_PATH));
                            if (!new File(fp).exists()) continue;
                            String thumbPath = cursor.getString(cursor.getColumnIndex(FILE_THUMB_PATH));
                            pathList.add(fp);
                            thumbMap.put(fp, thumbPath);
                        } while (cursor.moveToNext());
                        cursor.close();
                    }
                    DatabaseHelper.unInitDatabase();
                    for (int i = 0; i < files.length; i++) {
                        File f = files[i];
                        String videoPath = f.getAbsolutePath();
                        if (videoPath.endsWith(".mp4")) {
                            if (pathList.contains(videoPath)) {
                                // 视频 已有缩略图

                                // 需要注意的是, 这里返回的都是图片的路径, 所以都是.jpg结尾, 为了区分, 在最后加上0代表视频, .jpg代表图片
                                File file = new File(thumbMap.get(videoPath) + "0");
                                cameraFiles[i] = file;
                            } else {
                                // 视频 没有缩略图, 创建一个
                                Bitmap bitmap = ThumbnailUtils.createVideoThumbnail(videoPath, MINI_KIND);
                                if (bitmap == null) throw new IOException("Get Video BitMap Error");
                                String name = videoPath.substring(videoPath.lastIndexOf("/") + 1, videoPath.lastIndexOf("."));
                                String thumbPath = AppContext.getThumbDirCameraVideo() + name + "_thumb.jpg";
                                if (CommonMethod.saveBitmap2JPG(bitmap, thumbPath)) {
                                    ContentValues c = new ContentValues();
                                    c.put(FILE_ID, "0");
                                    c.put(FILE_PATH, videoPath);
                                    c.put(FILE_THUMB_PATH, thumbPath);
                                    DatabaseHelper.insert(getApplicationContext(), c);
                                }
                                File file = new File(thumbPath + "0");
                                cameraFiles[i] = file;
                            }
                            videoPathMap.put(cameraFiles[i].getAbsolutePath(), videoPath);
                        } else cameraFiles[i] = f;
                    }
                    filePath = cameraFiles[0].getAbsolutePath();
                } else {
                    cameraFiles = new File[0];
                }
            } else {
                // 拍照、摄像回调
                File file = new File(path);
                if (!file.exists()) return "";
                filePath = path;
                File[] tempFile = new File[cameraFiles.length + 1];
                System.arraycopy(cameraFiles, 0, tempFile, 1, cameraFiles.length);
                // 先把路径赋值, 如果是视频文件, 则重新创建缩略图后再次赋值
                if (filePath.endsWith(".mp4")) {
                    // 新的录像视频 没有缩略图, 创建一个
                    Bitmap bitmap = ThumbnailUtils.createVideoThumbnail(filePath, MINI_KIND);
                    if (bitmap == null) throw new IOException("Get Video BitMap Error");
                    String name = filePath.substring(filePath.lastIndexOf("/") + 1, filePath.lastIndexOf("."));
                    String thumbPath = AppContext.getThumbDirCameraVideo() + name + "_thumb.jpg";
                    if (CommonMethod.saveBitmap2JPG(bitmap, thumbPath)) {
                        ContentValues c = new ContentValues();
                        c.put(FILE_ID, "0");
                        c.put(FILE_PATH, filePath);
                        c.put(FILE_THUMB_PATH, thumbPath);
                        DatabaseHelper.insert(getApplicationContext(), c);
                    }
                    filePath = thumbPath + "0";
                    videoPathMap.put(filePath, path);
                }
                tempFile[0] = new File(filePath);
                cameraFiles = tempFile;
            }
            return filePath;
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(targetPath -> {
            if (TextUtils.isEmpty(targetPath)) return;
            File target = null;
            if (targetPath.endsWith(".jpg0")) {
                targetPath = targetPath.substring(0, targetPath.length() - 1);
                target = new File(targetPath);
                videoSign.setVisibility(View.VISIBLE);
            } else if (targetPath.endsWith(".jpg")) {
                videoSign.clearAnimation();
                videoSign.setVisibility(View.GONE);
                target = new File(targetPath);
            } else {
                Timber.e("Error, File Path Illegal:%s", targetPath);
            }
            GlideApp.with(ActivityCamera2.this)
                    .load(target == null ? R.mipmap.free_pic_error : target)
                    .error(getDrawable(R.mipmap.free_pic_error))
                    .centerCrop()
                    .into(viewFiles);
            if (animation) {
                ScaleAnimation scale = new ScaleAnimation(1.2f, 1.0f, 1.2f, 1.0f,
                        Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                scale.setDuration(500);
                scale.setRepeatCount(0);
                viewFiles.startAnimation(scale);
            }
        }, Throwable::printStackTrace);
        mComDis.add(subscribe);
    }
}

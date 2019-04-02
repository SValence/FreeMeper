package com.valence.freemeper.function.camera;

import android.Manifest;
import android.app.Activity;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.LifecycleRegistry;
import android.arch.lifecycle.OnLifecycleEvent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import com.valence.freemeper.tool.AppContext;
import com.valence.freemeper.tool.CommonMethod;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static android.content.Context.CAMERA_SERVICE;

public class CameraHelper extends HandlerThread implements TextureView.SurfaceTextureListener, SurfaceHolder.Callback, LifecycleOwner {

    private ArrayList<Size> cameraSTList;
    private CameraManager manager;
    private CameraCharacteristics chara;
    private ImageReader imageReader;
    private int screenWidth, screenHeight;
    private CameraDevice cameraDevice;
    private Size previewSize = null;
    private CameraCaptureSession mCaptureSession;
    private CaptureRequest.Builder mPreviewBuilder, takePictureBuilder, recordBuilder;
    private final int DEFAULT_CAMERA_ID = 0;
    private int currentCameraID;
    private MediaRecorder mMediaRecorder;
    private static String recordingPath = "";
    private boolean isCameraReady;
    private int deviceRotation;

    private final LifecycleRegistry registry = new LifecycleRegistry(this);
    private OnPicRecCompleteListener mCompleteListener;
    private WeakReference<SurfaceView> surfaceViewRef;
    private WeakReference<TextureView> textureViewRef;
    private MediaScannerConnection mediaScanner;
    private Context mContext;
    private View backView;
    private View backViewFull;
    private final Handler looperHandler;

    private CompositeDisposable mComDis = new CompositeDisposable();

    private boolean isRecordMode = false; // 录像, 拍摄模式, 二选一, 非此即彼
    private int rotation = 0;
    private Integer mSensorOrientation;
    private static final int SENSOR_ORIENTATION_DEFAULT_DEGREES = 90;
    private static final int SENSOR_ORIENTATION_INVERSE_DEGREES = 270;
    private static final SparseIntArray DEFAULT_ORIENTATIONS = new SparseIntArray();
    private static final SparseIntArray INVERSE_ORIENTATIONS = new SparseIntArray();
    public static final int CAMERA_TYPE_PICTURE = 1;
    public static final int CAMERA_TYPE_RECORD = 2;

    static {
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_0, 90);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_90, 0);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_180, 270);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_270, 180);

        INVERSE_ORIENTATIONS.append(Surface.ROTATION_0, 270);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_90, 180);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_180, 90);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_270, 0);
    }

    private CameraHelper(CameraBuilder builder) {
        super("CameraHelper");
        this.surfaceViewRef = builder.surfaceViewRef;
        this.textureViewRef = builder.textureViewRef;
        this.mediaScanner = builder.mediaScanner;
        this.mContext = builder.mContext;
        this.backView = builder.backView;
        this.backViewFull = builder.backViewFull;
        this.mCompleteListener = builder.mCompleteListener;
        registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
        cameraSTList = new ArrayList<>();
        isCameraReady = false;

        start();
        looperHandler = new Handler(getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                Timber.i(msg.toString());
                super.handleMessage(msg);
            }
        };
        looperHandler.post(() -> {
            doStart();
            if (!openCamera(null, DEFAULT_CAMERA_ID)) {
                stopThread();
            }
        });
    }

    @Override
    public void run() {
        super.run();
    }

    private void doStart() {
        if (mContext == null) {
            throw new IllegalStateException("Must Call CameraBuilder.setContext");
        } else if (surfaceViewRef == null && textureViewRef == null) {
            throw new IllegalStateException("Call CameraBuilder.setSurfaceView or CameraBuilder.setTextureView first!");
        }
        manager = (CameraManager) mContext.getSystemService(CAMERA_SERVICE);
        DisplayMetrics dm = mContext.getResources().getDisplayMetrics();
        screenHeight = dm.heightPixels;
        screenWidth = dm.widthPixels;
        getLifecycle().addObserver(new LifecycleObserver() {
            @OnLifecycleEvent(value = Lifecycle.Event.ON_START)
            public void onStart() {
                // 在这里不做任何操作, 在Activity或者Fragment中调用 CameraHelper 时, 就意味着TextureView或者SurfaceView已经处于可预览状态
                // 这是要做的就是在 实例化 CameraHelper 后就立即开始预览相机画面
            }

            @OnLifecycleEvent(value = Lifecycle.Event.ON_STOP)
            public void onStop() {
                looperHandler.post(() -> {
                    closeCamera(false);
                    stopThread();
                });
            }
        });
        /*
         *  注意:
         *  这里 registry 的 Event 需要成对出现, 即没有 Lifecycle.Event.ON_START, 就不会执行 Lifecycle.Event.ON_STOP
         *  所以下面的代码中加了 Lifecycle.Event.ON_START 事件
         */
        if (surfaceViewRef != null && surfaceViewRef.get() != null) {
            SurfaceView surfaceView = surfaceViewRef.get();
            surfaceView.getHolder().addCallback(this);
            if (surfaceView.getHolder().getSurface() != null) {
                registry.handleLifecycleEvent(Lifecycle.Event.ON_START);
            }
        } else if (textureViewRef != null && textureViewRef.get() != null) {
            TextureView textureView = textureViewRef.get();
            textureView.setSurfaceTextureListener(this);
            if (textureView.isAvailable()) registry.handleLifecycleEvent(Lifecycle.Event.ON_START);
        }
    }

    private void stopThread() {
        mComDis.clear();
        quitSafely();
        interrupt();
        try {
            join(1000);
            if (!interrupted()) Timber.e("CameraHelper Thread not stopped");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    void changePreviewMode(boolean isRecordMode) {
        this.isRecordMode = isRecordMode;
        isCameraReady = false;

        Disposable d = Single.just(0).subscribeOn(AndroidSchedulers.from(looperHandler.getLooper()))
                .map(integer -> {
                    changeCameraResolution(previewSize, currentCameraID);
                    return 0;
                }).observeOn(AndroidSchedulers.mainThread()).subscribe();
        mComDis.add(d);
    }

    /**
     * 需要注意的问题:
     * public abstract void createCaptureSession(@NonNull List<Surface> outputs, @NonNull CameraCaptureSession.StateCallback callback, @Nullable Handler handler) 中:
     * List<Surface> 使用 Arrays.asList(mSurface, imageReader.getSurface()) 时, 初步测试是只能预览出 ImageFormat.JPEG 格式的相机画面。
     * 想要预览除了 NV21 格式的相机画面, List<Surface> 就需要使用 Collections.singletonList(mSurface) 类似的只有 Surface 的 List, 不能有从 imageReader 中获取的Surface
     */
    private void doPreview() {
        // TODO... 确认上述问题
        try {
            if (cameraDevice == null) {
                Timber.e("CameraDevice.TEMPLATE_PREVIEW----CameraDevice is null");
                return;
            }
            stopPreviewSession();
            //创建预览请求
            mPreviewBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            //设置自动对焦模式
            SurfaceTexture mSurfaceTexture = textureViewRef.get().getSurfaceTexture();
            //设置TextureView的缓冲区大小
            mSurfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            //获取Surface显示预览数据
            Surface mSurface = new Surface(mSurfaceTexture);
            //设置Surface作为预览数据的显示界面
            mPreviewBuilder.addTarget(mSurface);
            //创建相机捕获会话，第一个参数是捕获数据的输出Surface列表，第二个参数是CameraCaptureSession的状态回调接口，当它创建好后会回调onConfigured方法，第三个参数用来确定Callback在哪个线程执行，为null的话就在当前线程执行
            cameraDevice.createCaptureSession(isRecordMode ? Collections.singletonList(mSurface) : Arrays.asList(mSurface, imageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            try {
                                isCameraReady = true;
                                //开始预览
                                mCaptureSession = session;
                                mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                mPreviewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                                //设置反复捕获数据的请求，这样预览界面就会一直有数据显示
                                mCaptureSession.setRepeatingRequest(mPreviewBuilder.build(), null, looperHandler);
                                setBackViewVisibility(View.GONE);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            mCaptureSession = null;
                            Timber.e("TEMPLATE_PREVIEW onConfigureFailed");
                        }
                    }, null);
        } catch (CameraAccessException e) {
            closeCamera(false);
            e.printStackTrace();
        }
    }

    private CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        // 摄像头被打开时触发该方法
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            CameraHelper.this.cameraDevice = cameraDevice;
            // 开始预览
            doPreview();
        }

        // 摄像头断开连接时触发该方法
        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            cameraDevice.close();
            closeCamera(false);
        }

        // 打开摄像头出现错误时触发该方法
        @Override
        public void onError(CameraDevice cameraDevice, int error) {
            cameraDevice.close();
            if (CameraHelper.this.cameraDevice != null) CameraHelper.this.cameraDevice.close();
            CameraHelper.this.cameraDevice = null;
        }
    };

    private CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            // 拍照成功
            try {
                setBackViewVisibility(View.GONE);
                takePictureBuilder = null;
                // 重设自动对焦模式
                mPreviewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
                // 设置自动对焦模式
                mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                //重新进行预览
                mCaptureSession.setRepeatingRequest(mPreviewBuilder.build(), null, looperHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }

        }

        @Override
        public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
            super.onCaptureFailed(session, request, failure);
        }
    };

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        registry.handleLifecycleEvent(Lifecycle.Event.ON_START);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        registry.handleLifecycleEvent(Lifecycle.Event.ON_START);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    private boolean openCamera(@Nullable Size size, int id) {
        try {
            String cameraId = id < 0 ? "0" : String.valueOf(id);
            chara = manager.getCameraCharacteristics(cameraId);
            // 获取摄像头支持的配置属性
            StreamConfigurationMap map = chara.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            mSensorOrientation = chara.get(CameraCharacteristics.SENSOR_ORIENTATION);
            if (map == null) {
                Timber.e("获取摄像头支持的配置属性 ERROR");
                return false;
            }
            if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions((Activity) mContext, new String[]{"android.permission.CAMERA"}, 1);
                return false;
            }
            initCameraSize(size, map);
            // 创建一个ImageReader对象，用于获取摄像头的图像数据
            imageReader = ImageReader.newInstance(previewSize.getWidth(), previewSize.getHeight(), ImageFormat.JPEG, 2);
            // 设置获取图片的监听
            imageReader.setOnImageAvailableListener(reader -> CommonMethod.saveCameraImage(imageReader.acquireNextImage(),
                    mediaScanner, -deviceRotation, mCompleteListener), null);
            mMediaRecorder = new MediaRecorder();
            Completable.fromCallable(() -> {
                reSizeCameraWindow(previewSize);
                return 0;
            })
                    .subscribeOn(AndroidSchedulers.mainThread())
                    .observeOn(AndroidSchedulers.from(looperHandler.getLooper()))
                    .subscribe(() -> {
                        currentCameraID = id;
                        manager.openCamera(cameraId, stateCallback, null);
                    });
        } catch (CameraAccessException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void initCameraSize(@Nullable Size size, StreamConfigurationMap map) {
        cameraSTList.clear();
        if (isRecordMode) {
            if (size != null && (size.getWidth() > screenHeight || size.getHeight() > screenWidth))
                size = null;
            Size[] sizes = map.getOutputSizes(SurfaceTexture.class);
            for (Size s : sizes) {
                if (s.getWidth() <= screenHeight && s.getHeight() <= screenWidth)
                    cameraSTList.add(s);
            }
        } else
            cameraSTList.addAll(Arrays.asList(map.getOutputSizes(SurfaceTexture.class)));
        // 获取摄像头支持的最大尺寸
        Size largest = Collections.max(cameraSTList, new CompareSizesByArea());
        previewSize = size == null ? chooseOptimalSize(cameraSTList.toArray(new Size[0]), largest) : size;
    }

    void closeCamera(boolean mediaStayOn) {
        if (!mediaStayOn) {
            if (mMediaRecorder != null) {
                // 先停止录制
                if (isRecording()) {
                    mMediaRecorder.stop();
                    mMediaRecorder.reset();
                    // 扫描该文件, 否则系统中 Media 数据库 可能没有保存该文件的信息
                    if (!TextUtils.isEmpty(recordingPath) && mediaScanner != null && mediaScanner.isConnected())
                        mediaScanner.scanFile(recordingPath, null);
                    recordingPath = "";
                }
                mMediaRecorder.release();
                mMediaRecorder = null;
            }
            stopPreviewSession();
            if (cameraDevice != null) {
                cameraDevice.close();
                cameraDevice = null;
            }
        }
    }

    private Size chooseOptimalSize(Size[] choices, /*int width, int height,*/ Size aspectRatio) {
        ArrayList<Size> suitSizeArray = new ArrayList<>();
        Size result;
        for (Size size : choices) {
            if (isRecordMode && size.getWidth() > screenHeight && size.getHeight() > size.getWidth())
                continue;
            if (size.getHeight() * aspectRatio.getWidth() == size.getWidth() * aspectRatio.getHeight()) {
                suitSizeArray.add(size);
            }
        }
        result = suitSizeArray.size() > 0 ? Collections.max(suitSizeArray, new CompareSizesByArea()) : choices[0];
        Timber.i("chooseOptimalSize %s", result.toString());
        return result;
    }

    private void reSizeCameraWindow(Size size) {
        // 对于相机来说, 一般情况下为:宽度是不变的, 在分辨率发生变化的时候, 调整高度来适应尺寸
        // 所以, 这里以宽度为标准
        TextureView textureView = textureViewRef.get();
        ViewGroup.LayoutParams params = textureView.getLayoutParams();
        int heightP;
        int widthP = textureView.getWidth();
        // 先以宽度为标准, 如果当前View的宽度大于屏幕宽度, 把View的宽度设置为屏幕宽度, 确保View在屏幕内
        // 否则以当前View的宽度为准
        if (widthP > screenWidth) widthP = screenWidth;
        // 以size的比例计算出View的实际高度
        heightP = size.getWidth() * widthP / size.getHeight();
        // 如果这时计算出的View本来应该有的高度大于屏幕的高度, 那么把View的高度设置为屏幕的高度, 进一步确保View在屏幕内
        // 这时就是以高度为标准, 重新计算宽度
        // 否则直接更新设置View的宽高
        if (heightP > screenHeight) {
            heightP = screenHeight;
            widthP = size.getHeight() * heightP / size.getWidth();
        }
        params.height = heightP;// 实际上是Size的宽度
        params.width = widthP;  // 实际上是Size的高度
        textureView.setLayoutParams(params);

        ViewGroup.LayoutParams backParams = backView.getLayoutParams();
        backParams.width = widthP;
        backParams.height = heightP;
        backView.setLayoutParams(backParams);
    }

    void takePicture(final int rotation) {
        try {
            if (cameraDevice == null) {
                Timber.e("CameraDevice.TEMPLATE_STILL_CAPTURE----CameraDevice is null");
                return;
            }
            mPreviewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
            // 创建拍照请求
            takePictureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            // 将imageReader的surface设为目标
            takePictureBuilder.addTarget(imageReader.getSurface());
            // 获取设备方向, 根据设备方向计算设置照片的方向
            takePictureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getJpegOrientation(chara, rotation));
            // 停止预览
            // mCaptureSession.stopRepeating();
            // mCaptureSession.abortCaptures();
            //拍照, 设置拍照监听
            mCaptureSession.capture(takePictureBuilder.build(), captureCallback, looperHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    void startRecord(int rotation) {
        this.rotation = rotation;
        if (cameraDevice == null) {
            Timber.e("CameraDevice.TEMPLATE_RECORD----CameraDevice is null");
            return;
        }
        // 创建录像请求
        try {
            stopPreviewSession();
            setMediaRecorder();
            recordBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            recordBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            recordBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            // 为相机预览设置Surface
            SurfaceTexture mSurfaceTexture = textureViewRef.get().getSurfaceTexture();
            mSurfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            Surface mSurface = new Surface(mSurfaceTexture);
            recordBuilder.addTarget(mSurface);
            // 为 MediaRecorder设置Surface
            Surface recorderSurface = mMediaRecorder.getSurface();
            recordBuilder.addTarget(recorderSurface);
            cameraDevice.createCaptureSession(Arrays.asList(mSurface, recorderSurface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    try {
                        mCaptureSession = cameraCaptureSession;
                        recordBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                        mCaptureSession.setRepeatingRequest(recordBuilder.build(), null, looperHandler);
                        Timber.i("Start Record Success");
                        mMediaRecorder.start();
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Timber.e("Start Record Failed");
                }
            }, null);
        } catch (CameraAccessException | IOException e) {
            e.printStackTrace();
        }
    }

    void stopRecord() {
        try {
            mCaptureSession.stopRepeating();
            mCaptureSession.abortCaptures();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Disposable disposable = Observable
                .timer(30, TimeUnit.MICROSECONDS, Schedulers.computation())
                .observeOn(AndroidSchedulers.from(looperHandler.getLooper()))
                .subscribe(l -> {
                    // 停止录制
                    mMediaRecorder.stop();
                    mMediaRecorder.reset();
                    Timber.i("Stop Record Success");
                    // 扫描该文件, 否则系统中 Media 数据库 可能没有保存该文件的信息
                    if (!TextUtils.isEmpty(recordingPath) && mediaScanner != null && mediaScanner.isConnected())
                        mediaScanner.scanFile(recordingPath, null);
                    if (mCompleteListener != null)
                        mCompleteListener.onPicRecComplete(CameraHelper.CAMERA_TYPE_RECORD, recordingPath);
                    recordingPath = "";
                    recordBuilder = null;
                    doPreview();
                }, Throwable::printStackTrace);
    }

    public static boolean isRecording() {
        return !TextUtils.isEmpty(recordingPath);
    }

    private void stopPreviewSession() {
        if (mCaptureSession != null) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
    }

    private void setMediaRecorder() throws IOException {
        if (TextUtils.isEmpty(recordingPath)) {
            recordingPath = AppContext.getNewRecordFilePath();
            if (TextUtils.isEmpty(recordingPath)) {
                return;
            }
        }
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setOutputFile(recordingPath);
        mMediaRecorder.setVideoSize(previewSize.getWidth(), previewSize.getHeight());
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

        mMediaRecorder.setAudioChannels(1);
        mMediaRecorder.setVideoFrameRate(30);
        long size = previewSize.getHeight() * previewSize.getWidth();
        if (size >= 2073600) {
            // >= 1080P
            mMediaRecorder.setVideoEncodingBitRate(10000000);
            mMediaRecorder.setAudioEncodingBitRate(320000);
            mMediaRecorder.setAudioSamplingRate(48000);
        } else if (size >= 921600) {
            // >= 720P
            mMediaRecorder.setVideoEncodingBitRate(5000000);
            mMediaRecorder.setAudioEncodingBitRate(192000);
            mMediaRecorder.setAudioSamplingRate(32000);
        } else {
            // < 720P
            mMediaRecorder.setVideoEncodingBitRate(2500000);
            mMediaRecorder.setAudioEncodingBitRate(128000);
            mMediaRecorder.setAudioSamplingRate(12000);
        }
        switch (mSensorOrientation) {
            case SENSOR_ORIENTATION_DEFAULT_DEGREES:
                mMediaRecorder.setOrientationHint(DEFAULT_ORIENTATIONS.get(rotation));
                break;
            case SENSOR_ORIENTATION_INVERSE_DEGREES:
                mMediaRecorder.setOrientationHint(INVERSE_ORIENTATIONS.get(rotation));
                break;
        }
        mMediaRecorder.prepare();
    }

    int getCameraCount() {
        int count;
        if (manager == null) {
            Timber.e("Can not get Camera count, CameraManager is null");
            count = -1;
        } else {
            try {
                count = manager.getCameraIdList().length;
            } catch (CameraAccessException e) {
                e.printStackTrace();
                count = -1;
            }
        }
        return count;
    }

    boolean isCameraReady() {
        return isCameraReady;
    }

    int getCurrentCameraId() {
        return currentCameraID;
    }

    void setDeviceRotation(int deviceRotation) {
        this.deviceRotation = deviceRotation;
    }

    ArrayList<Size> getCameraSTList() {
        return cameraSTList;
    }

    void switchCamera(int cameraId) {
        setBackViewVisibility(View.VISIBLE);
        closeCamera(false);
        if (!openCamera(null, cameraId)) {
            stopThread();
        }
    }

    private void setBackViewVisibility(int visible) {
        if (visible == View.GONE) {
            looperHandler.postDelayed(() -> {
                Disposable d = Single.fromCallable(() -> {
                    backViewFull.setVisibility(visible);
                    return 0;
                }).subscribeOn(AndroidSchedulers.mainThread()).subscribe();
                mComDis.add(d);
            }, 500);
        } else {
            Disposable d = Single.fromCallable(() -> {
                backViewFull.setVisibility(visible);
                return 0;
            }).subscribeOn(AndroidSchedulers.mainThread()).subscribe();
            mComDis.add(d);
        }
    }

    void changeCameraResolution(@Nullable Size size, int cameraId) {
        setBackViewVisibility(View.VISIBLE);
        closeCamera(false);
        if (!openCamera(size, cameraId)) {
            stopThread();
        }
    }

    @NonNull
    @Override
    public Lifecycle getLifecycle() {
        return registry;
    }

    private int getJpegOrientation(CameraCharacteristics c, int deviceOrientation) {
        if (c == null) {
            return 0;
        }
        if (deviceOrientation == android.view.OrientationEventListener.ORIENTATION_UNKNOWN)
            return 0;
        int sensorOrientation = c.get(CameraCharacteristics.SENSOR_ORIENTATION);

        // Round device orientation to a multiple of 90
        deviceOrientation = (deviceOrientation + 45) / 90 * 90;

        // Reverse device orientation for front-facing cameras
        boolean facingFront = c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT;
        if (facingFront) deviceOrientation = -deviceOrientation;

        // Calculate desired JPEG orientation relative to camera orientation to make
        // the image upright relative to the device orientation

        return (sensorOrientation + deviceOrientation + 360) % 360;
    }

    // 为Size定义一个比较器Comparator
    static class CompareSizesByArea implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            // 强转为long保证不会发生溢出
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() - (long) rhs.getWidth() * rhs.getHeight());
        }
    }

    Size getPreviewSize() {
        return previewSize;
    }

    public static class CameraBuilder {

        private CameraHelper helper;

        private OnPicRecCompleteListener mCompleteListener;
        private WeakReference<SurfaceView> surfaceViewRef;
        private WeakReference<TextureView> textureViewRef;
        private MediaScannerConnection mediaScanner;
        private Context mContext;
        private View backView;
        private View backViewFull;

        public CameraBuilder setContext(Context mContext) {
            this.mContext = mContext;
            return this;
        }

        public CameraBuilder setSurfaceView(SurfaceView surfaceView) {
            this.surfaceViewRef = new WeakReference<>(surfaceView);
            return this;
        }

        CameraBuilder setTextureView(TextureView textureView) {
            this.textureViewRef = new WeakReference<>(textureView);
            return this;
        }

        CameraBuilder setBackView(View backView) {
            this.backView = backView;
            return this;
        }

        CameraBuilder setBackViewFull(View backViewFull) {
            this.backViewFull = backViewFull;
            return this;
        }

        CameraBuilder setMediaScanner(MediaScannerConnection mediaScanner) {
            this.mediaScanner = mediaScanner;
            return this;
        }

        CameraHelper startCamera() {
            helper = new CameraHelper(this);
            return helper;
        }

        CameraBuilder setCompleteListener(OnPicRecCompleteListener mCompleteListener) {
            this.mCompleteListener = mCompleteListener;
            return this;
        }
    }

    public interface OnPicRecCompleteListener {
        void onPicRecComplete(int picOrRec, String filePath);
    }
}


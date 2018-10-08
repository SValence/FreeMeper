package com.valence.freemeper.function.camera;

import android.Manifest;
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
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import com.valence.freemeper.R;
import com.valence.freemeper.base.BaseActivity;
import com.valence.freemeper.tool.AppContext;
import com.valence.freemeper.tool.CommonMethod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

public class ActivityCamera extends BaseActivity {

    private static final String TAG = "ActivityCamera";

    //    private int height = 0, width = 0;
    private TextureView textureView;
    private CameraCaptureSession mPreviewSession;
    private CaptureRequest.Builder mCaptureRequestBuilder, takePictureBuilder;
    private CaptureRequest mCaptureRequest;
    private ArrayList<Size> cameraSTList;
    private ArrayList<Size> pictureSizeList;
    private int screenWidth, screenHeight;
    private CameraManager manager;
    private int cameraID;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_camera);

        manager = (CameraManager) getSystemService(CAMERA_SERVICE);
        textureView = findViewById(R.id.free_camera);
        textureView.setOpaque(true);
        textureView.setSurfaceTextureListener(textureListener);
        cameraSTList = new ArrayList<>();
        pictureSizeList = new ArrayList<>();

        DisplayMetrics dm = getResources().getDisplayMetrics();
        screenHeight = dm.heightPixels;
        screenWidth = dm.widthPixels;
        cameraID = 0;
    }

    private ImageReader imageReader;
    private Size previewSize = null;

    private void openCamera(@Nullable Size size, int id) {
        if (manager == null) {
            Log.e(TAG, "Get CameraManager Failed");
            return;
        }
        try {
            int count = manager.getCameraIdList().length;
            if (count == 0) {
                Log.e(TAG, "Device Has No Camera");
                return;
            }
            String cameraId = id < 0 ? "0" : String.valueOf(id);
            CameraCharacteristics chara = manager.getCameraCharacteristics(cameraId);
            // 获取摄像头支持的配置属性
            StreamConfigurationMap map = chara.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (map == null) {
                Log.e(TAG, "获取摄像头支持的配置属性 ERROR");
                return;
            }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                ActivityCompat.requestPermissions(ActivityCamera.this, new String[]{"android.permission.CAMERA"}, 1);
                return;
            }
            // 获取摄像头支持的最大尺寸
            pictureSizeList = new ArrayList<>(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)));
            Size largest = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)), new CompareSizesByArea());
            // 创建一个ImageReader对象，用于获取摄像头的图像数据
            imageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(), ImageFormat.JPEG, 2);
            // 设置获取图片的监听
            imageReader.setOnImageAvailableListener(reader -> CommonMethod.saveImage(imageReader.acquireNextImage()), null);
            // 获取最佳的预览尺寸
            cameraSTList = new ArrayList<>(Arrays.asList(map.getOutputSizes(SurfaceTexture.class)));
            previewSize = size == null ? chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), largest) : size;
            reSizeCameraWindow(previewSize);
            manager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private CameraDevice cameraDevice;
    /**
     * 摄像头状态的监听
     */
    private CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        // 摄像头被打开时触发该方法
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            ActivityCamera.this.cameraDevice = cameraDevice;
            // 开始预览
            doPreview();
        }

        // 摄像头断开连接时触发该方法
        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            closeCamera();
            ActivityCamera.this.cameraDevice = null;
        }

        // 打开摄像头出现错误时触发该方法
        @Override
        public void onError(CameraDevice cameraDevice, int error) {
            cameraDevice.close();
            ActivityCamera.this.cameraDevice = null;
        }
    };

    /**
     * 监听拍照结果
     */
    private CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {
        // 拍照成功
        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            // 重设自动对焦模式
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            // 设置自动曝光模式
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            try {
                //重新进行预览
                mPreviewSession.setRepeatingRequest(mCaptureRequest, null, null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }

        }

        @Override
        public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
            super.onCaptureFailed(session, request, failure);
        }
    };

    private void doPreview() {
        SurfaceTexture mSurfaceTexture = textureView.getSurfaceTexture();
        //设置TextureView的缓冲区大小
        mSurfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
        //获取Surface显示预览数据
        Surface mSurface = new Surface(mSurfaceTexture);
        try {
            //创建预览请求
            mCaptureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            //设置自动对焦模式
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            //设置Surface作为预览数据的显示界面
            mCaptureRequestBuilder.addTarget(mSurface);
            //创建相机捕获会话，第一个参数是捕获数据的输出Surface列表，第二个参数是CameraCaptureSession的状态回调接口，当它创建好后会回调onConfigured方法，第三个参数用来确定Callback在哪个线程执行，为null的话就在当前线程执行
            cameraDevice.createCaptureSession(Arrays.asList(mSurface, imageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        //开始预览
                        mCaptureRequest = mCaptureRequestBuilder.build();
                        mPreviewSession = session;
                        //设置反复捕获数据的请求，这样预览界面就会一直有数据显示
                        mPreviewSession.setRepeatingRequest(mCaptureRequest, captureCallback, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }

                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                }
            }, null);
        } catch (CameraAccessException e) {
            closeCamera();
            e.printStackTrace();
        }
    }

    private void closeCamera() {
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        preOrOpenCamera();
    }

    @Override
    protected void onPause() {
        super.onPause();
        closeCamera();
    }


    private void preOrOpenCamera() {
        if (textureView.isAvailable()) openCamera(previewSize, cameraID);
        else textureView.setSurfaceTextureListener(textureListener);
    }

    private TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            Log.e(TAG, "onSurfaceTextureAvailable----height:" + height + ";width:" + width);
//            ActivityCamera.this.height = height;
//            ActivityCamera.this.width = width;
            openCamera(previewSize, cameraID);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            Log.e(TAG, "onSurfaceTextureSizeChanged----height:" + height + ";width:" + width);
//                closeCamera();
//                ActivityCamera.this.height = height;
//                ActivityCamera.this.width = width;
//                openCamera();
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            closeCamera();
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    private Size chooseOptimalSize(Size[] choices, /*int width, int height,*/ Size aspectRatio) {

        ArrayList<Size> suitSizeArray = new ArrayList<>();
        Size result;
        for (Size size : choices) {
            if (size.getHeight() * aspectRatio.getWidth() == size.getWidth() * aspectRatio.getHeight()) {
                suitSizeArray.add(size);
            }
        }
        if (suitSizeArray.size() > 0) {
            result = Collections.max(suitSizeArray, new CompareSizesByArea());
        } else {
            result = choices[0];
        }
        return result;

//        // 收集摄像头支持的大过预览Surface的分辨率
//        List<Size> bigEnough = new ArrayList<>();
//        int w = aspectRatio.getWidth();
//        int h = aspectRatio.getHeight();
//        for (Size option : choices) {
//            if (option.getHeight() == option.getWidth() * h / w && option.getWidth() >= width && option.getHeight() >= height) {
//                bigEnough.add(option);
//            }
//        }
//        // 如果找到多个预览尺寸，获取其中面积最小的
//        if (bigEnough.size() > 0) {
//            return Collections.min(bigEnough, new CompareSizesByArea());
//        } else {
//            //没有合适的预览尺寸
//            return choices[0];
//        }
    }

    // 为Size定义一个比较器Comparator
    static class CompareSizesByArea implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            // 强转为long保证不会发生溢出
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }
    }

    private void reSizeCameraWindow(Size size) {
        // 对于相机来说, 一般情况下为:宽度是不变的, 在分辨率发生变化的时候, 调整高度来适应尺寸
        // 所以, 这里以宽度为标准
        ViewGroup.LayoutParams params = textureView.getLayoutParams();
        int heightP;
        int widthP = textureView.getHeight();
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
            widthP = size.getWidth() * heightP / size.getHeight();
        }
        params.height = heightP;// 实际上是Size的宽度
        params.width = widthP;  // 实际上是Size的高度
        textureView.setLayoutParams(params);
    }

    public void showResolutionList(View view) {
        if (cameraSTList == null || cameraSTList.isEmpty()) AppContext.showToast("支持分辨率为空");
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("摄像头支持分辨率");
        CharSequence[] list = new CharSequence[cameraSTList.size()];
        for (int i = 0; i < list.length; i++) {
            list[i] = cameraSTList.get(i).toString();
        }
        dialog.setItems(list, (dialog1, which) -> {
            closeCamera();
            openCamera(cameraSTList.get(which), cameraID);
        });
        dialog.setCancelable(true);
        dialog.show();
    }

    public void showPictureList(View view) {
        if (pictureSizeList == null || pictureSizeList.isEmpty()) AppContext.showToast("支持图片分辨率为空");
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("相片支持分辨率");
        CharSequence[] list = new CharSequence[pictureSizeList.size()];
        for (int i = 0; i < list.length; i++) {
            list[i] = pictureSizeList.get(i).toString();
        }
        dialog.setItems(list, null);
        dialog.setCancelable(true);
        dialog.show();
    }

    public void onSwitchCamera(View view) {
        if (manager == null) {
            Log.e(TAG, "Get CameraManager Failed");
            return;
        }
        try {
            int count = manager.getCameraIdList().length;
            if (count == 0) {
                Log.e(TAG, "Device Has No Camera");
            } else if (count == 1) {
                AppContext.showToast(getString(R.string.device_one_camera));
            } else {
                view.setClickable(false);
                AlertDialog.Builder dialog = new AlertDialog.Builder(this);
                dialog.setTitle(R.string.choose_camera);
                CharSequence[] list = new CharSequence[count];
                list[0] = getString(R.string.back_camera);
                list[1] = getString(R.string.front_camera);
                for (int i = 2; i < list.length; i++) {
                    list[i] = pictureSizeList.get(i).toString();
                }
                dialog.setItems(list, (dialog1, which) -> {
                    cameraID = which;
                    closeCamera();
                    openCamera(null, cameraID);
                });
                dialog.setCancelable(true);
                dialog.setOnDismissListener(dialog1 -> view.setClickable(true));
                dialog.show();
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void onTakePicture(View view) {
        try {
            if (cameraDevice == null) {
                return;
            }
            // 创建拍照请求
            takePictureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            // 设置自动对焦模式
            takePictureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            // 将imageReader的surface设为目标
            takePictureBuilder.addTarget(imageReader.getSurface());
            // 获取设备方向
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            // 根据设备方向计算设置照片的方向
            takePictureBuilder.set(CaptureRequest.JPEG_ORIENTATION
                    , ORIENTATIONS.get(rotation));
            // 停止连续取景
            mPreviewSession.stopRepeating();
            //拍照
            CaptureRequest captureRequest = takePictureBuilder.build();
            //设置拍照监听
            mPreviewSession.capture(captureRequest, captureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
}

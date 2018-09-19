package com.valence.freemeper.tool;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Instrumentation;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.valence.freemeper.cusview.CircleImage;
import com.valence.freemeper.cusview.MarginBean;
import com.valence.freemeper.database.DatabaseHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * 共通方法类库
 *
 * @author Valence
 * @version 1.0
 * @since 2017/09/13
 */
public class CommonMethod {

    private static final String TAG = "CommonMethod";

    public static void makeSingleToast(Context context, String value) {
        Toast.makeText(context, value, Toast.LENGTH_SHORT).show();
    }

    /******************************************************************************
     * 该方法用来短暂显示一些提示信息,可以在多次Toast时,直接显示最新的提示内容
     *
     * @param mToast
     *            Toast对象,已经指定了上下文,所以不需要传递Context
     * @param tips
     *            要显示的提示信息
     */
    public static void makeShortToast(Toast mToast, String tips) {
        mToast.setDuration(Toast.LENGTH_SHORT);
        mToast.setText(tips);
        mToast.show();
    }

    public static void setMargins(View v, int l, int t, int r, int b) {
        if (v.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            p.setMargins(l, t, r, b);
            v.requestLayout();
        }
    }

    /******************************************************************************
     * 该函数用来从已知SD卡或存储中文件的path，获得其所在文件夹的path
     *
     * @param filePath
     *            文件路径
     */
    public static String getDirPathOfFile(String filePath) {
        String dirPath = null;
        int index = 0;
        if (filePath.equals("") || filePath.length() == 0)
            return null;
        for (int i = filePath.length() - 1; i >= 0; i--) {
            if (filePath.charAt(i) == '/') {
                index = i;
                break;
            }
        }
        dirPath = filePath.substring(0, index);
        return dirPath;
    }

    /******************************************************************************
     * 该方法用来显示单一按钮的对话框，用来显示一些重要的提示信息
     *
     * @param ac
     *            需要显示对话框的 activity
     * @param title
     *            提示标题
     * @param message
     *            提示内容
     * @param buttonMessage
     *            按钮显示文本
     */
    public static void makeSigleDialog(Activity ac, String title, String message, String buttonMessage) {
        new AlertDialog.Builder(ac).setTitle(title)
                // 设置对话框标题
                .setMessage(message)
                .setPositiveButton(buttonMessage, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int which) {
                        // 确定按钮的响应事件

                    }
                }).show();
    }

    /******************************************************************************
     * 方法：setTranslucentStatus(boolean on),用来设置是否对状态栏进行编辑
     *
     * @param on
     *            true 是, false 否;
     * @param ac
     *            ac：指出获取窗口的页面
     */
    @TargetApi(19)
    public static void setTranslucentStatus(boolean on, Activity ac) {
        Window win = ac.getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        final int bits = WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
        if (on) {
            winParams.flags |= bits;
        } else {
            winParams.flags &= ~bits;
        }
        win.setAttributes(winParams);
    }

    /******************************************************************************
     * 方法: back() , 用来触发点击返回键效果
     */
    public static void back() {
        new Thread() {
            public void run() {
                try {
                    Instrumentation inst = new Instrumentation();
                    inst.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
                } catch (Exception e) {
                    Log.e("SendKeyException", e.toString());
                }
            }
        }.start();
    }

    /******************************************************************************
     * 方法: longTime2String() , 用来触发点击返回键效果
     *
     * @param time long 类型的时间
     *
     */
    public static String longTime2String(long time) {
        String timeStr;
        time /= 1000;
        long hour = time / 3600;
        long minute = (time - hour * 3600) / 60;
        long second = time - hour * 3600 - minute * 60;
        timeStr = String.format("%s:%s:%s", hour < 10 ? "0" + hour : hour, minute < 10 ? "0" + minute : minute,
                second < 10 ? "0" + second : second);
        return timeStr;
    }

    /******************************************************************************
     * 保存图片到指定路径
     * @param bt bitmap图片
     * @param path 文件路径
     */
    public static boolean saveBitmap2JPG(Bitmap bt, String path) {
        if (TextUtils.isEmpty(path)) {
            Log.e(TAG, "File Path is Empty!");
            return false;
        }
        File file = new File(path);
        try {
            FileOutputStream out = new FileOutputStream(file);
            bt.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @SuppressLint("CheckResult")
    public static void doClearCache(Context context) {
        Single.fromCallable(() -> {
            // 这里删除两次是因为华为Mate10删除时会出现删除文件对应的.hwbk文件, 所以要进行二次删除
            DatabaseHelper.cleanTable(context);
            int ret = deleteFile(AppContext.getThumbDirPhoto());
            if (ret != 0) return ret;
            ret = deleteFile(AppContext.getThumbDirPhoto());
            if (ret != 0) return ret;
            Log.w(TAG, "Photo Thumb Directory Has Cleaned");
            ret = deleteFile(AppContext.getThumbDirVideo());
            if (ret == 0) Log.w(TAG, "Video Thumb Directory Has Cleaned");
            ret = deleteFile(AppContext.getThumbDirVideo());
            if (ret == 0) Log.w(TAG, "Video Thumb Directory Has Cleaned");
            return ret;
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(integer -> {
                    if (integer != 0)
                        makeSingleToast(context, "Clear Cache Failed —— ErrorCode:" + integer);
                    else makeSingleToast(context, "Clear Cache Success");
                }, Throwable::printStackTrace);
    }

    /******************************************************************************
     * 删除指定路径的文件
     * @param dirPath 要删除的文件路径
     * @return 错误码
     */
    public static int deleteFile(String dirPath) {
        if (TextUtils.isEmpty(dirPath)) return 30001;
        File f = new File(dirPath);
        if (!f.exists()) return 0;
        if (!f.isDirectory()) return 30002;
        for (File fi : f.listFiles()) {
            if (fi.isDirectory()) deleteFile(fi.getAbsolutePath());
            else {
                if (!fi.delete()) {
                    Log.e(TAG, String.format("Delete File %s Error", fi.getAbsolutePath()));
                    return 30003;
                }
            }
        }
        return 0;
    }

    public static int px2dip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    public static int getStatusHeight(Context context) {

        int statusHeight = -1;
        try{
            Class clazz = Class.forName("com.android.internal.R$dimen");
            Object object = clazz.newInstance();
            int height = Integer.parseInt(clazz.getField("status_bar_height")
                    .get(object).toString());
            statusHeight = context.getResources().getDimensionPixelSize(height);
        }catch(Exception e) {
            e.printStackTrace();
        }
        return statusHeight;
    }

    public static void setImageMargin(boolean isOutSide, CircleImage image, MarginBean marginBean, int imageSize) {
        if (!isMarginValid(marginBean, imageSize)) return;
        ViewGroup.MarginLayoutParams param = (ViewGroup.MarginLayoutParams) image.getLayoutParams();
        int left = marginBean.getLeft();
        int top = marginBean.getTop();
        int right = marginBean.getRight();
        int bottom = 0;
        int image_margin = marginBean.getMargin();
        if (!isOutSide) {
            if (image_margin == 0) param.setMargins(left, top, right, bottom);
            else param.setMargins(image_margin, image_margin, image_margin, 0);
        } else param.setMargins(left, top, right, bottom);
        image.setLayoutParams(param);
    }

    public static  boolean isMarginValid(MarginBean marginBean, int image_size) {
        int image_margin = marginBean.getMargin();
        if (image_margin == 0) {
            if (image_size <= marginBean.getTop() + marginBean.getBottom() || image_size <= marginBean.getLeft() + marginBean.getRight()) {
                Log.e(TAG, "Margin is invalid");
                return false;
            }
        } else {
            if (image_size <= 2 * image_margin || image_size <= 2 * image_margin) {
                Log.e(TAG, "Margin is invalid");
                return false;
            }
        }
        return true;
    }
}

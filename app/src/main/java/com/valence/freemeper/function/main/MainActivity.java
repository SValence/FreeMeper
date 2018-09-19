package com.valence.freemeper.function.main;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.valence.freemeper.R;
import com.valence.freemeper.base.BaseActivity;
import com.valence.freemeper.base.IFindView;
import com.valence.freemeper.cusview.ImageTextGroup;
import com.valence.freemeper.function.about.AboutActivity;
import com.valence.freemeper.function.album.AlbumActivity;
import com.valence.freemeper.function.video.VideoListActivity;
import com.valence.freemeper.tool.CommonMethod;

/**
 * @author Valence
 * @version 1.0
 * @since 2017/09/03
 */

public class MainActivity extends BaseActivity implements IFindView, View.OnClickListener {

    private DefinedHorizontalScrollView scrollView;
    private FrameLayout mMenu;
    private FrameLayout mSetting;
    private Toast mToast;
    private ImageTextGroup video;
    private ImageTextGroup album;
    private ImageTextGroup music;
    private ImageTextGroup text;
    private ImageTextGroup camera;
    private ImageTextGroup about;
    private PopupWindow mPopWindow;
    private ResolveInfo resolveInfo;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findView();
        setListener();
        setVariate();
    }

    /**
     * 变量初始值设定
     */
    @SuppressLint("ShowToast")
    @Override
    public void setVariate() {
        scrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        mToast = Toast.makeText(getApplicationContext(), "", Toast.LENGTH_SHORT);
        initPopupWindow(getApplicationContext());
        resolveInfo = this.getPackageManager().resolveActivity(new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME), 0);
    }

    /**
     * 调用findViewById()确认控件
     */
    @Override
    public void findView() {
        scrollView = findViewById(R.id.scrollView);
        mMenu = findViewById(R.id.freeMainMenu);
        mSetting = findViewById(R.id.freeMainPerson);
        video = findViewById(R.id.freeMainVideo);
        album = findViewById(R.id.freeMainAlbum);
        music = findViewById(R.id.freeMainMusic);
        text = findViewById(R.id.freeMainText);
        camera = findViewById(R.id.freeMainCamera);
        about = findViewById(R.id.freeMainAbout);
    }

    /**
     * 给各控件设置监听事件
     */
    @Override
    public void setListener() {
        mMenu.setOnClickListener(this);
        mSetting.setOnClickListener(this);
        video.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, VideoListActivity.class);
            startActivity(intent);
        });
        album.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AlbumActivity.class);
            startActivity(intent);
        });
        music.setOnClickListener(v -> {
//            Intent intent = new Intent(MainActivity.this, Main2Activity.class);
//            startActivity(intent);
        });
        text.setOnClickListener(this);
        camera.setOnClickListener(v -> {
//            Intent intent = new Intent(MainActivity.this, Main3Activity.class);
//            startActivity(intent);
        });
        about.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AboutActivity.class);
            startActivity(intent);
        });
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.freeMainMenu) {
            scrollView.click();
        } else if (id == R.id.freeMainPerson) {
            mPopWindow.getContentView().measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            mSetting.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            int i = mPopWindow.getContentView().getMeasuredWidth();
            int j = mSetting.getMeasuredWidth();
            mPopWindow.showAsDropDown(mSetting, j * 2 - i - 10, 10);
        } else if (id == R.id.mainPopMenuSetting) {
            if (mPopWindow.isShowing()) {
                mPopWindow.dismiss();
            }
            clearCache();
        } else if (id == R.id.mainPopMenuAbout) {
            if (mPopWindow.isShowing()) {
                mPopWindow.dismiss();
            }
            Intent intent = new Intent(MainActivity.this, Main2Activity.class);
            startActivity(intent);
        }
    }

    private void clearCache() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("清除缓存");
        dialog.setMessage("确认清除缓存?\n清除缓存将清除所有缩略图和数据库中的相关信息");
        dialog.setNegativeButton("取消", null);
        dialog.setPositiveButton("确认", (dialog1, which) -> CommonMethod.doClearCache(this));
        dialog.show();
    }

    public void initPopupWindow(Context context) {
        @SuppressLint("InflateParams") View inflateView = LayoutInflater.from(context)
                .inflate(R.layout.layout_popup_menu, null, false);
        mPopWindow = new PopupWindow(inflateView, ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, true);
        mPopWindow.setAnimationStyle(R.style.PopupMenuStyle);
        mPopWindow.setTouchable(true);
        mPopWindow.setOutsideTouchable(true);
        mPopWindow.setFocusable(true);
        mPopWindow.setBackgroundDrawable(new ColorDrawable(0x00000000));
        mPopWindow.setTouchInterceptor((view, motionEvent) -> {
            // 暂时使用不到这一层触摸监听事件
            return false;
        });

        // 初始化PopupWindow内控件
        TextView txt_setting = inflateView.findViewById(R.id.mainPopMenuSetting);
        TextView txt_about = inflateView.findViewById(R.id.mainPopMenuAbout);
        txt_setting.setOnClickListener(this);
        txt_about.setOnClickListener(this);
    }

    private void toHome() {
        ActivityInfo activityInfo = resolveInfo.activityInfo;
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        homeIntent.setComponent(new ComponentName(activityInfo.packageName, activityInfo.name));
        homeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivitySafely(homeIntent);
    }

    private void startActivitySafely(Intent intent) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            CommonMethod.makeShortToast(mToast, "ACTIVITY START ERROR!");
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            toHome();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}

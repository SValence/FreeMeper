package com.valence.freemeper.function.about;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.valence.freemeper.R;
import com.valence.freemeper.base.BaseActivity2;
import com.valence.freemeper.tool.CommonMethod;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import timber.log.Timber;

public class AboutActivity extends BaseActivity2 implements View.OnClickListener {

    private TextView deviceID;
    private TextView model;
    private TextView hardVersion;
    private TextView softwareVersion;
    private RelativeLayout upgrade;
    private ImageView back;
    private ImageView logo;
    private Disposable disposable = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        findView();
        setListener();
        initData();
    }

    @Override
    public void initData() {
        deviceID.setText(String.valueOf(getString(R.string.app_name) + "001"));
        model.setText(Build.MODEL);
        hardVersion.setText(String.valueOf("Android " + Build.VERSION.SDK_INT));

        try {
            PackageManager pm = getPackageManager();
            PackageInfo packageInfo = pm.getPackageInfo(getPackageName(), 0);
            softwareVersion.setText(packageInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void findView() {
        deviceID = findViewById(R.id.tv_device_id);
        model = findViewById(R.id.tv_model);
        hardVersion = findViewById(R.id.tv_hardVer);
        softwareVersion = findViewById(R.id.tv_version);
        upgrade = findViewById(R.id.about_update);
        back = findViewById(R.id.free_tool_left_img);

        logo = findViewById(R.id.free_about_logo);
        logo.getDrawable().setCallback(new Drawable.Callback() {
            @Override
            public void invalidateDrawable(@NonNull Drawable who) {
                Timber.e("ImageView: logo:" + logo.getVisibility());
            }

            @Override
            public void scheduleDrawable(@NonNull Drawable who, @NonNull Runnable what, long when) {

            }

            @Override
            public void unscheduleDrawable(@NonNull Drawable who, @NonNull Runnable what) {

            }
        });
    }

    @Override
    public void setListener() {
        upgrade.setOnClickListener(this);
        back.setOnClickListener(this);

//        disposable = Observable.interval(0, 2, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
//                .subscribe(aLong -> logo.setVisibility(logo.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));
    }

    @Override
    protected void onDestroy() {
        if (disposable != null) {
            disposable.dispose();
            disposable = null;
        }
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.about_update:
                CommonMethod.makeSingleToast(this, getString(R.string.latest_version));
                break;
            case R.id.free_tool_left_img:
                onBackPressed();
                break;
            default:
                break;
        }
    }
}

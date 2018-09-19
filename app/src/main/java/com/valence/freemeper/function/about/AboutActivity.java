package com.valence.freemeper.function.about;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.valence.freemeper.R;
import com.valence.freemeper.base.BaseActivity;
import com.valence.freemeper.base.IFindView;
import com.valence.freemeper.tool.CommonMethod;

public class AboutActivity extends BaseActivity implements IFindView, View.OnClickListener {

    private TextView deviceID;
    private TextView model;
    private TextView hardVersion;
    private TextView softwareVersion;
    private RelativeLayout upgrade;
    private ImageView back;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        findView();
        setListener();
        setVariate();
    }

    @Override
    public void setVariate() {
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
    }

    @Override
    public void setListener() {
        upgrade.setOnClickListener(this);
        back.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.about_update:
                CommonMethod.makeSingleToast(this, "已是最新版本");
                break;
            case R.id.free_tool_left_img:
                onBackPressed();
                break;
            default:
                break;
        }
    }
}

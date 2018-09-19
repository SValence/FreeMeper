package com.valence.freemeper.welcome;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.valence.freemeper.R;
import com.valence.freemeper.function.main.MainActivity;
import com.valence.freemeper.tool.VariableSet;

/**
 * @author Valence
 * @version 1.1
 * @since 2017/09/03
 */
public class WelcomeActivity extends AppCompatActivity implements View.OnClickListener {

    private final String[] permissions = {
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private final int PMS_LENGTH = permissions.length;
    private int countTime;
    private boolean isSkip;

    private Button skipButton;
    private Handler countHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
         * 这里不需要这个设置也能够达到全屏显示,并且没有闪屏现象的效果
         *
         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
         View decorView = getWindow().getDecorView();
         int option = View.SYSTEM_UI_FLAG_FULLSCREEN;
         decorView.setSystemUiVisibility(option);
         }
         *
         */
        setContentView(R.layout.activity_welcome);
        initVariable();
        toMain();
    }

    public void toMain() {
        checkRunTimePermission();
    }

    private void initVariable() {
        countTime = VariableSet.NUM_THREE;
        isSkip = false;

        skipButton = findViewById(R.id.free_welcome_skip);
        skipButton.setText(getString(R.string.free_skip, countTime));
        skipButton.setOnClickListener(this);

        // mToast = Toast.makeText(getApplicationContext(), "", Toast.LENGTH_SHORT);
    }

    private Runnable countTimeRun = new Runnable() {
        @Override
        public void run() {
            if (isSkip) {
                startMain();
            } else {
                countTime--;
                if (countTime > VariableSet.NUM_ZERO) {
                    skipButton.setText(getString(R.string.free_skip, countTime));
                    countHandler.postDelayed(countTimeRun, VariableSet.JUMP_DELAY_TIME);
                } else if (countTime == VariableSet.NUM_ZERO) {
                    // 此处可以直接选择立即启动新 Activity, 但是继续发送一个消息, 可延长部分时间显示, 即显示 "0" 时, 不立即跳转, 减少 APP 转变的突兀性
                    skipButton.setClickable(false);
                    if (isSkip) {
                        startMain();
                    } else {
                        skipButton.setText(getString(R.string.free_skip, countTime));
                        // 设置为0时, 应该立即跳转, 但是看不到0就会跳转, 所以这里进行延迟一定时间跳转
                        countHandler.postDelayed(countTimeRun, VariableSet.JUMP_DELAY_TIME / 4);
                    }
                } else {
                    startMain();
                }
            }
        }
    };

    private void startMain() {
        countHandler.removeCallbacks(countTimeRun);
        Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
        startActivity(intent);
        WelcomeActivity.this.finish();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.free_welcome_skip:
                isSkip = true;
                countHandler.postDelayed(countTimeRun, VariableSet.JUMP_DELAY_TIME);
                break;
            default:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void checkRunTimePermission() {
        int granted = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (int i = 0; i < PMS_LENGTH; i++) {
                if (ContextCompat.checkSelfPermission(this, permissions[i]) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(WelcomeActivity.this, permissions, VariableSet.PERMISSION_GRANTED);
                } else {
                    granted++;
                }
            }
            if (granted == 2) {
                // CommonMethod.makeShortToast(mToast, "checkRunTimePermission Run!");
                countHandler.postDelayed(countTimeRun, VariableSet.JUMP_DELAY_TIME);
            }
        } else countHandler.postDelayed(countTimeRun, VariableSet.JUMP_DELAY_TIME);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length <= 0) {
            return;
        }
        if (requestCode == 1) {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    new AlertDialog.Builder(WelcomeActivity.this).setTitle("Check Permission Result")
                            // 设置对话框标题
                            .setMessage("GET PERMISSION ERROR! APP WILL EXIT!")
                            .setPositiveButton("EXIT", (dialog, which) -> {
                                // 确定按钮的响应事件
                                WelcomeActivity.this.finish();
                            }).show();
                    return;
                }
            }
            // CommonMethod.makeShortToast(mToast, "onRequestPermissionsResult Run!");
            countHandler.postDelayed(countTimeRun, VariableSet.JUMP_DELAY_TIME);
        }
    }
}
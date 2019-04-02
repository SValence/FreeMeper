package com.valence.freemeper.cusview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;

import com.valence.freemeper.R;

import timber.log.Timber;

public class CameraFlashTextureView extends FrameLayout {

    private TextureView textureView;
    private View backView;
    private View backViewFull;

    public CameraFlashTextureView(Context context) {
        super(context);
        init(context);
    }

    public CameraFlashTextureView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraFlashTextureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater == null) {
            Timber.e("LayoutInflater is Null!!!");
            return;
        }
        inflater.inflate(R.layout.layout_camera_textureview, this);
        textureView = findViewById(R.id.free_camera_texture);
        backView = findViewById(R.id.free_camera_back);
        backViewFull = findViewById(R.id.free_camera_back_full);
    }

    public TextureView getTextureView() {
        return textureView;
    }

    public View getBackView() {
        return backView;
    }

    public View getBackViewFull() {
        return backViewFull;
    }
}

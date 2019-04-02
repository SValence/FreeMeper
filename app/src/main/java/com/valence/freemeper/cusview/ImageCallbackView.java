package com.valence.freemeper.cusview;

import android.content.Context;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;

public class ImageCallbackView extends AppCompatImageView {

    private VisibilityChangeListener changeListener;
    private int lastVisibility;

    public ImageCallbackView(Context context) {
        super(context);
        lastVisibility = getVisibility();
    }

    public ImageCallbackView(Context context, AttributeSet attrs) {
        super(context, attrs);
        lastVisibility = getVisibility();
    }

    public ImageCallbackView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        lastVisibility = getVisibility();
    }

    public void setVisibilityChangeListener(VisibilityChangeListener changeListener) {
        this.changeListener = changeListener;
    }

    @Override
    public void setVisibility(int visibility) {
        if (changeListener != null && lastVisibility != visibility) {
            changeListener.onVisibilityChange(lastVisibility, visibility);
        }
        lastVisibility = visibility;
        super.setVisibility(visibility);
    }

    public interface VisibilityChangeListener {
        void onVisibilityChange(int oldVisibility, int newVisibility);
    }
}

package com.valence.freemeper.cusview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;

import com.valence.freemeper.R;

import static com.valence.freemeper.tool.CommonMethod.setImageMargin;
import static com.valence.freemeper.tool.VariableSet.DEFAULT_COLOR;
import static com.valence.freemeper.tool.VariableSet.DEFAULT_SIZE_DIFF;
import static com.valence.freemeper.tool.VariableSet.DEFAULT_WIDTH;
import static com.valence.freemeper.tool.VariableSet.OPAQUE_ALPHA;
import static com.valence.freemeper.tool.VariableSet.START_ALPHA;

/**
 * CircleImage: 圆形ImageView, 支持边框
 *
 * @author Valence
 * @version 1.1
 * @since 2018/08/07
 */
public class CircleImage1 extends AppCompatImageView {

    private static final String TAG = "CircleTextImage1";
    private Bitmap mBitmap;
    private ColorFilter mColorFilter;
    private Paint mBitmapPaint;
    private Paint mBorderPaint;
    private RectF mBorderRect;
    private RectF mBitmapRect;
    private int mBorderWidth;
    private int mBorderColor;
    private float mBorderRadius;
    private float mBitmapRadius;
    private Drawable backDrawable;
    private BitmapShader mBitmapShader;
    private boolean initialized;
    private boolean sizeMeasured;
    private Matrix mShaderMatrix;
    private int mBitmapWidth;
    private int mBitmapHeight;
    private float imgCenterX;
    private float imgCenterY;
    private AlphaAnimation alphaAnimation;
    private int textSize;
    private MarginBean marginBean;

    public CircleImage1(Context context) {
        super(context);
        init();
    }

    public CircleImage1(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CircleImage1(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (attrs != null) {
            TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.CircleImage1, defStyleAttr, 0);
            mBorderWidth = array.getDimensionPixelSize(R.styleable.CircleImage1_border_width_c1, DEFAULT_WIDTH);
            mBorderColor = array.getColor(R.styleable.CircleImage1_border_color_c1, DEFAULT_COLOR);
            array.recycle();
        }
        init();
    }

    public void setmBorderWidth(int mBorderWidth) {
        this.mBorderWidth = mBorderWidth;
        setImagePrePare();
    }

    public void setmBorderColor(int mBorderColor) {
        this.mBorderColor = mBorderColor;
        setImagePrePare();
    }

    public void setMarginBean(MarginBean marginBean) {
        this.marginBean = marginBean;
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(bm);
        mBitmap = bm;

    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);
        mBitmap = getImageBitmap(drawable);
        setImagePrePare();
    }

    @Override
    public void setImageResource(@DrawableRes int resId) {
        super.setImageResource(resId);
        mBitmap = getImageBitmap(getDrawable());
        setImagePrePare();
    }

    @Override
    public void setForeground(Drawable foreground) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Log.e(TAG, "Android Version Lower Than 23, Can Not Set Foreground!");
            return;
        }
        super.setForeground(foreground);
        setImagePrePare();
    }

    @Override
    public void setImageURI(Uri uri) {
        super.setImageURI(uri);
        mBitmap = getImageBitmap(getDrawable());
        setImagePrePare();
    }

    @Override
    public void setBackground(Drawable background) {
        super.setBackground(background);
        backDrawable = background;
        setImagePrePare();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        sizeMeasured = true;
        setImagePrePare();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        ViewGroup mViewGroup = (ViewGroup) getParent();
        int height = mViewGroup.getLayoutParams().height;
        int width = mViewGroup.getLayoutParams().width;
        Log.e(TAG, "Parent height:" + height + "----width:" + width);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int maxSize;
        if (height <= 0 && width <= 0) {
            maxSize = getMaxSize(heightSize, widthSize);
        } else {
            maxSize = getMaxSize(height, width);
        }
        widthMeasureSpec = MeasureSpec.makeMeasureSpec(maxSize, widthMode);
        heightMeasureSpec = MeasureSpec.makeMeasureSpec(maxSize, heightMode);
        setImageMargin(false, this, marginBean, maxSize);
//        setMeasuredDimension(maxSize, maxSize);

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private int getMaxSize(int height, int width) {
        int size;
        if (height == 0 || width == 0) {
            int max = Math.max(height, width);
            if (max == 0) {
                Log.e(TAG, "Height、Width max is 0");
                return 0;
            }
            size = max;
        } else size = Math.min(height, width);
        int diff = Math.abs(height - width);
        int size1 = size;
        // 在图片下方留出 text_ts + DEFAULT_SIZE_DIFF 的空间来显示文字
        if (size == height) {
            size = size - textSize - DEFAULT_SIZE_DIFF;
        } else if (diff <= textSize + DEFAULT_SIZE_DIFF)
            size = size - textSize - DEFAULT_SIZE_DIFF + diff >= DEFAULT_SIZE_DIFF ? 0 : diff;
        if (size <= 0) return size1;
        return size;
    }

    @Override
    public void setScaleType(ScaleType scaleType) {
        super.setScaleType(ScaleType.CENTER_CROP);
        setImagePrePare();
    }

    public void setTextSize(int textSize) {
        this.textSize = textSize;
    }

    private void init() {
        mBitmapPaint = new Paint();
        mBitmapPaint.setAntiAlias(true);
        mBorderPaint = new Paint();
        mBorderPaint.setAntiAlias(true);
        mBorderRect = new RectF();
        mBitmapRect = new RectF();
        mShaderMatrix = new Matrix();
        initialized = true;
        textSize = 0;

        alphaAnimation = new AlphaAnimation(START_ALPHA, OPAQUE_ALPHA);
        alphaAnimation.setDuration(200);
        //        alphaAnimation.setAnimationListener(new Animation.AnimationListener() {
        //            @Override
        //            public void onAnimationStart(Animation animation) {
        //                //setAlpha(0f);
        //            }
        //
        //            @Override
        //            public void onAnimationEnd(Animation animation) {
        //                // 不应该在这里设置, 应该在动画开始时就把透明度设置为不透明
        //                 setAlpha(OPAQUE_ALPHA);
        //            }
        //
        //            @Override
        //            public void onAnimationRepeat(Animation animation) {
        //
        //            }
        //        });
    }

    private Bitmap getImageBitmap(Drawable drawable) {
        if (drawable == null) {
            Log.e(TAG, "Drawable is Null");
            return null;
        }
        Bitmap bitmap;
        if (drawable instanceof ColorDrawable) {
            bitmap = Bitmap.createBitmap(2, 2,
                    drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565);
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(),
                    drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565);
        }
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    private void setImagePrePare() {
        if (!initialized || mBitmap == null || !sizeMeasured) return;

        mBorderPaint.setStyle(Paint.Style.STROKE);
        mBorderPaint.setColor(mBorderColor);
        mBorderPaint.setStrokeWidth(mBorderWidth);

        mBitmapHeight = mBitmap.getHeight();
        mBitmapWidth = mBitmap.getWidth();

        int size = Math.min(getWidth(), getHeight());
        mBorderRect.set(0, 0, size, size);
        mBorderRadius = Math.min((mBorderRect.width() - mBorderWidth) / 2, (mBorderRect.height() - mBorderWidth) / 2);
        mBitmapRect.set(mBorderRect);
        mBitmapRadius = Math.min(mBitmapRect.width() / 2, mBitmapRect.height() / 2);
        imgCenterX = mBorderRadius + mBorderWidth / 2.0f;
        imgCenterY = mBorderRadius + mBorderWidth / 2.0f;

        mBitmapShader = new BitmapShader(mBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        mBitmapPaint.setShader(mBitmapShader);

        setShaderMatrix();
        invalidate();
    }

    private void setShaderMatrix() {
        float scale;
        float dx = 0;
        float dy = 0;
        mShaderMatrix.set(null);
        if (mBitmapWidth * mBitmapRect.height() > mBitmapRect.width() * mBitmapHeight) {
            scale = mBitmapRect.height() / (float) mBitmapHeight;
            dx = (mBitmapRect.width() - mBitmapWidth * scale) * 0.5f;
        } else {
            scale = mBitmapRect.width() / (float) mBitmapWidth;
            dy = (mBitmapRect.height() - mBitmapHeight * scale) * 0.5f;
        }
        mShaderMatrix.setScale(scale, scale);
        mShaderMatrix.postTranslate((int) (dx + 0.5f) + mBitmapRect.left, (int) (dy + 0.5f) + mBitmapRect.top);
        mBitmapShader.setLocalMatrix(mShaderMatrix);
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        if (cf != mColorFilter) {
            mColorFilter = cf;
            mBitmapPaint.setColorFilter(mColorFilter);
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawCircle(imgCenterX, imgCenterY, mBitmapRadius, mBitmapPaint);
        if (mBorderWidth != 0) {
            canvas.drawCircle(imgCenterX, imgCenterY, mBorderRadius, mBorderPaint);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) return super.onTouchEvent(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
                startAnimation(alphaAnimation);
                setAlpha(OPAQUE_ALPHA);
                break;
            case MotionEvent.ACTION_DOWN:
                clearAnimation();
                setAlpha(START_ALPHA);
                break;
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_CANCEL:
                startAnimation(alphaAnimation);
                return true;
            default:
                break;
        }
        return super.onTouchEvent(event);
    }
}

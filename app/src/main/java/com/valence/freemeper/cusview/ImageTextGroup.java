package com.valence.freemeper.cusview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.valence.freemeper.R;

import static com.valence.freemeper.tool.CommonMethod.dip2px;
import static com.valence.freemeper.tool.CommonMethod.isMarginValid;
import static com.valence.freemeper.tool.CommonMethod.px2dip;
import static com.valence.freemeper.tool.VariableSet.DEFAULT_COLOR;
import static com.valence.freemeper.tool.VariableSet.DEFAULT_WIDTH;
import static com.valence.freemeper.tool.VariableSet.GRAVITY_BOTTOM;
import static com.valence.freemeper.tool.VariableSet.GRAVITY_CENTER;
import static com.valence.freemeper.tool.VariableSet.GRAVITY_END;
import static com.valence.freemeper.tool.VariableSet.GRAVITY_START;
import static com.valence.freemeper.tool.VariableSet.GRAVITY_TOP;

public class ImageTextGroup extends LinearLayout {

    private static final String TAG = "ImageTextGroup";
    private CircleImage image;
    private TextView text;

    private int image_bw;
    private int image_bc;
    private int image_size;
    private int image_gv;
    private int text_ts;
    private int text_tc;
    private int text_tg;
    private int text_ellipsize;
    private int text_maxLines;
    private int image_margin;
    private int image_marStart;
    private int image_marEnd;
    private Drawable src;
    private Drawable back;
    private Drawable front;
    private String text_t;
    private boolean clickable;
    private TextUtils.TruncateAt ell;
    private MarginBean marginBean;

    public ImageTextGroup(Context context) {
        super(context);
        init(context);
    }

    public ImageTextGroup(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ImageTextGroup(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        getAttributes(context, attrs, defStyleAttr);
        init(context);
        setAttributes();
    }

    private void getAttributes(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.ImageTextGroup, defStyleAttr, 0);
        image_bw = array.getDimensionPixelSize(R.styleable.ImageTextGroup_group_bw, DEFAULT_WIDTH);
        image_bw = image_bw < 0 ? 0 : image_bw;
        image_bc = array.getColor(R.styleable.ImageTextGroup_group_bc, DEFAULT_COLOR);
        image_size = array.getInteger(R.styleable.ImageTextGroup_group_img_size, 0);
        if (image_size > 0) image_size = dip2px(context, image_size);
        image_margin = array.getDimensionPixelSize(R.styleable.ImageTextGroup_group_img_margin, 0);
        image_margin = image_margin < 0 ? 0 : image_margin;
        image_marStart = array.getDimensionPixelSize(R.styleable.ImageTextGroup_group_img_marStart, 0);
        image_marStart = image_marStart < 0 ? 0 : image_marStart;
        image_marEnd = array.getDimensionPixelSize(R.styleable.ImageTextGroup_group_img_marEnd, 0);
        image_marEnd = image_marEnd < 0 ? 0 : image_marEnd;
        int image_marTop = array.getDimensionPixelSize(R.styleable.ImageTextGroup_group_img_marTop, 0);
        image_marTop = image_marTop < 0 ? 0 : image_marTop;
        int image_marBottom = array.getDimensionPixelSize(R.styleable.ImageTextGroup_group_img_marBottom, 0);
        image_marBottom = image_marBottom < 0 ? 0 : image_marBottom;
        marginBean = new MarginBean(image_margin, image_marStart, image_marTop, image_marEnd, image_marBottom);

        src = array.getDrawable(R.styleable.ImageTextGroup_group_src);
        back = array.getDrawable(R.styleable.ImageTextGroup_group_bg);
        front = array.getDrawable(R.styleable.ImageTextGroup_group_fg);
        text_ts = array.getDimensionPixelSize(R.styleable.ImageTextGroup_group_ts, DEFAULT_WIDTH);
        text_tc = array.getColor(R.styleable.ImageTextGroup_group_tc, DEFAULT_COLOR);
        text_t = array.getString(R.styleable.ImageTextGroup_group_text);
        text_t = text_t == null ? "" : text_t;
        clickable = array.getBoolean(R.styleable.ImageTextGroup_group_clickable, false);
        String gravity = array.getString(R.styleable.ImageTextGroup_group_tg);
        if (TextUtils.equals(gravity, GRAVITY_START)) text_tg = Gravity.START;
        else if (TextUtils.equals(gravity, GRAVITY_END)) text_tg = Gravity.END;
        else if (TextUtils.equals(gravity, GRAVITY_BOTTOM)) text_tg = Gravity.BOTTOM;
        else if (TextUtils.equals(gravity, GRAVITY_TOP)) text_tg = Gravity.TOP;
        else if (TextUtils.isEmpty(gravity) || TextUtils.equals(gravity, GRAVITY_CENTER))
            text_tg = Gravity.CENTER;
        else text_tg = Gravity.CENTER;

        int img_gv = array.getInteger(R.styleable.ImageTextGroup_group_img_gv, -1);
        if (img_gv == 0) image_gv = Gravity.CENTER_HORIZONTAL;
        else if (img_gv == 1) image_gv = Gravity.END;
        else if (img_gv == -1) image_gv = Gravity.START;
        text_ellipsize = array.getInteger(R.styleable.ImageTextGroup_group_ellipsize, 0);
        ell = getTruncateAt(text_ellipsize);
        text_maxLines = array.getInteger(R.styleable.ImageTextGroup_group_maxLines, 2);
        text_maxLines = text_maxLines <= 0 ? 2 : text_maxLines;
        array.recycle();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
//        Log.e(TAG, "width:" + w + "----height" + h);
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private void init(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater == null) {
            Log.e(TAG, "LayoutInflater is Null!!!");
            return;
        }
        inflater.inflate(R.layout.image_text_layout, this);
        text = findViewById(R.id.circleImageText);
        image = findViewById(R.id.circleImageIcon);
    }

    private void setAttributes() {
        text.setText(text_t);
        text.setTextSize(px2dip(getContext(), text_ts));
        text.setTextColor(text_tc);
        text.setGravity(text_tg);
        text.setMaxLines(text_maxLines);
        text.setEllipsize(ell);

        // 要先设置图片控件的宽度和高度
        setImageSize();
        //setImageMargin(false, image, marginBean, image_size);
        image.setMarginBean(marginBean);

        image.setmBorderColor(image_bc);
        image.setmBorderWidth(image_bw);
        image.setImageDrawable(src);
        image.setBackground(back);
        image.setForeground(front);
        image.setClickable(clickable);
        image.setFocusable(clickable);

        setImageGravity(image_gv);
    }

    public void setImageGravity(int gravity) {
        LayoutParams param = (LayoutParams) image.getLayoutParams();
        param.gravity = gravity;
        image.setLayoutParams(param);
    }

    private void setImageSize() {
        image.setTextSize(text_ts);
        ViewGroup.LayoutParams param = image.getLayoutParams();
        if (isMarginValid(marginBean, image_size)) {
            int size = image_size - (image_margin == 0 ? (image_marStart - image_marEnd) : 2 * image_margin);
            param.width = size;
            param.height = size;
        } else {
            param.width = image_size;
            param.height = image_size;
        }
        image.setLayoutParams(param);
    }

    @Override
    public void setOnClickListener(@Nullable OnClickListener l) {
        image.setOnClickListener(l);
    }

    public void setImage_bw(int image_bw) {
        this.image_bw = image_bw;
        image.setmBorderWidth(dip2px(getContext(), image_bw));
    }

    public void setImage_bc(int image_bc) {
        this.image_bc = image_bc;
        image.setmBorderColor(getResources().getColor(image_bc));
    }

    public void setImage_size(int image_size) {
        this.image_size = image_size;
        setImageSize();
    }

    public void setImage_gv(int image_gv) {
        this.image_gv = image_gv;
        setImageGravity(image_gv);
    }

    public void setText_ts(int text_ts) {
        this.text_ts = text_ts;
        text.setTextSize(text_ts);
    }

    public void setText_tc(int text_tc) {
        this.text_tc = text_tc;
        text.setTextColor(getResources().getColor(text_tc));
    }

    public void setText_tg(int text_tg) {
        this.text_tg = text_tg;
        text.setGravity(text_tg);
    }

    public void setImageSrc(Drawable src) {
        this.src = src;
        image.setImageDrawable(src);
    }

    public void setText_ellipsize(int text_ellipsize) {
        this.text_ellipsize = text_ellipsize;
        text.setEllipsize(getTruncateAt(text_ellipsize));
    }

    public void setText_maxLines(int text_maxLines) {
        this.text_maxLines = text_maxLines;
        text.setMaxLines(text_maxLines);
    }

    public void setBack(Drawable back) {
        this.back = back;
        image.setBackground(back);
    }

    public void setFront(Drawable front) {
        this.front = front;
        image.setForeground(front);
    }

    public void setText_t(String text_t) {
        this.text_t = text_t;
        text.setText(text_t);
    }

    public void setImageClickable(boolean clickable) {
        this.clickable = clickable;
        image.setClickable(clickable);
    }

    public int getImage_bw() {
        return image_bw;
    }

    public int getImage_bc() {
        return image_bc;
    }

    public int getImage_gv() {
        return image_gv;
    }

    public int getText_ts() {
        return text_ts;
    }

    public int getText_tc() {
        return text_tc;
    }

    public int getText_tg() {
        return text_tg;
    }

    public int getImage_size() {
        return image_size;
    }

    public Drawable getImageSrc() {
        return src;
    }

    public Drawable getBack() {
        return back;
    }

    public String getText_t() {
        return text_t;
    }

    public boolean isImageClickable() {
        // return image.isClickable();
        return clickable;
    }

    public TextUtils.TruncateAt getText_ellipsize() {
        return getTruncateAt(text_ellipsize);
    }

    public int getText_maxLines() {
        return text_maxLines;
    }

    @NonNull
    private TextUtils.TruncateAt getTruncateAt(int text_ellipsize) {
        TextUtils.TruncateAt ell;
        switch (text_ellipsize) {
            case -1:
                ell = TextUtils.TruncateAt.START;
                break;
            case 0:
                ell = TextUtils.TruncateAt.END;
                break;
            case 1:
                ell = TextUtils.TruncateAt.MIDDLE;
                break;
            default:
                ell = TextUtils.TruncateAt.END;
                break;
        }
        return ell;
    }
}

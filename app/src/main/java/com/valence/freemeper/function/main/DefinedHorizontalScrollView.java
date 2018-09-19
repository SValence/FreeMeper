package com.valence.freemeper.function.main;

import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

import com.nineoldandroids.view.ViewHelper;
import com.valence.freemeper.cusview.ChildClickableLinearLayout;
import com.valence.freemeper.tool.VariableSet;

import static com.valence.freemeper.tool.VariableSet.STATUS_CLOSE;
import static com.valence.freemeper.tool.VariableSet.STATUS_CLOSING;
import static com.valence.freemeper.tool.VariableSet.STATUS_OPEN;
import static com.valence.freemeper.tool.VariableSet.STATUS_OPENING;

/**
 * DefinedHorizontalScrollView: 自定义水平滚动View
 *
 * @author Valence
 * @version 1.4
 * @since 2017/09/13
 */
public class DefinedHorizontalScrollView extends HorizontalScrollView {

    private final static String TAG = "HorizontalScrollView";

    /**
     * scrollStatus: DefinedHorizontalScrollView 所处的状态:<br/>
     * {@link com.valence.freemeper.tool.VariableSet#STATUS_CLOSE} 1: 关闭<br/>
     * {@link com.valence.freemeper.tool.VariableSet#STATUS_OPENING} 2: 正在开启<br/>
     * {@link com.valence.freemeper.tool.VariableSet#STATUS_OPEN} 3: 开启<br/>
     * {@link com.valence.freemeper.tool.VariableSet#STATUS_CLOSING} 4: 正在关闭
     */
    private int scrollStatus;

    /**
     * scrolling: 是否正在滑动标记<br/>
     * true: 正在滑动<br/>
     * false: 滑动结束
     */
    private boolean scrolling;

    /**
     * isMove: 触摸屏幕过程中手指是否移动<br/>
     * true: 移动<br/>
     * false: 未移动
     */
    private boolean isMove;

    /**
     * isSet: View是否已经进行初始化操作的标记<br/>
     * true: 已初始化<br/>
     * false: 未初始化
     */
    private boolean isSet;

    /**
     * isInitClose: 初始显示时是否已经关闭View<br/>
     * true: 已初始化<br/>
     * false: 未初始化
     */
    private boolean isInitClose;

    /**
     * mContext: 上下文标记
     */
    private Context mContext;

    /**
     * point_down: OnTouch()监听事件中所使用得变量, 屏幕Touch被按下位置所处的坐标
     */
    private ViewPoint point_down;

    /**
     * point_move: OnTouch()监听事件中所使用得变量, 屏幕Touch过程中最左或者最右所处位置的坐标
     */
    private ViewPoint point_move;

    /**
     * point_up: OnTouch()监听事件中所使用得变量, 屏幕Touch完成后, 抬起位置所处的坐标
     */
    private ViewPoint point_up;

    /**
     * mMenuWidth: 左侧菜单宽度
     */
    private int mMenuWidth;

    /**
     * mScrollWidth: 手势滑动的有效距离
     */
    private int mScrollWidth;

    /**
     * screenWidth: 屏幕宽度
     */
    private int screenWidth;

    /**
     * screenHeight: 屏幕高度
     */
    private int screenHeight;

    /**
     * metrics: 尺寸获取工具<br/>
     * ps: 对此没有深入思考，暂且这么叫
     */
    private DisplayMetrics metrics;

    /**
     * nMenuView: 左侧菜单View
     */
    private LinearLayout mMenuView;

    /**
     * main: 主界面View
     */
    private ChildClickableLinearLayout main;

    public DefinedHorizontalScrollView(Context context) {
        super(context);

        // 初始化触点位置的变量, 由于变量不是很多，就不在单独集成在一个方法中了
        init(context);
    }

    public DefinedHorizontalScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // 初始化触点位置的变量
        init(context);
    }

    private void init(Context context) {
        point_down = new ViewPoint();
        point_move = new ViewPoint();
        point_up = new ViewPoint();

        // 获取尺寸工具实例
        metrics = new DisplayMetrics();
        isSet = false;
        this.mContext = context;
    }

    public DefinedHorizontalScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        // 初始化触点位置的变量
        init(context);
    }

    /**
     * onMeasure: 重写方法, 对自定义View进行对应的测量和初始化等等工作
     *
     * @param widthMeasureSpec  widthMeasureSpec
     * @param heightMeasureSpec heightMeasureSpec
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // 获取 WindowManager 实例, 得到屏幕的操作权
        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);

        // 给 metrics 赋值
        if (wm == null)
            return;
        wm.getDefaultDisplay().getMetrics(metrics);

        // 设备屏幕的宽度,高度变量
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
        mMenuWidth = (int) (screenWidth * 0.8);
        mScrollWidth = mMenuWidth / 3;

        // 测试时使用的打印语句
        // Log.e("width", mMenuWidth + "");

        /*
         * 初始化菜单View和主界面View的宽度, 由于在系统调用到onMeasure()时,
         * 才能对各个View进行操作, 故在此处进行初始化
         */
        if (!isSet) {
            // 根据View在布局文件中的顺序可在此改变getChildAt(index)的index参数
            LinearLayout wrapper = (LinearLayout) this.getChildAt(0);
            mMenuView = (LinearLayout) wrapper.getChildAt(0);
            main = (ChildClickableLinearLayout) wrapper.getChildAt(1);

            // 设置宽度
            mMenuView.getLayoutParams().width = mMenuWidth;
            main.getLayoutParams().width = screenWidth;

            // 这里只需要进行一次初始化, 而onMeasure()会调用多次, 所以需要设置判断条件
            isSet = true;
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (changed) {
            if (!isInitClose) {
                // closeView();
                // 这里直接显示在主界面, 不慢慢滑动
                scrollTo(mMenuWidth, 0);
                isInitClose = true;
                scrollStatus = STATUS_CLOSE;
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                isMove = false;
                point_down.setCoo_x((int) ev.getX());
                point_down.setCoo_y((int) ev.getY());
                point_down.setCoo_time(System.currentTimeMillis());
                break;
            case MotionEvent.ACTION_MOVE:
                // 只有初次或者 point_move 被主动 clear 后才会执行的语句
                if (point_move.getCoo_x() == 0) {
                    point_move.setCoo_x((int) ev.getX());
                    point_move.setCoo_y((int) ev.getY());
                    point_move.setCoo_time(System.currentTimeMillis());
                } else {
                    // 运行到此处说明 point_move 已经存有值
                    if ((point_move.getCoo_x() <= point_down.getCoo_x()) && (point_move.getCoo_x() > ev.getX())) {
                        // 本条件下为: 只记录滑动手势中最左侧的位置坐标点
                        // 原因: 当第一次运行到此处时, 说明滑动手势在 down 之后先往左侧滑动, 且以后 point_move 只记录更左侧位置坐标点
                        point_move.setCoo_x((int) ev.getX());
                        point_move.setCoo_y((int) ev.getY());
                        point_move.setCoo_time(System.currentTimeMillis());
                        //break;
                        // 此处必须加 break      (错误注释, 详细看下方解释 1 )
                        /*
                         * break 标号 2
                         */
                    }
                    if ((point_move.getCoo_x() >= point_down.getCoo_x()) && (point_move.getCoo_x() < ev.getX())) {
                        // 本条件下为: 只记录滑动手势中最右侧的位置坐标点
                        // 原因: 当第一次运行到此处时, 说明滑动手势在 down 之后先往右侧滑动, 且以后 point_move 只记录更右侧位置坐标点
                        point_move.setCoo_x((int) ev.getX());
                        point_move.setCoo_y((int) ev.getY());
                        point_move.setCoo_time(System.currentTimeMillis());
                    }
                    if (!isMove) {
                        isMove = getDistance(point_down, point_move) > VariableSet.MOVE_MAX_VALID_LEN;
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                point_up.setCoo_x((int) ev.getX());
                point_up.setCoo_y((int) ev.getY());
                point_up.setCoo_time(System.currentTimeMillis());
                // Touch 过程是点击屏幕, 没有滑动
                if (point_down.getCoo_x() == point_up.getCoo_x()) {
                    // 打开之后点击屏幕
                    if (scrollStatus == STATUS_OPEN) {
                        // 点击位置在菜单 View 之外
                        if (point_up.getCoo_x() >= mMenuWidth) {
                            closeView();
                            return true;
                        }
                    } else if (scrollStatus == STATUS_OPENING) {
                        openView();
                        return true;
                    } else if (scrollStatus == STATUS_CLOSING) {
                        closeView();
                        return true;
                    }
                    // scrollStatus=1 (已关闭) 和处于其他状态的点击情况不作处理, 保持原状态即可
                } else {
                    // 手指点击屏幕有移动
                    // 小范围移动, 即手抖移动, 此时为无效移动, 当作未移动处理, 这种处理是为了防止正在开启或者关闭的滑动状态下,
                    // 手指突然点击屏幕并造成小幅度移动, 致使算法认为是不符合开启或者关闭状态, 进而中断已有操作
                    if (!isMove) {
                        if (scrollStatus == STATUS_OPENING || scrollStatus == STATUS_OPEN) {
                            openView();
                        } else if (scrollStatus == STATUS_CLOSING || scrollStatus == STATUS_CLOSE) {
                            closeView();
                        } else {
                            Log.e(TAG, "ILLEGAL STATUS: " + scrollStatus);
                        }
                        return true;
                    }
                    // 这时已经处于开启或者关闭状态, 对应于手指抬起之前已经滑动到最边缘的位置, 那么就不需要在关闭或者开启, 否则会造成滑动状态异常
                    // 前提是主界面已经不处于滑动状态了, 即已经滑动到最左或者最右的位置
                    if ((scrollStatus == STATUS_OPEN || scrollStatus == STATUS_CLOSE) && !scrolling) {
                        return true;
                    }
                    // 不是小范围移动, 那么就要判断
                    if (openOrClose(point_down, point_move, point_up, mScrollWidth, scrollStatus)) {
                        openView();
                    } else {
                        closeView();
                    }
                    return true;
                }
            default:
                break;
        }
        return super.onTouchEvent(ev);
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);

        // 测试时使用的打印语句
        // Log.e("Scroll", l + "," + t);
        if (l != 0 && l != mMenuWidth) {
            scrolling = true;
        }
        switch (scrollStatus) {
            case STATUS_OPENING:
                // 正在开启
                if (l == 0) {
                    scrolling = false;
                    scrollStatus = STATUS_OPEN;
                    main.setChildClickable(false);
                }
                break;
            case STATUS_CLOSING:
                // 正在关闭
                if (l == mMenuWidth) {
                    scrolling = false;
                    scrollStatus = STATUS_CLOSE;
                    main.setChildClickable(true);
                }
                break;
            default:
                if (scrollStatus != STATUS_CLOSE && scrollStatus != STATUS_OPEN) {
                    Log.e(TAG, "ILLEGAL STATUS: " + scrollStatus);
                    break;
                }
                // 普通状态, 当滑动到尽头时也要把滑动状态和能否点击置为对应的状态
                if (l == 0) {
                    scrolling = false;
                    scrollStatus = STATUS_OPEN;
                    main.setChildClickable(false);
                }
                if (l == mMenuWidth) {
                    scrolling = false;
                    scrollStatus = STATUS_CLOSE;
                    main.setChildClickable(true);
                }
                break;
        }
        ViewHelper.setTranslationX(mMenuView, l * 0.4f);
    }

    /**
     * openOrClose(ViewPoint down, ViewPoint move, ViewPoint up, int condition, boolean open):<br/>
     * 判断手势是否符合打开或者关闭View的条件时使用的方法
     *
     * @param down      ViewPoint 位置坐标点
     * @param move      ViewPoint 位置坐标点
     * @param up        ViewPoint 位置坐标点
     * @param condition 判断依据条件
     * @param status    View 状态标记
     * @return boolean 类型数据 true: 打开, false: 关闭
     */
    public boolean openOrClose(ViewPoint down, ViewPoint move, ViewPoint up, int condition, int status) {

        // 关闭或者长在开启的场合
        if (status == STATUS_CLOSE || status == STATUS_OPENING) {
            // 先由左向右滑动
            if (move.getCoo_x() > down.getCoo_x()) { //这种情况下，整个滑动过程均有效，且有效点为：down,up
                // 长度有效, 从左往右滑动, 那么 up.x > down.x, 所以是 up - down

                // 这里是滑动长度为屏幕的百分之80以上(即菜单宽度), 那么不管什么情况都打关闭
                if ((up.getCoo_x() - down.getCoo_x()) >= mMenuWidth) {
                    return true;
                }
                if ((up.getCoo_x() - down.getCoo_x()) >= condition) {
                    // 滑动角度有效
                    // 符合打开条件
                    return angleFit(down, up) || (up.getCoo_x() - down.getCoo_x()) >= (condition * 3) && (angleFitMax(down, up));
                } else {//长度不合适，此时为另一种快速滑动短距离打开方式
                    // 角度有效
                    // 速度有效
                    return angleFit(down, up) && speed(down, up) >= VariableSet.SPEED_LIMIT;
                }
            } else {// 这种情况是在关闭状态下，先由右往左滑动，此时，往左滑动的距离是无效的，所以有效距离为，最左位置 move 和抬起位置 up
                // 长度有效
                if ((up.getCoo_x() - move.getCoo_x()) >= condition) {
                    // 滑动角度有效
                    // 符合打开条件
                    return angleFit(move, up) || (up.getCoo_x() - move.getCoo_x()) >= (condition * 3) && (angleFitMax(move, up));
                } else {//长度不合适，此时为另一种快速滑动短距离打开方式
                    // 角度有效
                    // 速度有效
                    return angleFit(move, up) && speed(move, up) >= VariableSet.SPEED_LIMIT;
                }
            }
        } else if (status == STATUS_OPEN || status == STATUS_CLOSING) {//此处为开启状态的场合或者正在关闭的场合
            // 先由右往左滑动
            if (move.getCoo_x() < down.getCoo_x()) {// 这种情况下，整个滑动过程均有效，且有效点为：down,up
                // 长度有效, 从右往左滑动, 那么 up.x < down.x, 所以是 down - up

                // 这里是滑动长度为屏幕的百分之80以上(即菜单宽度), 那么不管什么情况都打关闭
                if ((down.getCoo_x() - up.getCoo_x()) >= mMenuWidth) {
                    return false;
                }
                if ((down.getCoo_x() - up.getCoo_x()) >= condition) {
                    // 滑动角度有效
                    // 符合关闭条件
                    return !angleFit(down, up) && ((up.getCoo_x() - down.getCoo_x()) < (condition * 3) || (!angleFitMax(down, up)));
                } else {//长度不合适，此时为另一种快速滑动短距离打开方式
                    // 角度有效
                    // 速度有效
                    return !angleFit(down, up) || !(speed(down, up) >= VariableSet.SPEED_LIMIT);
                }
            } else {// 这种情况是在打开状态下，先由左往右滑动，此时，往右滑动的距离是无效的，所以有效距离为，最右位置 move 和抬起位置 up
                // 长度有效
                if ((move.getCoo_x() - up.getCoo_x()) >= condition) {
                    // 滑动角度有效
                    // 符合关闭条件
                    return !angleFit(move, up) && ((up.getCoo_x() - move.getCoo_x()) < (condition * 3) || (!angleFitMax(move, up)));
                } else {//长度不合适，此时为另一种快速滑动短距离打开方式
                    // 角度有效
                    // 速度有效
                    return !angleFit(move, up) || !(speed(move, up) >= VariableSet.SPEED_LIMIT);
                }
            }
        } else {
            Log.e(TAG, "ILLEGAL STATUS: " + scrollStatus);
            return false;
        }
    }

    /**
     * performClick(): 复写方法
     *
     * @return super.performClick()
     */
    @Override
    public boolean performClick() {
        return super.performClick();
    }

    /**
     * openView(): 打开View
     */
    public void openView() {
        scrollStatus = STATUS_OPENING;
        this.smoothScrollTo(0, 0);
        point_move.clearPoint();
    }

    /**
     * closeView(): 关闭View
     */
    public void closeView() {
        scrollStatus = STATUS_CLOSING;
        this.smoothScrollTo(mMenuWidth, 0);
        point_move.clearPoint();
    }

    /**
     * click(): 点击特定位置后调用此方法, 进行打开或者关闭操作
     */
    public void click() {
        if (scrollStatus == STATUS_OPEN) {
            closeView();
        } else if (scrollStatus == STATUS_CLOSE) {
            openView();
        } else {
            Log.i(TAG, "INVALID CLICK");
        }
    }

    /**
     * getMenuWidth(): 获取左菜单宽度
     *
     * @return mMenuWidth, 返回左菜单宽度值
     */
    public int getMenuWidth() {
        return mMenuWidth;
    }

    /**
     * getScreenWidth(): 获取屏幕宽度
     *
     * @return mMenuWidth, 返回屏幕宽度
     */
    public int getScreenWidth() {
        return screenWidth;
    }

    /**
     * getScreenHeight(): 获取屏幕高度
     *
     * @return mMenuWidth, 返回屏幕高度
     */
    public int getScreenHeight() {
        return screenHeight;
    }

    /**
     * speed(ViewPoint start, ViewPoint end): 获取从一个位置坐标点到滑动到另一个位置坐标点的平均速度
     *
     * @param start ViewPoint 位置坐标点
     * @param end   ViewPoint 位置坐标点
     * @return boolean 类型数据
     */
    public double speed(ViewPoint start, ViewPoint end) {
        return (1.0 * Math.abs(start.getCoo_x() - end.getCoo_x()) / (Math.abs(start.getCoo_time() - end.getCoo_time())));
    }

    /**
     * angleFit(ViewPoint start, ViewPoint end): 判断两个位置坐标点与水平线的夹角是否符合特定要求
     *
     * @param start ViewPoint 位置坐标点
     * @param end   ViewPoint 位置坐标点
     * @return boolean 类型数据
     */
    public boolean angleFit(ViewPoint start, ViewPoint end) {
        double angle = Math.abs(1.0 * (start.getCoo_y() - end.getCoo_y()) / getDistance(start, end));
        return (angle <= VariableSet.SIN_TWENTY);
    }

    /**
     * angleFitMax(ViewPoint start, ViewPoint end): 判断两个位置坐标点与水平线的夹角是否符合特定的最大角度要求
     *
     * @param start ViewPoint 位置坐标点
     * @param end   ViewPoint 位置坐标点
     * @return boolean 类型数据
     */
    public boolean angleFitMax(ViewPoint start, ViewPoint end) {
        double angle = Math.abs(1.0 * (start.getCoo_y() - end.getCoo_y()) / getDistance(start, end));
        return (angle <= VariableSet.SIN_FORTY);
    }

    /**
     * getDistance(ViewPoint down, ViewPoint up): 获取两个给定位置坐标点之间的直线距离
     *
     * @param start ViewPoint 位置坐标点
     * @param end   ViewPoint 位置坐标点
     * @return double 类型数据
     */
    public double getDistance(ViewPoint start, ViewPoint end) {
        double distance;
        distance = (start.getCoo_x() - end.getCoo_x()) * (start.getCoo_x() - end.getCoo_x()) +
                (start.getCoo_y() - end.getCoo_y()) * (start.getCoo_y() - end.getCoo_y());
        distance = Math.abs(Math.pow(distance, 0.5));
        return distance;
    }
}

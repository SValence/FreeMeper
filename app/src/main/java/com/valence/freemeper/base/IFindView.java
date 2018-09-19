package com.valence.freemeper.base;

/**
 * Created by valence on 2017/9/3.
 */

public interface IFindView {

    /**
     * 变量初始值设定
     */
    public void setVariate();
    
    /**
     * 调用findViewById()确认控件
     */
    public void findView();

    /**
     * 给各控件设置监听事件
     */
    public void setListener();
}

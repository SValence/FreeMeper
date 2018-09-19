package com.valence.freemeper.cusview;

public class MarginBean {
    private int margin;
    private int left;
    private int top;
    private int right;
    private int bottom;

    public MarginBean() {
        this.margin = 0;
        this.left = 0;
        this.top = 0;
        this.right = 0;
        this.bottom = 0;
    }

    public MarginBean(int margin, int left, int top, int right, int bottom) {
        this.margin = margin;
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    public int getMargin() {
        return margin;
    }

    public int getLeft() {
        return left;
    }

    public int getTop() {
        return top;
    }

    public int getRight() {
        return right;
    }

    public int getBottom() {
        return bottom;
    }
}

package com.aeye.face.lightView;

/**
 * create by liulu at 2022/8/26
 **/
public class LightCacheBean {

    byte[] rgbImg;
    int currentColor;
    int state;
    /**当前帧的位置*/
    int pos;

    public LightCacheBean(byte[] rgbImg, int currentColor, int state, int mPos) {
        this.rgbImg = rgbImg;
        this.currentColor = currentColor;
        this.state = state;
        this.pos = mPos;
    }

    public int getPos() {
        return pos;
    }

    public void setPos(int pos) {
        this.pos = pos;
    }

    public byte[] getRgbImg() {
        return rgbImg;
    }

    public void setRgbImg(byte[] rgbImg) {
        this.rgbImg = rgbImg;
    }

    public int getCurrentColor() {
        return currentColor;
    }

    public void setCurrentColor(int currentColor) {
        this.currentColor = currentColor;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }
}

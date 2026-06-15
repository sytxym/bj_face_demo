package com.aeye.face.lightView;

import java.io.Serializable;

public class DecodeData implements Serializable {
    private byte[] data;
    private int currentColorIndex;
    private int currentColor;

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public int getCurrentColorIndex() {
        return currentColorIndex;
    }

    public void setCurrentColorIndex(int currentColorIndex) {
        this.currentColorIndex = currentColorIndex;
    }

    public int getCurrentColor() {
        return currentColor;
    }

    public void setCurrentColor(int currentColor) {
        this.currentColor = currentColor;
    }
}

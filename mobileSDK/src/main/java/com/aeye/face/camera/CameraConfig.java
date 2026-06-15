package com.aeye.face.camera;

public class CameraConfig {
    private static CameraConfig instance;

    public static CameraConfig getInstance() {
        if (instance == null) {
            synchronized (CameraConfig.class) {
                if (instance == null) {
                    instance = new CameraConfig();
                }
            }
        }
        return instance;
    }

    private int width;
    private int height;

    public void setSurfaceSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }
}

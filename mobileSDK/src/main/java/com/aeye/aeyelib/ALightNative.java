package com.aeye.aeyelib;

public class ALightNative {
    static {
        System.loadLibrary("LightAlive");
    }

    private static ALightNative instance;

    public static ALightNative getInstance() {
        if (instance == null) {
            synchronized (ALightNative.class) {
                if (instance == null) {
                    instance = new ALightNative();
                }
            }
        }
        return instance;
    }

    public native int Init(int[] maskImgInfo, byte[] maskImage, int maskAlignSize, int flashNum, float maskNormalizePercent,
                           int[] faceImageInfo, int frameNum, int faceAlignSize, int faceSize, int cutSize, int maxAlignOffsetPixel,
                           int maxKeyPointsNum, int leftEyeId, int rightEyeId, float faceNormalizePercent, int mergeWeight
    );

    public native String GetVersion();

    public native int Destroy();

    /**
     * @param cameraImage
     * @param color
     * @param state
     * @param isNotBoundary
     * @param quality
     * @param frameId
     * @param encode        传入数据编码格式 0：BGR 1：yuv420
     * @param rotate  rotate=[0, 90, 180, 270]
     * @return
     */
    public native int InsertImage(byte[] cameraImage, int color, int state, boolean isNotBoundary, int[] quality, int frameId, int encode, int rotate);

    public native void GetCurrentInsertImage(byte[] lastInsertImg, int frameId);

    public native int InsertKeyPoints(int[] keyPointX, int[] keyPointY, int frameId);

    public native int getImage(byte[] alignData, byte[] lasTData);


}

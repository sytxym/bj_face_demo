package com.aeye.aeyelib;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.File;

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

//    public static void initStaticSo(Context context) {
//        try {
//            System.loadLibrary("LightAlive");
//            Log.e("ALightNative", "load Soft success");
//        } catch (UnsatisfiedLinkError e) {
//            e.printStackTrace();
//            Log.e("ALightNative", "load Soft fail");
//            loadSoWithAbsolutePath(context);
//        }
//    }
//
//    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
//    private static void loadSoWithAbsolutePath(Context context) {
//        try {
//            String nativeLibraryDir = context.getApplicationInfo().nativeLibraryDir;
//            Log.e("ALightNative", "nativeLibraryDir=" + nativeLibraryDir);
//            String abi = Build.SUPPORTED_ABIS[0];
//            Log.e("ALightNative", "abi=" + abi);
//            // arm64-v8a 与其它 ABI 均尝试从 nativeLibraryDir 绝对路径加载
//            String libPath = nativeLibraryDir + File.separator + "libLightAlive.so";
//            File soFile = new File(libPath);
//            if (soFile.exists()) {
//                System.load(libPath);
//                Log.e("ALightNative", "load Soft with absolute path success:" + libPath);
//            } else {
//                Log.e("ALightNative", "so file not exists:" + libPath);
//            }
//        } catch (Throwable e) {
//            e.printStackTrace();
//            Log.e("ALightNative", "load Soft with absolute path fail");
//        }
//    }

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

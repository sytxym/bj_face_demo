//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.aeye.android.face;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;

import com.aeye.android.config.AEModelMgr;
import com.aeye.android.uitls.BitmapUtils;
import com.aeye.face.uitls.MLog;

import java.util.Arrays;
import java.util.Comparator;

public class UtilMtcnn {
    private static final String TAG = "UtilMtcnn";
    private static final String VERSION = "mtcnn_ncnn_android_20181221";
    private long hFaceRec = 0L;
    private boolean inited = false;
    private static UtilMtcnn mInstanse;

    static {
        System.loadLibrary("mtcnn_jni");
        mInstanse = null;
    }

    public UtilMtcnn() {
    }

    public String getVersion() {
        return "mtcnn_ncnn_android_20181221";
    }

    public static UtilMtcnn getInstance() {
        if (mInstanse == null) {
            mInstanse = new UtilMtcnn();
        }

        return mInstanse;
    }

    private void copyModel(Context context, Bundle paras) {
        MLog.d(TAG, "copyModel start");
        AEModelMgr.loadModelData(context, "pnet_model", "bin");
        MLog.d(TAG, "loadModelData pnet_model end");
        AEModelMgr.loadModelData(context, "pnet", "param");
        MLog.d(TAG, "loadModelData pnet end");
        AEModelMgr.loadModelData(context, "rnet_model", "bin");
        MLog.d(TAG, "loadModelData rnet_model end");
        AEModelMgr.loadModelData(context, "rnet", "param");
        MLog.d(TAG, "loadModelData rnet end");
        AEModelMgr.loadModelData(context, "onet_model", "bin");
        MLog.d(TAG, "loadModelData onet_model end");
        AEModelMgr.loadModelData(context, "onet", "param");
        MLog.d(TAG, "copyModel end");
    }

    private int initAlg(Context context, Bundle paras) {
        if (this.hFaceRec != 0L) {
            this.MtcnnDestroy(this.hFaceRec);
        }

        this.hFaceRec = this.MtcnnCreate();
        if (this.hFaceRec != 0L) {
            this.MtcnnUnserialize(this.hFaceRec, AEModelMgr.getModelFilePath(context, "pnet.param"), AEModelMgr.getModelFilePath(context, "pnet_model.bin"), AEModelMgr.getModelFilePath(context, "rnet.param"), AEModelMgr.getModelFilePath(context, "rnet_model.bin"), AEModelMgr.getModelFilePath(context, "onet.param"), AEModelMgr.getModelFilePath(context, "onet_model.bin"));
            this.inited = true;
            return 0;
        } else {
            this.inited = false;
            Log.e("Detect", "init error!");
            return -1;
        }
    }

    public int init(Context context, Bundle paras) {
        this.copyModel(context, paras);
        return this.initAlg(context, paras);
    }

    public int destroy() {
        this.MtcnnDestroy(this.hFaceRec);
        this.hFaceRec = 0L;
        this.inited = false;
        return 0;
    }

    public Rect[] faceDetect(int[] image, int width, int height) {
        if (!this.inited) {
            Log.e("Detect", "no init!");
            this.initAlg((Context) null, (Bundle) null);
            return null;
        } else {
            Rect[] faceRect = this.MtcnnDetect(this.hFaceRec, image, width, height, 1, 1.3F, 6, 48);
            if (faceRect != null && faceRect.length > 1) {
                Arrays.sort(faceRect, new Comparator<Rect>() {
                    public int compare(Rect lhs, Rect rhs) {
                        return lhs.width() > rhs.width() ? -1 : 0;
                    }
                });
            }

            return faceRect;
        }
    }

    public Rect[] faceDetect(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] image = BitmapUtils.getBitmapData(bitmap);
        return this.faceDetect(image, width, height);
    }

    native long MtcnnCreate();

    native void MtcnnDestroy(long var1);

    native void MtcnnUnserialize(long var1, String var3, String var4, String var5, String var6, String var7, String var8);

    native Rect[] MtcnnDetect(long var1, int[] var3, int var4, int var5, int var6, float var7, int var8, int var9);
}

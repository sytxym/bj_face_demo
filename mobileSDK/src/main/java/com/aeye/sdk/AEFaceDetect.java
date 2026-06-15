//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.aeye.sdk;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import com.aeye.android.face.UtilMtcnn;

public class AEFaceDetect {
    private static AEFaceDetect mInstance = null;

    public AEFaceDetect() {
    }

    public static AEFaceDetect getInstance() {
        if (mInstance == null) {
            mInstance = new AEFaceDetect();
        }

        return mInstance;
    }

    public String AEYE_FaceDetect_GetVersion() {
        return UtilMtcnn.getInstance().getVersion();
    }

    public int AEYE_FaceDetect_Init(Context context, Bundle paras) {
        UtilMtcnn.getInstance().init(context, paras);
        return 0;
    }

    public Rect[] AEYE_FaceDetect(int[] image, int width, int height) {
        Rect[] faceRect = UtilMtcnn.getInstance().faceDetect(image, width, height);
        return faceRect;
    }

    public Rect[] AEYE_FaceDetect(Bitmap image) {
        if (image == null) {
            return null;
        } else {
            long before = System.currentTimeMillis();
            Rect[] face = UtilMtcnn.getInstance().faceDetect(image);
            long after = System.currentTimeMillis();
            Log.d("ZDX", image.getWidth() + " X " + image.getHeight() + " cost time " + (after - before) + "ms");
            return face;
        }
    }

    public Rect[] AEYE_FaceDetectImg(int[] image, int width, int height) {
        return this.AEYE_FaceDetect(image, width, height);
    }

    public int AEYE_FaceDetect_Destory() {
        UtilMtcnn.getInstance().destroy();
        return 0;
    }
}

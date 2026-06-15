//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.aeye.android.uitls;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.util.Log;

import com.aeye.android.data.AEFaceInfo;

public class ImageUtils {
    public static final double FACE_ZOOM_NUM = 2.0D;
    public static final int IMAGE_WIDTH = 240;

    public ImageUtils() {
    }

    public static Rect resize(Rect src, int imgWidth, int imgHeight, boolean[] success) {
        int centerX = src.centerX();
        int centerY = src.centerY();
        int width = src.width();
        int height = src.height();
        int length = (int)((double)width * 2.0D / 2.0D);
        int left = centerX >= length ? length : centerX;
        int right = centerX >= imgWidth - length ? imgWidth - centerX : length;
        int top = centerY >= length ? length : centerY;
        int bottom = centerY >= imgHeight - length ? imgHeight - centerY : length;
        if (left == length && right == length && top == length && bottom == length) {
            success[0] = true;
        } else {
            success[0] = false;
        }

        int total = left + right;
        int totalY = top + bottom;
        int half;
        if (totalY < total) {
            total = top + bottom;
            half = total / 2;
            left = half;
            right = half;
            if (centerX < half) {
                left = centerX;
                right = total - centerX;
            }

            if (imgWidth - centerX < half) {
                right = imgWidth - centerX;
                left = total - right;
            }
        } else if (totalY > total) {
            half = total / 2;
            top = half;
            bottom = half;
            if (centerY < half) {
                top = centerY;
                bottom = total - centerY;
            }

            if (imgHeight - centerY < half) {
                bottom = imgHeight - centerY;
                top = total - bottom;
            }
        }

        return new Rect(centerX - left, centerY - top, centerX + right, centerY + bottom);
    }

    public static Rect getFaceRect(Rect src, Rect srcResize) {
        Rect dst = new Rect();
        int width = srcResize.right - srcResize.left;
        int height = srcResize.bottom - srcResize.top;
        int maxlength;
        if (width > height) {
            maxlength = width;
        } else {
            maxlength = height;
        }

        float scale = 240.0F / (float)maxlength;
        dst.left = src.left - srcResize.left;
        dst.right = src.right - srcResize.left;
        dst.top = src.top - srcResize.top;
        dst.bottom = src.bottom - srcResize.top;
        dst.left = (int)((double)((float)dst.left * scale) + 0.5D);
        dst.right = (int)((double)((float)dst.right * scale) + 0.5D);
        dst.top = (int)((double)((float)dst.top * scale) + 0.5D);
        dst.bottom = (int)((double)((float)dst.bottom * scale) + 0.5D);
        return dst;
    }

    public static final int[] GetEyePosition(Rect faceSrcInfo, Rect faceDstInfo, float[] eyeSrcInfo) {
        int[] eyeDstInfo = new int[]{0, 0, 0, 0, 0, 0, 0, 0};
        int faceWd = faceSrcInfo.right - faceSrcInfo.left;
        int faceHt = faceSrcInfo.bottom - faceSrcInfo.top;
        int faceWdNew = faceDstInfo.right - faceDstInfo.left;
        int faceHtNew = faceDstInfo.bottom - faceDstInfo.top;
        float ratio = 0.0F;
        ratio = eyeSrcInfo[4] / (float)faceWd;
        eyeDstInfo[4] = (int)(ratio * (float)faceWdNew);
        ratio = eyeSrcInfo[5] / (float)faceHt;
        eyeDstInfo[5] = (int)(ratio * (float)faceHtNew);
        ratio = eyeSrcInfo[6] / (float)faceWd;
        eyeDstInfo[6] = (int)(ratio * (float)faceWdNew);
        ratio = eyeSrcInfo[7] / (float)faceHt;
        eyeDstInfo[7] = (int)(ratio * (float)faceHtNew);
        ratio = (eyeSrcInfo[0] - (float)faceSrcInfo.left) / (float)faceWd;
        eyeDstInfo[0] = (int)((float)faceWdNew * ratio) + faceDstInfo.left;
        ratio = (eyeSrcInfo[1] - (float)faceSrcInfo.top) / (float)faceHt;
        eyeDstInfo[1] = (int)((float)faceHtNew * ratio) + faceDstInfo.top;
        ratio = (eyeSrcInfo[2] - (float)faceSrcInfo.left) / (float)faceWd;
        eyeDstInfo[2] = (int)((float)faceWdNew * ratio) + faceDstInfo.left;
        ratio = (eyeSrcInfo[3] - (float)faceSrcInfo.top) / (float)faceHt;
        eyeDstInfo[3] = (int)((float)faceHtNew * ratio) + faceDstInfo.top;
        return eyeDstInfo;
    }

    public static Bitmap enlarge(Bitmap bitmap) {
        Matrix matrix = new Matrix();
        matrix.postScale(240.0F / (float)bitmap.getWidth(), 240.0F / (float)bitmap.getHeight());
        Bitmap resizeBmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        Log.e("TAG","resize Bmp : "+resizeBmp.getWidth()+"*"+resizeBmp.getHeight());
        return resizeBmp;
    }

    public static AEFaceInfo cutoutImage(Bitmap bitmap, Rect rect) {
        boolean[] bSuccess = new boolean[1];
        Rect faceRect = resize(rect, bitmap.getWidth(), bitmap.getHeight(), bSuccess);
        Bitmap cutBitmap = Bitmap.createBitmap(bitmap, faceRect.left, faceRect.top, faceRect.width(), faceRect.height());
        Rect nuFace = getFaceRect(rect, faceRect);
        AEFaceInfo faceInfo = new AEFaceInfo();
        faceInfo.faceBitmap = enlarge(cutBitmap);
        faceInfo.faceRect = nuFace;
        return faceInfo;
    }
}

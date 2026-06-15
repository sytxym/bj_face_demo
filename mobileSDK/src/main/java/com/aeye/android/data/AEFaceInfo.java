//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.aeye.android.data;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.util.Log;

import com.aeye.android.uitls.BitmapUtils;
import com.aeye.android.uitls.ImageUtils;

public class AEFaceInfo {
    public Bitmap faceBitmap = null;
    public Bitmap faceBitmapNir = null;
    public Rect faceRect = null;
    public Rect faceRectNir = null;
    public int faceWidth = 0;
    public int faceHeight = 0;
    public int[] faceArr = new int[4];
    public byte[] imgByteA = null;
    public Rect imgRect = null;
    public int imgWidth = 0;
    public int imgHeight = 0;
    public byte[] imgByteANir = null;
    public Rect imgRectNir = null;
    public int imgWidthNir = 0;
    public int imgHeightNir = 0;
    public byte[] grayByteA = null;
    public byte[] grayByteANir = null;
    public int width = 0;
    public int height = 0;
    public int cameraId = 0;
    public int direction = 0;
    public boolean isAlive = false;
    public int faceNumber = 0;

    public AEFaceInfo() {
    }

    public AEFaceInfo clone() {
        AEFaceInfo info = new AEFaceInfo();
        info.isAlive = this.isAlive;
        info.direction = this.direction;
        info.cameraId = this.cameraId;
        info.width = this.width;
        info.height = this.height;
        info.faceWidth = this.faceWidth;
        info.faceHeight = this.faceHeight;
        info.imgWidth = this.imgWidth;
        info.imgHeight = this.imgHeight;
        info.imgWidthNir = this.imgWidthNir;
        info.imgHeightNir = this.imgHeightNir;
        if (this.faceBitmap != null) {
            info.faceBitmap = this.faceBitmap.copy(this.faceBitmap.getConfig(), true);
        }

        if (this.faceBitmapNir != null) {
            info.faceBitmapNir = this.faceBitmapNir.copy(this.faceBitmapNir.getConfig(), true);
        }

        if (this.faceRect != null) {
            info.faceRect = Rect.unflattenFromString(this.faceRect.flattenToString());
        }

        if (this.faceRectNir != null) {
            info.faceRectNir = Rect.unflattenFromString(this.faceRectNir.flattenToString());
        }

        if (this.imgRect != null) {
            info.imgRect = Rect.unflattenFromString(this.imgRect.flattenToString());
        }

        if (this.imgRectNir != null) {
            info.imgRectNir = Rect.unflattenFromString(this.imgRectNir.flattenToString());
        }

        if (this.imgByteA != null) {
            info.imgByteA = (byte[])this.imgByteA.clone();
        }

        if (this.imgByteANir != null) {
            info.imgByteANir = (byte[])this.imgByteANir.clone();
        }

        if (this.grayByteA != null) {
            info.grayByteA = (byte[])this.grayByteA.clone();
        }

        if (this.grayByteANir != null) {
            info.grayByteANir = (byte[])this.grayByteANir.clone();
        }

        if (this.faceArr != null) {
            info.faceArr = (int[])this.faceArr.clone();
        }
        info.faceNumber = this.faceNumber;

        return info;
    }

    public void recycle(boolean all) {
        if (all) {
            if (this.faceBitmap != null) {
                this.faceBitmap.recycle();
                this.faceBitmap = null;
            }

            if (this.faceBitmapNir != null) {
                this.faceBitmapNir.recycle();
                this.faceBitmapNir = null;
            }
        }

        this.faceRect = null;
        this.faceRectNir = null;
        this.imgRect = null;
        this.imgRectNir = null;
        this.imgByteA = null;
        this.imgByteANir = null;
        this.grayByteA = null;
        this.grayByteANir = null;
        this.faceArr = null;
        this.faceNumber =0;
    }

    public void cutFaceImage() {
        Bitmap bitmap = BitmapUtils.rawByteArray2RGBABitmap2(this.imgByteA, this.imgWidth, this.imgHeight, this.direction);
        boolean[] bSuccess = new boolean[1];
        Rect rect = ImageUtils.resize(this.imgRect, bitmap.getWidth(), bitmap.getHeight(), bSuccess);
        Bitmap cutBitmap = Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width(), rect.height());
        bitmap.recycle();
        this.faceRect = ImageUtils.getFaceRect(this.imgRect, rect);
        this.faceBitmap = ImageUtils.enlarge(cutBitmap);
        Log.e("TAG","face enlarge bitmap : "+this.faceBitmap.getWidth()+"*"+this.faceBitmap.getHeight());
        cutBitmap.recycle();
    }

    public void cutFaceImageNir() {
        Bitmap bitmap = BitmapUtils.yuy2Array2RGBABitmap(this.imgByteA, this.width, this.height);
        Matrix matrix = new Matrix();
        matrix.postRotate((float)this.direction);
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, this.width, this.height, matrix, true);
        boolean[] bSuccess = new boolean[1];
        Rect rect = ImageUtils.resize(this.imgRect, bitmap.getWidth(), bitmap.getHeight(), bSuccess);
        Bitmap cutBitmap = Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width(), rect.height());
        bitmap.recycle();
        this.faceRect = ImageUtils.getFaceRect(this.imgRect, rect);
        this.faceBitmap = ImageUtils.enlarge(cutBitmap);
        cutBitmap.recycle();
    }

    public void cutFaceImageNir2() {
        Bitmap bitmap = BitmapUtils.yuy2Array2RGBABitmap(this.imgByteANir, this.width, this.height);
        Matrix matrix = new Matrix();
        matrix.postRotate((float)this.direction);
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, this.width, this.height, matrix, true);
        boolean[] bSuccess = new boolean[1];
        Rect rect = ImageUtils.resize(this.imgRectNir, bitmap.getWidth(), bitmap.getHeight(), bSuccess);
        Bitmap cutBitmap = Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width(), rect.height());
        bitmap.recycle();
        this.faceRectNir = ImageUtils.getFaceRect(this.imgRectNir, rect);
        this.faceBitmapNir = ImageUtils.enlarge(cutBitmap);
        cutBitmap.recycle();
    }
}

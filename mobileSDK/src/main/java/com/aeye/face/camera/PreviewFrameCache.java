package com.aeye.face.camera;

import android.graphics.Bitmap;

import com.aeye.face.uitls.MBitmapUtil;

/**
 * 缓存相机预览最近一帧 NV21 数据，用于成功/失败/超时后定格显示。
 */
public final class PreviewFrameCache {

    private static final Object LOCK = new Object();
    private static byte[] frameData;
    private static int frameWidth;
    private static int frameHeight;
    private static int frameDirection;

    private PreviewFrameCache() {
    }

    public static void update(byte[] data, int width, int height, int direction) {
        if (data == null || width <= 0 || height <= 0) {
            return;
        }
        synchronized (LOCK) {
            if (frameData == null || frameData.length != data.length) {
                frameData = new byte[data.length];
            }
            System.arraycopy(data, 0, frameData, 0, data.length);
            frameWidth = width;
            frameHeight = height;
            frameDirection = direction;
        }
    }

    public static void clear() {
        synchronized (LOCK) {
            frameData = null;
            frameWidth = 0;
            frameHeight = 0;
        }
    }

    public static boolean hasFrame() {
        synchronized (LOCK) {
            return frameData != null && frameWidth > 0 && frameHeight > 0;
        }
    }

    /**
     * 将缓存帧转为与预览朝向一致的 Bitmap；无缓存时返回 null。
     */
    public static Bitmap toBitmap() {
        synchronized (LOCK) {
            if (frameData == null || frameWidth <= 0 || frameHeight <= 0) {
                return null;
            }
            return MBitmapUtil.rawByteArray2RGBABitmap2(
                    frameData, frameWidth, frameHeight, frameDirection);
        }
    }
}

package com.aeye.face.uitls;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.util.Log;

import com.aeye.android.data.AEFaceInfo;
import com.aeye.android.libutils.ComplexUtil;
import com.aeye.android.uitls.BitmapUtils;
import com.aeye.android.uitls.ImageUtils;


public class MBitmapUtil {

    private static String TAG = MBitmapUtil.class.getSimpleName();

    public static Bitmap cutFaceImageWithAlive(AEFaceInfo face) {
        Bitmap bitmap = BitmapUtils.rawByteArray2RGBABitmap2(face.imgByteA,
                face.imgWidth, face.imgHeight, face.direction);
        return bitmap;
    }

    private static Bitmap cutAliveImage(Rect rect, Bitmap image) {
        int rectWidth = rect.width();
        int rectHeight = rect.height();
        int rectCenterX = rect.centerX();
        int rectCenterY = rect.centerY();

        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();

        int min;

        int minLeft = rectCenterX > rectWidth ? rectWidth : rectCenterX;
        min = minLeft;
        int minRight = (imageWidth - rectCenterX) > rectWidth ? rectWidth : (imageWidth - rectCenterX);
        min = min > minRight ? minRight : min;
        int minTop = rectCenterY > rectHeight ? rectHeight : rectCenterY;
        min = min > minTop ? minTop : min;
        int minBottom = (imageHeight - rectCenterY) > rectHeight ? rectHeight : (imageHeight - rectCenterY);
        min = min > minBottom ? minBottom : min;

        Rect resizeRect = new Rect(rectCenterX - min, rectCenterY - min, rectCenterX + min, rectCenterY + min);
        Bitmap cutBmp = Bitmap.createBitmap(image, resizeRect.left, resizeRect.top,
                resizeRect.width(), resizeRect.height());
        return Bitmap.createScaledBitmap(cutBmp, 240, 240, true);
    }

    public static Bitmap cutFaceImage(AEFaceInfo face) {
        Bitmap bitmap = rawByteArray2RGBABitmap2(face.imgByteA,
                face.imgWidth, face.imgHeight, face.direction);
        Log.e("TAG","face enlarge bitmap : "+bitmap.getWidth()+"*"+bitmap.getHeight());
        return bitmap;
    }

    public static Bitmap rawByteArray2RGBABitmap2(byte[] data, int width, int height, int direction) {
        Log.d(TAG, "rawByteArray2RGBABitmap2 w=" + width + ",height=" + height);
        int frameSize = width * height;
        int[] rgba = null;
        byte imageSize;
        // TODO: 2022/8/19
//        if (width <= 960 && height <= 960) {
        rgba = new int[frameSize];
        imageSize = 0;
//        } else {
//
//            rgba = new int[frameSize / 4];
//            imageSize = 1;
//        }

        ComplexUtil.getInstance().YUVToBitmapR(data, rgba, width, height, imageSize, direction);
        Bitmap bmp = null;
        if (imageSize == 0) {
            if (direction != 90 && direction != -90) {
                bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                bmp.setPixels(rgba, 0, width, 0, 0, width, height);
            } else {
                bmp = Bitmap.createBitmap(height, width, Bitmap.Config.ARGB_8888);
                bmp.setPixels(rgba, 0, height, 0, 0, height, width);
            }
        } else if (imageSize == 1) {
            if (direction != 90 && direction != -90) {
                bmp = Bitmap.createBitmap(width / 2, height / 2, Bitmap.Config.ARGB_8888);
                bmp.setPixels(rgba, 0, width / 2, 0, 0, width / 2, height / 2);
            } else {
                bmp = Bitmap.createBitmap(height / 2, width / 2, Bitmap.Config.ARGB_8888);
                bmp.setPixels(rgba, 0, height / 2, 0, 0, height / 2, width / 2);
            }
        }
        return bmp;
    }

    public static Bitmap getFullFaceImageWithAlive(AEFaceInfo face) {
        // TODO: 2022/8/19  人脸图
//        Bitmap bitmap = rawByteArray2RGBABitmap2(face.imgByteA,
//                face.imgWidth, face.imgHeight, face.direction);
//        boolean[] bSuccess = new boolean[1];
//
//
//        Rect rect = ImageUtils.resize(face.imgRect, bitmap.getWidth(),
//                bitmap.getHeight(), bSuccess);
//        int min = bitmap.getWidth() > bitmap.getHeight() ? bitmap.getHeight() : bitmap.getWidth();
//        int top = 45;
//        int y = rect.top > top ? rect.top - top : rect.top;
//        int limit = bitmap.getHeight() - min;
//        if (y > limit) {
//            Log.e(TAG, "change limit");
//            y = limit;
//        }
//        Bitmap cutBitmap = Bitmap.createBitmap(bitmap, 0, y,
//                min, min);
//        face.faceRect = ImageUtils.getFaceRect(face.imgRect, rect);
//        face.faceBitmap = cutBitmap;
//        return cutBitmap;
        //2025年8月29日临时修改 images节点为640*480的图
        Bitmap fulBitmap =  rawByteArray2RGBABitmap2(face.imgByteA,
                face.imgWidth, face.imgHeight, face.direction);
        face.faceBitmap = fulBitmap;
        face.faceRect = face.imgRect;
        return  fulBitmap;


        //2025年7月25日 人脸图改为240*240
//        Bitmap bitmap = rawByteArray2RGBABitmap2(face.imgByteA,
//                face.imgWidth, face.imgHeight, face.direction);
//        boolean[] bSuccess = new boolean[1];
//        Rect rect = ImageUtils.resize(face.imgRect, bitmap.getWidth(),
//                bitmap.getHeight(), bSuccess);
//        face.faceRect = ImageUtils.getFaceRect(face.imgRect, rect);
//        Bitmap cutBitmap = cutAliveImage(face.imgRect, bitmap);
//        face.faceBitmap = cutBitmap;
//        return cutBitmap;
    }

    /**
     * json 返回 中的Alive全景图 480 * 640 2024年12月
     * @param face
     * @return
     */
    public  static Bitmap getFull640(AEFaceInfo face){
        //全景图 480 * 640 2024年12月
        Bitmap bitmap = BitmapUtils.rawByteArray2RGBABitmap2(face.imgByteA,
                face.imgWidth, face.imgHeight, face.direction);
        return  bitmap;
    }

    public static Bitmap rawByteArray2RGBABitmapFull(byte[] data, int width, int height, int direction) {

        int frameSize = width * height;
        int[] rgba = null;
        byte imageSize;

//        if (width <= 960 && height <= 960) {
//            rgba = new int[frameSize];
//            imageSize = 0;
//        } else {
//            rgba = new int[frameSize / 4];
//            imageSize = 1;
//        }
        rgba = new int[frameSize];
        imageSize = 0;
        ComplexUtil.getInstance().YUVToBitmapR(data, rgba, width, height, imageSize, direction);
        Bitmap bmp = null;
        if (imageSize == 0) {
            if (direction != 90 && direction != -90) {
                bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
                bmp.setPixels(rgba, 0, width, 0, 0, width, height);
            } else {
                bmp = Bitmap.createBitmap(height, width, Bitmap.Config.RGB_565);
                bmp.setPixels(rgba, 0, height, 0, 0, height, width);
            }
        } else if (imageSize == 1) {
            if (direction != 90 && direction != -90) {
                bmp = Bitmap.createBitmap(width / 2, height / 2, Bitmap.Config.RGB_565);
                bmp.setPixels(rgba, 0, width / 2, 0, 0, width / 2, height / 2);
            } else {
                bmp = Bitmap.createBitmap(height / 2, width / 2, Bitmap.Config.RGB_565);
                bmp.setPixels(rgba, 0, height / 2, 0, 0, height / 2, width / 2);
            }
        }

        return bmp;
    }

    public static Bitmap scaleBitmap(Bitmap origin, int minSize) {
        Bitmap result;
        if (origin == null) {
            return null;
        }
        int width = origin.getWidth();
        int height = origin.getHeight();
        int min = width > height ? height : width;
        if (min > minSize) {
            float ratio = (float) minSize / min;
            Matrix matrix = new Matrix();
            matrix.preScale(ratio, ratio);
            result = Bitmap.createBitmap(origin, 0, 0, width, height, matrix, false);
        } else {
            result = origin;
        }

        return result;
    }


    public static byte[] rawByteArray2Y(byte[] data, int width, int height, int direction) {
        int frameSize = width * height;
        byte[] rgba = null;
        byte imageSize;
        rgba = new byte[frameSize];
        imageSize = 0;
        ComplexUtil.getInstance().YUVToYR(data, rgba, width, height, imageSize, direction);
        return rgba;
    }

    public static int[] cvtSpace(byte[] data, int width, int height,
                                 int direction) {
        int imageSize = -1;
        int frameSize = width * height;
        int[] rgba = null;

        rgba = new int[frameSize];
        imageSize = 0;

        ComplexUtil.getInstance().YUVToBitmapR(data, rgba, width, height,
                imageSize, direction);
        return rgba;
    }

}

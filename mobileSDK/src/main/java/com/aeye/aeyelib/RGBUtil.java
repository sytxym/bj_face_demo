package com.aeye.aeyelib;

import android.graphics.Bitmap;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

public class RGBUtil {
    private static String TAG = RGBUtil.class.getSimpleName();
    public static int width = 1080;
    public static int height = 1920;

    public static byte[] RGB2BGR(byte[] rgbPixels) throws IOException {
        byte[] bgrPixels = new byte[rgbPixels.length];
        for (int i = 0; i < rgbPixels.length; i++) {
            int color = rgbPixels[i];
            int red = ((color & 0x00FF0000) >> 16);
            int green = ((color & 0x0000FF00) >> 8);
            int blue = color & 0x000000FF;
            // 将rgb三个通道的数值合并为一个int数值，同时调换b通道和r通道
            bgrPixels[i] = (byte) ((red & 0x000000FF) | (green << 8 & 0x0000FF00) | (blue << 16 & 0x00FF0000));
        }
        return bgrPixels;
    }

    public static byte[] pixelToBGR(int[] pix, int width, int height){
        byte[] bgr = new byte[width * height * 3];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int idx = width * i + j;
                int color = pix[idx]; //获取像素
                int b = ((color & 0x00FF0000) >> 16);
                int green = ((color & 0x0000FF00) >> 8);
                int r = color & 0x000000FF;

//                b=((pixel>>16)&0x000000ff),g=((pixel>>8)&0x000000ff),r=(pixel&0x000000ff)

                int rgbIdx = idx * 3;
                bgr[rgbIdx] = (byte) b;
                bgr[rgbIdx + 1] = (byte) green;
                bgr[rgbIdx + 2] = (byte) r;
            }
        }
        return bgr;
    }

    public static byte[] bitmap2RGB(Bitmap bitmap) {
        int bytes = bitmap.getByteCount();  //返回可用于储存此位图像素的最小字节数
        Log.e(TAG, "bytes=" + bytes);
        ByteBuffer buffer = ByteBuffer.allocate(bytes); //  使用allocate()静态方法创建字节缓冲区
        bitmap.copyPixelsToBuffer(buffer); // 将位图的像素复制到指定的缓冲区

        byte[] rgba = buffer.array();
        byte[] pixels = new byte[(rgba.length / 4) * 3];

        int count = rgba.length / 4;

        //Bitmap像素点的色彩通道排列顺序是RGBA
        for (int i = 0; i < count; i++) {
            pixels[i * 3] = rgba[i * 4];        //R
            pixels[i * 3 + 1] = rgba[i * 4 + 1];    //G
            pixels[i * 3 + 2] = rgba[i * 4 + 2];       //B
        }
        return pixels;
    }

    public static byte[] bitmap2BGR(Bitmap bitmap) {
        int bytes = bitmap.getByteCount();  //返回可用于储存此位图像素的最小字节数
        ByteBuffer buffer = ByteBuffer.allocate(bytes); //  使用allocate()静态方法创建字节缓冲区
        bitmap.copyPixelsToBuffer(buffer); // 将位图的像素复制到指定的缓冲区

        byte[] rgba = buffer.array();
        byte[] pixels = new byte[(rgba.length / 4) * 3];

        int count = rgba.length / 4;

        //Bitmap像素点的色彩通道排列顺序是RGBA
        for (int i = 0; i < count; i++) {
            pixels[i * 3] = rgba[i * 4 + 2];        //B
            pixels[i * 3 + 1] = rgba[i * 4 + 1];    //G
            pixels[i * 3 + 2] = rgba[i * 4];       //B
        }
        return pixels;
    }

    public static byte[] RGBA2RGB(byte[] rgba) {
        byte[] pixels = new byte[(rgba.length / 4) * 3];
        int count = rgba.length / 4;
        //Bitmap像素点的色彩通道排列顺序是RGBA
        for (int i = 0; i < count; i++) {
            pixels[i * 3] = rgba[i * 4];        //R
            pixels[i * 3 + 1] = rgba[i * 4 + 1];    //G
            pixels[i * 3 + 2] = rgba[i * 4 + 2];       //B
        }
        return pixels;
    }

    public static Bitmap RGBToBitmap(byte[] data, int w, int h) {
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        int[] pixels = BGR2Pixel(data);
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h);
        return bitmap;
    }

    public static Bitmap bestBGRToBitmap(byte[] data, int w, int h) {
        int width = w, height = h;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        int[] pixels = BGR2Pixel(data);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }

    public static int[] BGR2Pixel(byte[] data) {
        int len = data.length / 3;
        int[] result = new int[len];
        for (int i = 0; i < len; i++) {
//            result[i] = (data[i * 3] << 16 & 0x00FF0000) |
//                    (data[i * 3 + 1] << 8 & 0x0000FF00) |
//                    (data[i * 3 + 2] & 0x000000FF) |
//                    0xFF000000;
            result[i] = (data[i * 3+2] << 16 & 0x00FF0000) |
                    (data[i * 3 + 1] << 8 & 0x0000FF00) |
                    (data[i * 3 ] & 0x000000FF) |
                    0xFF000000;

        }
        return result;
    }

    static public Bitmap rgb2Bitmap(byte[] data, int width, int height) {
        int[] colors = convertByteToColor(data);    //取RGB值转换为int数组
        if (colors == null) {
            return null;
        }

        Bitmap bmp = Bitmap.createBitmap(colors, 0, width, width, height,
                Bitmap.Config.ARGB_8888);
        return bmp;

    }

    // 将一个byte数转成int
    // 实现这个函数的目的是为了将byte数当成无符号的变量去转化成int
    public static int convertByteToInt(byte data) {

        int heightBit = (int) ((data >> 4) & 0x0F);
        int lowBit = (int) (0x0F & data);
        return heightBit * 16 + lowBit;
    }


    // 将纯RGB数据数组转化成int像素数组
    public static int[] convertByteToColor(byte[] data) {
        int size = data.length;
        if (size == 0) {
            return null;
        }

        int arg = 0;
        if (size % 3 != 0) {
            arg = 1;
        }

        // 一般RGB字节数组的长度应该是3的倍数，
        // 不排除有特殊情况，多余的RGB数据用黑色0XFF000000填充
        int[] color = new int[size / 3 + arg];
        int red, green, blue;
        int colorLen = color.length;
        if (arg == 0) {
            for (int i = 0; i < colorLen; ++i) {
                red = convertByteToInt(data[i * 3]);
                green = convertByteToInt(data[i * 3 + 1]);
                blue = convertByteToInt(data[i * 3 + 2]);

                // 获取RGB分量值通过按位或生成int的像素值
                color[i] = (red << 16) | (green << 8) | blue | 0xFF000000;
            }
        } else {
            for (int i = 0; i < colorLen - 1; ++i) {
                red = convertByteToInt(data[i * 3]);
                green = convertByteToInt(data[i * 3 + 1]);
                blue = convertByteToInt(data[i * 3 + 2]);
                color[i] = (red << 16) | (green << 8) | blue | 0xFF000000;
            }

            color[colorLen - 1] = 0xFF000000;
        }

        return color;
    }
}

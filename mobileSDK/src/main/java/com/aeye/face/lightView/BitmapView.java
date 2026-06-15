package com.aeye.face.lightView;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;

public class BitmapView
{
    String TAG =  BitmapView.class.getSimpleName();
    private int mWidth ,mHeight;
    private Paint paintOval,paintCenter,mPaint;
    private RectF rel;
    public BitmapView(int width , int height, RectF mOvalRect)
    {
        this.mWidth = width;
        this.mHeight = height;
        this.rel = mOvalRect;

        paintOval = new Paint();
        paintOval.setAntiAlias(true);
        paintOval.setColor(Color.parseColor("#ffffff"));
        paintOval.setStyle(Paint.Style.FILL);
//        paintOval.setStrokeWidth(3);

        paintCenter = new Paint();
        paintCenter.setColor(Color.parseColor("#00000000"));

        mPaint = new Paint();
        mPaint.setColor(Color.BLACK);
    }

    public Bitmap onDraw()
    {
        Bitmap bitmap = Bitmap.createBitmap(mWidth,mHeight, Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.parseColor("#000000"));
//        canvas.drawRect(new Rect(0,0,mWidth,mHeight),mPaint);
        //绘制椭圆
        int min = mWidth > mHeight ? mHeight : mWidth;
        int width = min * 4 / 5;
        int height = width;
        Log.e(TAG, "width=" + width);
        Rect centerRect = new Rect();
        int faceRectCenterY =(int) ((float)1 * mHeight / 3);

        centerRect.left = (int)((float)(mWidth / 2) - (float)(width / 2));
        centerRect.top = (int)(faceRectCenterY -(float) (height / 2));
        centerRect.right = (int)((float)(mWidth / 2) + (float)(width / 2));
        centerRect.bottom = (int)((float)faceRectCenterY + (float)(height / 2));

//        int ovalLeft =centerRect.left+width/8 ;
//        int ovalRight = centerRect.right-width/8;
//        int ovalTop = centerRect.top+5;
//        int ovalBottom = centerRect.bottom-5;
//        RectF rel = new RectF(ovalLeft,ovalTop,ovalRight,ovalBottom);
        //RectF(96.0, 26.0, 384.0, 400.0)
        Log.e(TAG,"rel oval : "+rel);
        canvas.drawOval(rel, paintOval);
//        RectF ovalCenter = new RectF(ovalLeft+3,ovalTop+3,ovalRight-3,ovalBottom-3);
//        canvas.drawOval(ovalCenter,paintCenter);
        //保存全部图层
        canvas.save();
        canvas.restore();
        return  bitmap;
    }
    //保存到本地
    public void SaveBitmap(Bitmap bitmap, String filename)
    {
        //存储路径
        File file = new File("/sdcard/FaceCollect");
        if(!file.exists())
            file.mkdirs();
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file.getPath() + "/ovalMask.jpg");
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
            fileOutputStream.close();
            System.out.println("saveBmp is here");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
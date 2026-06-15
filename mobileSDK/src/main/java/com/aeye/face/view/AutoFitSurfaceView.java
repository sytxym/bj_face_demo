package com.aeye.face.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceView;

import com.aeye.face.lightView.RecognizeLightActivity;


public class AutoFitSurfaceView extends SurfaceView {
    private static final String TAG = "AutoFitSurfaceView";

    private int ratioW = 0;
    private int ratioH = 0;

    public AutoFitSurfaceView(Context context) {
        super(context);
    }

    public AutoFitSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AutoFitSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * 设置宽高比
     * @param width
     * @param height
     */
    public void setAspectRation(int width, int height){
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("width or height can not be negative.");
        }
        ratioW = width;
        ratioH = height;
        Log.e(TAG, "setAspectRation: ratioW = "+ratioW+" ratioH = "+ratioH );
        //请求重新布局
        requestLayout();
    }
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//        int width = MeasureSpec.getSize(widthMeasureSpec);
//        int height = MeasureSpec.getSize(heightMeasureSpec);

        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        Log.e(TAG, "onMeasure: width = "+width+" height = "+height );
        if (0 == ratioW || 0 == ratioH){
            //未设定宽高比，使用预览窗口默认宽高，当AutoFitTextureView初始化的时候会走一次这个逻辑
            setMeasuredDimension(width, height);
            RecognizeLightActivity.screenWidth = width;
            RecognizeLightActivity.screenHeight = height;
            Log.e(TAG, "onMeasure 000: width = "+width+" height = "+height );
        } else {
            //设定宽高比，调整预览窗口大小（调整后窗口大小不超过默认值）
//            int measuredWidth = height * ratioW / ratioH;
//            if (width < measuredWidth) {
//                int measuredHeight = width * ratioH / ratioW;
//                Log.e(TAG, "onMeasure 111: width = "+width+" height = "+ measuredHeight);
//                setMeasuredDimension(width, measuredHeight);
//
//                RecognizeLightActivity.screenWidth = width;
//                RecognizeLightActivity.screenHeight = measuredHeight;
//            } else {
//                Log.e(TAG, "onMeasure 222: width = "+ measuredWidth +" height = "+height );
//                setMeasuredDimension(measuredWidth, height);
//
//                RecognizeLightActivity.screenWidth = measuredWidth;
//                RecognizeLightActivity.screenHeight = height;
//            }
        }


        if(RecognizeLightActivity.screenHeight ==0) {
            int radius = 2 * width / 5;
            radius =  height/6;//竞品的数据
            int width1 = radius * 2;
//            width1 = 700;
            double diff = (float)ratioH / (float) ratioW;
            double height111 = width1 *diff;
            Log.e(TAG, "meature  height111 : " + height111 );
            int measuredHeight = (int) height111;
            RecognizeLightActivity.screenWidth = width1;
            RecognizeLightActivity.screenHeight = measuredHeight;
            Log.e(TAG, "meature : " + width1 + " ,height : " + measuredHeight + " , diff : " + diff+", radius : "+radius);
        }
        setMeasuredDimension(RecognizeLightActivity.screenWidth, RecognizeLightActivity.screenHeight);

    }
}


package com.aeye.face.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceView;

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
     * ?????
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
        //??????
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
            //???????????????????AutoFitTextureView??????????????
            setMeasuredDimension(width, height);
            RecognizeActivity.screenWidth = width;
            RecognizeActivity.screenHeight = height;
            Log.e(TAG, "onMeasure 000: width = "+width+" height = "+height );
        } else {
            //?????????????????????????????
//            int measuredWidth = height * ratioW / ratioH;
//            if (width < measuredWidth) {
//                int measuredHeight = width * ratioH / ratioW;
//                Log.e(TAG, "onMeasure 111: width = "+width+" height = "+ measuredHeight);
//                setMeasuredDimension(width, measuredHeight);
//
//                RecognizeActivity.screenWidth = width;
//                RecognizeActivity.screenHeight = measuredHeight;
//            } else {
//                Log.e(TAG, "onMeasure 222: width = "+ measuredWidth +" height = "+height );
//                setMeasuredDimension(measuredWidth, height);
//
//                RecognizeActivity.screenWidth = measuredWidth;
//                RecognizeActivity.screenHeight = height;
//            }
        }


        if(RecognizeActivity.screenHeight ==0) {
            int radius = 2 * width / 5;
            radius =  height/6;//?????
            int width1 = radius * 2;
//            width1 = 700;
            double diff = (float)ratioH / (float) ratioW;
            double height111 = width1 *diff;
            Log.e(TAG, "meature  height111 : " + height111 );
            int measuredHeight = (int) height111;
            RecognizeActivity.screenWidth = width1;
            RecognizeActivity.screenHeight = measuredHeight;
            Log.e(TAG, "meature : " + width1 + " ,height : " + measuredHeight + " , diff : " + diff+", radius : "+radius);
        }
        setMeasuredDimension(RecognizeActivity.screenWidth, RecognizeActivity.screenHeight);

    }
}


package com.aeye.face.lightView;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;

import com.aeye.face.uitls.FLogUtil;
import com.sdk.core.R;


public class CheckFaceView extends View{
    private final static String TAG = "FACEVIEW_DEBUG";
    private Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float mTextSize = 60f;
    private Bitmap mInnerCircleBitmap = null;//内环圆
    private Bitmap mOutCircleBitmap = null;//外环圆
    private float mDegress = 0;//旋转角度
    private ValueAnimator valueAnimator;

    private int outColor = -11520;
    private boolean isRest;
    private int defaultColor = Color.parseColor("#FFFFFFFF");
    private boolean isHasFace = false,isChangeFromSetColor = false;
    private Paint paintOval;
    private Paint mPaintCircle;
    //圆环的矩形区域
    private RectF mRectF;
    int mRingWidth = 20;
    int mCurrentProgress =0;
    private Handler colorChangeHandler;

    private static int canvasWidth = 0,canvasHeight;
    private static RectF ovalRect;
    public static int getCanvasWidth(){
        return canvasWidth;
    }
    public static int getCanvasHeight(){
        return canvasHeight;
    }

    public static RectF getOvalRect(){
        return ovalRect;
    }

    public CheckFaceView(Context context) {
        this(context,null);
    }

    public CheckFaceView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public CheckFaceView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(Color.WHITE);
        mPaint.setColor(Color.BLUE);

        mTextPaint.setTextSize(mTextSize);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setColor(Color.BLACK);


        paintOval = new Paint();
        paintOval.setAntiAlias(true);
        paintOval.setColor(Color.parseColor("#ffffff"));
        paintOval.setStyle(Paint.Style.STROKE);
        paintOval.setStrokeWidth(3);

        mPaintCircle = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaintCircle.setAntiAlias(true);
        mPaintCircle.setColor(Color.parseColor("#ffffff"));
        //空心
        mPaintCircle.setStyle(Paint.Style.STROKE);
        //宽度
        mPaintCircle.setStrokeWidth(20);
        mCurrentProgress =0;
        int countTime =  RecognizeLightActivity.getSplitTime() * 5;
        valueAnimator = getValA(countTime);
    }

    public void setOutColor(int outColor, Handler timeHandler) {
        if(colorChangeHandler == null){
            colorChangeHandler = timeHandler;
        }
//        if(RecognizeActivity.getCurrentIndex() == 1 && (valueAnimator !=null && !valueAnimator.isRunning())) {
//            FLogUtil.printLog( "go in counttime============ progress "+mCurrentProgress);
//            startCountDown( );
//        }else if(RecognizeActivity.getmFaceOK()!=1){
//            stopCountDown();
//        }
        isChangeFromSetColor = true;
        FLogUtil.printLog( "CheckFaceView setOutColor color=" + outColor+", mCurrentProgress : "+mCurrentProgress+" current Index: "+ RecognizeLightActivity.getCurrentIndex());
        Log.e(TAG,"&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&");
        this.outColor = outColor;
        isRest = true;
        postInvalidate();
    }
    private ValueAnimator getValA(long countdownTime) {
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(0, 20);
        valueAnimator.setDuration(countdownTime);
        valueAnimator.setInterpolator(new LinearInterpolator());
        valueAnimator.setRepeatCount(0);
        return valueAnimator;
    }
    /**
     * 开始倒计时
     */
    public void startCountDown(  ){
        mCurrentProgress =0;
        if(valueAnimator !=null && valueAnimator.isRunning()){
            valueAnimator.cancel();
        }
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float i = Float.valueOf(String.valueOf(animation.getAnimatedValue()));
                mCurrentProgress = (int) (360 * (i / 20f));
                if(RecognizeLightActivity.getmFaceOK()!=1){
                    stopCountDown();
                }
//                FLogUtil.printLog("Recog colorIndex=" + RecognizeActivity.getCurrentIndex()+", mCurrentProgress : "+mCurrentProgress);
                postInvalidate();
            }
        });
        valueAnimator.start();
    }

    public  void stopCountDown(){
        if(valueAnimator !=null){
            valueAnimator.cancel();
        }
    }
    public void hasFace(boolean hasFace) {
        Log.e(TAG, "has face :" + hasFace);
        isHasFace = hasFace;
        if(isHasFace){
            paintOval.setColor(Color.parseColor("#008000"));
            paintOval.setStyle(Paint.Style.STROKE);
            paintOval.setStrokeWidth(3);
        }else{
            paintOval.setColor(Color.parseColor("#ffffff"));
            paintOval.setStyle(Paint.Style.STROKE);
            paintOval.setStrokeWidth(3);
        }
        postInvalidate();
    }
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int measuredWidth = getMeasuredWidth();
        int measuredHeight = getMeasuredHeight();
        canvasWidth = measuredWidth;
        canvasHeight = measuredHeight;
        drawCircleMask(canvas);
        if(colorChangeHandler !=null && isChangeFromSetColor  ){
            isChangeFromSetColor = false;
            //真正变换界面颜色需要几十毫秒延迟，在延迟后再index+1 以及记录颜色值
            colorChangeHandler.sendEmptyMessageDelayed(RecognizeLightActivity.MSG_CODE_COLOR_INDEX_UPDATE,60);
        }
//        FLogUtil.printLog( "onDraw ==================== mCurrentProgress : "+mCurrentProgress);
//        drawBitmapCircle(canvas);
//        canvas.drawText("请把脸移入圈内",getWidth() / 2, (float) (getWidth() * 1.2),mTextPaint);
    }

    /**
     * 绘制圆圈遮罩
     * @param canvas
     */
    private void drawCircleMask(Canvas canvas) {
        canvas.save();
        int currentColor = !isRest ? defaultColor : outColor;
        mPaint.setColor(currentColor);
        //目标图Dst
        int width = getWidth();
        int height = getHeight();

        canvas.drawRect(new Rect(0,0, width, height), mPaint);
        //设置混合模式
        mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        int circleCenterYPer = 6;
        //源图Src，重叠区域右下角部分
        int cx = width / 2;
        int cy = height / 3;
        int top2screent = height/4;
        int radius = 2 * width / 5;
        radius = height/circleCenterYPer;//竞品的数据
//        Log.e(TAG, "drawCircleMask  : width = "+width+" height = "+height +",radius : "+radius);
        cy = top2screent+radius/2;
        cy = 2*height/circleCenterYPer;//竞品的数据
        canvas.drawCircle(cx, cy, radius, mPaint);
        int ovalLeft =cx - 2*radius /3 ;
        int ovalRight = cx + 2*radius /3;
        int ovalTop = cy- (4*radius/5);
        int ovalBottom = cy +(4*radius/5);

        //以下为竞品的数据
        ovalLeft = cx - 6*radius/7;
        ovalRight = cx + 6*radius/7;
        ovalTop = cy - 9*radius/9;
        ovalBottom = cy +9*radius/9;

        RectF rel = new RectF(ovalLeft,ovalTop,ovalRight,ovalBottom);
        //绘制椭圆
        canvas.drawOval(rel, paintOval);
//        canvas.drawArc(rel,0,360,false,paintOval);
        ovalRect = rel;

        int circleLeft = cx - radius;
        int circleRight = cx +radius;
        int circleTop = ovalTop;
        int circleBottom = ovalBottom;
        mRectF = new RectF(circleLeft + mRingWidth / 2, circleTop + mRingWidth / 2,
                circleRight - mRingWidth / 2, circleBottom - mRingWidth / 2);
        canvas.drawArc(mRectF, -90, mCurrentProgress , false, mPaintCircle);
        //清除混合模式
        mPaint.setXfermode(null);
        canvas.restore();
    }

    /**
     * 画圆圈外部的圆圈图片
     */
    private void drawBitmapCircle(Canvas canvas) {
        if(mInnerCircleBitmap == null){
            int dstWidthAndHeight = (int) (getWidth() / 1.5f + getWidth() / 1.5f / 4);
            mInnerCircleBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.circle1);
            mInnerCircleBitmap = Bitmap.createScaledBitmap(mInnerCircleBitmap,dstWidthAndHeight,dstWidthAndHeight,true);
        }
        if(mOutCircleBitmap == null){
            int dstWidthAndHeight = (int) (getWidth() / 1.5f + getWidth() / 1.5f / 4);
            mOutCircleBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.circle2);
            mOutCircleBitmap = Bitmap.createScaledBitmap(mOutCircleBitmap,dstWidthAndHeight,dstWidthAndHeight,true);
        }
        int left = (getWidth() - mInnerCircleBitmap.getWidth()) / 2;
        int top = (int) (getWidth() / 2 - getWidth() / 3 - getWidth() / 1.5f / 8);

        canvas.save();
        canvas.rotate(mDegress,getWidth() / 2, getWidth() / 2);
        canvas.drawBitmap(mInnerCircleBitmap,left,top,mPaint);
        canvas.restore();

        canvas.save();
        canvas.rotate(-mDegress,getWidth() / 2, getWidth() / 2);
        canvas.drawBitmap(mOutCircleBitmap,left,top,mPaint);
        canvas.restore();
    }

    public void resumeAnim(){
        if(valueAnimator == null){
            return;
        }
        if(valueAnimator.isStarted()){
//            valueAnimator.resume();
        }else {
            valueAnimator.start();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if(valueAnimator != null){
            valueAnimator.cancel();
        }
    }

    public void pauseAnim(){
        if(valueAnimator != null && valueAnimator.isRunning()){
//            valueAnimator.pause();
        }
    }
}

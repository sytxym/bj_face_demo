//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.aeye.sdk;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import com.aeye.android.config.AEModelMgr;
import com.aeye.android.face.UtilFacePre;
import com.aeye.android.uitls.BitmapUtils;
import com.aeye.face.AEFaceParam;
import com.aeye.face.uitls.MLog;

import java.util.ArrayList;
import java.util.Arrays;

public class AEFaceAlive {
    public static final String TAG = "AliveTAG";
    public static boolean USE_NOD = false;
    public static boolean MOHU_FILTER = true;
    public static boolean ZHENGLIAN_FILTER = true;
    public static boolean ZHEDANG_FILTER = true;
    public static boolean FILTER_FRAME = true;
    private static AEFaceAlive mInstance = null;
    private AEFaceAliveListener mListener = null;
    private int mCurPose = -1;
    private int mPoseNum = -1;
    private int mPoseCount = 0;
    private int mLevel = 2;
    private double[] poseValue = new double[]{1.0D};
    private double[] poseThreshold = null;
//    private final double[][] poseThresholdLevel = new double[][]{{45.0D, -14.0D, 14.0D, -14.0D, 7.0D, 1.88D, 0.2D}, {35.0D, -15.0D, 15.0D, -15.0D, 8.0D, 1.68D, 0.19D}, {25.0D, -16.0D, 16.0D, -16.0D, 10.0D, 1.52D, 0.17D}};
//    private final float[] faceThresholdLevel = new float[]{13.0F, -15.0F, 15.0F, -10.0F, 16.0F, 2.12F, 0.23F};
      private final double[][] poseThresholdLevel = new double[][]{{40.0D, -14.0D, 14.0D, -14.0D, 8.0D, 1.88D, 0.2D}, {35.0D, -15.0D, 15.0D, -15.0D, 9.0D, 1.68D, 0.2D}, {30.0D, -16.0D, 15.0D, -16.0D, 10.0D, 1.52D, 0.19D}};
      private final float[] faceThresholdLevel = new float[]{13.0F, -14.0F, 35.0F, -12.0F, 16.0F, 2.3F, 0.25F};
    private boolean mEnd = false;
    private boolean isFirst = true;
    public static final int POSE_FACE_LEFT = 1;
    public static final int POSE_FACE_RIGHT = 2;
    public static final int POSE_FACE_SHAKE = 2;
    public static final int POSE_FACE_UP = 3;
    public static final int POSE_FACE_DOWN = 4;
    public static final int POSE_FACE_NOD = 4;
    public static final int POSE_MOUTH_OPEN = 5;
    public static final int POSE_EYE_BLINK = 6;
    public static final int THRESHOLD_NUM = 7;
    public static final int POSE_MIN = 2;
    public static final int POSE_MAX = 5;
    public static final int ERROR_PARAM = -31;
    public static final int ERROR_END = -32;
    public static final int RETURN_NEXT = 10;
    public static final int LEN_LANDMARK = 166;
    private long hFaceRec = 0L;
    private boolean inited = false;
    private float[] mLandmark = new float[166];
    private float[] axis = new float[3];
    private float[] quality = new float[2];
    float[] axisX = new float[1];
    float[] axisY = new float[1];
    float[] mouth = new float[1];
    float[] eye = new float[1];
    float[] mask = new float[1];
    boolean faceDC = false;
    float faceDCValue = 0.0F;
    float offsetBig = 0.0F;
    final float DC_CHANGE = 3.3F;
    final int DC_FRAMES = 3;
//    final float lenPassY = 7.0F;
    final float lenPassY = 6.2F;
    ArrayList<AEFaceAlive.Frame> mBuff = new ArrayList();
    long DC_TIME = 300L;
    boolean full = false;
//    private double EYE_THRESH = 0.2D;
    private double EYE_THRESH = 0.22D;
    private int EYE_COUNT_FRAMES = 1;
    private int count;
    private int total;

    public AEFaceAlive() {
    }

    public static AEFaceAlive getInstance() {
        if (mInstance == null) {
            mInstance = new AEFaceAlive();
        }

        return mInstance;
    }

    private boolean init(Context context) {
        if (this.hFaceRec != 0L) {
            UtilFacePre.getInstance().FacePreDestroy(this.hFaceRec);
        }

        this.hFaceRec = UtilFacePre.getInstance().FacePreCreate();
        if (this.hFaceRec != 0L) {
            UtilFacePre.getInstance().FacePreUnserialize(this.hFaceRec, AEModelMgr.getModelFilePath(context, "facepre.param"), AEModelMgr.getModelFilePath(context, "facepre_model.bin"));
            this.inited = true;
            return true;
        } else {
            this.inited = false;
            Log.e("Alive", "init error!");
            return false;
        }
    }

    public long AEYE_Alive_InitVIS(Context context, Bundle paras) {
        AEModelMgr.loadModelData(context, "facepre_model", "bin");
        AEModelMgr.loadModelData(context, "facepre", "param");
        this.mEnd = false;
        return this.init(context) ? 0L : -1L;
    }

    public String AEYE_Alive_GetVersionVIS() {
        return UtilFacePre.getInstance().getVersion();
    }

    public void AEYE_Alive_setAliveParamVIS(int poseNum) {
        this.isFirst = true;
        this.mEnd = false;
        this.poseThreshold = null;
        this.mPoseNum = poseNum;
        this.mPoseCount = 0;
    }

    public void AEYE_Alive_setAliveParamVIS(int poseNum, int aliveLevel) {
        this.isFirst = true;
        this.mEnd = false;
        this.poseThreshold = null;
        this.mPoseNum = poseNum;
        this.mPoseCount = 0;
        if (aliveLevel >= 1 && aliveLevel <= this.poseThresholdLevel.length) {
            this.mLevel = aliveLevel;
        }

        this.poseThreshold = this.poseThresholdLevel[this.mLevel - 1];
    }

    public void AEYE_Alive_setAliveParamVIS(int poseNum, Bundle paras) {
        this.isFirst = true;
        this.mEnd = false;
        this.poseThreshold = null;
        this.mPoseNum = poseNum;
        this.mPoseCount = 0;
        if (paras != null && paras.containsKey("threshold")) {
            double[] threshold = paras.getDoubleArray("threshold");
            if (threshold.length == 7) {
                this.poseThreshold = threshold;
            }
        }

    }

    public void AEYE_Alive_setAliveListenerVIS(AEFaceAliveListener listener) {
        this.mListener = listener;
    }

    boolean isDCStatusY(AEFaceAlive.Frame cur) {
        int last = -1;

        int i;
        for(i = 0; i < this.mBuff.size() && cur.time - ((AEFaceAlive.Frame)this.mBuff.get(i)).time > this.DC_TIME; last = i++) {
        }

        if (last >= 0) {
            for(i = 0; i <= last && this.mBuff.size() >= 3; ++i) {
                this.mBuff.remove(0);
                this.full = true;
            }
        }

        this.mBuff.add(cur);
        if (this.full && Math.abs(cur.posY) < 20.0F) {
            for(i = 0; i < this.mBuff.size(); ++i) {
                Float left = ((AEFaceAlive.Frame)this.mBuff.get(i)).posY;

                for(int j = i + 1; j < this.mBuff.size(); ++j) {
                    Float right = ((AEFaceAlive.Frame)this.mBuff.get(j)).posY;
                    if (Math.abs(left - right) > 3.3F) {
                        return false;
                    }
                }
            }

            return true;
        } else {
            return false;
        }
    }

    public int judgePose(int pose, double[] output, float x, float y, float m, float eyeStatus) {
        int ret = -1;
        printLog(" judge pose : "+pose);
        printLog(" judge posevalue  : "+ Arrays.toString(output));
        switch(pose) {
            case 1:
                if (!this.faceDC) {
                    if (x > this.faceThresholdLevel[1]) {
                        this.faceDC = true;
                    }
                } else if ((double)x < this.poseThreshold[pose]) {
                    ret = 1;
                }

                output[0] = (double)x;
                break;
            case 2://摇头
                printLog("====this.faceDC:"+this.faceDC+" , this.faceThresholdLevel[2]: "+this.faceThresholdLevel[2]+" , this.poseThreshold[pose]: "+this.poseThreshold[pose]);
                if (Math.abs(y) >= 13.0F) {
                    this.faceDC = false;
                } else if (!this.faceDC) {
                    if (Math.abs(x) < this.faceThresholdLevel[2]) {
                        this.faceDC = true;
                    }
                } else if ((double)Math.abs(x) > this.poseThreshold[pose]) {
                    ret = 1;
                }

                printLog( "SHAKE x:" + x);
                output[0] = (double)x;
                break;
            case 3://抬头
                if (Math.abs(x) >= 13.0F || this.faceDC && y - this.faceDCValue > lenPassY) {
                    this.faceDC = false;
                } else if (this.isDCStatusY(new AEFaceAlive.Frame(y))) {
                    this.offsetBig = 1.0F;
                    if (y > this.faceThresholdLevel[4]) {
                        this.offsetBig = y - this.faceThresholdLevel[4] + 2.0F;
                    }

                    printLog( "DC Status");
                    this.faceDCValue = y;
                    this.faceDC = true;
                } else if (this.faceDC && this.faceDCValue - y > lenPassY + this.offsetBig) {
                    ret = 1;
                }

                output[0] = (double)y;
                printLog("UP  y:" + y + " x:" + x + " DC:" + this.faceDC);
                break;
            case 4://低头
                printLog("USE_NOD--"+USE_NOD +  "this.faceDC======="+this.faceDC+" , this.faceDCValue : "+this.faceDCValue+",this.offsetBig: "+this.offsetBig);
                if (USE_NOD) {
                    if (Math.abs(x) >= 13.0F) {
                        this.faceDC = false;
                    } else if (this.isDCStatusY(new AEFaceAlive.Frame(y))) {
                        printLog( "DC Status");
                        this.faceDCValue = y;
                        this.faceDC = true;
                    } else if (this.faceDC && (double)Math.abs(this.faceDCValue - y) > this.poseThreshold[pose]) {
                        ret = 1;
                    }

                    printLog( "NOD y:" + y);
                    output[0] = (double)y;
                } else {
                    if (Math.abs(x) >= 13.0F || this.faceDC && this.faceDCValue - y > lenPassY) {
                        this.faceDC = false;
                    } else if (this.isDCStatusY(new AEFaceAlive.Frame(y))) {
                        this.offsetBig = 0.0F;
                        if (y < this.faceThresholdLevel[3]) {
                            this.offsetBig = this.faceThresholdLevel[3] - y + 2.0F;
                        }

                        printLog( "DC Status");
                        this.faceDCValue = y;
                        this.faceDC = true;
                    } else if (this.faceDC && y - this.faceDCValue > lenPassY + this.offsetBig) {
                        ret = 1;
                    }

                    output[0] = (double)y;
                    printLog("DOWN y:" + y + " x:" + x + " DC:" + this.faceDC);
                }
                break;
            case 5://张嘴
                if (Math.abs(x) < 17.0F && Math.abs(y) < 18.0F) {
                    printLog("mouth   this.faceDC======="+this.faceDC+" , mouth : "+m+",poseThreshold[pose]+ "+this.poseThreshold[pose]);
                    if (!this.faceDC) {
                        if (m > this.faceThresholdLevel[5]) {
                            this.faceDC = true;
                        }
                    } else if ((double)m < this.poseThreshold[pose]) {
                        ret = 1;
                    }
                } else {
                    this.faceDC = false;
                }

                output[0] = (double)m;
                break;
            case 6://眨眼
                if (this.frameFilter2()) {
                    if (Math.abs(x) < 17.0F && Math.abs(y) < 18.0F) {
                        printLog("11111111this.faceDC======="+this.faceDC+" ,  faceThresholdLevel[6]: "+this.faceThresholdLevel[6]+" ,  poseThreshold[pose]: "+this.poseThreshold[pose]);
                        if (!this.faceDC) {
                            if (eyeStatus > this.faceThresholdLevel[6]) {
                                this.faceDC = true;
                                printLog("!this.faceDC======="+this.faceDC);
                            }
                        } else if ((double)eyeStatus < this.poseThreshold[pose]) {
                            ret = 1;
                        }
                    } else {
                        this.faceDC = false;
                    }
                }

                output[0] = (double)eyeStatus;
        }

        if (ret == 1) {
            this.total = 0;
            this.count = 0;
            this.faceDC = false;
            this.full = false;
            this.mBuff.clear();
            ++this.mPoseCount;
            if (this.mPoseCount >= this.mPoseNum) {
                ret = 0;
            }
        }

        return ret;
    }

    private int checkEye(float ear) {
        Log.e("AEFaceAlive", "checkEye ear=" + ear);
        if ((double)ear < this.EYE_THRESH) {
            ++this.count;
        } else {
            if (this.count >= this.EYE_COUNT_FRAMES) {
                ++this.total;
            }

            this.count = 0;
            Log.e("AEFaceAlive", "checkEye rest");
        }

        Log.e("AEFaceAlive", "checkEye count=" + this.count + ",total=" + this.total);
        return this.total;
    }

    private boolean frameFilter() {
        printLog("frameFilter --poseThreshold[0]： "+poseThreshold[0]);
        printLog("frameFilter -- this.mask[0]： "+ (double)this.mask[0]);
        boolean farameFilter = (double) this.quality[0] < this.poseThreshold[0] - 1.5D && this.axisY[0] > this.faceThresholdLevel[3] && this.axisY[0] < this.faceThresholdLevel[4] + 4.0F && this.axisX[0] > this.faceThresholdLevel[1] - 2.0F && this.axisX[0] < this.faceThresholdLevel[2] + 2.0F && (double) this.mask[0] < 0.7D;
        printLog("frameFilter : "+farameFilter);
        return farameFilter;
    }

    private boolean frameFilter2() {
        boolean isFrame2 = (double) this.quality[0] < this.poseThreshold[0] - 1.5D && this.axisY[0] > this.faceThresholdLevel[3] && this.axisY[0] < this.faceThresholdLevel[4] + 4.0F && this.axisX[0] > this.faceThresholdLevel[1] - 2.0F && this.axisX[0] < this.faceThresholdLevel[2] + 2.0F;
        printLog("frameFilter2 : "+isFrame2);
        return isFrame2;
    }

    public long AEYE_Alive_DetectVIS_Single(int[] image, int width, int height, Rect faceRect, Object reserve) {
        if (!this.inited) {
            this.isFirst = true;
            this.mEnd = false;
            return -32L;
        } else if (this.mEnd) {
            return -32L;
        } else {
            long ret = -1L;
            if (this.poseThreshold == null) {
                this.poseThreshold = this.poseThresholdLevel[this.mLevel - 1];
            }

            UtilFacePre.getInstance().FacePreTransform(this.hFaceRec, image, width, height, faceRect, this.mLandmark, this.axis, this.quality, this.axisX, this.axisY, this.mouth, this.eye, this.mask);
            printLog( "blur[" + this.quality[0] + "] axis[" + this.axis[0] + "]axisX[" + this.axisX[0] + "] axisY [" + this.axisY[0] + "] mask[" + this.mask[0] + "] mouth[" + this.mouth[0] + "]eye[+" + this.eye[0] + "]");
            if (this.isFirst) {
                if (this.frameFilter()) {
                    this.isFirst = false;
                    this.mBuff.clear();
                }
            } else {
                if (FILTER_FRAME && !this.frameFilter()) {
//                    LogFileUtil.saveLog("!frameFilter()");
                    return ret;
                }
                // modify from 50 to 55, 降低抬头和低头的难度
                if (this.quality[0] < 55.0F && (double)this.mask[0] < 0.7D) {
                    int status = this.judgePose(this.mCurPose, this.poseValue, this.axisX[0], this.axisY[0], this.mouth[0], this.eye[0]);
                    printLog("status : "+status+" , mCurPose : "+this.mCurPose);
                    if (status == -1) {
                        ret =-1;
                    } else if (status == 1) {
//                        if (this.mListener != null) {
//                            this.mCurPose = this.mListener.onAlivePose(this.mCurPose, this.poseValue[0], reserve);
//                        }
                        ret = 10L;
                    } else if (status == 0) {
                        ret =0l;
//                        this.mEnd = true;
                    }
                }
            }
            return ret;
        }
    }

    public long AEYE_Alive_DetectVIS(int[] image, int width, int height, Rect faceRect, Object reserve) {
        if (!this.inited) {
            this.isFirst = true;
            this.mEnd = false;
            return -32L;
        } else if (this.mEnd) {
            return -32L;
        } else {
            long ret = 0L;
            if (this.poseThreshold == null) {
                this.poseThreshold = this.poseThresholdLevel[this.mLevel - 1];
            }

            UtilFacePre.getInstance().FacePreTransform(this.hFaceRec, image, width, height, faceRect, this.mLandmark, this.axis, this.quality, this.axisX, this.axisY, this.mouth, this.eye, this.mask);
            float blur = this.quality[0];
            float axi = this.axis[0];
            float axisX = this.axisX[0];
            float axisY = this.axisY[0];
            printLog( "blur[" + blur + "] axis[" + axi + "]axisX[" + axisX + "] axisY [" + axisY + "] mask[" + this.mask[0] + "] mouth[" + this.mouth[0] + "]eye[+" + this.eye[0] + "]");
            boolean checkJingMoAlive = imageCheckJingMoAlive(image, width, height);
            boolean checkPose = checkPose(axi, axisX, axisY);
            if (this.isFirst && blur < 5.0F && checkJingMoAlive && checkPose) {
                if (this.frameFilter()) {
                    this.isFirst = false;
                    if (this.mListener != null) {
                        if (this.mPoseCount >= this.mPoseNum) {
                            this.mListener.onAliveFrame(0, (double) axisX, reserve);
                            this.mListener.onAliveFinish(0, reserve);
                        } else {
                            this.mCurPose = this.mListener.onAlivePose(0, 0.0D, reserve);
                            ret = 10L;
                        }
                    }

                    this.mBuff.clear();
                }
            } else {
                if (FILTER_FRAME && !this.frameFilter()) {
//                    LogFileUtil.saveLog("!frameFilter()");
                    return ret;
                }
                // modify from 50 to 55, 降低抬头和低头的难度
                if (blur < 5.0F && (double)this.mask[0] < 0.7D &&checkJingMoAlive && checkPose ) {
                    int status = this.judgePose(this.mCurPose, this.poseValue, axisX, axisY, this.mouth[0], this.eye[0]);
                    if (status == -1) {
                        if (this.mListener != null) {
                            this.mListener.onAliveFrame(this.mCurPose, this.poseValue[0], reserve);
                        }
                    } else if (status == 1) {
                        if (this.mListener != null) {
                            this.mCurPose = this.mListener.onAlivePose(this.mCurPose, this.poseValue[0], reserve);
                        }

                        ret = 10L;
                    } else if (status == 0) {
                        if (this.mListener != null) {
                            this.mListener.onAliveFrame(this.mCurPose, this.poseValue[0], reserve);
                            this.mListener.onAliveFinish(0, reserve);
                        }

                        this.mEnd = true;
                    }
                }
            }

            return ret;
        }

    }


    private boolean checkPose(float z, float x, float y){
        if(this.mCurPose == AEFaceAlive.POSE_FACE_SHAKE || this.mCurPose == AEFaceAlive.POSE_FACE_DOWN || this.mCurPose== AEFaceAlive.POSE_FACE_UP){
            return  true;
        }
        if(z<15 && x<15 && y<15){
            Log.e(TAG,"pose ok ");
            return  true;
        }
        Log.e(TAG,"pose error");
        return  false;
    }
    private void printInfoToLocal(String s) {
    }

    public void AEYE_Alive_SetPose(int pose) {
        this.mCurPose = pose;
    }

    public float[] AEYE_Alive_Quality(Bitmap image, Rect faceRect) {
        int width = image.getWidth();
        int height = image.getHeight();
        return this.AEYE_Alive_Quality(BitmapUtils.getBitmapData(image), width, height, faceRect);
    }

    public float[] AEYE_Alive_Quality(int[] image, int width, int height, Rect faceRect) {
        if (!this.inited) {
            this.isFirst = true;
            this.mEnd = false;
            return null;
        } else if (this.mEnd) {
            return null;
        } else {
            UtilFacePre.getInstance().FacePreTransform(this.hFaceRec, image, width, height, faceRect, this.mLandmark, this.axis, this.quality,
                    this.axisX, this.axisY, this.mouth, this.eye, this.mask);
            Log.d("Alive", "blur[" + this.quality[0] + "] axis[" + this.axis[0] + "] mask[" + this.mask[0] + "]");
            float[] result = new float[169];
            result[0] = this.quality[0];
            result[1] = this.axis[0];
            result[2] = this.mask[0];
            System.arraycopy(this.mLandmark, 0, result, 3, 166);
            return result;
        }
    }

    public float[] AEYE_Alive_QualityLight(Bitmap image, Rect faceRect) {
        int width = image.getWidth();
        int height = image.getHeight();
        return this.AEYE_Alive_QualityLight(BitmapUtils.getBitmapData(image), width, height, faceRect);
    }

    public float[] AEYE_Alive_QualityLight(int[] image, int width, int height, Rect faceRect) {
        if (!this.inited) {
            this.isFirst = true;
            this.mEnd = false;
            return null;
        } else {
            UtilFacePre.getInstance().FacePreTransform(this.hFaceRec, image, width, height, faceRect, this.mLandmark, this.axis, this.quality, this.axisX, this.axisY, this.mouth, this.eye, this.mask);
            Log.d("Alive", "blur[" + this.quality[0] + "] axis[" + this.axis[0] + "] mask[" + this.mask[0] + "]");
            float[] result = new float[169];
            result[0] = this.quality[0];
            result[1] = this.axis[0];
            result[2] = this.mask[0];
            System.arraycopy(this.mLandmark, 0, result, 3, 166);
            return result;
        }
    }

    public float AEYE_Alive_Quality_Blur(float[] quality) {
        if (quality == null) {
            throw new NullPointerException();
        } else {
            return quality[0];
        }
    }

    public float AEYE_Alive_Quality_Angle(float[] quality) {
        if (quality == null) {
            throw new NullPointerException();
        } else {
            return quality[1];
        }
    }

    public float AEYE_Alive_Quality_Mask(float[] quality) {
        if (quality == null) {
            throw new NullPointerException();
        } else {
            return quality[2];
        }
    }

    public float[] AEYE_Alive_Quality_Landmark(float[] quality) {
        if (quality == null) {
            throw new NullPointerException();
        } else {
            return Arrays.copyOfRange(quality, 3, 169);
        }
    }

    public int AEYE_Alive_DestoryVIS() {
        UtilFacePre.getInstance().FacePreDestroy(this.hFaceRec);
        this.hFaceRec = 0L;
        this.inited = false;
        Log.e(TAG,"AEYE_Alive_DestoryVIS   ");
        return 0;
    }

    private class Frame {
        float posY;
        long time;

        public Frame(float posY) {
            this.posY = posY;
            this.time = System.currentTimeMillis();
        }
    }


    private void printLog(String content){
        Log.e(TAG,content);
    }


    private boolean imageCheckJingMoAlive(int[]  image,int width ,int height) {
        long time = System.currentTimeMillis();
        float score = AEFaceUnhack.getInstance().AEYE_AliveUnhack_Detect(image,width,height);
        Log.e("TAG","1111 unhack alive : "+(System.currentTimeMillis() - time));
        MLog.d(TAG, "imageCheck score=" + score);
        return score >= 0.46;
    }
}

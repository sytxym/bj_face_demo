package com.aeye.face;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.os.Build;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import android.text.format.Formatter;
import android.util.Log;

//import com.aeye.aeyelib.AEyeAlive;
import com.aeye.aeyelib.AEyeLightAlive;
import com.aeye.android.config.AEModelMgr;
import com.aeye.android.config.ConfigData;
import com.aeye.face.config.FaceSdkHostParamBuilder;
import com.aeye.face.lightView.RecognizeLightActivity;
import com.aeye.face.uitls.ColorInfo;
import com.aeye.face.uitls.PictureManagerUtils;
import com.aeye.face.uitls.PictureManagerUtilsLight;
import com.aeye.face.view.RecognizeActivity;
import com.aeye.sdk.AEFaceAlive;
import com.aeye.sdk.AEFaceDetect;
import com.aeye.sdk.AEFaceQuality;
import com.aeye.sdk.AEFaceUnhack;
import com.sdk.core.BuildConfig;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AEFacePack {
    private String TAG = AEFacePack.class.getSimpleName();
    private static final String VERSION = "FCALDV221C004_HRXJ20230418";
    private static AEFacePack instance = null;

    public static final int THEME_FACE = 0;
    public static final int THEME_RECT = 1;

    public static final int SUCCESS = 0;
    public static final int ERROR_FAIL = -1;
    public static final int ERROR_TIMEOUT = -4;
    public static final int ERROR_CANCEL = -9;
    public static final int ERROR_CAMERA = -10;
    public static final int ERROR_DANGER_DEVICE = -11;//add by liulu  20230403 设备安全判断，防止摄像头劫持等
    /** 用户选择「其他核验方式」，应回到宿主认证方式选择页，非活体失败结果 */
    public static final int ERROR_OTHER_VERIFY = -12;

    private AEFaceInterface m_finishListener = null;
    private Context mAppContext;
    private String mHostHomeActivityClass;
    private final List<WeakReference<Activity>> mFaceFlowActivities = new ArrayList<>();
    private int mPicNum = 1;
    private int mAliveMotions = 2;
    private int m_RecogTime = 30;
    private int m_Theme = THEME_FACE;
    private boolean mOpenReturnBtn = false;
    private int m_TopColor = 0x00000000;
    private int mBottomColor = 0x00000000;
    private int mAliveSwitch = 0;
    private int mVoiceSwitch = 0;
    private int mQualitySwitch = 0;
    private int mCamId = CameraInfo.CAMERA_FACING_FRONT;
    private int mCamOri = 0;
    private int mCapOri = 0;
    private Bundle mParas;
    private int mFirstPose = 0;
    private int mLoseFace = 3;
    private int mCaptureFace = 3;
    private int mModelAllSide = 0;
    private int mMotionPicNum = 1;
    private int mMotionTime = 5;
    private int mFixMotion = 0;
    private int mAliveLevel = FaceSdkHostParamBuilder.DEFAULT_ALIVE_LEVEL;
    private int mShowFaceRect = 0;
    private int mNoticeTimeout = 1;
    private int mFaceStartTimer = 0;
    private int mSimpleAnim = 0;
    private int mShowPrepare = 1;
    private int mShowIntroduce = 1;
    private int mStrictMode = 0;
    private int mWhiteBackgroud = 0;
    private int mMaxBrightness = 0;
    private int mCaptureStraight = 0;
    private String mTitle = null;
    /** 信息确认页可选：活体失败说明文案 */
    private String mPendingFailDetail = null;
    /****
     * 闪光活体颜色
     */
    ColorInfo mColor1 = null,mColor2=null,mColor3=null;
    String colorSeq="";
    /***闪光活体颜色参数**/
    private int[] mAlivePose = null;

    private int mAliveMask = AEFaceParam.ALIVE_MASK_DEFAULT;
    private int mEncryptType = AEFaceParam.ENCRYPT_TYPE_NULL;
    private int mAliveType = AEFaceParam.ALIVE_TYPE_POSE;

    /**
     * 2026.2.9 增加人脸检测区域居中参数roi_center，只人脸在检测区域才采集*/
    private boolean mRoiCenterSwitch = true;

    /**是否横屏，默认是竖屏0, 横屏是1**/
    private int mIsLand = 0;

    private AEFacePack() {
    }

    public String AEYE_GetVersion() {
        return VERSION+ BuildConfig.versionName;
    }

    public static AEFacePack getInstance() {
        if (instance == null) {
            instance = new AEFacePack();
        }
        return instance;
    }

    public AEFaceInterface getInterface() {
        return m_finishListener;
    }

    public int AEYE_SetListener(AEFaceInterface listener) {
        m_finishListener = listener;
        return 0;
    }

    public boolean isOpenReturnButton() {
        return mOpenReturnBtn;
    }

    public boolean isNoticeTimeout() {
        return (mNoticeTimeout == 0) ? false : true;
    }

    public boolean isSimpleAnim() {
        return (mSimpleAnim == 0) ? false : true;
    }

    public boolean isShowPrepare() {
        return (mShowPrepare == 0) ? false : true;
    }

    public boolean isShowIntroduce() {
        return (mShowIntroduce == 0) ? false : true;
    }

    public boolean isStrictMode() {
        return (mStrictMode == 0) ? false : true;
    }

    public boolean isWhiteBackgroud() {
        return (mWhiteBackgroud == 0) ? false : true;
    }

    public boolean isMaxBrightness() {
        return (mMaxBrightness == 0) ? false : true;
    }

    public boolean isCaptureStraight() {
        return (mCaptureStraight == 0) ? false : true;
    }

    public boolean isLand() {
        return (mIsLand == 0) ? false : true;
    }

    public int getBottomColor() {
        return mBottomColor;
    }

    public int getAliveFirstMotion() {
        return mFirstPose;
    }

    public int getTheme() {
        return m_Theme;
    }

    public int getTopColor() {
        return m_TopColor;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setPendingFailDetail(String detail) {
        mPendingFailDetail = detail;
    }

    public String getPendingFailDetail() {
        return mPendingFailDetail;
    }

    /** 登记 SDK 人脸流程 Activity（确认页 / 活体页），便于一键结束 */
    public void registerFaceFlowActivity(Activity activity) {
        if (activity == null) {
            return;
        }
        synchronized (mFaceFlowActivities) {
            Iterator<WeakReference<Activity>> it = mFaceFlowActivities.iterator();
            while (it.hasNext()) {
                Activity ref = it.next().get();
                if (ref == null || ref == activity) {
                    it.remove();
                }
            }
            mFaceFlowActivities.add(new WeakReference<>(activity));
        }
    }

    public void unregisterFaceFlowActivity(Activity activity) {
        if (activity == null) {
            return;
        }
        synchronized (mFaceFlowActivities) {
            Iterator<WeakReference<Activity>> it = mFaceFlowActivities.iterator();
            while (it.hasNext()) {
                Activity ref = it.next().get();
                if (ref == null || ref == activity) {
                    it.remove();
                }
            }
        }
    }

    /** 结束当前人脸核验流程内所有 SDK 页面（确认页、活体页等） */
    public void finishAllFaceFlowActivities() {
        synchronized (mFaceFlowActivities) {
            for (WeakReference<Activity> ref : mFaceFlowActivities) {
                Activity activity = ref.get();
                if (activity != null && !activity.isFinishing()) {
                    activity.finish();
                }
            }
            mFaceFlowActivities.clear();
        }
    }

    /**
     * 「其他核验方式」：销毁人脸流程页面并回到宿主认证方式首页。
     */
    public void returnToHostAuthHome() {
        finishAllFaceFlowActivities();
        Class<?> home = resolveHostHomeClass();
        if (home == null || mAppContext == null) {
            return;
        }
        try {
            Intent intent = new Intent(mAppContext, home);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP
                    | Intent.FLAG_ACTIVITY_NEW_TASK);
            mAppContext.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "returnToHostAuthHome failed", e);
        }
    }

    private Class<?> resolveHostHomeClass() {
        if (mHostHomeActivityClass == null || mHostHomeActivityClass.isEmpty()) {
            return null;
        }
        try {
            return Class.forName(mHostHomeActivityClass);
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "HostHomeActivity not found: " + mHostHomeActivityClass, e);
            return null;
        }
    }

    public int getAliveMotions() {
        return mAliveMotions;
    }

    public int getCameraId() {
        return mCamId;
    }

    public int getMotionPicNum() {
        return mMotionPicNum;
    }

    public int[] getAlivePose() {
        return mAlivePose;
    }

    public boolean isAliveMask() {
        return mAliveMask == 1;
    }

    public int getEncryptType() {
        return mEncryptType;
    }
    public ColorInfo getmColor1() {
        return mColor1;
    }

    public void setmColor1(ColorInfo mColor1) {
        this.mColor1 = mColor1;
    }

    public ColorInfo getmColor2() {
        return mColor2;
    }

    public void setmColor2(ColorInfo mColor2) {
        this.mColor2 = mColor2;
    }

    public ColorInfo getmColor3() {
        return mColor3;
    }

    public void setmColor3(ColorInfo mColor3) {
        this.mColor3 = mColor3;
    }

    public String getColorSeq() {
        return colorSeq;
    }

    public void setColorSeq(String colorSeq) {
        this.colorSeq = colorSeq;
    }

    public boolean ismRoiCenterSwitch() {
        return mRoiCenterSwitch;
    }

    public void setmRoiCenterSwitch(boolean mRoiCenterSwitch) {
        this.mRoiCenterSwitch = mRoiCenterSwitch;
    }

    private void resetData() {
        mPicNum = 1;
        mAliveMotions = 2;
        m_RecogTime = 30;
        m_Theme = THEME_FACE;
        mOpenReturnBtn = false;
        m_TopColor = 0x00000000;
        mBottomColor = 0x00000000;
        mAliveSwitch = 1;
        mVoiceSwitch = 1;
        mQualitySwitch = 1;
        mCamId = CameraInfo.CAMERA_FACING_FRONT;
        mCamOri = 0;
        mCapOri = 0;
        mFirstPose = 0;
        mLoseFace = 3;
        mCaptureFace = 3;
        mModelAllSide = 0;
        mMotionTime = 5;
        mAliveLevel = FaceSdkHostParamBuilder.DEFAULT_ALIVE_LEVEL;
        mShowFaceRect = 0;
        mTitle = null;
        mFixMotion = 0;
        mNoticeTimeout = 1;
        mMotionPicNum = 1;
        mFaceStartTimer = 0;
        mSimpleAnim = 0;
        mShowPrepare = 1;
        mShowIntroduce = 0;
        mStrictMode = 0;
        mWhiteBackgroud = 0;
        mMaxBrightness = 0;
        mCaptureStraight = 0;

        mRoiCenterSwitch = true;

        mAlivePose = new int[]{2, 3, 4, 5, 6};

        mAliveMask = AEFaceParam.ALIVE_MASK_DEFAULT;
        mEncryptType = AEFaceParam.ENCRYPT_TYPE_NULL;
        mAliveType = AEFaceParam.ALIVE_TYPE_POSE;

    }

    public int AEYE_SetParameter(Bundle paras) {
        resetData();
        mParas = paras;

        if (mParas.containsKey(AEFaceParam.FetchImageNum)) {
            int picnum = mParas.getInt(AEFaceParam.FetchImageNum);
            setPictureNumber(picnum);
        }

        if (mParas.containsKey(AEFaceParam.AliveSwitch)) {
            mAliveSwitch = mParas.getInt(AEFaceParam.AliveSwitch);
        }

        if (mParas.containsKey(AEFaceParam.AliveMotionNum)) {
            int aliveTimes = mParas.getInt(AEFaceParam.AliveMotionNum);
            if (aliveTimes >= 0 && aliveTimes <= 5) {
                mAliveMotions = aliveTimes;
            }
        }

        if (mParas.containsKey(AEFaceParam.AliveFirstMotion)) {
            mFirstPose = mParas.getInt(AEFaceParam.AliveFirstMotion);
        }

        //连续多少张无人脸
        if (mParas.containsKey(AEFaceParam.ContinueFailDetectNum)) {
            mLoseFace = mParas.getInt(AEFaceParam.ContinueFailDetectNum);
        }

        //连续多少张人脸后开始采集图片
        if (mParas.containsKey(AEFaceParam.ContinueSuccessDetectNum)) {
            mCaptureFace = mParas.getInt(AEFaceParam.ContinueSuccessDetectNum);
        }

        //多角度建模
		/*if (mParas.containsKey(AEFaceParam.ModelMutiAngleSwitch)) {
			mModelAllSide = mParas.getInt(AEFaceParam.ModelMutiAngleSwitch);
			if (isModelAllSide() && isAliveOff()) {
				setPictureNumber(IDConstants.SIDE_NUM);
			}
		}*/

        if (mParas.containsKey(AEFaceParam.SingleAliveMotionTime)) {
            int recogTime = mParas.getInt(AEFaceParam.SingleAliveMotionTime);
            if (recogTime >= 1 && recogTime <= 60) {
                mMotionTime = recogTime;
            } else {
                mMotionTime = 8;
            }
        }

        if (mParas.containsKey(AEFaceParam.ModelOverTime)) {
            int recogTime = mParas.getInt(AEFaceParam.ModelOverTime);
            if (recogTime >= 10 && recogTime <= 60) {
                m_RecogTime = recogTime;
                //  如果有总超时，就用总超时，否则是单个动作时间
                mMotionTime = m_RecogTime;
            }
        }

        if (mParas.containsKey(AEFaceParam.ShowBackButton)) {
            int openBtn = mParas.getInt(AEFaceParam.ShowBackButton);
            if (openBtn == 1) {
                mOpenReturnBtn = true;
            } else if (openBtn == 0) {
                mOpenReturnBtn = false;
            }
        }

        if (mParas.containsKey(AEFaceParam.TitleTopBar)) {
            mTitle = mParas.getString(AEFaceParam.TitleTopBar);
        }

        if (mParas.containsKey(AEFaceParam.ColorTopBarBg)) {
            m_TopColor = mParas.getInt(AEFaceParam.ColorTopBarBg);
        }

        if (mParas.containsKey(AEFaceParam.ColorBottomBarBg)) {
            mBottomColor = mParas.getInt(AEFaceParam.ColorBottomBarBg);
        }

        if (mParas.containsKey(AEFaceParam.Theme)) {
            m_Theme = mParas.getInt(AEFaceParam.Theme);
        }

        if (mParas.containsKey(AEFaceParam.VoiceSwitch)) {
            mVoiceSwitch = mParas.getInt(AEFaceParam.VoiceSwitch);
        }

        if (mParas.containsKey(AEFaceParam.QualitySwitch)) {
            mQualitySwitch = mParas.getInt(AEFaceParam.QualitySwitch);
        }


        if (mParas.containsKey(AEFaceParam.CameraId)) {
            mCamId = mParas.getInt(AEFaceParam.CameraId);
            if (mCamId != CameraInfo.CAMERA_FACING_BACK &&
                    mCamId != CameraInfo.CAMERA_FACING_FRONT) {
                mCamId = CameraInfo.CAMERA_FACING_FRONT;
            }
        }

        if (mParas.containsKey(AEFaceParam.DisplayRotate)) {
            mCamOri = mParas.getInt(AEFaceParam.DisplayRotate);
        }

        if (mParas.containsKey(AEFaceParam.DecodeRotate)) {
            mCapOri = mParas.getInt(AEFaceParam.DecodeRotate);
        }

        if (mParas.containsKey(AEFaceParam.AliveLevel)) {
            mAliveLevel = mParas.getInt(AEFaceParam.AliveLevel);
        }

        if (mParas.containsKey(AEFaceParam.ShowFaceRect)) {
            mShowFaceRect = mParas.getInt(AEFaceParam.ShowFaceRect);
        }

        if (mParas.containsKey(AEFaceParam.AliveFixMotionSwitch)) {
            mFixMotion = mParas.getInt(AEFaceParam.AliveFixMotionSwitch);
        }

        if (mParas.containsKey(AEFaceParam.TimeoutNotice)) {
            mNoticeTimeout = mParas.getInt(AEFaceParam.TimeoutNotice);
        }

        if (mParas.containsKey(AEFaceParam.AliveMotionPicNum)) {
            mMotionPicNum = mParas.getInt(AEFaceParam.AliveMotionPicNum);
        }

        if (mParas.containsKey(AEFaceParam.FaceStartTimer)) {
            mFaceStartTimer = mParas.getInt(AEFaceParam.FaceStartTimer);
        }

        if (mParas.containsKey(AEFaceParam.ShowPrepare)) {
            mShowPrepare = mParas.getInt(AEFaceParam.ShowPrepare);
        }

        if (mParas.containsKey(AEFaceParam.SimpleAnim)) {
            mSimpleAnim = mParas.getInt(AEFaceParam.SimpleAnim);
        }

        if (mParas.containsKey(AEFaceParam.ShowIntroduce)) {
            mShowIntroduce = mParas.getInt(AEFaceParam.ShowIntroduce);
        }

        if (mParas.containsKey(AEFaceParam.StrictMode)) {
            mStrictMode = mParas.getInt(AEFaceParam.StrictMode);
        }

        if (mParas.containsKey(AEFaceParam.MaxBrightness)) {
            mMaxBrightness = mParas.getInt(AEFaceParam.MaxBrightness);
        }

        if (mParas.containsKey(AEFaceParam.WhiteBackgroud)) {
            mWhiteBackgroud = mParas.getInt(AEFaceParam.WhiteBackgroud);
        }

        if (mParas.containsKey(AEFaceParam.CaptureStraight)) {
            mCaptureStraight = mParas.getInt(AEFaceParam.CaptureStraight);
        }

        if (mParas.containsKey(AEFaceParam.AliveMotion)) {
            mAlivePose = mParas.getIntArray(AEFaceParam.AliveMotion);
        }
        if (mParas.containsKey(AEFaceParam.AliveMask)) {
            mAliveMask = mParas.getInt(AEFaceParam.AliveMask);
        }
        if (mParas.containsKey(AEFaceParam.EnCryptType)) {
            mEncryptType = mParas.getInt(AEFaceParam.EnCryptType);
        }
        if (mParas.containsKey(AEFaceParam.AliveType)) {
            mAliveType = mParas.getInt(AEFaceParam.AliveType);
        }


        if(mParas.containsKey(AEFaceParam.ColorInfo1)){
            mColor1 = (ColorInfo) mParas.getSerializable(AEFaceParam.ColorInfo1);
        }
        if(mParas.containsKey(AEFaceParam.ColorInfo2)){
            mColor2 = (ColorInfo) mParas.getSerializable(AEFaceParam.ColorInfo2);
        }
        if(mParas.containsKey(AEFaceParam.ColorInfo3)){
            mColor3 = (ColorInfo) mParas.getSerializable(AEFaceParam.ColorInfo3);
        }
        if(mParas.containsKey(AEFaceParam.Colorseq)){
            colorSeq =  mParas.getString(AEFaceParam.Colorseq);
        }

        if (mParas.containsKey(AEFaceParam.ROI_CenterSwitch)) {
            mRoiCenterSwitch = mParas.getBoolean(AEFaceParam.ROI_CenterSwitch);
        }

        if (mParas.containsKey(AEFaceParam.IS_LAND_Switch)) {
            mIsLand = mParas.getInt(AEFaceParam.IS_LAND_Switch);
        }

        if (mParas.containsKey(AEFaceParam.HostHomeActivity)) {
            mHostHomeActivityClass = mParas.getString(AEFaceParam.HostHomeActivity);
        }

        checkParam();
        return 0;
    }

    private void checkParam() {
//		if (!isModelAllSide() && !isAliveOff()) {
//			mMotionTime = m_RecogTime;
//		}
        if (!isAliveOff()) {
            setPictureNumber(getAliveMotions() + 1);
        }
    }

    public boolean isAlivePose() {
        return mAliveType == AEFaceParam.ALIVE_TYPE_POSE;
    }

    public boolean isAliveOff() {
        return (mAliveSwitch == 0) ? true : false;
    }

    public boolean isVoiceOff() {
        return (mVoiceSwitch == 0) ? true : false;
    }

    public boolean isQualityOff() {
        return (mQualitySwitch == 0) ? true : false;
    }

    public boolean isModelAllSide() {
        return (mModelAllSide == 0) ? false : true;
    }

    public boolean isShowFaceRect() {
        return (mShowFaceRect == 0) ? false : true;
    }

    public boolean isFixMotion() {
        return (mFixMotion == 0) ? false : true;
    }

    public boolean isSetDisplayOrientation() {
        if (mParas.containsKey(AEFaceParam.DisplayRotate)) {
            return true;
        }
        return false;
    }

    public int getDisplayOrientation() {
        return mCamOri;
    }

    public boolean isSetCaptureOrientation() {
        if (mParas.containsKey(AEFaceParam.DecodeRotate)) {
            return true;
        }
        return false;
    }

    public int getCaptureOrientation() {
        return mCapOri;
    }

    private void setPictureNumber(int pictureNum) {
        if (pictureNum < 0)
            mPicNum = 1;
        else
            mPicNum = pictureNum;
    }

    public int getPictureNumber() {
        return mPicNum;
    }

    public int getLoseFaceNum() {
        return mLoseFace;
    }

    public int getCaptureFace() {
        return mCaptureFace;
    }

    public int getAliveLevel() {
        return mAliveLevel;
    }

    public int AEYE_BeginRecog(Context context) {
        if (context == null)
            return 1;

        PictureManagerUtils.getPictureManager().setPicNum(mPicNum);

        int defaultId = -1;
        if (mParas.containsKey(AEFaceParam.CameraId)) {
            defaultId = getCameraId();
        } else {
            int numberOfCameras = Camera.getNumberOfCameras();

            if (numberOfCameras > 1)
                defaultId = CameraInfo.CAMERA_FACING_FRONT;
            else
                defaultId = CameraInfo.CAMERA_FACING_BACK;
        }

        int aliveMode = 0;
        if (mParas.containsKey(AEFaceParam.ALIVEMODE)) {
            aliveMode = mParas.getInt(AEFaceParam.ALIVEMODE);
        }
        if(aliveMode>0){
            PictureManagerUtilsLight.getPictureManager().resetPictureManager();
        }
        PictureManagerUtils.getPictureManager().resetPictureManager();
        //注释，此包只动作活体
//        if(aliveMode ==AEFaceParam.ALIVEMODE_MOTION) {
            Intent intent = new Intent(context, RecognizeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("Camera_Direction", defaultId);
            context.startActivity(intent);
//        }else if(aliveMode ==AEFaceParam.ALIVEMODE_LIGHT || aliveMode == AEFaceParam.ALIVEMODE_MOTION_LIGHT){
//            Intent intent = new Intent(context, RecognizeLightActivity.class);
//            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            intent.putExtra("Camera_Direction", defaultId);
//            intent.putExtra(AEFaceParam.ALIVEMODE,aliveMode);
//            context.startActivity(intent);
//        }
        return 0;
    }
    public int AEYE_BeginRecog(Activity activity, int requestCode) {
        if (activity == null)
            return 1;

        PictureManagerUtils.getPictureManager().setPicNum(mPicNum);

        int defaultId = -1;
        if (mParas.containsKey(AEFaceParam.CameraId)) {
            defaultId = getCameraId();
        } else {
            int numberOfCameras = Camera.getNumberOfCameras();

            if (numberOfCameras > 1)
                defaultId = CameraInfo.CAMERA_FACING_FRONT;
            else
                defaultId = CameraInfo.CAMERA_FACING_BACK;
        }
        PictureManagerUtils.getPictureManager().resetPictureManager();
        Intent intent = new Intent(activity, RecognizeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("Camera_Direction", defaultId);
        activity.startActivityForResult(intent, requestCode);
        return 0;
    }
    private void closeAndroidPDialog() {
        try {
            Class aClass = Class.forName("android.content.pm.PackageParser$Package");
            Constructor declaredConstructor = aClass.getDeclaredConstructor(String.class);
            declaredConstructor.setAccessible(true);
        } catch (Exception e) {
        }
        try {
            Class cls = Class.forName("android.app.ActivityThread");
            Method declaredMethod = cls.getDeclaredMethod("currentActivityThread");
            declaredMethod.setAccessible(true);
            Object activityThread = declaredMethod.invoke(null);
            Field mHiddenApiWarningShown = cls.getDeclaredField("mHiddenApiWarningShown");
            mHiddenApiWarningShown.setAccessible(true);
            mHiddenApiWarningShown.setBoolean(activityThread, true);
        } catch (Exception e) {
        }
    }

    public int AEYE_Init(Context context) {
        if (context == null)
            return 1;

        mAppContext = context.getApplicationContext();

//		context.startService(new Intent(context, InitService.class));
        if (Build.VERSION.SDK_INT >= 23) {
            List<String> mPermissionList = new ArrayList<>();
            String[] permissions = new String[]{
                    Manifest.permission.CAMERA,
//                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
//                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WAKE_LOCK
            };
            for (int i = 0; i < permissions.length; i++) {
                if (context.checkSelfPermission(permissions[i]) != PackageManager.PERMISSION_GRANTED) {
                    mPermissionList.add(permissions[i]);
                }
            }
            if (!mPermissionList.isEmpty()) {
                String[] permission = mPermissionList.toArray(new String[mPermissionList.size()]);
                ActivityCompat.requestPermissions((Activity) context, permission, 0);
                return 1;
            }
        }

        if (Build.VERSION.SDK_INT >= 28) {
            closeAndroidPDialog();
        }

		ConfigData.makeDestDir(context);

//        AEModelMgr.checkVersion(context, BuildConfig.BUILD_TIME);
        AEModelMgr.beforeInit(context);

        AEFaceDetect.getInstance().AEYE_FaceDetect_Init(context, null);
        AEFaceQuality.getInstance().AEYE_FaceQuality_Init(context, null);
        AEFaceAlive.getInstance().AEYE_Alive_InitVIS(context, null);
//		AEFaceRecog.getInstance().AEYE_FaceExtract_Init(this, null);

        AEFaceUnhack.getInstance().AEYE_AliveUnhack_Init(context, null);
//        AEyeAlive.getInstance().AEYE_AliveInit(context);
        AEModelMgr.afterInit(context);
        Log.e(TAG, "init");
        return 0;
    }

    public int AEYE_Destory(Context context) {
        if (context == null)
            return 1;

//		context.stopService(new Intent(context, InitService.class));
        AEFaceDetect.getInstance().AEYE_FaceDetect_Destory();
        AEFaceQuality.getInstance().AEYE_FaceQuality_Destory();
        AEFaceAlive.getInstance().AEYE_Alive_DestoryVIS();
        AEFaceUnhack.getInstance().AEYE_AliveUnhack_Destory();
        int ret = AEyeLightAlive.getInstance().AEYE_AliveDestroy();
        Log.e("LightAlive", "destroy t2=" + System.currentTimeMillis() + ",ret=" + ret);
        instance = null;
        Log.e(TAG, "destory");
        return 0;
    }

    public long getRecogTime() {
        return m_RecogTime;
    }

    public long getMotionTime() {
        return mMotionTime;
    }

    public boolean isUseGlobalTime() {
        return m_RecogTime == mMotionTime ? true : false;
    }

    public boolean isFaceAppearStartMode() {
        return (mFaceStartTimer == 0) ? false : true;
    }

    public int stopRecog() {
        return 0;
    }

    public boolean AEYE_EnvCheck(Context context, int threshold) {
        ConfigData.makeDestDir(context);
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        MemoryInfo mi = new MemoryInfo();
        am.getMemoryInfo(mi);
        if (threshold == 0) {
            threshold = 350 * 1024 * 1024;
        }
        if (Build.VERSION.SDK_INT >= 23) {
            List<String> mPermissionList = new ArrayList<>();
            String[] permissions = new String[]{
                    Manifest.permission.CAMERA,
//                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
//                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WAKE_LOCK
            };
            for (int i = 0; i < permissions.length; i++) {
                if (context.checkSelfPermission(permissions[i]) != PackageManager.PERMISSION_GRANTED) {
                    mPermissionList.add(permissions[i]);
                }
            }
            if (!mPermissionList.isEmpty()) {
                String[] permission = mPermissionList.toArray(new String[mPermissionList.size()]);
                ActivityCompat.requestPermissions((Activity) context, permission, 0);
                return false;
            }
        }

        if (threshold < 0) {
            return true;
        }

        if (mi.availMem < threshold) {
            return false;
        }

        return true;
    }

    private String getAvailMemory(Context context) {// 获取android当前可用内存大小
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        MemoryInfo mi = new MemoryInfo();
        am.getMemoryInfo(mi);
        //mi.availMem; 当前系统的可用内存  

        return Formatter.formatFileSize(context, mi.availMem);// 将获取的内存大小规格化  
    }

    private String getTotalMemory(Context context) {
        String str1 = "/proc/meminfo";// 系统内存信息文件  
        String str2;
        String[] arrayOfString;
        long initial_memory = 0;

        try {
            FileReader localFileReader = new FileReader(str1);
            BufferedReader localBufferedReader = new BufferedReader(
                    localFileReader, 8192);
            str2 = localBufferedReader.readLine();// 读取meminfo第一行，系统总内存大小  

            arrayOfString = str2.split("\\s+");
            for (String num : arrayOfString) {
                Log.i(str2, num + "\t");
            }

            initial_memory = Integer.valueOf(arrayOfString[1]).intValue() * 1024;// 获得系统总内存，单位是KB，乘以1024转换为Byte  
            localBufferedReader.close();

        } catch (IOException e) {
        }
        return Formatter.formatFileSize(context, initial_memory);// Byte转换为KB或者MB，内存大小规格化  
    }

    //   ┏┓　　　┏┓
    // ┏┛┻━━━┛┻┓
    // ┃　　　　　　　┃ 　
    // ┃　　　━　　　┃
    // ┃　┳┛　┗┳　┃
    // ┃　　　　　　　┃
    // ┃　　　┻　　　┃
    // ┃　　　　　　　┃
    // ┗━┓　　　┏━┛
    //     ┃　　　┃ 神兽保佑　　　　　　　　
    //     ┃　　　┃ 代码无BUG！
    //     ┃　　　┗━━━┓
    //     ┃　　　　　　　┣┓
    //     ┃　　　　　　　┏┛
    //     ┗┓┓┏━┳┓┏┛
    //       ┃┫┫　┃┫┫
    //       ┗┻┛　┗┻┛

};
package com.aeye.face.config;

import android.os.Bundle;

import com.aeye.face.AEFaceParam;

/**
 * 宿主启动活体时的通用 SDK 参数（与动作配置无关的固定项）。
 */
public final class FaceSdkHostParamBuilder {

    private static final int DEFAULT_MOTION_TIMEOUT_SEC = 15;
    /** 连续多少帧未检测到人脸才提示（原 3 帧约 100ms，易误触；15 帧约 0.5~1s） */
    private static final int DEFAULT_LOST_FACE = 15;
    private static final int DEFAULT_ALIVE_LEVEL = 1;

    private FaceSdkHostParamBuilder() {
    }

    /**
     * @param hostHomeActivity 活体结束后返回的 Activity 全类名
     * @param onlyAlive          true=仅活体
     */
    public static Bundle buildBase(String hostHomeActivity, boolean onlyAlive) {
        Bundle paras = new Bundle();
        paras.putInt(AEFaceParam.SingleAliveMotionTime, DEFAULT_MOTION_TIMEOUT_SEC);
        paras.putInt(AEFaceParam.AliveSwitch, onlyAlive ? 1 : 0);
        paras.putInt(AEFaceParam.VoiceSwitch, 1);
        paras.putInt(AEFaceParam.ShowIntroduce, 0);
        paras.putInt(AEFaceParam.QualitySwitch, 1);
        paras.putInt(AEFaceParam.AliveLevel, DEFAULT_ALIVE_LEVEL);
        paras.putInt(AEFaceParam.AliveFirstMotion, 0);
        paras.putInt(AEFaceParam.ContinueSuccessDetectNum, 3);
        paras.putInt(AEFaceParam.ContinueFailDetectNum, DEFAULT_LOST_FACE);
        paras.putInt(AEFaceParam.StrictMode, 1);
        paras.putInt(AEFaceParam.WhiteBackgroud, 1);
        paras.putInt(AEFaceParam.FaceStartTimer, 1);
        paras.putInt(AEFaceParam.SimpleAnim, 1);
        paras.putInt(AEFaceParam.ShowPrepare, 0);
        paras.putInt(AEFaceParam.ShowBackButton, 1);
        paras.putString(AEFaceParam.TitleTopBar, "");
        paras.putInt(AEFaceParam.AliveMask, 0);
        paras.putInt(AEFaceParam.EnCryptType, 0);
        paras.putInt(AEFaceParam.ALIVEMODE, AEFaceParam.ALIVEMODE_MOTION);
        paras.putBoolean(AEFaceParam.ROI_CenterSwitch, false);
        paras.putInt(AEFaceParam.IS_LAND_Switch, 0);
        paras.putString(AEFaceParam.HostHomeActivity, hostHomeActivity);
        return paras;
    }
}

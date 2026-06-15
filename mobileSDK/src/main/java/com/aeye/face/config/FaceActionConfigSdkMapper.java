package com.aeye.face.config;

import android.os.Bundle;

import com.aeye.face.AEFaceParam;
import com.aeye.sdk.AEFaceAlive;

/**
 * 将 {@link FaceActionConfig} 映射为 SDK 活体动作相关 Bundle 参数（供宿主在启动活体前合并进总参数）。
 */
public final class FaceActionConfigSdkMapper {

    private static final int[] ALL_MOTION_IDS = {
            AEFaceAlive.POSE_FACE_SHAKE,
            AEFaceAlive.POSE_FACE_UP,
            AEFaceAlive.POSE_FACE_DOWN,
            AEFaceAlive.POSE_MOUTH_OPEN,
            AEFaceAlive.POSE_EYE_BLINK
    };

    private FaceActionConfigSdkMapper() {
    }

    /**
     * 写入动作活体相关参数：AliveSwitch、AliveFixMotionSwitch、AliveMotionNum、AliveMotion。
     */
    public static void applyToBundle(Bundle paras, FaceActionConfig config) {
        if (paras == null || config == null) {
            return;
        }
        paras.putInt(AEFaceParam.AliveSwitch, 1);
        paras.putInt(AEFaceParam.ALIVEMODE, AEFaceParam.ALIVEMODE_MOTION);
        boolean fixedPool = config.isSequenceActionType();
        paras.putInt(AEFaceParam.AliveFixMotionSwitch, fixedPool ? 1 : 0);
        int motionNum = Math.max(0, Math.min(5, config.getActionCount()));
        paras.putInt(AEFaceParam.AliveMotionNum, motionNum);
        if (fixedPool) {
            int[] motions = buildFixedPoolMotionIds(config);
            if (motions.length > 0) {
                paras.putIntArray(AEFaceParam.AliveMotion, motions);
            }
        }
    }

    /** 固定顺序：抬头→低头→摇头→张嘴→眨眼 */
    public static int[] buildFixedPoolMotionIds(FaceActionConfig config) {
        java.util.ArrayList<Integer> list = new java.util.ArrayList<>();
        if (config.isEnableLookUp()) {
            list.add(AEFaceAlive.POSE_FACE_UP);
        }
        if (config.isEnableLookDown()) {
            list.add(AEFaceAlive.POSE_FACE_DOWN);
        }
        if (config.isEnableShakeHead()) {
            list.add(AEFaceAlive.POSE_FACE_SHAKE);
        }
        if (config.isEnableOpenMouth()) {
            list.add(AEFaceAlive.POSE_MOUTH_OPEN);
        }
        if (config.isEnableBlink()) {
            list.add(AEFaceAlive.POSE_EYE_BLINK);
        }
        if (list.isEmpty()) {
            return ALL_MOTION_IDS.clone();
        }
        int[] arr = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            arr[i] = list.get(i);
        }
        return arr;
    }
}

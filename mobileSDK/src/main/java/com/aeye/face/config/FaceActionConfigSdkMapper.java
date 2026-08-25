package com.aeye.face.config;

import android.os.Bundle;
import android.text.TextUtils;

import com.aeye.face.AEFaceParam;
import com.aeye.sdk.AEFaceAlive;

/**
 * 将 {@link FaceActionConfig} 映射为 SDK 活体相关 Bundle 参数（供宿主在启动活体前合并进总参数）。
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
     * 写入活体模式与动作参数：AliveSwitch、ALIVEMODE、AliveFixMotionSwitch、AliveMotionNum、AliveMotion。
     */
    public static void applyToBundle(Bundle paras, FaceActionConfig config) {
        if (paras == null || config == null) {
            return;
        }
        int aliveMode = mapDetectTypeToAliveMode(config.getDetectType());
        paras.putInt(AEFaceParam.ALIVEMODE, aliveMode);
        if (aliveMode == AEFaceParam.ALIVEMODE_SILENT) {
            paras.putInt(AEFaceParam.AliveSwitch, 0);
            return;
        }
        paras.putInt(AEFaceParam.AliveSwitch, 1);
        // 纯炫彩不写动作池；动作 / 动作+炫彩写入动作参数（actionCount 最多 5）
        if (aliveMode == AEFaceParam.ALIVEMODE_LIGHT) {
            return;
        }
        boolean fixedPool = config.isSequenceActionType();
        paras.putInt(AEFaceParam.AliveFixMotionSwitch, fixedPool ? 1 : 0);
        int motionNum = Math.max(0, Math.min(5, config.getActionCount()));
        // 动作+炫彩与纯动作共用 actionCount（最多 5）；至少 1 个，避免 0 导致无法进入炫彩
        if (aliveMode == AEFaceParam.ALIVEMODE_MOTION_LIGHT) {
            motionNum = Math.max(1, motionNum);
        }
        paras.putInt(AEFaceParam.AliveMotionNum, motionNum);
        if (fixedPool) {
            int[] motions = buildFixedPoolMotionIds(config);
            if (motions.length > 0) {
                paras.putIntArray(AEFaceParam.AliveMotion, motions);
            }
        }
    }

    /**
     * 配置接口 {@code detectType} → {@link AEFaceParam} ALIVEMODE。仅认后台数字码：
     * <ul>
     *   <li>1 → 静默</li>
     *   <li>2 / 空 → 动作</li>
     *   <li>3 → 炫彩</li>
     *   <li>4 → 动作+炫彩</li>
     * </ul>
     */
    public static int mapDetectTypeToAliveMode(String detectType) {
        if (TextUtils.isEmpty(detectType)) {
            return AEFaceParam.ALIVEMODE_MOTION;
        }
        switch (detectType.trim()) {
            case "1":
                return AEFaceParam.ALIVEMODE_SILENT;
            case "3":
                return AEFaceParam.ALIVEMODE_LIGHT;
            case "4":
                return AEFaceParam.ALIVEMODE_MOTION_LIGHT;
            case "2":
            default:
                return AEFaceParam.ALIVEMODE_MOTION;
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

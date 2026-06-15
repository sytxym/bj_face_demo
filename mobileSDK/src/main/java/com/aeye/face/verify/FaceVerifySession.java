package com.aeye.face.verify;

import android.text.TextUtils;

/**
 * 当前核验会话上下文：由 {@link com.aeye.face.AEFaceVerifyFlow} 在启动流程时写入，
 * 活体结束后 SDK 内部调用人脸核验接口、日志上报时使用。
 */
public final class FaceVerifySession {

    /** 宿主 App 直接发起（非扫码）时使用的演示 authRecordId */
    public static final String DEMO_AUTH_RECORD_ID = "102458";

    private static String userId;
    private static String authRecordId;
    private static String businessCode;
    private static boolean endLogSent;

    private FaceVerifySession() {
    }

    public static void begin(String verifyUserId, String verifyAuthRecordId, String verifyBusinessCode) {
        userId = verifyUserId;
        authRecordId = verifyAuthRecordId;
        businessCode = verifyBusinessCode;
        endLogSent = false;
    }

    public static void clear() {
        userId = null;
        authRecordId = null;
        businessCode = null;
        endLogSent = false;
    }

    public static void resetEndLogSent() {
        endLogSent = false;
    }

    /** @return true 表示本次会话尚未上报结束日志，并已标记为已上报 */
    public static boolean tryMarkEndLogSent() {
        if (endLogSent) {
            return false;
        }
        endLogSent = true;
        return true;
    }

    public static String getUserId() {
        return userId;
    }

    public static String getBusinessCode() {
        return businessCode;
    }

    /**
     * 扫码场景使用二维码中的 authIdentRecordId；宿主 App 直启时使用演示默认值。
     */
    public static String getAuthRecordId() {
        if (!TextUtils.isEmpty(authRecordId)) {
            return authRecordId;
        }
        return DEMO_AUTH_RECORD_ID;
    }
}

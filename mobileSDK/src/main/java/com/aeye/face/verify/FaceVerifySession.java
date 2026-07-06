package com.aeye.face.verify;

import android.text.TextUtils;

/**
 * 当前核验会话上下文：由 {@link com.aeye.face.AEFaceVerifyFlow} 在启动流程时写入，
 * 活体结束后 SDK 内部调用人脸核验接口、日志上报时使用。
 */
public final class FaceVerifySession {

    private static String userId;
    private static String authRecordId;
    private static String businessCode;
    private static boolean authRecordIdFromHost;
    private static boolean endLogSent;
    /**
     * 本地核验模式：仅做本地活体检测，不调用任何我方后台接口
     * （配置拉取、信息预览、insertRecord、人脸核验、日志上报、二维码状态更新均跳过），
     * 活体完成后直接把结果与人脸图片数组回调给宿主，由宿主自行对接第三方接口。
     */
    private static boolean localVerifyOnly;

    private FaceVerifySession() {
    }

    public static void begin(String verifyUserId, String verifyAuthRecordId, String verifyBusinessCode) {
        begin(verifyUserId, verifyAuthRecordId, verifyBusinessCode, false);
    }

    public static void begin(String verifyUserId, String verifyAuthRecordId,
                             String verifyBusinessCode, boolean localOnly) {
        userId = verifyUserId;
        authRecordId = verifyAuthRecordId;
        businessCode = verifyBusinessCode;
        authRecordIdFromHost = !TextUtils.isEmpty(verifyAuthRecordId);
        endLogSent = false;
        localVerifyOnly = localOnly;
    }

    public static void clear() {
        userId = null;
        authRecordId = null;
        businessCode = null;
        authRecordIdFromHost = false;
        endLogSent = false;
        localVerifyOnly = false;
    }

    /** 是否为「仅本地核验、不调用我方接口」模式。 */
    public static boolean isLocalVerifyOnly() {
        return localVerifyOnly;
    }

    /** 扫码等场景由宿主传入 authRecordId 时为 true，无需调用 insertRecord，且才上报 updateRecord。 */
    public static boolean isAuthRecordIdFromHost() {
        return authRecordIdFromHost;
    }

    public static void setAuthRecordId(String verifyAuthRecordId) {
        authRecordId = verifyAuthRecordId;
    }

    public static void setUserId(String verifyUserId) {
        userId = verifyUserId;
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

    /** 认证记录 ID：扫码传入或 insertRecord 返回。 */
    public static String getAuthRecordId() {
        return authRecordId;
    }
}

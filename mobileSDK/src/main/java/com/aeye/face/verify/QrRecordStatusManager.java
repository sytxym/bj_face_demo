package com.aeye.face.verify;

import android.text.TextUtils;

import com.aeye.face.AEFaceSdk;
import com.aeye.face.api.FaceApiService;

import org.json.JSONObject;

/**
 * 更新后台二维码认证记录状态（{@code /qrCode/updateRecord}），异步上报，不处理返回业务。
 * <p>仅扫码场景上报：宿主启动时传入 {@code authRecordId} 时才会调用；直启人脸（由 insertRecord 创建记录）不上报。</p>
 * <p>{@code failedType} 仅在以下场景传入，其它场景不传该字段：</p>
 * <ul>
 *   <li>异常退出 {@link QrRecordStatus#ABNORMAL_EXIT} → {@link QrRecordStatus.FailedType#CANCELLED}</li>
 *   <li>动作活体检测未通过 3 次 → {@link QrRecordStatus#NOT_PASS} + {@link QrRecordStatus.FailedType#LIVENESS_ACTION}</li>
 * </ul>
 */
public final class QrRecordStatusManager {

    private QrRecordStatusManager() {
    }

    /**
     * @param isPass {@link QrRecordStatus} 中除 {@code WAIT_SCAN} 外的状态值（不带 failedType）
     */
    public static void update(String isPass) {
        update(isPass, null);
    }

    /**
     * @param isPass     {@link QrRecordStatus} 中除 {@code WAIT_SCAN} 外的状态值
     * @param failedType {@link QrRecordStatus.FailedType}；为 null/空则不传该字段
     */
    public static void update(String isPass, String failedType) {
        if (FaceVerifySession.isLocalVerifyOnly()) {
            return;
        }
        if (!FaceVerifySession.isAuthRecordIdFromHost()) {
            return;
        }
        if (QrRecordStatus.WAIT_SCAN.equals(isPass)) {
            return;
        }
        final String authRecordId = FaceVerifySession.getAuthRecordId();
        if (TextUtils.isEmpty(authRecordId)) {
            return;
        }
        final String pass = isPass;
        final String failType = TextUtils.isEmpty(failedType) ? null : failedType;
        new Thread(() -> {
            try {
                AEFaceSdk.ensureInitialized();
                JSONObject body = new JSONObject();
                body.put("authRecordId", authRecordId);
                body.put("isPass", pass);
                if (failType != null) {
                    body.put("failedType", failType);
                }
                FaceApiService.updateQrCodeRecord(
                        AEFaceSdk.getApiBaseUrl(), body.toString());
            } catch (Exception ignored) {
                // 状态更新失败不影响主流程
            }
        }, "AEFace-QrStatus").start();
    }
}

package com.aeye.face.verify;

import android.text.TextUtils;

import com.aeye.face.AEFaceSdk;
import com.aeye.face.api.FaceApiService;

import org.json.JSONObject;

/**
 * 更新后台认证记录（任务）状态（{@code /qrCode/updateRecord}），异步上报，不处理返回业务。
 * <p>扫码或直启（宿主已传入 {@code authRecordId}）均上报；
 * {@code status=0}（待扫码）客户端不上报。本地核验不上报。</p>
 * <p>{@code failedType} 仅在以下场景传入，其它场景不传该字段：</p>
 * <ul>
 *   <li>用户返回取消 {@link QrRecordStatus#TASK_CANCELLED} → {@link QrRecordStatus.FailedType#CANCELLED}</li>
 *   <li>动作活体检测未通过 3 次 → {@link QrRecordStatus#NOT_PASS} + {@link QrRecordStatus.FailedType#LIVENESS_ACTION}</li>
 * </ul>
 */
public final class QrRecordStatusManager {

    private QrRecordStatusManager() {
    }

    /**
     * @param status {@link QrRecordStatus} 中除 {@code WAIT_SCAN} 外的状态值（不带 failedType）
     */
    public static void update(String status) {
        update(status, null);
    }

    /**
     * @param status     {@link QrRecordStatus} 中除 {@code WAIT_SCAN} 外的状态值
     * @param failedType {@link QrRecordStatus.FailedType}；为 null/空则不传该字段
     */
    public static void update(String status, String failedType) {
        if (FaceVerifySession.isLocalVerifyOnly()) {
            return;
        }
        if (!shouldReport()) {
            return;
        }
        // status=0 待扫码：扫码、直启均不上报
        if (QrRecordStatus.WAIT_SCAN.equals(status)) {
            return;
        }
        final String authRecordId = FaceVerifySession.getAuthRecordId();
        if (TextUtils.isEmpty(authRecordId)) {
            return;
        }
        final String statusValue = status;
        final String failType = TextUtils.isEmpty(failedType) ? null : failedType;
        new Thread(() -> {
            try {
                AEFaceSdk.ensureInitialized();
                JSONObject body = new JSONObject();
                body.put("authRecordId", authRecordId);
                body.put("status", statusValue);
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

    /** 扫码，或直启且宿主传入了认证记录 ID。 */
    private static boolean shouldReport() {
        return FaceVerifySession.isQrScanFlow()
                || FaceVerifySession.isAuthRecordIdFromHost();
    }
}

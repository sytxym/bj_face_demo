package com.aeye.face.verify;

import android.text.TextUtils;

import com.aeye.face.AEFaceSdk;
import com.aeye.face.api.FaceApiService;

import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 更新后台认证记录（任务）状态（{@code /qrCode/updateRecord}），异步上报，不处理返回业务。
 * <p>扫码或直启（宿主已传入 {@code authRecordId}）均上报；
 * {@code status=0}（待扫码）客户端不上报。本地核验不上报。</p>
 * <p>{@code failedType} 仅在以下场景传入，其它场景不传该字段：</p>
 * <ul>
 *   <li>用户返回取消 {@link QrRecordStatus#TASK_CANCELLED} → {@link QrRecordStatus.FailedType#CANCELLED}</li>
 *   <li>动作活体检测未通过 3 次 → {@link QrRecordStatus#NOT_PASS} + {@link QrRecordStatus.FailedType#LIVENESS_ACTION}</li>
 * </ul>
 * <p>{@code status=2}（异常退出）在 queryVerifyResult 轮询 5 次仍未得到 4/5 时上报，不传 failedType。</p>
 */
public final class QrRecordStatusManager {

    /** 已上报终态（2/4/5/6），避免失败页或 onDestroy 再覆盖 */
    private static final AtomicBoolean sTerminalReported = new AtomicBoolean(false);

    private QrRecordStatusManager() {
    }

    public static boolean isTerminalReported() {
        return sTerminalReported.get();
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
        if (QrRecordStatus.VERIFYING.equals(status) || QrRecordStatus.SCAN_DONE.equals(status)) {
            sTerminalReported.set(false);
        } else if (isTerminalStatus(status)) {
            sTerminalReported.set(true);
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

    private static boolean isTerminalStatus(String status) {
        return QrRecordStatus.ABNORMAL_EXIT.equals(status)
                || QrRecordStatus.NOT_PASS.equals(status)
                || QrRecordStatus.PASSED.equals(status)
                || QrRecordStatus.TASK_CANCELLED.equals(status);
    }

    /** 扫码，或直启且宿主传入了认证记录 ID。 */
    private static boolean shouldReport() {
        return FaceVerifySession.isQrScanFlow()
                || FaceVerifySession.isAuthRecordIdFromHost()
                || !TextUtils.isEmpty(FaceVerifySession.getAuthRecordId());
    }
}

package com.aeye.face.verify;

import android.text.TextUtils;

import com.aeye.face.AEFaceSdk;
import com.aeye.face.api.FaceApiService;

import org.json.JSONObject;

/**
 * 更新后台二维码认证记录状态（{@code /qrCode/updateRecord}），异步上报，不处理返回业务。
 * <p>仅扫码场景上报：宿主启动时传入 {@code authRecordId} 时才会调用；直启人脸（由 insertRecord 创建记录）不上报。</p>
 */
public final class QrRecordStatusManager {

    private QrRecordStatusManager() {
    }

    /**
     * @param isPass {@link QrRecordStatus} 中除 {@code WAIT_SCAN} 外的状态值
     */
    public static void update(String isPass) {
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
        new Thread(() -> {
            try {
                AEFaceSdk.ensureInitialized();
                JSONObject body = new JSONObject();
                body.put("authRecordId", authRecordId);
                body.put("isPass", isPass);
                FaceApiService.updateQrCodeRecord(
                        AEFaceSdk.getApiBaseUrl(), body.toString());
            } catch (Exception ignored) {
                // 状态更新失败不影响主流程
            }
        }, "AEFace-QrStatus").start();
    }
}

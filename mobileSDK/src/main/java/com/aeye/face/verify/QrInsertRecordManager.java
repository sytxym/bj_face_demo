package com.aeye.face.verify;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.aeye.face.AEFaceSdk;
import com.aeye.face.api.FaceApiService;
import com.aeye.face.api.model.QrInsertRecordResult;
import com.aeye.face.config.FaceActionConfig;
import com.aeye.face.config.FaceActionConfigManager;
import com.aeye.face.confirm.InfoConfirmPayload;
import com.aeye.face.confirm.InfoConfirmRepository;

/**
 * 新增二维码认证记录：非扫码场景在预览前创建 authRecordId。
 */
public final class QrInsertRecordManager {

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public interface Callback {
        void onSuccess(QrInsertRecordResult result);

        void onError(String message);
    }

    private QrInsertRecordManager() {
    }

    /**
     * 使用已缓存的动作配置、预览信息与设备信息调用 {@code /qrCode/insertRecord}。
     */
    public static void insert(Context context, Callback callback) {
        if (context == null) {
            postError(callback, "Context 为空");
            return;
        }
        final Context appContext = context.getApplicationContext();
        new Thread(() -> {
            try {
                AEFaceSdk.ensureInitialized();
                FaceActionConfig config = FaceActionConfigManager.getCached();
                if (config == null) {
                    throw new IllegalStateException("动作配置未加载");
                }
                InfoConfirmPayload preview = InfoConfirmRepository.getCached();
                String certNo = preview != null ? preview.getIdNumber() : null;
                String body = FaceApiService.buildInsertRecordRequestJson(
                        appContext,
                        config,
                        FaceVerifySession.getUserId(),
                        certNo);
                QrInsertRecordResult result = FaceApiService.insertQrCodeRecord(
                        AEFaceSdk.getApiBaseUrl(), body);
                if (TextUtils.isEmpty(result.getAuthRecordId())) {
                    throw new IllegalArgumentException("authRecordId 为空");
                }
                FaceVerifySession.setAuthRecordId(result.getAuthRecordId());
                if (!TextUtils.isEmpty(result.getUserId())) {
                    FaceVerifySession.setUserId(result.getUserId());
                }
                postSuccess(callback, result);
            } catch (Exception e) {
                if (AEFaceSdk.isUseMockOnError()) {
                    try {
                        QrInsertRecordResult mock = FaceApiService.mockInsertRecord(
                                FaceVerifySession.getUserId());
                        FaceVerifySession.setAuthRecordId(mock.getAuthRecordId());
                        if (!TextUtils.isEmpty(mock.getUserId())) {
                            FaceVerifySession.setUserId(mock.getUserId());
                        }
                        postSuccess(callback, mock);
                    } catch (Exception mockError) {
                        postError(callback, mockError.getMessage());
                    }
                } else {
                    postError(callback, e.getMessage() != null
                            ? e.getMessage() : "新增认证记录失败");
                }
            }
        }, "AEFace-InsertRecord").start();
    }

    private static void postSuccess(final Callback callback, final QrInsertRecordResult result) {
        MAIN.post(() -> {
            if (callback != null) {
                callback.onSuccess(result);
            }
        });
    }

    private static void postError(final Callback callback, final String message) {
        MAIN.post(() -> {
            if (callback != null) {
                callback.onError(message);
            }
        });
    }
}

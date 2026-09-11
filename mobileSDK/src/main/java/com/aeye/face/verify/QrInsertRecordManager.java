package com.aeye.face.verify;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.aeye.face.AEFaceSdk;
import com.aeye.face.api.FaceApiService;
import com.aeye.face.api.model.QrInsertRecordResult;

/**
 * 新增认证记录：调试场景在拉活体配置前创建 authRecordId；正式由业务 App 调用后传入。
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
     * 新增认证记录：无宿主 authRecordId 时在拉活体配置前创建（调试用；正式由业务 App 调用）。
     */
    public static void insert(Context context, Callback callback) {
        if (context == null) {
            postError(callback, "Context 为空");
            return;
        }
        new Thread(() -> {
            try {
                AEFaceSdk.ensureInitialized();
                String body = FaceApiService.buildInsertRecordRequestJson();
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

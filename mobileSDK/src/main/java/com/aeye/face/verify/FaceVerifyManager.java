package com.aeye.face.verify;

import com.aeye.face.AEFaceSdk;
import com.aeye.face.api.FaceApiService;
import com.aeye.face.api.model.FaceIdentResult;

/**
 * 活体完成后的人脸核验：在 SDK 内发起网络请求，宿主无需重复封装。
 */
public final class FaceVerifyManager {

    public interface Callback {
        void onPassed(FaceIdentResult result);

        void onFailed(String message);
    }

    private FaceVerifyManager() {
    }

    /**
     * @param livenessJson {@link com.aeye.face.uitls.PictureManagerUtils#getJsonString} 返回的 JSON
     */
    public static void submit(String livenessJson, Callback callback) {
        if (callback == null) {
            return;
        }
        new Thread(() -> {
            try {
                AEFaceSdk.ensureInitialized();
                String userId = FaceVerifySession.getUserId();
                String authRecordId = FaceVerifySession.getAuthRecordId();
                FaceIdentResult result = FaceApiService.submitFaceIdent(
                        AEFaceSdk.getApiBaseUrl(),
                        livenessJson,
                        userId,
                        authRecordId);
                if (result.isPass()) {
                    callback.onPassed(result);
                } else {
                    callback.onFailed("人脸核验未通过");
                }
            } catch (Exception e) {
                if (AEFaceSdk.isUseMockOnError()) {
                    try {
                        FaceIdentResult mock = FaceApiService.mockFaceIdentPass(
                                FaceVerifySession.getUserId(),
                                FaceVerifySession.getAuthRecordId());
                        callback.onPassed(mock);
                    } catch (Exception mockError) {
                        callback.onFailed(mockError.getMessage());
                    }
                } else {
                    callback.onFailed(e.getMessage() != null ? e.getMessage() : "人脸核验失败");
                }
            }
        }, "AEFace-Verify").start();
    }
}

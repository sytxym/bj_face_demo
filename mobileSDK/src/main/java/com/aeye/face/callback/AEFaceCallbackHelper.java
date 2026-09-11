package com.aeye.face.callback;

import com.aeye.face.AEFaceInterface;

/**
 * 活体结束回调分发：把 SDK 内部码映射为三端统一 {@code resultCode}，
 * 通过 {@link AEFaceInterface#onFinish(int, String, String, String)} 的第三、四个参数
 * 回调 {@code resultCode}/{@code resultMsg} 给宿主。
 * {@code data} 顶层同时保留 {@code resultCode}/{@code resultMsg}/{@code code}/{@code msg}。
 */
public final class AEFaceCallbackHelper {

    private AEFaceCallbackHelper() {
    }

    public static void dispatchFinish(AEFaceInterface listener, int sdkValue, String data) {
        dispatchFinish(listener, sdkValue, data, null);
    }

    public static void dispatchFinish(AEFaceInterface listener, int sdkValue,
                                      String data, String detailMessage) {
        dispatchFinish(listener, sdkValue, data, detailMessage, false);
    }

    public static void dispatchFinish(AEFaceInterface listener, int sdkValue,
                                      String data, String detailMessage, boolean submitFailure) {
        dispatchFinish(listener, sdkValue, data, detailMessage, submitFailure, null);
    }

    /**
     * @param backendErrorCode 场景异常编码或查询接口 {@code errorCode}；为 null 则用 SDK 内部码映射
     */
    public static void dispatchFinish(AEFaceInterface listener, int sdkValue,
                                      String data, String detailMessage, boolean submitFailure,
                                      String backendErrorCode) {
        if (listener == null) {
            return;
        }
        String enriched = FaceUniResultMapper.mergeIntoData(
                sdkValue, data, detailMessage, submitFailure, backendErrorCode);
        String resultCode = FaceUniResultMapper.unifiedResultCode(sdkValue, backendErrorCode);
        String resultMsg = FaceUniResultMapper.unifiedCallbackMessage(
                sdkValue, detailMessage, submitFailure);
        listener.onFinish(sdkValue, enriched, resultCode, resultMsg);
    }
}

package com.aeye.face.callback;

import com.aeye.face.AEFaceInterface;

/**
 * 活体结束回调分发：把 SDK 内部码映射为三端统一 {@code resultCode}，
 * 通过 {@link AEFaceInterface#onFinish(int, String, String)} 的第三个参数回调给宿主。
 * {@code data} 顶层同时保留 {@code resultCode}/{@code resultMsg}，供解析 JSON 的宿主使用。
 */
public final class AEFaceCallbackHelper {

    private AEFaceCallbackHelper() {
    }

    public static void dispatchFinish(AEFaceInterface listener, int sdkValue, String data) {
        dispatchFinish(listener, sdkValue, data, null);
    }

    public static void dispatchFinish(AEFaceInterface listener, int sdkValue,
                                      String data, String detailMessage) {
        if (listener == null) {
            return;
        }
        String enriched = FaceUniResultMapper.mergeIntoData(sdkValue, data, detailMessage);
        String resultCode = FaceUniResultMapper.unifiedResultCode(sdkValue);
        listener.onFinish(sdkValue, enriched, resultCode);
    }
}

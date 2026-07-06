package com.aeye.face.callback;

import com.aeye.face.AEFaceInterface;

import org.json.JSONObject;

/**
 * 活体结束回调分发：保留原生 {@link AEFaceInterface#onFinish(int, String)} 语义，
 * 同时在 {@code data} 中追加 {@code uniResult}，并触发 {@link AEFaceInterface#onUniFinish(String)}。
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
        listener.onFinish(sdkValue, enriched);
        JSONObject uni = FaceUniResultMapper.toUniJson(sdkValue, detailMessage);
        listener.onUniFinish(uni.toString());
    }
}

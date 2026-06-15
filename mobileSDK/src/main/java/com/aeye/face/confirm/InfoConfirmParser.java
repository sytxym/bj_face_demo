package com.aeye.face.confirm;

import android.text.TextUtils;

import com.aeye.face.api.ApiResponseParser;

import org.json.JSONObject;

/**
 * 解析用户信息预览业务 JSON（{@code data.data} 节点）。
 */
public final class InfoConfirmParser {

    private InfoConfirmParser() {
    }

    public static InfoConfirmPayload parse(String json) {
        return InfoConfirmPayload.fromApiData(ApiResponseParser.parse(json).getBusinessData());
    }

    static String firstNonEmpty(JSONObject data, String primary, String fallback) {
        String v = data.optString(primary, null);
        if (!TextUtils.isEmpty(v)) {
            return v;
        }
        return data.optString(fallback, null);
    }
}

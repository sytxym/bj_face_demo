package com.aeye.face.api;

import android.text.TextUtils;

import com.aeye.face.api.model.ApiResult;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 统一解析后台响应外层结构，并提取 {@code data.data} 业务节点。
 */
public final class ApiResponseParser {

    private ApiResponseParser() {
    }

    /**
     * 仅校验外层 {@code ok} 字段（{@code data} 可为 null）。
     * 适用于 faceIdent、saveFaceVerifyLog、updateRecord 等无业务 data 的接口。
     */
    public static void assertOk(String json) {
        parse(json, false);
    }

    public static ApiResult parse(String json) {
        return parse(json, true);
    }

    public static ApiResult parse(JSONObject root) {
        return parse(root, true);
    }

    private static ApiResult parse(String json, boolean requireBusinessData) {
        if (TextUtils.isEmpty(json)) {
            throw new IllegalArgumentException("响应为空");
        }
        try {
            return parse(new JSONObject(json), requireBusinessData);
        } catch (JSONException e) {
            throw new IllegalArgumentException("JSON 解析失败: " + e.getMessage());
        }
    }

    private static ApiResult parse(JSONObject root, boolean requireBusinessData) {
        if (root == null) {
            throw new IllegalArgumentException("响应为空");
        }
        boolean ok = root.optBoolean("ok", false);
        if (!ok) {
            String error = firstNonEmpty(
                    root.optString("errorCode", null),
                    root.optString("errors", null),
                    "接口返回 ok=false");
            throw new IllegalArgumentException(error);
        }
        JSONObject businessData = extractBusinessData(root);
        if (requireBusinessData && businessData == null) {
            throw new IllegalArgumentException("data 为空");
        }
        return new ApiResult(
                true,
                root.optString("errorCode", null),
                root.optString("mygType", null),
                businessData,
                root.optString("errors", null),
                root.optString("messageType", null));
    }

    /** 提取业务 JSON：优先 {@code data.data}，兼容旧版 {@code data} 直接为业务对象。 */
    public static JSONObject extractBusinessData(JSONObject root) {
        JSONObject data = root.optJSONObject("data");
        if (data == null) {
            return null;
        }
        JSONObject inner = data.optJSONObject("data");
        return inner != null ? inner : data;
    }

    private static String firstNonEmpty(String... values) {
        if (values == null) {
            return "接口异常";
        }
        for (String v : values) {
            if (!TextUtils.isEmpty(v)) {
                return v;
            }
        }
        return "接口异常";
    }
}

package com.xym.testface;

import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 解析扫码 JSON。{@code authRecordId} / {@code authIdentRecordId} 二选一即可；{@code userId} 可选。
 */
public final class ScanAuthParser {

    public static final class Result {
        private final String authIdentRecordId;
        private final String userId;

        Result(String authIdentRecordId, String userId) {
            this.authIdentRecordId = authIdentRecordId;
            this.userId = userId;
        }

        public String getAuthIdentRecordId() {
            return authIdentRecordId;
        }

        public String getUserId() {
            return userId;
        }
    }

    private ScanAuthParser() {
    }

    public static Result parse(String qrContent) throws JSONException {
        if (TextUtils.isEmpty(qrContent)) {
            throw new JSONException("扫码内容为空");
        }
        JSONObject json = new JSONObject(qrContent.trim());
        String userId = firstNonEmpty(json, "userId");
        String authIdentRecordId = firstNonEmpty(json, "authRecordId", "authIdentRecordId");
        if (TextUtils.isEmpty(authIdentRecordId)) {
            throw new JSONException("authRecordId 为空");
        }
        return new Result(authIdentRecordId, userId);
    }

    private static String firstNonEmpty(JSONObject json, String... keys) {
        if (json == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            if (TextUtils.isEmpty(key) || !json.has(key) || json.isNull(key)) {
                continue;
            }
            String value = json.optString(key, null);
            if (!TextUtils.isEmpty(value) && !"null".equalsIgnoreCase(value.trim())) {
                return value.trim();
            }
        }
        return null;
    }
}

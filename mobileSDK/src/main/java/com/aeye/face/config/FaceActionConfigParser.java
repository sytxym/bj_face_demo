package com.aeye.face.config;

import android.text.TextUtils;

import com.aeye.face.api.ApiResponseParser;

import org.json.JSONObject;

/**
 * 解析动作活体配置业务 JSON（{@code data.data} 节点）。
 */
public final class FaceActionConfigParser {

    private FaceActionConfigParser() {
    }

    public static FaceActionConfig parse(String json) {
        return parseBusinessData(ApiResponseParser.parse(json).getBusinessData());
    }

    public static FaceActionConfig parseBusinessData(JSONObject data) {
        if (data == null) {
            throw new IllegalArgumentException("data 为空");
        }
        FaceActionConfig config = new FaceActionConfig();
        config.setActionConfigId(data.optLong("actionConfigId", 0L));
        config.setBusinessCode(data.optString("businessCode", ""));
        config.setBusinessName(data.optString("businessName", ""));
        config.setDetectType(data.optString("detectType", "FACE"));
        String actionType = data.optString("actionType", FaceActionConfig.ACTION_RANDOM);
        config.setActionType(TextUtils.isEmpty(actionType) ? FaceActionConfig.ACTION_RANDOM : actionType);
        config.setActionCount(data.optInt("actionCount", 3));
        config.setEnableLookUp(parseFlag(data, "enableLookUp", true));
        config.setEnableLookDown(parseFlag(data, "enableLookDown", false));
        config.setEnableShakeHead(parseFlag(data, "enableShakeHead", true));
        config.setEnableOpenMouth(parseFlag(data, "enableOpenMouth", false));
        config.setEnableBlink(parseFlag(data, "enableBlink", true));
        config.setMemo(data.optString("memo", ""));
        return config;
    }

    private static boolean parseFlag(JSONObject data, String key, boolean defaultValue) {
        if (!data.has(key) || data.isNull(key)) {
            return defaultValue;
        }
        Object raw = data.opt(key);
        if (raw instanceof Boolean) {
            return (Boolean) raw;
        }
        if (raw instanceof Number) {
            return ((Number) raw).intValue() != 0;
        }
        String s = String.valueOf(raw).trim();
        if ("1".equals(s) || "true".equalsIgnoreCase(s) || "Y".equalsIgnoreCase(s)) {
            return true;
        }
        if ("0".equals(s) || "false".equalsIgnoreCase(s) || "N".equalsIgnoreCase(s)) {
            return false;
        }
        return defaultValue;
    }
}

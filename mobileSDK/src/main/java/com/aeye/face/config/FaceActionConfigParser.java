package com.aeye.face.config;

import android.text.TextUtils;

import com.aeye.face.api.ApiResponseParser;
import com.aeye.face.verify.FaceUserInfo;

import org.json.JSONObject;

/**
 * 解析动作活体配置业务 JSON（{@code data.data}：{@code userInfo} + {@code actionConfig}）。
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
        JSONObject actionNode = data.optJSONObject("actionConfig");
        JSONObject configSrc = actionNode != null ? actionNode : data;
        FaceActionConfig config = new FaceActionConfig();
        config.setActionConfigId(configSrc.optLong("actionConfigId", 0L));
        config.setBusinessCode(configSrc.optString("businessCode", ""));
        config.setBusinessName(configSrc.optString("businessName", ""));
        // detectType 后台为数字码（1 静默/2 动作/3 炫彩/4 动作+炫彩），缺省按动作
        config.setDetectType(configSrc.optString("detectType", FaceActionConfig.DETECT_MOTION));
        // actionType 后台为数字码（1 顺序/2 随机），兼容旧 SEQUENCE/RANDOM，缺省随机
        String actionType = configSrc.optString("actionType", FaceActionConfig.ACTION_RANDOM);
        config.setActionType(TextUtils.isEmpty(actionType) ? FaceActionConfig.ACTION_RANDOM : actionType);
        config.setActionCount(configSrc.optInt("actionCount", 3));
        config.setEnableLookUp(parseFlag(configSrc, "enableLookUp", true));
        config.setEnableLookDown(parseFlag(configSrc, "enableLookDown", false));
        config.setEnableShakeHead(parseFlag(configSrc, "enableShakeHead", true));
        config.setEnableOpenMouth(parseFlag(configSrc, "enableOpenMouth", false));
        config.setEnableBlink(parseFlag(configSrc, "enableBlink", true));
        config.setMemo(configSrc.optString("memo", ""));
        JSONObject userNode = data.optJSONObject("userInfo");
        if (userNode != null) {
            config.setUserInfo(FaceUserInfo.fromJson(userNode));
        }
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

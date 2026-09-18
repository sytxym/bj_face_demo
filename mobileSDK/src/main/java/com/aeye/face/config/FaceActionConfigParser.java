package com.aeye.face.config;

import android.text.TextUtils;

import com.aeye.face.api.ApiResponseParser;
import com.aeye.face.verify.FaceUserInfo;

import org.json.JSONObject;

/**
 * 解析动作活体配置业务 JSON（{@code userInfo} + {@code actionConfig} + 同级 {@code pollingCount}/{@code pollingTime}）。
 * 兼容网关多层 {@code data} 信封，避免剥不干净时回退成默认 3 个动作。
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
        JSONObject payload = unwrapToActionPayload(data);
        JSONObject configSrc = resolveConfigSrc(payload);
        FaceActionConfig config = new FaceActionConfig();
        config.setActionConfigId(configSrc.optLong("actionConfigId", 0L));
        config.setBusinessCode(configSrc.optString("businessCode", ""));
        config.setBusinessName(configSrc.optString("businessName", ""));
        // detectType 后台为数字码（1 静默/2 动作/3 炫彩/4 动作+炫彩），缺省按动作
        config.setDetectType(configSrc.optString("detectType", FaceActionConfig.DETECT_MOTION));
        // actionType 后台为数字码（1 顺序/2 随机），兼容旧 SEQUENCE/RANDOM，缺省随机
        String actionType = configSrc.optString("actionType", FaceActionConfig.ACTION_RANDOM);
        config.setActionType(TextUtils.isEmpty(actionType) ? FaceActionConfig.ACTION_RANDOM : actionType);
        config.setEnableLookUp(parseFlag(configSrc, "enableLookUp", true));
        config.setEnableLookDown(parseFlag(configSrc, "enableLookDown", false));
        config.setEnableShakeHead(parseFlag(configSrc, "enableShakeHead", true));
        config.setEnableOpenMouth(parseFlag(configSrc, "enableOpenMouth", false));
        config.setEnableBlink(parseFlag(configSrc, "enableBlink", true));
        config.setActionCount(parseActionCount(configSrc, config.countEnabled()));
        config.setMemo(configSrc.optString("memo", ""));
        JSONObject userNode = payload.optJSONObject("userInfo");
        if (userNode != null) {
            config.setUserInfo(FaceUserInfo.fromJson(userNode));
        }
        // pollingCount / pollingTime 与 userInfo 同级，不在 actionConfig 内
        config.setPollingCount(firstPresentInt(payload, configSrc, "pollingCount",
                FaceActionConfig.DEFAULT_POLLING_COUNT, 1, 30));
        config.setPollingTimeSec(firstPresentInt(payload, configSrc, "pollingTime",
                FaceActionConfig.DEFAULT_POLLING_TIME_SEC, 0, 60));
        return config;
    }

    /**
     * 网关/直启可能再套一层 {@code {code,message,data:{userInfo,actionConfig}}}。
     * 只剥信封、不改业务字段；剥不到则沿用当前节点（兼容平铺 / Mock）。
     */
    private static JSONObject unwrapToActionPayload(JSONObject data) {
        JSONObject cur = data;
        for (int i = 0; i < 8 && cur != null; i++) {
            if (looksLikeActionPayload(cur)) {
                return cur;
            }
            JSONObject nested = cur.optJSONObject("data");
            if (nested == null) {
                return cur;
            }
            cur = nested;
        }
        return cur;
    }

    private static boolean looksLikeActionPayload(JSONObject obj) {
        return obj.has("actionConfig")
                || obj.has("userInfo")
                || obj.has("pollingCount")
                || obj.has("pollingTime")
                || obj.has("actionCount")
                || obj.has("detectType")
                || obj.has("enableLookUp")
                || obj.has("enableBlink");
    }

    /**
     * 兼容两种结构：字段在 {@code actionConfig} 内，或直接平铺在 {@code data.data}。
     * 若两者都有，actionConfig 覆盖同名字段，缺的用平铺字段补上（避免空的 actionConfig 把 actionCount 吃成默认 3）。
     */
    private static JSONObject resolveConfigSrc(JSONObject data) {
        JSONObject actionNode = data.optJSONObject("actionConfig");
        if (actionNode == null) {
            return data;
        }
        JSONObject merged = new JSONObject();
        copyKnown(data, merged);
        copyKnown(actionNode, merged);
        return merged;
    }

    private static final String[] CONFIG_KEYS = {
            "actionConfigId", "businessCode", "businessName", "detectType", "actionType",
            "actionCount", "enableLookUp", "enableLookDown", "enableShakeHead",
            "enableOpenMouth", "enableBlink", "memo"
    };

    private static void copyKnown(JSONObject from, JSONObject to) {
        if (from == null || to == null) {
            return;
        }
        for (String key : CONFIG_KEYS) {
            if (from.has(key) && !from.isNull(key)) {
                try {
                    to.put(key, from.opt(key));
                } catch (org.json.JSONException ignored) {
                }
            }
        }
    }

    private static int parseActionCount(JSONObject src, int enabledCount) {
        int fallback = enabledCount > 0 ? Math.min(5, enabledCount) : 3;
        if (src == null || !src.has("actionCount") || src.isNull("actionCount")) {
            return fallback;
        }
        Object raw = src.opt("actionCount");
        int parsed = toInt(raw, -1);
        if (parsed < 0) {
            return fallback;
        }
        return Math.max(0, Math.min(5, parsed));
    }

    private static int firstPresentInt(JSONObject primary, JSONObject fallback, String key,
                                       int defaultValue, int min, int max) {
        if (hasValue(primary, key)) {
            return parseBoundedInt(primary.opt(key), defaultValue, min, max);
        }
        if (hasValue(fallback, key)) {
            return parseBoundedInt(fallback.opt(key), defaultValue, min, max);
        }
        return defaultValue;
    }

    private static boolean hasValue(JSONObject obj, String key) {
        return obj != null && obj.has(key) && !obj.isNull(key);
    }

    private static int parseBoundedInt(Object raw, int defaultValue, int min, int max) {
        int parsed = toInt(raw, Integer.MIN_VALUE);
        if (parsed == Integer.MIN_VALUE || parsed < min) {
            return defaultValue;
        }
        return Math.min(max, parsed);
    }

    private static int toInt(Object raw, int defaultValue) {
        if (raw instanceof Number) {
            return ((Number) raw).intValue();
        }
        if (raw instanceof Boolean) {
            return defaultValue;
        }
        if (raw == null) {
            return defaultValue;
        }
        String s = String.valueOf(raw).trim();
        if (s.isEmpty() || "null".equalsIgnoreCase(s)) {
            return defaultValue;
        }
        try {
            return (int) Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
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

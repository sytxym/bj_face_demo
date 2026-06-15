package com.aeye.face.config;

/**
 * 动作活体配置接口默认常量（与后台契约一致）。
 */
public final class FaceActionConfigDefaults {

    public static final String DEFAULT_BUSINESS_CODE = "REGISTER";

    /**
     * 接口不可用时的内置 Mock（统一外层 {@code ok/data/data} 结构）。
     */
    public static final String FALLBACK_JSON = "{"
            + "\"ok\":true,"
            + "\"errorCode\":null,"
            + "\"mygType\":null,"
            + "\"data\":{"
            + "\"data\":{"
            + "\"actionConfigId\":10001,"
            + "\"businessCode\":\"REGISTER\","
            + "\"businessName\":\"用户注册\","
            + "\"detectType\":\"FACE\","
            + "\"actionType\":\"RANDOM\","
            + "\"actionCount\":3,"
            + "\"enableLookUp\":\"1\","
            + "\"enableLookDown\":\"0\","
            + "\"enableShakeHead\":\"1\","
            + "\"enableOpenMouth\":\"0\","
            + "\"enableBlink\":\"1\","
            + "\"memo\":\"Demo 模拟动作配置\""
            + "}"
            + "},"
            + "\"errors\":null,"
            + "\"messageList\":[],"
            + "\"messageType\":null"
            + "}";

    private FaceActionConfigDefaults() {
    }
}

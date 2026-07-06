package com.aeye.face.config;

import android.text.TextUtils;

/**
 * 动作活体配置接口默认常量（与后台 business_config.business_code 一致）。
 */
public final class FaceActionConfigDefaults {

    /** 法人注册 */
    public static final String CODE_LEGAL_REGISTER = "01";
    /** 单位账号重置密码 */
    public static final String CODE_UNIT_PWD_RESET = "03";
    /** 法人账号启用 */
    public static final String CODE_LEGAL_ACCOUNT_ENABLE = "04";
    /** 账号所有人信息修改或实名 */
    public static final String CODE_OWNER_INFO_MODIFY = "05";
    /** 单位信息修改 */
    public static final String CODE_UNIT_INFO_MODIFY = "06";
    /** 单位账号注销 */
    public static final String CODE_UNIT_ACCOUNT_CANCEL = "07";
    /** 自然人用户注册 */
    public static final String CODE_NATURAL_REGISTER = "08";
    /** 自然人账号找回用户名 */
    public static final String CODE_NATURAL_USERNAME_RECOVER = "09";
    /** 自然人账号重置密码 */
    public static final String CODE_NATURAL_PWD_RESET = "10";
    /** 证件管理新增/修改 */
    public static final String CODE_CERT_MANAGE = "11";
    /** 自然人实名认证 */
    public static final String CODE_NATURAL_REAL_NAME = "12";
    /** 自然人账号注销 */
    public static final String CODE_NATURAL_ACCOUNT_CANCEL = "13";

    /** 宿主未传 businessCode 时的默认回退值（Mock / 空参请求） */
    public static final String DEFAULT_BUSINESS_CODE = CODE_NATURAL_REGISTER;

    /**
     * 注册场景：无 userId 时 insertRecord 传 certNo，有 userId 时仍传 userId。
     */
    public static boolean isRegisterScene(String businessCode) {
        if (TextUtils.isEmpty(businessCode)) {
            return false;
        }
        String code = businessCode.trim();
        return CODE_LEGAL_REGISTER.equals(code) || CODE_NATURAL_REGISTER.equals(code);
    }

    /**
     * Mock 回退时按 businessCode 填充 businessName。
     */
    public static String resolveBusinessName(String businessCode) {
        if (TextUtils.isEmpty(businessCode)) {
            return "";
        }
        switch (businessCode.trim()) {
            case CODE_LEGAL_REGISTER:
                return "法人注册";
            case CODE_UNIT_PWD_RESET:
                return "单位账号重置密码";
            case CODE_LEGAL_ACCOUNT_ENABLE:
                return "法人账号启用";
            case CODE_OWNER_INFO_MODIFY:
                return "账号所有人信息修改或实名";
            case CODE_UNIT_INFO_MODIFY:
                return "单位信息修改";
            case CODE_UNIT_ACCOUNT_CANCEL:
                return "单位账号注销";
            case CODE_NATURAL_REGISTER:
                return "自然人用户注册";
            case CODE_NATURAL_USERNAME_RECOVER:
                return "自然人账号找回用户名";
            case CODE_NATURAL_PWD_RESET:
                return "自然人账号重置密码";
            case CODE_CERT_MANAGE:
                return "证件管理新增/修改";
            case CODE_NATURAL_REAL_NAME:
                return "自然人实名认证";
            case CODE_NATURAL_ACCOUNT_CANCEL:
                return "自然人账号注销";
            default:
                return businessCode.trim();
        }
    }

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
            + "\"businessCode\":\"" + DEFAULT_BUSINESS_CODE + "\","
            + "\"businessName\":\"" + resolveBusinessName(DEFAULT_BUSINESS_CODE) + "\","
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

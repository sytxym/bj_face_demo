package com.aeye.face.confirm;

/**
 * 用户信息预览 Mock 数据（统一信封 {@code code/message/data}）。
 */
public final class InfoConfirmDefaults {

    public static final String FALLBACK_JSON = "{"
            + "\"code\":\"200\","
            + "\"message\":\"成功\","
            + "\"data\":{"
            + "\"userId\":\"2\","
            + "\"certType\":\"身份证\","
            + "\"certNo\":\"430622195815263182\","
            + "\"userType\":\"自然人\","
            + "\"name\":\"邓总\","
            + "\"birthDay\":\"1988-05-05\","
            + "\"nation\":\"中国\""
            + "}"
            + "}";

    private InfoConfirmDefaults() {
    }
}

package com.aeye.face.api;

/**
 * 新增认证记录 Mock 数据（统一信封 {@code code/message/data}）。
 */
final class QrInsertRecordDefaults {

    static final String MOCK_RESPONSE_JSON = "{"
            + "\"code\":\"200\","
            + "\"message\":\"成功\","
            + "\"data\":{"
            + "\"userId\":\"2\","
            + "\"authRecordId\":\"12345\","
            + "\"status\":\"0\""
            + "}"
            + "}";

    private QrInsertRecordDefaults() {
    }
}

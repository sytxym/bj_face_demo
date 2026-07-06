package com.aeye.face.api;

/**
 * 新增认证记录 Mock 数据（统一外层 {@code ok/data/data} 结构）。
 */
final class QrInsertRecordDefaults {

    static final String MOCK_RESPONSE_JSON = "{"
            + "\"ok\":true,"
            + "\"errorCode\":null,"
            + "\"mygType\":null,"
            + "\"data\":{"
            + "\"data\":{"
            + "\"userId\":\"2\","
            + "\"authRecordId\":\"12345\""
            + "}"
            + "},"
            + "\"errors\":null,"
            + "\"messageList\":[],"
            + "\"messageType\":null"
            + "}";

    private QrInsertRecordDefaults() {
    }
}

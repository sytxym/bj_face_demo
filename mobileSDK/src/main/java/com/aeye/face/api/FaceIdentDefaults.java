package com.aeye.face.api;

/**
 * 人脸核验接口 Mock 数据（统一外层 {@code ok/data/data} 结构，与后台契约一致）。
 */
final class FaceIdentDefaults {

    static final String MOCK_RESPONSE_JSON = "{"
            + "\"ok\":true,"
            + "\"errorCode\":null,"
            + "\"mygType\":null,"
            + "\"data\":{"
            + "\"data\":{"
            + "\"authRecordId\":102458,"
            + "\"userId\":\"U10086\","
            + "\"code\":200,"
            + "\"isPass\":\"1\""
            + "}"
            + "},"
            + "\"errors\":null,"
            + "\"messageList\":[],"
            + "\"messageType\":null"
            + "}";

    private FaceIdentDefaults() {
    }
}

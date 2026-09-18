package com.aeye.face.api;

/**
 * 人脸核验 Mock 数据：统一信封 {@code code=200}，{@code data=null}。
 */
final class FaceIdentDefaults {

    static final String MOCK_RESPONSE_JSON = "{"
            + "\"code\":\"200\","
            + "\"message\":\"成功\","
            + "\"data\":null"
            + "}";

    private FaceIdentDefaults() {
    }
}

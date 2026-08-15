package com.aeye.face.api;

/**
 * 人脸核验 Mock 数据：仅外层 {@code ok=true}，{@code data=null}。
 */
final class FaceIdentDefaults {

    static final String MOCK_RESPONSE_JSON = "{"
            + "\"ok\":true,"
            + "\"errorCode\":null,"
            + "\"mygType\":null,"
            + "\"data\":null,"
            + "\"errors\":null,"
            + "\"messageList\":[],"
            + "\"messageType\":null"
            + "}";

    private FaceIdentDefaults() {
    }
}

package com.aeye.face.api.model;

/**
 * 人脸核验接口 {@code /fivweb/assistant/faceIdent} 提交结果。
 * <p>后台仅返回外层 {@code ok}，成功即 {@link #pass()}；最终通过/未通过以
 * {@code /qrCode/authStatus} 为准。</p>
 */
public final class FaceIdentResult {

    private final boolean pass;

    private FaceIdentResult(boolean pass) {
        this.pass = pass;
    }

    public static FaceIdentResult pass() {
        return new FaceIdentResult(true);
    }

    public boolean isPass() {
        return pass;
    }
}

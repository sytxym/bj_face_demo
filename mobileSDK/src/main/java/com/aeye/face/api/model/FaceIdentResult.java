package com.aeye.face.api.model;

/**
 * 人脸核验接口 {@code /fivweb/assistant/faceIdent} 结果。
 * <p>后台仅返回外层 {@code ok}，{@code data} 恒为 null，成功即 {@link #pass()}。</p>
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

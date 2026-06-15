package com.aeye.face.api.model;

/**
 * 人脸核验接口 {@code /assistant/faceIdent} 业务结果。
 */
public final class FaceIdentResult {

    private final boolean pass;
    private final String userId;
    private final long authRecordId;
    private final String isPass;
    private final int code;

    public FaceIdentResult(boolean pass, String userId, long authRecordId, String isPass, int code) {
        this.pass = pass;
        this.userId = userId;
        this.authRecordId = authRecordId;
        this.isPass = isPass;
        this.code = code;
    }

    public boolean isPass() {
        return pass;
    }

    public String getUserId() {
        return userId;
    }

    public long getAuthRecordId() {
        return authRecordId;
    }

    public String getIsPass() {
        return isPass;
    }

    public int getCode() {
        return code;
    }
}

package com.aeye.face.api.model;

/**
 * 新增认证记录 {@code /qrCode/insertRecord} 业务结果。
 */
public final class QrInsertRecordResult {

    private final String userId;
    private final String authRecordId;

    public QrInsertRecordResult(String userId, String authRecordId) {
        this.userId = userId;
        this.authRecordId = authRecordId;
    }

    public String getUserId() {
        return userId;
    }

    public String getAuthRecordId() {
        return authRecordId;
    }
}

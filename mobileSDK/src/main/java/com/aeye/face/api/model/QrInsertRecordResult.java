package com.aeye.face.api.model;

/**
 * 新增认证记录 {@code /fivweb/qrCode/insertRecord} 业务结果。
 */
public final class QrInsertRecordResult {

    private final String userId;
    private final String authRecordId;
    /** 认证状态：0 待扫码 … 5 已通过，6 任务已取消，见 {@link com.aeye.face.verify.QrRecordStatus} */
    private final String status;

    public QrInsertRecordResult(String userId, String authRecordId, String status) {
        this.userId = userId;
        this.authRecordId = authRecordId;
        this.status = status;
    }

    public String getUserId() {
        return userId;
    }

    public String getAuthRecordId() {
        return authRecordId;
    }

    public String getStatus() {
        return status;
    }
}

package com.aeye.face.api.model;

import android.text.TextUtils;

import com.aeye.face.verify.QrRecordStatus;

/**
 * 查询核验结果状态 {@code /qrCode/authStatus}。
 */
public final class AuthStatusResult {

    public static final String BUSY_MESSAGE = "繁忙中，请稍后重试";

    private final String status;
    private final String failType;
    private final boolean busy;

    private AuthStatusResult(String status, String failType, boolean busy) {
        this.status = status;
        this.failType = failType;
        this.busy = busy;
    }

    public static AuthStatusResult of(String status, String failType) {
        return new AuthStatusResult(status, failType, false);
    }

    public static AuthStatusResult busy() {
        return new AuthStatusResult(QrRecordStatus.VERIFYING, BUSY_MESSAGE, true);
    }

    public String getStatus() {
        return status;
    }

    public String getFailType() {
        return failType;
    }

    public boolean isBusy() {
        return busy;
    }

    public boolean isPassed() {
        return !busy && QrRecordStatus.PASSED.equals(status);
    }

    public boolean isVerifying() {
        return !busy && QrRecordStatus.VERIFYING.equals(status);
    }

    /** 未通过时的详情 / 回调 msg：优先 failtype，忙碌时为固定文案 */
    public String displayFailMessage() {
        if (busy) {
            return BUSY_MESSAGE;
        }
        if (!TextUtils.isEmpty(failType)) {
            return failType;
        }
        return "核验失败";
    }
}

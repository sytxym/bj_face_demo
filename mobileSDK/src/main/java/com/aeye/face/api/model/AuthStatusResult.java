package com.aeye.face.api.model;

import android.text.TextUtils;

import com.aeye.face.verify.QrRecordStatus;

/**
 * 查询核验结果 {@code /faceRecord/queryVerifyResult}。
 * <p>{@code errorCode}/{@code messageList} 取业务信封，不用网关最外层字段。</p>
 */
public final class AuthStatusResult {

    public static final String BUSY_MESSAGE = "繁忙中，请稍后重试";

    private final String status;
    private final String failType;
    private final boolean busy;
    /** 业务信封 {@code errorCode}，如 0415001 / 0412006 */
    private final String backendErrorCode;
    private final boolean apiError;
    private final String verifyTerminal;

    private AuthStatusResult(String status, String failType, boolean busy) {
        this(status, failType, busy, false, null, null);
    }

    private AuthStatusResult(String status, String failType, boolean busy,
                             boolean apiError, String backendErrorCode) {
        this(status, failType, busy, apiError, backendErrorCode, null);
    }

    private AuthStatusResult(String status, String failType, boolean busy,
                             boolean apiError, String backendErrorCode, String verifyTerminal) {
        this.status = status;
        this.failType = failType;
        this.busy = busy;
        this.apiError = apiError;
        this.backendErrorCode = backendErrorCode;
        this.verifyTerminal = verifyTerminal;
    }

    public static AuthStatusResult of(String status, String failType) {
        return of(status, failType, null, null);
    }

    public static AuthStatusResult of(String status, String message, String errorCode,
                                      String verifyTerminal) {
        return new AuthStatusResult(status, message, false, false, errorCode, verifyTerminal);
    }

    public static AuthStatusResult busy() {
        return new AuthStatusResult(QrRecordStatus.VERIFYING, BUSY_MESSAGE, true);
    }

    /**
     * 查询核验 {@code ok=false}（第 21 项比对异常、第 22 项二次核验异常）。
     *
     * @param errorCode 业务信封 {@code errorCode}
     * @param message   业务信封 {@code messageList[0]}
     */
    public static AuthStatusResult apiError(String errorCode, String message) {
        return new AuthStatusResult(null, message, false, true, errorCode, null);
    }

    public String getStatus() {
        return status;
    }

    public String getFailType() {
        return failType;
    }

    public String getVerifyTerminal() {
        return verifyTerminal;
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

    /** 已拿到最终核验结果：未通过(4) 或已通过(5) */
    public boolean isFinishedResult() {
        return !busy && !apiError
                && (QrRecordStatus.NOT_PASS.equals(status) || QrRecordStatus.PASSED.equals(status));
    }

    public boolean isAbnormalExit() {
        return !busy && !apiError && QrRecordStatus.ABNORMAL_EXIT.equals(status);
    }

    public static AuthStatusResult abnormalExit() {
        return new AuthStatusResult(QrRecordStatus.ABNORMAL_EXIT, null, false);
    }

    /** 查询接口 {@code ok=false}，应立即失败、不再轮询 */
    public boolean isApiError() {
        return apiError;
    }

    /** 业务信封 {@code errorCode}，无则 null */
    public String getBackendErrorCode() {
        return backendErrorCode;
    }

    /** 未通过时的详情 / 回调 msg；优先业务 {@code messageList[0]} */
    public String displayFailMessage() {
        if (busy) {
            return BUSY_MESSAGE;
        }
        if (!TextUtils.isEmpty(failType)) {
            return failType;
        }
        if (apiError) {
            return "核验未通过，请重试";
        }
        if (isAbnormalExit()) {
            return "服务异常，请重试";
        }
        return "验证失败，请重试";
    }
}

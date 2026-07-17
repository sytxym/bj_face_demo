package com.aeye.face.verify;

/**
 * 二维码认证记录状态 {@code isPass} / {@code failedType}，
 * 与后台 {@code /qrCode/updateRecord} 约定一致。
 */
public final class QrRecordStatus {

    /** 待扫码（客户端不上报） */
    public static final String WAIT_SCAN = "0";
    /** 扫码完成 */
    public static final String SCAN_DONE = "1";
    /** 异常退出 */
    public static final String ABNORMAL_EXIT = "2";
    /** 核验中 */
    public static final String VERIFYING = "3";
    /** 未通过 */
    public static final String NOT_PASS = "4";
    /** 已通过 */
    public static final String PASSED = "5";

    /**
     * 失败原因 {@code failedType}：仅异常退出、动作活体未通过 3 次需传，其余场景不传。
     */
    public static final class FailedType {
        /** 动作活体检测未通过 */
        public static final String LIVENESS_ACTION = "1";
        /** 任务超期（客户端当前不传） */
        public static final String TIMEOUT = "2";
        /** 交叉验核未通过（客户端当前不传） */
        public static final String CROSS_CHECK = "3";
        /** 静默活体检测未通过（客户端当前不传） */
        public static final String LIVENESS_SILENT = "4";
        /** 认证比对未通过（客户端当前不传） */
        public static final String FACE_COMPARE = "5";
        /** 已取消（异常退出时必传） */
        public static final String CANCELLED = "6";

        private FailedType() {
        }
    }

    private QrRecordStatus() {
    }
}

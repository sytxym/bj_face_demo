package com.aeye.face.verify;

/**
 * 认证记录（任务）状态 {@code status} / {@code failedType}，
 * 与后台 {@code /qrCode/updateRecord} 约定一致（扫码、直启共用）。
 */
public final class QrRecordStatus {

    /** 待扫码 / 任务未开始（客户端不上报） */
    public static final String WAIT_SCAN = "0";
    /** 扫码完成 / 任务已进入确认页（扫码、直启均上报） */
    public static final String SCAN_DONE = "1";
    /** 异常退出（后台保留；客户端暂不上报） */
    public static final String ABNORMAL_EXIT = "2";
    /** 核验中 */
    public static final String VERIFYING = "3";
    /** 未通过 */
    public static final String NOT_PASS = "4";
    /** 已通过（最终核验由业务 App 二次确认，SDK 提交成功时不上报此状态） */
    public static final String PASSED = "5";
    /** 任务已取消（用户点返回 / 系统返回，任务直接结束失效） */
    public static final String TASK_CANCELLED = "6";

    /**
     * 失败原因 {@code failedType}：仅任务取消、动作活体未通过 3 次需传，其余场景不传。
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
        /** 已取消（用户返回取消任务时必传，配 {@link QrRecordStatus#TASK_CANCELLED}） */
        public static final String CANCELLED = "6";
        /** 炫彩对比未通过 */
        public static final String LIVENESS_COLOR = "7";

        private FailedType() {
        }
    }

    private QrRecordStatus() {
    }
}

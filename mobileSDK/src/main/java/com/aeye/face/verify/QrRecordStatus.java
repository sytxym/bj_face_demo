package com.aeye.face.verify;

/**
 * 二维码认证记录状态 {@code isPass}，与后台 {@code /qrCode/updateRecord} 约定一致。
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

    private QrRecordStatus() {
    }
}

package com.aeye.face.callback;

/**
 * 人脸核验结果码常量。
 * <ul>
 *   <li>{@code RESULT_*}：三端统一结果码（Android / 鸿蒙 / iOS 一致，字符串含前导零），
 *       随 {@code onFinish} 的 {@code data.resultCode} 下发给业务 APP。</li>
 *   <li>其余 int 常量：SDK 流程前置错误（配置/参数/授权），仅用于 {@code onError} 的提示文案。</li>
 * </ul>
 */
public final class FaceUniResultCodes {

    // ===== 统一结果码（三端一致，随 data.resultCode 下发）=====
    /** 核验成功 */
    public static final String RESULT_SUCCESS = "0";
    /** 核验失败 */
    public static final String RESULT_VERIFY_FAILED = "0414009";
    /** 核验超时 */
    public static final String RESULT_TIMEOUT = "0414010";
    /** 用户取消 */
    public static final String RESULT_USER_CANCEL = "0414011";
    /** 摄像头异常 */
    public static final String RESULT_CAMERA_ERROR = "0414012";
    /** 设备不安全 */
    public static final String RESULT_DEVICE_UNSAFE = "0414013";
    /** 选择其他核验方式 */
    public static final String RESULT_OTHER_VERIFY = "0414014";

    public static final String RESULT_MSG_SUCCESS = "核验成功";
    public static final String RESULT_MSG_VERIFY_FAILED = "核验失败";
    public static final String RESULT_MSG_TIMEOUT = "核验超时";
    public static final String RESULT_MSG_USER_CANCEL = "用户取消";
    public static final String RESULT_MSG_CAMERA_ERROR = "摄像头异常";
    public static final String RESULT_MSG_DEVICE_UNSAFE = "设备不安全";
    public static final String RESULT_MSG_OTHER_VERIFY = "选择其他核验方式";

    // ===== 流程前置错误码（仅用于 onError 文案，不下发 resultCode）=====
    /** 无法获取 Activity 上下文 */
    public static final int NO_ACTIVITY = -1;
    /** 缺少必要参数 */
    public static final int MISSING_PARAMS = -2;
    /** 认证失败（活体失败、后台核验未通过等） */
    public static final int AUTH_FAILED = -3;
    /** 返回数据解析失败 */
    public static final int PARSE_FAILED = -5;

    public static final String MSG_NO_ACTIVITY = "无法获取Activity上下文";
    public static final String MSG_MISSING_PARAMS = "缺少必要参数";
    public static final String MSG_AUTH_FAILED = "人脸认证失败";
    public static final String MSG_PARSE_FAILED = "返回数据解析失败";

    private FaceUniResultCodes() {
    }
}

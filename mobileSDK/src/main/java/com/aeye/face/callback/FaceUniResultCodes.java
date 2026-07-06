package com.aeye.face.callback;

/**
 * UniApp 人脸核验统一结果码（与甲方 uni 插件约定一致）。
 */
public final class FaceUniResultCodes {

    /** 认证成功 */
    public static final int SUCCESS = 0;
    /** 无法获取 Activity 上下文 */
    public static final int NO_ACTIVITY = -1;
    /** 缺少必要参数 */
    public static final int MISSING_PARAMS = -2;
    /** 认证失败（活体失败、超时、后台核验未通过等） */
    public static final int AUTH_FAILED = -3;
    /** 用户取消认证 */
    public static final int USER_CANCEL = -4;
    /** 返回数据解析失败 */
    public static final int PARSE_FAILED = -5;

    public static final String MSG_SUCCESS = "认证成功";
    public static final String MSG_NO_ACTIVITY = "无法获取Activity上下文";
    public static final String MSG_MISSING_PARAMS = "缺少必要参数";
    public static final String MSG_AUTH_FAILED = "人脸认证失败";
    public static final String MSG_USER_CANCEL = "用户取消认证";
    public static final String MSG_PARSE_FAILED = "返回数据解析失败";

    private FaceUniResultCodes() {
    }
}

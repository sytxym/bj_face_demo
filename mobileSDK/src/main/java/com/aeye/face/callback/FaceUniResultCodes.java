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

    /** 统一结果码（三端一致，随 data.resultCode 下发） */
    /** 提交并经查询核验结果确认通过 */
    public static final String RESULT_SUCCESS = "0414000";
    /** 第 12 项：首次检测无人脸超时 */
    public static final String RESULT_NO_FACE_TIMEOUT = "0412002";
    /** 第 11 项：Root / 越狱等设备不安全（USB 调试拦截同码） */
    public static final String RESULT_DEVICE_UNSAFE = "0414001";
    /** 第 13 项：多人脸 */
    public static final String RESULT_MULTI_FACE = "0414003";
    /** 第 14 项：环境光过暗 */
    public static final String RESULT_LIGHT_DARK = "0414004";
    /** 第 15 项：环境光过亮 */
    public static final String RESULT_LIGHT_BRIGHT = "0414005";
    /** 第 16 项：单动作超时 */
    public static final String RESULT_ACTION_TIMEOUT = "0414008";
    /** 第 17 项：动作中出框超时 */
    public static final String RESULT_ACTION_OUT_OF_FRAME = "0414009";
    /**
     * @deprecated 现为出框超时码 {@link #RESULT_ACTION_OUT_OF_FRAME}；笼统失败请用
     *             {@link #RESULT_LIVENESS_FAIL} 或具体场景码。
     */
    public static final String RESULT_VERIFY_FAILED = RESULT_ACTION_OUT_OF_FRAME;
    /** 第 18 项：活体算法判定失败（表内编码为 04014010） */
    public static final String RESULT_LIVENESS_FAIL = "04014010";
    /** 整体认证超时（失败页 timeout 态；表 10–22 未单列） */
    public static final String RESULT_TIMEOUT = "0414010";
    /** 第 19 项：提交网络超时 */
    public static final String RESULT_NETWORK_TIMEOUT = "0114011";
    /** 用户取消 */
    public static final String RESULT_USER_CANCEL = "0414011";
    /** 相机设备异常（初始化/打开失败，非权限） */
    public static final String RESULT_CAMERA_ERROR = "0414012";
    /** 第 10 项：未授予相机权限 */
    public static final String RESULT_CAMERA_NO_PERMISSION = "0414013";
    /** 选择其他核验方式 */
    public static final String RESULT_OTHER_VERIFY = "0414014";
    /** 第 20 项：提交服务异常 */
    public static final String RESULT_SERVICE_ERROR = "0419001";

    public static final String RESULT_MSG_SUCCESS = "核验成功";
    /** 活体检测失败 */
    public static final String RESULT_MSG_LIVENESS_FAILED = "验证失败";
    /** @deprecated 请用 {@link #RESULT_MSG_LIVENESS_FAILED} 或 {@link #RESULT_MSG_SUBMIT_FAILED} */
    public static final String RESULT_MSG_VERIFY_FAILED = "验证失败";
    /** faceIdent 提交失败 */
    public static final String RESULT_MSG_SUBMIT_FAILED = "核验失败";
    public static final String RESULT_MSG_TIMEOUT = "核验超时";
    public static final String RESULT_MSG_USER_CANCEL = "用户已取消，请稍后重试";
    public static final String RESULT_MSG_CAMERA_ERROR = "相机暂时无法使用，请稍后重试";
    /** 第 11 项提示文案 */
    public static final String RESULT_MSG_DEVICE_UNSAFE = "设备不安全，请更换设备后重试";
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

    /**
     * 提交阶段失败且无后台 {@code code} 时：网络超时走第 19 项，其余走第 20 项。
     * 第 21/22 项已有后台码，不要调用本方法覆盖。
     */
    public static String submitSceneCode(String message) {
        if (message != null) {
            String lower = message.toLowerCase();
            if (message.contains("网络超时")
                    || message.contains("网络异常")
                    || lower.contains("timeout")
                    || lower.contains("timed out")) {
                return RESULT_NETWORK_TIMEOUT;
            }
        }
        return RESULT_SERVICE_ERROR;
    }

    private FaceUniResultCodes() {
    }
}

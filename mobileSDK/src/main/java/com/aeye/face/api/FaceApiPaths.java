package com.aeye.face.api;

/**
 * SDK 后台接口路径集中管理，新增接口在此维护。
 */
public final class FaceApiPaths {

    /** POST {@code {"businessCode":"..."}} 动作活体配置 */
    public static final String ACTION_CONFIG_LIST = "/faceActionConfig/listActionConfigByBusinessType";

    /** GET {@code /faceUser/selectById/{userId}} 用户信息预览 */
    public static final String USER_SELECT_BY_ID = "/faceUser/selectById";

    /** POST {@code /assistant/faceIdent} 人脸核验 */
    public static final String FACE_IDENT = "/assistant/faceIdent";

    /** POST {@code /logManagement/saveFaceVerifyLog} 核验日志记录 */
    public static final String SAVE_FACE_VERIFY_LOG = "/logManagement/saveFaceVerifyLog";

    /** POST {@code /qrCode/insertRecord} 新增认证记录 */
    public static final String QR_CODE_INSERT_RECORD = "/qrCode/insertRecord";

    /** POST {@code /qrCode/updateRecord} 认证记录状态更新（二维码） */
    public static final String QR_CODE_UPDATE_RECORD = "/qrCode/updateRecord";

    /** POST 炫彩颜色序列（flashUrl 基地址下） */
    public static final String THUNDER_ALIVE_COLOR = "/alg-api/liveness/thunderAliveColor";

    /** POST {@code /assistant/thunderAliveColor} 炫彩获取颜色（apiBaseUrl 基地址，无请求参数，isNewColorIntenface=true 时使用） */
    public static final String ASSISTANT_THUNDER_ALIVE_COLOR = "/assistant/thunderAliveColor";

    /** POST 炫彩服务端活体验证（flashUrl 基地址下） */
    public static final String THUNDER_ALIVE_CHECK = "/alg-api/liveness/thunderAliveCheck";

    /** 炫彩请求来源标识（SDK 内部固定） */
    public static final String THUNDER_SOURCE = "app";
}

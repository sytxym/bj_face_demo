package com.aeye.face.api;

/**
 * SDK 后台接口路径集中管理，新增接口在此维护。
 * <p>业务接口均带 {@code /fiv} 上下文前缀。</p>
 */
public final class FaceApiPaths {

    /** 新接口统一上下文前缀 */
    public static final String API_CONTEXT = "/fiv";

    /** POST {@code {"businessCode":"..."}} 动作活体配置 */
    public static final String ACTION_CONFIG_LIST = API_CONTEXT + "/faceActionConfig/listActionConfigByBusinessType";

    /** GET {@code /fivweb/faceUser/selectById/{userId}} 用户信息预览 */
    public static final String USER_SELECT_BY_ID = API_CONTEXT + "/faceUser/selectById";

    /** POST {@code /fivweb/assistant/faceIdent} 人脸核验 */
    public static final String FACE_IDENT = API_CONTEXT + "/assistant/faceIdent";

    /** POST {@code /fivweb/logManagement/saveFaceVerifyLog} 核验日志记录 */
    public static final String SAVE_FACE_VERIFY_LOG = API_CONTEXT + "/logManagement/saveFaceVerifyLog";

    /** POST {@code /fivweb/qrCode/insertRecord} 新增认证记录 */
    public static final String QR_CODE_INSERT_RECORD = API_CONTEXT + "/qrCode/insertRecord";

    /** POST {@code /fivweb/qrCode/updateRecord} 认证记录状态更新（二维码） */
    public static final String QR_CODE_UPDATE_RECORD = API_CONTEXT + "/qrCode/updateRecord";

    /** POST {@code /fivweb/qrCode/authStatus} 查询核验结果状态 */
    public static final String QR_CODE_AUTH_STATUS = API_CONTEXT + "/qrCode/authStatus";

    /** POST {@code /fivweb/assistant/thunderAliveColor} 炫彩获取颜色（apiBaseUrl 基地址，无请求参数） */
    public static final String ASSISTANT_THUNDER_ALIVE_COLOR = API_CONTEXT + "/assistant/thunderAliveColor";
}

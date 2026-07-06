package com.aeye.face.api;

/**
 * SDK 后台接口路径集中管理，新增接口在此维护。
 */
public final class FaceApiPaths {

    /** GET {@code ?businessCode=} 动作活体配置 */
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
}

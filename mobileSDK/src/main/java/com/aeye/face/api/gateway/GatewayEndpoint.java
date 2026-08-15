package com.aeye.face.api.gateway;

import com.aeye.face.api.FaceApiPaths;

/**
 * SDK 内需要经网关转发的接口清单：REST path → 网关 interface_id 映射。
 * <p>只有这里列出的 6 个接口会在 {@code AEFaceSdk.isUseGateway()==true} 时改走网关；
 * 老炫彩接口（{@link FaceApiPaths#THUNDER_ALIVE_COLOR}/{@link FaceApiPaths#THUNDER_ALIVE_CHECK}，
 * 均为 {@code /alg-api/liveness/*}）不在此清单中，不受网关开关影响，
 * 后续部署环境下线这两个老接口时，只需删掉对应调用方代码，不涉及本清单。</p>
 */
public enum GatewayEndpoint {

    /** 动作活体配置查询 */
    ACTION_CONFIG_LIST(FaceApiPaths.ACTION_CONFIG_LIST, "listActionConfigByBusinessType"),
    /** 人脸核验（动作活体 + 炫彩活体共用同一 path，interfaceId 均为 faceIdent1） */
    FACE_IDENT(FaceApiPaths.FACE_IDENT, "faceIdent1"),
    /** 核验日志上报 */
    SAVE_FACE_VERIFY_LOG(FaceApiPaths.SAVE_FACE_VERIFY_LOG, "saveFaceVerifyLog"),
    /** 新增认证记录 */
    QR_CODE_INSERT_RECORD(FaceApiPaths.QR_CODE_INSERT_RECORD, "addRecord"),
    /** 认证状态更新 */
    QR_CODE_UPDATE_RECORD(FaceApiPaths.QR_CODE_UPDATE_RECORD, "updateRecord"),
    /** 炫彩活体获取颜色（新接口，{@code isNewColorIntenface=true} 时使用） */
    ASSISTANT_THUNDER_ALIVE_COLOR(FaceApiPaths.ASSISTANT_THUNDER_ALIVE_COLOR, "thunderAliveColor");

    private final String path;
    private final String interfaceId;

    GatewayEndpoint(String path, String interfaceId) {
        this.path = path;
        this.interfaceId = interfaceId;
    }

    public String getPath() {
        return path;
    }

    public String getInterfaceId() {
        return interfaceId;
    }

    /** 按 REST path 查找对应网关配置；不在清单内返回 {@code null}（调用方应直连，不走网关）。 */
    public static GatewayEndpoint byPath(String path) {
        if (path == null) {
            return null;
        }
        for (GatewayEndpoint endpoint : values()) {
            if (endpoint.path.equals(path)) {
                return endpoint;
            }
        }
        return null;
    }
}

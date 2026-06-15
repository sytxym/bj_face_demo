package com.aeye.face.verify;

import android.content.Context;
import android.text.TextUtils;

import com.aeye.face.AEFaceSdk;
import com.aeye.face.api.FaceApiService;
import com.aeye.face.config.FaceActionConfig;
import com.aeye.face.config.FaceActionConfigManager;
import com.aeye.face.confirm.InfoConfirmManager;
import com.aeye.face.confirm.InfoConfirmPayload;
import com.aeye.face.uitls.DeviceInfoCollector;

import org.json.JSONObject;

/**
 * 核验日志上报：无人脸、核验结束两种场景，异步 fire-and-forget，不处理返回业务。
 */
public final class FaceVerifyLogManager {

    /** operType：活体检测 */
    public static final String OPER_TYPE_LIVENESS = "1";
    /** result：成功 */
    public static final String RESULT_SUCCESS = "0";
    /** result：失败 */
    public static final String RESULT_FAIL = "1";
    /** riskType：无人脸 */
    public static final String RISK_TYPE_NO_FACE = "2";

    private FaceVerifyLogManager() {
    }

    /** 取景过程出现无人脸时调用（每次触发上报一次）。 */
    public static void uploadNoFace(Context context) {
        submit(context, buildNoFaceBody(context));
    }

    /** 本地核验结束：成功 result=0，失败 result=1（同一会话仅上报一次）。 */
    public static void uploadVerifyEnd(Context context, boolean success, String failReason) {
        if (!FaceVerifySession.tryMarkEndLogSent()) {
            return;
        }
        submit(context, buildVerifyEndBody(context, success, failReason));
    }

    private static void submit(Context context, JSONObject body) {
        if (context == null || body == null) {
            return;
        }
        new Thread(() -> {
            try {
                AEFaceSdk.ensureInitialized();
                FaceApiService.saveFaceVerifyLog(AEFaceSdk.getApiBaseUrl(), body.toString());
            } catch (Exception ignored) {
                // 日志接口失败不影响主流程
            }
        }, "AEFace-VerifyLog").start();
    }

    static JSONObject buildNoFaceBody(Context context) {
        JSONObject body = buildCommonBody(context);
        try {
            body.put("operType", OPER_TYPE_LIVENESS);
            body.put("riskType", RISK_TYPE_NO_FACE);
        } catch (Exception ignored) {
        }
        return body;
    }

    static JSONObject buildVerifyEndBody(Context context, boolean success, String failReason) {
        JSONObject body = buildCommonBody(context);
        try {
            body.put("operType", OPER_TYPE_LIVENESS);
            body.put("result", success ? RESULT_SUCCESS : RESULT_FAIL);
            if (!success && !TextUtils.isEmpty(failReason)) {
                body.put("failReason", failReason);
            }
        } catch (Exception ignored) {
        }
        return body;
    }

    private static JSONObject buildCommonBody(Context context) {
        JSONObject body = DeviceInfoCollector.collect(context);
        try {
            InfoConfirmPayload user = InfoConfirmManager.getCached();
            FaceActionConfig config = FaceActionConfigManager.getCached();

            putIfNotEmpty(body, "userId", FaceVerifySession.getUserId());
            if (user != null) {
                putIfNotEmpty(body, "name", user.getRealName());
                putIfNotEmpty(body, "certType", user.getIdType());
                putIfNotEmpty(body, "certNo", user.getIdNumber());
                putIfNotEmpty(body, "userType", user.getUserType());
            }
            if (config != null) {
                putIfNotEmpty(body, "businessCode", config.getBusinessCode());
                putIfNotEmpty(body, "businessName", config.getBusinessName());
            } else {
                putIfNotEmpty(body, "businessCode", FaceVerifySession.getBusinessCode());
            }
            putIfNotEmpty(body, "busId", FaceVerifySession.getAuthRecordId());
            putIfNotEmpty(body, "source", AEFaceSdk.getLogSource());
        } catch (Exception ignored) {
        }
        return body;
    }

    private static void putIfNotEmpty(JSONObject json, String key, String value) {
        if (json == null || TextUtils.isEmpty(key) || TextUtils.isEmpty(value)) {
            return;
        }
        try {
            json.put(key, value);
        } catch (Exception ignored) {
        }
    }
}

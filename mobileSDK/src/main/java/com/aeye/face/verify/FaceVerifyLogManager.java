package com.aeye.face.verify;

import android.content.Context;
import android.text.TextUtils;

import com.aeye.face.AEFaceSdk;
import com.aeye.face.api.FaceApiService;
import com.aeye.face.config.FaceActionConfig;
import com.aeye.face.config.FaceActionConfigManager;

import org.json.JSONObject;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;

/**
 * 核验日志上报（{@code saveFaceVerifyLog}）：字段与后台接口文档对齐。
 * 场景 A 无人脸：{@code operType + riskType}；场景 B 核验结束：{@code operType + result}（失败可带 failReason）。
 * 异步 fire-and-forget，不处理返回业务。
 */
public final class FaceVerifyLogManager {

    /** operType：活体检测（移动端固定 1） */
    public static final String OPER_TYPE_LIVENESS = "1";
    /** result：成功 */
    public static final String RESULT_SUCCESS = "0";
    /** result：失败 */
    public static final String RESULT_FAIL = "1";
    /** riskType：无人脸 */
    public static final String RISK_TYPE_NO_FACE = "2";

    private FaceVerifyLogManager() {
    }

    /** 场景 A：取景过程出现无人脸时调用（每次触发上报一次）。 */
    public static void uploadNoFace(Context context) {
        if (FaceVerifySession.isLocalVerifyOnly()) {
            return;
        }
        submit(context, buildNoFaceBody(context));
    }

    /** 场景 B：本地活体结束，成功 result=0，失败 result=1（同一会话仅上报一次）。 */
    public static void uploadVerifyEnd(Context context, boolean success, String failReason) {
        if (FaceVerifySession.isLocalVerifyOnly()) {
            return;
        }
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

    /**
     * 文档公共可选字段：userId / businessCode / businessName / busId / source / requestIp。
     * 不传 brand、证件号等文档未列出的字段。
     */
    private static JSONObject buildCommonBody(Context context) {
        JSONObject body = new JSONObject();
        try {
            FaceActionConfig config = FaceActionConfigManager.getCached();
            putIfNotEmpty(body, "userId", FaceVerifySession.getUserId());
            if (config != null) {
                putIfNotEmpty(body, "businessCode", config.getBusinessCode());
                putIfNotEmpty(body, "businessName", config.getBusinessName());
            } else {
                putIfNotEmpty(body, "businessCode", FaceVerifySession.getBusinessCode());
            }
            putIfNotEmpty(body, "busId", FaceVerifySession.getAuthRecordId());
            putIfNotEmpty(body, "source", AEFaceSdk.getLogSource());
            putIfNotEmpty(body, "requestIp", resolveRequestIp());
        } catch (Exception ignored) {
        }
        return body;
    }

    /**
     * 取本机有效 IPv4。不使用 {@code WifiInfo.getIpAddress()}：Android 10+ 常返回 0，
     * 且依赖 ACCESS_WIFI_STATE，移动数据场景也拿不到地址。
     */
    private static String resolveRequestIp() {
        try {
            for (NetworkInterface nif : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!nif.isUp() || nif.isLoopback()) {
                    continue;
                }
                for (InetAddress addr : Collections.list(nif.getInetAddresses())) {
                    if (!(addr instanceof Inet4Address)
                            || addr.isLoopbackAddress()
                            || addr.isLinkLocalAddress()
                            || addr.isAnyLocalAddress()) {
                        continue;
                    }
                    String host = addr.getHostAddress();
                    if (!TextUtils.isEmpty(host) && !"0.0.0.0".equals(host)) {
                        return host;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return "";
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

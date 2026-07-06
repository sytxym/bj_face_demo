package com.aeye.face.api;

import android.content.Context;
import android.text.TextUtils;

import com.aeye.face.AEFaceSdk;
import com.aeye.face.api.model.ApiResult;
import com.aeye.face.api.model.FaceIdentResult;
import com.aeye.face.api.model.QrInsertRecordResult;
import com.aeye.face.config.FaceActionConfig;
import com.aeye.face.config.FaceActionConfigDefaults;
import com.aeye.face.config.FaceActionConfigParser;
import com.aeye.face.confirm.InfoConfirmDefaults;
import com.aeye.face.confirm.InfoConfirmParser;
import com.aeye.face.confirm.InfoConfirmPayload;
import com.aeye.face.uitls.DeviceInfoCollector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * SDK 后台接口统一入口：请求、Mock 回退、解析均在此扩展。
 */
public final class FaceApiService {

    private FaceApiService() {
    }

    // ---------- 动作活体配置 ----------

    public static String fetchActionConfigJson(String baseUrl, String businessCode) throws Exception {
        String code = TextUtils.isEmpty(businessCode)
                ? FaceActionConfigDefaults.DEFAULT_BUSINESS_CODE
                : businessCode;
        return SdkHttpClient.get(
                baseUrl,
                FaceApiPaths.ACTION_CONFIG_LIST,
                SdkHttpClient.query("businessCode", code));
    }

    public static FaceActionConfig parseActionConfig(String json) {
        return FaceActionConfigParser.parse(json);
    }

    public static FaceActionConfig mockActionConfig() {
        return mockActionConfig(FaceActionConfigDefaults.DEFAULT_BUSINESS_CODE);
    }

    /**
     * Mock 回退：{@code businessCode}/{@code businessName} 与宿主请求参数保持一致。
     */
    public static FaceActionConfig mockActionConfig(String businessCode) {
        FaceActionConfig config = parseActionConfig(FaceActionConfigDefaults.FALLBACK_JSON);
        String code = TextUtils.isEmpty(businessCode)
                ? FaceActionConfigDefaults.DEFAULT_BUSINESS_CODE
                : businessCode.trim();
        config.setBusinessCode(code);
        config.setBusinessName(FaceActionConfigDefaults.resolveBusinessName(code));
        return config;
    }

    // ---------- 用户信息预览 ----------
    public static String fetchUserPreviewJson(String baseUrl, String userId) throws Exception {
        if (TextUtils.isEmpty(userId)) {
            throw new IllegalArgumentException("userId 为空");
        }
        return SdkHttpClient.get(
                baseUrl,
                FaceApiPaths.USER_SELECT_BY_ID + "/" + SdkHttpClient.encodePathSegment(userId));
    }

    public static InfoConfirmPayload parseUserPreview(String json) {
        return InfoConfirmParser.parse(json);
    }

    public static InfoConfirmPayload mockUserPreview() {
        return parseUserPreview(InfoConfirmDefaults.FALLBACK_JSON);
    }

    // ---------- 新增认证记录 ----------

    /**
     * 组装 {@code /qrCode/insertRecord} 请求体。
     * <p>注册场景无 userId 时传 {@code certNo}；其他场景传 {@code userId}。
     * {@code busType}/{@code busId} 来自动作配置接口返回值。</p>
     */
    public static String buildInsertRecordRequestJson(Context context,
                                                      FaceActionConfig config,
                                                      String userId,
                                                      String certNo) throws JSONException {
        if (config == null) {
            throw new IllegalArgumentException("动作配置为空");
        }
        JSONObject req = new JSONObject();
        if (FaceActionConfigDefaults.isRegisterScene(config.getBusinessCode())) {
            if (TextUtils.isEmpty(userId)) {
                if (TextUtils.isEmpty(certNo)) {
                    throw new IllegalArgumentException("注册场景 certNo 为空");
                }
                req.put("certNo", certNo.trim());
            } else {
                req.put("userId", userId.trim());
            }
        } else {
            if (TextUtils.isEmpty(userId)) {
                throw new IllegalArgumentException("userId 为空");
            }
            req.put("userId", userId.trim());
        }
        req.put("busType", config.getBusinessCode());
        req.put("busId", String.valueOf(config.getActionConfigId()));
        JSONObject device = DeviceInfoCollector.collect(context);
        req.put("brand", device.optString("brand", ""));
        req.put("model", device.optString("model", ""));
        req.put("osType", device.optString("osType", DeviceInfoCollector.OS_TYPE_ANDROID));
        req.put("osVersion", device.optString("osVersion", ""));
        req.put("deviceId", device.optString("deviceId", ""));
        req.put("source", AEFaceSdk.getLogSource());
        return req.toString();
    }

    public static QrInsertRecordResult insertQrCodeRecord(String baseUrl, String jsonBody) throws Exception {
        String response = SdkHttpClient.postJson(baseUrl, FaceApiPaths.QR_CODE_INSERT_RECORD, jsonBody);
        return parseInsertRecordResponse(response);
    }

    public static QrInsertRecordResult mockInsertRecord(String userId) throws Exception {
        JSONObject mock = new JSONObject(QrInsertRecordDefaults.MOCK_RESPONSE_JSON);
        JSONObject businessData = ApiResponseParser.extractBusinessData(mock);
        if (businessData == null) {
            throw new IllegalArgumentException("Mock 响应 data 为空");
        }
        if (!TextUtils.isEmpty(userId)) {
            businessData.put("userId", userId);
        }
        return parseInsertRecordResponse(mock.toString());
    }

    public static QrInsertRecordResult parseInsertRecordResponse(String json) throws JSONException {
        ApiResult apiResult = ApiResponseParser.parse(json);
        JSONObject data = apiResult.getBusinessData();
        if (data == null) {
            throw new IllegalArgumentException("insertRecord 响应 data 为空");
        }
        String authRecordId = data.optString("authRecordId", null);
        if (TextUtils.isEmpty(authRecordId) && data.has("authRecordId")) {
            authRecordId = String.valueOf(data.optLong("authRecordId", 0L));
            if ("0".equals(authRecordId)) {
                authRecordId = null;
            }
        }
        return new QrInsertRecordResult(
                data.optString("userId", null),
                authRecordId);
    }

    // ---------- 人脸核验 ----------

    /**
     * 活体完成后提交人脸核验。
     *
     * @param livenessJson {@code onFinish} 中返回的采集 JSON（含 images 数组）
     */
    public static FaceIdentResult submitFaceIdent(String baseUrl,
                                                  String livenessJson,
                                                  String userId,
                                                  String authRecordId) throws Exception {
        if (TextUtils.isEmpty(userId)) {
            throw new IllegalArgumentException("userId 为空");
        }
        String body = buildFaceIdentRequestJson(livenessJson, userId, authRecordId);
        String response = SdkHttpClient.postJson(baseUrl, FaceApiPaths.FACE_IDENT, body);
        return parseFaceIdentResponse(response);
    }

    public static FaceIdentResult mockFaceIdentPass(String userId, String authRecordId) throws Exception {
        JSONObject mock = new JSONObject(FaceIdentDefaults.MOCK_RESPONSE_JSON);
        JSONObject businessData = ApiResponseParser.extractBusinessData(mock);
        if (businessData == null) {
            throw new IllegalArgumentException("Mock 响应 data 为空");
        }
        if (!TextUtils.isEmpty(userId)) {
            businessData.put("userId", userId);
        }
        if (!TextUtils.isEmpty(authRecordId)) {
            businessData.put("authRecordId", parseAuthRecordId(authRecordId));
        }
        return parseFaceIdentResponse(mock.toString());
    }

    /**
     * 从活体 JSON 组装核验请求体。
     * facePic1=正脸(images[0])，facePic2~facePic6=抓拍图(images[1]~[5])。
     */
    public static String buildFaceIdentRequestJson(String livenessJson,
                                                   String userId,
                                                   String authRecordId) throws JSONException {
        JSONObject live = new JSONObject(livenessJson);
        JSONArray images = live.optJSONArray("images");
        if (images == null || images.length() == 0) {
            throw new IllegalArgumentException("活体图片为空");
        }
        JSONObject req = new JSONObject();
        req.put("userId", userId);
        if (!TextUtils.isEmpty(authRecordId)) {
            req.put("authRecordId", parseAuthRecordId(authRecordId));
        }
        req.put("facePic1", images.getString(0));
        for (int picIndex = 2; picIndex <= 6; picIndex++) {
            int imageIndex = picIndex - 1;
            String key = "facePic" + picIndex;
            if (images.length() > imageIndex) {
                req.put(key, images.getString(imageIndex));
            } else {
                req.put(key, JSONObject.NULL);
            }
        }
        return req.toString();
    }

    public static FaceIdentResult parseFaceIdentResponse(String json) throws JSONException {
        JSONObject root = new JSONObject(json);
        if (root.has("isPass")) {
            return fromFaceIdentJson(root);
        }
        if (root.has("ok")) {
            ApiResult apiResult = ApiResponseParser.parse(root);
            JSONObject data = apiResult.getBusinessData();
            if (data == null) {
                throw new IllegalArgumentException("核验响应 data 为空");
            }
            return fromFaceIdentJson(data);
        }
        throw new IllegalArgumentException("无法解析人脸核验响应");
    }

    private static FaceIdentResult fromFaceIdentJson(JSONObject json) {
        String isPass = json.optString("isPass", "0");
        return new FaceIdentResult(
                "1".equals(isPass),
                json.optString("userId", null),
                json.optLong("authRecordId", 0L),
                isPass,
                json.optInt("code", 0));
    }

    private static long parseAuthRecordId(String authRecordId) {
        try {
            return Long.parseLong(authRecordId.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("authRecordId 格式错误: " + authRecordId);
        }
    }

    // ---------- 核验日志 ----------

    /**
     * 上报核验日志，返回结构与配置接口一致；data 为空对象，客户端无需处理。
     */
    public static void saveFaceVerifyLog(String baseUrl, String jsonBody) throws Exception {
        String response = SdkHttpClient.postJson(baseUrl, FaceApiPaths.SAVE_FACE_VERIFY_LOG, jsonBody);
        ApiResponseParser.parse(response);
    }

    /**
     * 更新二维码认证记录状态；返回结构与配置/日志接口一致，data 为空对象。
     */
    public static void updateQrCodeRecord(String baseUrl, String jsonBody) throws Exception {
        String response = SdkHttpClient.postJson(baseUrl, FaceApiPaths.QR_CODE_UPDATE_RECORD, jsonBody);
        ApiResponseParser.parse(response);
    }
}

package com.aeye.face.api;

import android.content.Context;
import android.text.TextUtils;

import com.aeye.face.AEFaceSdk;
import com.aeye.face.api.model.ApiResult;
import com.aeye.face.api.model.ColorResponseBean;
import com.aeye.face.api.model.FaceIdentResult;
import com.aeye.face.api.model.LightAliveResponse;
import com.aeye.face.api.model.QrInsertRecordResult;
import com.aeye.face.config.FaceActionConfig;
import com.aeye.face.config.FaceActionConfigDefaults;
import com.aeye.face.config.FaceActionConfigParser;
import com.aeye.face.confirm.InfoConfirmDefaults;
import com.aeye.face.confirm.InfoConfirmParser;
import com.aeye.face.confirm.InfoConfirmPayload;
import com.aeye.face.uitls.DeviceInfoCollector;
import com.aeye.face.verify.FaceUserInfo;
import com.aeye.face.verify.FaceVerifySession;

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
        JSONObject body = new JSONObject();
        body.put("businessCode", code);
        return SdkHttpClient.postJson(
                baseUrl,
                FaceApiPaths.ACTION_CONFIG_LIST,
                body.toString());
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

    // ---------- 用户信息预览（已取消调用：用户基本信息改由外部业务 App 传入，方法保留以兼容） ----------
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
    /** faceIdent 含多张人脸图，上传超时需长于普通 8s 接口 */
    private static final int FACE_IDENT_TIMEOUT_MS = 40_000;

    public static FaceIdentResult submitFaceIdent(String baseUrl,
                                                  String livenessJson,
                                                  String userId,
                                                  String authRecordId) throws Exception {
        if (TextUtils.isEmpty(userId)) {
            throw new IllegalArgumentException("userId 为空");
        }
        String body = buildFaceIdentRequestJson(livenessJson, userId, authRecordId);
        String response = SdkHttpClient.postJson(
                baseUrl, FaceApiPaths.FACE_IDENT, body,
                FACE_IDENT_TIMEOUT_MS, FACE_IDENT_TIMEOUT_MS);
        return parseFaceIdentResponse(response);
    }

    /**
     * 炫彩活体完成后提交人脸核验（新接口，isNewColorIntenface=true 时使用）。
     * <p>炫彩字段：{@code isColor=true}、{@code seq}（拉色接口返回的唯一序列）、
     * {@code colorPics}（算法图，与老接口 thunderAliveCheck 的 {@code alivePics} 同数据）。
     * {@code facePic1~facePic6} 与动作活体保持一致（人脸原图）。
     * 基本信息字段（certName/certType/certNo/country/busId）取自外部业务 App
     * 传入的 {@link FaceUserInfo}。</p>
     *
     * @param facePics  解密后的人脸原图 base64 列表，映射 facePic1~facePic6
     * @param colorPics 炫彩算法图 base64 列表（同老接口 alivePics 数据）
     */
    public static FaceIdentResult submitFaceIdentColor(String baseUrl,
                                                       String userId,
                                                       String authRecordId,
                                                       String businessCode,
                                                       String seq,
                                                       JSONArray facePics,
                                                       JSONArray colorPics) throws Exception {
        String body = buildFaceIdentColorRequestJson(userId, authRecordId, businessCode, seq, facePics, colorPics);
        String response = SdkHttpClient.postJson(
                baseUrl, FaceApiPaths.FACE_IDENT, body,
                FACE_IDENT_TIMEOUT_MS, FACE_IDENT_TIMEOUT_MS);
        return parseFaceIdentResponse(response);
    }

    public static String buildFaceIdentColorRequestJson(String userId,
                                                        String authRecordId,
                                                        String businessCode,
                                                        String seq,
                                                        JSONArray facePics,
                                                        JSONArray colorPics) throws JSONException {
        if (facePics == null || facePics.length() == 0) {
            throw new IllegalArgumentException("炫彩人脸原图为空");
        }
        JSONObject req = new JSONObject();
        if (!TextUtils.isEmpty(userId)) {
            req.put("userId", userId.trim());
        }
        if (!TextUtils.isEmpty(businessCode)) {
            req.put("businessCode", businessCode.trim());
        }
        if (!TextUtils.isEmpty(authRecordId)) {
            req.put("authRecordId", parseAuthRecordId(authRecordId));
        }
        appendUserIdentityFields(req);
        // facePic1~facePic6 与动作活体一致：facePic1 正脸，其余抓拍图
        req.put("facePic1", facePics.getString(0));
        for (int picIndex = 2; picIndex <= 6; picIndex++) {
            int imageIndex = picIndex - 1;
            String key = "facePic" + picIndex;
            if (facePics.length() > imageIndex) {
                req.put(key, facePics.getString(imageIndex));
            } else {
                req.put(key, JSONObject.NULL);
            }
        }
        // 炫彩字段
        req.put("isColor", true);
        if (!TextUtils.isEmpty(seq)) {
            try {
                req.put("seq", Long.parseLong(seq.trim()));
            } catch (NumberFormatException e) {
                req.put("seq", seq.trim());
            }
        }
        req.put("colorPics", colorPics != null ? colorPics : new JSONArray());
        return req.toString();
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
        putIfNotEmpty(req, "businessCode", FaceVerifySession.getBusinessCode());
        appendUserIdentityFields(req);
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
        // 炫彩活体：透传 lightData / sequnce / alignData（后台若不需要可忽略）
        if (live.has("lightData") && !TextUtils.isEmpty(live.optString("lightData"))) {
            req.put("lightData", live.optString("lightData"));
        }
        if (live.has("sequnce") && !TextUtils.isEmpty(live.optString("sequnce"))) {
            req.put("sequnce", live.optString("sequnce"));
        }
        if (live.has("alignData") && !TextUtils.isEmpty(live.optString("alignData"))) {
            req.put("alignData", live.optString("alignData"));
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

    /**
     * 追加用户基本信息字段（certName/certType/certNo/country/busId）：
     * 由外部业务 App 传入、经 {@link FaceVerifySession} 透传，未传入时不追加。
     */
    private static void appendUserIdentityFields(JSONObject req) throws JSONException {
        FaceUserInfo info = FaceVerifySession.getUserInfo();
        if (info == null) {
            return;
        }
        putIfNotEmpty(req, "certName", info.getCertName());
        putIfNotEmpty(req, "certType", info.getCertType());
        putIfNotEmpty(req, "certNo", info.getCertNo());
        putIfNotEmpty(req, "country", info.getCountry());
        putIfNotEmpty(req, "busId", info.getBusId());
    }

    private static void putIfNotEmpty(JSONObject req, String key, String value) throws JSONException {
        if (!TextUtils.isEmpty(value)) {
            req.put(key, value.trim());
        }
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

    // ---------- 炫彩 Thunder（flashUrl 基地址） ----------

    /**
     * 炫彩接口含多张 base64 大图上传+服务端验活，超时需明显高于普通业务接口。
     * 参考 demo OkHttp 亦为 30s 量级；此处放宽到 60s，避免弱网/大图误报超时。
     */
    private static final int THUNDER_TIMEOUT_MS = 60_000;

    /**
     * 获取炫彩颜色序列（同步）。
     *
     * @param flashUrl 炫彩服务基地址
     * @param jsonBody 已组装的请求体（含 app_id / sn / riskType 等）
     */
    public static ColorResponseBean fetchThunderColor(String flashUrl, String jsonBody) throws Exception {
        String response = SdkHttpClient.postJson(
                flashUrl, FaceApiPaths.THUNDER_ALIVE_COLOR, jsonBody,
                THUNDER_TIMEOUT_MS, THUNDER_TIMEOUT_MS);
        return ColorResponseBean.parse(response);
    }

    /**
     * 炫彩服务端活体验证（同步）。
     *
     * @param flashUrl 炫彩服务基地址
     * @param jsonBody 已组装的请求体（含 sequence / alivePics / facePic 等）
     */
    public static LightAliveResponse checkThunderAlive(String flashUrl, String jsonBody) throws Exception {
        String response = SdkHttpClient.postJson(
                flashUrl, FaceApiPaths.THUNDER_ALIVE_CHECK, jsonBody,
                THUNDER_TIMEOUT_MS, THUNDER_TIMEOUT_MS);
        return LightAliveResponse.parse(response);
    }

    // ---------- 炫彩获取颜色（正式新接口，apiBaseUrl 基地址） ----------

    /**
     * 炫彩活体获取颜色（isNewColorIntenface=true 时使用）。
     * <p>POST {@code {apiBaseUrl}/assistant/thunderAliveColor}，无请求参数；
     * 响应外层为统一 {@code {ok, data:{data:{...}}}} 结构，
     * 业务节点含 {@code color1/color2/color3/sequnce}。</p>
     */
    public static ColorResponseBean.ColorsBean fetchAssistantThunderColor(String baseUrl) throws Exception {
        String response = SdkHttpClient.postJson(
                baseUrl, FaceApiPaths.ASSISTANT_THUNDER_ALIVE_COLOR, "{}",
                THUNDER_TIMEOUT_MS, THUNDER_TIMEOUT_MS);
        ApiResult apiResult = ApiResponseParser.parse(response);
        JSONObject data = apiResult.getBusinessData();
        if (data == null) {
            throw new IllegalArgumentException("炫彩颜色响应 data 为空");
        }
        ColorResponseBean.ColorsBean colors = new ColorResponseBean.ColorsBean();
        colors.setColor1(parseColorBean(data.optJSONObject("color1")));
        colors.setColor2(parseColorBean(data.optJSONObject("color2")));
        colors.setColor3(parseColorBean(data.optJSONObject("color3")));
        if (colors.getColor1() == null || colors.getColor2() == null || colors.getColor3() == null) {
            throw new IllegalArgumentException("炫彩颜色响应缺少 color1/color2/color3");
        }
        String seq = data.optString("sequnce", null);
        if (TextUtils.isEmpty(seq)) {
            throw new IllegalArgumentException("炫彩颜色响应缺少 sequnce");
        }
        colors.setSequnce(seq);
        return colors;
    }

    private static ColorResponseBean.ColorsBean.Color1Bean parseColorBean(JSONObject json) {
        if (json == null) {
            return null;
        }
        ColorResponseBean.ColorsBean.Color1Bean bean = new ColorResponseBean.ColorsBean.Color1Bean();
        bean.setC1(json.optInt("c1", 0));
        bean.setC2(json.optInt("c2", 0));
        bean.setC3(json.optInt("c3", 0));
        return bean;
    }
}

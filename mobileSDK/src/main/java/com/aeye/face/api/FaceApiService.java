package com.aeye.face.api;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.Base64;

import com.aeye.android.uitls.BitmapUtils;
import com.aeye.face.AEFaceParam;
import com.aeye.face.AEFaceSdk;
import com.aeye.face.api.gateway.GatewayCrypto;
import com.aeye.face.api.model.ApiResult;
import com.aeye.face.api.model.ColorResponseBean;
import com.aeye.face.api.model.FaceIdentResult;
import com.aeye.face.api.model.QrInsertRecordResult;
import com.aeye.face.config.FaceActionConfig;
import com.aeye.face.config.FaceActionConfigDefaults;
import com.aeye.face.config.FaceActionConfigParser;
import com.aeye.face.confirm.InfoConfirmDefaults;
import com.aeye.face.confirm.InfoConfirmParser;
import com.aeye.face.confirm.InfoConfirmPayload;
import com.aeye.face.uitls.DeviceInfoCollector;
import com.aeye.face.uitls.SMUtil;
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
     * 组装 {@code /fivweb/qrCode/insertRecord} 请求体。
     * <p>{@code userId}/{@code businessCode} 来自 {@link FaceVerifySession}；
     * {@code busId} 由外部业务 App 经 {@link FaceUserInfo} 传入。</p>
     */
    public static String buildInsertRecordRequestJson() throws JSONException {
        String userId = FaceVerifySession.getUserId();
        if (TextUtils.isEmpty(userId)) {
            FaceUserInfo userInfo = FaceVerifySession.getUserInfo();
            if (userInfo != null) {
                userId = userInfo.getUserId();
            }
        }
        if (TextUtils.isEmpty(userId)) {
            throw new IllegalArgumentException("userId 为空");
        }
        String businessCode = FaceVerifySession.getBusinessCode();
        if (TextUtils.isEmpty(businessCode)) {
            throw new IllegalArgumentException("businessCode 为空");
        }
        FaceUserInfo userInfo = FaceVerifySession.getUserInfo();
        String busId = userInfo != null ? userInfo.getBusId() : null;
        if (TextUtils.isEmpty(busId)) {
            throw new IllegalArgumentException("busId 为空");
        }
        JSONObject req = new JSONObject();
        req.put("userId", userId.trim());
        req.put("businessCode", businessCode.trim());
        req.put("busId", busId.trim());
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
        String status = data.optString("status", null);
        if (TextUtils.isEmpty(status) && data.has("status")) {
            status = String.valueOf(data.optInt("status", -1));
            if ("-1".equals(status)) {
                status = null;
            }
        }
        return new QrInsertRecordResult(
                data.optString("userId", null),
                authRecordId,
                status);
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
     * 炫彩活体完成后提交人脸核验。
     * <p>炫彩字段：{@code isColor=true}、{@code seq}（拉色接口返回的唯一序列）、
     * {@code colorPics}（算法色序图）。{@code facePic1~facePic6} 与动作活体保持一致（人脸原图）。
     * 基本信息字段（certName/certType/certNo/country/busId）取自外部业务 App
     * 传入的 {@link FaceUserInfo}。</p>
     *
     * @param facePics  解密后的人脸原图 base64 列表，映射 facePic1~facePic6
     * @param colorPics 炫彩算法图 base64 列表
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
        applyFaceIdentFieldEncryption(req);
        return req.toString();
    }

    public static FaceIdentResult mockFaceIdentPass(String userId, String authRecordId) throws Exception {
        return parseFaceIdentResponse(FaceIdentDefaults.MOCK_RESPONSE_JSON);
    }

    /**
     * 从活体 JSON 组装核验请求体。
     * facePic1=正脸(images[0])，facePic2~facePic6=抓拍图(images[1]~[5])。
     * 动作活体采集图为 PNG，提交前转为 JPEG 以降低网关 form 体积（与炫彩人脸图质量一致）。
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
        req.put("facePic1", toJpegFacePic(images.getString(0), live));
        for (int picIndex = 2; picIndex <= 6; picIndex++) {
            int imageIndex = picIndex - 1;
            String key = "facePic" + picIndex;
            if (images.length() > imageIndex) {
                req.put(key, toJpegFacePic(images.getString(imageIndex), live));
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
        applyFaceIdentFieldEncryption(req);
        return req.toString();
    }

    public static FaceIdentResult parseFaceIdentResponse(String json) {
        ApiResponseParser.assertOk(json);
        return FaceIdentResult.pass();
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

    /** 与炫彩人脸图一致：JPEG 质量 90，降低网关 form 体积。 */
    private static final int FACE_IDENT_JPEG_QUALITY = 90;
    private static final String DEFAULT_SM4_KEY = "E3A03D4A1586F6952F0E699344D0F4E2";

    /**
     * 动作活体 images[] 多为 PNG（可能再套 SM4）。提交 faceIdent 前转为 JPEG。
     * 解码失败则回退原串，避免空图导致核验失败。
     */
    private static String toJpegFacePic(String raw, JSONObject live) {
        if (TextUtils.isEmpty(raw)) {
            return raw;
        }
        Bitmap bmp = decodeLivenessImage(raw, live);
        if (bmp == null || bmp.isRecycled()) {
            return raw;
        }
        try {
            String jpeg = BitmapUtils.convertJpegToString(bmp, FACE_IDENT_JPEG_QUALITY);
            return TextUtils.isEmpty(jpeg) ? raw : jpeg;
        } finally {
            bmp.recycle();
        }
    }

    private static Bitmap decodeLivenessImage(String raw, JSONObject live) {
        try {
            boolean crypt = live != null && live.optBoolean("isCrypt", false);
            int encType = live != null
                    ? live.optInt("enCryptType", AEFaceParam.ENCRYPT_TYPE_NULL)
                    : AEFaceParam.ENCRYPT_TYPE_NULL;
            if (crypt && encType == AEFaceParam.ENCRYPT_TYPE_SM4) {
                String key = live.optString("decryptKey", DEFAULT_SM4_KEY);
                if (TextUtils.isEmpty(key)) {
                    key = DEFAULT_SM4_KEY;
                }
                return SMUtil.DataSM4Decode(key, raw);
            }
            byte[] bytes = Base64.decode(raw, Base64.DEFAULT);
            if (bytes == null || bytes.length == 0) {
                return null;
            }
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * faceIdent 接口文档要求 SM2 字段级加密的敏感字段：
     * certName/certType/certNo/country、facePic1~facePic6、colorPics（数组每项）。
     * 走网关时加密后的 JSON 还会作为 biz_content 整体再做一次 SM2 加密。
     */
    private static final String[] FACE_IDENT_SM2_STRING_FIELDS = {
            "certName", "certType", "certNo", "country",
            "facePic1", "facePic2", "facePic3", "facePic4", "facePic5", "facePic6"
    };

    private static void applyFaceIdentFieldEncryption(JSONObject req) throws JSONException {
//        if (!AEFaceSdk.isUseGateway()) {
//            return;
//        }
//        for (String key : FACE_IDENT_SM2_STRING_FIELDS) {
//            encryptFaceIdentFieldIfPresent(req, key);
//        }
//        encryptColorPicsIfPresent(req);
    }

    private static void encryptFaceIdentFieldIfPresent(JSONObject req, String key) throws JSONException {
        if (!req.has(key) || req.isNull(key)) {
            return;
        }
        String plain = req.optString(key, null);
        if (TextUtils.isEmpty(plain)) {
            return;
        }
        req.put(key, sm2EncryptField(key, plain));
    }

    /** colorPics 为 List&lt;String&gt;，文档要求数组内每一项单独 SM2 加密。 */
    private static void encryptColorPicsIfPresent(JSONObject req) throws JSONException {
        if (!req.has("colorPics") || req.isNull("colorPics")) {
            return;
        }
        JSONArray colorPics = req.optJSONArray("colorPics");
        if (colorPics == null || colorPics.length() == 0) {
            return;
        }
        JSONArray encrypted = new JSONArray();
        for (int i = 0; i < colorPics.length(); i++) {
            Object item = colorPics.opt(i);
            if (item == null || item == JSONObject.NULL) {
                encrypted.put(JSONObject.NULL);
                continue;
            }
            String plain = String.valueOf(item);
            if (TextUtils.isEmpty(plain)) {
                encrypted.put(JSONObject.NULL);
                continue;
            }
            encrypted.put(sm2EncryptField("colorPics[" + i + "]", plain));
        }
        req.put("colorPics", encrypted);
    }

    private static String sm2EncryptField(String fieldName, String plain) {
        String encrypted = GatewayCrypto.sm2EncryptToHex(plain);
        if (TextUtils.isEmpty(encrypted)) {
            throw new IllegalStateException("faceIdent 字段 SM2 加密失败: " + fieldName);
        }
        return encrypted;
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
     * 上报核验日志；成功仅看外层 {@code ok}，{@code data} 可为 null。
     */
    public static void saveFaceVerifyLog(String baseUrl, String jsonBody) throws Exception {
        String response = SdkHttpClient.postJson(baseUrl, FaceApiPaths.SAVE_FACE_VERIFY_LOG, jsonBody);
        ApiResponseParser.assertOk(response);
    }

    /**
     * 更新二维码认证记录状态；成功仅看外层 {@code ok}，{@code data} 可为 null。
     */
    public static void updateQrCodeRecord(String baseUrl, String jsonBody) throws Exception {
        String response = SdkHttpClient.postJson(baseUrl, FaceApiPaths.QR_CODE_UPDATE_RECORD, jsonBody);
        ApiResponseParser.assertOk(response);
    }

    // ---------- 炫彩拉色 ----------

    /**
     * 炫彩接口含多张 base64 大图上传+服务端验活，超时需明显高于普通业务接口。
     * 参考 demo OkHttp 亦为 30s 量级；此处放宽到 60s，避免弱网/大图误报超时。
     */
    private static final int THUNDER_TIMEOUT_MS = 60_000;

    /**
     * 炫彩活体获取颜色。
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

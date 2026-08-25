package com.aeye.face.api.gateway;

import android.text.TextUtils;
import android.util.Log;

import com.aeye.face.api.SdkHttpClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;

/**
 * 网关请求统一入口。
 * <p>把 SDK 现有的 {@code (path, jsonBody)} 请求，按 {@link GatewayEndpoint} 配置包装成网关信封格式
 * （{@code app_id/interface_id/version/biz_content/header/charset/timestamp/origin/sign}）发送，
 * 再把网关响应解包、SM2 解密回 SDK 自身的 {@code {"ok":true,"data":{"data":...}}} 结构，
 * 使 {@code FaceApiService}/{@code ApiResponseParser} 等既有业务解析代码完全不用感知网关的存在
 * ——调用层封装思路参考内部网关 SDK 的 {@code JAGSRepository.requestJAGS()}。</p>
 * <p>签名/加密算法源码内嵌本包（不依赖对方私有 Maven 仓库），底层 SM2/SM3 原语使用
 * 公开的 BouncyCastle（{@code org.bouncycastle:bcprov-jdk18on}，来自 Maven Central），
 * 依赖引入方式参考内部网关 SDK 早期版本（源码内嵌，而非私有组件）。</p>
 */
public final class GatewayHttpClient {

    private static final String TAG = "GatewayHttpClient";

    private GatewayHttpClient() {
    }

    /**
     * 发起网关请求。
     *
     * @param gatewayUrl  网关地址，见 {@code AEFaceSdk.setGatewayUrl}
     * @param endpoint    接口配置（interfaceId 等）
     * @param bizJsonBody 业务参数 JSON（明文，加密前）
     * @return 适配后的响应 JSON（{@code {"ok":true,"data":{"data":...}}} 结构）
     */
    public static String postJson(String gatewayUrl, GatewayEndpoint endpoint, String bizJsonBody,
                                  int connectTimeoutMs, int readTimeoutMs) throws Exception {
        if (TextUtils.isEmpty(gatewayUrl)) {
            throw new IllegalStateException("网关地址未配置，请先调用 AEFaceSdk.setGatewayUrl()");
        }
        if (endpoint == null) {
            throw new IllegalArgumentException("网关接口配置为空");
        }

        String plainBizContent = TextUtils.isEmpty(bizJsonBody) ? "{}" : bizJsonBody;
        String bizContent = plainBizContent;
//        String bizContent = Sm2Cipher.encryptToHex(plainBizContent, GatewayConfig.SM2_PUBLIC_KEY);
        if (TextUtils.isEmpty(bizContent)) {
            throw new IllegalStateException("网关请求 biz_content SM2 加密失败[" + endpoint.getInterfaceId() + "]");
        }

        String timestamp = String.valueOf(System.currentTimeMillis());
        String header = "{}";
        String sign = GatewaySigner.createSign(
                GatewayConfig.APP_ID, endpoint.getInterfaceId(), GatewayConfig.VERSION,
                bizContent, GatewayConfig.CHARSET, timestamp, GatewayConfig.ORIGIN_APP);
        if (TextUtils.isEmpty(sign)) {
            throw new IllegalStateException("网关请求签名生成失败[" + endpoint.getInterfaceId() + "]");
        }

        String formBody = buildForm(
                GatewayConfig.APP_ID, endpoint.getInterfaceId(), GatewayConfig.VERSION,
                bizContent, header, GatewayConfig.CHARSET, timestamp, GatewayConfig.ORIGIN_APP, sign);

        logRequest(gatewayUrl, endpoint, timestamp, header, sign, plainBizContent, bizContent);
        if (bizContent.length() > 1_000_000) {
            Log.w(TAG, "postJson: interfaceId=" + endpoint.getInterfaceId()
                    + " biz_content(hex) 长度=" + bizContent.length()
                    + " 字节，加上表单其余字段后整包请求体可能超过网关/服务器默认的表单大小上限"
                    + "（如 Tomcat maxPostSize 默认 2MB），若网关返回字段全部为空/必填校验失败，优先怀疑这里，"
                    + "需要网关侧调大 post 请求体大小限制。");
        }

        String rawResponse = SdkHttpClient.postForm(gatewayUrl, formBody, connectTimeoutMs, readTimeoutMs);
        return adaptResponse(endpoint, rawResponse);
    }

    private static String buildForm(String appId, String interfaceId, String version, String bizContent,
                                    String header, String charset, String timestamp, String origin,
                                    String sign) throws Exception {
        StringBuilder sb = new StringBuilder();
        appendField(sb, "app_id", appId);
        appendField(sb, "interface_id", interfaceId);
        appendField(sb, "version", version);
        appendField(sb, "biz_content", bizContent);
        appendField(sb, "header", header);
        appendField(sb, "charset", charset);
        appendField(sb, "timestamp", timestamp);
        appendField(sb, "origin", origin);
        appendField(sb, "sign", sign);
        return sb.toString();
    }

    private static void appendField(StringBuilder sb, String key, String value) throws Exception {
        if (sb.length() > 0) {
            sb.append('&');
        }
        sb.append(key).append('=').append(URLEncoder.encode(value != null ? value : "", "UTF-8"));
    }

    /**
     * 打印网关请求字段，含 {@code biz_content} 加密前明文（联调排查用，注意：明文含业务数据，
     * 正式对外发布版本建议关掉或走 {@code AEFaceSdk.isHttpLogEnabled()} 统一开关收敛）与加密后密文。
     * 两者都可能很长（人脸图片类接口明文/密文都到 MB 级），超过阈值只截取首尾，避免刷屏 Logcat。
     */
    private static void logRequest(String gatewayUrl, GatewayEndpoint endpoint, String timestamp,
                                   String header, String sign, String plainBizContent, String bizContent) {
        Log.d(TAG, "postJson request -> url=" + gatewayUrl
                + ", app_id=" + GatewayConfig.APP_ID
                + ", interface_id=" + endpoint.getInterfaceId()
                + ", version=" + GatewayConfig.VERSION
                + ", charset=" + GatewayConfig.CHARSET
                + ", origin=" + GatewayConfig.ORIGIN_APP
                + ", timestamp=" + timestamp
                + ", header=" + header
                + ", sign=" + sign
                + ", biz_content_plain(len=" + plainBizContent.length() + ")=" + summarize(plainBizContent)
                + ", biz_content_encrypted(hex,len=" + bizContent.length() + ")=" + summarize(bizContent));
    }

    private static String summarize(String text) {
        int max = 1000;
        if (text.length() <= max) {
            return text;
        }
        int half = max / 2;
        return text.substring(0, half) + "...(省略 " + (text.length() - max) + " 字符)..."
                + text.substring(text.length() - half);
    }

    /**
     * 把网关信封解包为 SDK 自身的 {@code {"ok":true,"data":{"data":...}}} 结构。
     * <p>网关外层字段目前按 {@code success/code/msg/data} 假设（与内部网关 SDK 的
     * {@code GatewayResponse} 一致）。{@code data} 可能是 SM2 hex 密文（生产），
     * 也可能是明文 JSON 字符串（联调）；已是 {@code ok/data} 业务信封时直接透传。</p>
     */
    private static String adaptResponse(GatewayEndpoint endpoint, String rawResponse) throws Exception {
        if (TextUtils.isEmpty(rawResponse)) {
            throw new IllegalStateException("网关响应为空[" + endpoint.getInterfaceId() + "]");
        }
        JSONObject resp;
        try {
            resp = new JSONObject(rawResponse);
        } catch (JSONException e) {
            throw new IllegalStateException("网关响应非 JSON[" + endpoint.getInterfaceId() + "]: " + rawResponse);
        }

        boolean success = !resp.has("success") || resp.optBoolean("success", true);
        if (!success) {
            String msg = resp.optString("msg", "网关接口异常");
            throw new IllegalStateException("网关调用失败[" + endpoint.getInterfaceId() + "]: " + msg);
        }

        String dataField = resp.optString("data", "");
        String plainJson = unwrapGatewayData(dataField, endpoint.getInterfaceId());
        Log.d(TAG, "postJson response -> interfaceId=" + endpoint.getInterfaceId()
                + ", data_raw(len=" + dataField.length() + ")=" + summarize(dataField)
                + ", data_plain(len=" + plainJson.length() + ")=" + summarize(plainJson));

        if (!TextUtils.isEmpty(plainJson)) {
            JSONObject businessEnvelope = tryParseObject(plainJson);
            if (businessEnvelope != null && businessEnvelope.has("ok")) {
                return businessEnvelope.toString();
            }
        }

        Object businessNode = TextUtils.isEmpty(plainJson) ? new JSONObject() : parseLoosely(plainJson);

        JSONObject inner = new JSONObject();
        inner.put("data", businessNode);
        JSONObject envelope = new JSONObject();
        envelope.put("ok", true);
        envelope.put("data", inner);
        return envelope.toString();
    }

    /**
     * 解包网关 {@code data}：联调环境常为明文 JSON；生产环境为 SM2 hex 密文。
     */
    private static String unwrapGatewayData(String dataField, String interfaceId) throws Exception {
        if (TextUtils.isEmpty(dataField)) {
            return "";
        }
        String trimmed = dataField.trim();
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            return trimmed;
        }
        String decrypted = Sm2Cipher.decryptFromHex(trimmed, GatewayConfig.SM2_PRIVATE_KEY);
        if (TextUtils.isEmpty(decrypted)) {
            throw new IllegalStateException("网关响应 data SM2 解密失败[" + interfaceId + "]");
        }
        return decrypted;
    }

    private static JSONObject tryParseObject(String text) {
        try {
            return new JSONObject(text);
        } catch (JSONException e) {
            return null;
        }
    }

    /** 解密后的业务数据可能是对象/数组/纯字符串，逐一尝试，保证任何形状都能正确透传。 */
    private static Object parseLoosely(String text) {
        try {
            return new JSONObject(text);
        } catch (JSONException e1) {
            try {
                return new JSONArray(text);
            } catch (JSONException e2) {
                return text;
            }
        }
    }
}

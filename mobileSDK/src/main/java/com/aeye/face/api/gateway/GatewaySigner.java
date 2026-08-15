package com.aeye.face.api.gateway;

import org.bouncycastle.crypto.digests.SM3Digest;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

/**
 * 网关请求签名。
 * <p>对 {@code app_id/interface_id/version/biz_content/charset/timestamp/origin} 七个字段
 * 按 key 升序排序、拼接成 {@code k1=v1&k2=v2...}（跳过空值），再做 SM3 摘要转十六进制。
 * 注意：{@code header}、{@code sign} 本身不参与签名，算法与参考网关 SDK
 * （jags_helper.CreateSign）逐字段保持一致。</p>
 */
final class GatewaySigner {

    private GatewaySigner() {
    }

    static String createSign(String appId, String interfaceId, String version, String bizContent,
                             String charset, String timestamp, String origin) {
        Map<String, String> fields = new TreeMap<>();
        fields.put("app_id", appId);
        fields.put("interface_id", interfaceId);
        fields.put("version", version);
        fields.put("biz_content", bizContent);
        fields.put("charset", charset);
        fields.put("timestamp", timestamp);
        fields.put("origin", origin);

        StringBuilder content = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key == null || key.isEmpty() || value == null || value.isEmpty()) {
                continue;
            }
            if (!first) {
                content.append('&');
            }
            content.append(key).append('=').append(value);
            first = false;
        }
        return sm3Hex(content.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static String sm3Hex(byte[] data) {
        SM3Digest digest = new SM3Digest();
        digest.update(data, 0, data.length);
        byte[] out = new byte[digest.getDigestSize()];
        digest.doFinal(out, 0);
        return HexCodec.bytesToHex(out);
    }
}

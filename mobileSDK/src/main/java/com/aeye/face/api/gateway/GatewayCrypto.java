package com.aeye.face.api.gateway;

/**
 * 网关/业务层共用的 SM2 加解密入口（供 {@code com.aeye.face.api} 包调用）。
 */
public final class GatewayCrypto {

    private GatewayCrypto() {
    }

    /** SM2 加密（C1C3C2），返回十六进制密文；失败返回 {@code null}。 */
    public static String sm2EncryptToHex(String plainText) {
        return Sm2Cipher.encryptToHex(plainText, GatewayConfig.SM2_PUBLIC_KEY);
    }
}

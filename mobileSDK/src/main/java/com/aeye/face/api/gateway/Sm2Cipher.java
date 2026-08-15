package com.aeye.face.api.gateway;

import org.bouncycastle.crypto.engines.SM2Engine;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.math.ec.custom.gm.SM2P256V1Curve;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * SM2 加解密（国密网关 biz_content / 响应 data 用）。
 * <p>只使用 BouncyCastle 的轻量 {@code crypto.*} API（{@link SM2Engine} 等），
 * 不调用 {@code Security.addProvider(new BouncyCastleProvider())} 注册 JCE Provider——
 * Android 系统自带 "BC" Provider，重复注册容易引发冲突/异常，轻量 API 完全不需要它。</p>
 * <p>曲线参数、密钥构造方式照抄参考网关 SDK（encrypt_gm 模块 SM2Constants/SM2KeyHelper）实现，
 * 确保与网关侧签名验证/加解密结果一致。</p>
 */
final class Sm2Cipher {

    private static final SM2P256V1Curve CURVE = new SM2P256V1Curve();
    private static final BigInteger GX =
            new BigInteger("32C4AE2C1F1981195F9904466A39C9948FE30BBFF2660BE1715A4589334C74C7", 16);
    private static final BigInteger GY =
            new BigInteger("BC3736A2F4F6779C59BDCEE36B692153D0A9877CC62A474002DF32E52139F0A0", 16);
    private static final ECPoint G_POINT = CURVE.createPoint(GX, GY);
    private static final ECDomainParameters DOMAIN_PARAMS =
            new ECDomainParameters(CURVE, G_POINT, CURVE.getOrder(), CURVE.getCofactor());

    private Sm2Cipher() {
    }

    /** SM2 加密（C1C3C2 模式），返回十六进制密文；失败返回 {@code null}。 */
    static String encryptToHex(String plainText, String publicKeyHex) {
        try {
            ECPublicKeyParameters publicKey = buildPublicKey(HexCodec.hexToBytes(publicKeyHex));
            SM2Engine engine = new SM2Engine(SM2Engine.Mode.C1C3C2);
            engine.init(true, new ParametersWithRandom(publicKey, new SecureRandom()));
            byte[] data = plainText.getBytes(StandardCharsets.UTF_8);
            byte[] encrypted = engine.processBlock(data, 0, data.length);
            return HexCodec.bytesToHex(encrypted);
        } catch (Exception e) {
            return null;
        }
    }

    /** SM2 解密（C1C3C2 模式），入参为十六进制密文；失败返回 {@code null}。 */
    static String decryptFromHex(String cipherHex, String privateKeyHex) {
        try {
            ECPrivateKeyParameters privateKey = buildPrivateKey(HexCodec.hexToBytes(privateKeyHex));
            SM2Engine engine = new SM2Engine(SM2Engine.Mode.C1C3C2);
            engine.init(false, privateKey);
            byte[] data = HexCodec.hexToBytes(cipherHex);
            byte[] decrypted = engine.processBlock(data, 0, data.length);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    private static ECPublicKeyParameters buildPublicKey(byte[] uncompressedPoint) {
        ECPoint point = CURVE.decodePoint(uncompressedPoint);
        return new ECPublicKeyParameters(point, DOMAIN_PARAMS);
    }

    private static ECPrivateKeyParameters buildPrivateKey(byte[] rawKey) {
        return new ECPrivateKeyParameters(new BigInteger(1, rawKey), DOMAIN_PARAMS);
    }
}

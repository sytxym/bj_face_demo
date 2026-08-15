package com.aeye.face.api.gateway;

/**
 * 网关请求固定配置。
 * <p>appId、SM2 公私钥当前为测试环境固定值（正式环境如更换，只需改这里，不影响其余代码）。
 * 签名/加密算法与参考网关 SDK（内部 jags_helper + encrypt_gm）保持一致，
 * 便于跟对方网关联调时互相核对结果。</p>
 */
final class GatewayConfig {

    /** 网关分配的 app_id */
    static final String APP_ID = "zshgapp1";
    /** 网关接口版本号，固定 1.0 */
    static final String VERSION = "1.0";
    /** 请求编码 */
    static final String CHARSET = "UTF-8";
    /** 接口渠道：0 PC，1 APP，2 支付宝，3 微信；SDK 固定走 APP */
    static final String ORIGIN_APP = "1";

    /** SM2 公钥（十六进制，04 开头未压缩点），加密 biz_content 用 */
    static final String SM2_PUBLIC_KEY =
            "04c5750d95a58b83c6f5e500fd84622ae3e7806f0c746e1482d071ab4020ffa68c05e2a9ee9da24ccb84410ac513b848fe483c9c7522ba32714d78528b8ceb16aa";
    /** SM2 私钥（十六进制），解密网关响应 data 用 */
    static final String SM2_PRIVATE_KEY =
            "00935845aeccdccf6c77b9861029f3785a99f598bb8901ac9d5a88b1eb1bbf3307";

    private GatewayConfig() {
    }
}

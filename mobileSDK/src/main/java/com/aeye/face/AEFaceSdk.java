package com.aeye.face;

import android.text.TextUtils;

/**
 * SDK 全局配置。宿主在 Application 或首个 Activity 中调用 {@link #init(String)} 一次即可。
 */
public final class AEFaceSdk {

    private static volatile String apiBaseUrl;
    private static volatile boolean useMockOnError = true;
    /**
     * 炫彩接口切换开关（调试用，与 {@link #useMockOnError} 相互独立）：
     * <ul>
     * <li>{@code true}：新接口 —— 拉色走 {@code {apiBaseUrl}/assistant/thunderAliveColor}（无参），
     *     核验走 {@code {apiBaseUrl}/assistant/faceIdent}（带 isColor/seq/colorPics 炫彩字段）；</li>
     * <li>{@code false}：老接口 —— 拉色走 {@code alg-api/liveness/thunderAliveColor}，
     *     核验只走 {@code alg-api/liveness/thunderAliveCheck}，不再调用 faceIdent。</li>
     * </ul>
     */
    private static volatile boolean isNewColorIntenface = false;
    /** 是否在 Logcat 输出网络请求 URL、参数与响应（默认开启，便于调试）。 */
    private static volatile boolean httpLogEnabled = true;
    /** 日志 source：见 {@link #setLogSource(String)} 取值说明（掌上海关 APP 默认 6）。 */
    private static volatile String logSource = "6";
    /** 炫彩服务端基地址（可为空，启动时也可由 Bundle ThunderFlashUrl 覆盖） */
    private static volatile String thunderFlashUrl = "";
    private static volatile String thunderAppId = "";
    private static volatile String thunderAppSecret = "";
    /**
     * 网关接入开关：{@code true} 时，{@code com.aeye.face.api.gateway.GatewayEndpoint} 清单内的接口
     * （动作配置查询/人脸核验/核验日志/新增认证记录/认证状态更新/炫彩获取颜色）改为经网关转发，
     * 老炫彩接口 {@code /alg-api/liveness/thunderAliveColor|thunderAliveCheck} 不受此开关影响，
     * 始终直连（这两个接口后续会整体下线，不纳入网关改造范围）。
     * 默认 {@code false}，与现网直连行为完全一致，联调网关时显式打开即可，出问题可随时关回直连。
     */
    private static volatile boolean useGateway = false;
    /** 网关地址，如 {@code https://xxx/empgatewayserver/interface/gateway.do}；开启网关前必须设置。 */
    private static volatile String gatewayUrl = "";

    private AEFaceSdk() {
    }

    /**
     * @param apiBaseUrl 后台接口根地址，如 {@code http://10.0.2.2:8080}
     */
    public static void init(String apiBaseUrl) {
        init(apiBaseUrl, true);
    }

    /**
     * @param apiBaseUrl      后台接口根地址
     * @param useMockOnError  接口不可用时是否使用 SDK 内置 Mock 数据（调试建议 true）
     */
    public static void init(String apiBaseUrl, boolean useMockOnError) {
        AEFaceSdk.apiBaseUrl = apiBaseUrl != null ? apiBaseUrl.trim() : "";
        AEFaceSdk.useMockOnError = useMockOnError;
    }

    /**
     * 配置炫彩 Thunder 服务凭证（拉色 / 服务端验活）。也可在启动 Bundle 中传入同名参数覆盖。
     */
    public static void setThunderCredentials(String flashUrl, String appId, String appSecret) {
        thunderFlashUrl = flashUrl != null ? flashUrl.trim() : "";
        thunderAppId = appId != null ? appId.trim() : "";
        thunderAppSecret = appSecret != null ? appSecret.trim() : "";
    }

    public static String getThunderFlashUrl() {
        return thunderFlashUrl;
    }

    public static String getThunderAppId() {
        return thunderAppId;
    }

    public static String getThunderAppSecret() {
        return thunderAppSecret;
    }

    public static String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public static boolean isUseMockOnError() {
        return useMockOnError;
    }

    /** 炫彩接口切换开关，见 {@link #isNewColorIntenface} 字段说明。 */
    public static void setNewColorIntenface(boolean newColorIntenface) {
        isNewColorIntenface = newColorIntenface;
    }

    public static boolean isNewColorIntenface() {
        return isNewColorIntenface;
    }

    /** 开启/关闭 SDK 网络请求日志，Logcat 过滤 {@code AEFaceApi}。 */
    public static void setHttpLogEnabled(boolean enabled) {
        httpLogEnabled = enabled;
    }

    public static boolean isHttpLogEnabled() {
        return httpLogEnabled;
    }

    /**
     * 核验来源 {@code source} 字段，宿主启动时传入。
     * <p>取值：1 单一窗口 PC；2 掌上单一窗口 APP；3 掌上单一窗口微信小程序；
     * 4 掌上单一窗口支付宝小程序；5 掌上海关 PC；6 掌上海关 APP；
     * 7 掌上海关微信小程序；8 掌上海关支付宝小程序；9 其他。</p>
     *
     * @param source 来源标识，如掌上海关 APP 传 {@code "6"}
     */
    public static void setLogSource(String source) {
        if (!TextUtils.isEmpty(source)) {
            logSource = source.trim();
        }
    }

    public static String getLogSource() {
        return logSource;
    }

    /** 开启/关闭网关转发，见 {@link #useGateway} 字段说明。 */
    public static void setUseGateway(boolean enabled) {
        useGateway = enabled;
    }

    public static boolean isUseGateway() {
        return useGateway;
    }

    /**
     * 配置网关地址。{@link #setUseGateway(boolean)} 打开前必须设置，否则网关请求会直接抛异常失败。
     */
    public static void setGatewayUrl(String url) {
        gatewayUrl = url != null ? url.trim() : "";
    }

    public static String getGatewayUrl() {
        return gatewayUrl;
    }

    public static void ensureInitialized() {
        if (TextUtils.isEmpty(apiBaseUrl)) {
            throw new IllegalStateException("请先调用 AEFaceSdk.init(apiBaseUrl)");
        }
    }
}

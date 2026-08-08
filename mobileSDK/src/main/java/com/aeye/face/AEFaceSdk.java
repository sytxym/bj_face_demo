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
    /** 日志 source：2 掌上单一窗口 APP，5 掌上海关 APP（默认 5）。 */
    private static volatile String logSource = "5";
    /** 炫彩服务端基地址（可为空，启动时也可由 Bundle ThunderFlashUrl 覆盖） */
    private static volatile String thunderFlashUrl = "";
    private static volatile String thunderAppId = "";
    private static volatile String thunderAppSecret = "";

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
     * 核验日志 {@code source} 字段，宿主启动时传入。
     *
     * @param source 2：掌上单一窗口 APP；5：掌上海关 APP
     */
    public static void setLogSource(String source) {
        if (!TextUtils.isEmpty(source)) {
            logSource = source.trim();
        }
    }

    public static String getLogSource() {
        return logSource;
    }

    public static void ensureInitialized() {
        if (TextUtils.isEmpty(apiBaseUrl)) {
            throw new IllegalStateException("请先调用 AEFaceSdk.init(apiBaseUrl)");
        }
    }
}

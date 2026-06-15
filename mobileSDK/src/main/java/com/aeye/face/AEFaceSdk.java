package com.aeye.face;

import android.text.TextUtils;

/**
 * SDK 全局配置。宿主在 Application 或首个 Activity 中调用 {@link #init(String)} 一次即可。
 */
public final class AEFaceSdk {

    private static volatile String apiBaseUrl;
    private static volatile boolean useMockOnError = true;
    /** 是否在 Logcat 输出网络请求 URL、参数与响应（默认开启，便于调试）。 */
    private static volatile boolean httpLogEnabled = true;
    /** 日志 source：2 掌上单一窗口 APP，5 掌上海关 APP（默认 5）。 */
    private static volatile String logSource = "5";

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

    public static String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public static boolean isUseMockOnError() {
        return useMockOnError;
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

package com.aeye.face.config;

import android.os.Bundle;

import com.aeye.face.AEFaceSdk;

/**
 * 动作活体配置对外门面：拉取后台配置并写入 SDK 启动参数。
 */
public final class FaceActionConfigManager {

    private FaceActionConfigManager() {
    }

    /** 使用 {@link AEFaceSdk#init(String)} 配置的 baseUrl 拉取动作配置。 */
    public static void fetch(String businessCode, FaceActionConfigRepository.FetchCallback callback) {
        AEFaceSdk.ensureInitialized();
        FaceActionConfigRepository.fetch(
                AEFaceSdk.getApiBaseUrl(),
                businessCode,
                AEFaceSdk.isUseMockOnError(),
                callback);
    }

    public static void fetch(String baseUrl, String businessCode,
                             FaceActionConfigRepository.FetchCallback callback) {
        FaceActionConfigRepository.fetch(baseUrl, businessCode, callback);
    }

    public static FaceActionConfig getCached() {
        return FaceActionConfigRepository.getCached();
    }

    public static boolean isLastFetchFromRemote() {
        return FaceActionConfigRepository.isLastFetchFromRemote();
    }

    public static void applyToSdkBundle(Bundle paras, FaceActionConfig config) {
        FaceActionConfigSdkMapper.applyToBundle(paras, config);
    }

    public static void applyCachedToSdkBundle(Bundle paras) {
        applyToSdkBundle(paras, getCached());
    }
}

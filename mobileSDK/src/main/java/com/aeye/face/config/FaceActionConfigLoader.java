package com.aeye.face.config;

/**
 * 动作活体配置加载器。默认实现见 {@link DefaultFaceActionConfigLoader}；
 * 宿主也可自定义实现（鉴权、网关等）后通过 {@link FaceActionConfigRepository#fetch(FaceActionConfigLoader, String, FaceActionConfigRepository.FetchCallback)} 注入。
 */
public interface FaceActionConfigLoader {

    interface Callback {
        /** @param fromRemote true=来自后台 HTTP；false=内置回退 JSON */
        void onSuccess(FaceActionConfig config, boolean fromRemote);

        void onError(String message);
    }

    void fetchByBusinessCode(String businessCode, Callback callback);
}

package com.aeye.face.confirm;

/**
 * 实名信息预览加载器。宿主可注入 HTTP 实现；Demo 可用本地 JSON 解析实现。
 */
public interface InfoConfirmLoader {

    interface Callback {
        /** @param fromRemote true=网络接口；false=本地/Mock 数据 */
        void onSuccess(InfoConfirmPayload payload, boolean fromRemote);

        void onError(String message);
    }

    void fetch(Callback callback);
}

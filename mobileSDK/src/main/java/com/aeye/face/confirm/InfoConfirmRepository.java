package com.aeye.face.confirm;

/**
 * 实名信息预览：拉取、缓存（甲方集成入口之一）。
 */
public final class InfoConfirmRepository {

    private static volatile InfoConfirmPayload cached;
    private static volatile boolean lastFetchFromRemote;

    private InfoConfirmRepository() {
    }

    public interface FetchCallback {
        void onSuccess(InfoConfirmPayload payload, boolean fromRemote);

        void onError(String message);
    }

    public static InfoConfirmPayload getCached() {
        return cached;
    }

    public static boolean isLastFetchFromRemote() {
        return lastFetchFromRemote;
    }

    public static void setCached(InfoConfirmPayload payload) {
        cached = payload;
    }

    public static void fetch(InfoConfirmLoader loader, FetchCallback callback) {
        if (loader == null) {
            if (callback != null) {
                callback.onError("InfoConfirmLoader 为空");
            }
            return;
        }
        loader.fetch(new InfoConfirmLoader.Callback() {
            @Override
            public void onSuccess(InfoConfirmPayload payload, boolean fromRemote) {
                cached = payload;
                lastFetchFromRemote = fromRemote;
                if (callback != null) {
                    callback.onSuccess(payload, fromRemote);
                }
            }

            @Override
            public void onError(String message) {
                if (callback != null) {
                    callback.onError(message);
                }
            }
        });
    }

    /** 解析 JSON 并缓存（宿主自行请求网络或 Mock 时可调用） */
    public static InfoConfirmPayload parseAndCache(String json) {
        InfoConfirmPayload payload = InfoConfirmParser.parse(json);
        cached = payload;
        lastFetchFromRemote = true;
        return payload;
    }
}

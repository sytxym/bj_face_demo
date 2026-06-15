package com.aeye.face.confirm;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.aeye.face.api.FaceApiService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SDK 默认预览加载：通过 {@link FaceApiService} 拉取；失败时回退 Mock。
 */
public final class DefaultInfoConfirmLoader implements InfoConfirmLoader {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private final String baseUrl;
    private final String userId;
    private final boolean useFallbackOnError;

    public DefaultInfoConfirmLoader(String baseUrl, String userId) {
        this(baseUrl, userId, true);
    }

    public DefaultInfoConfirmLoader(String baseUrl, String userId, boolean useFallbackOnError) {
        this.baseUrl = baseUrl;
        this.userId = userId;
        this.useFallbackOnError = useFallbackOnError;
    }

    @Override
    public void fetch(final Callback callback) {
        if (TextUtils.isEmpty(userId)) {
            postError(callback, "userId 为空");
            return;
        }
        EXECUTOR.execute(() -> {
            InfoConfirmPayload payload = null;
            boolean fromRemote = false;
            try {
                payload = FaceApiService.parseUserPreview(
                        FaceApiService.fetchUserPreviewJson(baseUrl, userId));
                fromRemote = true;
            } catch (Exception ignored) {
                if (useFallbackOnError) {
                    payload = FaceApiService.mockUserPreview();
                }
            }
            if (payload == null) {
                postError(callback, "获取用户信息预览失败");
                return;
            }
            final InfoConfirmPayload result = payload;
            final boolean remote = fromRemote;
            MAIN.post(() -> {
                if (callback != null) {
                    callback.onSuccess(result, remote);
                }
            });
        });
    }

    private static void postError(final Callback callback, final String message) {
        MAIN.post(() -> {
            if (callback != null) {
                callback.onError(message);
            }
        });
    }
}

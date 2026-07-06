package com.aeye.face.config;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.aeye.face.api.FaceApiService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SDK 默认配置加载：通过 {@link FaceApiService} 拉取；失败时回退 Mock。
 */
public final class DefaultFaceActionConfigLoader implements FaceActionConfigLoader {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private final String baseUrl;
    private final String defaultBusinessCode;
    private final boolean useFallbackOnError;

    public DefaultFaceActionConfigLoader(String baseUrl, String defaultBusinessCode) {
        this(baseUrl, defaultBusinessCode, true);
    }

    public DefaultFaceActionConfigLoader(String baseUrl, String defaultBusinessCode,
                                         boolean useFallbackOnError) {
        this.baseUrl = baseUrl;
        this.defaultBusinessCode = TextUtils.isEmpty(defaultBusinessCode)
                ? FaceActionConfigDefaults.DEFAULT_BUSINESS_CODE
                : defaultBusinessCode;
        this.useFallbackOnError = useFallbackOnError;
    }

    @Override
    public void fetchByBusinessCode(String businessCode, final Callback callback) {
        final String code = TextUtils.isEmpty(businessCode) ? defaultBusinessCode : businessCode;
        EXECUTOR.execute(() -> {
            FaceActionConfig config = null;
            boolean fromRemote = false;
            try {
                config = FaceApiService.parseActionConfig(
                        FaceApiService.fetchActionConfigJson(baseUrl, code));
                fromRemote = true;
            } catch (Exception ignored) {
                if (useFallbackOnError) {
                    config = FaceApiService.mockActionConfig(code);
                }
            }
            if (config == null) {
                postError(callback, "获取动作活体配置失败");
                return;
            }
            final FaceActionConfig result = config;
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

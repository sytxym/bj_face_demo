package com.aeye.face.config;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

/**
 * 动作活体配置：拉取、缓存、驱动 SDK Bundle（甲方集成入口之一）。
 */
public final class FaceActionConfigRepository {

    private static final String PREFS = "aeye_face_action_config";
    private static final String KEY_SUMMARY = "config_summary";

    private static volatile FaceActionConfig cached;
    private static volatile boolean lastFetchFromRemote;

    private FaceActionConfigRepository() {
    }

    public interface FetchCallback {
        /** @param fromRemote true=后台接口；false=内置回退配置 */
        void onSuccess(FaceActionConfig config, boolean fromRemote);

        void onError(String message);
    }

    public static FaceActionConfig getCached() {
        return cached;
    }

    public static boolean isLastFetchFromRemote() {
        return lastFetchFromRemote;
    }

    public static void setCached(FaceActionConfig config) {
        cached = config;
    }

    /**
     * 使用默认 HTTP 拉取配置；接口失败且允许回退时仍 onSuccess（内置 JSON）。
     */
    public static void fetch(String baseUrl, String businessCode, FetchCallback callback) {
        fetch(baseUrl, businessCode, true, callback);
    }

    public static void fetch(String baseUrl, String businessCode,
                             boolean useFallbackOnError, FetchCallback callback) {
        DefaultFaceActionConfigLoader loader = new DefaultFaceActionConfigLoader(
                baseUrl, businessCode, useFallbackOnError);
        fetch(loader, businessCode, callback);
    }

    public static void fetch(FaceActionConfigLoader loader, String businessCode,
                             FetchCallback callback) {
        if (loader == null) {
            if (callback != null) {
                callback.onError("FaceActionConfigLoader 为空");
            }
            return;
        }
        loader.fetchByBusinessCode(businessCode, new FaceActionConfigLoader.Callback() {
            @Override
            public void onSuccess(FaceActionConfig config, boolean fromRemote) {
                cached = config;
                lastFetchFromRemote = fromRemote;
                if (callback != null) {
                    callback.onSuccess(config, fromRemote);
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

    public static void saveCacheSummary(Context context, FaceActionConfig config) {
        if (context == null || config == null) {
            return;
        }
        SharedPreferences sp = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_SUMMARY, config.toSummaryText()).apply();
    }

    public static String getCacheSummary(Context context) {
        if (cached != null) {
            return cached.toSummaryText();
        }
        if (context == null) {
            return "";
        }
        SharedPreferences sp = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return sp.getString(KEY_SUMMARY, "");
    }

    /** 解析 JSON 并缓存（宿主自行请求网络时可调用） */
    public static FaceActionConfig parseAndCache(String json) {
        FaceActionConfig config = FaceActionConfigParser.parse(json);
        cached = config;
        lastFetchFromRemote = true;
        return config;
    }
}

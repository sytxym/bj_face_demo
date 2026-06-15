package com.aeye.face.api;

import android.text.TextUtils;
import android.util.Log;

import com.aeye.face.AEFaceSdk;

import org.json.JSONObject;

import java.util.Map;

/**
 * SDK 网络请求日志，Logcat 过滤 {@value #TAG} 可查看全部接口调用。
 */
final class ApiLogger {

    static final String TAG = "AEFaceApi";
    private static final int MAX_LOG_LENGTH = 3500;

    private ApiLogger() {
    }

    static void logRequest(String method, String url, Map<String, String> queryParams) {
        if (!AEFaceSdk.isHttpLogEnabled()) {
            return;
        }
        Log.d(TAG, "---------- HTTP Request ----------");
        Log.d(TAG, "Method: " + method);
        Log.d(TAG, "URL: " + url);
        Log.d(TAG, "Params: " + formatParams(queryParams));
    }

    static void logPostRequest(String url, String bodySummary) {
        if (!AEFaceSdk.isHttpLogEnabled()) {
            return;
        }
        Log.d(TAG, "---------- HTTP Request ----------");
        Log.d(TAG, "Method: POST");
        Log.d(TAG, "URL: " + url);
        logLong("Body: ", bodySummary);
    }

    static void logResponse(String url, int httpCode, String body, long durationMs) {
        if (!AEFaceSdk.isHttpLogEnabled()) {
            return;
        }
        Log.d(TAG, "---------- HTTP Response ----------");
        Log.d(TAG, "URL: " + url);
        Log.d(TAG, "HTTP Code: " + httpCode);
        Log.d(TAG, "Duration: " + durationMs + " ms");
        logLong("Body: ", body);
        Log.d(TAG, "----------------------------------");
    }

    static void logError(String url, Throwable error) {
        if (!AEFaceSdk.isHttpLogEnabled()) {
            return;
        }
        Log.e(TAG, "---------- HTTP Error ----------");
        Log.e(TAG, "URL: " + url);
        Log.e(TAG, "Error: " + (error != null ? error.getMessage() : "unknown"));
        if (error != null) {
            Log.e(TAG, "Stack: ", error);
        }
        Log.e(TAG, "--------------------------------");
    }

    private static String formatParams(Map<String, String> queryParams) {
        if (queryParams == null || queryParams.isEmpty()) {
            return "{}";
        }
        try {
            JSONObject json = new JSONObject();
            for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                if (!TextUtils.isEmpty(entry.getKey())) {
                    json.put(entry.getKey(), entry.getValue());
                }
            }
            return json.toString();
        } catch (Exception e) {
            return queryParams.toString();
        }
    }

    private static void logLong(String prefix, String message) {
        String content = message == null ? "null" : message;
        if (content.length() <= MAX_LOG_LENGTH) {
            Log.d(TAG, prefix + content);
            return;
        }
        int offset = 0;
        int part = 1;
        while (offset < content.length()) {
            int end = Math.min(offset + MAX_LOG_LENGTH, content.length());
            Log.d(TAG, prefix + "[" + part + "] " + content.substring(offset, end));
            offset = end;
            part++;
        }
    }
}

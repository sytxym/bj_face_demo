package com.aeye.face.api;

import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SDK 统一 HTTP GET 客户端。
 */
public final class SdkHttpClient {

    private static final int CONNECT_TIMEOUT_MS = 8000;
    private static final int READ_TIMEOUT_MS = 8000;

    private SdkHttpClient() {
    }

    public static String get(String baseUrl, String path) throws Exception {
        return get(baseUrl, path, null);
    }

    public static String get(String baseUrl, String path, Map<String, String> queryParams) throws Exception {
        return getInternal(baseUrl, path, queryParams);
    }

    public static String postJson(String baseUrl, String path, String jsonBody) throws Exception {
        if (TextUtils.isEmpty(baseUrl)) {
            throw new IllegalArgumentException("baseUrl 为空");
        }
        String root = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String normalizedPath = path.startsWith("/") ? path : ("/" + path);
        String requestUrl = root + normalizedPath;
        ApiLogger.logPostRequest(requestUrl, summarizeJsonBody(jsonBody));
        long startMs = System.currentTimeMillis();
        try {
            URL url = new URL(requestUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setDoOutput(true);
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Content-Type", "application/json;charset=utf-8");
            byte[] payload = jsonBody != null ? jsonBody.getBytes(StandardCharsets.UTF_8) : new byte[0];
            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload);
            }
            int httpCode = conn.getResponseCode();
            InputStream stream = httpCode >= 200 && httpCode < 300
                    ? conn.getInputStream()
                    : conn.getErrorStream();
            if (stream == null) {
                IllegalStateException error = new IllegalStateException("HTTP " + httpCode + "，无响应体");
                ApiLogger.logError(requestUrl, error);
                throw error;
            }
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            } finally {
                conn.disconnect();
            }
            String body = sb.toString();
            ApiLogger.logResponse(requestUrl, httpCode, body, System.currentTimeMillis() - startMs);
            if (httpCode < 200 || httpCode >= 300) {
                throw new IllegalStateException("HTTP " + httpCode + ": " + body);
            }
            return body;
        } catch (Exception e) {
            if (!(e instanceof IllegalStateException)) {
                ApiLogger.logError(requestUrl, e);
            }
            throw e;
        }
    }

    /** 日志中省略 base64 大图，只保留字段摘要 */
    private static String summarizeJsonBody(String jsonBody) {
        if (TextUtils.isEmpty(jsonBody)) {
            return "{}";
        }
        try {
            org.json.JSONObject json = new org.json.JSONObject(jsonBody);
            for (int i = 1; i <= 6; i++) {
                String key = "facePic" + i;
                if (json.has(key) && !json.isNull(key)) {
                    String value = json.optString(key, "");
                    json.put(key, "[base64 len=" + value.length() + "]");
                }
            }
            return json.toString();
        } catch (Exception e) {
            return jsonBody.length() > 200 ? jsonBody.substring(0, 200) + "..." : jsonBody;
        }
    }

    /** 路径参数拼接到 URL 末尾，如 {@code /faceUser/selectById/demoUser001} */
    public static String encodePathSegment(String segment) {
        if (segment == null) {
            return "";
        }
        try {
            return URLEncoder.encode(segment, "UTF-8").replace("+", "%20");
        } catch (Exception e) {
            return segment;
        }
    }

    private static String getInternal(String baseUrl, String path, Map<String, String> queryParams) throws Exception {
        if (TextUtils.isEmpty(baseUrl)) {
            throw new IllegalArgumentException("baseUrl 为空");
        }
        String root = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String normalizedPath = path.startsWith("/") ? path : ("/" + path);
        StringBuilder urlBuilder = new StringBuilder(root).append(normalizedPath);
        if (queryParams != null && !queryParams.isEmpty()) {
            boolean first = true;
            for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                if (TextUtils.isEmpty(entry.getKey()) || entry.getValue() == null) {
                    continue;
                }
                urlBuilder.append(first ? '?' : '&');
                first = false;
                urlBuilder.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
                urlBuilder.append('=');
                urlBuilder.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
            }
        }
        String requestUrl = urlBuilder.toString();
        ApiLogger.logRequest("GET", requestUrl, queryParams);
        long startMs = System.currentTimeMillis();
        try {
            URL url = new URL(requestUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setRequestProperty("Accept", "application/json");
            int httpCode = conn.getResponseCode();
            InputStream stream = httpCode >= 200 && httpCode < 300
                    ? conn.getInputStream()
                    : conn.getErrorStream();
            if (stream == null) {
                IllegalStateException error = new IllegalStateException("HTTP " + httpCode + "，无响应体");
                ApiLogger.logError(requestUrl, error);
                throw error;
            }
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            } finally {
                conn.disconnect();
            }
            String body = sb.toString();
            ApiLogger.logResponse(requestUrl, httpCode, body, System.currentTimeMillis() - startMs);
            if (httpCode < 200 || httpCode >= 300) {
                throw new IllegalStateException("HTTP " + httpCode + ": " + body);
            }
            return body;
        } catch (Exception e) {
            if (!(e instanceof IllegalStateException)) {
                ApiLogger.logError(requestUrl, e);
            }
            throw e;
        }
    }

    public static Map<String, String> query(String key, String value) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(key, value);
        return map;
    }
}

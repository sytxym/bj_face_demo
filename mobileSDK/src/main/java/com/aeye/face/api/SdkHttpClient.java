package com.aeye.face.api;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SDK 统一 HTTP 客户端（基于 {@link HttpURLConnection}，SDK 内不引入第三方网络库）。
 * <p>GET / POST-JSON 共用同一套 {@link #execute} 流程：连接配置、读流、日志、异常映射、资源释放收敛一处。</p>
 */
public final class SdkHttpClient {

    private static final int CONNECT_TIMEOUT_MS = 8000;
    private static final int READ_TIMEOUT_MS = 8000;

    /** 请求体日志脱敏：超过该长度的字符串值只记录长度（base64 大图 / 图片数组等） */
    private static final int MAX_LOGGED_VALUE_LEN = 512;

    private SdkHttpClient() {
    }

    // ==================== GET ====================

    public static String get(String baseUrl, String path) throws Exception {
        return get(baseUrl, path, null);
    }

    public static String get(String baseUrl, String path, Map<String, String> queryParams) throws Exception {
        String requestUrl = buildUrl(baseUrl, path, queryParams);
        ApiLogger.logRequest("GET", requestUrl, queryParams);
        return execute("GET", requestUrl, null, CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS);
    }

    // ==================== POST JSON ====================

    public static String postJson(String baseUrl, String path, String jsonBody) throws Exception {
        return postJson(baseUrl, path, jsonBody, CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS);
    }

    /**
     * 自定义超时的 POST-JSON（炫彩等大图上传接口用更长超时）。
     */
    public static String postJson(String baseUrl, String path, String jsonBody,
                                  int connectTimeoutMs, int readTimeoutMs) throws Exception {
        String requestUrl = buildUrl(baseUrl, path, null);
        ApiLogger.logPostRequest(requestUrl, summarizeJsonBody(jsonBody));
        return execute("POST", requestUrl, jsonBody, connectTimeoutMs, readTimeoutMs);
    }

    // ==================== 统一执行 ====================

    /** 复用到失效 keep-alive 连接（Broken pipe / connection reset 等）时的自动重试次数 */
    private static final int STALE_CONN_RETRY_MAX = 1;

    /**
     * 带失效连接重试的执行入口。
     * <p>HttpURLConnection 会复用系统连接池中的 keep-alive 连接；服务端/网关把空闲连接关掉后，
     * 客户端在旧连接上写大请求体（faceIdent 多图 base64）会偶发 {@code Broken pipe}。
     * 此类错误说明请求体未完整送达、服务端未处理，重试一次是安全的。</p>
     */
    private static String execute(String method, String requestUrl, String jsonBody,
                                  int connectTimeoutMs, int readTimeoutMs) throws Exception {
        Exception lastError = null;
        for (int attempt = 0; attempt <= STALE_CONN_RETRY_MAX; attempt++) {
            try {
                return executeOnce(method, requestUrl, jsonBody, connectTimeoutMs, readTimeoutMs);
            } catch (Exception e) {
                if (attempt < STALE_CONN_RETRY_MAX && isStaleConnectionError(e)) {
                    ApiLogger.logRetry(requestUrl, attempt + 1, e);
                    lastError = e;
                    continue;
                }
                throw e;
            }
        }
        throw lastError;
    }

    /** 是否为「复用了已被服务端关闭的连接」导致的可安全重试错误 */
    private static boolean isStaleConnectionError(Exception e) {
        if (e instanceof java.io.EOFException) {
            return true;
        }
        if (!(e instanceof java.net.SocketException)
                && !(e instanceof javax.net.ssl.SSLException)) {
            return false;
        }
        String msg = e.getMessage();
        if (msg == null) {
            return false;
        }
        String lower = msg.toLowerCase();
        return lower.contains("broken pipe")
                || lower.contains("connection reset")
                || lower.contains("unexpected end of stream")
                || lower.contains("software caused connection abort");
    }

    private static String executeOnce(String method, String requestUrl, String jsonBody,
                                      int connectTimeoutMs, int readTimeoutMs) throws Exception {
        long startMs = System.currentTimeMillis();
        HttpURLConnection conn = null;
        try {
            URL url = new URL(requestUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            conn.setConnectTimeout(connectTimeoutMs);
            conn.setReadTimeout(readTimeoutMs);
            conn.setRequestProperty("Accept", "application/json");
            // 不复用 keep-alive 连接：SDK 请求频率低、间隔长，池内连接极易被服务端先行关闭，
            // 复用后在写大请求体时偶发 Broken pipe；每次新建连接可从源头规避
            conn.setRequestProperty("Connection", "close");
            if ("POST".equals(method)) {
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json;charset=utf-8");
                byte[] payload = jsonBody != null ? jsonBody.getBytes(StandardCharsets.UTF_8) : new byte[0];
                conn.setFixedLengthStreamingMode(payload.length);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(payload);
                    os.flush();
                }
            }
            int httpCode = conn.getResponseCode();
            String body = readBody(conn, httpCode, requestUrl);
            ApiLogger.logResponse(requestUrl, httpCode, body, System.currentTimeMillis() - startMs);
            if (httpCode < 200 || httpCode >= 300) {
                throw new IllegalStateException("HTTP " + httpCode + ": " + body);
            }
            return body;
        } catch (SocketTimeoutException e) {
            ApiLogger.logError(requestUrl, e);
            throw new SocketTimeoutException("网络超时，请重试");
        } catch (UnknownHostException e) {
            ApiLogger.logError(requestUrl, e);
            throw new UnknownHostException("网络异常，请稍后重试");
        } catch (ConnectException e) {
            ApiLogger.logError(requestUrl, e);
            throw new ConnectException("网络异常，请稍后重试");
        } catch (Exception e) {
            if (!(e instanceof IllegalStateException)) {
                ApiLogger.logError(requestUrl, e);
            }
            throw e;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static String readBody(HttpURLConnection conn, int httpCode, String requestUrl) throws Exception {
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
        }
        return sb.toString();
    }

    // ==================== URL 拼接 ====================

    private static String buildUrl(String baseUrl, String path, Map<String, String> queryParams) throws Exception {
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
        return urlBuilder.toString();
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

    public static Map<String, String> query(String key, String value) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(key, value);
        return map;
    }

    // ==================== 日志脱敏 ====================

    /**
     * 通用脱敏：递归把过长的字符串值（base64 大图等）替换为 {@code [len=N]}，
     * 不再耦合具体业务字段名，动作侧 facePic1..6、炫彩侧 alivePics/facePics 均适用。
     */
    private static String summarizeJsonBody(String jsonBody) {
        if (TextUtils.isEmpty(jsonBody)) {
            return "{}";
        }
        try {
            JSONObject json = new JSONObject(jsonBody);
            maskLongStrings(json);
            return json.toString();
        } catch (Exception e) {
            return jsonBody.length() > 200 ? jsonBody.substring(0, 200) + "..." : jsonBody;
        }
    }

    private static void maskLongStrings(JSONObject json) {
        List<String> keys = new ArrayList<>();
        for (java.util.Iterator<String> it = json.keys(); it.hasNext(); ) {
            keys.add(it.next());
        }
        for (String key : keys) {
            Object value = json.opt(key);
            if (value instanceof String) {
                String s = (String) value;
                if (s.length() > MAX_LOGGED_VALUE_LEN) {
                    try {
                        json.put(key, "[len=" + s.length() + "]");
                    } catch (JSONException ignored) {
                    }
                }
            } else if (value instanceof JSONArray) {
                maskLongStrings((JSONArray) value);
            } else if (value instanceof JSONObject) {
                maskLongStrings((JSONObject) value);
            }
        }
    }

    private static void maskLongStrings(JSONArray array) {
        for (int i = 0; i < array.length(); i++) {
            Object value = array.opt(i);
            if (value instanceof String) {
                String s = (String) value;
                if (s.length() > MAX_LOGGED_VALUE_LEN) {
                    try {
                        array.put(i, "[len=" + s.length() + "]");
                    } catch (JSONException ignored) {
                    }
                }
            } else if (value instanceof JSONArray) {
                maskLongStrings((JSONArray) value);
            } else if (value instanceof JSONObject) {
                maskLongStrings((JSONObject) value);
            }
        }
    }
}

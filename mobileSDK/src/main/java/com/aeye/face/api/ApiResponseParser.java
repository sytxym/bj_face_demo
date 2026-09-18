package com.aeye.face.api;

import android.text.TextUtils;

import com.aeye.face.api.model.ApiResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 统一解析后台响应。网关与直启业务体均为：
 * <pre>
 * { "code": "200", "message": "成功", "data": { ... } }
 * </pre>
 * 失败示例：{@code {"code":"500","message":"错误","data":null}}。
 * 网关仅多一层 {@code success/code/msg/data}（{@code data} 常为 JSON 字符串），解包后与直启相同。
 * 不再使用 {@code errorCode}/{@code messageList}/{@code ok}。
 */
public final class ApiResponseParser {

    public static final String SUCCESS_CODE = "200";

    private ApiResponseParser() {
    }

    /**
     * 仅校验业务 {@code code=200}（{@code data} 可为 null）。
     * 适用于 faceIdent、saveFaceVerifyLog、updateRecord 等无业务 data 的接口。
     */
    public static void assertOk(String json) {
        parse(json, false);
    }

    public static ApiResult parse(String json) {
        return parse(json, true);
    }

    public static ApiResult parse(JSONObject root) {
        return parse(root, true, true);
    }

    /**
     * 解析统一信封，{@code code!=200} 时不抛异常（查询核验等需自行处理失败码）。
     */
    public static ApiResult parseUnchecked(String json) {
        return parse(toJson(json), false, false);
    }

    private static ApiResult parse(String json, boolean requireBusinessData) {
        return parse(toJson(json), requireBusinessData, true);
    }

    private static JSONObject toJson(String json) {
        if (TextUtils.isEmpty(json)) {
            throw new IllegalArgumentException("响应为空");
        }
        try {
            return new JSONObject(json);
        } catch (JSONException e) {
            throw new IllegalArgumentException("JSON 解析失败: " + e.getMessage());
        }
    }

    private static ApiResult parse(JSONObject root, boolean requireBusinessData, boolean throwIfNotOk) {
        if (root == null) {
            throw new IllegalArgumentException("响应为空");
        }
        JSONObject envelope = unwrapToBusinessEnvelope(root);
        boolean ok = isSuccessCode(readCode(envelope));
        String message = readMessage(envelope);
        String errorCode = ok ? null : nonEmpty(readCode(envelope));
        if (!ok && throwIfNotOk) {
            throw new IllegalArgumentException(failUserMessage(envelope));
        }
        JSONObject businessData = extractBusinessData(envelope);
        if (ok && requireBusinessData && businessData == null) {
            throw new IllegalArgumentException("data 为空");
        }
        return new ApiResult(ok, errorCode, message, businessData);
    }

    /**
     * 网关外层 {@code success/msg/data} 剥掉后，得到业务信封 {@code {code,message,data}}。
     * 直启本身就是业务信封，原样返回。
     */
    public static JSONObject unwrapToBusinessEnvelope(JSONObject root) {
        if (root == null) {
            return new JSONObject();
        }
        JSONObject cur = root;
        for (int i = 0; i < 3 && isGatewayOuter(cur); i++) {
            JSONObject inner = readGatewayDataObject(cur);
            if (inner == null) {
                break;
            }
            cur = inner;
        }
        return cur;
    }

    /** 是否为网关外层（含 {@code success}+{@code msg}，业务信封用 {@code message}）。 */
    public static boolean isGatewayOuter(JSONObject obj) {
        return obj != null && obj.has("success") && obj.has("msg") && !obj.has("ok");
    }

    /**
     * 已是统一业务信封：含 {@code code}，或同时含 {@code message}+{@code data}。
     * 网关解包后若已是信封则不再二次包装。
     */
    public static boolean isBusinessEnvelope(JSONObject obj) {
        if (obj == null || isGatewayOuter(obj)) {
            return false;
        }
        if (obj.has("code") && !obj.isNull("code")) {
            return true;
        }
        return obj.has("message") && obj.has("data");
    }

    private static JSONObject readGatewayDataObject(JSONObject gateway) {
        if (gateway == null || !gateway.has("data") || gateway.isNull("data")) {
            return null;
        }
        Object raw = gateway.opt("data");
        if (raw instanceof JSONObject) {
            return (JSONObject) raw;
        }
        if (raw instanceof String) {
            String text = ((String) raw).trim();
            if (text.startsWith("{")) {
                try {
                    return new JSONObject(text);
                } catch (JSONException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    /** 业务 {@code code}，保留前导零；缺省返回 null。 */
    public static String readCode(JSONObject envelope) {
        if (envelope == null || !envelope.has("code") || envelope.isNull("code")) {
            return null;
        }
        Object raw = envelope.opt("code");
        if (raw == null) {
            return null;
        }
        String code = String.valueOf(raw).trim();
        if (code.endsWith(".0")) {
            code = code.substring(0, code.length() - 2);
        }
        if (TextUtils.isEmpty(code) || "null".equalsIgnoreCase(code)) {
            return null;
        }
        return code;
    }

    public static boolean isSuccessCode(String code) {
        return SUCCESS_CODE.equals(code);
    }

    /**
     * 读取业务信封 {@code message}。查询核验失败原因与回调 msg 用这里，不用网关外层 {@code msg}。
     */
    public static String readMessage(JSONObject envelope) {
        if (envelope == null) {
            return "";
        }
        String message = envelope.optString("message", null);
        return nonEmpty(message) != null ? message.trim() : "";
    }

    /**
     * 给用户看的失败文案：业务 {@code message}，空则「接口返回失败」。
     * {@code code} 只用于回调，不弹在提示里。
     */
    public static String failUserMessage(JSONObject envelope) {
        String message = readMessage(envelope);
        if (!TextUtils.isEmpty(message)) {
            return message;
        }
        return "接口返回失败";
    }

    /** @deprecated 请用 {@link #readMessage(JSONObject)} */
    public static String firstMessage(JSONObject root) {
        return readMessage(unwrapToBusinessEnvelope(root));
    }

    /**
     * 提取业务 JSON：信封 {@code data}；若仍套一层仅含信封字段的 {@code data.data} 则再剥一次。
     * {@code data} 为 JSON 字符串时先解析。
     */
    public static JSONObject extractBusinessData(JSONObject root) {
        JSONObject envelope = unwrapToBusinessEnvelope(root);
        JSONObject data = readDataObject(envelope);
        if (data == null) {
            return null;
        }
        if (isEnvelopeOnly(data)) {
            JSONObject inner = readDataObject(data);
            if (inner != null) {
                return inner;
            }
        }
        return data;
    }

    private static JSONObject readDataObject(JSONObject parent) {
        if (parent == null || !parent.has("data") || parent.isNull("data")) {
            return null;
        }
        JSONObject obj = parent.optJSONObject("data");
        if (obj != null) {
            return obj;
        }
        String asString = parent.optString("data", null);
        if (!TextUtils.isEmpty(asString) && asString.trim().startsWith("{")) {
            try {
                return new JSONObject(asString.trim());
            } catch (JSONException ignored) {
                return null;
            }
        }
        return null;
    }

    /** 节点本身只是信封（code/message/data），还不是业务字段。 */
    private static boolean isEnvelopeOnly(JSONObject obj) {
        if (obj == null) {
            return false;
        }
        if (!obj.has("data")) {
            return false;
        }
        if (obj.has("code") || obj.has("message") || obj.has("ok")) {
            return true;
        }
        JSONArray names = obj.names();
        return names != null && names.length() == 1 && "data".equals(names.optString(0));
    }

    private static String nonEmpty(String value) {
        if (TextUtils.isEmpty(value) || "null".equalsIgnoreCase(value.trim())) {
            return null;
        }
        return value.trim();
    }
}

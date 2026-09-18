package com.aeye.face.api.model;

import org.json.JSONObject;

/**
 * 统一业务信封。网关解包后与直启 HTTP 体相同：
 * <pre>
 * {
 *   "code": "200",
 *   "message": "成功",
 *   "data": { ... }
 * }
 * </pre>
 * 失败时 {@code code} 非 200、{@code data} 可为 null。没有 {@code errorCode}/{@code messageList}。
 */
public final class ApiResult {

    private final boolean ok;
    private final String errorCode;
    private final String message;
    private final JSONObject businessData;

    public ApiResult(boolean ok, String errorCode, String message, JSONObject businessData) {
        this.ok = ok;
        this.errorCode = errorCode;
        this.message = message;
        this.businessData = businessData;
    }

    public boolean isOk() {
        return ok;
    }

    /** 业务 {@code code}；成功（200）时为 null，失败时原样回传给 {@code onFinish.resultCode} */
    public String getErrorCode() {
        return errorCode;
    }

    /** 业务 {@code message} */
    public String getMessage() {
        return message;
    }

    /** 业务数据节点，即信封 {@code data}（若仍套一层 {@code data.data} 则已剥掉）。 */
    public JSONObject getBusinessData() {
        return businessData;
    }
}

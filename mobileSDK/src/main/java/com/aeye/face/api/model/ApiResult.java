package com.aeye.face.api.model;

import org.json.JSONObject;

/**
 * 后台统一响应外层结构。
 * <pre>
 * {
 *   "ok": true,
 *   "errorCode": null,
 *   "mygType": null,
 *   "data": { "data": { ...业务字段... } },
 *   "errors": null,
 *   "messageList": [],
 *   "messageType": null
 * }
 * </pre>
 */
public final class ApiResult {

    private final boolean ok;
    private final String errorCode;
    private final String mygType;
    private final JSONObject businessData;
    private final String errors;
    private final String messageType;

    public ApiResult(boolean ok, String errorCode, String mygType,
                     JSONObject businessData, String errors, String messageType) {
        this.ok = ok;
        this.errorCode = errorCode;
        this.mygType = mygType;
        this.businessData = businessData;
        this.errors = errors;
        this.messageType = messageType;
    }

    public boolean isOk() {
        return ok;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getMygType() {
        return mygType;
    }

    /** 业务数据节点，即 {@code data.data}（若无内层 data 则取 {@code data}）。 */
    public JSONObject getBusinessData() {
        return businessData;
    }

    public String getErrors() {
        return errors;
    }

    public String getMessageType() {
        return messageType;
    }
}

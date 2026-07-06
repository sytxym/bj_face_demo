package com.aeye.face.callback;

import android.text.TextUtils;

import com.aeye.face.AEFacePack;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 将 SDK 内部结果码映射为 UniApp 统一 {@code {code, message}} 结构。
 */
public final class FaceUniResultMapper {

    private FaceUniResultMapper() {
    }

    public static JSONObject toUniJson(int sdkValue) {
        return toUniJson(sdkValue, null);
    }

    public static JSONObject toUniJson(int sdkValue, String detailMessage) {
        JSONObject json = new JSONObject();
        try {
            int code;
            String message;
            switch (sdkValue) {
                case AEFacePack.SUCCESS:
                    code = FaceUniResultCodes.SUCCESS;
                    message = FaceUniResultCodes.MSG_SUCCESS;
                    break;
                case AEFacePack.ERROR_CANCEL:
                case AEFacePack.ERROR_OTHER_VERIFY:
                    code = FaceUniResultCodes.USER_CANCEL;
                    message = FaceUniResultCodes.MSG_USER_CANCEL;
                    break;
                case AEFacePack.ERROR_FAIL:
                case AEFacePack.ERROR_TIMEOUT:
                case AEFacePack.ERROR_CAMERA:
                case AEFacePack.ERROR_DANGER_DEVICE:
                    code = FaceUniResultCodes.AUTH_FAILED;
                    message = FaceUniResultCodes.MSG_AUTH_FAILED;
                    break;
                default:
                    code = FaceUniResultCodes.AUTH_FAILED;
                    message = FaceUniResultCodes.MSG_AUTH_FAILED;
                    break;
            }
            if (!TextUtils.isEmpty(detailMessage)
                    && code != FaceUniResultCodes.SUCCESS
                    && code != FaceUniResultCodes.USER_CANCEL) {
                // 保留 detail 供 uni 侧排查，message 仍用约定文案
                json.put("detail", detailMessage.trim());
            }
            json.put("code", code);
            json.put("message", message);
        } catch (JSONException ignored) {
            // unreachable
        }
        return json;
    }

    public static JSONObject flowErrorToUniJson(int uniCode, String message) {
        JSONObject json = new JSONObject();
        try {
            json.put("code", uniCode);
            json.put("message", TextUtils.isEmpty(message) ? defaultMessage(uniCode) : message.trim());
        } catch (JSONException ignored) {
        }
        return json;
    }

    public static String defaultMessage(int uniCode) {
        switch (uniCode) {
            case FaceUniResultCodes.SUCCESS:
                return FaceUniResultCodes.MSG_SUCCESS;
            case FaceUniResultCodes.NO_ACTIVITY:
                return FaceUniResultCodes.MSG_NO_ACTIVITY;
            case FaceUniResultCodes.MISSING_PARAMS:
                return FaceUniResultCodes.MSG_MISSING_PARAMS;
            case FaceUniResultCodes.USER_CANCEL:
                return FaceUniResultCodes.MSG_USER_CANCEL;
            case FaceUniResultCodes.PARSE_FAILED:
                return FaceUniResultCodes.MSG_PARSE_FAILED;
            case FaceUniResultCodes.AUTH_FAILED:
            default:
                return FaceUniResultCodes.MSG_AUTH_FAILED;
        }
    }

    /**
     * 在原有 {@code data} JSON 上追加 {@code uniResult}，原生宿主可忽略该字段。
     */
    public static String mergeIntoData(int sdkValue, String data) {
        return mergeIntoData(sdkValue, data, null);
    }

    public static String mergeIntoData(int sdkValue, String data, String detailMessage) {
        JSONObject uniResult = toUniJson(sdkValue, detailMessage);
        if (TextUtils.isEmpty(data)) {
            JSONObject wrapper = new JSONObject();
            try {
                wrapper.put("uniResult", uniResult);
                return wrapper.toString();
            } catch (JSONException e) {
                return uniResult.toString();
            }
        }
        if (isInvalidDataJson(data)) {
            JSONObject wrapper = new JSONObject();
            try {
                wrapper.put("uniResult", flowErrorToUniJson(
                        FaceUniResultCodes.PARSE_FAILED,
                        FaceUniResultCodes.MSG_PARSE_FAILED));
                wrapper.put("legacyData", data);
                return wrapper.toString();
            } catch (JSONException e) {
                return uniResult.toString();
            }
        }
        try {
            JSONObject root = new JSONObject(data);
            root.put("uniResult", uniResult);
            return root.toString();
        } catch (JSONException e) {
            JSONObject wrapper = new JSONObject();
            try {
                wrapper.put("uniResult", flowErrorToUniJson(
                        FaceUniResultCodes.PARSE_FAILED,
                        FaceUniResultCodes.MSG_PARSE_FAILED));
                wrapper.put("legacyData", data);
                return wrapper.toString();
            } catch (JSONException ex) {
                return uniResult.toString();
            }
        }
    }

    private static boolean isInvalidDataJson(String data) {
        return data.contains("JSONException error!");
    }
}

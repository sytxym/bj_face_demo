package com.aeye.face.callback;

import android.text.TextUtils;

import com.aeye.face.AEFacePack;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 结果码映射：把 SDK 内部结果码映射为三端统一的 {@code resultCode}/{@code resultMsg}，
 * 并注入到 {@code onFinish} 的 {@code data} JSON 顶层（同时写入 {@code code}/{@code msg}）。
 * 查询核验未通过时 {@code resultMsg}/{@code msg} 使用接口 failtype。
 */
public final class FaceUniResultMapper {

    private FaceUniResultMapper() {
    }

    /**
     * SDK 内部结果码 → 三端统一结果码（字符串，含前导零）。
     * 随 {@code data.resultCode} 下发给业务 APP。
     */
    public static String unifiedResultCode(int sdkValue) {
        switch (sdkValue) {
            case AEFacePack.SUCCESS:
                return FaceUniResultCodes.RESULT_SUCCESS;
            case AEFacePack.ERROR_TIMEOUT:
                return FaceUniResultCodes.RESULT_TIMEOUT;
            case AEFacePack.ERROR_CANCEL:
                return FaceUniResultCodes.RESULT_USER_CANCEL;
            case AEFacePack.ERROR_CAMERA:
                return FaceUniResultCodes.RESULT_CAMERA_ERROR;
            case AEFacePack.ERROR_DANGER_DEVICE:
                return FaceUniResultCodes.RESULT_DEVICE_UNSAFE;
            case AEFacePack.ERROR_OTHER_VERIFY:
                return FaceUniResultCodes.RESULT_OTHER_VERIFY;
            case AEFacePack.ERROR_FAIL:
            default:
                // 其它异常码（含炫彩算法失败等）统一归为核验失败
                return FaceUniResultCodes.RESULT_VERIFY_FAILED;
        }
    }

    /** 统一结果码对应的默认中文文案 */
    public static String unifiedResultMessage(int sdkValue) {
        return unifiedResultMessage(sdkValue, false);
    }

    /** @param submitFailure 仅对 {@link com.aeye.face.AEFacePack#ERROR_FAIL} 生效：true=提交失败，false=验证失败 */
    public static String unifiedResultMessage(int sdkValue, boolean submitFailure) {
        switch (sdkValue) {
            case AEFacePack.SUCCESS:
                return FaceUniResultCodes.RESULT_MSG_SUCCESS;
            case AEFacePack.ERROR_TIMEOUT:
                return FaceUniResultCodes.RESULT_MSG_TIMEOUT;
            case AEFacePack.ERROR_CANCEL:
                return FaceUniResultCodes.RESULT_MSG_USER_CANCEL;
            case AEFacePack.ERROR_CAMERA:
                return FaceUniResultCodes.RESULT_MSG_CAMERA_ERROR;
            case AEFacePack.ERROR_DANGER_DEVICE:
                return FaceUniResultCodes.RESULT_MSG_DEVICE_UNSAFE;
            case AEFacePack.ERROR_OTHER_VERIFY:
                return FaceUniResultCodes.RESULT_MSG_OTHER_VERIFY;
            case AEFacePack.ERROR_FAIL:
            default:
                return submitFailure
                        ? FaceUniResultCodes.RESULT_MSG_SUBMIT_FAILED
                        : FaceUniResultCodes.RESULT_MSG_LIVENESS_FAILED;
        }
    }

    /** 把统一结果码写入 data JSON 顶层（业务 APP 直接读 {@code resultCode} / {@code resultMsg}） */
    private static void putUnifiedResult(JSONObject target, int sdkValue, String detailMessage) {
        putUnifiedResult(target, sdkValue, detailMessage, false);
    }

    private static void putUnifiedResult(JSONObject target, int sdkValue,
                                         String detailMessage, boolean submitFailure) {
        if (target == null) {
            return;
        }
        try {
            target.put("resultCode", unifiedResultCode(sdkValue));
            String resultMsg = unifiedResultMessage(sdkValue, submitFailure);
            // 查询/提交未通过：msg 用 failtype（或接口返回的失败详情）
            if (sdkValue != AEFacePack.SUCCESS && submitFailure && !TextUtils.isEmpty(detailMessage)) {
                resultMsg = detailMessage.trim();
            }
            target.put("resultMsg", resultMsg);
            target.put("code", unifiedResultCode(sdkValue));
            target.put("msg", resultMsg);
            if (!TextUtils.isEmpty(detailMessage) && sdkValue != AEFacePack.SUCCESS) {
                target.put("resultDetail", detailMessage.trim());
            }
        } catch (JSONException ignored) {
        }
    }

    /** 流程前置错误码对应的默认文案（用于 {@code onError}） */
    public static String defaultMessage(int flowCode) {
        switch (flowCode) {
            case FaceUniResultCodes.NO_ACTIVITY:
                return FaceUniResultCodes.MSG_NO_ACTIVITY;
            case FaceUniResultCodes.MISSING_PARAMS:
                return FaceUniResultCodes.MSG_MISSING_PARAMS;
            case FaceUniResultCodes.PARSE_FAILED:
                return FaceUniResultCodes.MSG_PARSE_FAILED;
            case FaceUniResultCodes.AUTH_FAILED:
            default:
                return FaceUniResultCodes.MSG_AUTH_FAILED;
        }
    }

    /**
     * 在原有 {@code data} JSON 顶层注入 {@code resultCode}/{@code resultMsg}。
     */
    public static String mergeIntoData(int sdkValue, String data) {
        return mergeIntoData(sdkValue, data, null);
    }

    public static String mergeIntoData(int sdkValue, String data, String detailMessage) {
        return mergeIntoData(sdkValue, data, detailMessage, false);
    }

    public static String mergeIntoData(int sdkValue, String data,
                                       String detailMessage, boolean submitFailure) {
        if (TextUtils.isEmpty(data)) {
            JSONObject wrapper = new JSONObject();
            putUnifiedResult(wrapper, sdkValue, detailMessage, submitFailure);
            return wrapper.toString();
        }
        if (isInvalidDataJson(data)) {
            JSONObject wrapper = new JSONObject();
            try {
                wrapper.put("legacyData", data);
            } catch (JSONException ignored) {
            }
            putUnifiedResult(wrapper, sdkValue, detailMessage, submitFailure);
            return wrapper.toString();
        }
        try {
            JSONObject root = new JSONObject(data);
            putUnifiedResult(root, sdkValue, detailMessage, submitFailure);
            return root.toString();
        } catch (JSONException e) {
            JSONObject wrapper = new JSONObject();
            try {
                wrapper.put("legacyData", data);
            } catch (JSONException ignored) {
            }
            putUnifiedResult(wrapper, sdkValue, detailMessage, submitFailure);
            return wrapper.toString();
        }
    }

    private static boolean isInvalidDataJson(String data) {
        return data.contains("JSONException error!");
    }
}

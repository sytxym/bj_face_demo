package com.aeye.face.uitls;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;

import org.json.JSONObject;

/**
 * 设备信息采集，供日志上报等场景使用。
 */
public final class DeviceInfoCollector {

    /** 操作系统：Android */
    public static final String OS_TYPE_ANDROID = "1";

    private DeviceInfoCollector() {
    }

    public static JSONObject collect(Context context) {
        JSONObject json = new JSONObject();
        try {
            json.put("brand", safe(Build.BRAND));
            json.put("model", safe(Build.MODEL));
            json.put("osType", OS_TYPE_ANDROID);
            json.put("osVersion", safe(Build.VERSION.RELEASE));
            json.put("deviceId", resolveDeviceId(context));
        } catch (Exception ignored) {
        }
        return json;
    }

    public static String resolveDeviceId(Context context) {
        if (context == null) {
            return "";
        }
        try {
            String androidId = Settings.Secure.getString(
                    context.getContentResolver(), Settings.Secure.ANDROID_ID);
            return safe(androidId);
        } catch (Exception e) {
            return "";
        }
    }

    private static String safe(String value) {
        return TextUtils.isEmpty(value) ? "" : value.trim();
    }
}

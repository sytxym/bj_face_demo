package com.aeye.face.api;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.aeye.face.api.model.ColorResponseBean;
import com.aeye.face.api.model.LightAliveResponse;
import com.aeye.face.uitls.DeviceSafeCheckUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 炫彩接口异步封装：凭证持有、设备信息采集、主线程回调、在途取消。
 * <p>同步请求与解析统一走 {@link FaceApiService}。</p>
 */
public final class ThunderAliveApi {

    private static final String TAG = "ThunderAliveApi";

    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService mExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "AEFace-Thunder");
        t.setDaemon(true);
        return t;
    });
    private final List<Future<?>> mPendingTasks = new CopyOnWriteArrayList<>();

    private final String mAppId;
    private final String mAppSecret;
    private final String mFlashUrl;

    public ThunderAliveApi(String appId, String appSecret, String flashUrl) {
        mAppId = appId;
        mAppSecret = appSecret;
        mFlashUrl = flashUrl;
    }

    /**
     * POST 获取炫彩颜色序列（回调在主线程）。
     */
    public void fetchColor(Context ctx,
                           ColorResponseBean.Response callback,
                           ColorResponseBean.WrongDeal failCb) {
        final String bodyStr;
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("app_id", mAppId);
            jsonObject.put("app_secret", mAppSecret);
            jsonObject.put("sn", getSn(ctx));
            jsonObject.put("deviceMode", getDeviceMode());
            jsonObject.put("riskType", new JSONArray(getRiskTypeList(ctx)));
            bodyStr = jsonObject.toString();
        } catch (JSONException e) {
            Log.e(TAG, "fetchColor JSON error: " + e.getMessage());
            postColorFail(failCb, -1, "请求参数构建失败: " + e.getMessage());
            return;
        }

        submit(() -> {
            try {
                ColorResponseBean colorResp = FaceApiService.fetchThunderColor(mFlashUrl, bodyStr);
                if (colorResp != null && colorResp.getResult() == 0 && colorResp.getColors() != null) {
                    ColorResponseBean.ColorsBean colorsBean = colorResp.getColors();
                    mMainHandler.post(() -> {
                        if (callback != null) {
                            callback.onResponse(colorsBean, colorsBean.getSequnce());
                        }
                    });
                } else {
                    String msg = (colorResp != null) ? colorResp.getInfo() : "服务器接口异常";
                    Log.e(TAG, "fetchColor server error: " + msg);
                    postColorFail(failCb, -1, msg);
                }
            } catch (Exception e) {
                Log.e(TAG, "fetchColor error: " + e.getMessage());
                postColorFail(failCb, -1, "网络异常: " + e.getMessage());
            }
        });
    }

    /**
     * POST 服务端炫彩活体验证（回调在主线程）。
     */
    public void checkAlive(Context ctx, String seq, String alivePic, String facePic,
                           JSONArray facePics,
                           LightAliveResponse.Response callback,
                           LightAliveResponse.WrongDeal failCb) {
        final String bodyStr;
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("app_id", mAppId);
            jsonObject.put("app_secret", mAppSecret);
            jsonObject.put("sn", getSn(ctx));
            jsonObject.put("deviceMode", getDeviceMode());
            jsonObject.put("sequence", seq);

            JSONArray aliveArray = new JSONArray();
            aliveArray.put(0, alivePic);
            jsonObject.put("alivePics", aliveArray);

            jsonObject.put("facePic", facePic != null ? facePic : "");
            jsonObject.put("facePics", facePics != null ? facePics : new JSONArray());
            jsonObject.put("source", FaceApiPaths.THUNDER_SOURCE);
            // 与参考 demo postLightColorCheck 一致，附带设备风险标签
            jsonObject.put("riskType", new JSONArray(getRiskTypeList(ctx)));
            bodyStr = jsonObject.toString();
        } catch (JSONException e) {
            Log.e(TAG, "checkAlive JSON error: " + e.getMessage());
            postLightFail(failCb, null, "请求参数构建失败: " + e.getMessage());
            return;
        }

        submit(() -> {
            try {
                LightAliveResponse lightResp = FaceApiService.checkThunderAlive(mFlashUrl, bodyStr);
                if (lightResp != null && lightResp.getResult() == 0) {
                    mMainHandler.post(() -> {
                        if (callback != null) {
                            callback.onResponse(lightResp.getResult(), lightResp);
                        }
                    });
                } else {
                    String msg = (lightResp != null)
                            ? lightResp.getInfo() + "\n得分: " + lightResp.getScore()
                            : "服务器接口异常";
                    Log.e(TAG, "checkAlive server error: " + msg);
                    postLightFail(failCb, lightResp, msg);
                }
            } catch (Exception e) {
                Log.e(TAG, "checkAlive error: " + e.getMessage());
                postLightFail(failCb, null, "网络异常: " + e.getMessage());
            }
        });
    }

    /** 取消所有在途请求（AEYE_Destory 时调用，实例可继续复用）。 */
    public void cancelAll() {
        for (Future<?> f : mPendingTasks) {
            if (f != null && !f.isDone()) {
                f.cancel(true);
            }
        }
        mPendingTasks.clear();
        Log.d(TAG, "cancelAll: all pending tasks cancelled");
    }

    private void postColorFail(ColorResponseBean.WrongDeal failCb, int code, String message) {
        mMainHandler.post(() -> {
            if (failCb != null) {
                failCb.onPostFailed(code, message);
            }
        });
    }

    private void postLightFail(LightAliveResponse.WrongDeal failCb, LightAliveResponse resp, String message) {
        mMainHandler.post(() -> {
            if (failCb != null) {
                failCb.onPostFailed(resp, message);
            }
        });
    }

    private void submit(Runnable task) {
        pruneCompleted();
        Future<?> future = mExecutor.submit(task);
        mPendingTasks.add(future);
    }

    private void pruneCompleted() {
        for (Future<?> f : mPendingTasks) {
            if (f == null || f.isDone()) {
                mPendingTasks.remove(f);
            }
        }
    }

    // ==================== 设备信息采集 ====================

    private String getSn(Context ctx) {
        try {
            TelephonyManager tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
            if (tm != null) {
                String deviceId = tm.getDeviceId();
                if (!TextUtils.isEmpty(deviceId)) {
                    return deviceId;
                }
            }
        } catch (SecurityException e) {
            Log.w(TAG, "getSn: no READ_PHONE_STATE permission");
        }

        String androidId = Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.ANDROID_ID);
        if (!TextUtils.isEmpty(androidId)) {
            return androidId;
        }

        if (Build.VERSION.SDK_INT < 26) {
            try {
                String serial = Build.SERIAL;
                if (!TextUtils.isEmpty(serial)) {
                    return serial;
                }
            } catch (Exception e) {
                Log.w(TAG, "getSn: Build.SERIAL not available");
            }
        }
        return "unknown";
    }

    private String getDeviceMode() {
        return Build.MANUFACTURER + "_" + Build.MODEL;
    }

    private List<String> getRiskTypeList(Context ctx) {
        List<String> list = new ArrayList<>();
        if (DeviceSafeCheckUtils.checkSystemUser() || DeviceSafeCheckUtils.checkDeviceDebuggable()) {
            list.add("debug版本");
        }
        if (DeviceSafeCheckUtils.isOpenDevelop(ctx)) {
            list.add("处于开发者模式");
        }
        if (DeviceSafeCheckUtils.isUsbAdbOpen(ctx)) {
            list.add("处于调试模式");
        }
        if (DeviceSafeCheckUtils.isHook(ctx)) {
            list.add("被HOOK");
        }
        if (DeviceSafeCheckUtils.hasEmulatorAdb()
                || DeviceSafeCheckUtils.hasEmulatorBuild(ctx)
                || DeviceSafeCheckUtils.hasQEmuDrivers()
                || DeviceSafeCheckUtils.hasGenyFiles()
                || DeviceSafeCheckUtils.hasPipes()
                || DeviceSafeCheckUtils.hasKnownDeviceId(ctx)
                || DeviceSafeCheckUtils.hasKnownImsi(ctx)
                || DeviceSafeCheckUtils.hasKnownPhoneNumber(ctx)) {
            list.add("模拟器");
        }
        if (DeviceSafeCheckUtils.isDeviceUnSafe()) {
            list.add("已root");
        }
        if (DeviceSafeCheckUtils.checkVPN(ctx)) {
            list.add("使用了VPN");
        }
        if (DeviceSafeCheckUtils.isWifiProxy(ctx)) {
            list.add("使用了代理");
        }
        if (DeviceSafeCheckUtils.isMockLocation(ctx)) {
            list.add("伪造地理位置");
        }
        if (DeviceSafeCheckUtils.checkMagisk() || DeviceSafeCheckUtils.checkForBinary(ctx, "magisk")) {
            list.add("使用Magisk进行系统劫持");
        }
        if (DeviceSafeCheckUtils.getroDebugProp() == 1) {
            list.add("进程被动态调试");
        }
        return list;
    }
}

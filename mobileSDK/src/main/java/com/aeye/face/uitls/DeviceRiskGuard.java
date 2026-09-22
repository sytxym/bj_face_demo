package com.aeye.face.uitls;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.text.TextUtils;

import com.lahm.library.EasyProtectorLib;
import com.lahm.library.SecurityCheckUtil;
import com.lahm.library.VirtualApkCheckUtil;
import com.sdk.core.R;

/**
 * Demo 原风险项拦截：VPN / 代理 / 多开 / 模拟器 / Hook 等。
 * 检测到时弹出与 USB / Root 同款不可取消提示，用户确认后结束核验流程。
 * USB 调试与 Root 由 {@link UsbDeveloperModeGuard}、{@link DeviceRootGuard} 单独处理。
 */
public final class DeviceRiskGuard {

    private static final String MULTI_OPEN_SOCKET_NAME = "aeye-face-verify";

    public interface ExitHandler {
        void onAcknowledged();
    }

    private DeviceRiskGuard() {
    }

    public static boolean shouldBlock(Context context) {
        return !TextUtils.isEmpty(findBlockReason(context));
    }

    /**
     * @return 命中的风险文案；未命中返回 {@code null}
     */
    public static String findBlockReason(Context context) {
        if (context == null) {
            return null;
        }
        Context app = context.getApplicationContext() != null
                ? context.getApplicationContext() : context;
        boolean appDebuggable = isAppDebuggable(app);

        if (safeVpn(app)) {
            return app.getString(R.string.aeye_risk_reason_vpn);
        }
        if (safeWifiProxy(app)) {
            return app.getString(R.string.aeye_risk_reason_proxy);
        }
        if (isMultiOpen(app)) {
            return app.getString(R.string.aeye_risk_reason_multi_open);
        }
        if (isEmulator(app)) {
            return app.getString(R.string.aeye_risk_reason_emulator);
        }
        if (easy(() -> EasyProtectorLib.checkIsXposedExist())) {
            return app.getString(R.string.aeye_risk_reason_xposed);
        }
        if (!appDebuggable && isDebugBuildOrDebugger(app)) {
            return app.getString(R.string.aeye_risk_reason_debug_build);
        }
        if (isHooked(app)) {
            return app.getString(R.string.aeye_risk_reason_hook);
        }
        if (easy(() -> EasyProtectorLib.checkHasLoadSO("frida"))) {
            return app.getString(R.string.aeye_risk_reason_inject);
        }
        if (DeviceSafeCheckUtils.isMockLocation(app)) {
            return app.getString(R.string.aeye_risk_reason_mock_location);
        }
        if (easy(() -> EasyProtectorLib.checkHasLoadSO("minicap"))) {
            return app.getString(R.string.aeye_risk_reason_minicap);
        }
        if (DeviceSafeCheckUtils.checkDeviceDebuggable()) {
            return app.getString(R.string.aeye_risk_reason_dev_board);
        }
        if (!appDebuggable && isBeingTraced()) {
            return app.getString(R.string.aeye_risk_reason_traced);
        }
        if (!appDebuggable && DeviceSafeCheckUtils.checkSystemUser()) {
            return app.getString(R.string.aeye_risk_reason_high_risk_rom);
        }
        return null;
    }

    public static void showBlockDialogAndExit(Activity activity, String reason, ExitHandler handler) {
        CharSequence message = activity != null
                ? activity.getString(R.string.aeye_risk_block_message,
                TextUtils.isEmpty(reason) ? "" : reason)
                : "";
        FaceNoticeDialog.show(activity,
                R.string.aeye_risk_block_title,
                message,
                R.string.aeye_risk_block_confirm,
                handler == null ? null : handler::onAcknowledged);
    }

    private static boolean isAppDebuggable(Context context) {
        return (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }

    private static boolean safeVpn(Context context) {
        try {
            return DeviceSafeCheckUtils.checkVPN(context);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean safeWifiProxy(Context context) {
        try {
            return DeviceSafeCheckUtils.isWifiProxy(context);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isMultiOpen(Context context) {
        try {
            VirtualApkCheckUtil util = VirtualApkCheckUtil.getSingleInstance();
            return util.checkByOriginApkPackageName(context, null)
                    || util.checkByCreateLocalServerSocket(MULTI_OPEN_SOCKET_NAME, null)
                    || util.checkByHasSameUid(null)
                    || util.checkByMultiApkPackageName(null)
                    || util.checkByPrivateFilePath(context, null);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isEmulator(Context context) {
        if (easy(() -> EasyProtectorLib.checkIsRunningInEmulator(context, null))) {
            return true;
        }
        try {
            return DeviceSafeCheckUtils.hasEmulatorAdb()
                    || DeviceSafeCheckUtils.hasAppAnalysisPackage(context)
                    || DeviceSafeCheckUtils.hasTaintClass()
                    || DeviceSafeCheckUtils.isUserAMonkey()
                    || DeviceSafeCheckUtils.hasKnownDeviceId(context)
                    || DeviceSafeCheckUtils.hasKnownPhoneNumber(context)
                    || DeviceSafeCheckUtils.hasKnownImsi(context)
                    || DeviceSafeCheckUtils.hasEmulatorBuild(context)
                    || DeviceSafeCheckUtils.hasQEmuDrivers()
                    || DeviceSafeCheckUtils.hasGenyFiles()
                    || DeviceSafeCheckUtils.hasPipes();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isDebugBuildOrDebugger(Context context) {
        try {
            SecurityCheckUtil util = SecurityCheckUtil.getSingleInstance();
            return util.checkIsDebugVersion(context) || util.checkIsDebuggerConnected();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isHooked(Context context) {
        if (easy(() -> EasyProtectorLib.checkHasLoadSO("Xpose"))
                || easy(() -> EasyProtectorLib.checkHasLoadSO("substrate"))
                || easy(() -> EasyProtectorLib.checkHasLoadSO("frida"))) {
            return true;
        }
        try {
            return DeviceSafeCheckUtils.isHook(context);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isBeingTraced() {
        if (easy(EasyProtectorLib::checkIsBeingTracedByJava)) {
            return true;
        }
        try {
            return DeviceSafeCheckUtils.getroDebugProp() == 1;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private interface Probe {
        boolean test();
    }

    private static boolean easy(Probe probe) {
        try {
            return probe.test();
        } catch (Throwable ignored) {
            return false;
        }
    }
}

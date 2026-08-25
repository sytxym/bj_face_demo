package com.aeye.face.uitls;

import android.app.Activity;
import android.content.Context;

import com.aeye.face.AEFaceSdk;
import com.sdk.core.R;

/**
 * USB 调试（ADB）拦截：开启时弹出不可取消提示，用户确认后结束核验流程。
 */
public final class UsbDeveloperModeGuard {

    public interface ExitHandler {
        void onAcknowledged();
    }

    private UsbDeveloperModeGuard() {
    }

    public static boolean isUsbDebuggingEnabled(Context context) {
        return context != null && DeviceSafeCheckUtils.isUsbAdbOpen(context);
    }

    /** 开关打开且 USB 调试已开启时拦截。 */
    public static boolean shouldBlock(Context context) {
        return AEFaceSdk.isUsbDebugBlockEnabled() && isUsbDebuggingEnabled(context);
    }

    /**
     * 弹出拦截框。点「我知道了」或系统返回与确认同等处理。
     * 点击空白处不可关闭。
     */
    public static void showBlockDialogAndExit(Activity activity, ExitHandler handler) {
        FaceNoticeDialog.show(activity,
                R.string.aeye_usb_debug_block_title,
                R.string.aeye_usb_debug_block_message,
                R.string.aeye_usb_debug_block_confirm,
                handler == null ? null : handler::onAcknowledged);
    }
}

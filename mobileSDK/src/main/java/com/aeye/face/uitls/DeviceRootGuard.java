package com.aeye.face.uitls;

import android.app.Activity;
import android.content.Context;

import com.sdk.core.R;

/**
 * Root / 越狱拦截：检测到时弹出与 USB 调试同款不可取消提示，用户确认后结束核验流程。
 */
public final class DeviceRootGuard {

    public interface ExitHandler {
        void onAcknowledged();
    }

    private DeviceRootGuard() {
    }

    public static boolean shouldBlock(Context context) {
        return DeviceSafeCheckUtils.isRootedOrJailbroken(context);
    }

    /**
     * 弹出拦截框。点「确认」或系统返回与确认同等处理。
     * 点击空白处不可关闭。
     */
    public static void showBlockDialogAndExit(Activity activity, ExitHandler handler) {
        FaceNoticeDialog.show(activity,
                R.string.aeye_root_block_title,
                R.string.aeye_root_block_message,
                R.string.aeye_root_block_confirm,
                handler == null ? null : handler::onAcknowledged);
    }
}

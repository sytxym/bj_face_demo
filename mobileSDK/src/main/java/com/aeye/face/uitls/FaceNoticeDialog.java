package com.aeye.face.uitls;

import android.app.Activity;
import android.app.Dialog;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.sdk.core.R;

/**
 * 核验流程通用提示框（与 USB 调试拦截同款样式）。
 */
public final class FaceNoticeDialog {

    public interface OnAcknowledged {
        void onAcknowledged();
    }

    private FaceNoticeDialog() {
    }

    public static void show(Activity activity, int titleRes, int messageRes, int confirmRes,
                            OnAcknowledged handler) {
        if (activity == null || activity.isFinishing()) {
            if (handler != null) {
                handler.onAcknowledged();
            }
            return;
        }
        Dialog dialog = new Dialog(activity, R.style.AeyeUsbDebugDialog);
        dialog.setContentView(R.layout.aeye_dialog_usb_debug);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.width = (int) (activity.getResources().getDisplayMetrics().widthPixels * 0.78f);
            window.setAttributes(lp);
        }
        TextView title = dialog.findViewById(R.id.tv_usb_debug_title);
        if (title != null) {
            title.setText(titleRes);
        }
        TextView message = dialog.findViewById(R.id.tv_usb_debug_message);
        if (message != null) {
            message.setText(messageRes);
        }
        Button confirm = dialog.findViewById(R.id.btn_usb_debug_confirm);
        if (confirm != null) {
            confirm.setText(confirmRes);
        }
        Runnable acknowledge = () -> {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            if (handler != null) {
                handler.onAcknowledged();
            }
        };
        if (confirm != null) {
            confirm.setOnClickListener((View v) -> acknowledge.run());
        }
        dialog.setOnKeyListener((d, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                acknowledge.run();
                return true;
            }
            return false;
        });
        dialog.show();
    }
}

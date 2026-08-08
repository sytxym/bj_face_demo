package com.aeye.face.camera;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

/**
 * 相机驱动调用专用线程。
 * <p>autoFocus/getParameters 等是到 cameraserver 的同步 binder 调用，HAL 异常时可能
 * 长时间（甚至永久）不返回，且 binder 等待处于内核不可中断状态——若发生在主线程，
 * 会造成整页卡死、进程难以杀掉的 ANR。此类调用统一投递到本线程执行。</p>
 */
final class CameraOps {

    private static volatile Handler sHandler;

    private CameraOps() {
    }

    static Handler handler() {
        if (sHandler == null) {
            synchronized (CameraOps.class) {
                if (sHandler == null) {
                    HandlerThread thread = new HandlerThread("AEFace-CameraOps");
                    thread.setDaemon(true);
                    thread.start();
                    sHandler = new Handler(thread.getLooper());
                }
            }
        }
        return sHandler;
    }

    static void post(Runnable task) {
        Handler handler = handler();
        if (Looper.myLooper() == handler.getLooper()) {
            task.run();
        } else {
            handler.post(task);
        }
    }
}

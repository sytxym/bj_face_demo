package com.aeye.face.uitls;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import java.util.Map;

/**
 * 主线程卡死看门狗（仅诊断用，不做恢复）。
 * <p>每 2 秒向主线程投递一次心跳，若 ≥5 秒未被执行，判定主线程被阻塞，
 * 立即以 E 级别输出全部线程堆栈（主线程置顶），用于定位偶发 ANR 的真实卡点；
 * 卡死持续期间每 30 秒重复输出一次。</p>
 */
public final class MainThreadWatchdog {

    private static final String TAG = "AEFaceWatchdog";

    private static final long PING_INTERVAL_MS = 2000L;
    private static final long STALL_THRESHOLD_MS = 5000L;
    private static final long REDUMP_INTERVAL_MS = 30000L;

    private static volatile boolean sRunning = false;
    private static volatile long sLastPongUptime;

    private MainThreadWatchdog() {
    }

    public static synchronized void start() {
        if (sRunning) {
            return;
        }
        sRunning = true;
        sLastPongUptime = SystemClock.uptimeMillis();

        final Handler mainHandler = new Handler(Looper.getMainLooper());
        Thread watcher = new Thread(() -> {
            long lastDumpUptime = 0L;
            while (sRunning) {
                mainHandler.post(() -> sLastPongUptime = SystemClock.uptimeMillis());
                SystemClock.sleep(PING_INTERVAL_MS);

                long now = SystemClock.uptimeMillis();
                long stall = now - sLastPongUptime;
                if (stall >= STALL_THRESHOLD_MS
                        && now - lastDumpUptime >= REDUMP_INTERVAL_MS) {
                    lastDumpUptime = now;
                    dumpAllThreads(stall);
                }
            }
        }, "AEFace-Watchdog");
        watcher.setDaemon(true);
        watcher.start();
        Log.i(TAG, "watchdog started");
    }

    public static void stop() {
        sRunning = false;
    }

    private static void dumpAllThreads(long stallMs) {
        try {
            Log.e(TAG, "==== MAIN THREAD BLOCKED " + stallMs + "ms, dumping all threads ====");
            Thread mainThread = Looper.getMainLooper().getThread();
            logThread(mainThread, mainThread.getStackTrace());

            Map<Thread, StackTraceElement[]> all = Thread.getAllStackTraces();
            for (Map.Entry<Thread, StackTraceElement[]> entry : all.entrySet()) {
                Thread t = entry.getKey();
                if (t == mainThread) {
                    continue;
                }
                logThread(t, entry.getValue());
            }
            Log.e(TAG, "==== dump end ====");
        } catch (Throwable t) {
            Log.e(TAG, "dumpAllThreads failed: " + t.getMessage());
        }
    }

    private static void logThread(Thread thread, StackTraceElement[] stack) {
        Log.e(TAG, "-- Thread \"" + thread.getName() + "\" state=" + thread.getState());
        if (stack == null || stack.length == 0) {
            Log.e(TAG, "    (no java stack)");
            return;
        }
        for (StackTraceElement element : stack) {
            Log.e(TAG, "    at " + element);
        }
    }
}

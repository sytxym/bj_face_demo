package com.aeye.face.uitls;

import android.util.Log;

public class MLog {
    private static final boolean isDebug = true;

    public static void e(String tag, String message) {
        if (isDebug) {
            Log.e(tag, message);
        }
    }

    public static void d(String tag, String message) {
        if (isDebug) {
            Log.d(tag, message);
        }
    }
}

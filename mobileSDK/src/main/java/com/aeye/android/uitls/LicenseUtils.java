//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.aeye.android.uitls;

public class LicenseUtils {
    static {
        System.loadLibrary("AESecret");
    }

    public LicenseUtils() {
    }

    private static native String getHWDeviceId();

    private static native String getHWDeviceId2();

    public static native boolean checkAuthStatus();

    public static String getDeviceId() {
        return getHWDeviceId();
    }

    public static String getDeviceId2() {
        return getHWDeviceId2();
    }
}

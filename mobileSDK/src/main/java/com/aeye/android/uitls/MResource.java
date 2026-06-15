//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.aeye.android.uitls;

import android.content.Context;

import com.aeye.face.uitls.MLog;

public class MResource {
    private static final String TAG = "MResource";
    static String mPackageName = null;

    public MResource() {
    }

    public static int getIdByName(Context context, String className, String name) {
        if (mPackageName == null) {
            mPackageName = context.getPackageName();
        }
        MLog.d(TAG, "getIdByName className=" + className + ",name=" + name + ",mPackageName=" + mPackageName);
        int id = context.getResources().getIdentifier(name, className, mPackageName);
        return id;
    }

    public static void setPackageName(String packageName) {
        mPackageName = packageName;
    }
}

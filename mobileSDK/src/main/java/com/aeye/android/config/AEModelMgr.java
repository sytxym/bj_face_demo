//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.aeye.android.config;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.aeye.android.uitls.MResource;
import com.aeye.face.uitls.MLog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class AEModelMgr {
    private static final String TAG = "AEModelMgr";
    private static final String LOCK = "init";
    private static boolean mInited = true;

    public AEModelMgr() {
    }

    public static String getModelDir(Context context) {
        return context.getFilesDir().getAbsolutePath();
    }

    public static File getModelFile(Context context, String fileName) {
        return context.getFileStreamPath(fileName);
    }

    public static String getModelFilePath(Context context, String fileName) {
        return getModelFile(context, fileName).getAbsolutePath();
    }

    private static void copyModelFromRaw(Context context, int id, File file) {
        try {
            InputStream is = context.getResources().openRawResource(id);
            FileOutputStream fos = new FileOutputStream(file);
            byte[] buf = new byte[1024];

            for (int len = is.read(buf); len != -1; len = is.read(buf)) {
                fos.write(buf, 0, len);
            }

            is.close();
            fos.close();
        } catch (Exception var7) {
            var7.printStackTrace();
        }

    }

    public static String loadModelData(Context context, String name, String type) {
        String fullName = name + "." + type;
        File dest = getModelFile(context, fullName);
        MLog.d(TAG, "loadModelData mInited=" + mInited +
                ",fullName" + fullName + ",file exit=" + dest.exists());
        if (!mInited && dest.exists()) {
            dest.delete();
        }
        if (!dest.exists()) {
            int id = MResource.getIdByName(context, "raw", name);
            MLog.d(TAG, "loadModelData id=" + id);
            copyModelFromRaw(context, id, dest);
        }

        return dest.getAbsolutePath();
    }

    public static void beforeInit(Context context) {
        SharedPreferences sp = context.getSharedPreferences("init", 0);
        mInited = sp.getBoolean("init", false);
    }

    public static void afterInit(Context context) {
        SharedPreferences sp = context.getSharedPreferences("init", 0);
        Editor edit = sp.edit();
        edit.putBoolean("init", true);
        edit.commit();
    }

    public static void flushData(Context context) {
        SharedPreferences sp = context.getSharedPreferences("init", 0);
        Editor edit = sp.edit();
        edit.putBoolean("init", false);
        edit.commit();
    }

    public static void checkVersion(Context context, String version) {
        SharedPreferences sp = context.getSharedPreferences("init", 0);
        if (!sp.getBoolean(version, false)) {
            Editor edit = sp.edit();
            edit.putBoolean("init", false);
            edit.putBoolean(version, true);
            edit.commit();
        }

    }
}

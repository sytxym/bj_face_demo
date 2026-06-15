package com.aeye.sdk;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.aeye.android.uitls.MResource;
import com.aeye.face.uitls.MLog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class AEModelMgrTest {
    private static final String TAG = "AEModelMgrTest";
    private static final String LOCK = "init";
    private static boolean mInited = true;


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
        MLog.d(TAG, "loadModelData fullName=" + fullName);
        File dest = getModelFile(context, fullName);
        if (!mInited && dest.exists()) {
            dest.delete();
        }

        if (!dest.exists()) {
            int id = MResource.getIdByName(context, "raw", name);
            Log.e(TAG, "id=" + id + ",name=" + name + ",type=" + type);
            copyModelFromRaw(context, id, dest);
        }
        int id = MResource.getIdByName(context, "raw", name);
        Log.e(TAG, "id=" + id + ",name=" + name + ",type=" + type);
        return dest.getAbsolutePath();
    }

    public static void beforeInit(Context context) {
        SharedPreferences sp = context.getSharedPreferences("init", 0);
        mInited = sp.getBoolean("init", false);
    }

    public static void afterInit(Context context) {
        SharedPreferences sp = context.getSharedPreferences("init", 0);
        SharedPreferences.Editor edit = sp.edit();
        edit.putBoolean("init", true);
        edit.commit();
    }

    public static void flushData(Context context) {
        SharedPreferences sp = context.getSharedPreferences("init", 0);
        SharedPreferences.Editor edit = sp.edit();
        edit.putBoolean("init", false);
        edit.commit();
    }

    public static void checkVersion(Context context, String version) {
        SharedPreferences sp = context.getSharedPreferences("init", 0);
        if (!sp.getBoolean(version, false)) {
            SharedPreferences.Editor edit = sp.edit();
            edit.putBoolean("init", false);
            edit.putBoolean(version, true);
            edit.commit();
        }

    }
}

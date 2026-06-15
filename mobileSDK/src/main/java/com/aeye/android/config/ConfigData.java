//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.aeye.android.config;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import com.aeye.android.uitls.MResource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ConfigData {
    public static final String SD_PATH = "com.aeye.model";
    public static final String MODEL_SD_PATH = "model";
    public static final String VIDEO_SD_PATH = "video";
    public static final String FACEDETECT_MODEL = "AEyeFaceDetectionModel.dat";
    public static final String FACEPOSE_MODEL = "facepose_model.dat";
    public static final String FACEPOSE_BLINKMOUTH_MODEL = "BlinkMouthMerge.dat";
    public static final String FACEFEATURE_MODEL = "facefeature_model.dat";
    public static final String CMP_MODELVIS = "cmp_modelvis.dat";
    public static final String CMP_VLIGHT_MODEL = "cmp_vlight_model.dat";
    public static final String FACE_REPRESENT_VLIGHT_MODEL_V2 = "face_represent_vlight_model_v2.xml";
    public static final String FACEREP_LEFTEYE_VLIGHT_MODEL = "facerep_leftEye_vlight_model.dat";
    public static final String FACEREP_MOUTH_VLIGHT_MODEL = "facerep_mouth_vlight_model.dat";
    public static final String FACEREP_NOSE_VLIGHT_MODEL = "facerep_nose_vlight_model.dat";
    public static final String FACEREP_RIGHTEYE_VLIGHT_MODEL = "facerep_rightEye_vlight_model.dat";
    public static final String HAARCASCADE_FRONTALFACE_ALT2 = "haarcascade_frontalface_alt2.xml";
    public static final String LANDMARK_MODEL = "landmark_model.dat";
    public static final String MODEL31_NIR = "model31_nir.dat";
    public static final String MODEL31L_NIR = "model31l_nir.dat";
    public static final String MODEL60_NIR = "model60_nir.dat";
    public static final String MODEL60L_NIR = "model60l_nir.dat";
    public static final String POST_MODELVIS = "post_modelvis.dat";
    public static final String POST_VLIGHT_MODEL = "post_vlight_model.dat";
    public static final String FACE_REPRESENT_NIR_MODEL_V2 = "face_represent_nir_model_v2.xml";
    public static final String CMP_NIR_MODEL = "cmp_nir_model.dat";
    public static final String POST_NIR_MODEL = "post_nir_model.dat";
    public static final String SP_CAMERA_INFO = "camera_info";
    public static final String SP_CAMERA_DIRECTION = "camera_direction";
    private static final String LOCK = "init";
    private static boolean mInited = true;

    public ConfigData() {
    }

    public static String getDestDir(Context context) {
        String dir = null;
//        if (Environment.getExternalStorageState().equals("mounted")) {
//            dir = Environment.getExternalStorageDirectory().getAbsolutePath();
//        } else {
//            dir = Environment.getDataDirectory().getAbsolutePath();
//        }
        //        String sdPath = dir + "/" + "com.aeye.model" + "/" + "model";
//        return sdPath;
        // 使用应用的外部专属目录，不需要任何权限
        File externalDir = context.getExternalFilesDir("model");
        if (externalDir != null) {
            return externalDir.getAbsolutePath();
        }

        // 如果外部不可用，回退到内部存储
        File internalDir = new File(context.getFilesDir(), "model");
        return internalDir.getAbsolutePath();
    }

    public static String getDestDir(Context context, String name) {
        return getDestDir(context) + "/" + name;
    }

    public static String getVideoDir() {
        String dir = Environment.getExternalStorageDirectory().getAbsolutePath();
        String sdPath = dir + "/" + "com.aeye.model" + "/" + "video";
        return sdPath;
    }

    public static String getDestPath(Context context,String model) {
        String sdPath = getDestDir(context);
        if (!isFileExist(sdPath)) {
            makeDestDir(context);
        }

        return sdPath + "/" + model;
    }

    private static final String TAG = "ConfigData";

    // 安全配置常量
    private static final String ALLOWED_DIR_NAME = "aeye";
    private static final String DIR_NAME_REGEX = "^[A-Za-z0-9_-]{1,32}$";
    private static final String PATH_REGEX = "^(/[A-Za-z0-9_-]+)+$";

    /**
     * 安全创建目标目录（改造 makeDestDir 方法）
     * @param context 上下文对象
     */
    public static void makeDestDir(Context context) {
        try {
            // 1. 参数校验
            if (context == null) {
                Log.e(TAG, "Context is null");
                return;
            }

            // 2. 获取安全的目录路径
            String destDirPath = getSafeDestDir(context);
            if (destDirPath == null) {
                Log.e(TAG, "Failed to get safe destination directory");
                return;
            }

            // 3. 创建目录对象
            File dest = new File(destDirPath);

            // 4. 路径规范化
            String canonicalPath = dest.getCanonicalPath();

            // 5. 获取基础路径进行安全校验
            File baseDir = getBaseDirectory(context);
            if (baseDir == null) {
                Log.e(TAG, "Failed to get base directory");
                return;
            }

            String canonicalBasePath = baseDir.getCanonicalPath();

            // 6. 路径遍历防护：确保目标目录在基础路径内
            if (!canonicalPath.startsWith(canonicalBasePath)) {
                Log.e(TAG, "Path traversal detected: " + canonicalPath);
                return;
            }

            // 7. 创建目录（如果不存在）
            if (!dest.exists()) {
                boolean created = dest.mkdirs();
                if (created) {
                    Log.d(TAG, "Directory created successfully: " + canonicalPath);
                    // 设置目录权限
                    setDirectoryPermissions(dest);
                } else {
                    Log.e(TAG, "Failed to create directory: " + canonicalPath);
                }
            } else {
                Log.d(TAG, "Directory already exists: " + canonicalPath);
            }

        } catch (IOException e) {
            Log.e(TAG, "IO Exception when creating directory", e);
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception when creating directory", e);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected exception when creating directory", e);
        }
    }

    /**
     * 安全检查文件是否存在（改造 isFileExist 方法）
     * @param filePath 文件路径
     * @return 文件是否存在
     */
    public static boolean isFileExist(String filePath) {
        // 1. 参数校验
        if (filePath == null || filePath.trim().isEmpty()) {
            Log.e(TAG, "File path is null or empty");
            return false;
        }

        try {
            // 2. 创建文件对象
            File file = new File(filePath);

            // 3. 路径规范化
            String canonicalPath = file.getCanonicalPath();

            // 4. 安全检查：防止访问系统敏感目录
            if (!isPathSafe(canonicalPath)) {
                Log.e(TAG, "Access to unsafe path blocked: " + canonicalPath);
                return false;
            }

            // 5. 检查文件是否存在
            boolean exists = file.exists();
            Log.d(TAG, "File " + canonicalPath + " exists: " + exists);
            return exists;

        } catch (IOException e) {
            Log.e(TAG, "IO Exception when checking file existence", e);
            return false;
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception when checking file existence", e);
            return false;
        }
    }

    /**
     * 安全删除文件（改造 deleteIfExit 方法）
     * @param filePath 文件路径
     * @return 是否删除成功
     */
    public static boolean deleteIfExit(String filePath) {
        // 1. 参数校验
        if (filePath == null || filePath.trim().isEmpty()) {
            Log.e(TAG, "File path is null or empty");
            return false;
        }

        try {
            // 2. 创建文件对象
            File file = new File(filePath);

            // 3. 路径规范化
            String canonicalPath = file.getCanonicalPath();

            // 4. 安全检查：防止删除系统关键文件
            if (!isPathSafeForDeletion(canonicalPath)) {
                Log.e(TAG, "Deletion of unsafe path blocked: " + canonicalPath);
                return false;
            }

            // 5. 检查文件是否存在
            if (!file.exists()) {
                Log.d(TAG, "File does not exist: " + canonicalPath);
                return true; // 文件不存在，视为删除成功
            }

            // 6. 检查是否是文件（不能删除目录）
            if (file.isDirectory()) {
                Log.e(TAG, "Cannot delete directory using this method: " + canonicalPath);
                return false;
            }

            // 7. 删除文件
            boolean deleted = file.delete();
            if (deleted) {
                Log.d(TAG, "File deleted successfully: " + canonicalPath);
            } else {
                Log.e(TAG, "Failed to delete file: " + canonicalPath);
            }
            return deleted;

        } catch (IOException e) {
            Log.e(TAG, "IO Exception when deleting file", e);
            return false;
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception when deleting file", e);
            return false;
        }
    }

    /**
     * 获取安全的目标目录路径（原 getDestDir 方法的安全版本）
     * @param context 上下文对象
     * @return 安全的目录路径
     */
    private static String getSafeDestDir(Context context) {
        try {
            // 获取基础目录
            File baseDir = getBaseDirectory(context);
            if (baseDir == null) {
                Log.e(TAG, "Base directory is null");
                return null;
            }

            // 校验基础路径
            String canonicalBasePath = baseDir.getCanonicalPath();
            String normalizedBase = canonicalBasePath.replace(File.separator, "/");
            if (!normalizedBase.matches(PATH_REGEX)) {
                Log.e(TAG, "Invalid base path format");
                return null;
            }

            // 校验目录名
            if (!ALLOWED_DIR_NAME.matches(DIR_NAME_REGEX)) {
                Log.e(TAG, "Invalid directory name: " + ALLOWED_DIR_NAME);
                return null;
            }

            // 构建目标目录
            File targetDir = new File(baseDir, ALLOWED_DIR_NAME);
            String canonicalTargetPath = targetDir.getCanonicalPath();

            // 路径遍历防护
            if (!canonicalTargetPath.startsWith(canonicalBasePath)) {
                Log.e(TAG, "Path traversal detected: " + canonicalTargetPath);
                return null;
            }

            // 校验目标路径格式
            String normalizedTarget = canonicalTargetPath.replace(File.separator, "/");
            if (!normalizedTarget.matches(PATH_REGEX)) {
                Log.e(TAG, "Invalid target path format");
                return null;
            }

            return canonicalTargetPath;

        } catch (IOException e) {
            Log.e(TAG, "IO Exception when getting safe destination directory", e);
            return null;
        }
    }

    /**
     * 获取基础目录（根据不同Android版本）
     * @param context 上下文对象
     * @return 基础目录
     */
    private static File getBaseDirectory(Context context) {
            // Android 9 及以下使用外部存储
            if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                return Environment.getExternalStorageDirectory();
            }
            // 外部存储不可用，使用内部存储
            return context.getFilesDir();
    }

    /**
     * 检查路径是否安全（通用检查）
     * @param path 路径
     * @return 是否安全
     */
    private static boolean isPathSafe(String path) {
        try {
            // 获取外部存储路径
            File externalStorage = Environment.getExternalStorageDirectory();
            if (externalStorage != null) {
                String externalPath = externalStorage.getCanonicalPath();
                // 只允许访问外部存储目录下的文件
                if (!path.startsWith(externalPath)) {
                    Log.w(TAG, "Path is outside external storage: " + path);
                    return false;
                }
            }

            // 禁止访问路径遍历
            if (path.contains("..")) {
                Log.w(TAG, "Path traversal detected: " + path);
                return false;
            }

            // 禁止访问根目录
            if (path.equals("/") || path.equals("/system") || path.startsWith("/system/")) {
                Log.w(TAG, "Access to system directory blocked: " + path);
                return false;
            }

            return true;

        } catch (IOException e) {
            Log.e(TAG, "IO Exception when checking path safety", e);
            return false;
        }
    }

    /**
     * 检查路径是否安全可删除
     * @param path 路径
     * @return 是否安全
     */
    private static boolean isPathSafeForDeletion(String path) {
        try {
            // 首先进行通用安全检查
            if (!isPathSafe(path)) {
                return false;
            }

            // 获取外部存储路径
            File externalStorage = Environment.getExternalStorageDirectory();
            if (externalStorage != null) {
                String externalPath = externalStorage.getCanonicalPath();

                // 禁止删除外部存储根目录
                if (path.equals(externalPath)) {
                    Log.w(TAG, "Cannot delete external storage root");
                    return false;
                }

                // 禁止删除应用私有目录根目录（如果使用应用私有目录）
                String appPrivatePath = getAppPrivatePath();
                if (appPrivatePath != null && path.equals(appPrivatePath)) {
                    Log.w(TAG, "Cannot delete app private directory root");
                    return false;
                }
            }

            return true;

        } catch (IOException e) {
            Log.e(TAG, "IO Exception when checking deletion safety", e);
            return false;
        }
    }

    /**
     * 获取应用私有目录路径（如果可用）
     */
    private static String getAppPrivatePath() {
        // 这个方法需要在有Context的环境下调用
        // 由于这里没有Context，返回null
        return null;
    }

    /**
     * 设置目录权限
     */
    private static void setDirectoryPermissions(File dir) {
        dir.setReadable(true, false);   // 所有用户可读
        dir.setWritable(true, true);    // 仅所有者可写
        dir.setExecutable(true, false); // 所有用户可执行
    }

    public static String copyAssetFileToDest(Context context, String name) {
        String file = getDestPath(context,name);
        if (!mInited) {
            deleteIfExit(file);
        }

        if (!isFileExist(file)) {
            copyModelFromAssets(context, name);
        }

        return file;
    }

    public static String copyRawFileToDest(Context context, String name, String type) {
        String fullName = name + "." + type;
        String file = getDestPath(context,fullName);
        if (!mInited) {
            deleteIfExit(file);
        }

        if (!isFileExist(file)) {
            int id = MResource.getIdByName(context, "raw", name);
            copyModelFromRaw(context, id, fullName);
        }

        return file;
    }

    public static void copyModelFromRaw(Context context, int id, String fileName) {
        try {
            InputStream is = context.getResources().openRawResource(id);
            FileOutputStream fos = new FileOutputStream(getDestDir(context) + "/" + fileName);
            byte[] buf = new byte[1024];

            for(int len = is.read(buf); len != -1; len = is.read(buf)) {
                fos.write(buf, 0, len);
            }

            is.close();
            fos.close();
        } catch (Exception var7) {
            var7.printStackTrace();
        }

    }

    public static void copyModelFromAssets(Context context, String fileName) {
        try {
            AssetManager am = context.getAssets();
            InputStream is = am.open(fileName);
            FileOutputStream fos = new FileOutputStream(getDestDir(context) + "/" + fileName);
            byte[] buf = new byte[1024];

            for(int len = is.read(buf); len != -1; len = is.read(buf)) {
                fos.write(buf, 0, len);
            }

            is.close();
            fos.close();
        } catch (Exception var7) {
            var7.printStackTrace();
        }

    }

    public static void unlockInit(Context context) {
        SharedPreferences sp = context.getSharedPreferences("init", 0);
        mInited = sp.getBoolean("init", false);
    }

    public static void lockInit(Context context) {
        SharedPreferences sp = context.getSharedPreferences("init", 0);
        Editor edit = sp.edit();
        edit.putBoolean("init", true);
        edit.commit();
    }

    public static void unlockModel(Context context) {
        SharedPreferences sp = context.getSharedPreferences("init", 0);
        Editor edit = sp.edit();
        edit.putBoolean("init", false);
        edit.commit();
    }

    public static void checkUpdated(Context context, String version) {
        SharedPreferences sp = context.getSharedPreferences("init", 0);
        if (!sp.getBoolean(version, false)) {
            Editor edit = sp.edit();
            edit.putBoolean("init", false);
            edit.putBoolean(version, true);
            edit.commit();
        }

    }
}

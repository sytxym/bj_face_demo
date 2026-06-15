package com.aeye.face.uitls;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class FLogUtil {
    private static final String TAG = "FLogUtil";
    private static boolean canPrinterLog = false;

    // 安全配置常量
    private static final String ALLOWED_DIR_NAME = "alarm";
    private static final String ALLOWED_SUB_DIR_NAME = "Log";
    private static final String LOG_FILE_NAME = "FLog.txt";
    private static final String DIR_NAME_REGEX = "^[A-Za-z0-9_-]{1,32}$";
    private static final String PATH_REGEX = "^(/[A-Za-z0-9_-]+)+$";
    private static final String FILENAME_REGEX = "^[A-Za-z0-9_-]{1,32}\\.(txt|log)$";

    // 日志文件最大大小（10MB）
    private static final long MAX_LOG_FILE_SIZE = 10 * 1024 * 1024;

    // 安全日志文件路径（使用getter方法，延迟初始化）
    private static String logFilePath = null;

    /**
     * 获取安全的日志文件路径
     */
    private static synchronized String getSafeLogFilePath() {
        if (logFilePath == null) {
            File safeLogFile = getSafeLogFile();
            if (safeLogFile != null) {
                logFilePath = safeLogFile.getAbsolutePath();
            }
        }
        return logFilePath;
    }

    /**
     * 保存日志到服务器（安全版本）
     * @param content 日志内容
     */
    public static void saveLogServer(String content) {
        FileWriter fw = null;
        PrintWriter pw = null;
        FileInputStream fis = null;

        try {
            // 1. 参数校验
            if (content == null) {
                Log.e(TAG, "Log content is null");
                return;
            }

            // 2. 获取安全的日志文件
            File logFile = getSafeLogFile();
            if (logFile == null) {
                Log.e(TAG, "Failed to get safe log file");
                return;
            }

            // 3. 检查文件大小，如果超过限制则重新创建
            if (logFile.exists()) {
                try (FileInputStream checkFis = new FileInputStream(logFile)) {
                    long size = checkFis.available();
                    Log.d(TAG, "Log file size: " + size + " bytes");

                    if (size > MAX_LOG_FILE_SIZE) {
                        // 文件过大，备份后重新创建
                        backupAndRecreateLogFile(logFile);
                    }
                }
            }

            // 4. 写入日志（追加模式）
            fw = new FileWriter(logFile, true);
            pw = new PrintWriter(fw);

            // 添加时间戳
            String timestamp = getCurrentTimestamp();
            String logEntry = "[" + timestamp + "] " + content;
            pw.println(logEntry);
            pw.flush();

            Log.d(TAG, "Log saved successfully: " + logFile.getAbsolutePath());

        } catch (IOException e) {
            Log.e(TAG, "IO Exception when saving log", e);
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception when saving log", e);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected exception when saving log", e);
        } finally {
            // 5. 关闭资源
            closeResources(pw, fw, fis);
        }
    }

    /**
     * 获取安全的日志文件对象
     * @return 安全的日志文件对象，失败返回null
     */
    private static File getSafeLogFile() {
        try {
            // 1. 检查外部存储状态
            if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                Log.e(TAG, "External storage is not mounted");
                return null;
            }

            // 2. 获取外部存储根目录
            File baseDir = Environment.getExternalStorageDirectory();
            if (baseDir == null) {
                Log.e(TAG, "External storage directory is null");
                return null;
            }

            // 3. 获取规范化基础路径
            String canonicalBasePath = baseDir.getCanonicalPath();

            // 4. 校验基础路径格式
            String normalizedBase = canonicalBasePath.replace(File.separator, "/");
            if (!normalizedBase.matches(PATH_REGEX)) {
                Log.e(TAG, "Invalid base path format: " + canonicalBasePath);
                return null;
            }

            // 5. 校验目录名（白名单）
            if (!ALLOWED_DIR_NAME.matches(DIR_NAME_REGEX)) {
                Log.e(TAG, "Invalid directory name: " + ALLOWED_DIR_NAME);
                return null;
            }

            // 6. 构建 alarm 目录
            File alarmDir = new File(baseDir, ALLOWED_DIR_NAME);
            String canonicalAlarmPath = alarmDir.getCanonicalPath();

            // 7. 路径遍历防护 - alarm目录
            if (!canonicalAlarmPath.startsWith(canonicalBasePath)) {
                Log.e(TAG, "Path traversal detected in alarm directory");
                return null;
            }

            // 8. 校验 alarm 目录路径格式
            String normalizedAlarm = canonicalAlarmPath.replace(File.separator, "/");
            if (!normalizedAlarm.matches(PATH_REGEX)) {
                Log.e(TAG, "Invalid alarm path format");
                return null;
            }

            // 9. 创建 alarm 目录（如果不存在）
            if (!alarmDir.exists()) {
                boolean created = alarmDir.mkdirs();
                if (!created) {
                    Log.e(TAG, "Failed to create alarm directory");
                    return null;
                }
                Log.d(TAG, "Alarm directory created: " + canonicalAlarmPath);
                setDirectoryPermissions(alarmDir);
            }

            // 10. 校验子目录名
            if (!ALLOWED_SUB_DIR_NAME.matches(DIR_NAME_REGEX)) {
                Log.e(TAG, "Invalid sub directory name: " + ALLOWED_SUB_DIR_NAME);
                return null;
            }

            // 11. 构建 Log 子目录
            File logDir = new File(alarmDir, ALLOWED_SUB_DIR_NAME);
            String canonicalLogPath = logDir.getCanonicalPath();

            // 12. 路径遍历防护 - Log目录
            if (!canonicalLogPath.startsWith(canonicalAlarmPath)) {
                Log.e(TAG, "Path traversal detected in log directory");
                return null;
            }

            // 13. 校验 Log 目录路径格式
            String normalizedLog = canonicalLogPath.replace(File.separator, "/");
            if (!normalizedLog.matches(PATH_REGEX)) {
                Log.e(TAG, "Invalid log path format");
                return null;
            }

            // 14. 创建 Log 目录（如果不存在）
            if (!logDir.exists()) {
                boolean created = logDir.mkdirs();
                if (!created) {
                    Log.e(TAG, "Failed to create log directory");
                    return null;
                }
                Log.d(TAG, "Log directory created: " + canonicalLogPath);
                setDirectoryPermissions(logDir);
            }

            // 15. 校验文件名
            if (!LOG_FILE_NAME.matches(FILENAME_REGEX)) {
                Log.e(TAG, "Invalid log file name: " + LOG_FILE_NAME);
                return null;
            }

            // 16. 构建日志文件
            File logFile = new File(logDir, LOG_FILE_NAME);
            String canonicalLogFilePath = logFile.getCanonicalPath();

            // 17. 路径遍历防护 - 日志文件
            if (!canonicalLogFilePath.startsWith(canonicalLogPath)) {
                Log.e(TAG, "Path traversal detected in log file");
                return null;
            }

            // 18. 创建日志文件（如果不存在）
            if (!logFile.exists()) {
                boolean created = logFile.createNewFile();
                if (!created) {
                    Log.e(TAG, "Failed to create log file");
                    return null;
                }
                Log.d(TAG, "Log file created: " + canonicalLogFilePath);
                setFilePermissions(logFile);
            }

            return logFile;

        } catch (IOException e) {
            Log.e(TAG, "IO Exception when getting safe log file", e);
            return null;
        }
    }

    /**
     * 备份并重新创建日志文件
     */
    private static void backupAndRecreateLogFile(File logFile) {
        try {
            // 创建备份文件名
            String timestamp = getCurrentTimestamp().replace(":", "-").replace(" ", "_");
            String backupFileName = "FLog_backup_" + timestamp + ".txt";

            // 获取日志文件所在目录
            File logDir = logFile.getParentFile();
            if (logDir == null) {
                Log.e(TAG, "Log directory is null");
                return;
            }

            // 校验备份文件名
            if (!backupFileName.matches(FILENAME_REGEX)) {
                Log.e(TAG, "Invalid backup file name");
                return;
            }

            // 创建备份文件
            File backupFile = new File(logDir, backupFileName);
            String canonicalBackupPath = backupFile.getCanonicalPath();
            String canonicalLogDirPath = logDir.getCanonicalPath();

            // 路径遍历防护
            if (!canonicalBackupPath.startsWith(canonicalLogDirPath)) {
                Log.e(TAG, "Path traversal detected in backup file");
                return;
            }

            // 重命名原文件为备份文件
            if (logFile.renameTo(backupFile)) {
                Log.d(TAG, "Log file backed up to: " + backupFileName);

                // 创建新的日志文件
                boolean created = logFile.createNewFile();
                if (created) {
                    Log.d(TAG, "New log file created");
                    setFilePermissions(logFile);
                } else {
                    Log.e(TAG, "Failed to create new log file");
                }
            } else {
                Log.e(TAG, "Failed to backup log file");
            }

        } catch (IOException e) {
            Log.e(TAG, "IO Exception when backing up log file", e);
        }
    }

    /**
     * 删除目录及其所有内容（安全版本）
     * @param dirPath 目录路径
     * @return 是否删除成功
     */
    public static boolean delDir(String dirPath) {
        // 1. 参数校验
        if (dirPath == null || dirPath.trim().isEmpty()) {
            Log.e(TAG, "Directory path is null or empty");
            return false;
        }

        try {
            // 2. 路径规范化
            File fDir = new File(dirPath);
            String canonicalDirPath = fDir.getCanonicalPath();

            // 3. 安全检查：不允许删除关键系统目录
            String externalStoragePath = Environment.getExternalStorageDirectory()
                    .getCanonicalPath();
            if (canonicalDirPath.equals(externalStoragePath)) {
                Log.e(TAG, "Cannot delete external storage root directory");
                return false;
            }

            // 4. 检查目录是否存在
            if (!fDir.exists()) {
                Log.d(TAG, "Directory does not exist: " + canonicalDirPath);
                return true;
            }

            // 5. 检查是否是目录
            if (!fDir.isDirectory()) {
                Log.e(TAG, "Path is not a directory: " + canonicalDirPath);
                return false;
            }

            // 6. 递归删除目录内容
            return deleteDirectoryRecursively(fDir);

        } catch (IOException e) {
            Log.e(TAG, "IO Exception when deleting directory", e);
            return false;
        }
    }

    /**
     * 递归删除目录（内部方法）
     */
    private static boolean deleteDirectoryRecursively(File directory) {
        boolean success = true;

        try {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        // 递归删除子目录
                        success = deleteDirectoryRecursively(file) && success;
                    } else {
                        // 删除文件
                        boolean deleted = file.delete();
                        if (!deleted) {
                            Log.e(TAG, "Failed to delete file: " + file.getAbsolutePath());
                            success = false;
                        }
                    }
                }
            }

            // 删除空目录
            boolean deleted = directory.delete();
            if (!deleted) {
                Log.e(TAG, "Failed to delete directory: " + directory.getAbsolutePath());
                success = false;
            }

        } catch (SecurityException e) {
            Log.e(TAG, "Security exception when deleting directory", e);
            return false;
        }

        return success;
    }

    /**
     * 打印日志（调试用）
     * @param message 日志消息
     */
    public static void printLog(String message) {
        if (canPrinterLog && message != null) {
            Log.d("LIULU", message);
        }
    }

    /**
     * 设置是否允许打印日志
     * @param enable 是否启用
     */
    public static void setCanPrinterLog(boolean enable) {
        canPrinterLog = enable;
    }

    /**
     * 获取当前时间戳（用于日志记录）
     */
    private static String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
        return sdf.format(new Date());
    }

    /**
     * 设置目录权限
     */
    private static void setDirectoryPermissions(File dir) {
        dir.setReadable(true, false);   // 所有用户可读
        dir.setWritable(true, true);    // 仅所有者可写
        dir.setExecutable(true, false); // 所有用户可执行
    }

    /**
     * 设置文件权限
     */
    private static void setFilePermissions(File file) {
        file.setReadable(true, false);  // 所有用户可读
        file.setWritable(true, true);   // 仅所有者可写
        file.setExecutable(false);      // 文件不可执行
    }

    /**
     * 关闭资源
     */
    private static void closeResources(PrintWriter pw, FileWriter fw, FileInputStream fis) {
        try {
            if (pw != null) {
                pw.close();
            }
            if (fw != null) {
                fw.close();
            }
            if (fis != null) {
                fis.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error closing resources", e);
        }
    }

    /**
     * 获取日志文件路径（安全版本）
     * @return 日志文件路径
     */
    public static String getLogFilePath() {
        return getSafeLogFilePath();
    }

    /**
     * 清除日志文件内容（安全版本）
     */
    public static void clearLogFile() {
        File logFile = getSafeLogFile();
        if (logFile != null && logFile.exists()) {
            try {
                if (logFile.delete()) {
                    boolean created = logFile.createNewFile();
                    if (created) {
                        setFilePermissions(logFile);
                        Log.d(TAG, "Log file cleared successfully");
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Failed to clear log file", e);
            }
        }
    }
}
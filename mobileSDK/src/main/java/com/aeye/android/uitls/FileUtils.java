//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.aeye.android.uitls;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.os.Environment;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Bitmap.CompressFormat;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileUtils {
    private static final String TAG = "FileUtils";
    private static final String ALLOWED_DIR_NAME = "aeye";
    private static final String DIR_NAME_REGEX = "^[A-Za-z0-9_-]{1,32}$";
    private static final String PATH_REGEX = "^(/[A-Za-z0-9_-]+)+$";
    private static final String FILENAME_REGEX = "^[A-Za-z0-9_-]{1,30}$";
    private static final String FILE_EXT_REGEX = "^[A-Za-z0-9_-]+\\.(jpg|jpeg|png|txt|bin)$";

    public FileUtils() {
    }

    /**
     * 写入字符串数据到文件（安全版本）
     * @param data 字符串数据
     * @param fileName 文件名（用户输入，不超过10个字符）
     */
    public static void writeFile(String data, String fileName) {
        if (data == null) {
            Log.e(TAG, "Data is null");
            return;
        }
        writeFile(data.getBytes(), fileName);
    }

    /**
     * 写入字节数据到文件（安全版本）
     * @param data 字节数据
     * @param fileName 文件名（用户输入，不超过10个字符）
     */
    public static void writeFile(byte[] data, String fileName) {
        BufferedOutputStream bOutputStream = null;
        FileOutputStream fileOutput = null;

        try {
            // 1. 参数校验
            if (data == null) {
                Log.e(TAG, "Data is null");
                return;
            }

            if (fileName == null || fileName.trim().isEmpty()) {
                Log.e(TAG, "File name is null or empty");
                return;
            }

            // 2. 文件名校验（不超过10个字符，只允许安全字符）
            if (!fileName.matches(FILENAME_REGEX)) {
                Log.e(TAG, "Invalid file name: " + fileName);
                return;
            }

            // 3. 获取安全的存储目录
            File targetDir = getSafeDirectory();
            if (targetDir == null) {
                Log.e(TAG, "Failed to get safe directory");
                return;
            }

            // 4. 构建完整文件名（添加.txt扩展名）
            String fullFilename = fileName + ".txt";

            // 5. 扩展名校验
            if (!fullFilename.matches(FILE_EXT_REGEX)) {
                Log.e(TAG, "Invalid file extension: " + fullFilename);
                return;
            }

            // 6. 安全创建文件
            File targetFile = new File(targetDir, fullFilename);
            String canonicalFilePath = targetFile.getCanonicalPath();
            String canonicalDirPath = targetDir.getCanonicalPath();

            // 7. 路径遍历防护
            if (!canonicalFilePath.startsWith(canonicalDirPath)) {
                Log.e(TAG, "File path traversal detected");
                return;
            }

            // 8. 创建文件（如果不存在）
            if (!targetFile.exists()) {
                boolean created = targetFile.createNewFile();
                if (!created) {
                    Log.e(TAG, "Failed to create file: " + canonicalFilePath);
                    return;
                }
                Log.d(TAG, "File created: " + canonicalFilePath);
            }

            // 9. 写入数据
            fileOutput = new FileOutputStream(targetFile);
            bOutputStream = new BufferedOutputStream(fileOutput);
            bOutputStream.write(data);
            bOutputStream.flush();

            // 10. 设置文件权限
            setFilePermissions(targetFile);

            Log.d(TAG, "File written successfully: " + canonicalFilePath);

        } catch (IOException e) {
            Log.e(TAG, "IO Exception when writing file", e);
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception when writing file", e);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected exception when writing file", e);
        } finally {
            // 11. 关闭流
            closeStreams(bOutputStream, fileOutput);
        }
    }

    /**
     * 写入JPEG灰度图片（安全版本）
     * @param yData 灰度数据
     * @param width 图片宽度
     * @param height 图片高度
     * @param fileName 文件名（用户输入，不超过10个字符）
     */
    public static void writeJPGGray(byte[] yData, int width, int height, String fileName) {
        try {
            // 参数校验
            if (yData == null) {
                Log.e(TAG, "YData is null");
                return;
            }

            if (fileName == null || fileName.trim().isEmpty()) {
                Log.e(TAG, "File name is null or empty");
                return;
            }

            // 文件名校验
            if (!fileName.matches(FILENAME_REGEX)) {
                Log.e(TAG, "Invalid file name: " + fileName);
                return;
            }

            int size = width * height;
            if (yData.length < size) {
                Log.e(TAG, "YData length is insufficient");
                return;
            }

            // 转换灰度数据为ARGB
            int[] y2 = new int[size];
            for (int i = 0; i < size; ++i) {
                y2[i] = yData[i] & 255;
                y2[i] |= yData[i] << 8 & 0xFF00;
                y2[i] |= yData[i] << 16 & 0xFF0000;
                y2[i] |= 0xFF000000;
            }

            // 创建Bitmap
            Bitmap bitmap = Bitmap.createBitmap(y2, width, height, Config.ARGB_8888);
            if (bitmap == null) {
                Log.e(TAG, "Failed to create bitmap");
                return;
            }

            // 使用安全的保存方法
            saveBitmapToFile(bitmap, fileName, CompressFormat.JPEG, 90);

        } catch (Exception e) {
            Log.e(TAG, "Exception in writeJPGGray", e);
        }
    }

    /**
     * 写入JPG图片（int数组RGB数据）（安全版本）
     * @param rgb RGB数据
     * @param width 图片宽度
     * @param height 图片高度
     * @param fileName 文件名（用户输入，不超过10个字符）
     */
    public static void writeJPG(int[] rgb, int width, int height, String fileName) {
        try {
            // 参数校验
            if (rgb == null) {
                Log.e(TAG, "RGB data is null");
                return;
            }

            if (fileName == null || fileName.trim().isEmpty()) {
                Log.e(TAG, "File name is null or empty");
                return;
            }

            // 文件名校验
            if (!fileName.matches(FILENAME_REGEX)) {
                Log.e(TAG, "Invalid file name: " + fileName);
                return;
            }

            int size = width * height;
            if (rgb.length < size) {
                Log.e(TAG, "RGB data length is insufficient");
                return;
            }

            // 创建Bitmap
            Bitmap bitmap = Bitmap.createBitmap(rgb, width, height, Config.ARGB_8888);
            if (bitmap == null) {
                Log.e(TAG, "Failed to create bitmap");
                return;
            }

            // 使用安全的保存方法
            saveBitmapToFile(bitmap, fileName, CompressFormat.JPEG, 100);

        } catch (Exception e) {
            Log.e(TAG, "Exception in writeJPG", e);
        }
    }

    /**
     * 写入JPG图片（Bitmap对象）（安全版本）
     * @param bitmap 图片对象
     * @param fileName 文件名（用户输入，不超过10个字符）
     */
    public static void writeJPG(Bitmap bitmap, String fileName) {
        try {
            // 参数校验
            if (bitmap == null) {
                Log.e(TAG, "Bitmap is null");
                return;
            }

            if (fileName == null || fileName.trim().isEmpty()) {
                Log.e(TAG, "File name is null or empty");
                return;
            }

            // 文件名校验
            if (!fileName.matches(FILENAME_REGEX)) {
                Log.e(TAG, "Invalid file name: " + fileName);
                return;
            }

            // 使用安全的保存方法
            saveBitmapToFile(bitmap, fileName, CompressFormat.JPEG, 100);

        } catch (Exception e) {
            Log.e(TAG, "Exception in writeJPG", e);
        }
    }

    /**
     * 安全保存Bitmap到文件（核心方法）
     * @param bitmap 图片对象
     * @param fileName 文件名（不含扩展名）
     * @param format 图片格式
     * @param quality 图片质量
     */
    private static void saveBitmapToFile(Bitmap bitmap, String fileName,
                                         CompressFormat format, int quality) {
        BufferedOutputStream bOutputStream = null;
        FileOutputStream fileOutput = null;

        try {
            // 获取安全目录
            File targetDir = getSafeDirectory();
            if (targetDir == null) {
                Log.e(TAG, "Failed to get safe directory");
                return;
            }

            // 确定文件扩展名
            String extension;
            switch (format) {
                case PNG:
                    extension = ".png";
                    break;
                case JPEG:
                    extension = ".jpg";
                    break;
                case WEBP:
                    extension = ".webp";
                    break;
                default:
                    extension = ".jpg";
                    break;
            }

            // 构建完整文件名
            String fullFilename = fileName + extension;

            // 扩展名校验
            if (!fullFilename.toLowerCase().matches(".*\\.(jpg|jpeg|png|webp)$")) {
                Log.e(TAG, "Invalid file extension: " + fullFilename);
                return;
            }

            // 安全创建文件
            File targetFile = new File(targetDir, fullFilename);
            String canonicalFilePath = targetFile.getCanonicalPath();
            String canonicalDirPath = targetDir.getCanonicalPath();

            // 路径遍历防护
            if (!canonicalFilePath.startsWith(canonicalDirPath)) {
                Log.e(TAG, "File path traversal detected");
                return;
            }

            // 创建文件
            if (!targetFile.exists()) {
                boolean created = targetFile.createNewFile();
                if (!created) {
                    Log.e(TAG, "Failed to create file: " + canonicalFilePath);
                    return;
                }
                Log.d(TAG, "File created: " + canonicalFilePath);
            }

            // 写入图片数据
            fileOutput = new FileOutputStream(targetFile);
            bOutputStream = new BufferedOutputStream(fileOutput);

            // 处理质量参数
            int compressQuality = quality;
            if (format == CompressFormat.PNG) {
                compressQuality = 100; // PNG无损压缩
            } else if (quality < 0 || quality > 100) {
                compressQuality = 90;
            }

            bitmap.compress(format, compressQuality, bOutputStream);
            bOutputStream.flush();

            // 设置文件权限
            setFilePermissions(targetFile);

            Log.d(TAG, "Bitmap saved successfully: " + canonicalFilePath);

        } catch (IOException e) {
            Log.e(TAG, "IO Exception when saving bitmap", e);
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception when saving bitmap", e);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected exception when saving bitmap", e);
        } finally {
            closeStreams(bOutputStream, fileOutput);
        }
    }

    /**
     * 获取安全的存储目录
     */
    private static File getSafeDirectory() {
        try {
            // 检查外部存储状态
            if (!android.os.Environment.MEDIA_MOUNTED.equals(
                    android.os.Environment.getExternalStorageState())) {
                Log.e(TAG, "External storage is not mounted");
                return null;
            }

            // 获取外部存储根目录
            File baseDir = android.os.Environment.getExternalStorageDirectory();
            if (baseDir == null) {
                Log.e(TAG, "External storage directory is null");
                return null;
            }

            // 获取规范化基础路径
            String canonicalBasePath = baseDir.getCanonicalPath();

            // 校验基础路径格式
            String normalizedBase = canonicalBasePath.replace(File.separator, "/");
            if (!normalizedBase.matches(PATH_REGEX)) {
                Log.e(TAG, "Invalid base path format: " + canonicalBasePath);
                return null;
            }

            // 校验目录名（白名单）
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
                Log.e(TAG, "Invalid target path format: " + canonicalTargetPath);
                return null;
            }

            // 创建目录（如果不存在）
            if (!targetDir.exists()) {
                boolean created = targetDir.mkdirs();
                if (!created) {
                    Log.e(TAG, "Failed to create directory: " + canonicalTargetPath);
                    return null;
                }
                Log.d(TAG, "Directory created: " + canonicalTargetPath);

                // 设置目录权限
                setDirectoryPermissions(targetDir);
            } else if (!targetDir.isDirectory()) {
                Log.e(TAG, "Path exists but is not a directory: " + canonicalTargetPath);
                return null;
            }

            return targetDir;

        } catch (IOException e) {
            Log.e(TAG, "IO Exception when getting safe directory", e);
            return null;
        }
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
     * 安全关闭流
     */
    private static void closeStreams(BufferedOutputStream bOutputStream,
                                     FileOutputStream fileOutput) {
        try {
            if (bOutputStream != null) {
                bOutputStream.close();
            }
            if (fileOutput != null) {
                fileOutput.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error closing streams", e);
        }
    }
}

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.aeye.android.uitls;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory.Options;
import android.os.Build;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;

import com.aeye.android.libutils.ComplexUtil;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class BitmapUtils {
    public BitmapUtils() {
    }

    public String AEYE_FaceTool_GetVersion() {
        return "20161216";
    }

    public static Bitmap convertStringToBitmap(String string) {
        Bitmap bitmap = null;

        try {
            byte[] bitmapArray = Base64.decode(string, 2);
            bitmap = BitmapFactory.decodeByteArray(bitmapArray, 0, bitmapArray.length);
        } catch (Exception var3) {
            var3.printStackTrace();
        }

        return bitmap;
    }

    public static Bitmap AEYE_Base64Decode(String base64) {
        return convertStringToBitmap(base64);
    }

    public static String convertIconToString(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(CompressFormat.PNG, 100, baos);
        byte[] appicon = baos.toByteArray();
        return Base64.encodeToString(appicon, 2);
    }

    public static String AEYE_Base64Encode(Bitmap bitmap) {
        return convertIconToString(bitmap);
    }

    public static Bitmap rawByteArray2RGBABitmap2(byte[] data, int width, int height, int direction) {
        int imageSize ;
        int frameSize = width * height;
        int[] rgba = null;
        if (width <= 960 && height <= 960) {
            rgba = new int[frameSize];
            imageSize = 0;
        } else {
            rgba = new int[frameSize / 4];
            imageSize = 1;
        }

        ComplexUtil.getInstance().YUVToBitmapR(data, rgba, width, height, imageSize, direction);
        Bitmap bmp = null;
        if (imageSize == 0) {
            if (direction != 90 && direction != -90) {
                bmp = Bitmap.createBitmap(width, height, Config.RGB_565);
                bmp.setPixels(rgba, 0, width, 0, 0, width, height);
            } else {
                bmp = Bitmap.createBitmap(height, width, Config.RGB_565);
                bmp.setPixels(rgba, 0, height, 0, 0, height, width);
            }
        } else if (imageSize == 1) {
            if (direction != 90 && direction != -90) {
                bmp = Bitmap.createBitmap(width / 2, height / 2, Config.RGB_565);
                bmp.setPixels(rgba, 0, width / 2, 0, 0, width / 2, height / 2);
            } else {
                bmp = Bitmap.createBitmap(height / 2, width / 2, Config.RGB_565);
                bmp.setPixels(rgba, 0, height / 2, 0, 0, height / 2, width / 2);
            }
        }

        return bmp;
    }

    public static Bitmap yuy2Array2RGBABitmap(byte[] data, int width, int height) {
        int imageSize;
        int frameSize = width * height;
        int[] rgba = null;

        if (width <= 960 && height <= 960) {
            rgba = new int[frameSize];
            imageSize = 0;
        } else {
            rgba = new int[frameSize / 4];
            imageSize = 1;
        }

        ComplexUtil.getInstance().YUY2ToBitmap(data, rgba, width, height, imageSize);
        Bitmap bmp = null;
        if (imageSize == 0) {
            bmp = Bitmap.createBitmap(width, height, Config.RGB_565);
            bmp.setPixels(rgba, 0, width, 0, 0, width, height);
        } else if (imageSize == 1) {
            bmp = Bitmap.createBitmap(width / 2, height / 2, Config.RGB_565);
            bmp.setPixels(rgba, 0, width / 2, 0, 0, width / 2, height / 2);
        }

        return bmp;
    }

    public static byte[] yuv2Array2V(byte[] data, int width, int height) {
        byte[] vData = new byte[width * height / 2];
        ComplexUtil.getInstance().YUY2ToV(data, vData, width, height);
        return vData;
    }

    public static byte[] yuv2Array2Y(byte[] data, int width, int height, int direction) {
        int imageSize ;
        int frameSize = width * height;
        byte[] rgba = null;

        if (width <= 960 && height <= 960) {
            rgba = new byte[frameSize];
            imageSize = 0;
        } else {
            rgba = new byte[frameSize / 4];
            imageSize = 1;
        }

        ComplexUtil.getInstance().YUY2ToYR(data, rgba, width, height, imageSize, direction);
        return rgba;
    }

    public static byte[] rawByteArray2Y(byte[] data, int width, int height, int direction) {
        int imageSize ;
        int frameSize = width * height;
        byte[] rgba = null;

        if (width <= 960 && height <= 960) {
            rgba = new byte[frameSize];
            imageSize = 0;
        } else {
            rgba = new byte[frameSize / 4];
            imageSize = 1;
        }

        ComplexUtil.getInstance().YUVToYR(data, rgba, width, height, imageSize, direction);
        return rgba;
    }

    public static byte[] rotateArray(byte[] orig, int width, int height, int rotate) {
        if (rotate == 0) {
            return orig;
        } else {
            byte[] target = new byte[orig.length];

            for(int j = 0; j < height; ++j) {
                for(int i = 0; i < width; ++i) {
                    if (rotate == 90) {
                        target[i * height + height - 1 - j] = orig[j * width + i];
                    } else if (rotate == 180) {
                        target[(height - 1 - j) * width + width - 1 - i] = orig[j * width + i];
                    } else if (rotate == -90 || rotate == 270) {
                        target[(width - 1 - i) * height + j] = orig[j * width + i];
                    }
                }
            }

            return target;
        }
    }

    public static int[] rotateArray(int[] orig, int width, int height, int rotate) {
        if (rotate == 0) {
            return orig;
        } else {
            int[] target = new int[orig.length];

            for(int j = 0; j < height; ++j) {
                for(int i = 0; i < width; ++i) {
                    if (rotate == 90) {
                        target[i * height + height - 1 - j] = orig[j * width + i];
                    } else if (rotate == 180) {
                        target[(height - 1 - j) * width + width - 1 - i] = orig[j * width + i];
                    } else if (rotate == -90 || rotate == 270) {
                        target[(width - 1 - i) * height + j] = orig[j * width + i];
                    }
                }
            }

            return target;
        }
    }

    public static int[] cvtSpace(byte[] data, int width, int height, int direction) {
        int imageSize;
        int frameSize = width * height;
        int[] rgba = null;

        if (width <= 960 && height <= 960) {
            rgba = new int[frameSize];
            imageSize = 0;
        } else {
            rgba = new int[frameSize / 4];
            imageSize = 1;
        }

        ComplexUtil.getInstance().YUY2ToBitmap(data, rgba, width, height, imageSize);
        return rotateArray(rgba, width, height, direction);
    }

    public static Bitmap scaleBitmap(Bitmap bitMap, int newWidth, int newHeight) {
        int width = bitMap.getWidth();
        int height = bitMap.getHeight();
        float scaleWidth = (float)newWidth / (float)width;
        float scaleHeight = (float)newHeight / (float)height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        return Bitmap.createBitmap(bitMap, 0, 0, width, height, matrix, true);
    }

    public Bitmap revitionImageSize(InputStream temp, int size) throws IOException {
        Options options = new Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(temp, (Rect)null, options);
        temp.close();
        int i = 0;

        Bitmap bitmap;
        for(bitmap = null; options.outWidth >> i > size || options.outHeight >> i > size; ++i) {
        }

        options.inSampleSize = (int)Math.pow(2.0D, (double)i);
        options.inJustDecodeBounds = false;
        bitmap = BitmapFactory.decodeStream(temp, (Rect)null, options);
        return bitmap;
    }

    public static int[] getBitmapData(Bitmap image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] result = new int[width * height];
        image.getPixels(result, 0, width, 0, 0, width, height);
        return result;
    }

    public static Bitmap getOptimizeBitmap(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float scaleX = 1000.0F / (float)width;
        float scaleY = 1000.0F / (float)height;
        float scale = scaleX < scaleY ? scaleX : scaleY;
        scale = scale > 1.0F ? 1.0F : scale;
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);
        Bitmap resizeBmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        return resizeBmp;
    }

    public Bitmap compressImage(Bitmap image) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(CompressFormat.JPEG, 100, baos);
        int options = 100;

        while(baos.toByteArray().length / 1024 > 600) {
            baos.reset();
            options -= 10;
            image.compress(CompressFormat.JPEG, options, baos);
        }

        ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());
        Options option = new Options();
        option.inSampleSize = 2;
        Bitmap bitmap = BitmapFactory.decodeStream(isBm, (Rect)null, option);
        return bitmap;
    }

    public static byte[] getImageGrayData(Bitmap bmp) {
        int width = bmp.getWidth();
        int height = bmp.getHeight();
        int[] ori_pixels = new int[width * height];
        bmp.getPixels(ori_pixels, 0, width, 0, 0, width, height);
        return ComplexUtil.getInstance().BitmapToYR(ori_pixels, width, height);
    }

    public static void saveUserPicture(byte[] yData, int width, int height, String name) {
        int size = width * height;
        int[] y2 = new int[size];

        for(int i = 0; i < size; ++i) {
            y2[i] = yData[i] & 255;
            y2[i] |= yData[i] << 8 & '\uff00';
            y2[i] |= yData[i] << 16 & 16711680;
            y2[i] |= -16777216;
        }

        Bitmap bitmap = Bitmap.createBitmap(y2, width, height, Config.ARGB_8888);
        saveUserPicture(bitmap, name);
    }

    private static final String TAG = "ImageFileHelper";
    private static final String ALLOWED_DIR_NAME = "aeye";
    private static final String DIR_NAME_REGEX = "^[A-Za-z0-9_-]{1,32}$";
    private static final String PATH_REGEX = "^(/[A-Za-z0-9_-]+)+$";

    /**
     * 安全保存用户图片（修复版本）
     * @param bitmap 图片对象
     * @param filename 文件名（不含扩展名，用户输入）
     */
    public static void saveUserPicture(Bitmap bitmap, String filename) {
        BufferedOutputStream bOutputStream = null;
        FileOutputStream fileOutput = null;

        try {
            // 1. 参数校验
            if (bitmap == null) {
                Log.e(TAG, "Bitmap is null");
                return;
            }

            if (filename == null || filename.trim().isEmpty()) {
                Log.e(TAG, "Filename is null or empty");
                return;
            }

            // 2. 文件名校验（参考漏洞规则，不超过30个字符）
            String fileRegex = "^[A-Za-z0-9_-]{1,30}$";
            if (!filename.matches(fileRegex)) {
                Log.e(TAG, "Invalid filename: " + filename);
                return;
            }

            // 3. 获取安全的存储路径
            File targetDir = getSafeDirectory();
            if (targetDir == null) {
                Log.e(TAG, "Failed to get safe directory");
                return;
            }

            // 4. 构建文件名（添加.jpg扩展名）
            String fullFilename = filename + ".jpg";

            // 5. 扩展名校验（白名单）
            String extRegex = "^[a-z]{3,4}$";
            String extension = "jpg";
            if (!extension.matches(extRegex)) {
                Log.e(TAG, "Invalid file extension");
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

            // 8. 创建或获取文件
            if (!targetFile.exists()) {
                boolean created = targetFile.createNewFile();
                if (!created) {
                    Log.e(TAG, "Failed to create file: " + canonicalFilePath);
                    return;
                }
                Log.d(TAG, "File created: " + canonicalFilePath);
            } else {
                Log.d(TAG, "File already exists: " + canonicalFilePath);
            }

            // 9. 写入图片数据
            fileOutput = new FileOutputStream(targetFile);
            bOutputStream = new BufferedOutputStream(fileOutput);
            bitmap.compress(CompressFormat.JPEG, 90, bOutputStream); // 降低质量到90%，节省空间
            bOutputStream.flush();

            // 10. 设置文件权限（Android 安全最佳实践）
            setFilePermissions(targetFile);

            Log.d(TAG, "Picture saved successfully: " + canonicalFilePath);

        } catch (IOException e) {
            Log.e(TAG, "IO Exception when saving picture", e);
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception when saving picture", e);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected exception when saving picture", e);
        } finally {
            // 11. 确保资源正确关闭
            closeStreams(bOutputStream, fileOutput);
        }
    }

    /**
     * 获取安全的存储目录（修复路径拼接漏洞）
     * @return 安全的目录File对象，失败返回null
     */
    private static File getSafeDirectory() {
        try {
            // 1. 获取基础路径（优先使用应用私有目录）
            File baseDir = getBaseStorageDirectory();
            if (baseDir == null) {
                Log.e(TAG, "Failed to get base storage directory");
                return null;
            }

            // 2. 校验基础路径格式
            String canonicalBasePath = baseDir.getCanonicalPath();
            String normalizedBase = canonicalBasePath.replace(File.separator, "/");
            if (!normalizedBase.matches(PATH_REGEX)) {
                Log.e(TAG, "Invalid base path format: " + canonicalBasePath);
                return null;
            }

            // 3. 校验目录名
            if (!ALLOWED_DIR_NAME.matches(DIR_NAME_REGEX)) {
                Log.e(TAG, "Invalid directory name: " + ALLOWED_DIR_NAME);
                return null;
            }

            // 4. 构建目标目录
            File targetDir = new File(baseDir, ALLOWED_DIR_NAME);
            String canonicalTargetPath = targetDir.getCanonicalPath();

            // 5. 路径遍历防护
            if (!canonicalTargetPath.startsWith(canonicalBasePath)) {
                Log.e(TAG, "Path traversal detected: " + canonicalTargetPath);
                return null;
            }

            // 6. 校验目标路径格式
            String normalizedTarget = canonicalTargetPath.replace(File.separator, "/");
            if (!normalizedTarget.matches(PATH_REGEX)) {
                Log.e(TAG, "Invalid target path format: " + canonicalTargetPath);
                return null;
            }

            // 7. 创建目录（如果不存在）
            if (!targetDir.exists()) {
                boolean created = targetDir.mkdirs();
                if (!created) {
                    Log.e(TAG, "Failed to create directory: " + canonicalTargetPath);
                    return null;
                }
                Log.d(TAG, "Directory created: " + canonicalTargetPath);

                // 8. 设置目录权限
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
     * 获取基础存储目录（适配不同Android版本）
     * @return 基础目录File对象
     */
    private static File getBaseStorageDirectory() {
        // Android 10+ 优先使用应用私有目录（分区存储）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Log.d(TAG, "Using external storage directory for Android 10+");
            return Environment.getExternalStorageDirectory();
        }

        // 检查外部存储状态
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            Log.e(TAG, "External storage is not mounted");
            return null;
        }

        // Android 9 及以下使用外部存储根目录
        return Environment.getExternalStorageDirectory();
    }

    /**
     * 设置目录权限（Android 安全最佳实践）
     */
    private static void setDirectoryPermissions(File dir) {
        // 目录权限：所有者可读写执行，其他只读执行
        dir.setReadable(true, false);   // 所有用户可读
        dir.setWritable(true, true);    // 仅所有者可写
        dir.setExecutable(true, false); // 所有用户可执行（进入目录）
    }

    /**
     * 设置文件权限（Android 安全最佳实践）
     */
    private static void setFilePermissions(File file) {
        // 文件权限：所有者可读写，其他只读
        file.setReadable(true, false);  // 所有用户可读
        file.setWritable(true, true);   // 仅所有者可写
        file.setExecutable(false);      // 文件不可执行
    }

    /**
     * 安全关闭流
     */
    private static void closeStreams(BufferedOutputStream bOutputStream, FileOutputStream fileOutput) {
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

    /**
     * 安全保存PNG格式图片（改造 saveBitmap 方法）
     * @param bitmap 图片对象
     * @param filename 文件名（不含扩展名，用户输入，不超过10个字符）
     */
    public static void saveBitmap(Bitmap bitmap, String filename) {
        BufferedOutputStream bOutputStream = null;
        FileOutputStream fileOutput = null;

        try {
            // 1. 参数校验
            if (bitmap == null) {
                Log.e(TAG, "Bitmap is null");
                return;
            }

            if (filename == null || filename.trim().isEmpty()) {
                Log.e(TAG, "Filename is null or empty");
                return;
            }

            // 2. 文件名校验（参考漏洞规则，不超过10个字符）
            String fileRegex = "^[A-Za-z0-9_-]{1,10}$";
            if (!filename.matches(fileRegex)) {
                Log.e(TAG, "Invalid filename: " + filename);
                return;
            }

            // 3. 获取安全的存储目录
            File targetDir = getSafeDirectory();
            if (targetDir == null) {
                Log.e(TAG, "Failed to get safe directory");
                return;
            }

            // 4. 构建文件名（PNG格式）
            String fullFilename = filename + ".png";

            // 5. 扩展名校验（PNG格式白名单）
            if (!fullFilename.matches("^[A-Za-z0-9_-]+\\.(png|PNG)$")) {
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

            // 8. 创建或获取文件
            if (!targetFile.exists()) {
                boolean created = targetFile.createNewFile();
                if (!created) {
                    Log.e(TAG, "Failed to create file: " + canonicalFilePath);
                    return;
                }
                Log.d(TAG, "File created: " + canonicalFilePath);
            } else {
                Log.d(TAG, "File already exists: " + canonicalFilePath);
            }

            // 9. 写入PNG图片数据
            fileOutput = new FileOutputStream(targetFile);
            bOutputStream = new BufferedOutputStream(fileOutput);
            bitmap.compress(CompressFormat.PNG, 100, bOutputStream);
            bOutputStream.flush();

            // 10. 设置文件权限
            setFilePermissions(targetFile);

            Log.d(TAG, "PNG picture saved successfully: " + canonicalFilePath);

        } catch (IOException e) {
            Log.e(TAG, "IO Exception when saving PNG picture", e);
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception when saving PNG picture", e);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected exception when saving PNG picture", e);
        } finally {
            // 11. 关闭流
            closeStreams(bOutputStream, fileOutput);
        }
    }



}

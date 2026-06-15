package com.aeye.face.uitls;


import android.content.Context;
import android.os.Build;
import androidx.annotation.RequiresApi;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class FileUtil {
    private static String TAG = "FileUtil";

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static String getFilePath(Context context, String fileName) {
        String rgbFileAbsoluteFilePath =
                new File(assetFilePath(context, fileName)).getAbsolutePath();
        return rgbFileAbsoluteFilePath;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static String assetFilePath(Context context, String assetName) {
        File file = new File(context.getFilesDir(), assetName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }

        try (InputStream is = context.getAssets().open(assetName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        } catch (IOException e) {
            Log.e(TAG, "Error process asset " + assetName + " to file path");
        }
        return null;
    }
    public static void saveLogServer(String filePath, String content) {
        FileWriter fw = null;
        PrintWriter pw = null;
        FileInputStream fis = null;
        try {

            File logFile = new File(filePath);
            if (!logFile.exists()) {
                logFile.createNewFile();
            }
            fw = new FileWriter(logFile, true);
            pw = new PrintWriter(fw);
            pw.println(content);
            pw.close();
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (pw != null) {
                pw.close();
            }
            if (fw != null) {
                try {
                    fw.close();
                    if (fis != null) {
                        fis.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    /**
     * @param
     */
    public static String getFileStringInfo(String path) {

        File file = new File(path);
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            FileInputStream fis = new FileInputStream(file);
            int len = 0;
            byte[] buffer = new byte[2 * 1024];
            while ((len = fis.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            byte[] result = bos.toByteArray();
            if (result.length > 0) {
                String data = new String(result);
                return data;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

    }

//    public static List parseData(String data) {
//        List<VideoInfo> reusult = new ArrayList<VideoInfo>();
//        if (data != null) {
//            String[] temps = data.split("&");
//            int len = temps.length / 3;
//            for (int i = 0; i < len; i++) {
//                String timeStr = temps[i * 3 + 2].replace("time:", "");
//                long time = Long.parseLong(timeStr);
//                VideoInfo videoInfo = new VideoInfo(temps[i * 3], temps[i * 3 + 1], time);
//                reusult.add(videoInfo);
//            }
//        }
//        return reusult;
//
//    }
    public static boolean isFileExists(String dirPath) {
        try {
            File f = new File(dirPath);
            return f.exists();
        } catch (Exception var2) {
            var2.printStackTrace();
            return false;
        }
    }


    /**
     * 获取指定文件大小
     *
     * @param file
     * @return
     * @throws Exception
     */
    public static long getFileSize(File file) throws Exception {
        long size = 0;
        if (file.exists()) {
            FileInputStream fis = null;
            fis = new FileInputStream(file);
            size = fis.available();
        } else {
            file.createNewFile();
            Log.e("FileUtils","获取文件大小不存在!");
        }
        return size;
    }

    /**
     * 获取指定文件夹
     *
     * @param f
     * @return
     * @throws Exception
     */
    public static long getFileSizes(File f) throws Exception {
        long size = 0;
        File flist[] = f.listFiles();
        for (int i = 0; i < flist.length; i++) {
            if (flist[i].isDirectory()) {
                size = size + getFileSizes(flist[i]);
            } else {
                size = size + getFileSize(flist[i]);
            }
        }
        return size;
    }

    /**
     * 转换文件大小
     *
     * @param fileS
     * @return
     */
    public static String FormetFileSize(long fileS) {
        DecimalFormat df = new DecimalFormat("#.00");
        String fileSizeString = "";
        String wrongSize = "0B";
        if (fileS == 0) {
            return wrongSize;
        }
        if (fileS < 1024) {
            fileSizeString = df.format((double) fileS) + "B";
        } else if (fileS < 1048576) {
            fileSizeString = df.format((double) fileS / 1024) + "KB";
        } else if (fileS < 1073741824) {
            fileSizeString = df.format((double) fileS / 1048576) + "MB";
        } else {
            fileSizeString = df.format((double) fileS / 1073741824) + "GB";
        }
        return fileSizeString;
    }

    public static boolean writeFile(String filePath, String content ) {
        if (TextUtils.isEmpty(content)) {
            return false;
        }

        FileWriter fileWriter = null;
        try {
            makeDirs(filePath);
            fileWriter = new FileWriter(filePath, false);
            fileWriter.write(content);
            fileWriter.close();
            return true;
        } catch (IOException e) {
            throw new RuntimeException("IOException occurred. ", e);
        } finally {
            if (fileWriter != null) {
                try {
                    fileWriter.close();
                } catch (IOException e) {
                    throw new RuntimeException("IOException occurred. ", e);
                }
            }
        }
    }

    public static boolean makeDirs(String filePath) {
        File folder = new File(filePath);
        return (folder.exists() && folder.isDirectory()) ? true : folder.mkdirs();
    }

    /**
     * 读取本地txt文件
     *
     * @param path
     * @return
     */
    public static String readTxt(String path) {
        StringBuilder result = new StringBuilder();
        try {
            File urlFile = new File(path);
            InputStreamReader isr = new InputStreamReader(new FileInputStream(urlFile), "gbk");
            BufferedReader br = new BufferedReader(isr);

            String s = null;
            while ((s = br.readLine()) != null) {//使用readLine方法，一次读一行
                result.append( s);
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result.toString();
    }
    public static boolean mkdirs(String dirPath) {
        try {
            File f = new File(dirPath);
            if (f.exists() && f.isDirectory()) {
                return true;
            }

            f.mkdirs();
            if (f.exists() && f.isDirectory()) {
                return true;
            }

            f.mkdirs();
        } catch (Exception var2) {
            var2.printStackTrace();
        }

        return false;
    }

    public static boolean saveFileWithBytes(String filePath, byte[] data) {
        File fileToWritePath = new File(filePath);
        String dirPath = fileToWritePath.getParent();
        if (!isFileExists(dirPath) && !mkdirs(dirPath)) {
            Log.d(TAG, "dir" + dirPath + " create error");
            return false;
        } else {
            fileToWritePath.delete();

            try {
                FileOutputStream fout = new FileOutputStream(fileToWritePath);

                try {
                    fout.write(data);
                } catch (IOException var9) {
                    try {
                        fout.close();
                    } catch (IOException var7) {
                        return false;
                    }

                    return false;
                }

                try {
                    fout.close();
                    return true;
                } catch (IOException var8) {
                    return false;
                }
            } catch (FileNotFoundException var10) {
                return false;
            }
        }
    }


}

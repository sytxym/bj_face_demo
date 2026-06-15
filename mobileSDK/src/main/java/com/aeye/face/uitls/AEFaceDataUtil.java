package com.aeye.face.uitls;

import static com.aeye.face.uitls.AESUtil.encrypt;

import android.graphics.Bitmap;

import com.aeye.android.uitls.BitmapUtils;

public class AEFaceDataUtil {
    private static String SM4Key = "E3A03D4A1586F6952F0E699344D0F4E2";
    private static String AESKey = "1234567890123456";

    public static Bitmap SM42Bitmap(String data) {
        return SMUtil.DataSM4Decode(SM4Key, data);
    }

    public static Bitmap AES2Bitmap(String data) {
        String base64 = AESUtil.decrypt(AESKey, data).replaceAll("QUVZRQ", "");
        return BitmapUtils.convertStringToBitmap(base64);
    }

    protected static String AESEnCode(Bitmap bitmap) {
        String pic = "QUVZRQ" + BitmapUtils.AEYE_Base64Encode(bitmap) + "QUVZRQ";
        String result = encrypt(AESKey, pic);
        return result;
    }
}

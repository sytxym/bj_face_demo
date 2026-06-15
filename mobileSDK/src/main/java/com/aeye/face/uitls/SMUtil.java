package com.aeye.face.uitls;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;

import com.aeye.android.uitls.BitmapUtils;
import com.aeye.mylibrary.utils.AeyeBase64;
import com.aeye.sm.SMCipherCaculater;
import com.aeye.sm.Sm2Keys;

import java.io.ByteArrayOutputStream;
import java.security.Key;

public class SMUtil {
    private static boolean isDebug = false;
    private static Sm2Keys keys;

    public static String DataSM4Encode(String sign, Bitmap bitmap) {
        return DataSM4Encode(sign, BitmapToBit(bitmap));
    }

    public static String DataSM4Encode(String sign, byte[] data) {
        //sm4加密  sign为sm4密钥  data 为图片数据组成的json串
        byte[] key = hexToBytes(sign);
        byte[] sm4_encrypt = SMCipherCaculater.SM4_encrypt(key, data);
//        String fileBase64 = AeyeBase64.toBase64String(sm4_encrypt);
        String fileBase64 = Base64.encodeToString(sm4_encrypt, 2);
        return fileBase64;
    }

    public static Bitmap DataSM4Decode(String sign, String data) {
        //新建一个BASE64Decoder
//        byte[] sm4_encrypt = AeyeBase64.decode(data);
        byte[] sm4_encrypt = Base64.decode(data, 2);
        byte[] key = hexToBytes(sign);
        byte[] imgData = SMCipherCaculater.SM4_decrypt(key, sm4_encrypt);
        Bitmap bitmap = BitmapFactory.decodeByteArray(imgData, 0, imgData.length);
        return bitmap;
    }

    public static String BitmapSM2Sign(Bitmap faceBitmap) {
        //sm2加密
        String base64 = convertIconToString(faceBitmap);
//        FLogUtil.saveLogServer("sign:" + base64);
//        byte[] picKey = generateImg(base64);
        byte[] picKey = base64.getBytes();
        if (isDebug) {
            //测试       boolean isDebug；true表示测试；false表示生产
            SMCipherCaculater.SM2_ChangeKeyPairTest();
        } else {
            SMCipherCaculater.SM2_ChangeKeyPairPrd();//生产
        }
//        Sm2Keys keys = SMCipherCaculater.SM2_generateKeyPair();
        keys = SMCipherCaculater.SM2_generateKeyPair();
        byte[] sm2encrypt = SMCipherCaculater.SM2_sign(picKey);

        //sm2签名串
        String sign = bytesToHexString(sm2encrypt);


        return sign;
    }

    public static boolean checkSign(Bitmap faceBitmap, String sign) {
        boolean result;
        String base64 = convertIconToString(faceBitmap);
//        FLogUtil.saveLogServer("图片Base64:" + base64);
//        FLogUtil.saveLogServer("pubKey:" + bytesToHexString(keys.pubKey));
//        FLogUtil.saveLogServer("签名:" + sign);
//        byte[] picKey = generateImg(base64);
        byte[] picKey = base64.getBytes();
        result = SMCipherCaculater.SM2_signVerify(picKey, keys.pubKey, hexToBytes(sign));
        return result;
    }

    public static String getPubKey() {
        String result = null;
        if (keys != null) {
            result = bytesToHexString(keys.getPubKey());
        }
        return result;
    }

    public static byte[] BitmapToBit(Bitmap bitmap) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos);
        return bos.toByteArray();
    }


    //工具方法
    public static byte[] hexToBytes(String hexString) {
        if (hexString != null && hexString.length() != 0) {
            char[] hex = hexString.toCharArray();
            int length = hex.length / 2;
            byte[] rawData = new byte[length];

            for (int i = 0; i < length; ++i) {
                int high = Character.digit(hex[i * 2], 16);
                int low = Character.digit(hex[i * 2 + 1], 16);
                int value = high << 4 | low;
                if (value > 127) {
                    value -= 256;
                }
                rawData[i] = (byte) value;
            }
            return rawData;
        } else {
            return null;
        }
    }

    public static final String bytesToHexString(byte[] buf) {
        return bytesToHexString(buf, buf.length);
    }

    public static final String bytesToHexString(byte[] buf, int length) {
        if (buf != null && buf.length >= length && length != 0) {
            StringBuilder sb = new StringBuilder(length * 2);
            String tmp = "";

            for (int i = 0; i < length; ++i) {
                tmp = Integer.toHexString(255 & buf[i]);
                tmp = tmp.length() == 1 ? "0" + tmp : tmp;
                sb.append(tmp);
            }

            return sb.toString();
        } else {
            return null;
        }
    }


    public static String convertIconToString(Bitmap bitmap) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos);
        byte[] appicon = bos.toByteArray();
        return Base64.encodeToString(appicon, 2);
    }


}

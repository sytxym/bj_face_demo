package com.aeye.face.uitls;

import android.util.Base64;
import android.util.Log;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class AESUtil {
    public static String encrypt(String secretKey, String data) {
        try {
            // 创建AES秘钥
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(), "AES");
            // 创建密码器
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            // 初始化加密器
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            byte[] encryptByte = cipher.doFinal(data.getBytes());
            // 将加密以后的数据进行 Base64 编码
            return Base64.encodeToString(encryptByte, 2);
        } catch (Exception e) {
            Log.e("xiaomin", "encrypt e = " + e.toString());
        }
        return null;
    }
    public static String decrypt(String secretKey, String base64Data) {
        try {

            byte[] data = Base64.decode(base64Data, 2);
            // 创建AES秘钥
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(), "AES");
            // 创建密码器
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            // 初始化解密器
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
            // 执行解密操作
            byte[] result = cipher.doFinal(data);
//            Log.e("xiaomin", "result = " + result.toString());
            return new String(result);
        } catch (Exception e) {
            Log.e("xiaomin", "e = " + e.toString());
        }
        return null;
    }
}

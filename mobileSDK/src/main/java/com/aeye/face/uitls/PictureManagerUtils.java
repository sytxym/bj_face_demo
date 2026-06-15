package com.aeye.face.uitls;


import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;


import com.aeye.android.data.AEFaceInfo;
import com.aeye.android.uitls.BitmapUtils;
import com.aeye.face.AEFacePack;
import com.aeye.face.AEFaceParam;
import com.sdk.core.BuildConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PictureManagerUtils {
    private String TAG = PictureManagerUtils.class.getSimpleName();
    private static PictureManagerUtils instance = null;
    private int m_picNum = 0;
    private int m_MaxPicNum = 3;
    private ArrayList<String> m_Pics = null;
    private ArrayList<String> m_PicSigns = null;
    private ArrayList<String> m_PicThumbnails = null;
    private ArrayList<String> m_PicThumbnailSigns = null;
    private ArrayList<String> mAlive = null;
    private ArrayList<String> mAliveSign = null;
    private Rect[] m_Faces = null;

    public static PictureManagerUtils getPictureManager() {
        if (instance == null)
            instance = new PictureManagerUtils();
        return instance;
    }

    public static void destroyManager() {
        instance = null;
    }

    private PictureManagerUtils() {
        m_Pics = new ArrayList<>();
        m_PicSigns = new ArrayList<>();
        m_PicThumbnails = new ArrayList<>();
        m_PicThumbnailSigns = new ArrayList<>();
        mAlive = new ArrayList<>();
        mAliveSign = new ArrayList<>();
    }

    public void setPicNum(int num) {
        m_Faces = new Rect[num];
        m_picNum = 0;
        m_MaxPicNum = num;
    }

    public void resetPictureManager() {
        m_picNum = 0;
        m_Pics.clear();
        m_PicSigns.clear();
        m_PicThumbnailSigns.clear();
        m_PicThumbnailSigns.clear();
        m_Faces = new Rect[m_MaxPicNum];
        mAlive.clear();
        mAliveSign.clear();
    }

    public void addOnePictureInfo(AEFaceInfo bitRect, int curPos) {
        Log.e(TAG,"add on PictureInfo ......");
        if ((curPos <= m_picNum) && (m_picNum < m_MaxPicNum)) {
            m_Faces[m_picNum] = bitRect.faceRect;
            BitmapAddToList(bitRect.faceBitmap, m_Pics, m_PicSigns);
            if (m_picNum == 0) {
                Bitmap bitmap = MBitmapUtil.scaleBitmap(bitRect.faceBitmap, 120);
                BitmapAddToList(bitmap, m_PicThumbnails, m_PicThumbnailSigns);
            }
            m_picNum++;
        }
    }

    public void addAliveImage(Bitmap alive) {
        BitmapAddToList(alive, mAlive, mAliveSign);
    }

    private String decryptKey = "E3A03D4A1586F6952F0E699344D0F4E2";

    public void BitmapAddToList(Bitmap bitmap, List<String> imgList, List<String> imgSignList) {
        if (AEFaceParam.ENCRYPT_TYPE_SM4 == AEFacePack.getInstance().getEncryptType()) {
            imgSignList.add(SMUtil.BitmapSM2Sign(bitmap));
            imgList.add(SMUtil.DataSM4Encode(decryptKey, bitmap));
        } else if (AEFaceParam.ENCRYPT_TYPE_AES == AEFacePack.getInstance().getEncryptType()) {
            imgList.add(AEFaceDataUtil.AESEnCode(bitmap));
        } else {
            imgList.add(BitmapUtils.AEYE_Base64Encode(bitmap));
        }
    }


    public Rect getFaceRect() {
        return m_Faces[m_picNum - 1];
    }

    public int getCurNum() {
        return m_picNum;
    }

    private int code;
    private int currentPose;

    public void setCode(int code) {
        this.code = code;
    }

    public void setCurrentPose(int currentPose) {
        this.currentPose = currentPose;
    }

    private String getJsonBody(Activity activity) {
        try {
            JSONArray imageJson = new JSONArray();
            JSONArray imageThumbnailJson = new JSONArray();
            JSONArray imageThumbnailSignJson = new JSONArray();
            JSONArray imageSignJson = new JSONArray();
            JSONArray rectJson = new JSONArray();
            JSONArray aliveJson = new JSONArray();
            JSONArray aliveSignJson = new JSONArray();
            JSONObject detailJson = new JSONObject();

            Array2JsonArray(rectJson, m_Faces);
            List2JsonArray(imageJson, m_Pics);//人脸
            List2JsonArray(imageSignJson, m_PicSigns);
            List2JsonArray(imageThumbnailJson, m_PicThumbnails);
            List2JsonArray(imageThumbnailSignJson, m_PicThumbnailSigns);
            List2JsonArray(aliveJson, mAlive);//全景
            List2JsonArray(aliveSignJson, mAliveSign);
            detailJson.put("code", code);
            detailJson.put("message", AEFaceParam.getCodeStr(code));
            detailJson.put("reason", AEFaceParam.getReasonStr(code));
            detailJson.put("pose", currentPose);
            detailJson.put("poseStr", AEFaceParam.getPoseStr(currentPose));
            detailJson.put("poseIndex", m_picNum);

            JSONObject json = new JSONObject();
            json.put("images", imageJson);
            json.put("compress_pic", imageThumbnailJson);
            json.put("alive", aliveJson);
            json.put("rect", rectJson);
            json.put("picnum", m_picNum + "");
            json.put("bioType", "1");
            json.put("channel", "005");
            json.put("isCompress", true);
            json.put("isCrypt", AEFacePack.getInstance().getEncryptType() != AEFaceParam.ENCRYPT_TYPE_NULL);
            json.put("enCryptType", AEFacePack.getInstance().getEncryptType());
            if (AEFaceParam.ENCRYPT_TYPE_SM4 == AEFacePack.getInstance().getEncryptType()) {
                json.put("imageSign", imageSignJson);
                json.put("aliveSign", aliveSignJson);
                json.put("compress_picSign", imageThumbnailSignJson);
                json.put("pubKey", SMUtil.getPubKey());
                json.put("decryptKey", decryptKey);
            }

            json.put("detail", detailJson);

            json.put("version", BuildConfig.versionName);
            return json.toString();
        } catch ( Exception e) {
            e.printStackTrace();
            return "JSONException error!";
        }
    }

    private void List2JsonArray(JSONArray jsonArray, List<String> list) {
        if (list != null && list.size() > 0) {
            for (String temp : list) {
                jsonArray.put(temp);
            }
        }
    }

    private void Array2JsonArray(JSONArray jsonArray, Rect[] rects) {
        if (rects != null && rects.length > 0) {
            for (Rect temp : rects) {
                if (temp != null)
                    jsonArray.put(temp.flattenToString());
            }
        }
    }

    public String getJsonString(Activity activity) {
//        if (m_picNum == 0)
//            return null;

        try {
            String message = getJsonBody(  activity);
            Log.e(TAG, "getJsonBody=" + message);
            return message;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }

}

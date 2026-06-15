package com.aeye.face.uitls;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;

import com.aeye.android.data.AEFaceInfo;
import com.aeye.face.AEFacePack;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class PictureManagerUtilsLight {
    private static PictureManagerUtilsLight instance = null;
    private int m_picNum = 0;
    private int m_MaxPicNum = 5;
    private String[] m_Pics = null;
    private String[] m_PicSigns = null;
    private ArrayList<String> mAlive = null;
    private ArrayList<String> mAliveSign = null;
    private Rect[] m_Faces = null;

    public static PictureManagerUtilsLight getPictureManager() {
        if (instance == null)
            instance = new PictureManagerUtilsLight();
        return instance;
    }

    public static void destroyManager() {
        instance = null;
    }

    private PictureManagerUtilsLight() {
        mAlive = new ArrayList<String>();
        mAliveSign = new ArrayList<>();
    }

    public void setPicNum(int num) {
        m_Pics = new String[num];
        m_PicSigns = new String[num];
        m_Faces = new Rect[num];
        m_picNum = 0;
        m_MaxPicNum = num;
    }

    public void resetPictureManager() {
        m_picNum = 0;
        m_Pics = new String[m_MaxPicNum];
        m_PicSigns = new String[m_MaxPicNum];
        m_Faces = new Rect[m_MaxPicNum];
        mAlive.clear();
        mAliveSign.clear();
    }

    public void addOnePictureInfo(AEFaceInfo bitRect, int curPos) {
        Log.e("LIULU","add pic : "+curPos+", mPicNum : "+m_picNum+" ,maxPic : "+m_MaxPicNum);
        if ((curPos <= m_picNum) && (m_picNum < m_MaxPicNum)) {
            m_Faces[m_picNum] = bitRect.faceRect;
            m_PicSigns[m_picNum] = SMUtil.BitmapSM2Sign(bitRect.faceBitmap);
            m_Pics[m_picNum++] = SMUtil.DataSM4Encode(decryptKey, bitRect.faceBitmap);
            FLogUtil.printLog("add pic success nums : "+m_picNum);
        }
    }


    private String decryptKey = "E3A03D4A1586F6952F0E699344D0F4E2";

    public void addAliveImage(Bitmap alive) {
        mAliveSign.add(SMUtil.BitmapSM2Sign(alive));
        mAlive.add(SMUtil.DataSM4Encode(decryptKey, alive));
    }

    public Rect getFaceRect() {
        return m_Faces[m_picNum - 1];
    }

    public int getCurNum() {
        return m_picNum;
    }

    private String getJsonBody(String aliveEncodeData) {
        try {
            JSONArray imageJson = new JSONArray();
            JSONArray imageSignJson = new JSONArray();
            JSONArray rectJson = new JSONArray();
            JSONArray aliveJson = new JSONArray();
            JSONArray aliveSignJson = new JSONArray();
            FLogUtil.printLog("add pic  result  mPics : "+m_Pics.length);
            for (int i = 0; i < m_picNum; i++) {
                imageJson.put(m_Pics[i]);
                imageSignJson.put(m_PicSigns[i]);
                rectJson.put(m_Faces[i].flattenToString());
            }
            for (int i = 0; i < mAlive.size(); i++) {
                aliveJson.put(mAlive.get(i));
                aliveSignJson.put(mAliveSign.get(i));
            }
            JSONObject json = new JSONObject();
            json.put("images", imageJson);
            json.put("imageSign", imageSignJson);
            json.put("alive", aliveJson);
            json.put("aliveSign", aliveSignJson);
            json.put("rect", rectJson);
            json.put("picnum", m_picNum + "");

            json.put("bioType", "1");
            json.put("channel", "005");
            json.put("isCompress", true);
            json.put("isCrypt", false);
            json.put("pubKey", SMUtil.getPubKey());
            json.put("decryptKey", decryptKey);
            json.put("sequnce", AEFacePack.getInstance().getColorSeq());
            if(aliveEncodeData !=null){
//                json.put("lightData", Base64.encodeToString(encodeData, 2));
                json.put("lightData", aliveEncodeData);
            }

            return json.toString();
        } catch (JSONException e) {
            e.printStackTrace();
            return "JSONException error!";
        }
    }

    private String getJsonBody(String aliveEncodeData,String alignBitBase64) {
        try {
            JSONArray imageJson = new JSONArray();
            JSONArray imageSignJson = new JSONArray();
            JSONArray rectJson = new JSONArray();
            JSONArray aliveJson = new JSONArray();
            JSONArray aliveSignJson = new JSONArray();
            FLogUtil.printLog("add pic  result align  mPics : "+m_Pics.length);
            for (int i = 0; i < m_picNum; i++) {
                imageJson.put(m_Pics[i]);
                if(m_PicSigns !=null && m_PicSigns[i] !=null)
                imageSignJson.put(m_PicSigns[i]);
                if(m_Faces !=null && m_Faces[i] !=null)
                rectJson.put(m_Faces[i].flattenToString());
            }
            for (int i = 0; i < mAlive.size(); i++) {
                aliveJson.put(mAlive.get(i));
                aliveSignJson.put(mAliveSign.get(i));
            }
            JSONObject json = new JSONObject();
            json.put("images", imageJson);
            json.put("imageSign", imageSignJson);
            json.put("alive", aliveJson);
            json.put("aliveSign", aliveSignJson);
            json.put("rect", rectJson);
            json.put("picnum", m_picNum + "");

            json.put("bioType", "1");
            json.put("channel", "005");
            json.put("isCompress", true);
            json.put("isCrypt", false);
            json.put("pubKey", SMUtil.getPubKey());
            json.put("decryptKey", decryptKey);
            json.put("sequnce", AEFacePack.getInstance().getColorSeq());
            if(aliveEncodeData !=null){
//                json.put("lightData", Base64.encodeToString(encodeData, 2));
                json.put("lightData", aliveEncodeData);
            }

            if(alignBitBase64 !=null){
                json.put("alignData", alignBitBase64);
            }

            return json.toString();
        } catch (JSONException e) {
            e.printStackTrace();
            return "JSONException error!";
        }
    }
//    private String getJsonBody() {
//        try {
//            JSONArray imageJson = new JSONArray();
//            JSONArray imageSignJson = new JSONArray();
//            JSONArray rectJson = new JSONArray();
//            JSONArray aliveJson = new JSONArray();
//            JSONArray aliveSignJson = new JSONArray();
//            for (int i = 0; i < m_picNum; i++) {
////				imageJson.put(m_Pics[i]);
//                imageJson.put("加密图片" + i);
//                imageSignJson.put("图片签名" + i);
//                rectJson.put(m_Faces[i].flattenToString());
//
//
//            }
//            for (int i = 0; i < mAlive.size(); i++) {
//                aliveJson.put("加密活体图片" + i);
//                aliveSignJson.put("活体图片签名" + i);
//            }
//            JSONObject json = new JSONObject();
//            json.put("images", imageJson);
//            json.put("imageSign", imageSignJson);
//            json.put("alive", aliveJson);
//            json.put("aliveSign", aliveSignJson);
//            json.put("rect", rectJson);
//            json.put("picnum", m_picNum + "");
//
//            json.put("bioType", "1");
//            json.put("channel", "005");
//            json.put("isCompress", true);
//            json.put("isCrypt", false);
//            json.put("pubKey", "公钥");
//            json.put("decryptKey", "密钥");
//
//            return json.toString();
//        } catch (JSONException e) {
//            e.printStackTrace();
//            return "JSONException error!";
//        }
//    }

    public String getJsonString(String aliveEncodeData) {
        if (m_picNum == 0)
            return null;
        try {
            return getJsonBody(aliveEncodeData);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public String getJsonString(String aliveEncodeData,String alignBitBase64) {
        if (m_picNum == 0)
            return null;
        try {
            return getJsonBody(aliveEncodeData,alignBitBase64);
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

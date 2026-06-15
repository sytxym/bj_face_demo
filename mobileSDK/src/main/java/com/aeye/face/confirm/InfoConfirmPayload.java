package com.aeye.face.confirm;

import android.text.TextUtils;

import org.json.JSONObject;

/**
 * 用户信息预览业务模型（映射 {@code faceUser/selectById/{userId}} 的 data.data）。
 */
public final class InfoConfirmPayload {

    private String userId;
    private String realName;
    private String region;
    private String idType;
    private String idNumber;
    private String userType;
    private String birthDay;
    private String mainTitle;
    private String subTitle;
    private String failDetail;

    public static InfoConfirmPayload fromApiData(JSONObject data) {
        InfoConfirmPayload p = new InfoConfirmPayload();
        if (data == null) {
            return p;
        }
        p.userId = data.optString("userId", null);
        p.realName = InfoConfirmParser.firstNonEmpty(data, "name", "realName");
        p.region = InfoConfirmParser.firstNonEmpty(data, "nation", "region");
        p.idType = InfoConfirmParser.firstNonEmpty(data, "certType", "idType");
        p.idNumber = InfoConfirmParser.firstNonEmpty(data, "certNo", "idNumber");
        p.userType = data.optString("userType", null);
        p.birthDay = data.optString("birthDay", null);
        p.mainTitle = data.optString("mainTitle", null);
        p.subTitle = data.optString("subTitle", null);
        p.failDetail = data.optString("failDetail", null);
        if (TextUtils.isEmpty(p.failDetail)) {
            p.failDetail = data.optString("memo", null);
        }
        return p;
    }

    public String getUserId() {
        return userId;
    }

    public String getRealName() {
        return realName;
    }

    public String getRegion() {
        return region;
    }

    public String getIdType() {
        return idType;
    }

    public String getIdNumber() {
        return idNumber;
    }

    public String getUserType() {
        return userType;
    }

    public String getBirthDay() {
        return birthDay;
    }

    public String getMainTitle() {
        return mainTitle;
    }

    public String getSubTitle() {
        return subTitle;
    }

    public String getFailDetail() {
        return failDetail;
    }
}

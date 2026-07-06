//package com.aeye.test;
//
//import android.text.TextUtils;
//
//import org.json.JSONException;
//import org.json.JSONObject;
//
///**
// * 解析扫码返回 JSON：{@code {"authIdentRecordId":"1234","userId":"123"}}。
// */
//public final class ScanAuthParser {
//
//    public static final class Result {
//        private final String authIdentRecordId;
//        private final String userId;
//
//        Result(String authIdentRecordId, String userId) {
//            this.authIdentRecordId = authIdentRecordId;
//            this.userId = userId;
//        }
//
//        public String getAuthIdentRecordId() {
//            return authIdentRecordId;
//        }
//
//        public String getUserId() {
//            return userId;
//        }
//    }
//
//    private ScanAuthParser() {
//    }
//
//    public static Result parse(String qrContent) throws JSONException {
//        if (TextUtils.isEmpty(qrContent)) {
//            throw new JSONException("扫码内容为空");
//        }
//        JSONObject json = new JSONObject(qrContent.trim());
//        String userId = json.optString("userId", null);
//        if (TextUtils.isEmpty(userId)) {
//            throw new JSONException("userId 为空");
//        }
//        String authIdentRecordId = json.optString("authIdentRecordId", null);
//        return new Result(authIdentRecordId, userId);
//    }
//}

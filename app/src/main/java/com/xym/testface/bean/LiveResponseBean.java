package com.xym.testface.bean;

import com.alibaba.fastjson2.JSON;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class LiveResponseBean implements Serializable {

    private int result;
    private String code;
    /**
     * seq : 1a9b452338637d28
     * info : 活体检测成功
     * debugInfo : {"elapse":"6227","path":"/flash-alive-api/liveness/action"}
     * scores : {"0":0.8291435837745667,"1":0.9599366784095764,"2":0.9702832102775574,"3":0.9273063540458679}
     * errorPic : null
     */

    private String seq;
    private String info;
    /**
     * elapse : 6227
     * path : /flash-alive-api/liveness/action
     */

    private DebugInfoBean debugInfo;
    /**
     * 0 : 0.8291435837745667
     * 1 : 0.9599366784095764
     * 2 : 0.9702832102775574
     * 3 : 0.9273063540458679
     */

    private String scores;
    private String errorPic;

    public static LiveResponseBean objectFromData(String str) {

        return JSON.parseObject(str, LiveResponseBean.class);
    }

    public static LiveResponseBean objectFromData(String str, String key) {

        try {
            JSONObject jsonObject = new JSONObject(str);

            return JSON.parseObject(jsonObject.getString(str), LiveResponseBean.class);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    public int getResult() {
        return result;
    }

    public void setResult(int result) {
        this.result = result;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getSeq() {
        return seq;
    }

    public void setSeq(String seq) {
        this.seq = seq;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public DebugInfoBean getDebugInfo() {
        return debugInfo;
    }

    public void setDebugInfo(DebugInfoBean debugInfo) {
        this.debugInfo = debugInfo;
    }

    public String getScores() {
        return scores;
    }

    public void setScores(String scores) {
        this.scores = scores;
    }

    public String getErrorPic() {
        return errorPic;
    }

    public void setErrorPic(String errorPic) {
        this.errorPic = errorPic;
    }

    public static class DebugInfoBean {
        private String elapse;//请求时间 毫秒
        private String path;

        public String getReqDataSize() {
            return reqDataSize;
        }

        public void setReqDataSize(String reqDataSize) {
            this.reqDataSize = reqDataSize;
        }

        private String reqDataSize;//请求包大小 ，字节

        public static DebugInfoBean objectFromData(String str) {

            return JSON.parseObject(str, DebugInfoBean.class);
        }

        public static DebugInfoBean objectFromData(String str, String key) {

            try {
                JSONObject jsonObject = new JSONObject(str);

                return JSON.parseObject(jsonObject.getString(str), DebugInfoBean.class);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        public String getElapse() {
            return elapse;
        }

        public void setElapse(String elapse) {
            this.elapse = elapse;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }

}

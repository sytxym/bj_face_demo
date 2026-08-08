package com.aeye.face.api.model;

import com.alibaba.fastjson.JSON;

/**
 * 炫彩服务端活体验证响应。
 */
public class LightAliveResponse {

    private String seq;
    private int result;
    private String info;
    private DebugInfoBean debugInfo;
    private Object colors;
    private double score;
    private String errorPic;

    public static LightAliveResponse parse(String str) {
        return JSON.parseObject(str, LightAliveResponse.class);
    }

    public static class DebugInfoBean {
        private String elapse;
        private String path;
        /** 请求包大小(字节) */
        private String reqDataSize;

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

        public String getReqDataSize() {
            return reqDataSize;
        }

        public void setReqDataSize(String reqDataSize) {
            this.reqDataSize = reqDataSize;
        }
    }

    public String getSeq() {
        return seq;
    }

    public void setSeq(String seq) {
        this.seq = seq;
    }

    public int getResult() {
        return result;
    }

    public void setResult(int result) {
        this.result = result;
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

    public Object getColors() {
        return colors;
    }

    public void setColors(Object colors) {
        this.colors = colors;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public String getErrorPic() {
        return errorPic;
    }

    public void setErrorPic(String errorPic) {
        this.errorPic = errorPic;
    }

    /** 成功回调: 返回 result 码和完整响应 */
    public interface Response {
        int onResponse(int result, LightAliveResponse info);
    }

    /** 失败回调: 返回响应对象和错误消息 */
    public interface WrongDeal {
        int onPostFailed(LightAliveResponse code, String message);
    }
}

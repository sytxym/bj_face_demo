package com.xym.testface.bean;

import java.io.Serializable;
import java.util.List;

public class LiveRequestEntity implements Serializable {
    private String app_id="system";
    private String app_secret="123456";
    private String seq;
    private String param;

    private String sn;
    private String deviceMode;

    public List<String> getRiskType() {
        return riskType;
    }

    public void setRiskType(List<String> riskType) {
        this.riskType = riskType;
    }

    private List<String> riskType;

    public String getDeviceMode() {
        return deviceMode;
    }

    public void setDeviceMode(String deviceMode) {
        this.deviceMode = deviceMode;
    }

    public String getSn() {
        return sn;
    }

    public void setSn(String sn) {
        this.sn = sn;
    }
    public String getApp_id() {
        return app_id;
    }

    public void setApp_id(String app_id) {
        this.app_id = app_id;
    }

    public String getApp_secret() {
        return app_secret;
    }

    public void setApp_secret(String app_secret) {
        this.app_secret = app_secret;
    }

    public String getSeq() {
        return seq;
    }

    public void setSeq(String seq) {
        this.seq = seq;
    }

    public String getParam() {
        return param;
    }

    public void setParam(String param) {
        this.param = param;
    }
}

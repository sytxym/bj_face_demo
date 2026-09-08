package com.aeye.face.confirm;

import android.text.TextUtils;

/**
 * 扫脸认证服务协议：正式 H5 地址待提供，当前为可滑动的测试页。
 */
public final class AgreementConfig {

    /** 测试用长页面（百度百科·人脸识别）；正式协议下发后改这里或 {@link com.aeye.face.AEFaceSdk#setAgreementUrl(String)} */
    public static final String DEFAULT_AGREEMENT_URL =
            "https://baike.baidu.com/item/%E4%BA%BA%E8%84%B8%E8%AF%86%E5%88%AB";

    public static final int READ_COUNTDOWN_SEC = 5;

    private static volatile String agreementUrl = DEFAULT_AGREEMENT_URL;

    private AgreementConfig() {
    }

    public static void setAgreementUrl(String url) {
        if (!TextUtils.isEmpty(url)) {
            agreementUrl = url.trim();
        }
    }

    public static String getAgreementUrl() {
        return TextUtils.isEmpty(agreementUrl) ? DEFAULT_AGREEMENT_URL : agreementUrl;
    }
}

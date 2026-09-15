package com.aeye.face.confirm;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

/**
 * 扫脸认证服务协议：正式 H5 地址待提供，当前为可滑动的测试页。
 */
public final class AgreementConfig {

    /** 测试用长页面（百度百科·人脸识别）；正式协议下发后改这里或 {@link com.aeye.face.AEFaceSdk#setAgreementUrl(String)} */
    public static final String DEFAULT_AGREEMENT_URL =
            "https://baike.baidu.com/item/%E4%BA%BA%E8%84%B8%E8%AF%86%E5%88%AB";

    public static final int READ_COUNTDOWN_SEC = 5;

    private static final String PREFS = "aeye_face_agreement";
    private static final String KEY_AGREED_URL = "agreed_url";

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

    /** 当前协议地址是否已在本机点过「已阅读」。协议 URL 变更后需重新同意。 */
    public static boolean hasAgreed(Context context) {
        if (context == null) {
            return false;
        }
        String saved = prefs(context).getString(KEY_AGREED_URL, null);
        return getAgreementUrl().equals(saved);
    }

    public static void markAgreed(Context context) {
        if (context == null) {
            return;
        }
        prefs(context).edit().putString(KEY_AGREED_URL, getAgreementUrl()).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}

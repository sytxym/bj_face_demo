package com.aeye.face.verify;

import android.text.TextUtils;

import org.json.JSONObject;

/**
 * 用户基本信息：优先由外部业务 App 传入；未传时可用活体配置接口返回的 {@code userInfo} 兜底。
 * <p>与「SDK 配置信息」（活体检测方式、动作配置等，见
 * {@link com.aeye.face.config.FaceActionOptions}）分开管理：本类只承载身份字段。</p>
 */
public final class FaceUserInfo {

    /** 姓名 */
    private final String certName;
    /** 证件类型 */
    private final String certType;
    /** 证件号码 */
    private final String certNo;
    /** 国家（地区） */
    private final String country;
    /** 用户 ID */
    private final String userId;
    /** 业务侧唯一 ID */
    private final String busId;
    /** 微信/支付宝用户 openId，查询核验结果时可选透传 */
    private final String openId;

    private FaceUserInfo(Builder b) {
        this.certName = b.certName;
        this.certType = b.certType;
        this.certNo = b.certNo;
        this.country = b.country;
        this.userId = b.userId;
        this.busId = b.busId;
        this.openId = b.openId;
    }

    public String getCertName() {
        return certName;
    }

    public String getCertType() {
        return certType;
    }

    public String getCertNo() {
        return certNo;
    }

    public String getCountry() {
        return country;
    }

    public String getUserId() {
        return userId;
    }

    public String getBusId() {
        return busId;
    }

    public String getOpenId() {
        return openId;
    }

    /** 解析配置接口 {@code userInfo} 节点。 */
    public static FaceUserInfo fromJson(JSONObject json) {
        if (json == null) {
            return null;
        }
        FaceUserInfo info = new Builder()
                .certName(optTrim(json, "certName"))
                .certType(optTrim(json, "certType"))
                .certNo(optTrim(json, "certNo"))
                .country(optTrim(json, "country"))
                .userId(optTrim(json, "userId"))
                .busId(optTrim(json, "busId"))
                .openId(optTrim(json, "openId"))
                .build();
        return info.hasIdentityOrAccount() ? info : null;
    }

    /**
     * 确认页/会话用：业务 App 传入的字段优先，空缺再用配置接口 {@code userInfo}。
     */
    public static FaceUserInfo mergePreferHost(FaceUserInfo host, FaceUserInfo fallback) {
        if (host == null) {
            return fallback;
        }
        if (fallback == null) {
            return host;
        }
        return new Builder()
                .certName(firstNonEmpty(host.certName, fallback.certName))
                .certType(firstNonEmpty(host.certType, fallback.certType))
                .certNo(firstNonEmpty(host.certNo, fallback.certNo))
                .country(firstNonEmpty(host.country, fallback.country))
                .userId(firstNonEmpty(host.userId, fallback.userId))
                .busId(firstNonEmpty(host.busId, fallback.busId))
                .openId(firstNonEmpty(host.openId, fallback.openId))
                .build();
    }

    private boolean hasIdentityOrAccount() {
        return !TextUtils.isEmpty(certName)
                || !TextUtils.isEmpty(certType)
                || !TextUtils.isEmpty(certNo)
                || !TextUtils.isEmpty(country)
                || !TextUtils.isEmpty(userId)
                || !TextUtils.isEmpty(busId)
                || !TextUtils.isEmpty(openId);
    }

    private static String optTrim(JSONObject json, String key) {
        if (json == null || !json.has(key) || json.isNull(key)) {
            return null;
        }
        String value = json.optString(key, null);
        if (TextUtils.isEmpty(value) || "null".equalsIgnoreCase(value.trim())) {
            return null;
        }
        return value.trim();
    }

    private static String firstNonEmpty(String primary, String fallback) {
        return TextUtils.isEmpty(primary) ? fallback : primary;
    }

    public static final class Builder {
        private String certName;
        private String certType;
        private String certNo;
        private String country;
        private String userId;
        private String busId;
        private String openId;

        public Builder certName(String certName) {
            this.certName = certName;
            return this;
        }

        public Builder certType(String certType) {
            this.certType = certType;
            return this;
        }

        public Builder certNo(String certNo) {
            this.certNo = certNo;
            return this;
        }

        public Builder country(String country) {
            this.country = country;
            return this;
        }

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder busId(String busId) {
            this.busId = busId;
            return this;
        }

        public Builder openId(String openId) {
            this.openId = openId;
            return this;
        }

        public FaceUserInfo build() {
            return new FaceUserInfo(this);
        }
    }
}

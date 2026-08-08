package com.aeye.face.verify;

/**
 * 用户基本信息：由外部业务 App 传入（SDK 已取消调用用户信息预览接口），
 * 活体完成后透传给人脸核验接口 {@code /assistant/faceIdent}。
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

    private FaceUserInfo(Builder b) {
        this.certName = b.certName;
        this.certType = b.certType;
        this.certNo = b.certNo;
        this.country = b.country;
        this.userId = b.userId;
        this.busId = b.busId;
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

    public static final class Builder {
        private String certName;
        private String certType;
        private String certNo;
        private String country;
        private String userId;
        private String busId;

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

        public FaceUserInfo build() {
            return new FaceUserInfo(this);
        }
    }
}

package com.aeye.face.config;

/**
 * 动作活体配置（与后台 {@link FaceActionConfigDefaults#API_PATH} 的 data 字段对齐）。
 * <p>拉取与缓存见 {@link FaceActionConfigRepository} / {@link FaceActionConfigManager}。</p>
 */
public final class FaceActionConfig {

    /**
     * 动作模式 {@code actionType}（后台数字码）：1 顺序、2 随机。仅认数字码。
     */
    /** 动作模式：按 enable* 顺序依次执行 */
    public static final String ACTION_SEQUENCE = "1";
    /** 动作模式：从动作池中随机 */
    public static final String ACTION_RANDOM = "2";

    /**
     * 检测类型 {@code detectType}（后台数字码）：
     * 1 静默活体、2 动作活体、3 炫彩活体、4 动作+炫彩活体。仅认数字码。
     * 见 {@link FaceActionConfigSdkMapper#mapDetectTypeToAliveMode(String)}。
     */
    /** 检测类型：静默活体 */
    public static final String DETECT_SILENT = "1";
    /** 检测类型：动作活体（默认） */
    public static final String DETECT_MOTION = "2";
    /** 检测类型：炫彩活体 */
    public static final String DETECT_LIGHT = "3";
    /** 检测类型：动作+炫彩活体 */
    public static final String DETECT_MOTION_LIGHT = "4";

    private long actionConfigId;
    private String businessCode;
    private String businessName;
    private String detectType;
    private String actionType = ACTION_RANDOM;
    private int actionCount = 3;
    private boolean enableLookUp = true;
    private boolean enableLookDown;
    private boolean enableShakeHead = true;
    private boolean enableOpenMouth;
    private boolean enableBlink = true;
    private String memo;

    public long getActionConfigId() {
        return actionConfigId;
    }

    public void setActionConfigId(long actionConfigId) {
        this.actionConfigId = actionConfigId;
    }

    public String getBusinessCode() {
        return businessCode;
    }

    public void setBusinessCode(String businessCode) {
        this.businessCode = businessCode;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getDetectType() {
        return detectType;
    }

    public void setDetectType(String detectType) {
        this.detectType = detectType;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public int getActionCount() {
        return actionCount;
    }

    public void setActionCount(int actionCount) {
        this.actionCount = actionCount;
    }

    public boolean isEnableLookUp() {
        return enableLookUp;
    }

    public void setEnableLookUp(boolean enableLookUp) {
        this.enableLookUp = enableLookUp;
    }

    public boolean isEnableLookDown() {
        return enableLookDown;
    }

    public void setEnableLookDown(boolean enableLookDown) {
        this.enableLookDown = enableLookDown;
    }

    public boolean isEnableShakeHead() {
        return enableShakeHead;
    }

    public void setEnableShakeHead(boolean enableShakeHead) {
        this.enableShakeHead = enableShakeHead;
    }

    public boolean isEnableOpenMouth() {
        return enableOpenMouth;
    }

    public void setEnableOpenMouth(boolean enableOpenMouth) {
        this.enableOpenMouth = enableOpenMouth;
    }

    public boolean isEnableBlink() {
        return enableBlink;
    }

    public void setEnableBlink(boolean enableBlink) {
        this.enableBlink = enableBlink;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    /** 1=固定顺序；其余（含 2 / 空）视为随机 */
    public boolean isSequenceActionType() {
        return ACTION_SEQUENCE.equals(actionType);
    }

    public boolean isRandomActionType() {
        return !isSequenceActionType();
    }

    public String toSummaryText() {
        StringBuilder sb = new StringBuilder();
        sb.append("业务=").append(businessCode);
        sb.append("，动作数=").append(actionCount);
        if (isSequenceActionType()) {
            sb.append("，模式=顺序[");
            if (enableLookUp) sb.append("抬头→");
            if (enableLookDown) sb.append("低头→");
            if (enableShakeHead) sb.append("摇头→");
            if (enableOpenMouth) sb.append("张嘴→");
            if (enableBlink) sb.append("眨眼→");
            if (sb.charAt(sb.length() - 1) == '→') {
                sb.setLength(sb.length() - 1);
            }
            sb.append(']');
        } else {
            sb.append("，模式=随机");
        }
        return sb.toString();
    }
}

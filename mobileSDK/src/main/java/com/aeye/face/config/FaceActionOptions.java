package com.aeye.face.config;

import android.os.Bundle;

import com.aeye.face.AEFaceParam;

/**
 * 宿主启动「本地核验」时可自定义的动作活体配置参数。
 * <p>
 * 用于 {@link com.aeye.face.AEFaceVerifyFlow#startLocal} 场景：不依赖后台动作配置接口，
 * 由宿主直接指定动作种类、顺序/随机、动作数量、活体难度与单动作超时。
 * </p>
 * 通过 {@link Builder} 构建，未设置项使用默认值（顺序：抬头→摇头→眨眼，难度 1，超时 15s）。
 */
public final class FaceActionOptions {

    private final String actionType;
    private final int actionCount;
    private final boolean enableLookUp;
    private final boolean enableLookDown;
    private final boolean enableShakeHead;
    private final boolean enableOpenMouth;
    private final boolean enableBlink;
    private final int aliveLevel;
    private final int motionTimeoutSec;
    private final boolean voiceEnabled;

    private FaceActionOptions(Builder b) {
        this.actionType = b.actionType;
        this.actionCount = b.actionCount;
        this.enableLookUp = b.enableLookUp;
        this.enableLookDown = b.enableLookDown;
        this.enableShakeHead = b.enableShakeHead;
        this.enableOpenMouth = b.enableOpenMouth;
        this.enableBlink = b.enableBlink;
        this.aliveLevel = b.aliveLevel;
        this.motionTimeoutSec = b.motionTimeoutSec;
        this.voiceEnabled = b.voiceEnabled;
    }

    /** 默认配置：顺序动作[抬头→摇头→眨眼]、3 个动作、难度 1、超时 15s、开启语音。 */
    public static FaceActionOptions defaults() {
        return new Builder().build();
    }

    /** 转为 {@link FaceActionConfig}，复用既有的 SDK 参数映射逻辑。 */
    public FaceActionConfig toActionConfig() {
        FaceActionConfig config = new FaceActionConfig();
        config.setActionType(actionType);
        config.setActionCount(actionCount);
        config.setEnableLookUp(enableLookUp);
        config.setEnableLookDown(enableLookDown);
        config.setEnableShakeHead(enableShakeHead);
        config.setEnableOpenMouth(enableOpenMouth);
        config.setEnableBlink(enableBlink);
        return config;
    }

    /**
     * 将动作配置写入 SDK 启动参数：动作映射复用 {@link FaceActionConfigSdkMapper}，
     * 另外覆盖活体难度 {@code AliveLevel}、单动作超时 {@code SingleAliveMotionTime} 与语音开关。
     */
    public void applyToBundle(Bundle paras) {
        if (paras == null) {
            return;
        }
        FaceActionConfigSdkMapper.applyToBundle(paras, toActionConfig());
        paras.putInt(AEFaceParam.AliveLevel, clampAliveLevel(aliveLevel));
        if (motionTimeoutSec > 0) {
            paras.putInt(AEFaceParam.SingleAliveMotionTime, motionTimeoutSec);
        }
        paras.putInt(AEFaceParam.VoiceSwitch, voiceEnabled ? 1 : 0);
    }

    private static int clampAliveLevel(int level) {
        if (level < 1) {
            return 1;
        }
        if (level > 3) {
            return 3;
        }
        return level;
    }

    public String getActionType() {
        return actionType;
    }

    public int getActionCount() {
        return actionCount;
    }

    public boolean isEnableLookUp() {
        return enableLookUp;
    }

    public boolean isEnableLookDown() {
        return enableLookDown;
    }

    public boolean isEnableShakeHead() {
        return enableShakeHead;
    }

    public boolean isEnableOpenMouth() {
        return enableOpenMouth;
    }

    public boolean isEnableBlink() {
        return enableBlink;
    }

    public int getAliveLevel() {
        return aliveLevel;
    }

    public int getMotionTimeoutSec() {
        return motionTimeoutSec;
    }

    public boolean isVoiceEnabled() {
        return voiceEnabled;
    }

    public static final class Builder {
        private String actionType = FaceActionConfig.ACTION_SEQUENCE;
        private int actionCount = 3;
        private boolean enableLookUp = true;
        private boolean enableLookDown = false;
        private boolean enableShakeHead = true;
        private boolean enableOpenMouth = false;
        private boolean enableBlink = true;
        private int aliveLevel = FaceSdkHostParamBuilder.DEFAULT_ALIVE_LEVEL;
        private int motionTimeoutSec = 15;
        private boolean voiceEnabled = true;

        /** 动作模式：{@link FaceActionConfig#ACTION_SEQUENCE} 顺序 / {@link FaceActionConfig#ACTION_RANDOM} 随机。 */
        public Builder actionType(String type) {
            if (type != null) {
                this.actionType = type;
            }
            return this;
        }

        /** 动作数量（0~5，超出自动钳制）。 */
        public Builder actionCount(int count) {
            this.actionCount = count;
            return this;
        }

        public Builder enableLookUp(boolean enable) {
            this.enableLookUp = enable;
            return this;
        }

        public Builder enableLookDown(boolean enable) {
            this.enableLookDown = enable;
            return this;
        }

        public Builder enableShakeHead(boolean enable) {
            this.enableShakeHead = enable;
            return this;
        }

        public Builder enableOpenMouth(boolean enable) {
            this.enableOpenMouth = enable;
            return this;
        }

        public Builder enableBlink(boolean enable) {
            this.enableBlink = enable;
            return this;
        }

        /** 活体难度：1 最低（最宽松），2 中等，3 最高。 */
        public Builder aliveLevel(int level) {
            this.aliveLevel = level;
            return this;
        }

        /** 单个动作超时（秒）。 */
        public Builder motionTimeoutSec(int seconds) {
            this.motionTimeoutSec = seconds;
            return this;
        }

        /** 是否播放语音提示。 */
        public Builder voiceEnabled(boolean enabled) {
            this.voiceEnabled = enabled;
            return this;
        }

        public FaceActionOptions build() {
            return new FaceActionOptions(this);
        }
    }
}

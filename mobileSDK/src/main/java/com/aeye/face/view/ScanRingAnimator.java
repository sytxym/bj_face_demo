package com.aeye.face.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

/**
 * 取景扫描环：检测态蓝弧旋转、无人脸延迟隐藏、提交态灰轨、成功填充。
 * View 绘制仍在 {@link ScanRingOverlayView}，这里只驱动动画与可见态。
 */
final class ScanRingAnimator {

    private static final long HIDE_SCAN_ARC_DEBOUNCE_MS = 400L;
    private static final long ROTATE_DURATION_MS = 1400L;
    private static final long SUCCESS_FILL_DURATION_MS = 420L;

    interface Host {
        /** 成功/失败/核验中/页面结束时，不再驱动检测态蓝弧 */
        boolean isScanRingBlocked();
    }

    private final ScanRingOverlayView ring;
    private final Host host;
    private final Runnable hideIdleRunnable;
    private ValueAnimator rotateAnimator;
    private ValueAnimator successAnimator;

    ScanRingAnimator(ScanRingOverlayView ring, Host host) {
        this.ring = ring;
        this.host = host;
        this.hideIdleRunnable = this::hideIfIdle;
    }

    boolean isAvailable() {
        return ring != null;
    }

    void setHoleMaskEnabled(boolean enabled) {
        if (ring != null) {
            ring.setHoleMaskEnabled(enabled);
        }
    }

    void setVisible(boolean visible) {
        if (ring != null) {
            ring.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    /** 仅灰色轨道，不转蓝弧（动画时钟保留，避免下一动作重开卡顿） */
    void prepare() {
        if (ring == null) {
            return;
        }
        ring.removeCallbacks(hideIdleRunnable);
        ring.setMode(ScanRingOverlayView.MODE_SCANNING);
        ring.setVisibility(View.VISIBLE);
        ring.setScanArcEnabled(false);
        ring.setProgress(0f);
    }

    /**
     * 有人脸启动蓝弧；无人脸时纯动作延迟隐藏，炫彩立即停。
     */
    void updateForFace(boolean hasFace, boolean hideImmediately) {
        if (host.isScanRingBlocked()) {
            return;
        }
        if (hasFace) {
            if (ring != null) {
                ring.removeCallbacks(hideIdleRunnable);
            }
            start();
            return;
        }
        if (ring == null) {
            return;
        }
        ring.removeCallbacks(hideIdleRunnable);
        if (hideImmediately) {
            hideIfIdle();
        } else {
            ring.postDelayed(hideIdleRunnable, HIDE_SCAN_ARC_DEBOUNCE_MS);
        }
    }

    void stop() {
        if (ring != null) {
            ring.removeCallbacks(hideIdleRunnable);
        }
        if (rotateAnimator != null) {
            rotateAnimator.cancel();
        }
        if (ring != null
                && ring.getMode() == ScanRingOverlayView.MODE_SCANNING
                && ring.getProgress() <= 0f) {
            ring.setScanArcEnabled(false);
        }
    }

    /**
     * 提交核验：灰轨可见、蓝弧停、进度归零，便于后续成功填充。
     */
    void prepareVerifying() {
        if (ring == null) {
            return;
        }
        stop();
        ring.setMode(ScanRingOverlayView.MODE_SCANNING);
        ring.setScanArcEnabled(false);
        ring.setProgress(0f);
        ring.setVisibility(View.VISIBLE);
    }

    void resetForRetry() {
        if (ring == null) {
            return;
        }
        ring.setMode(ScanRingOverlayView.MODE_SCANNING);
        ring.setScanArcEnabled(false);
        ring.setProgress(0f);
    }

    /** 蓝弧扫满后切绿色整圈 */
    void animateToSuccess() {
        if (ring == null) {
            return;
        }
        ring.setVisibility(View.VISIBLE);
        if (rotateAnimator != null) {
            rotateAnimator.cancel();
        }
        if (successAnimator != null) {
            successAnimator.cancel();
        }
        final float start = ring.getProgress();
        successAnimator = ValueAnimator.ofFloat(start, 1f);
        successAnimator.setDuration(SUCCESS_FILL_DURATION_MS);
        successAnimator.setInterpolator(new DecelerateInterpolator());
        successAnimator.addUpdateListener(animation ->
                ring.setProgress((Float) animation.getAnimatedValue()));
        successAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                ring.setMode(ScanRingOverlayView.MODE_SUCCESS);
            }
        });
        successAnimator.start();
    }

    void release() {
        stop();
        if (rotateAnimator != null) {
            rotateAnimator.cancel();
            rotateAnimator = null;
        }
        if (successAnimator != null) {
            successAnimator.cancel();
            successAnimator = null;
        }
    }

    private void hideIfIdle() {
        if (ring == null || host.isScanRingBlocked()) {
            return;
        }
        if (ring.getMode() == ScanRingOverlayView.MODE_SCANNING
                && ring.getProgress() <= 0f) {
            ring.setScanArcEnabled(false);
        }
    }

    private void start() {
        if (ring == null || host.isScanRingBlocked()) {
            return;
        }
        ring.removeCallbacks(hideIdleRunnable);
        ring.setMode(ScanRingOverlayView.MODE_SCANNING);
        ring.setVisibility(View.VISIBLE);
        ring.setScanArcEnabled(true);
        if (rotateAnimator != null && rotateAnimator.isStarted()) {
            return;
        }
        if (rotateAnimator == null) {
            rotateAnimator = ValueAnimator.ofFloat(0f, 360f);
            rotateAnimator.setDuration(ROTATE_DURATION_MS);
            rotateAnimator.setRepeatCount(ValueAnimator.INFINITE);
            rotateAnimator.setInterpolator(new LinearInterpolator());
            rotateAnimator.addUpdateListener(animation -> {
                if (ring == null || host.isScanRingBlocked()) {
                    return;
                }
                if (!ring.isScanArcEnabled()) {
                    return;
                }
                ring.setArcStartAngle((Float) animation.getAnimatedValue());
            });
        }
        float current = ring.getArcStartAngle();
        long duration = rotateAnimator.getDuration();
        if (duration > 0) {
            rotateAnimator.setCurrentPlayTime((long) ((current / 360f) * duration));
        }
        rotateAnimator.start();
    }
}

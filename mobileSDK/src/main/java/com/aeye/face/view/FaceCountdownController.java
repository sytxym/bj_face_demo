package com.aeye.face.view;

import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

/**
 * 取景倒计时：动作 10s / 出框·多人·环境光 20s。
 * 超时后的失败页由 {@link Host#onCountdownFinished(int)} 处理。
 */
final class FaceCountdownController {

    static final int NONE = 0;
    static final int ACTION = 1;
    static final int NO_FACE = 2;
    static final int MULTI_FACE = 3;
    static final int LIGHT = 4;

    interface Host {
        boolean isCountdownBlocked();

        boolean shouldShowActionCountdown();

        boolean canShowCountdownNumber();

        boolean shouldSkipActionTimer();

        int displayedGuideState();

        Handler timerHandler();

        void postToUi(Runnable r);

        void onCountdownFinished(int kind);
    }

    private final CountView legacyView;
    private final TextView actionView;
    private final Host host;
    private Ticker ticker;
    private int kind = NONE;
    private boolean frozen;
    private int actionRemainSec;

    FaceCountdownController(CountView legacyView, TextView actionView, Host host) {
        this.legacyView = legacyView;
        this.actionView = actionView;
        this.host = host;
    }

    int getKind() {
        return kind;
    }

    boolean isAction() {
        return kind == ACTION;
    }

    boolean isNoFace() {
        return kind == NO_FACE;
    }

    boolean isMultiFace() {
        return kind == MULTI_FACE;
    }

    boolean isLight() {
        return kind == LIGHT;
    }

    void setFrozen(boolean frozen) {
        this.frozen = frozen;
    }

    int getActionRemainSec() {
        return actionRemainSec;
    }

    void setActionRemainSec(int sec) {
        actionRemainSec = Math.max(0, sec);
    }

    void clearActionRemain() {
        actionRemainSec = 0;
    }

    void stashActionRemain() {
        if (isAction() && ticker != null) {
            actionRemainSec = ticker.getRemain();
        }
    }

    int getRemain() {
        return ticker != null ? ticker.getRemain() : 0;
    }

    void stop() {
        if (ticker != null) {
            ticker.cancel();
        }
        kind = NONE;
        hide();
    }

    void cancelKind(int targetKind) {
        if (kind != targetKind) {
            return;
        }
        if (ticker != null) {
            ticker.cancel();
        }
        kind = NONE;
    }

    boolean restart(long timeout, int newKind) {
        if (newKind == ACTION && !host.shouldShowActionCountdown()) {
            if (kind == ACTION) {
                stop();
            }
            return false;
        }
        if (newKind == ACTION && host.shouldSkipActionTimer()) {
            return false;
        }
        if (host.isCountdownBlocked()) {
            return false;
        }
        Handler timerHandler = host.timerHandler();
        if (timerHandler == null) {
            return false;
        }
        if (ticker != null) {
            ticker.cancel();
        } else {
            ticker = new Ticker(timerHandler);
        }
        kind = newKind;
        ticker.init((int) timeout);
        ticker.start();
        return true;
    }

    void publishNow() {
        if (ticker != null && kind != NONE) {
            if (kind == ACTION && !host.shouldShowActionCountdown()) {
                stop();
            } else {
                show(ticker.getRemain());
            }
        } else {
            hide();
        }
    }

    void show(int seconds) {
        if (actionView == null) {
            return;
        }
        if (Looper.myLooper() != Looper.getMainLooper()) {
            host.postToUi(() -> show(seconds));
            return;
        }
        if (frozen) {
            if (host.displayedGuideState() == 1) {
                return;
            }
            actionView.setVisibility(View.INVISIBLE);
            return;
        }
        if (!host.canShowCountdownNumber()) {
            actionView.setVisibility(View.INVISIBLE);
            return;
        }
        if (kind == ACTION && host.displayedGuideState() != 1) {
            actionView.setVisibility(View.INVISIBLE);
            return;
        }
        actionView.setText(seconds + "s");
        actionView.setVisibility(View.VISIBLE);
    }

    void hide() {
        if (actionView == null) {
            return;
        }
        if (Looper.myLooper() != Looper.getMainLooper()) {
            host.postToUi(this::hide);
            return;
        }
        actionView.setVisibility(View.INVISIBLE);
    }

    void hideForResult() {
        if (actionView != null) {
            actionView.setVisibility(View.GONE);
        }
    }

    void setLegacyVisible(boolean visible) {
        if (legacyView != null) {
            legacyView.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        }
    }

    void setLegacyGone() {
        if (legacyView != null) {
            legacyView.setVisibility(View.GONE);
        }
    }

    void resetSessionFlags() {
        frozen = false;
        actionRemainSec = 0;
    }

    void release() {
        if (ticker != null) {
            ticker.cancel();
            ticker = null;
        }
        kind = NONE;
    }

    private final class Ticker implements Runnable {
        private final Handler handler;
        private int time;
        private int count;

        Ticker(Handler handler) {
            this.handler = handler;
        }

        void init(int second) {
            time = Math.max(1, second);
            count = time;
            if (legacyView != null) {
                legacyView.setCount(time, time);
            }
            show(time);
        }

        void start() {
            count = time;
            if (legacyView != null) {
                legacyView.setCount(time, count);
            }
            show(count);
            handler.removeCallbacks(this);
            handler.postDelayed(this, 1000);
        }

        void cancel() {
            handler.removeCallbacks(this);
        }

        int getRemain() {
            return Math.max(0, count);
        }

        @Override
        public void run() {
            count--;
            if (count <= 0) {
                int finishedKind = kind;
                kind = NONE;
                hide();
                host.onCountdownFinished(finishedKind);
                return;
            }
            if (legacyView != null) {
                legacyView.setCount(time, count);
            }
            show(count);
            handler.postDelayed(this, 1000);
        }
    }
}

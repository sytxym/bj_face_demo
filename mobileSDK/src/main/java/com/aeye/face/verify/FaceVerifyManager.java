package com.aeye.face.verify;

import android.text.TextUtils;
import android.util.Log;

import com.aeye.face.AEFaceSdk;
import com.aeye.face.api.FaceApiService;
import com.aeye.face.api.model.AuthStatusResult;
import com.aeye.face.api.model.FaceIdentResult;
import com.aeye.face.config.FaceActionConfig;
import com.aeye.face.config.FaceActionConfigManager;

import org.json.JSONArray;

import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 活体完成后的人脸核验：提交 faceIdent 后轮询 {@code /faceRecord/queryVerifyResult}，
 * 以查询结果为最终通过/未通过。
 */
public final class FaceVerifyManager {

    private static final String TAG = "FaceVerifyManager";

    /**
     * 含建连、上传大图（炫彩多图）、读响应、以及提交成功后的 queryVerifyResult 轮询。
     * 略大于 {@link FaceApiService} faceIdent 的 HTTP 超时，避免过早 cancel 打断上传。
     */
    private static final long VERIFY_TIMEOUT_MS = 45_000L;
    /** 「人脸核验中」UI 最短兜底，需盖住 faceIdent 超时 */
    private static final long VERIFY_UI_TIMEOUT_FLOOR_MS = 70_000L;
    /** 每次 queryVerifyResult 预留的网络耗时 */
    private static final long POLL_HTTP_BUDGET_MS = 3_000L;

    public interface Callback {
        void onPassed(FaceIdentResult result);

        void onFailed(String message);

        /** @param backendErrorCode 查询接口 {@code ok=false} 时的 {@code errorCode}，其它失败为 null */
        default void onFailed(String message, String backendErrorCode) {
            onFailed(message);
        }
    }

    private FaceVerifyManager() {
    }

    /**
     * @param livenessJson {@link com.aeye.face.uitls.PictureManagerUtils#getJsonString} 返回的 JSON
     */
    public static void submit(String livenessJson, Callback callback) {
        submitInternal(() -> {
            AEFaceSdk.ensureInitialized();
            return FaceApiService.submitFaceIdent(
                    AEFaceSdk.getApiBaseUrl(),
                    livenessJson,
                    FaceVerifySession.getUserId(),
                    FaceVerifySession.getAuthRecordId());
        }, callback);
    }

    /**
     * 炫彩活体提交：faceIdent 请求体带 {@code isColor/seq/colorPics} 炫彩字段。
     *
     * @param seq       拉色接口返回的唯一序列
     * @param facePics  解密后的人脸原图 base64 列表，映射 facePic1~facePic6（与动作活体一致）
     * @param colorPics 炫彩算法图 base64 列表
     */
    public static void submitColor(String seq, JSONArray facePics, JSONArray colorPics, Callback callback) {
        submitInternal(() -> {
            AEFaceSdk.ensureInitialized();
            return FaceApiService.submitFaceIdentColor(
                    AEFaceSdk.getApiBaseUrl(),
                    FaceVerifySession.getUserId(),
                    FaceVerifySession.getAuthRecordId(),
                    FaceVerifySession.getBusinessCode(),
                    seq,
                    facePics,
                    colorPics);
        }, callback);
    }

    private static void submitInternal(Callable<FaceIdentResult> task, Callback callback) {
        if (callback == null) {
            return;
        }
        final AtomicBoolean finished = new AtomicBoolean(false);
        final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "AEFace-Verify");
            t.setDaemon(true);
            return t;
        });
        final Future<FaceIdentResult> future = executor.submit(task);
        new Thread(() -> {
            try {
                FaceIdentResult result = future.get(VERIFY_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                if (finished.get()) {
                    return;
                }
                if (result != null && result.isPass()) {
                    Log.d(TAG, "faceIdent ok, start queryVerifyResult poll");
                    dispatchFinalFromAuthStatus(finished, callback, result);
                } else if (finished.compareAndSet(false, true)) {
                    callback.onFailed("服务异常，请重试");
                }
            } catch (TimeoutException e) {
                future.cancel(true);
                // 超时：调试开关打开时回退 Mock 通过（联调 faceIdent 未部署/过慢）
                dispatchError(finished, callback, e);
            } catch (ExecutionException e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                dispatchError(finished, callback, cause);
            } catch (Exception e) {
                future.cancel(true);
                dispatchError(finished, callback, e);
            } finally {
                executor.shutdownNow();
            }
        }, "AEFace-Verify-Wait").start();
    }

    /**
     * faceIdent 提交成功后以 {@code /faceRecord/queryVerifyResult} 为最终结果：
     * 按配置 {@code pollingTime} 秒等间隔查询，共 {@code pollingCount} 次（时刻 0、T、2T…）；
     * 拿到 status=4/5 即结束；仍未拿到则上报 {@code updateRecord status=2}（异常退出）。
     */
    private static void dispatchFinalFromAuthStatus(AtomicBoolean finished, Callback callback,
                                                    FaceIdentResult identResult) {
        Log.d(TAG, "poll queryVerifyResult authRecordId=" + FaceVerifySession.getAuthRecordId());
        try {
            AuthStatusResult auth = pollAuthStatus();
            if (!finished.compareAndSet(false, true)) {
                return;
            }
            if (auth.isPassed()) {
                callback.onPassed(identResult);
            } else {
                callback.onFailed(auth.displayFailMessage(), auth.getBackendErrorCode());
            }
        } catch (Exception e) {
            if (AEFaceSdk.isUseMockOnError()) {
                Log.w(TAG, "queryVerifyResult failed, fallback mock pass. cause="
                        + e.getClass().getSimpleName() + ": " + e.getMessage());
                if (finished.compareAndSet(false, true)) {
                    callback.onPassed(identResult);
                }
                return;
            }
            dispatchError(finished, callback, e);
        }
    }

    /**
     * 「人脸核验中」UI 兜底超时：faceIdent 上传 + 按配置轮询 queryVerifyResult。
     */
    public static long recommendedUiTimeoutMs() {
        FaceActionConfig config = FaceActionConfigManager.getCached();
        int count = config != null
                ? config.resolvedPollingCount()
                : FaceActionConfig.DEFAULT_POLLING_COUNT;
        long pollWait = config != null
                ? config.pollingSpanMs()
                : (long) (FaceActionConfig.DEFAULT_POLLING_COUNT - 1)
                * FaceActionConfig.DEFAULT_POLLING_TIME_SEC * 1000L;
        long need = VERIFY_TIMEOUT_MS + pollWait + (long) count * POLL_HTTP_BUDGET_MS;
        return Math.max(VERIFY_UI_TIMEOUT_FLOOR_MS, need);
    }

    private static AuthStatusResult pollAuthStatus() throws Exception {
        String authRecordId = FaceVerifySession.getAuthRecordId();
        if (TextUtils.isEmpty(authRecordId)) {
            throw new IllegalArgumentException("authRecordId 为空");
        }
        FaceActionConfig config = FaceActionConfigManager.getCached();
        int maxTries = config != null
                ? config.resolvedPollingCount()
                : FaceActionConfig.DEFAULT_POLLING_COUNT;
        long intervalMs = (config != null
                ? config.resolvedPollingTimeSec()
                : FaceActionConfig.DEFAULT_POLLING_TIME_SEC) * 1000L;
        Log.d(TAG, "poll queryVerifyResult count=" + maxTries
                + ", intervalSec=" + (intervalMs / 1000)
                + ", authRecordId=" + authRecordId);
        AuthStatusResult last = null;
        for (int i = 0; i < maxTries; i++) {
            if (i > 0 && intervalMs > 0) {
                Thread.sleep(intervalMs);
            }
            try {
                last = FaceApiService.queryVerifyResult(AEFaceSdk.getApiBaseUrl(), authRecordId);
                if (last.isApiError() || last.isFinishedResult()) {
                    return last;
                }
                Log.d(TAG, "queryVerifyResult try " + (i + 1) + "/" + maxTries
                        + " status=" + last.getStatus() + ", wait next");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw e;
            } catch (Exception e) {
                Log.w(TAG, "queryVerifyResult try " + (i + 1) + "/" + maxTries
                        + " failed: " + e.getMessage());
            }
        }
        if (AEFaceSdk.isUseMockOnError()) {
            Log.w(TAG, "queryVerifyResult no result after " + maxTries
                    + " tries, fallback mock pass");
            return AuthStatusResult.of(QrRecordStatus.PASSED, null);
        }
        Log.w(TAG, "queryVerifyResult no result after " + maxTries
                + " tries, report updateRecord status=2");
        QrRecordStatusManager.update(QrRecordStatus.ABNORMAL_EXIT);
        return AuthStatusResult.abnormalExit();
    }

    private static void dispatchError(AtomicBoolean finished, Callback callback, Throwable error) {
        if (finished.get()) {
            return;
        }
        if (AEFaceSdk.isUseMockOnError()) {
            try {
                Log.w(TAG, "faceIdent failed, mock pass then poll queryVerifyResult. cause="
                        + (error != null ? error.getClass().getSimpleName() + ": " + error.getMessage() : "null"));
                FaceIdentResult mock = FaceApiService.mockFaceIdentPass(
                        FaceVerifySession.getUserId(),
                        FaceVerifySession.getAuthRecordId());
                dispatchFinalFromAuthStatus(finished, callback, mock);
            } catch (Exception mockError) {
                if (finished.compareAndSet(false, true)) {
                    callback.onFailed(friendlyMessage(mockError));
                }
            }
            return;
        }
        if (!finished.compareAndSet(false, true)) {
            return;
        }
        callback.onFailed(friendlyMessage(error));
    }

    private static String friendlyMessage(Throwable error) {
        if (error == null) {
            return "服务异常，请重试";
        }
        if (error instanceof SocketTimeoutException
                || error instanceof TimeoutException
                || error instanceof InterruptedIOException
                || error instanceof InterruptedException) {
            return "网络超时，请重试";
        }
        if (error instanceof UnknownHostException
                || error instanceof ConnectException) {
            return "网络异常，请稍后重试";
        }
        String msg = error.getMessage();
        if (msg != null) {
            String lower = msg.toLowerCase();
            if (lower.contains("timeout")
                    || lower.contains("timed out")
                    || lower.contains("interrupted")) {
                return "网络超时，请重试";
            }
            if (lower.contains("failed to connect")
                    || lower.contains("connection refused")
                    || lower.contains("unable to resolve")
                    || lower.contains("unknownhost")) {
                return "网络异常，请稍后重试";
            }
        }
        return "服务异常，请重试";
    }
}

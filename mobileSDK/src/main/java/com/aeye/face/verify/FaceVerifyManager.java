package com.aeye.face.verify;

import android.text.TextUtils;
import android.util.Log;

import com.aeye.face.AEFaceSdk;
import com.aeye.face.api.FaceApiService;
import com.aeye.face.api.model.AuthStatusResult;
import com.aeye.face.api.model.FaceIdentResult;

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
 * 活体完成后的人脸核验：提交 faceIdent 后轮询 {@code /qrCode/authStatus}，
 * 以查询结果为最终通过/未通过。
 */
public final class FaceVerifyManager {

    private static final String TAG = "FaceVerifyManager";

    /**
     * 含建连、上传大图（炫彩多图）、读响应、以及提交成功后的 authStatus 轮询。
     * 略大于 {@link FaceApiService} faceIdent 的 HTTP 超时，避免过早 cancel 打断上传。
     */
    private static final long VERIFY_TIMEOUT_MS = 45_000L;
    /** status=3 时 1 秒轮询一次，最多 5 次 */
    private static final int AUTH_STATUS_MAX_TRIES = 5;
    private static final long AUTH_STATUS_INTERVAL_MS = 1_000L;

    public interface Callback {
        void onPassed(FaceIdentResult result);

        void onFailed(String message);
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
                    Log.d(TAG, "faceIdent ok, start authStatus poll");
                    dispatchFinalFromAuthStatus(finished, callback, result);
                } else if (finished.compareAndSet(false, true)) {
                    callback.onFailed("核验失败");
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
     * faceIdent 提交成功后以 {@code /qrCode/authStatus} 为最终结果：
     * status=3 间隔 1 秒再查，最多 5 次；仍为 3 则忙碌失败；status=5 通过；其余未通过。
     */
    private static void dispatchFinalFromAuthStatus(AtomicBoolean finished, Callback callback,
                                                    FaceIdentResult identResult) {
        Log.d(TAG, "poll authStatus authRecordId=" + FaceVerifySession.getAuthRecordId());
        try {
            AuthStatusResult auth = pollAuthStatus();
            if (!finished.compareAndSet(false, true)) {
                return;
            }
            if (auth.isPassed()) {
                callback.onPassed(identResult);
            } else {
                callback.onFailed(auth.displayFailMessage());
            }
        } catch (Exception e) {
            if (AEFaceSdk.isUseMockOnError()) {
                Log.w(TAG, "authStatus failed, fallback mock pass. cause="
                        + e.getClass().getSimpleName() + ": " + e.getMessage());
                if (finished.compareAndSet(false, true)) {
                    callback.onPassed(identResult);
                }
                return;
            }
            dispatchError(finished, callback, e);
        }
    }

    private static AuthStatusResult pollAuthStatus() throws Exception {
        String authRecordId = FaceVerifySession.getAuthRecordId();
        if (TextUtils.isEmpty(authRecordId)) {
            throw new IllegalArgumentException("authRecordId 为空");
        }
        AuthStatusResult last = null;
        Exception lastError = null;
        for (int i = 0; i < AUTH_STATUS_MAX_TRIES; i++) {
            if (i > 0) {
                Thread.sleep(AUTH_STATUS_INTERVAL_MS);
            }
            try {
                last = FaceApiService.queryAuthStatus(AEFaceSdk.getApiBaseUrl(), authRecordId);
                lastError = null;
                if (last.isVerifying()) {
                    continue;
                }
                return last;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw e;
            } catch (Exception e) {
                lastError = e;
                Log.w(TAG, "authStatus try " + (i + 1) + "/" + AUTH_STATUS_MAX_TRIES
                        + " failed: " + e.getMessage());
            }
        }
        if (last != null && last.isVerifying()) {
            return AuthStatusResult.busy();
        }
        if (lastError != null) {
            throw lastError;
        }
        return AuthStatusResult.busy();
    }

    private static void dispatchError(AtomicBoolean finished, Callback callback, Throwable error) {
        if (finished.get()) {
            return;
        }
        if (AEFaceSdk.isUseMockOnError()) {
            try {
                Log.w(TAG, "faceIdent failed, mock pass then poll authStatus. cause="
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
            return "核验失败";
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
            if (!msg.trim().isEmpty()) {
                return msg;
            }
        }
        return "核验失败";
    }
}

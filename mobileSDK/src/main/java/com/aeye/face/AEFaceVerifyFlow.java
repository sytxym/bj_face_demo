package com.aeye.face;

import android.app.Activity;
import android.os.Bundle;

import com.aeye.face.config.FaceActionConfigManager;
import com.aeye.face.config.FaceActionConfigRepository;
import com.aeye.face.config.FaceSdkHostParamBuilder;
import com.aeye.face.confirm.InfoConfirmManager;
import com.aeye.face.confirm.InfoConfirmRepository;
import com.aeye.face.verify.FaceVerifySession;

/**
 * 人脸核验统一入口：宿主只需传入 businessCode、userId，SDK 内部完成配置拉取、参数设置与预览页跳转。
 * <p>
 * 使用前调用 {@link AEFaceSdk#init(String)} 配置后台根地址。
 * </p>
 */
public final class AEFaceVerifyFlow {

    public interface Callback {
        /** 预览确认页已打开，用户点击「开始核验」后 SDK 自动进入活体 */
        void onPreviewOpened();

        void onError(String message);
    }

    private AEFaceVerifyFlow() {
    }

    /**
     * @param businessCode         动作活体业务码，如 REGISTER
     * @param userId               实名预览用户标识
     * @param hostHomeActivityClass 活体结束后返回的 Activity 全类名
     * @param listener             活体过程回调
     */
    public static void start(final Activity activity,
                             final String businessCode,
                             final String userId,
                             final String hostHomeActivityClass,
                             final AEFaceInterface listener,
                             final Callback callback) {
        start(activity, businessCode, userId, null, hostHomeActivityClass, listener, callback);
    }

    /**
     * @param authRecordId 扫码场景传入 authIdentRecordId；宿主直启可传 null（SDK 使用演示默认值）
     */
    public static void start(final Activity activity,
                             final String businessCode,
                             final String userId,
                             final String authRecordId,
                             final String hostHomeActivityClass,
                             final AEFaceInterface listener,
                             final Callback callback) {
        if (activity == null || activity.isFinishing()) {
            return;
        }
        FaceVerifySession.begin(userId, authRecordId, businessCode);
        try {
            AEFaceSdk.ensureInitialized();
        } catch (IllegalStateException e) {
            if (callback != null) {
                callback.onError(e.getMessage());
            }
            return;
        }

        FaceActionConfigManager.fetch(businessCode, new FaceActionConfigRepository.FetchCallback() {
            @Override
            public void onSuccess(com.aeye.face.config.FaceActionConfig config, boolean fromRemote) {
                if (!prepareSdk(activity, hostHomeActivityClass, listener, callback)) {
                    return;
                }
                InfoConfirmManager.fetchAndOpen(activity, userId,
                        new InfoConfirmRepository.FetchCallback() {
                            @Override
                            public void onSuccess(
                                    com.aeye.face.confirm.InfoConfirmPayload payload,
                                    boolean previewFromRemote) {
                                if (callback != null) {
                                    callback.onPreviewOpened();
                                }
                            }

                            @Override
                            public void onError(String message) {
                                if (callback != null) {
                                    callback.onError(message != null
                                            ? message : "获取预览信息失败");
                                }
                            }
                        });
            }

            @Override
            public void onError(String message) {
                if (callback != null) {
                    callback.onError(message != null ? message : "获取动作配置失败");
                }
            }
        });
    }

    private static boolean prepareSdk(Activity activity, String hostHomeActivityClass,
                                      AEFaceInterface listener, Callback callback) {
        if (!AEFacePack.getInstance().AEYE_EnvCheck(activity, 200 * 1024 * 1024)) {
            if (callback != null) {
                callback.onError("内存不足，无法启动活体");
            }
            return false;
        }
        AEFacePack.getInstance().AEYE_Init(activity);

        Bundle paras = FaceSdkHostParamBuilder.buildBase(hostHomeActivityClass, true);
        FaceActionConfigManager.applyCachedToSdkBundle(paras);

        AEFacePack.getInstance().AEYE_SetListener(listener);
        AEFacePack.getInstance().AEYE_SetParameter(paras);
        return true;
    }
}

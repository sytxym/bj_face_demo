package com.aeye.face;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;

import com.aeye.face.callback.FaceUniResultCodes;
import com.aeye.face.callback.FaceUniResultMapper;
import com.aeye.face.config.FaceActionConfigDefaults;
import com.aeye.face.config.FaceActionConfigManager;
import com.aeye.face.config.FaceActionConfigRepository;
import com.aeye.face.config.FaceActionOptions;
import com.aeye.face.config.FaceSdkHostParamBuilder;
import com.aeye.face.confirm.InfoConfirmManager;
import com.aeye.face.confirm.InfoConfirmRepository;
import com.aeye.face.verify.FaceVerifySession;
import com.aeye.face.verify.QrInsertRecordManager;

/**
 * 人脸核验统一入口：宿主只需传入 businessCode、userId，SDK 内部完成配置拉取、参数设置与预览页跳转。
 * <p>
 * 使用前调用 {@link AEFaceSdk#init(String)} 配置后台根地址。
 * </p>
 */
public final class AEFaceVerifyFlow {

    public interface Callback {
        /**
         * 预览确认页已打开，用户点击「开始核验」后 SDK 自动进入活体
         */
        default void onPreviewOpened() {

        }

        /** 流程前置失败（配置/预览/insertRecord 等），原生宿主使用 */
        void onError(String message);

        /**
         * UniApp 统一结果（流程未进入活体时）。
         * 格式与活体结束 {@link AEFaceInterface#onUniFinish(String)} 一致。
         */
        default void onUniResult(int code, String message) {
        }
    }

    private AEFaceVerifyFlow() {
    }

    /**
     * @param businessCode         业务场景编码（business_config.business_code），如 01、08、12
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
     * @param authRecordId 扫码场景传入 authIdentRecordId；宿主直启传 null，SDK 调用 insertRecord 创建
     */
    public static void start(final Activity activity,
                             final String businessCode,
                             final String userId,
                             final String authRecordId,
                             final String hostHomeActivityClass,
                             final AEFaceInterface listener,
                             final Callback callback) {
        if (activity == null || activity.isFinishing()) {
            notifyFlowError(listener, callback, FaceUniResultCodes.NO_ACTIVITY,
                    FaceUniResultCodes.MSG_NO_ACTIVITY);
            return;
        }
        if (TextUtils.isEmpty(businessCode)) {
            notifyFlowError(listener, callback, FaceUniResultCodes.MISSING_PARAMS,
                    FaceUniResultCodes.MSG_MISSING_PARAMS);
            return;
        }
        boolean registerScene = FaceActionConfigDefaults.isRegisterScene(businessCode);
        if (!registerScene && TextUtils.isEmpty(userId)) {
            notifyFlowError(listener, callback, FaceUniResultCodes.MISSING_PARAMS,
                    FaceUniResultCodes.MSG_MISSING_PARAMS);
            return;
        }
        FaceVerifySession.begin(userId, authRecordId, businessCode);
        try {
            AEFaceSdk.ensureInitialized();
        } catch (IllegalStateException e) {
            notifyFlowError(listener, callback, FaceUniResultCodes.MISSING_PARAMS,
                    e.getMessage() != null ? e.getMessage() : FaceUniResultCodes.MSG_MISSING_PARAMS);
            return;
        }

        FaceActionConfigManager.fetch(businessCode, new FaceActionConfigRepository.FetchCallback() {
            @Override
            public void onSuccess(com.aeye.face.config.FaceActionConfig config, boolean fromRemote) {
                if (!prepareSdk(activity, hostHomeActivityClass, listener, callback)) {
                    return;
                }
                InfoConfirmManager.fetch(userId, new InfoConfirmRepository.FetchCallback() {
                    @Override
                    public void onSuccess(
                            com.aeye.face.confirm.InfoConfirmPayload payload,
                            boolean previewFromRemote) {
                        Runnable openPreview = () -> {
                            InfoConfirmManager.open(activity, payload);
                            if (callback != null) {
                                callback.onPreviewOpened();
                            }
                        };
                        if (FaceVerifySession.isAuthRecordIdFromHost()) {
                            openPreview.run();
                            return;
                        }
                        QrInsertRecordManager.insert(activity, new QrInsertRecordManager.Callback() {
                            @Override
                            public void onSuccess(
                                    com.aeye.face.api.model.QrInsertRecordResult result) {
                                openPreview.run();
                            }

                            @Override
                            public void onError(String message) {
                                notifyFlowError(listener, callback, FaceUniResultCodes.AUTH_FAILED,
                                        message != null ? message : "新增认证记录失败");
                            }
                        });
                    }

                    @Override
                    public void onError(String message) {
                        notifyFlowError(listener, callback, FaceUniResultCodes.AUTH_FAILED,
                                message != null ? message : "获取预览信息失败");
                    }
                });
            }

            @Override
            public void onError(String message) {
                notifyFlowError(listener, callback, FaceUniResultCodes.AUTH_FAILED,
                        message != null ? message : "获取动作配置失败");
            }
        });
    }

    /**
     * 本地核验模式：仅做本地活体检测，<b>不调用任何我方后台接口</b>
     * （动作配置、信息预览、insertRecord、人脸核验、日志上报、二维码状态均跳过）。
     * 活体完成后直接把结果与人脸图片数组通过 {@link AEFaceInterface#onFinish} 回调给宿主，
     * 由宿主自行对接第三方接口。
     * <p>
     * 与 {@link #start} 不同：无需 {@link AEFaceSdk#init(String)}，不展示信息预览页，
     * 动作配置由宿主通过 {@link FaceActionOptions} 自定义。
     * </p>
     *
     * @param activity              当前 Activity
     * @param options               动作活体配置（为 null 时使用 {@link FaceActionOptions#defaults()}）
     * @param hostHomeActivityClass 活体结束后「其他核验方式」返回的 Activity 全类名
     * @param listener              活体过程/结束回调
     * @param callback              流程前置失败回调（可为 null）
     */
    public static void startLocal(final Activity activity,
                                  final FaceActionOptions options,
                                  final String hostHomeActivityClass,
                                  final AEFaceInterface listener,
                                  final Callback callback) {
        if (activity == null || activity.isFinishing()) {
            notifyFlowError(listener, callback, FaceUniResultCodes.NO_ACTIVITY,
                    FaceUniResultCodes.MSG_NO_ACTIVITY);
            return;
        }
        FaceVerifySession.begin(null, null, null, true);

        if (!AEFacePack.getInstance().AEYE_EnvCheck(activity, 200 * 1024 * 1024)) {
            notifyFlowError(listener, callback, FaceUniResultCodes.AUTH_FAILED,
                    "内存不足，无法启动活体");
            return;
        }
        AEFacePack.getInstance().AEYE_Init(activity);

        Bundle paras = FaceSdkHostParamBuilder.buildBase(hostHomeActivityClass, true);
        FaceActionOptions effective = options != null ? options : FaceActionOptions.defaults();
        effective.applyToBundle(paras);

        AEFacePack.getInstance().AEYE_SetListener(listener);
        AEFacePack.getInstance().AEYE_SetParameter(paras);

        AEFacePack.getInstance().AEYE_BeginRecog(activity);
        if (callback != null) {
            callback.onPreviewOpened();
        }
    }

    private static boolean prepareSdk(Activity activity, String hostHomeActivityClass,
                                      AEFaceInterface listener, Callback callback) {
        if (!AEFacePack.getInstance().AEYE_EnvCheck(activity, 200 * 1024 * 1024)) {
            notifyFlowError(listener, callback, FaceUniResultCodes.AUTH_FAILED,
                    "内存不足，无法启动活体");
            return false;
        }
        AEFacePack.getInstance().AEYE_Init(activity);

        Bundle paras = FaceSdkHostParamBuilder.buildBase(hostHomeActivityClass, true);
        FaceActionConfigManager.applyCachedToSdkBundle(paras);

        AEFacePack.getInstance().AEYE_SetListener(listener);
        AEFacePack.getInstance().AEYE_SetParameter(paras);
        return true;
    }

    private static void notifyFlowError(AEFaceInterface listener, Callback callback,
                                        int uniCode, String detailMessage) {
        String uniMessage = FaceUniResultMapper.defaultMessage(uniCode);
        if (callback != null) {
            callback.onError(TextUtils.isEmpty(detailMessage) ? uniMessage : detailMessage);
            callback.onUniResult(uniCode, uniMessage);
        }
        if (listener != null) {
            listener.onUniFinish(FaceUniResultMapper.flowErrorToUniJson(uniCode, uniMessage).toString());
        }
    }
}

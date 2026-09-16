package com.aeye.face;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;

import com.aeye.face.callback.FaceUniResultCodes;
import com.aeye.face.callback.FaceUniResultMapper;
import com.aeye.face.config.FaceActionConfig;
import com.aeye.face.config.FaceActionConfigManager;
import com.aeye.face.config.FaceActionConfigRepository;
import com.aeye.face.config.FaceActionConfigSdkMapper;
import com.aeye.face.config.FaceActionOptions;
import com.aeye.face.config.FaceSdkHostParamBuilder;
import com.aeye.face.confirm.InfoConfirmManager;
import com.aeye.face.confirm.InfoConfirmPayload;
import com.aeye.face.callback.AEFaceCallbackHelper;
import com.aeye.face.uitls.DeviceRootGuard;
import com.aeye.face.uitls.FacePermissionRequester;
import com.aeye.face.uitls.UsbDeveloperModeGuard;
import com.aeye.face.verify.FaceUserInfo;
import com.aeye.face.verify.FaceVerifySession;
import com.aeye.face.verify.QrInsertRecordManager;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 人脸核验统一入口。传入参数分两类分别管理：
 * <ul>
 *   <li><b>用户基本信息</b>（{@link FaceUserInfo}：certName/certType/certNo/country/userId/busId）：
 *       优先由外部业务 App 传入；未传时用活体配置接口返回的 {@code userInfo} 填确认页；</li>
 *   <li><b>SDK 配置信息</b>（活体检测方式、动作配置等）：
 *       在线核验（{@link #start}）使用配置接口返回的字段；
 *       本地核验（{@link #startLocal}）由外部业务 App 通过 {@link FaceActionOptions} 传入，
 *       参数名称与数据格式沿用活体配置接口。</li>
 * </ul>
 * <p>
 * 在线核验使用前调用 {@link AEFaceSdk#init(String)} 配置后台根地址。
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
         * SDK 即将拉起系统权限对话框（当前仅相机权限）。
         * 宿主可在此收起「拉取活体配置」等 loading，避免用户误以为配置接口正在与授权同时进行。
         * <p>与 {@link #onError} 互斥：调用了本回调后，若最终授权成功会走 {@link #onPreviewOpened}，
         * 拒绝则走 {@link #onError}。</p>
         */
        default void onPermissionRequesting() {
        }

        /**
         * 系统权限对话框的结果。授权通过时，SDK 会自动继续核验流程，
         * 宿主可在此重新展示 loading 直到 {@link #onPreviewOpened}。
         *
         * @param granted true：全部权限已授予；false：存在被拒（后续会收到 {@link #onError}）
         */
        default void onPermissionResult(boolean granted) {
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
     * @param authRecordId 扫码或直启由宿主传入；非空则跳过 insertRecord，并上报任务状态（含 status=1）。
     *                     直启未传时 SDK 调用 insertRecord 创建，此时不上报 updateRecord。
     */
    public static void start(final Activity activity,
                             final String businessCode,
                             final String userId,
                             final String authRecordId,
                             final String hostHomeActivityClass,
                             final AEFaceInterface listener,
                             final Callback callback) {
        start(activity, businessCode, userId, authRecordId, null,
                hostHomeActivityClass, listener, callback);
    }

    /**
     * 在线核验推荐入口：外部业务 App 传入用户基本信息。
     * <p>确认页四项身份字段优先用 {@code userInfo}；缺省再用活体配置接口返回的 {@code userInfo}。
     * SDK 配置（活体方式、动作等）使用配置接口 {@code actionConfig}。</p>
     *
     * @param userInfo           外部业务 App 传入的用户基本信息（userId 可选）
     * @param authRecordId       扫码场景传入 authIdentRecordId；宿主直启传 null 或由业务传入已有 ID
     * @param detectTypeOverride 覆盖后台配置的 detectType（如 LIGHT / MOTION_LIGHT）；为 null 时以后台配置为准
     */
    public static void start(final Activity activity,
                             final String businessCode,
                             final FaceUserInfo userInfo,
                             final String authRecordId,
                             final String detectTypeOverride,
                             final String hostHomeActivityClass,
                             final AEFaceInterface listener,
                             final Callback callback) {
        start(activity, businessCode, userInfo, authRecordId, detectTypeOverride, false,
                hostHomeActivityClass, listener, callback);
    }

    /**
     * 在线核验推荐入口（可区分扫码 / 直启）。
     *
     * @param fromQrScan true=扫码认证；false=直启。
     *                   直启若传入 authRecordId，同样上报任务状态（含 status=1 进入确认页）。
     */
    public static void start(final Activity activity,
                             final String businessCode,
                             final FaceUserInfo userInfo,
                             final String authRecordId,
                             final String detectTypeOverride,
                             final boolean fromQrScan,
                             final String hostHomeActivityClass,
                             final AEFaceInterface listener,
                             final Callback callback) {
        startInternal(activity, businessCode,
                userInfo != null ? userInfo.getUserId() : null,
                userInfo, authRecordId, detectTypeOverride, fromQrScan,
                hostHomeActivityClass, listener, callback);
    }

    /**
     * @param detectTypeOverride 覆盖后台配置的 detectType（如 LIGHT / MOTION_LIGHT）；
     *                           为 null 时完全以后台配置为准。炫彩与动作走同一套后台流程
     *                           （配置、insertRecord、faceIdent、日志、二维码状态）。
     */
    public static void start(final Activity activity,
                             final String businessCode,
                             final String userId,
                             final String authRecordId,
                             final String detectTypeOverride,
                             final String hostHomeActivityClass,
                             final AEFaceInterface listener,
                             final Callback callback) {
        startInternal(activity, businessCode, userId, null, authRecordId, detectTypeOverride, false,
                hostHomeActivityClass, listener, callback);
    }

    private static void startInternal(final Activity activity,
                                      final String businessCode,
                                      final String userId,
                                      final FaceUserInfo userInfo,
                                      final String authRecordId,
                                      final String detectTypeOverride,
                                      final boolean fromQrScan,
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
        // userId 非必传：确认页身份优先业务 App，缺省用活体配置接口 userInfo

        // 环境预检：授权后自动继续，无需宿主再次点击按钮
        if (!ensureEnvironmentReady(activity, listener, callback, new Runnable() {
            @Override
            public void run() {
                startInternal(activity, businessCode, userId, userInfo, authRecordId,
                        detectTypeOverride, fromQrScan, hostHomeActivityClass, listener, callback);
            }
        })) {
            return;
        }

        FaceVerifySession.begin(userId, authRecordId, businessCode, false, detectTypeOverride, fromQrScan);
        FaceVerifySession.setUserInfo(userInfo);
        try {
            AEFaceSdk.ensureInitialized();
        } catch (IllegalStateException e) {
            notifyFlowError(listener, callback, FaceUniResultCodes.MISSING_PARAMS,
                    e.getMessage() != null ? e.getMessage() : FaceUniResultCodes.MSG_MISSING_PARAMS);
            return;
        }

        // 活体配置必填 authRecordId：正式由业务 App 传入；调试未传时先 insertRecord
        if (FaceVerifySession.isAuthRecordIdFromHost()) {
            fetchActionConfigThenOpenPreview(activity, detectTypeOverride,
                    hostHomeActivityClass, listener, callback);
            return;
        }
        QrInsertRecordManager.insert(activity, new QrInsertRecordManager.Callback() {
            @Override
            public void onSuccess(com.aeye.face.api.model.QrInsertRecordResult result) {
                if (activity == null || activity.isFinishing()) {
                    return;
                }
                fetchActionConfigThenOpenPreview(activity, detectTypeOverride,
                        hostHomeActivityClass, listener, callback);
            }

            @Override
            public void onError(String message) {
                notifyFlowError(listener, callback, FaceUniResultCodes.AUTH_FAILED,
                        message != null ? message : "新增认证记录失败");
            }
        });
    }

    private static void fetchActionConfigThenOpenPreview(final Activity activity,
                                                         final String detectTypeOverride,
                                                         final String hostHomeActivityClass,
                                                         final AEFaceInterface listener,
                                                         final Callback callback) {
        FaceActionConfigManager.fetch(FaceVerifySession.getBusinessCode(),
                new FaceActionConfigRepository.FetchCallback() {
            @Override
            public void onSuccess(FaceActionConfig config, boolean fromRemote) {
                if (activity == null || activity.isFinishing()) {
                    return;
                }
                if (!TextUtils.isEmpty(detectTypeOverride) && config != null) {
                    config.setDetectType(detectTypeOverride);
                }
                FaceVerifySession.setUserInfo(FaceUserInfo.mergePreferHost(
                        FaceVerifySession.getUserInfo(),
                        config != null ? config.getUserInfo() : null));
                if (!prepareSdk(activity, hostHomeActivityClass, listener, callback, config)) {
                    return;
                }
                InfoConfirmPayload payload = buildConfirmPayload(FaceVerifySession.getUserInfo());
                InfoConfirmManager.open(activity, payload);
                if (callback != null) {
                    callback.onPreviewOpened();
                }
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
        startLocal(activity, null, options, hostHomeActivityClass, listener, callback);
    }

    /**
     * 本地核验入口（带用户基本信息）：SDK 配置由外部通过 {@link FaceActionOptions} 传入
     * （参数名称与数据格式沿用活体配置接口，detectType 对应活体类型、enableXxx/actionCount 对应动作配置），
     * 不调用任何我方后台接口。{@code userInfo} 仅保存在会话中供宿主/日志使用，
     * 本地模式下不会提交给 faceIdent。
     *
     * @param userInfo 外部业务 App 传入的用户基本信息（可为 null）
     */
    public static void startLocal(final Activity activity,
                                  final FaceUserInfo userInfo,
                                  final FaceActionOptions options,
                                  final String hostHomeActivityClass,
                                  final AEFaceInterface listener,
                                  final Callback callback) {
        if (activity == null || activity.isFinishing()) {
            notifyFlowError(listener, callback, FaceUniResultCodes.NO_ACTIVITY,
                    FaceUniResultCodes.MSG_NO_ACTIVITY);
            return;
        }

        // 环境预检：授权后自动继续，无需宿主再次点击按钮
        if (!ensureEnvironmentReady(activity, listener, callback, new Runnable() {
            @Override
            public void run() {
                startLocal(activity, userInfo, options, hostHomeActivityClass, listener, callback);
            }
        })) {
            return;
        }

        FaceVerifySession.begin(userInfo != null ? userInfo.getUserId() : null, null, null, true);
        FaceVerifySession.setUserInfo(userInfo);
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

    /**
     * 确认页四项：国家地区、姓名、证件类型、证件号码。
     * 优先业务 App 传入，缺省字段已在 {@link FaceUserInfo#mergePreferHost} 用配置接口补齐。
     */
    private static InfoConfirmPayload buildConfirmPayload(FaceUserInfo info) {
        JSONObject data = new JSONObject();
        try {
            if (info != null) {
                data.putOpt("name", info.getCertName());
                data.putOpt("nation", info.getCountry());
                data.putOpt("certType", info.getCertType());
                data.putOpt("certNo", info.getCertNo());
                data.putOpt("userId", info.getUserId());
            }
        } catch (JSONException ignored) {
        }
        return InfoConfirmPayload.fromApiData(data);
    }

    private static boolean prepareSdk(Activity activity, String hostHomeActivityClass,
                                      AEFaceInterface listener, Callback callback,
                                      FaceActionConfig config) {
        // 环境检查已在 start()/startLocal() 入口完成，这里做一次内存兜底
        int env = AEFacePack.getInstance().AEYE_EnvCheckSilent(activity, 200 * 1024 * 1024);
        if (env == AEFacePack.ENV_CHECK_LOW_MEMORY) {
            notifyFlowError(listener, callback, FaceUniResultCodes.AUTH_FAILED,
                    "内存不足，无法启动活体");
            return false;
        }
        AEFacePack.getInstance().AEYE_Init(activity);

        Bundle paras = FaceSdkHostParamBuilder.buildBase(hostHomeActivityClass, true);
        FaceActionConfig effective = config != null ? config : FaceActionConfigManager.getCached();
        // Demo/联调可覆盖 detectType；不要 new 一份默认 actionCount=3 的配置把后台动作数冲掉
        String override = FaceVerifySession.getDetectTypeOverride();
        if (effective != null && !TextUtils.isEmpty(override)) {
            effective.setDetectType(override);
        }
        if (effective != null) {
            FaceActionConfigSdkMapper.applyToBundle(paras, effective);
        }

        AEFacePack.getInstance().AEYE_SetListener(listener);
        AEFacePack.getInstance().AEYE_SetParameter(paras);
        return true;
    }

    /**
     * 环境预检（入口调用）：
     * <ul>
     *   <li>OK：返回 true，调用方继续后续流程</li>
     *   <li>内存不足：走完整失败流程，返回 false</li>
     *   <li>权限缺失：拉起系统对话框；
     *     <ul>
     *       <li>用户授权 → 自动执行 {@code resume}（宿主无需重新点击按钮）</li>
     *       <li>用户拒绝 → 走完整失败流程，回调「未授予相机权限」</li>
     *     </ul>
     *     返回 false，当前调用直接返回，避免同一流程被并发执行两次。
     *   </li>
     * </ul>
     */
    private static boolean ensureEnvironmentReady(final Activity activity,
                                                  final AEFaceInterface listener,
                                                  final Callback callback,
                                                  final Runnable resume) {
        int env = AEFacePack.getInstance().AEYE_EnvCheckSilent(activity, 200 * 1024 * 1024);
        if (env == AEFacePack.ENV_CHECK_OK) {
            return !blockIfDeviceUnsafe(activity, listener, callback);
        }
        if (env == AEFacePack.ENV_CHECK_LOW_MEMORY) {
            notifyFlowError(listener, callback, FaceUniResultCodes.AUTH_FAILED,
                    "内存不足，无法启动活体");
            return false;
        }
        // ENV_CHECK_PERMISSION_MISSING：先通知宿主收起 loading，再拉起权限对话框；授权后自动继续
        if (callback != null) {
            callback.onPermissionRequesting();
        }
        FacePermissionRequester.requestIfNeeded(activity, new FacePermissionRequester.Callback() {
            @Override
            public void onResult(boolean granted) {
                if (callback != null) {
                    callback.onPermissionResult(granted);
                }
                if (activity.isFinishing()) {
                    notifyFlowError(listener, callback, FaceUniResultCodes.NO_ACTIVITY,
                            FaceUniResultCodes.MSG_NO_ACTIVITY);
                    return;
                }
                if (granted) {
                    resume.run();
                } else {
                    notifyCameraPermissionDenied(listener, callback);
                }
            }
        });
        return false;
    }

    /**
     * USB 调试或 Root/越狱时弹框并结束流程。
     *
     * @return true 表示已拦截，调用方应停止后续流程
     */
    private static boolean blockIfDeviceUnsafe(final Activity activity,
                                               final AEFaceInterface listener,
                                               final Callback callback) {
        final boolean usb = UsbDeveloperModeGuard.shouldBlock(activity);
        final boolean root = !usb && DeviceRootGuard.shouldBlock(activity);
        if (!usb && !root) {
            return false;
        }
        if (callback != null) {
            callback.onPermissionRequesting();
        }
        final String detail = activity.getString(usb
                ? com.sdk.core.R.string.aeye_usb_debug_block_message
                : com.sdk.core.R.string.face_fail_device_unsafe_detail);
        Runnable exit = () -> {
            AEFacePack.getInstance().finishAllFaceFlowActivities();
            if (listener != null) {
                AEFaceCallbackHelper.dispatchFinish(
                        listener, AEFacePack.ERROR_DANGER_DEVICE, null, detail, false,
                        FaceUniResultCodes.RESULT_DEVICE_UNSAFE);
            }
        };
        if (usb) {
            UsbDeveloperModeGuard.showBlockDialogAndExit(activity, exit::run);
        } else {
            DeviceRootGuard.showBlockDialogAndExit(activity, exit::run);
        }
        return true;
    }

    private static void notifyFlowError(AEFaceInterface listener, Callback callback,
                                        int flowCode, String detailMessage) {
        if (callback != null) {
            String message = TextUtils.isEmpty(detailMessage)
                    ? FaceUniResultMapper.defaultMessage(flowCode)
                    : detailMessage;
            callback.onError(message);
        }
    }

    /** 第 10 项：拒绝相机权限，经 onFinish 回调 {@code 0414013}。 */
    private static void notifyCameraPermissionDenied(AEFaceInterface listener, Callback callback) {
        String detail = "未授予相机权限";
        if (listener != null) {
            AEFaceCallbackHelper.dispatchFinish(
                    listener, AEFacePack.ERROR_CAMERA, null, detail, false,
                    FaceUniResultCodes.RESULT_CAMERA_NO_PERMISSION);
            return;
        }
        notifyFlowError(listener, callback, FaceUniResultCodes.AUTH_FAILED, detail);
    }
}

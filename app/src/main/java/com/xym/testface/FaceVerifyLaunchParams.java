package com.xym.testface;

import android.text.TextUtils;

import com.aeye.face.config.FaceActionConfig;
import com.aeye.face.config.FaceActionOptions;
import com.aeye.face.verify.FaceUserInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 宿主侧启动参数解析：把 H5 / RN 等业务端传入的 JSON 解析为 SDK 所需的类型安全对象，分两类管理：
 * <ul>
 *   <li><b>用户基本信息</b>：certName / certType / certNo / country / userId / busId，
 *       解析为 {@link FaceUserInfo}，在线核验时透传给 {@code /assistant/faceIdent}；</li>
 *   <li><b>SDK 配置信息</b>：useType / liveType / actionType，
 *       useType=0 在线核验（SDK 配置以配置接口返回为准），
 *       useType=1 本地核验（不调用我方后台，配置由传入参数决定，
 *       可通过 {@link #toLocalActionOptions()} 转为 {@link FaceActionOptions}）。</li>
 * </ul>
 *
 * <p><b>为什么放在宿主 App 而不是 SDK：</b>H5/RN 与宿主之间的 JSON 属于业务接入协议，
 * 字段名与结构可能随业务端调整；协议变化时只需修改本类的 {@link #fromJson} 映射逻辑，
 * SDK（入参为 {@link FaceUserInfo} / {@link FaceActionOptions}）无需改动。</p>
 *
 * <p>当前 JSON 示例（未传字段使用默认值）：</p>
 * <pre>{@code
 * {
 *   "certName": "张三",
 *   "certType": "1",
 *   "certNo": "430622199001011234",
 *   "country": "中国",
 *   "userId": "demoUser001",
 *   "busId": "demoBus001",
 *   "businessCode": "12",
 *   "authRecordId": "123",
 *   "useType": 1,
 *   "liveType": 0,
 *   "actionType": [0, 1, 1, 1, 0]
 * }
 * }</pre>
 *
 * <p>默认值：useType=0（在线核验）、liveType=0（动作活体）、
 * actionType 未传时默认动作为 抬头/摇头/眨眼。</p>
 */
public final class FaceVerifyLaunchParams {

    /** useType：在线核验（调用我方后台，SDK 配置以配置接口为准），默认值 */
    public static final int USE_TYPE_ONLINE = 0;
    /** useType：本地核验（不调用我方后台，SDK 配置由传入参数决定） */
    public static final int USE_TYPE_LOCAL = 1;

    /** liveType：动作活体（默认值，仅本地核验时生效） */
    public static final int LIVE_TYPE_MOTION = 0;
    /** liveType：静默活体（仅本地核验时生效；本地不支持炫彩活体） */
    public static final int LIVE_TYPE_SILENT = 1;

    /** actionType 数组下标含义：[抬头, 低头, 摇头, 眨眼, 张嘴] */
    private static final int ACTION_SLOT_COUNT = 5;

    private final FaceUserInfo userInfo;
    private final String businessCode;
    private final String authRecordId;
    private final int useType;
    private final int liveType;
    /** 长度 5：[抬头,低头,摇头,眨眼,张嘴]，1=需要该动作；null 表示未传（使用默认动作） */
    private final int[] actionType;

    private FaceVerifyLaunchParams(FaceUserInfo userInfo, String businessCode,
                                   String authRecordId, int useType, int liveType,
                                   int[] actionType) {
        this.userInfo = userInfo;
        this.businessCode = businessCode;
        this.authRecordId = authRecordId;
        this.useType = useType;
        this.liveType = liveType;
        this.actionType = actionType;
    }

    /**
     * 解析业务端传入的启动 JSON；{@code json} 为空时全部字段取默认值。
     * H5/RN 协议字段有变化时，只需调整本方法内的字段映射。
     *
     * @throws JSONException            JSON 格式非法
     * @throws IllegalArgumentException 本地动作活体 actionType 传入但没有任何值为 1
     */
    public static FaceVerifyLaunchParams fromJson(String json) throws JSONException {
        JSONObject root = new JSONObject(TextUtils.isEmpty(json) ? "{}" : json);
        FaceUserInfo userInfo = new FaceUserInfo.Builder()
                .certName(optTrimmed(root, "certName"))
                .certType(optTrimmed(root, "certType"))
                .certNo(optTrimmed(root, "certNo"))
                .country(optTrimmed(root, "country"))
                .userId(optTrimmed(root, "userId"))
                .busId(optTrimmed(root, "busId"))
                .build();
        int useType = root.optInt("useType", USE_TYPE_ONLINE);
        int liveType = root.optInt("liveType", LIVE_TYPE_MOTION);
        int[] actions = parseActionArray(root.optJSONArray("actionType"));
        if (useType == USE_TYPE_LOCAL && liveType == LIVE_TYPE_MOTION
                && actions != null && countEnabled(actions) == 0) {
            throw new IllegalArgumentException("本地动作活体 actionType 数组中必须至少有一个值为 1");
        }
        return new FaceVerifyLaunchParams(
                userInfo,
                optTrimmed(root, "businessCode"),
                optTrimmed(root, "authRecordId"),
                useType, liveType, actions);
    }

    /** 是否为本地核验（useType=1，不调用我方后台接口）。 */
    public boolean isLocalVerify() {
        return useType == USE_TYPE_LOCAL;
    }

    /**
     * 本地核验时使用：把 liveType / actionType 转为 SDK 配置
     * （参数名称与数据格式沿用活体配置接口）。
     * liveType=1 静默活体；liveType=0 动作活体，按 actionType 数组顺序执行，
     * 未传 actionType 时默认 抬头/摇头/眨眼。
     */
    public FaceActionOptions toLocalActionOptions() {
        FaceActionOptions.Builder builder = new FaceActionOptions.Builder()
                .aliveLevel(1)
                .motionTimeoutSec(10)
                .voiceEnabled(true);
        if (liveType == LIVE_TYPE_SILENT) {
            return builder.detectType(FaceActionConfig.DETECT_SILENT)
                    .actionCount(0)
                    .enableLookUp(false)
                    .enableLookDown(false)
                    .enableShakeHead(false)
                    .enableBlink(false)
                    .enableOpenMouth(false)
                    .build();
        }
        builder.detectType(FaceActionConfig.DETECT_MOTION)
                .actionType(FaceActionConfig.ACTION_SEQUENCE);
        if (actionType == null) {
            return builder.actionCount(3)
                    .enableLookUp(true)
                    .enableShakeHead(true)
                    .enableBlink(true)
                    .enableLookDown(false)
                    .enableOpenMouth(false)
                    .build();
        }
        return builder.actionCount(countEnabled(actionType))
                .enableLookUp(actionType[0] == 1)
                .enableLookDown(actionType[1] == 1)
                .enableShakeHead(actionType[2] == 1)
                .enableBlink(actionType[3] == 1)
                .enableOpenMouth(actionType[4] == 1)
                .build();
    }

    public FaceUserInfo getUserInfo() {
        return userInfo;
    }

    public String getBusinessCode() {
        return businessCode;
    }

    public String getAuthRecordId() {
        return authRecordId;
    }

    public int getUseType() {
        return useType;
    }

    public int getLiveType() {
        return liveType;
    }

    private static String optTrimmed(JSONObject json, String key) {
        String value = json.optString(key, null);
        if (value == null || "null".equals(value)) {
            return null;
        }
        value = value.trim();
        return value.isEmpty() ? null : value;
    }

    /** 兼容传入 6 位数组（如 [0,1,1,1,0,0]），只取前 5 位有效动作位。 */
    private static int[] parseActionArray(JSONArray array) {
        if (array == null) {
            return null;
        }
        int[] actions = new int[ACTION_SLOT_COUNT];
        for (int i = 0; i < ACTION_SLOT_COUNT && i < array.length(); i++) {
            actions[i] = array.optInt(i, 0) == 1 ? 1 : 0;
        }
        return actions;
    }

    private static int countEnabled(int[] actions) {
        int count = 0;
        for (int action : actions) {
            if (action == 1) {
                count++;
            }
        }
        return count;
    }
}

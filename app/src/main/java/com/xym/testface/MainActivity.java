package com.xym.testface;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.aeye.face.AEFaceInterface;
import com.aeye.face.AEFacePack;
import com.aeye.face.AEFaceVerifyFlow;
import android.text.TextUtils;

import com.aeye.face.config.FaceActionConfig;
import com.aeye.face.verify.FaceUserInfo;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

/**
 * Demo 宿主首页：提供两种进入人脸核验的入口。
 *   <li>人脸认证：使用固定 demo 用户直接走 SDK 核验流程</li>
 *   <li>扫码认证：扫二维码解析 userId 后进入同一套核验流程</li>
 * 接口请求、Mock 回退、信息预览与活体检测均由 {@link AEFaceVerifyFlow} / SDK 内部处理；
 * 宿主只需传入 businessCode、userId，并在 {@link AEFaceInterface} 中接收活体结果回调。
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener, AEFaceInterface {

    /** 动作活体业务码，与后台 business_config.business_code 一致（Demo：自然人实名认证）；JSON 未传 businessCode 时的默认值 */
    private static final String DEMO_BUSINESS_CODE = "12";

    /**
     * 模拟业务 App（H5/RN）传入的启动参数 JSON——在线核验（useType=0）。
     * 正式接入时由业务侧传入该 JSON，未传字段使用默认值；目前联调阶段写死。
     */
    private static final String DEMO_ONLINE_LAUNCH_JSON = "{"
            + "\"certName\":\"张三\","
            + "\"certType\":\"1\","
            + "\"certNo\":\"430622199001011234\","
            + "\"country\":\"中国\","
            + "\"userId\":\"demoUser001\","
            + "\"busId\":\"demoBus001\","
            + "\"businessCode\":\"" + DEMO_BUSINESS_CODE + "\","
            + "\"authRecordId\":\"" + 22222 + "\","
            + "\"useType\":" + FaceVerifyLaunchParams.USE_TYPE_ONLINE
            + "}";

    /**
     * 模拟业务 App 传入的启动参数 JSON——本地核验（useType=1，不调用我方后台）。
     * liveType=0 动作活体，actionType=[抬头,低头,摇头,眨眼,张嘴]，示例为 低头+摇头+眨眼。
     */
    private static final String DEMO_LOCAL_LAUNCH_JSON = "{"
            + "\"certName\":\"张三\","
            + "\"certType\":\"1\","
            + "\"certNo\":\"430622199001011234\","
            + "\"country\":\"中国\","
            + "\"userId\":\"demoUser001\","
            + "\"busId\":\"demoBus001\","
            + "\"useType\":" + FaceVerifyLaunchParams.USE_TYPE_LOCAL + ","
            + "\"liveType\":" + FaceVerifyLaunchParams.LIVE_TYPE_MOTION + ","
            + "\"actionType\":[0,1,1,1,0]"
            + "}";

    /** 拉取活体配置时的 loading，预览页打开或失败时关闭 */
    private ProgressDialog loadingDialog;
    /** 扫码前申请相机权限 */
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    /** 竖屏 QR 扫码（ZXing） */
    private ActivityResultLauncher<ScanOptions> scanLauncher;

    /** 活体/识别结束后要跳转的结果页 Intent，在 onFinish 中启动 */
    private Intent recogIntent = null;
    private DemoApplication mApp;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        mApp = (DemoApplication) getApplication();

        setSystemStatus();
        initScanLaunchers();

        Button btTestFace = findViewById(R.id.btTestFace);
        Button btScanAuth = findViewById(R.id.btScanAuth);
        Button btLocalFace = findViewById(R.id.btLocalFace);
        Button btLight = findViewById(R.id.btLocalLight);
        Button btMotionLight = findViewById(R.id.btLocalMotionLight);
        btTestFace.setOnClickListener(this);
        btScanAuth.setOnClickListener(this);
        btLocalFace.setOnClickListener(this);
        btLight.setOnClickListener(this);
        btMotionLight.setOnClickListener(this);
    }

    /** 注册相机权限与扫码结果的 Activity Result 回调 */
    private void initScanLaunchers() {
        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) {
                        launchScan();
                    } else {
                        Toast.makeText(this, R.string.scan_camera_denied, Toast.LENGTH_LONG).show();
                    }
                });

        scanLauncher = registerForActivityResult(new ScanContract(), result -> {
            if (result.getContents() == null) {
                Toast.makeText(this, R.string.scan_cancelled, Toast.LENGTH_SHORT).show();
                return;
            }
            handleScanResult(result.getContents());
        });
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.btTestFace) {
            startFaceVerify(DEMO_ONLINE_LAUNCH_JSON, null, null, null, false);
        } else if (id == R.id.btScanAuth) {
            startScanAuth();
        } else if (id == R.id.btLocalFace) {
            startFaceVerify(DEMO_LOCAL_LAUNCH_JSON, null, null, null, false);
        } else if (id == R.id.btLocalLight) {
            // 与动作活体同一套后台流程，仅覆盖 detectType=LIGHT
            startFaceVerify(DEMO_ONLINE_LAUNCH_JSON, null, null, FaceActionConfig.DETECT_LIGHT, false);
        } else if (id == R.id.btLocalMotionLight) {
            startFaceVerify(DEMO_ONLINE_LAUNCH_JSON, null, null, FaceActionConfig.DETECT_MOTION_LIGHT, false);
        }
    }

    /** 扫码认证入口：先检查相机权限，再打开扫码页 */
    private void startScanAuth() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchScan();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    /** 打开竖屏 QR 扫码（期望内容：{"authIdentRecordId":"...","userId":"..."}） */
    private void launchScan() {
        ScanOptions options = new ScanOptions();
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        options.setPrompt(getString(R.string.scan_prompt));
        options.setBeepEnabled(true);
        options.setBarcodeImageEnabled(false);
        options.setCaptureActivity(PortraitCaptureActivity.class);
        options.setOrientationLocked(true);
        scanLauncher.launch(options);
    }

    /** 解析扫码 JSON，用其中的 userId、authIdentRecordId 启动人脸核验（其余参数沿用启动 JSON） */
    private void handleScanResult(String qrContent) {
        try {
            ScanAuthParser.Result scanResult = ScanAuthParser.parse(qrContent);
            startFaceVerify(DEMO_ONLINE_LAUNCH_JSON,
                    scanResult.getUserId(), scanResult.getAuthIdentRecordId(), null, true);
        } catch (Exception e) {
            Toast.makeText(this, R.string.scan_parse_error, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 统一人脸核验入口：解析业务 App 传入的启动 JSON（用户基本信息 + SDK 配置），
     * 按 useType 路由到在线核验（拉配置 → 确认页 → insertRecord → 活体 → faceIdent）
     * 或本地核验（不调用我方后台，liveType/actionType 决定活体方式）。
     *
     * @param launchJson         业务 App（H5/RN）传入的启动参数 JSON；为空时全部字段用默认值
     * @param scanUserId         扫码场景覆盖 JSON 中的 userId；非扫码传 null
     * @param scanAuthRecordId   扫码场景传入的认证记录 ID；非扫码传 null
     * @param detectTypeOverride 非空时覆盖后台 detectType（联调炫彩用，仅在线核验生效）
     * @param fromQrScan         true=扫码认证；false=直启人脸
     */
    private void startFaceVerify(String launchJson, String scanUserId,
                                 String scanAuthRecordId, String detectTypeOverride,
                                 boolean fromQrScan) {
        FaceVerifyLaunchParams params;
        try {
            params = FaceVerifyLaunchParams.fromJson(launchJson);
        } catch (Exception e) {
            Toast.makeText(this, "启动参数解析失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }
        FaceUserInfo userInfo = TextUtils.isEmpty(scanUserId)
                ? params.getUserInfo()
                : withUserId(params.getUserInfo(), scanUserId);

        if (params.isLocalVerify()) {
            // 本地核验：不调用我方后台，SDK 配置由 JSON 的 liveType/actionType 决定
            AEFaceVerifyFlow.startLocal(
                    this,
                    userInfo,
                    params.toLocalActionOptions(),
                    getClass().getName(),
                    this,
                    new AEFaceVerifyFlow.Callback() {
                        @Override
                        public void onPreviewOpened() {
                            // 本地模式无预览/网络请求，活体页已直接启动
                        }

                        @Override
                        public void onError(String message) {
                            Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                        }
                    });
            return;
        }

        // 在线核验：SDK 配置以配置接口返回为准
        String businessCode = !TextUtils.isEmpty(params.getBusinessCode())
                ? params.getBusinessCode() : DEMO_BUSINESS_CODE;
        String authRecordId = !TextUtils.isEmpty(scanAuthRecordId)
                ? scanAuthRecordId : params.getAuthRecordId();
        showLoading(getString(R.string.loading_liveness_config));
        AEFaceVerifyFlow.start(
                this,
                businessCode,
                userInfo,
                authRecordId,
                detectTypeOverride,
                fromQrScan,
                getClass().getName(),
                this,
                new AEFaceVerifyFlow.Callback() {
                    @Override
                    public void onPreviewOpened() {
                        dismissLoading();
                    }

                    @Override
                    public void onError(String message) {
                        dismissLoading();
                        Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onPermissionRequesting() {
                        dismissLoading();
                    }

                    @Override
                    public void onPermissionResult(boolean granted) {
                        if (granted) {
                            showLoading(getString(R.string.loading_liveness_config));
                        }
                    }
                });
    }

    /** 扫码场景：用扫码结果中的 userId 覆盖启动 JSON 里的 userId，其余身份字段保持不变 */
    private FaceUserInfo withUserId(FaceUserInfo base, String userId) {
        FaceUserInfo.Builder builder = new FaceUserInfo.Builder().userId(userId);
        if (base != null) {
            builder.certName(base.getCertName())
                    .certType(base.getCertType())
                    .certNo(base.getCertNo())
                    .country(base.getCountry())
                    .busId(base.getBusId());
        }
        return builder.build();
    }

    private void showLoading(String message) {
        dismissLoading();
        loadingDialog = ProgressDialog.show(this, null, message);
        loadingDialog.setCancelable(false);
    }

    private void dismissLoading() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
        loadingDialog = null;
    }

    /** Edge-to-Edge：为根布局加上系统栏内边距，避免内容被状态栏/导航栏遮挡 */
    private void setSystemStatus() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    // ---------- AEFaceInterface：活体过程回调（Demo 仅 Toast 展示） ----------

    @Override
    public void onStart(int i, String s) {
        Toast.makeText(this, "开始识别", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPrompt(int i, String s) {
    }

    @Override
    public void onProcess(int i, String s) {
    }

    /**
     * 活体结束：value 为 SDK 内部码，data 为采集 JSON，
     * resultCode 为三端统一码（"0"/"0414009"…），业务端以 resultCode 为准。
     */
    @Override
    public void onFinish(int value, String data, String resultCode) {
        Log.d("terry", "onFinish: " + value + " resultCode=" + resultCode + " data" + data);
        if (value == AEFacePack.ERROR_OTHER_VERIFY
                || value == AEFacePack.ERROR_DANGER_DEVICE) {
            // 其他核验方式 / USB 调试拦截：已在 SDK 内提示并返回，不跳结果页
            dismissLoading();
            return;
        }
        recogIntent = new Intent(this, ResultAliveActivity.class);
//        FLogUtil.saveLogServer("MainActivity->onFinish:" + data);
        recogIntent.putExtra("VALUE", value);
        recogIntent.putExtra("RESULT_CODE", resultCode);
        mApp.setSnapData(data);
        recogIntent.putExtra("DATA", decodeError(value));

        startActivity(recogIntent);
    }

    /** 将 SDK 返回码转为结果页展示文案 */
    private String decodeError(int value) {
        switch (value) {
            case AEFacePack.SUCCESS:
                if (AEFacePack.getInstance().isAliveOff()) {
                    return getString(R.string.aeye_capture_success);
                }
                return getString(R.string.aeye_alive_success);
            default:
            case AEFacePack.ERROR_FAIL:
                return getString(R.string.aeye_alive_fail);
            case AEFacePack.ERROR_TIMEOUT:
                return getString(R.string.aeye_recog_timeout);
            case AEFacePack.ERROR_CANCEL:
                return getString(R.string.aeye_user_cancel);
            case AEFacePack.ERROR_CAMERA:
                return getString(R.string.aeye_camera_error);
            case  AEFacePack.ERROR_DANGER_DEVICE:
                return  getString(R.string.aeye_safetip);
        }
    }
}

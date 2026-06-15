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

    /** 动作活体业务码，与后台配置接口 businessCode 一致 */
    private static final String DEMO_BUSINESS_CODE = "REGISTER";
    /** 人脸认证按钮使用的演示用户 ID */
    private static final String DEMO_USER_ID = "demoUser001";

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
        btTestFace.setOnClickListener(this);
        btScanAuth.setOnClickListener(this);
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
            startFaceVerify(DEMO_USER_ID);
        } else if (id == R.id.btScanAuth) {
            startScanAuth();
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

    /** 解析扫码 JSON，用其中的 userId、authIdentRecordId 启动人脸核验 */
    private void handleScanResult(String qrContent) {
        try {
            ScanAuthParser.Result scanResult = ScanAuthParser.parse(qrContent);
            startFaceVerify(scanResult.getUserId(), scanResult.getAuthIdentRecordId());
        } catch (Exception e) {
            Toast.makeText(this, R.string.scan_parse_error, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 统一人脸核验入口：拉配置 → 信息预览 → 活体 → SDK 内人脸核验。
     *
     * @param userId       后台用户信息预览接口使用的用户标识
     * @param authRecordId 扫码 authIdentRecordId，非扫码传 null 使用 SDK 演示值
     */
    private void startFaceVerify(String userId) {
        startFaceVerify(userId, null);
    }

    private void startFaceVerify(String userId, String authRecordId) {
        showLoading(getString(R.string.loading_liveness_config));
        AEFaceVerifyFlow.start(
                this,
                DEMO_BUSINESS_CODE,
                userId,
                authRecordId,
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
                });
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

    /** 活体结束：value 为结果码，data 为采集 JSON（可上传后台做人脸核验） */
    @Override
    public void onFinish(int value, String data) {
        Log.d("terry", "onFinish: " + value + " " + data);
        if (value == AEFacePack.ERROR_OTHER_VERIFY) {
            // 二次核验流程由 SDK 内部处理，不在此跳转
            return;
        }
        recogIntent = new Intent(this, ResultAliveActivity.class);
//        FLogUtil.saveLogServer("MainActivity->onFinish:" + data);
        recogIntent.putExtra("VALUE", value);
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

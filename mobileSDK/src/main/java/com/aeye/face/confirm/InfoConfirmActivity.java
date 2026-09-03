package com.aeye.face.confirm;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.aeye.face.AEFacePack;
import com.aeye.face.ui.FaceImmersiveStatusBar;
import com.aeye.face.verify.QrRecordStatus;
import com.aeye.face.verify.QrRecordStatusManager;
import com.sdk.core.R;

/**
 * 实名信息确认页；数据由宿主通过 Intent 传入（宿主侧请求甲方接口后填充）。
 * 点击「开始核验」后走既有 {@link AEFacePack#AEYE_BeginRecog(android.content.Context)} 活体流程。
 */
public class InfoConfirmActivity extends Activity {

    private ProgressDialog lightLoadingDialog;
    private Button startButton;
    /** 已进入活体页，关闭确认页时不上报任务取消 */
    private boolean leavingForRecognize;
    private boolean cancelReported;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        FaceImmersiveStatusBar.install(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.aeye_activity_info_confirm);
        FaceImmersiveStatusBar.bindToolbar(this, findViewById(R.id.face_toolbar));
        FaceImmersiveStatusBar.bindBottomMargin(this, findViewById(R.id.btn_start), 20);
        AEFacePack.getInstance().registerFaceFlowActivity(this);
        QrRecordStatusManager.update(QrRecordStatus.SCAN_DONE);

        Intent in = getIntent();
        TextView tvMain = findViewById(R.id.tv_main_title);
        TextView tvSub = findViewById(R.id.tv_sub_title);
        TextView tvName = findViewById(R.id.tv_name);
        TextView tvRegion = findViewById(R.id.tv_region);
        TextView tvIdType = findViewById(R.id.tv_id_type);
        TextView tvIdNumber = findViewById(R.id.tv_id_number);

        String main = in.getStringExtra(InfoConfirmExtras.EXTRA_MAIN_TITLE);
        String sub = in.getStringExtra(InfoConfirmExtras.EXTRA_SUB_TITLE);
        if (!TextUtils.isEmpty(main)) {
            tvMain.setText(main);
        }
        if (!TextUtils.isEmpty(sub)) {
            tvSub.setText(sub);
        }
        tvName.setText(safe(in.getStringExtra(InfoConfirmExtras.EXTRA_REAL_NAME)));
        tvRegion.setText(safe(in.getStringExtra(InfoConfirmExtras.EXTRA_REGION)));
        tvIdType.setText(safe(in.getStringExtra(InfoConfirmExtras.EXTRA_ID_TYPE)));
        tvIdNumber.setText(safe(in.getStringExtra(InfoConfirmExtras.EXTRA_ID_NUMBER)));

        ImageView back = findViewById(R.id.btn_back);
        back.setOnClickListener(v -> finishByUserCancel());

        startButton = findViewById(R.id.btn_start);
        startButton.setOnClickListener(v -> {
            String failDetail = in.getStringExtra(InfoConfirmExtras.EXTRA_FAIL_DETAIL);
            AEFacePack pack = AEFacePack.getInstance();
            pack.setPendingFailDetail(failDetail);
            startButton.setEnabled(false);

            // 取景页启动后再关闭确认页，避免炫彩异步拉色期间闪回首页
            pack.setOnRecognizeLaunched(() -> {
                leavingForRecognize = true;
                dismissLightLoading();
                if (!isFinishing()) {
                    finish();
                }
            });

            pack.AEYE_BeginRecog(InfoConfirmActivity.this);
            if (pack.isThunderProcessing()) {
                // 异步拉色中：保持本页并展示 loading，等回调里 finish
                showLightLoading();
            } else if (!isFinishing()) {
                // 未起页（重复调用等）：恢复按钮，清理回调
                pack.clearOnRecognizeLaunched();
                startButton.setEnabled(true);
            }
            // 同步起页：launchRecognizeActivity 已触发回调并 finish
        });
    }

    private void showLightLoading() {
        dismissLightLoading();
        lightLoadingDialog = ProgressDialog.show(
                this, null, getString(R.string.info_light_loading), true, false);
    }

    private void dismissLightLoading() {
        if (lightLoadingDialog != null && lightLoadingDialog.isShowing()) {
            lightLoadingDialog.dismiss();
        }
        lightLoadingDialog = null;
    }

    private static String safe(String s) {
        return TextUtils.isEmpty(s) ? "--" : s;
    }

    /**
     * 用户点返回 / 系统返回：任务结束失效，上报 {@code status=6} + {@code failedType=6}。
     * 进入活体页后关闭本页不报取消。
     */
    private void finishByUserCancel() {
        if (!leavingForRecognize && !cancelReported) {
            cancelReported = true;
            QrRecordStatusManager.update(QrRecordStatus.TASK_CANCELLED,
                    QrRecordStatus.FailedType.CANCELLED);
        }
        finish();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finishByUserCancel();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        finishByUserCancel();
    }

    @Override
    protected void onDestroy() {
        dismissLightLoading();
        AEFacePack.getInstance().clearOnRecognizeLaunched();
        AEFacePack.getInstance().unregisterFaceFlowActivity(this);
        super.onDestroy();
    }
}

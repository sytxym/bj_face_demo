package com.aeye.face.confirm;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Outline;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewOutlineProvider;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        FaceImmersiveStatusBar.install(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.aeye_activity_info_confirm);
        FaceImmersiveStatusBar.bindToolbar(this, findViewById(R.id.face_toolbar), null);
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

        ImageView ivPerson = findViewById(R.id.iv_person_placeholder);
        ivPerson.post(() -> applyCircleClip(ivPerson));

        ImageView back = findViewById(R.id.btn_back);
        back.setOnClickListener(v -> finish());

        Button start = findViewById(R.id.btn_start);
        start.setOnClickListener(v -> {
            String failDetail = in.getStringExtra(InfoConfirmExtras.EXTRA_FAIL_DETAIL);
            AEFacePack.getInstance().setPendingFailDetail(failDetail);
            AEFacePack.getInstance().AEYE_BeginRecog(InfoConfirmActivity.this);
            finish();
        });
    }

    private static String safe(String s) {
        return TextUtils.isEmpty(s) ? "--" : s;
    }

    /** 人像占位图圆形裁剪，与产品稿一致 */
    private static void applyCircleClip(ImageView imageView) {
        if (imageView == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }
        imageView.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setOval(0, 0, view.getWidth(), view.getHeight());
            }
        });
        imageView.setClipToOutline(true);
    }

    @Override
    protected void onDestroy() {
        AEFacePack.getInstance().unregisterFaceFlowActivity(this);
        super.onDestroy();
    }
}

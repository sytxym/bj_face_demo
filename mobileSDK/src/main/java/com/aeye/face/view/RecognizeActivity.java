package com.aeye.face.view;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.ColorStateList;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.aeye.android.config.ConfigData;
import com.aeye.android.uitls.BitmapUtils;
import com.aeye.face.AEFacePack;
import com.aeye.face.AEFaceParam;
import com.aeye.face.ui.FaceImmersiveStatusBar;
import com.aeye.face.config.IDConstants;
import com.aeye.face.camera.CameraConfig;
import com.aeye.face.camera.CameraManager;
import com.aeye.face.camera.CaptureActivityHandler;
import com.aeye.face.lightView.RecognizeLightActivity;
import com.aeye.face.uitls.AudioUtils;
import com.aeye.face.uitls.DeviceSafeCheckUtils;
import com.aeye.face.uitls.FileUtil;
import com.aeye.face.uitls.MLog;
import com.aeye.face.uitls.PictureManagerUtils;
import com.aeye.face.api.model.FaceIdentResult;
import com.aeye.face.verify.FaceVerifyLogManager;
import com.aeye.face.verify.FaceVerifyManager;
import com.aeye.face.verify.FaceVerifySession;
import com.aeye.face.verify.QrRecordStatus;
import com.aeye.face.verify.QrRecordStatusManager;
import com.aeye.sdk.AEFaceAlive;
import com.aeye.sdk.AEFaceDetect;
import com.aeye.sdk.AEFaceQuality;
import com.aeye.sdk.AEFaceUnhack;
import com.aeye.sm.SMCipherCaculater;
import com.sdk.core.BuildConfig;
import com.sdk.core.R;

import java.io.IOException;
import java.io.InputStream;

public class RecognizeActivity extends Activity implements
        SurfaceHolder.Callback {
    public static final String TAG = RecognizeActivity.class.getSimpleName();
    /** 超时标志 **/
    public static final int TIME_OUT = 0;
    /** 摄像头方向参数 Key **/
    public static final String CAMERA_DIRECTION = "Camera_Direction";
    /** 侧脸质量不合格标志 **/
    public static final int QUALITY_SIDE = 10;
    /** 人脸出框质量不合格标志 **/
    public static final int QUALITY_OUT = 20;

    /** 活体连续未检测到人脸达到该时长后才展示「验证失败」 */
    private static final long NO_FACE_FAIL_DELAY_MS = 8000L;

    /** 底部提示着色（与 {@link #showHint(String, int)} 的第二个参数对应） */
    public static final int HINT_COLOR_THEME = 0;
    public static final int HINT_COLOR_SUCCESS = 1;
    public static final int HINT_COLOR_ERROR = 2;
    /**
     * 顶部标题栏
     **/
    private TextView tvTop;
    /**
     * 验证提示、环境提示、遮挡提示
     **/
    private TextView tvCheckHint, tvEnvHint, tvMaskHint;
    //	private ImageView ivMovie;
    /** 人脸状态布局 **/
    private RelativeLayout faceStatus;
    /** 引导页面布局 **/
    private FrameLayout introduceView;
    /** 开始引导按钮 **/
    private TextView introduceBegin;
    /** 人脸检测框视图 **/
    private FaceView faceRect;
    /**
     * 认证倒计时视图
     **/
    private CountView tvRecogTimeCountdown;
    /** 返回按钮、倒计时数字图片、语音开关图片 **/
    private ImageView ivReturn, ivNumber, ivVoice;
    /**
     * 获取Activity 的 Handler, 用于处理相机与解码消息
     **/
    public CaptureActivityHandler handler;
    /** SurfaceView 是否已经准备好 **/
    private boolean hasSurface;
    /** 摄像头方向 (前置/后置) **/
    private int cameraDirection = -1;
    /** 保存摄像头信息的 SharedPreferences **/
    private SharedPreferences spCameraInfo;
    /**
     * 记录当前是否正在解码人脸
     **/
    private boolean isDecode = true;
    /** 是否已经返回结果 **/
    private boolean m_hasFinishReturn = false;
    /** Activity 是否已经结束 **/
    private boolean mFinish = false;
    /** 保持屏幕唤醒的锁 **/
    WakeLock m_WakeLock = null;
    /** 人脸状态标志：-1无人脸，0不确定，1有人脸 **/
    private int mFaceOK = 0;
    /** 当前提示文本ID **/
    int textId = -1;
    /** 当前语音ID **/
    int curVoice = 0;

    /** 倒计时控制器 **/
    private MyCount countDown = null;
    /** 语音是否开启 **/
    private boolean voiceTriggle = false;
    /** 人脸是否已经显示 **/
    private boolean faceDisplayed = false;
    /** 计数器 **/
    int count = 1;

    /** 是否显示引导页 **/
    private boolean introduceTriggle = false;
    /** 是否需要采集 **/
    private boolean collect = true;
    /** 是否是 Android 6.0 及以上系统 **/
    boolean isUpAndroid6 = true;

    /** 扫描环覆盖视图 **/
    private ScanRingOverlayView scanRingMain;
    /** 失败详情提示 **/
    private TextView tvFailDetail;
    /** 失败重试按钮 **/
    private Button btnFailRetry;
    /** 失败其他方式按钮 **/
    private Button btnFailOther;
    /** 提示文本 **/
    private TextView tvHint;
    /** 扫描环动画相位 **/
    private float ringPhase;
    private final Handler ringHandler = new Handler(Looper.getMainLooper());
    private final Runnable ringProgressRunnable = new Runnable() {
        @Override
        public void run() {
            if (scanRingMain == null || mFinish || m_hasFinishReturn) {
                return;
            }
            if (mInPlaceSuccessUi || mInPlaceFailUi || mFaceVerifying) {
                return;
            }
            /* 约 2s 转一圈（90ms * 22 步 ≈ 352°，略补至 360） */
            ringPhase += 20f;
            if (ringPhase >= 360f) {
                ringPhase -= 360f;
            }
            scanRingMain.setProgress(0f);
            scanRingMain.setArcStartAngle(ringPhase);
            ringHandler.postDelayed(this, 60);
        }
    };

    /** 成功页 2s 后回调：在 onDestroy 中移除，避免界面已销毁仍触发 */
    private Runnable mSuccessFinishRunnable;
    /** 取景页内联成功态（绿环 + 底部验证通过） */
    private boolean mInPlaceSuccessUi;
    /** 活体动作已完成，正在调用人脸核验接口 */
    private boolean mFaceVerifying;
    private ValueAnimator mRingSuccessAnimator;
    /** 取景页内联失败/超时态（粉环 + 红色提示 + 底部按钮） */
    private boolean mInPlaceFailUi;
    private boolean mInPlaceFailIsTimeout;
    private long mNoFaceSinceElapsedMs = -1L;
    private Runnable mNoFaceFailRunnable;
    /** 是否已上报二维码终态（4 未通过 / 5 已通过 / 2 异常退出） */
    private boolean mQrRecordFinalized;

    /**
     * Activity 生命周期 onCreate
     * 初始化视图、绑定控件、获取配置参数并初始化摄像头管理
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        FaceImmersiveStatusBar.install(this);
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);
        if(AEFacePack.getInstance().isLand()) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }else{
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        setContentView(R.layout.aeye_recognize);
        FaceImmersiveStatusBar.bindToolbar(this,
                findViewById(R.id.face_toolbar),
                findViewById(R.id.face_toolbar_gap));

        introduceView = (FrameLayout) findViewById(R.id.introduceView);
        introduceBegin = (TextView) findViewById(R.id.introduceBegin);
        introduceBegin.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                prepareRecog();
                introduceView.setVisibility(View.GONE);
            }
        });
        /* 新版实名核身 UI：不再展示介绍页，直接进入取景 */
        introduceTriggle = false;
        introduceView.setVisibility(View.GONE);

        tvTop = (TextView) findViewById(R.id.tvTop);
        /** 认证期间的 提示 **/
        tvCheckHint = (TextView) findViewById(R.id.tvCheckHint);
        tvEnvHint = (TextView) findViewById(R.id.tvEnvHint);
        /** 3秒倒计时 **/
        ivNumber = (ImageView) findViewById(R.id.ivNumber);
//		ivMovie = (ImageView) findViewById(MResource.getIdByName(R.id.movies);
        ivVoice = (ImageView) findViewById(R.id.ivVoice);

//        ImageView ivPreviewMask = findViewById(R.id.ivPreviewMask);
//        if (ivPreviewMask != null) {
//            ivPreviewMask.setVisibility(View.GONE);
//        }

        faceStatus = (RelativeLayout) findViewById(R.id.faceStatus);
        if (AEFacePack.getInstance().isWhiteBackgroud() && !AEFacePack.getInstance().isLand()) {
//            faceStatus.setBackgroundResource(R.drawable.aeye_selector_bg_white);
        }
//        if(DeviceSafeCheckUtils.isDeviceUnSafe()){
//            String unSafeStr = getString(R.string.aeye_safetip);
//            showToast(unSafeStr);
//            /**start 临时注释20230717，在活体接口中上传风险标签*****/
//            finishActivityByOther(AEFacePack.ERROR_DANGER_DEVICE, "");
//            return;
//            /**end 临时注释20230717，在活体接口中上传风险标签*****/
//        }
//        boolean isSystemDebug =  DeviceSafeCheckUtils.checkSystemUser() || DeviceSafeCheckUtils.checkDeviceDebuggable();
//        if(  isSystemDebug) {
//            String str = "高危ROM";
//            finishActivityByOther(AEFacePack.ERROR_DANGER_DEVICE, "");
//            return;
//        }
        ivReturn = (ImageView) findViewById(R.id.ivReturn);

        faceRect = (FaceView) findViewById(R.id.faceRect);
        faceRect.setSuppressFaceRect(true);
        /** 认证倒计时 **/
        tvRecogTimeCountdown = (CountView) findViewById(R.id.tvRecogTimeCountdown);

        scanRingMain = findViewById(R.id.scan_ring_overlay);
        boolean ringHoleUi = AEFacePack.getInstance().isWhiteBackgroud()
                && !AEFacePack.getInstance().isLand();
        if (scanRingMain != null) {
            scanRingMain.setHoleMaskEnabled(ringHoleUi);
        }
        tvFailDetail = findViewById(R.id.tvFailDetail);
        btnFailRetry = findViewById(R.id.btn_fail_retry);
        btnFailOther = findViewById(R.id.btn_fail_other);
        tvHint = findViewById(R.id.tvHint);
        if (btnFailRetry != null) {
            btnFailRetry.setOnClickListener(v -> onInPlaceFailRetry());
        }
        if (btnFailOther != null) {
            btnFailOther.setOnClickListener(v -> exitToAuthMethodHome());
        }

        spCameraInfo = getSharedPreferences(ConfigData.SP_CAMERA_INFO,
                Context.MODE_PRIVATE);

        int topColor = AEFacePack.getInstance().getTopColor();
        if (topColor == 0) {
            topColor = ContextCompat.getColor(this, R.color.face_theme_primary);
        }
        tvTop.setBackgroundColor(topColor);

        String title = AEFacePack.getInstance().getTitle();
        if (!TextUtils.isEmpty(title)) {
            tvTop.setText(title);
        } else {
            tvTop.setText(R.string.face_title);
        }
        tvCheckHint.setVisibility(View.GONE);
//        try {只用于测试速度
//            AssetManager assetManager = this.getAssets();
//            InputStream stearm =  assetManager.open("0000.jpg");
//            Bitmap bitmap = BitmapFactory.decodeStream(stearm);
//            long start = System.currentTimeMillis();
//            Log.e(TAG,"start "+start);
//            Rect[] rect = AEFaceDetect.getInstance().AEYE_FaceDetect(bitmap);
//            Log.e(TAG,"end detect "+System.currentTimeMillis());
//            AEFaceAlive.getInstance().AEYE_Alive_DetectVIS(BitmapUtils.getBitmapData(bitmap),bitmap.getWidth(),bitmap.getHeight(),rect[0],null);
//            Log.e(TAG,"end transfor "+System.currentTimeMillis());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        CameraManager.init(getApplication());

        hasSurface = false;
        initData();
        m_hasFinishReturn = false;
        AEFacePack.getInstance().registerFaceFlowActivity(this);

        ivReturn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishActivityByUserCancel();
            }
        });

        ivVoice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (voiceTriggle) {
                    voiceTriggle = false;
                } else {
                    voiceTriggle = true;
                }
                updateVoice();
            }
        });

        if (!AEFacePack.getInstance().isOpenReturnButton()) {
            ivReturn.setVisibility(View.GONE);
        } else {
            ivReturn.setVisibility(View.VISIBLE);
        }

        tvRecogTimeCountdown.setVisibility(View.INVISIBLE);
        initVoice();
        tvMaskHint = findViewById(R.id.tvMaskHint);
        setWindowBrightness(255);
        int versionCode = Build.VERSION.SDK_INT;
        if(versionCode <24){
            isUpAndroid6 = false;
        }

        AEFacePack.getInstance().AEYE_Init(this);
        String sign = "E3A03D4A1586F6952F0E699344D0F4E2";
//        String data = "5468697320697320612074657374";
        String data = "b7ff10f411dee12488479e36f7912ed4";//8904231165912ba2eb84495043dc7452
//        byte[] sm4_encrypt = Base64.decode(data, 2);
//        String encrydata = "123456";
//        byte[] key = hexToBytes(sign);
//        byte[] sm4_encrypt = SMCipherCaculater.SM4_encrypt(key, encrydata.getBytes());
//        String sm4encrypt = SMUtil.bytesToHexString(sm4_encrypt);
//        Log.e(TAG,"sm4encrypt : "+sm4encrypt);
//
//
//        data = sm4encrypt;
//        byte[] imgData = SMCipherCaculater.SM4_decrypt(key, SMUtil.hexToBytes(data));
//        String sm4Decrypt = new String(imgData);
//        Log.e(TAG,"decrypt : "+sm4Decrypt);
//       String sm4Bitmap = FileUtil.readTxt("/sdcard/sm4.txt");
//        SMUtil.DataSM4Decode(sign,sm4Bitmap);
//        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.test);
//        float score = AEFaceUnhack.getInstance().AEYE_AliveUnhack_Detect(bitmap);
//        Log.d(TAG, "imageCheck score test =" + score);
//        bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.test1);
//         score = AEFaceUnhack.getInstance().AEYE_AliveUnhack_Detect(bitmap);
//        Log.d(TAG, "imageCheck score 1=" + score);
//        bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.test2);
//        score = AEFaceUnhack.getInstance().AEYE_AliveUnhack_Detect(bitmap);
//        Log.d(TAG, "imageCheck score 2=" + score);
//        bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.test3);
//        score = AEFaceUnhack.getInstance().AEYE_AliveUnhack_Detect(bitmap);
//        Log.d(TAG, "imageCheck score 3=" + score);
//        bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.test4);
//        score = AEFaceUnhack.getInstance().AEYE_AliveUnhack_Detect(bitmap);
//        Log.d(TAG, "imageCheck score 4=" + score);
//        bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.test5);
//        score = AEFaceUnhack.getInstance().AEYE_AliveUnhack_Detect(bitmap);
//        Log.d(TAG, "imageCheck score 5=" + score);



    Log.e(TAG,"version :  "+BuildConfig.versionName+", " );
        QrRecordStatusManager.update(QrRecordStatus.VERIFYING);
    }
    public static byte[] hexToBytes(String hexString) {
        if (hexString != null && hexString.length() != 0) {
            char[] hex = hexString.toCharArray();
            int length = hex.length / 2;
            byte[] rawData = new byte[length];

            for (int i = 0; i < length; ++i) {
                int high = Character.digit(hex[i * 2], 16);
                int low = Character.digit(hex[i * 2 + 1], 16);
                int value = high << 4 | low;
                if (value > 127) {
                    value -= 256;
                }
                rawData[i] = (byte) value;
            }
            return rawData;
        } else {
            return null;
        }
    }

    private static final int UI_MSG_HINT_SHOW = 1;
    private static final int UI_MSG_HINT_HIDE = UI_MSG_HINT_SHOW+1;
    private static final int UI_MSG_MESSAGE_BOX = UI_MSG_HINT_HIDE+1;
    private static final int UI_MSG_TIMEOUT_BOX = UI_MSG_MESSAGE_BOX+1;
    private static final int UI_MSG_TVENVHINT_SHOW = UI_MSG_TIMEOUT_BOX +1;
    private static final int UI_MSG_HINT_TEXT_SHOW = UI_MSG_TVENVHINT_SHOW +1;
    private static final int UI_MSG_TVENVHINT_HIDE = UI_MSG_HINT_TEXT_SHOW+1;
    private static final int UI_MSG_TIMEOUTCOUNT_SHOW = UI_MSG_TVENVHINT_HIDE+1;
    private static final int UI_MSG_FACESTATUS = UI_MSG_TIMEOUTCOUNT_SHOW+1;

    private Handler mUIHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case UI_MSG_HINT_SHOW:
                    if (mInPlaceFailUi || mInPlaceSuccessUi) {
                        break;
                    }
                    int mFinalTextId = (int) msg.obj;
                    int hintColorKindShow = msg.arg1;
                    if(isUpAndroid6) {
//                        float scale = getApplication().getResources().getDisplayMetrics().density;
//                        int px = (int) (150 * scale + 0.5f);
//                        TranslateAnimation ani = new TranslateAnimation(px, 0, 0, 0);
//                        ani.setInterpolator(new DecelerateInterpolator());
//                        ani.setDuration(600);
//                        tvCheckHint.startAnimation(ani);
                    }
                    if(tvCheckHint.getVisibility() != View.VISIBLE && isUpAndroid6) {
                        tvCheckHint.setVisibility(View.VISIBLE);
                    }
//                    tvCheckHint.setVisibility(View.VISIBLE);
//                    if(!isUpAndroid6) {
                        if(isUpAndroid6) {
                        clearCheckHintLeadingIcon();
                        tvCheckHint.setTextColor(ContextCompat.getColor(RecognizeActivity.this,
                                hintColorResId(hintColorKindShow)));
                        tvCheckHint.setText(mFinalTextId);
                    }else{
                        showShortToast(getString(mFinalTextId));
                    }
                    break;
                case UI_MSG_HINT_TEXT_SHOW:
                    if (mInPlaceFailUi || mInPlaceSuccessUi) {
                        break;
                    }
                    String text = (String) msg.obj;
                    int hintColorKind = msg.arg1;
                    if (tvCheckHint.getVisibility() != View.VISIBLE)
                        tvCheckHint.setVisibility(View.VISIBLE);
                    clearCheckHintLeadingIcon();
                    tvCheckHint.setTextColor(ContextCompat.getColor(RecognizeActivity.this,
                            hintColorResId(hintColorKind)));
                    switch (text) {
                        case "aeye_quality_out":
                            tvCheckHint.setText(R.string.aeye_quality_out);
                            break;
                        case "aeye_camera_notice":
                            Log.e(TAG, "请正视摄像头  showhint");
                            tvCheckHint.setText(R.string.aeye_camera_notice);
                            break;
                        case "face_far":
                            tvCheckHint.setText(R.string.aeye_face_far);
                            break;
                    }
                    break;
                case UI_MSG_HINT_HIDE:
                    tvCheckHint.setVisibility(View.GONE);
                    break;
                case UI_MSG_MESSAGE_BOX:
                    showInPlaceFailUi(false, resolveLoseFaceFailDetail());
                    break;
                case UI_MSG_TIMEOUT_BOX:
                    showInPlaceFailUi(true, null);
                    break;
                case UI_MSG_TVENVHINT_SHOW:
                    break;
                case UI_MSG_TVENVHINT_HIDE:
                    if (tvEnvHint.getVisibility() == View.VISIBLE)
                        tvEnvHint.setVisibility(View.INVISIBLE);
                    break;
                case UI_MSG_TIMEOUTCOUNT_SHOW:
                    if(mUIHandler.hasMessages(UI_MSG_TIMEOUTCOUNT_SHOW)){
                        mUIHandler.removeMessages(UI_MSG_TIMEOUTCOUNT_SHOW);
                    }
//                    if(tvRecogTimeCountdown.getVisibility() !=View.VISIBLE && isUpAndroid6) {
//                        tvRecogTimeCountdown.setVisibility(View.VISIBLE);
//                    }

                    break;
                case UI_MSG_FACESTATUS:
                    boolean face = (boolean) msg.obj;
                    faceStatus.setEnabled(face);
                    if (face) {
                        cancelNoFaceFailTimer();
                    }
                    break;
            }
        }
    };

    /**
     * @param brightness
     */
    private void setWindowBrightness(int brightness) {
        Window window = getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.screenBrightness = brightness / 255.0f;
        window.setAttributes(lp);
    }
    private void initVoice() {
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        if (AEFacePack.getInstance().isVoiceOff()) {
            voiceTriggle = false;
        } else {
            voiceTriggle = true;
        }
        updateVoice();
    }

    private void updateVoice() {
        ivVoice.setActivated(voiceTriggle);
    }

    public boolean isVoiceOpen(){
        return  voiceTriggle;
    }

    /**
     * 初始化摄像头方向数据并保存到 SharedPreferences
     */
    private void initData() {
        /** 初始化使用摄像头ID */
        int cameraD = -1;
        if (Camera.getNumberOfCameras() == 1) {
            cameraD = 0;
        } else if (Camera.getNumberOfCameras() == 2) {
            cameraD = 1;
        } else {
            cameraD = 0;
        }

        cameraDirection = getIntent().getIntExtra(CAMERA_DIRECTION, cameraD);

        Editor editor = spCameraInfo.edit();
        editor.clear();
        editor.putInt(ConfigData.SP_CAMERA_DIRECTION, cameraDirection);
        editor.commit();
    }

    public void setDecodeStatus(boolean decode) {
        isDecode = decode;
    }

    public boolean getDecodeStatus() {
        return isDecode;
    }

    /**
     * 显示对应的活体动作提示文字和播放语音
     * @param id 动作ID (例如: 抬头, 低头, 摇头)
     * @param voice 是否播放语音
     * @param anim 是否显示动画(目前暂未使用)
     */
    public void showAlivePose(int id, boolean voice, boolean anim) {
        int animId, audioId, textId;
        try {
            if (AEFacePack.getInstance().isAlivePose()) {
                if (id == AEFaceAlive.POSE_FACE_UP) {
                    textId = R.string.aeye_face_up;
//				animId = R.anim.aeye_anim_up;
                    audioId = R.raw.aeye_up;
                } else if (id == AEFaceAlive.POSE_FACE_DOWN) {
                    textId = R.string.aeye_face_down;
//				animId = R.anim.aeye_anim_down;
                    audioId = R.raw.aeye_down;
                } else if (id == AEFaceAlive.POSE_FACE_SHAKE) {
                    textId = R.string.aeye_face_shake;
//				animId = R.anim.aeye_anim_shake;
                    audioId = R.raw.aeye_shake;
                } else if (id == AEFaceAlive.POSE_MOUTH_OPEN) {
                    textId = R.string.aeye_face_mouth;
//				animId = R.anim.aeye_anim_mouth;
                    audioId = R.raw.aeye_mouth;
                } else if (id == AEFaceAlive.POSE_EYE_BLINK) {
                    textId = R.string.aeye_face_blick;
//				animId = R.anim.aeye_anim_eye;
                    audioId = R.raw.aeye_eye;
                } else {
                    textId = R.string.aeye_camera_notice;
//				animId = R.anim.aeye_anim_normal;
                    audioId = R.raw.aeye_face;
                }
            } else {
                textId = R.string.aeye_camera_notice;
//				animId = R.anim.aeye_anim_normal;
                audioId = R.raw.aeye_face;
            }
        } catch (Exception e) {
            audioId = 0;
            animId = 0;
            textId = 0;
            Log.e("AEYE", "m_Afd == null " + e.toString());
            return;
        }

        anim = true;
        Message msg = new Message();
        msg.what = UI_MSG_HINT_SHOW;
        msg.obj = textId;
        msg.arg1 = HINT_COLOR_THEME;
        mUIHandler.sendMessage(msg);

		
		/*ivMovie.setImageResource(animId);
		AnimationDrawable animationDrawable = (AnimationDrawable) ivMovie.getDrawable();
		if (AEFacePack.getInstance().isModelAllSide() &&
				AEFacePack.getInstance().isAliveOff()) {
			animationDrawable.setOneShot(true);
		}
		animationDrawable.start();*/
        if (voice && voiceTriggle) {
            handler.pauseDecode();
            handler.postDelayed(new Runnable() {

                @Override
                public void run() {
                    handler.resumeDecode();
                }
            }, 300);
            AudioUtils.playVoice(getApplication(), audioId, new MediaPlayer.OnCompletionListener() {

                @Override
                public void onCompletion(MediaPlayer mp) {
                    if (curVoice != 0 && isDecode) {
                        AudioUtils.playVoice(getApplication(), curVoice);
                    }
                }
            });
        }
    }

    public void stopTimer() {
        if (countDown != null) {
            countDown.cancel();
        }
    }

    public void restartTimer(long timeout) {
        if (countDown != null) {
            countDown.cancel();
        } else {
            countDown = new MyCount(handler);
        }

        countDown.init((int) timeout);
        countDown.start();

        if (AEFacePack.getInstance().isAliveOff() &&
                AEFacePack.getInstance().isModelAllSide()) {
            if (!handler.startOneSide()) {
                //采集失败。
                if (countDown != null) {
                    countDown.cancel();
                }
                mUIHandler.sendEmptyMessage(UI_MSG_MESSAGE_BOX);
            }
        }
    }

    @Override
    /**初始化SurfaceView预览、倒计时*/
    protected void onResume() {
        super.onResume();
        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
//        setSurfaceSize(surfaceView, 720, 960);
        WindowManager manager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        int w = display.getWidth();
        int h = display.getHeight();
        Log.e(TAG, "display w=" + w + ",h=" + h);
//        float rate = 1.5f;
        float rate = 1.2f;

        if(AEFacePack.getInstance().isLand() ){
            if(w>1200) {
                rate = 2.5f;
            }else  if(w<1199){
                rate = 1.5f;
            }
        }


        int targetW = (int) (w / rate);
        int targetH = targetW * 4 / 3;

        if (AEFacePack.getInstance().isWhiteBackgroud() && !AEFacePack.getInstance().isLand()) {
            int panelPx = getResources().getDimensionPixelSize(R.dimen.face_preview_panel_size);
            int[] previewSize = ScanRingOverlayView.computePortraitPreviewCoverSize(this, panelPx);
            clearCircularPreviewClip(surfaceView);
            setSurfaceSize(surfaceView, previewSize[0], previewSize[1], false);
        } else {
            setSurfaceSize(surfaceView, targetW, targetH, false);
        }

        if (hasSurface) {
            initCamera(surfaceHolder);
        } else {
            surfaceHolder.addCallback(this);
            surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }
    }

    private void setSurfaceSize(SurfaceView mSurfaceView, int w, int h, boolean fillParent) {
        Log.e(TAG, "setSurfaceSize w=" + w + ",h=" + h + " fillParent=" + fillParent);
        CameraConfig.getInstance().setSurfaceSize(w, h);
        FrameLayout.LayoutParams lp;
        if (fillParent) {
            lp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
        } else {
            lp = new FrameLayout.LayoutParams(w, h);
        }
        lp.gravity = Gravity.CENTER;
        mSurfaceView.setLayoutParams(lp);
    }

    /** 竖屏白底：取消圆形裁剪，靠圆孔遮罩做 center-cover，避免 1:1 拉伸 */
    private void clearCircularPreviewClip(View preview) {
        if (preview == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }
        preview.setClipToOutline(false);
        preview.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
    }

    public boolean isCollect() {
        return collect;
    }

    private void prepareRecog() {
        handler.startPreview();
        tvRecogTimeCountdown.setVisibility(View.GONE);
        if (AEFacePack.getInstance().isShowPrepare()) {
            mUIHandler.sendEmptyMessage(UI_MSG_TVENVHINT_HIDE);
            Drawable[] drawable = tvEnvHint.getCompoundDrawables();
            ((AnimationDrawable) drawable[0]).start();
            ivNumber.setVisibility(View.VISIBLE);
            AnimationDrawable anim = (AnimationDrawable) ivNumber.getBackground();
            anim.start();
            if (!AEFacePack.getInstance().isAliveOff() ||
                    !AEFacePack.getInstance().isModelAllSide()) {
                showAlivePose(0, true, false);
            } else {
                showAlivePose(0, false, false);
            }
            showHint("aeye_camera_notice", HINT_COLOR_THEME);

            handler.postDelayed(new Runnable() {

                @Override
                public void run() {
                    startRecog();
                }
            }, anim.getNumberOfFrames() * 1000);
        } else {
            startRecog();
        }
        showFaceStatus(true, true);
    }

    /**
     * 开始人脸核验和活体检测
     * 重启摄像头预览和解码任务，并启动相应的超时定时器
     */
    private void startRecog() {
        if (AEFacePack.getInstance().isAliveOff()) {
            if (AEFacePack.getInstance().isModelAllSide()) {
                showAlivePose(IDConstants.SIDE_MIN, true, false);
                restartTimer(AEFacePack.getInstance().getMotionTime());
            } else {
                restartTimer(AEFacePack.getInstance().getRecogTime());
            }
        } else {
            if (!AEFacePack.getInstance().isFaceAppearStartMode()) {
                restartTimer(AEFacePack.getInstance().getMotionTime());
            }
        }

        if (!AEFacePack.getInstance().isFaceAppearStartMode()) {
            mUIHandler.sendEmptyMessage(UI_MSG_TIMEOUTCOUNT_SHOW);
        }

        ivNumber.setVisibility(View.GONE);

        handler.restartPreviewAndDecode();
        startRingProgress();
    }

    /**
     * 根据摄像头ID开启摄像头、初始CaptureActivityHandler(处理编码结果、网络请求超时结果)<BR/>
     * 预览Act里初始化一个CaptureActivityHandler来处理解码的消息<BR/>
     * CaptureActivityHandler里初始一个DecodeThread线程,该线程包括一个DecodeHandler处理图片解码消息
     * DecodeHandler处理后最终给CaptureActivityHandler发送消息处理
     */
    private void initCamera(SurfaceHolder surfaceHolder) {
        try {
            CameraManager.get(this).openDriver(surfaceHolder, cameraDirection);// 开户摄像头驱动
        } catch (Exception e) {
            String str = getApplication().getString(R.string.aeye_camera_error);
            finishActivityByOther(AEFacePack.ERROR_CAMERA, str);
            return;
        }

        if (handler == null) {
            handler = new CaptureActivityHandler(this);// 创建处理解码结果的Handler对象.该对象一创建就执行预览操作
        }
        if (!introduceTriggle) {
            prepareRecog();
        }
    }

    public int getOrientation() {
        if (AEFacePack.getInstance().isSetCaptureOrientation()) {
            return AEFacePack.getInstance().getCaptureOrientation();
        } else {
            return CameraManager.get(this).getOrientation(cameraDirection);
        }
    }

    @Override
    /**持锁保持屏幕唤醒*/
    protected void onStart() {
        super.onStart();
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        m_WakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "AE:Screen");
        m_WakeLock.acquire();

        if (AEFacePack.getInstance().isMaxBrightness()) {
            Window window = this.getWindow();
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.screenBrightness = 1;
            window.setAttributes(lp);
        }

        Long timeout = Long.parseLong("60");
        Log.e("timeout", "" + timeout);
    }

    @Override
    /**释放锁以让屏幕可以锁屏、退出预览*/
    protected void onPause() {
        super.onPause();

        CameraManager.get(this).closeDriver();
    }

    @Override
    protected void onStop() {
        super.onStop();

        m_WakeLock.release();

        if (handler != null) {
            handler.cancelDecodeTask();
        }

        if (!mFinish) {
            finishActivityByUserCancel();
        }
    }

    @Override
    /**退出预览*/
    public void onDestroy() {
        if (!mQrRecordFinalized) {
            QrRecordStatusManager.update(QrRecordStatus.ABNORMAL_EXIT);
            mQrRecordFinalized = true;
        }
        AEFacePack.getInstance().unregisterFaceFlowActivity(this);
        super.onDestroy();
        if (handler != null) {
            handler.quitSynchronously();
            handler = null;
        }
        if (countDown != null) {
            countDown.cancel();
            countDown = null;
        }
        PictureManagerUtils.destroyManager();
        CameraManager.unInit();
        AudioUtils.destroyPlayer();
        stopRingProgress();
        if (mRingSuccessAnimator != null) {
            mRingSuccessAnimator.cancel();
            mRingSuccessAnimator = null;
        }
        if (mSuccessFinishRunnable != null) {
            mUIHandler.removeCallbacks(mSuccessFinishRunnable);
            mSuccessFinishRunnable = null;
        }
        cancelNoFaceFailTimer();
    }

    @Override
    public void finish() {
        super.finish();
        mFinish = true;
    }

    public void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private Toast mToast;
    private void showShortToast(String msg) {
        if(mToast != null){
            mToast.cancel();
            mToast = null;
        }
        mToast = Toast.makeText(RecognizeActivity.this,msg,Toast.LENGTH_LONG);
        mToast.setGravity(Gravity.CENTER_HORIZONTAL,0,200);
        mToast.show();
    }

    /**
     * 更新预览框上方文字提示信息
     */
    /**
     * 更新预览框上方文字提示信息
     */
    public void showHint(final String msg, final int hintColorKind) {
        Message message = new Message();
        message.what = UI_MSG_HINT_TEXT_SHOW;
        message.obj = msg;
        message.arg1 = hintColorKind;
        mUIHandler.sendMessage(message);
    }
    int mId = 0;
    public void showQualityHint(final int quality, final boolean voice) {
        mId =0;
        int voiceId = 0;
        curVoice = 0;
        switch (quality) {
            default:
            case AEFaceQuality.QUALITY_UNKNOW:
                return;
            case AEFaceQuality.QUALITY_OK:
                faceAppear();
                break;
            case AEFaceQuality.QUALITY_NEAR:
                mId = R.string.aeye_quality_near;
                voiceId = R.raw.aeye_quality_near;
                break;
            case AEFaceQuality.QUALITY_FAR:
                mId = R.string.aeye_quality_far;
                voiceId = R.raw.aeye_quality_far;
                break;
            case AEFaceQuality.QUALITY_BRIGHT:
                mId = R.string.aeye_quality_bright;
                voiceId = R.raw.aeye_quality_bright;
                break;
            case AEFaceQuality.QUALITY_DARK:
                mId = R.string.aeye_quality_dim;
                voiceId = R.raw.aeye_quality_dim;
                break;
            case AEFaceQuality.QUALITY_UNEVEN:
                mId = R.string.aeye_quality_uneven;
                break;
            case QUALITY_OUT:
                mId = R.string.aeye_quality_out;
                voiceId = R.raw.aeye_out;
                break;
            case QUALITY_SIDE:
                mId = R.string.aeye_quality_side;
                break;
        }
        curVoice = voiceId;


        final int id = mId;
        if (textId != id) {//zdx
            textId = id;
            if (id == 0) {
                mUIHandler.sendEmptyMessage(UI_MSG_TVENVHINT_HIDE);
                handler.flashDisplay(false, false);
                return;
            }

            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (voiceTriggle && curVoice != 0 && voice) {
                        AudioUtils.playVoiceIdle(getApplication(), curVoice);
                    }
                }
            }).start();

        }
    }


    public void showPoseSuccessMsg(final boolean display) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (display) {
                    tvEnvHint.setText(R.string.aeye_pose_success);
                    tvEnvHint.setEnabled(true);
                    tvEnvHint.setVisibility(View.VISIBLE);
                } else {
                    tvEnvHint.setVisibility(View.INVISIBLE);
                }
            }
        });
    }
	
	/*public void showHint(int textId, int color) {
		tvCheckHint.setText(textId);
		tvCheckHint.setVisibility(View.VISIBLE);
		tvCheckHint.setTextColor(color);
	}*/

    public void dismissHint() {
        if(tvCheckHint.getVisibility() == View.VISIBLE) {
            mUIHandler.sendEmptyMessage(UI_MSG_HINT_HIDE);
        }

    }

    /** 与 dismissHint 不同：主线程同步清空，避免与紧接着的成功提示异步冲突 */
    public void syncHideCheckHint() {
        if (tvCheckHint != null) {
            tvCheckHint.setVisibility(View.GONE);
        }
    }

    /** 当前活体动作通过：底部短提示绿色 */
    public void showPoseStepPassedBriefly() {
        if (tvCheckHint == null || mInPlaceSuccessUi || mInPlaceFailUi) {
            return;
        }
        clearCheckHintLeadingIcon();
        tvCheckHint.setVisibility(View.VISIBLE);
        tvCheckHint.setText(R.string.aeye_pose_success);
        tvCheckHint.setTextColor(ContextCompat.getColor(this, R.color.face_result_success));
    }

    /** 动作未通过或校验失败：底部红色提示 */
    public void showPoseStepFailedBriefly() {
        Runnable apply = new Runnable() {
            @Override
            public void run() {
                if (tvCheckHint == null || mInPlaceSuccessUi || mInPlaceFailUi) {
                    return;
                }
                clearCheckHintLeadingIcon();
                tvCheckHint.setVisibility(View.VISIBLE);
                tvCheckHint.setText(R.string.face_pose_fail_hint);
                tvCheckHint.setTextColor(ContextCompat.getColor(RecognizeActivity.this,
                        R.color.face_result_fail));
            }
        };
        if (Looper.myLooper() == Looper.getMainLooper()) {
            apply.run();
        } else {
            runOnUiThread(apply);
        }
    }

    private static int hintColorResId(int kind) {
        switch (kind) {
            case HINT_COLOR_SUCCESS:
                return R.color.face_result_success;
            case HINT_COLOR_ERROR:
                return R.color.face_result_fail;
            default:
                return R.color.face_theme_primary;
        }
    }
    private void showFaceStatus(final boolean face, final boolean voice) {
        Message msg = new Message();
        msg.obj = face;
        msg.what = UI_MSG_FACESTATUS;
        mUIHandler.sendMessage(msg);

        if (face) {
            if (handler != null && isDecode) {
                if (faceDisplayed) {
                    showAlivePose(handler.getCurPos(), false, false);
                }
                mUIHandler.sendEmptyMessage(UI_MSG_TVENVHINT_HIDE);
            }
        } else {
            if (handler != null && isDecode) {
                showQualityHint(QUALITY_OUT, voice);
            }
        }
    }

    /**
     * 当检测到合格的人脸并且出现在屏幕中时触发，开始动作判定
     */
    private void faceAppear() {
        if (AEFacePack.getInstance().isFaceAppearStartMode() && !faceDisplayed) {
            mUIHandler.sendEmptyMessage(UI_MSG_TIMEOUTCOUNT_SHOW);
            showAlivePose(handler.getCurPos(), false, false);
            restartTimer(AEFacePack.getInstance().getMotionTime());
        }
        faceDisplayed = true;
        cancelNoFaceFailTimer();
    }

    /*mFaceOK
     * -1 	 无人脸
     * 0	不确定
     * 1 	有人脸
     */
    public void showFaceOut(final boolean inRange) {
        if ((inRange && mFaceOK < 0) ||
                (!inRange && mFaceOK > 0) || mFaceOK == 0) {
            if (mFaceOK > 0) {
                mFaceOK = -1;
            } else if (mFaceOK < 0) {
                mFaceOK = 1;
            } else if (mFaceOK == 0) {
                if (inRange) {
                    mFaceOK = 1;
                } else {
                    mFaceOK = -1;
                }
            }
            showFaceStatus(inRange, true);
        }
    }

    public void showNoFace() {
        mFaceOK = -1;
        showFaceStatus(false, false);
        if (mInPlaceFailUi || mInPlaceSuccessUi) {
            return;
        }
        FaceVerifyLogManager.uploadNoFace(getApplicationContext());
        scheduleNoFaceFailAfterDelay();
    }

    /**
     * 连续 {@link #NO_FACE_FAIL_DELAY_MS} 未检测到人脸后再展示验证失败；
     * 期间若重新检测到人脸则 {@link #cancelNoFaceFailTimer()}。
     */
    private void scheduleNoFaceFailAfterDelay() {
        if (mInPlaceFailUi || mInPlaceSuccessUi) {
            return;
        }
        long now = SystemClock.elapsedRealtime();
        if (mNoFaceSinceElapsedMs < 0) {
            mNoFaceSinceElapsedMs = now;
        }
        long elapsed = now - mNoFaceSinceElapsedMs;
        if (elapsed >= NO_FACE_FAIL_DELAY_MS) {
            cancelNoFaceFailTimer();
            showInPlaceFailUi(false, resolveLoseFaceFailDetail());
            return;
        }
        if (mNoFaceFailRunnable != null) {
            return;
        }
        long delay = NO_FACE_FAIL_DELAY_MS - elapsed;
        Log.d(TAG, "scheduleNoFaceFailAfterDelay delayMs=" + delay);
        mNoFaceFailRunnable = () -> {
            mNoFaceFailRunnable = null;
            if (mInPlaceFailUi || mInPlaceSuccessUi) {
                return;
            }
            showInPlaceFailUi(false, resolveLoseFaceFailDetail());
        };
        mUIHandler.postDelayed(mNoFaceFailRunnable, delay);
    }

    private void cancelNoFaceFailTimer() {
        mNoFaceSinceElapsedMs = -1L;
        if (mNoFaceFailRunnable != null) {
            mUIHandler.removeCallbacks(mNoFaceFailRunnable);
            mNoFaceFailRunnable = null;
        }
    }

    /** 丢脸/超时未出脸等场景的失败说明 */
    private String resolveLoseFaceFailDetail() {
        if (faceDisplayed) {
            return getString(R.string.aeye_quality_out);
        }
        return getString(R.string.face_fail_no_face_detail);
    }


    /**
     * 原「未检测到人脸」弹窗（{@link R.layout#aeye_dialog_noface}），已改为取景页内联失败 UI。
     * 保留旧实现便于回溯。
     */
    public void showMessageBox() {
        scheduleNoFaceFailAfterDelay();
        /*
        handler.cancelDecodeTask();
        AlertDialog.Builder alert = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_LIGHT);
        alert.setView(getLayoutInflater().inflate(R.layout.aeye_dialog_noface, null));
        alert.setPositiveButton(getString(android.R.string.ok), null);
        alert.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (AEFacePack.getInstance().isStrictMode()) {
                    prepareRecog();
                } else {
                    Log.d(TAG, "showMessageBox");
                    finishActivityByFail();
                }
            }
        });
        AlertDialog dialog = alert.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.setVolumeControlStream(AudioManager.STREAM_MUSIC);
        dialog.show();
        Button btnPo = (Button) dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        btnPo.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        */
    }

    /**
     * 原超时提醒弹窗（{@link R.layout#aeye_dialog_timeout}：平视手机/光线充足/未被遮挡），
     * 已改为取景页内联失败 UI，文案见 {@link R.string#face_fail_timeout_detail}。
     */
    public void showTimeOutBox() {
        showInPlaceFailUi(true, null);
        /*
        handler.cancelDecodeTask();
        AlertDialog.Builder alert = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_LIGHT);
        alert.setView(getLayoutInflater().inflate(R.layout.aeye_dialog_timeout, null));
        alert.setPositiveButton(R.string.aeye_msg_retry, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                prepareRecog();
            }
        });
        alert.setNegativeButton(R.string.aeye_msg_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finishActivityByTimeOut();
            }
        });
        alert.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                    finishActivityByTimeOut();
                    return true;
                }
                return false;
            }
        });
        AlertDialog dialog = alert.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.setVolumeControlStream(AudioManager.STREAM_MUSIC);
        dialog.show();
        Button btnPo = (Button) dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        btnPo.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        */
    }

    /** 多人脸：内联失败提示 */
    public void showManyPersonMessageBox() {
        cancelNoFaceFailTimer();
        showInPlaceFailUi(false, getString(R.string.aeye_notice_morepeople));
        /*
        handler.cancelDecodeTask();
        AlertDialog.Builder alert = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_LIGHT);
        View view = getLayoutInflater().inflate(R.layout.aeye_dialog_noface, null);
        alert.setView(view);
        TextView content = view.findViewById(R.id.content);
        content.setText(R.string.aeye_notice_morepeople);
        alert.setPositiveButton(getString(android.R.string.ok), null);
        alert.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (AEFacePack.getInstance().isStrictMode()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            prepareRecog();
                        }
                    });
                } else {
                    Log.d(TAG, "showMessageBox");
                    finishActivityByFail();
                }
            }
        });
        AlertDialog dialog = alert.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.setVolumeControlStream(AudioManager.STREAM_MUSIC);
        dialog.show();
        Button btnPo = (Button) dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        btnPo.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        */
    }
    public void showFaceRect(Rect rect, int width, int height,
                             boolean bMirror) {
        faceRect.drawFaceRect(rect, width, height, bMirror);
    }

    // ///////////////////////SurfaceHolder.Callback
    // 的三个重写方法///////////////////////////
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;
    }

    public CaptureActivityHandler getHandler() {
        return handler;
    }

    public void resetData() {
        isDecode = false;
        mFaceOK = 0;
        faceDisplayed = false;
        textId = -1;

        mFinish = false;
    }

    /***********************************************************************************/

    public void finishActivityByOther(int code, String reason) {
        stopRingProgress();
        markQrRecordAbnormalExit();
        FaceVerifyLogManager.uploadVerifyEnd(getApplicationContext(), false,
                TextUtils.isEmpty(reason) ? String.valueOf(code) : reason);
        if (null != AEFacePack.getInstance().getInterface()) {
            if (!m_hasFinishReturn) {
                PictureManagerUtils.getPictureManager().setCode(code);
                AEFacePack.getInstance().getInterface().onFinish(code,
                        PictureManagerUtils.getPictureManager().getJsonString(RecognizeActivity.this));
            }
        }
        m_hasFinishReturn = true;
        finish();
    }

    public void finishActivityByTimeOut() {
        showInPlaceFailUi(true, null);
    }

    public void finishActivityByUserCancel() {
        stopRingProgress();
        markQrRecordAbnormalExit();
        FaceVerifyLogManager.uploadVerifyEnd(getApplicationContext(), false,
                getString(R.string.aeye_user_cancel));
        if (null != AEFacePack.getInstance().getInterface()) {
            if (!m_hasFinishReturn) {
                m_hasFinishReturn = true;
                String str = getApplication().getString(R.string.aeye_user_cancel);
                PictureManagerUtils.getPictureManager().setCode(AEFaceParam.CODE_ERROR_CANCEL);
                AEFacePack.getInstance().getInterface().onFinish(AEFacePack.ERROR_CANCEL,
                        PictureManagerUtils.getPictureManager().getJsonString(RecognizeActivity.this));
            }
        }
        finish();
    }

    public void finishActivityBySuccessful() {
        boolean isHook = DeviceSafeCheckUtils.isHook(this);
        if (isHook) {
            finishActivityByOther(AEFacePack.ERROR_DANGER_DEVICE, "");
            return;
        }
        if (mInPlaceSuccessUi) {
            return;
        }
        mFaceVerifying = true;
        stopRingProgress();
        if (handler != null) {
            handler.cancelDecodeTask();
        }
        runOnUiThread(this::showInPlaceVerifying);

        final String livenessJson = PictureManagerUtils.getPictureManager()
                .getJsonString(RecognizeActivity.this);
        FaceVerifyManager.submit(livenessJson, new FaceVerifyManager.Callback() {
            @Override
            public void onPassed(FaceIdentResult result) {
                if (isFinishing()) {
                    return;
                }
                runOnUiThread(() -> onFaceVerifyPassed());
            }

            @Override
            public void onFailed(String message) {
                if (isFinishing()) {
                    return;
                }
                runOnUiThread(() -> onFaceVerifyFailed(message));
            }
        });
    }

    /** 动作完成，等待后台人脸核验结果 */
    private void showInPlaceVerifying() {
        hideVerifySubtitle();
        if (ivVoice != null) {
            ivVoice.setVisibility(View.GONE);
        }
        if (tvCheckHint == null) {
            return;
        }
        tvCheckHint.setVisibility(View.VISIBLE);
        tvCheckHint.setText(R.string.face_verify_submitting);
        tvCheckHint.setTextColor(ContextCompat.getColor(this, R.color.face_theme_primary));
        clearCheckHintLeadingIcon();
    }

    private void onFaceVerifyPassed() {
        mFaceVerifying = false;
        mInPlaceSuccessUi = true;
        markQrRecordPassed();
        FaceVerifyLogManager.uploadVerifyEnd(getApplicationContext(), true, null);
        animateScanRingToSuccess();
        showInPlaceVerifySuccess();
        if (mSuccessFinishRunnable != null) {
            mUIHandler.removeCallbacks(mSuccessFinishRunnable);
        }
        mSuccessFinishRunnable = new Runnable() {
            @Override
            public void run() {
                mSuccessFinishRunnable = null;
                deliverSuccessCallbackAndFinish();
            }
        };
        mUIHandler.postDelayed(mSuccessFinishRunnable, 1000);
    }

    private void onFaceVerifyFailed(String message) {
        mFaceVerifying = false;
        String failReason = TextUtils.isEmpty(message)
                ? getString(R.string.face_verify_fail_default)
                : message;
        markQrRecordNotPass();
        FaceVerifyLogManager.uploadVerifyEnd(getApplicationContext(), false, failReason);
        AEFacePack.getInstance().setPendingFailDetail(failReason);
        showInPlaceFailUi(false, message);
    }

    private void clearCheckHintLeadingIcon() {
        if (tvCheckHint != null) {
            tvCheckHint.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
        }
    }

    /** 检测成功：底部绿色「验证通过」+ ic_face_suc，隐藏语音按钮 */
    private void showInPlaceVerifySuccess() {
        hideVerifySubtitle();
        if (ivVoice != null) {
            ivVoice.setVisibility(View.GONE);
        }
        if (tvCheckHint == null) {
            return;
        }
        tvCheckHint.setVisibility(View.VISIBLE);
        tvCheckHint.setText(R.string.face_verify_passed);
        tvCheckHint.setTextColor(ContextCompat.getColor(this, R.color.face_result_success));
        Drawable icon = ContextCompat.getDrawable(this, R.drawable.ic_face_suc);
        tvCheckHint.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
    }

    /** 蓝弧扫满后切换为绿色整圈，与产品稿第二张一致 */
    private void animateScanRingToSuccess() {
        if (scanRingMain == null) {
            return;
        }
        scanRingMain.setVisibility(View.VISIBLE);
        if (mRingSuccessAnimator != null) {
            mRingSuccessAnimator.cancel();
        }
        final float start = scanRingMain.getProgress();
        mRingSuccessAnimator = ValueAnimator.ofFloat(start, 1f);
        mRingSuccessAnimator.setDuration(420);
        mRingSuccessAnimator.setInterpolator(new DecelerateInterpolator());
        mRingSuccessAnimator.addUpdateListener(animation ->
                scanRingMain.setProgress((Float) animation.getAnimatedValue()));
        mRingSuccessAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                scanRingMain.setMode(ScanRingOverlayView.MODE_SUCCESS);
            }
        });
        mRingSuccessAnimator.start();
    }

    private void deliverSuccessCallbackAndFinish() {
        if (isFinishing()) {
            return;
        }
        if (m_hasFinishReturn) {
            finish();
            return;
        }
        if (null != AEFacePack.getInstance().getInterface()) {
            m_hasFinishReturn = true;
            PictureManagerUtils.getPictureManager().setCode(AEFaceParam.CODE_SUCCESS);
            AEFacePack.getInstance().getInterface().onFinish(AEFacePack.SUCCESS,
                    PictureManagerUtils.getPictureManager().getJsonString(RecognizeActivity.this));
        } else {
            m_hasFinishReturn = true;
        }
        finish();
    }

    public void finishActivityByFail() {
        MLog.d(TAG, "finishActivityByFail");
        showInPlaceFailUi(false, null);
    }

    /**
     * 取景页内联失败/超时（粉环 + 红色标题 + 原因 + 重新核验/其他核验方式）。
     *
     * @param timeout      true=认证超时；false=活体/比对失败
     * @param customDetail 自定义原因文案，可为 null
     */
    public void showInPlaceFailUi(boolean timeout, String customDetail) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            runOnUiThread(() -> showInPlaceFailUi(timeout, customDetail));
            return;
        }
        if (mInPlaceFailUi || mInPlaceSuccessUi) {
            return;
        }
        cancelNoFaceFailTimer();
        mInPlaceFailUi = true;
        mInPlaceFailIsTimeout = timeout;
        stopRingProgress();
        if (countDown != null) {
            countDown.cancel();
        }
        if (handler != null) {
            handler.cancelDecodeTask();
        }
        final String detail;
        if (!TextUtils.isEmpty(customDetail)) {
            detail = customDetail;
        } else if (timeout) {
            detail = getString(R.string.face_fail_timeout_detail);
        } else {
            String pending = AEFacePack.getInstance().getPendingFailDetail();
            detail = !TextUtils.isEmpty(pending)
                    ? pending
                    : getString(R.string.face_verify_fail_default);
        }
        final int titleRes = timeout ? R.string.aeye_recog_timeout : R.string.face_verify_failed;

        syncHideCheckHint();
        hideVerifySubtitle();
        if (scanRingMain != null) {
            scanRingMain.setVisibility(View.VISIBLE);
            scanRingMain.setMode(ScanRingOverlayView.MODE_FAIL);
        }
        if (ivVoice != null) {
            ivVoice.setVisibility(View.GONE);
        }
        if (tvCheckHint != null) {
            tvCheckHint.setVisibility(View.VISIBLE);
            tvCheckHint.setText(titleRes);
            tvCheckHint.setTextColor(ContextCompat.getColor(this, R.color.face_result_fail));
            Drawable icon = ContextCompat.getDrawable(this, R.drawable.ic_face_fail);
            tvCheckHint.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
        }
        if (tvFailDetail != null) {
            tvFailDetail.setVisibility(View.VISIBLE);
            tvFailDetail.setText(detail);
        }
        if (btnFailRetry != null) {
            btnFailRetry.setVisibility(View.VISIBLE);
        }
        if (btnFailOther != null) {
            btnFailOther.setVisibility(View.VISIBLE);
        }
    }

    private void resetInPlaceFailUi() {
        mInPlaceFailUi = false;
        mInPlaceFailIsTimeout = false;
        mFaceVerifying = false;
        cancelNoFaceFailTimer();
        if (tvFailDetail != null) {
            tvFailDetail.setVisibility(View.GONE);
        }
        if (btnFailRetry != null) {
            btnFailRetry.setVisibility(View.GONE);
        }
        if (btnFailOther != null) {
            btnFailOther.setVisibility(View.GONE);
        }
        if (ivVoice != null) {
            ivVoice.setVisibility(View.VISIBLE);
        }
        clearCheckHintLeadingIcon();
        if (tvCheckHint != null) {
            tvCheckHint.setVisibility(View.GONE);
        }
        if (scanRingMain != null) {
            scanRingMain.setMode(ScanRingOverlayView.MODE_SCANNING);
        }
        showVerifySubtitle();
    }

    private void hideVerifySubtitle() {
        if (tvHint != null) {
            tvHint.setVisibility(View.INVISIBLE);
        }
    }

    private void showVerifySubtitle() {
        if (tvHint != null) {
            tvHint.setVisibility(View.VISIBLE);
        }
    }

    private void onInPlaceFailRetry() {
        FaceVerifySession.resetEndLogSent();
        mQrRecordFinalized = false;
        QrRecordStatusManager.update(QrRecordStatus.VERIFYING);
        resetInPlaceFailUi();
        if (handler != null) {
            prepareRecog();
        }
    }

    /** 「其他核验方式」：结束人脸流程所有页面并回到宿主认证方式选择首页 */
    private void exitToAuthMethodHome() {
        if (m_hasFinishReturn) {
            AEFacePack.getInstance().returnToHostAuthHome();
            return;
        }
        markQrRecordAbnormalExit();
        m_hasFinishReturn = true;
        cancelNoFaceFailTimer();
        stopRingProgress();
        if (countDown != null) {
            countDown.cancel();
        }
        if (handler != null) {
            handler.cancelDecodeTask();
        }
        if (null != AEFacePack.getInstance().getInterface()) {
            PictureManagerUtils.getPictureManager().setCode(AEFaceParam.CODE_ERROR_CANCEL);
            AEFacePack.getInstance().getInterface().onFinish(AEFacePack.ERROR_OTHER_VERIFY, null);
        }
        AEFacePack.getInstance().returnToHostAuthHome();
    }

    /** 失败/超时仍走结果回调（非「其他核验方式」） */
    private void exitAfterInPlaceFail() {
        if (m_hasFinishReturn) {
            finish();
            return;
        }
        if (null != AEFacePack.getInstance().getInterface()) {
            if (mInPlaceFailIsTimeout) {
                if (handler != null) {
                    AEFacePack.getInstance().getInterface().onPrompt(handler.getCurPos(), null);
                }
                PictureManagerUtils.getPictureManager().setCode(AEFaceParam.CODE_ERROR_TIME_OUT);
                AEFacePack.getInstance().getInterface().onFinish(AEFacePack.ERROR_TIMEOUT,
                        PictureManagerUtils.getPictureManager().getJsonString(RecognizeActivity.this));
            } else {
                PictureManagerUtils.getPictureManager().setCode(AEFaceParam.CODE_ERROR_ALIVE_FAILED);
                AEFacePack.getInstance().getInterface().onFinish(AEFacePack.ERROR_FAIL,
                        PictureManagerUtils.getPictureManager().getJsonString(RecognizeActivity.this));
            }
            m_hasFinishReturn = true;
        }
        finish();
    }

    private void stopRingProgress() {
        ringHandler.removeCallbacks(ringProgressRunnable);
    }

    private void startRingProgress() {
        if (scanRingMain == null) {
            return;
        }
        cancelNoFaceFailTimer();
        stopRingProgress();
        scanRingMain.setMode(ScanRingOverlayView.MODE_SCANNING);
        scanRingMain.setVisibility(View.VISIBLE);
        ringPhase = 0f;
        scanRingMain.setProgress(0f);
        scanRingMain.setArcStartAngle(ringPhase);
        ringHandler.post(ringProgressRunnable);
    }

    public void showMessage(String s) {
        tvMaskHint.setText(s);
    }


    public class MyCount implements Runnable {
        private Handler mHandler;
        private int time, count;

        public MyCount(Handler handler) {
            mHandler = handler;
        }

        public void init(int second) {
            time = second;
            tvRecogTimeCountdown.setCount(time, time);
        }

        public void start() {
            count = time;
            tvRecogTimeCountdown.setCount(time, count);
            mHandler.postDelayed(this, 1000);
        }

        public void cancel() {
            mHandler.removeCallbacks(this);
        }

        private void onTick() {
            tvRecogTimeCountdown.setCount(time, count);
        }

        private void finish() {
            if (m_hasFinishReturn)
                return;

            if (AEFacePack.getInstance().isModelAllSide() &&
                    AEFacePack.getInstance().isAliveOff()) {
                int side = handler.getNextSide();
                if (side <= IDConstants.SIDE_MAX) {
                    showAlivePose(side, true, false);
                    restartTimer(AEFacePack.getInstance().getMotionTime());
                    return;
                } else {
                    if (IDConstants.SIDE_NUM == PictureManagerUtils.getPictureManager().getCurNum()) {
                        finishActivityBySuccessful();
                    }
                }
            } else {
                if (AEFacePack.getInstance().isNoticeTimeout()) {
                    if (faceDisplayed) {
                        mUIHandler.sendEmptyMessage(UI_MSG_TIMEOUT_BOX);
                    } else {
                        Log.d(TAG,"finish showMessageBox ");
                        mUIHandler.sendEmptyMessage(UI_MSG_MESSAGE_BOX);
                    }
                } else {
                    showInPlaceFailUi(true, null);
                }

            }
        }

        @Override
        public void run() {
            count--;
            onTick();
            if (count <= 0) {
                finish();
            } else {
                mHandler.postDelayed(this, 1000);
            }
        }

    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
//			if (!AEFacePack.getInstance().isOpenReturnButton())
            finishActivityByUserCancel();
            return true;
        }

        return super.onKeyUp(keyCode, event);
    }

    private void markQrRecordPassed() {
        if (mQrRecordFinalized) {
            return;
        }
        mQrRecordFinalized = true;
        QrRecordStatusManager.update(QrRecordStatus.PASSED);
    }

    private void markQrRecordNotPass() {
        if (mQrRecordFinalized) {
            return;
        }
        mQrRecordFinalized = true;
        QrRecordStatusManager.update(QrRecordStatus.NOT_PASS);
    }

    private void markQrRecordAbnormalExit() {
        if (mQrRecordFinalized) {
            return;
        }
        mQrRecordFinalized = true;
        QrRecordStatusManager.update(QrRecordStatus.ABNORMAL_EXIT);
    }
}
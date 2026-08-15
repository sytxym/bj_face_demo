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
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.RectF;
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
import android.view.animation.LinearInterpolator;
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
import com.aeye.aeyelib.AEyeLightAlive;
import com.aeye.face.AEFaceInterface;
import com.aeye.face.AEFacePack;
import com.aeye.face.AEFaceParam;
import com.aeye.face.AEFaceSdk;
import com.aeye.face.callback.AEFaceCallbackHelper;
import com.aeye.face.ui.FaceImmersiveStatusBar;
import com.aeye.face.config.IDConstants;
import com.aeye.face.camera.PreviewFrameCache;
import com.aeye.face.camera.CameraConfig;
import com.aeye.face.camera.CameraManager;
import com.aeye.face.camera.CameraManagerLight;
import com.aeye.face.camera.CaptureActivityHandler;
import com.aeye.face.camera.CaptureActivityHandlerLight;
import com.aeye.face.api.ThunderAliveApi;
import com.aeye.face.lightView.BitmapView;
import com.aeye.face.lightView.CheckFaceView;
import com.aeye.face.uitls.AudioUtils;
import com.aeye.face.uitls.ColorInfo;
import com.aeye.face.uitls.DataUtil;
import com.aeye.face.uitls.DeviceSafeCheckUtils;
import com.aeye.face.uitls.FLogUtil;
import com.aeye.face.uitls.FileUtil;
import com.aeye.face.uitls.MLog;
import com.aeye.face.uitls.PictureManagerUtils;
import com.aeye.face.uitls.PictureManagerUtilsLight;
import com.aeye.face.uitls.SMUtil;
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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

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
    /** 人脸状态标志：-1无人脸，0不确定，1有人脸；解码线程写、主线程读 **/
    private volatile int mFaceOK = 0;
    /** 当前提示文本ID（质量/环境提示状态，与 {@link #mDisplayedCheckHintResId} 分离） **/
    int textId = -1;
    /** {@link #tvCheckHint} 当前展示的 string 资源 id，用于避免重复 setText 闪烁 */
    private int mDisplayedCheckHintResId = 0;
    /** {@link #showHint(String, int)} 当前展示的文案 key，无人脸/太远是逐帧触发的，需要去重 */
    private String mDisplayedCheckHintKey = null;
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
    /** 是否启用扫描环圆外白底遮罩（竖屏白底模式） */
    private boolean mRingHoleUiEnabled;
    /** 定格最后一帧预览 **/
    private ImageView ivPreviewFreeze;
    private boolean mPreviewFrozen;
    private Bitmap mFrozenPreviewBitmap;
    /** 失败详情提示 **/
    private TextView tvFailDetail;
    /** 失败重试按钮 **/
    private Button btnFailRetry;
    /** 失败其他方式按钮 **/
    private Button btnFailOther;
    /** 提示文本 **/
    private TextView tvHint;
    /** 检测态蓝弧绕圆旋转（Choreographer / ValueAnimator，避免 Handler 每 60ms 往主队列塞消息） */
    private ValueAnimator mRingRotateAnimator;
    /** 无人脸时延迟隐藏蓝弧，避免动作切换瞬间误停动画 */
    private static final long HIDE_SCAN_ARC_DEBOUNCE_MS = 280L;
    private final Runnable hideScanArcRunnable = this::hideScanArcIfIdle;

    /** 成功页 2s 后回调：在 onDestroy 中移除，避免界面已销毁仍触发 */
    private Runnable mSuccessFinishRunnable;
    /** 取景页内联成功态（绿环 + 底部验证通过） */
    private boolean mInPlaceSuccessUi;
    /** 活体动作已完成，正在调用人脸核验接口 */
    private boolean mFaceVerifying;
    /** 「人脸核验中」UI 兜底超时（仅 faceIdent），需 ≥ FaceVerifyManager 整体超时 */
    private static final long FACE_VERIFY_UI_TIMEOUT_MS = 50_000L;
    /**
     * 炫彩路径：先 thunderAliveCheck（可达 60s）再 faceIdent。
     * 若仍用 25s，会在 Thunder 未返回时就被 UI 判超时，表现为「thunderAliveCheck 一直超时」。
     */
    private static final long FACE_VERIFY_UI_TIMEOUT_WITH_THUNDER_MS = 90_000L;
    /**
     * 核验通过后停留时长：需 &gt; 蓝弧扫满动画 420ms，保证「核验通过」绿环完整可见后再回调关闭。
     * 炫彩模式回调后会立即 finish（不像动作模式还要异步序列化大 JSON 拖住时间），
     * 若沿用过短的 200ms，绿环还没扫完 Activity 就关了，表现为「炫彩没有核验通过圆弧」。
     */
    private static final long SUCCESS_RING_HOLD_MS = 650L;
    private Runnable mVerifyTimeoutRunnable;
    private ValueAnimator mRingSuccessAnimator;
    /** 取景页内联失败/超时态（粉环 + 红色提示 + 底部按钮） */
    private boolean mInPlaceFailUi;
    private boolean mInPlaceFailIsTimeout;
    /** true=faceIdent 提交阶段失败；false=活体检测失败 */
    private boolean mInPlaceFailIsSubmit;
    private long mNoFaceSinceElapsedMs = -1L;
    private Runnable mNoFaceFailRunnable;
    /** 是否已上报二维码终态（4 未通过 / 5 已通过 / 2 异常退出） */
    private boolean mQrRecordFinalized;
    /** 本会话动作活体未通过次数；满 3 次上报 isPass=4 + failedType=1 */
    private int mLivenessFailCount;

    // ========== 炫彩活体（LIGHT / MOTION_LIGHT）运行时状态 ==========
    /** 用于界面颜色变换成功后颜色值设置，此颜色值用于传入 so */
    public static final int MSG_CODE_COLOR_INDEX_UPDATE = 1021;
    public static final int MSG_CODE_AUDIO_DELAY = 1022;
    public static final int MSG_CODE_PREPARE_DELAY = 1023;
    private static final int MSG_CODE_START = 1001;
    private static final int MSG_CODE_UPDATE = 1005;
    private static final int MSG_CODE_SAVE = 1007;
    private static final int MSG_CODE_END = 1010;
    private static final int MSG_CODE_FINISH = 1020;

    public CaptureActivityHandlerLight lightHandler;
    private CheckFaceView faceBgView;
    private List<ColorInfo> colorInfoList = new ArrayList<>();
    private ArrayList<Integer> colorForSoList = new ArrayList<>();
    private volatile int currentR, currentG, currentB;
    private volatile boolean isSave = false;
    private volatile boolean isAeyeLightInit = false;
    /** 炫彩 AliveInit 正在子线程执行，避免 surface 回调重复触发 */
    private volatile boolean mLightInitInFlight = false;
    private long startRecogTime = 0;
    private String lastShowMessage = "";
    private String mPendingLightJson;
    /** 当前「丢脸/人脸过小」事件是否已重置过色序，配合解码线程的逐帧回调做去重 */
    private volatile boolean mLightSeqResetDone = false;
    /** 炫彩色光序列是否已启动（纯炫彩需检测到人脸后再启动，避免帧与色序错位） */
    private volatile boolean mLightFlashStarted = false;
    /** 纯炫彩：人脸稳定后再延迟启动色光，避免入框瞬间就开始闪光 */
    private static final long LIGHT_FLASH_SETTLE_MS = 600L;
    /** 延迟启动色光是否已排队；解码线程逐帧调用，必须幂等，否则每帧重排会导致永远等不到 */
    private volatile boolean mBeginFlashScheduled = false;

    public static volatile int currentIndex;
    public static volatile int colorForSo;
    public static volatile boolean isRecord = false, isGetLastBitmap = false;
    public static boolean isFinish = false;
    public static int isChangeColor = 0;
    public static int screenWidth = 0;
    public static int screenHeight = 0;
    public final static int mPreviewWidth = 1080;
    public final static int mPreviewHeight = 1920;
    private static int splitTime = 1000;
    private static int[] picNumber = new int[6];
    private static boolean isFirstHasFace = false;
    private static int aliveMode = AEFaceParam.ALIVEMODE_MOTION;
    private static int pose = AEFaceAlive.POSE_EYE_BLINK;
    /** CheckFaceView 读取的人脸状态镜像 */
    private static int sFaceOK = 0;

    public static int getSplitTime() {
        return splitTime;
    }

    public static int getCurrentIndex() {
        return currentIndex;
    }

    public static int getColorForSo() {
        return colorForSo;
    }

    public ArrayList<Integer> getColorForSoList() {
        return colorForSoList;
    }

    public static int getPicNumber(int key) {
        if (key < picNumber.length) {
            return picNumber[key];
        }
        return 0;
    }

    public static void addPicNumber(int currentColor, int mPicCount) {
        Log.e(TAG, "addPicNumber****************count : " + mPicCount + " index : " + currentColor);
        if (currentColor >= 0 && currentColor < picNumber.length) {
            picNumber[currentColor] = mPicCount;
        }
    }

    public static void clearPicNumber() {
        for (int i = 0; i < 6; i++) {
            picNumber[i] = 0;
        }
    }

    public static boolean isIsFirstHasFace() {
        return isFirstHasFace;
    }

    public static void setIsFirstHasFace(boolean isFirstHasFace) {
        RecognizeActivity.isFirstHasFace = isFirstHasFace;
    }

    public static int getAliveMode() {
        return aliveMode;
    }

    public static void setAliveMode(int aliveMode) {
        RecognizeActivity.aliveMode = aliveMode;
    }

    public static int getPose() {
        return pose;
    }

    public static void setPose(int pose) {
        RecognizeActivity.pose = pose;
    }

    public static int getmFaceOK() {
        return sFaceOK;
    }

    public long getStartRecogTime() {
        return startRecogTime;
    }

    public CaptureActivityHandlerLight getLightHandler() {
        return lightHandler;
    }

    /** 炫彩色光序列是否正在进行（供解码线程判断是否可采集） */
    public boolean isLightFlashStarted() {
        return mLightFlashStarted;
    }

    public boolean isLightAliveMode() {
        return aliveMode == AEFaceParam.ALIVEMODE_LIGHT
                || aliveMode == AEFaceParam.ALIVEMODE_MOTION_LIGHT;
    }

    /**
     * Activity 生命周期 onCreate
     * 初始化视图、绑定控件、获取配置参数并初始化摄像头管理
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        FaceImmersiveStatusBar.install(this);
        super.onCreate(savedInstanceState);
        // 不使用 FLAG_SECURE：华为 SurfaceView + Choreographer 会泄漏/重复拆除
        // sync barrier，表现为检测页卡死无法返回，或
        // IllegalStateException: synchronization barrier token has already been removed。
        if(AEFacePack.getInstance().isLand()) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }else{
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        setContentView(R.layout.aeye_recognize);
        FaceImmersiveStatusBar.bindToolbar(this,
                findViewById(R.id.face_toolbar),
                findViewById(R.id.face_toolbar_gap),
                findViewById(R.id.face_chrome_toolbar_gap));

        if (getIntent().hasExtra(AEFaceParam.ALIVEMODE)) {
            aliveMode = getIntent().getIntExtra(AEFaceParam.ALIVEMODE, AEFaceParam.ALIVEMODE_MOTION);
        }
        isGetLastBitmap = false;
        setIsFirstHasFace(false);
        faceBgView = findViewById(R.id.facebgview);
        if (isLightAliveMode()) {
            initColorFromServer();
            clearPicNumber();
            if (aliveMode == AEFaceParam.ALIVEMODE_MOTION_LIGHT) {
                AEFaceAlive.getInstance().AEYE_Alive_setAliveParamVIS(1,
                        AEFacePack.getInstance().getAliveLevel());
                SecureRandom secureRandom = new SecureRandom();
                pose = (secureRandom.nextInt(2) == 0)
                        ? AEFaceAlive.POSE_EYE_BLINK
                        : AEFaceAlive.POSE_MOUTH_OPEN;
            }
        }

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
        ivPreviewFreeze = findViewById(R.id.iv_preview_freeze);
        mRingHoleUiEnabled = AEFacePack.getInstance().isWhiteBackgroud()
                && !AEFacePack.getInstance().isLand();
        if (scanRingMain != null) {
            scanRingMain.setHoleMaskEnabled(mRingHoleUiEnabled);
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
        if (isLightAliveMode()) {
            CameraManagerLight.init(getApplication());
        }

        hasSurface = false;
        initData();
        m_hasFinishReturn = false;
        AEFacePack.getInstance().registerFaceFlowActivity(this);

        ivReturn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleUserExit();
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
                    applyCheckHintText((int) msg.obj, msg.arg1);
                    break;
                case UI_MSG_HINT_TEXT_SHOW:
                    if (mInPlaceFailUi || mInPlaceSuccessUi) {
                        break;
                    }
                    String text = (String) msg.obj;
                    int hintColorKind = msg.arg1;
                    if (text != null && text.equals(mDisplayedCheckHintKey)
                            && tvCheckHint.getVisibility() == View.VISIBLE) {
                        break;
                    }
                    mDisplayedCheckHintKey = text;
                    if (tvCheckHint.getVisibility() != View.VISIBLE) {
                        tvCheckHint.setVisibility(View.VISIBLE);
                    }
                    clearCheckHintLeadingIcon();
                    tvCheckHint.setTextColor(ContextCompat.getColor(RecognizeActivity.this,
                            hintColorResId(hintColorKind)));
                    int shownResId = 0;
                    switch (text) {
                        case "aeye_quality_out":
                        case "quality_out":
                            shownResId = R.string.aeye_quality_out;
                            tvCheckHint.setText(shownResId);
                            break;
                        case "aeye_camera_notice":
                            Log.e(TAG, "请正视摄像头  showhint");
                            shownResId = R.string.aeye_camera_notice;
                            tvCheckHint.setText(shownResId);
                            break;
                        case "face_far":
                            shownResId = R.string.aeye_face_far;
                            tvCheckHint.setText(shownResId);
                            break;
                        case "aeye_eye_blink":
                            shownResId = R.string.aeye_face_blick;
                            tvCheckHint.setText(shownResId);
                            break;
                        case "aeye_mouth":
                            shownResId = R.string.aeye_face_mouth;
                            tvCheckHint.setText(shownResId);
                            break;
                        case "keep":
                            tvCheckHint.setText("屏幕即将闪烁，请保持姿势不动");
                            shownResId = 0;
                            break;
                        case "keep_face":
                            tvCheckHint.setText("请保持姿势不动");
                            shownResId = 0;
                            break;
                    }
                    mDisplayedCheckHintResId = shownResId;
                    break;
                case UI_MSG_HINT_HIDE:
                    tvCheckHint.setVisibility(View.GONE);
                    mDisplayedCheckHintResId = 0;
                    mDisplayedCheckHintKey = null;
                    break;
                case UI_MSG_MESSAGE_BOX:
                    noteLivenessFailForQrRecord();
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
        final Window window = getWindow();
        if (window == null) {
            return;
        }
        // 避开 Choreographer.doTraversal 期间改窗口属性，否则华为上可能
        // removeSyncBarrier 时 token 已失效而崩溃。
        window.getDecorView().post(() -> {
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.screenBrightness = brightness / 255.0f;
            window.setAttributes(lp);
        });
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
        editor.apply();
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
        int animId, audioId, hintResId;
        try {
            hintResId = resolvePoseHintTextId(id);
            audioId = resolvePoseHintAudioId(id);
        } catch (Exception e) {
            audioId = 0;
            animId = 0;
            Log.e("AEYE", "m_Afd == null " + e.toString());
            return;
        }

        anim = true;
        if (Looper.myLooper() == Looper.getMainLooper()) {
            applyCheckHintText(hintResId, HINT_COLOR_THEME);
        } else {
            Message msg = new Message();
            msg.what = UI_MSG_HINT_SHOW;
            msg.obj = hintResId;
            msg.arg1 = HINT_COLOR_THEME;
            mUIHandler.sendMessage(msg);
        }

		
		/*ivMovie.setImageResource(animId);
		AnimationDrawable animationDrawable = (AnimationDrawable) ivMovie.getDrawable();
		if (AEFacePack.getInstance().isModelAllSide() &&
				AEFacePack.getInstance().isAliveOff()) {
			animationDrawable.setOneShot(true);
		}
		animationDrawable.start();*/
        if (voice && voiceTriggle) {
            final Handler decodeCtl = isLightAliveMode() ? lightHandler : handler;
            if (decodeCtl != null) {
                if (decodeCtl instanceof CaptureActivityHandler) {
                    ((CaptureActivityHandler) decodeCtl).pauseDecode();
                } else if (decodeCtl instanceof CaptureActivityHandlerLight) {
                    ((CaptureActivityHandlerLight) decodeCtl).pauseDecode();
                }
                decodeCtl.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (decodeCtl instanceof CaptureActivityHandler) {
                            ((CaptureActivityHandler) decodeCtl).resumeDecode();
                        } else if (decodeCtl instanceof CaptureActivityHandlerLight) {
                            ((CaptureActivityHandlerLight) decodeCtl).resumeDecode();
                        }
                    }
                }, 300);
            }
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
        if (isLightAliveMode()) {
            return;
        }
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
        if (isLightAliveMode()) {
            screenWidth = w;
            screenHeight = h;
        }
//        float rate = 1.5f;
        float rate = 1.2f;

        if(AEFacePack.getInstance().isLand() ){
            if(w>1200) {
                rate = 2.5f;
            }else  if(w<1199){
                rate = 1.5f;
            }
        }


        // 动作相机约 640×480 → 竖屏 3:4；炫彩强制 1920×1080 → 竖屏 9:16。
        // Surface 比例必须与相机帧一致，否则会 fitXY 拉伸；分辨率仍保持算法所需的 1080×1920。
        float previewHeightOverWidth = isLightAliveMode() ? (16f / 9f) : (4f / 3f);
        int targetW = (int) (w / rate);
        int targetH = Math.round(targetW * previewHeightOverWidth);

        if (AEFacePack.getInstance().isWhiteBackgroud() && !AEFacePack.getInstance().isLand()) {
            int panelPx = getResources().getDimensionPixelSize(R.dimen.face_preview_panel_size);
            int[] previewSize = ScanRingOverlayView.computePortraitPreviewCoverSize(
                    this, panelPx, previewHeightOverWidth);
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

    /** 停止预览并显示缓存的最后一帧（成功/失败/超时）。 */
    private void freezePreviewFrame() {
        if (mPreviewFrozen) {
            return;
        }
        mPreviewFrozen = true;
        try {
            if (isLightAliveMode()) {
                CameraManagerLight.get(this).stopPreview();
            } else {
                CameraManager.get(this).stopPreview();
            }
        } catch (Exception ignored) {
        }
        if (!PreviewFrameCache.hasFrame()) {
            return;
        }
        new Thread(() -> {
            final Bitmap bitmap = PreviewFrameCache.toBitmap();
            runOnUiThread(() -> {
                if (isFinishing() || !mPreviewFrozen) {
                    if (bitmap != null && !bitmap.isRecycled()) {
                        bitmap.recycle();
                    }
                    return;
                }
                releaseFrozenPreviewBitmap();
                mFrozenPreviewBitmap = bitmap;
                if (ivPreviewFreeze != null && bitmap != null) {
                    ivPreviewFreeze.setImageBitmap(bitmap);
                    ivPreviewFreeze.setVisibility(View.VISIBLE);
                }
            });
        }, "AEFace-FreezePreview").start();
    }

    /** 重新核验时恢复实时预览。 */
    private void unfreezePreviewFrame() {
        if (!mPreviewFrozen) {
            return;
        }
        mPreviewFrozen = false;
        if (ivPreviewFreeze != null) {
            ivPreviewFreeze.setVisibility(View.GONE);
        }
        releaseFrozenPreviewBitmap();
    }

    private void releaseFrozenPreviewBitmap() {
        if (mFrozenPreviewBitmap != null && !mFrozenPreviewBitmap.isRecycled()) {
            mFrozenPreviewBitmap.recycle();
        }
        mFrozenPreviewBitmap = null;
        if (ivPreviewFreeze != null) {
            ivPreviewFreeze.setImageBitmap(null);
        }
    }

    public boolean isCollect() {
        return collect;
    }

    private void prepareRecog() {
        if (isLightAliveMode()) {
            prepareRecogLight();
            return;
        }
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

    /** 炫彩：准备识别后直接开始，无 3-2-1 倒计时 */
    private void prepareRecogLight() {
        // 必须先 AliveInit，再 startPreview/解码；否则 SetImageData 会因 not init 崩溃
        mLightSeqResetDone = false;
        resetLightSessionFlags();
        if (!isAeyeLightInit) {
            lightInit();
        }
        // 炫彩 native（LightAlive）在某些设备 ABI（如 arm64-v8a）下可能缺失 so，
        // 为避免 FATAL crash，这里检查初始化结果并按现有失败 UI 结束流程。
        if (!isAeyeLightInit) {
            // 多数为宿主 APK 含 arm64-v8a 其它 .so，进程走 64 位，但本 SDK 仅有 32 位 LightAlive.so
            finishActivityByOther(AEFacePack.ERROR_FAIL,
                    "当前设备不支持炫彩活体（缺少可用的 LightAlive.so，请确认宿主未以纯 arm64 进程运行，或提供 arm64-v8a 版 so）");
            return;
        }
        if (lightHandler != null) {
            lightHandler.startPreview();
        }
        tvRecogTimeCountdown.setVisibility(View.INVISIBLE);
        if (tvEnvHint != null) {
            tvEnvHint.setVisibility(View.INVISIBLE);
        }
        if (ivNumber != null) {
            ivNumber.setVisibility(View.GONE);
        }
        showAlivePose(0, aliveMode != AEFaceParam.ALIVEMODE_LIGHT, false);
        showHint("aeye_camera_notice", HINT_COLOR_THEME);
        int voiceId = R.raw.aeye_face;
        if (aliveMode == AEFaceParam.ALIVEMODE_LIGHT) {
            voiceId = R.raw.aeye_face;
        } else if (pose == AEFaceAlive.POSE_EYE_BLINK) {
            voiceId = R.raw.aeye_eye;
            showHint("aeye_eye_blink", HINT_COLOR_THEME);
        } else {
            voiceId = R.raw.aeye_mouth;
            showHint("aeye_mouth", HINT_COLOR_THEME);
        }
        if (voiceTriggle && voiceId != 0 && curVoice != voiceId) {
            AudioUtils.playVoiceIdle(RecognizeActivity.this, voiceId);
        }
        startRecog();
        showFaceStatus(true, true);
    }

    /**
     * 开始人脸核验和活体检测
     * 重启摄像头预览和解码任务，并启动相应的超时定时器
     */
    private void startRecog() {
        if (isLightAliveMode()) {
            startRecogLight();
            return;
        }
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
        prepareScanRingOverlay();
    }

    private void startRecogLight() {
        // 颜色序列（currentIndex / colorForSo / timeHandler 队列）必须单线程推进。
        // showNoFace 由解码线程回调，若直接在此重置，可能与主线程正在执行的
        // MSG_CODE_UPDATE 交错，产生两条并行的变色链，导致色序与帧号错位。
        if (Looper.myLooper() != Looper.getMainLooper()) {
            runOnUiThread(this::startRecogLight);
            return;
        }
        // 只清会话标志，禁止在此处 AliveDestroy：prepareRecogLight 已完成 Init，
        // 若再 Destroy，解码线程调用 SetImageData 会直接 FATAL。
        resetLightSessionFlags();
        if (!isAeyeLightInit || !AEyeLightAlive.getInstance().isInitialized()) {
            lightInit();
        }
        if (!isAeyeLightInit) {
            finishActivityByOther(AEFacePack.ERROR_FAIL, "炫彩算法未初始化");
            return;
        }
        if (lightHandler != null) {
            lightHandler.resetData();
        }
        initFirstColor();
        if (ivNumber != null) {
            ivNumber.setVisibility(View.GONE);
        }
        if (lightHandler != null) {
            lightHandler.restartPreviewAndDecode();
        }
        PictureManagerUtilsLight.getPictureManager().resetPictureManager();
        // 纯炫彩等人脸入框后再闪光；动作+炫彩等动作通过后再闪光。
        // 两种模式在闪光开始前都保持 isRecord=false，避免解码线程提前插帧。
        isRecord = false;
        mLightFlashStarted = false;
    }

    /**
     * 启动炫彩色光序列（白→三色→黑）。纯炫彩在人脸稳定后调用；动作+炫彩在动作通过后调用。
     */
    private void beginLightFlashSequence() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            runOnUiThread(this::beginLightFlashSequence);
            return;
        }
        if (mLightFlashStarted || isFinishing() || m_hasFinishReturn || !isLightAliveMode()) {
            return;
        }
        cancelScheduledBeginFlash();
        // 先置 isRecord，再放开闪光：避免解码线程看到「已闪光但未录制」后走最后一帧路径，
        // 此时 cacheBeanArrayList 仍为空会 IndexOutOfBoundsException。
        isRecord = true;
        isGetLastBitmap = false;
        isSave = false;
        mLightFlashStarted = true;
        mLightSeqResetDone = false;
        showFaceBgForFlash();
        initFirstColor();
        timeHandler.removeMessages(MSG_CODE_UPDATE);
        timeHandler.removeMessages(MSG_CODE_START);
        timeHandler.removeMessages(MSG_CODE_END);
        timeHandler.removeMessages(MSG_CODE_SAVE);
        timeHandler.removeMessages(MSG_CODE_COLOR_INDEX_UPDATE);
        clearPicNumber();
        PictureManagerUtilsLight.getPictureManager().resetPictureManager();
        if (lightHandler != null) {
            lightHandler.resetColorTracking();
        }
        timeHandler.sendEmptyMessage(MSG_CODE_START);
        Log.d(TAG, "beginLightFlashSequence: mode=" + aliveMode);
    }

    private final Runnable mBeginFlashRunnable = new Runnable() {
        @Override
        public void run() {
            mBeginFlashScheduled = false;
            if (isFinishing() || m_hasFinishReturn || mLightFlashStarted) {
                return;
            }
            if (mFaceOK <= 0) {
                return;
            }
            beginLightFlashSequence();
        }
    };

    /** 纯炫彩：人脸入框后稍等再启动色光，减少入框瞬间帧与首屏色不同步 */
    private void scheduleBeginLightFlash() {
        if (aliveMode != AEFaceParam.ALIVEMODE_LIGHT || mLightFlashStarted || mBeginFlashScheduled) {
            return;
        }
        mBeginFlashScheduled = true;
        mUIHandler.postDelayed(mBeginFlashRunnable, LIGHT_FLASH_SETTLE_MS);
    }

    private void cancelScheduledBeginFlash() {
        mBeginFlashScheduled = false;
        mUIHandler.removeCallbacks(mBeginFlashRunnable);
    }

    /** 人脸丢失/过远：暂停色光，等人脸恢复后再从头采集，避免色序与帧继续错位 */
    private void pauseLightFlashSequence() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            runOnUiThread(this::pauseLightFlashSequence);
            return;
        }
        if (!mLightFlashStarted) {
            cancelScheduledBeginFlash();
            return;
        }
        cancelScheduledBeginFlash();
        mLightFlashStarted = false;
        hideFaceBgFlash();
        timeHandler.removeMessages(MSG_CODE_UPDATE);
        timeHandler.removeMessages(MSG_CODE_START);
        timeHandler.removeMessages(MSG_CODE_END);
        timeHandler.removeMessages(MSG_CODE_SAVE);
        timeHandler.removeMessages(MSG_CODE_COLOR_INDEX_UPDATE);
        isRecord = false;
        isGetLastBitmap = false;
        isSave = false;
        initFirstColor();
        clearPicNumber();
        PictureManagerUtilsLight.getPictureManager().resetPictureManager();
        if (lightHandler != null) {
            lightHandler.resetColorTracking();
        }
        Log.d(TAG, "pauseLightFlashSequence");
    }

    /**
     * 清掉上轮炫彩会话标志（isGetLastBitmap / picNumber 等），不 Destroy native。
     * Destroy + 再 Init 只在「重新核验」入口 {@link #destroyLightAliveForReinit()} 做。
     */
    private void resetLightSessionFlags() {
        isGetLastBitmap = false;
        isRecord = false;
        isSave = false;
        isFinish = false;
        mPendingLightJson = null;
        currentIndex = 0;
        mLightFlashStarted = false;
        cancelScheduledBeginFlash();
        clearPicNumber();
        setIsFirstHasFace(false);
        timeHandler.removeMessages(MSG_CODE_UPDATE);
        timeHandler.removeMessages(MSG_CODE_START);
        timeHandler.removeMessages(MSG_CODE_END);
        timeHandler.removeMessages(MSG_CODE_SAVE);
        timeHandler.removeMessages(MSG_CODE_COLOR_INDEX_UPDATE);
    }

    /** 重新核验前销毁 native，迫使 prepareRecogLight 重新 AliveInit */
    private void destroyLightAliveForReinit() {
        if (!isAeyeLightInit && !AEyeLightAlive.getInstance().isInitialized()) {
            return;
        }
        try {
            AEyeLightAlive.getInstance().AEYE_AliveDestroy();
        } catch (Throwable t) {
            Log.w(TAG, "destroyLightAliveForReinit: " + t.getMessage());
        }
        isAeyeLightInit = false;
        mLightInitInFlight = false;
    }

    private void initFirstColor() {
        currentIndex = 0;
        // 首屏常为白光；先写入 RGB，避免闪光层刚显示时字色仍按 0 算成白字
        currentR = 255;
        currentG = 255;
        currentB = 255;
        colorForSo = (currentB << 16) + (currentG << 8) + currentR;
    }

    private void showFaceBgForFlash() {
        if (faceBgView == null) {
            return;
        }
        if (Looper.myLooper() != Looper.getMainLooper()) {
            runOnUiThread(this::showFaceBgForFlash);
            return;
        }
        syncFlashHoleToPreviewPanel();
        faceBgView.setVisibility(View.VISIBLE);
        // 闪光时关掉圆外白底，避免盖住色光；扫描环本身在 chrome 层仍可见
        if (scanRingMain != null) {
            scanRingMain.setHoleMaskEnabled(false);
            scanRingMain.setVisibility(View.VISIBLE);
        }
        // 文案浮层 / 顶栏叠在闪光之上；闪光底色较深时用白色文字保证可读
        View chrome = findViewById(R.id.face_chrome_overlay);
        if (chrome != null) {
            chrome.bringToFront();
        }
        applyFlashChromeTextColors(true);
        View toolbar = findViewById(R.id.face_toolbar);
        if (toolbar != null) {
            toolbar.bringToFront();
        }
        View introduce = findViewById(R.id.introduceView);
        if (introduce != null) {
            introduce.bringToFront();
        }
    }

    /** 闪光阶段按当前闪光亮度选字色；结束后恢复主题色 */
    private void applyFlashChromeTextColors(boolean flashing) {
        int color;
        if (flashing) {
            color = resolveFlashChromeTextColor();
        } else {
            color = ContextCompat.getColor(this, R.color.face_theme_primary);
        }
        if (tvHint != null) {
            tvHint.setTextColor(color);
        }
        if (tvCheckHint != null && !mInPlaceFailUi && !mInPlaceSuccessUi) {
            tvCheckHint.setTextColor(color);
        }
    }

    /**
     * 浅色闪光（白/黄等）用深蓝字，深色闪光用白字，保证提示可读。
     */
    private int resolveFlashChromeTextColor() {
        // 相对亮度（ITU-R BT.601）
        double luminance = 0.299 * currentR + 0.587 * currentG + 0.114 * currentB;
        if (luminance >= 180) {
            return ContextCompat.getColor(this, R.color.face_theme_primary);
        }
        return ContextCompat.getColor(this, R.color.white);
    }

    /**
     * 将炫彩镂空对齐到方形取景面板的预览圆孔，消除「黑大圆 + 白方块」错位。
     */
    private void syncFlashHoleToPreviewPanel() {
        if (faceBgView == null) {
            return;
        }
        View panel = findViewById(R.id.face_preview_panel);
        if (panel == null || panel.getWidth() <= 0 || panel.getHeight() <= 0) {
            faceBgView.post(this::syncFlashHoleToPreviewPanel);
            return;
        }
        int bgW = faceBgView.getWidth();
        int bgH = faceBgView.getHeight();
        if (bgW <= 0 || bgH <= 0) {
            Display display = getWindowManager().getDefaultDisplay();
            bgW = display.getWidth();
            bgH = display.getHeight();
        }
        int[] panelLoc = new int[2];
        panel.getLocationOnScreen(panelLoc);
        int[] bgLoc = new int[2];
        faceBgView.getLocationOnScreen(bgLoc);
        // View 尚未 layout 时 getLocationOnScreen 可能为 0，按全屏覆盖估算
        int bgLeft = bgLoc[0];
        int bgTop = bgLoc[1];
        if (faceBgView.getVisibility() != View.VISIBLE && bgLeft == 0 && bgTop == 0
                && faceBgView.getWidth() <= 0) {
            bgLeft = 0;
            bgTop = 0;
        }
        float holeR = ScanRingOverlayView.computePreviewHoleRadius(this, panel.getWidth());
        float cx = (panelLoc[0] - bgLeft) + panel.getWidth() / 2f;
        float cy = (panelLoc[1] - bgTop) + panel.getHeight() / 2f;
        RectF hole = new RectF(cx - holeR, cy - holeR, cx + holeR, cy + holeR);
        faceBgView.setPreviewHole(hole);
    }

    private void hideFaceBgFlash() {
        if (faceBgView == null) {
            return;
        }
        if (Looper.myLooper() != Looper.getMainLooper()) {
            runOnUiThread(this::hideFaceBgFlash);
            return;
        }
        faceBgView.clearPreviewHole();
        faceBgView.setVisibility(View.GONE);
        if (scanRingMain != null) {
            scanRingMain.setHoleMaskEnabled(mRingHoleUiEnabled);
        }
        applyFlashChromeTextColors(false);
    }

    /**
     * 根据摄像头ID开启摄像头、初始CaptureActivityHandler(处理编码结果、网络请求超时结果)<BR/>
     * 预览Act里初始化一个CaptureActivityHandler来处理解码的消息<BR/>
     * CaptureActivityHandler里初始一个DecodeThread线程,该线程包括一个DecodeHandler处理图片解码消息
     * DecodeHandler处理后最终给CaptureActivityHandler发送消息处理
     */
    private void initCamera(SurfaceHolder surfaceHolder) {
        if (isLightAliveMode()) {
            initCameraLight(surfaceHolder);
            return;
        }
        // Camera1 openDriver（Camera.open + setParameters）在部分机型上可能阻塞数秒~数十秒，
        // 放主线程会触发「Application does not have a focused window」ANR，改为子线程打开，
        // 打开成功后再回主线程建 Handler 并进入取景。相机回调线程不变（子线程无 Looper，
        // 帧/对焦回调仍投递到主 Looper）。
        openCameraAsync(surfaceHolder, false);
    }

    private void initCameraLight(SurfaceHolder surfaceHolder) {
        openCameraAsync(surfaceHolder, true);
    }

    /** 在子线程打开相机并 startPreview，成功后回主线程创建 Handler。避免 Camera.startPreview 卡住 UI。 */
    private void openCameraAsync(final SurfaceHolder surfaceHolder, final boolean light) {
        new Thread(() -> {
            try {
                if (light) {
                    CameraManagerLight.get(this).openDriver(surfaceHolder, cameraDirection);
                } else {
                    CameraManager.get(this).openDriver(surfaceHolder, cameraDirection);
                    CameraManager.get(this).startPreview();
                }
            } catch (Throwable e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    if (isFinishing()) {
                        return;
                    }
                    String str = getApplication().getString(R.string.aeye_camera_error);
                    finishActivityByOther(AEFacePack.ERROR_CAMERA, str);
                });
                return;
            }
            if (light) {
                runOnUiThread(() -> continueAfterCameraOpenLight());
            } else {
                runOnUiThread(() -> continueAfterCameraOpenMotion());
            }
        }, "aeye-camera-open").start();
    }

    private void continueAfterCameraOpenMotion() {
        if (isFinishing()) {
            return;
        }
        if (handler == null) {
            handler = new CaptureActivityHandler(this);
        }
        if (!introduceTriggle) {
            prepareRecog();
        }
    }

    /**
     * 炫彩：mask 在主线程画完后，AliveInit + startPreview 放到子线程，避免进页卡 1~2 秒。
     * 必须先 AliveInit 再 startPreview/解码。
     */
    private void continueAfterCameraOpenLight() {
        if (isFinishing()) {
            return;
        }
        if (lightHandler == null) {
            lightHandler = new CaptureActivityHandlerLight(this);
        }
        if (introduceTriggle) {
            return;
        }
        if (isAeyeLightInit && AEyeLightAlive.getInstance().isInitialized()) {
            prepareRecog();
            return;
        }
        if (mLightInitInFlight) {
            return;
        }
        mLightInitInFlight = true;
        final Bitmap mask = drawOvalMask();
        new Thread(() -> {
            try {
                lightInitWithMask(mask);
                if (isAeyeLightInit) {
                    try {
                        CameraManagerLight.get(this).startPreview();
                    } catch (Throwable t) {
                        Log.e(TAG, "light startPreview failed", t);
                    }
                }
            } finally {
                mLightInitInFlight = false;
            }
            runOnUiThread(() -> {
                if (isFinishing()) {
                    return;
                }
                if (!introduceTriggle) {
                    prepareRecog();
                }
            });
        }, "aeye-light-init").start();
    }

    public int getOrientation() {
        if (AEFacePack.getInstance().isSetCaptureOrientation()) {
            return AEFacePack.getInstance().getCaptureOrientation();
        } else if (isLightAliveMode()) {
            return CameraManagerLight.get(this).getOrientation(cameraDirection);
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
            setWindowBrightness(255);
        }

        Long timeout = Long.parseLong("60");
        Log.e("timeout", "" + timeout);
    }

    @Override
    /**释放锁以让屏幕可以锁屏、退出预览*/
    protected void onPause() {
        super.onPause();
        if (isLightAliveMode()) {
            try {
                CameraManagerLight.get(this).stopPreview();
                CameraManagerLight.get(this).closeDriver();
            } catch (Exception ignored) {
            }
        } else {
            CameraManager.get(this).closeDriver();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        m_WakeLock.release();

        if (handler != null) {
            handler.cancelDecodeTask();
        }
        if (lightHandler != null) {
            lightHandler.cancelDecodeTask();
        }

        if (!mFinish) {
            finishActivityByUserCancel();
        }
    }

    @Override
    /**退出预览*/
    public void onDestroy() {
        if (!mQrRecordFinalized) {
            QrRecordStatusManager.update(QrRecordStatus.ABNORMAL_EXIT,
                    QrRecordStatus.FailedType.CANCELLED);
            mQrRecordFinalized = true;
        }
        AEFacePack.getInstance().unregisterFaceFlowActivity(this);
        super.onDestroy();
        clearPicNumber();
        if (handler != null) {
            handler.quitSynchronously();
            handler = null;
        }
        if (lightHandler != null) {
            lightHandler.quitSynchronously();
            lightHandler = null;
        }
        if (countDown != null) {
            countDown.cancel();
            countDown = null;
        }
        timeHandler.removeMessages(MSG_CODE_UPDATE);
        timeHandler.removeMessages(MSG_CODE_START);
        timeHandler.removeMessages(MSG_CODE_END);
        cancelScheduledBeginFlash();
        PictureManagerUtils.destroyManager();
        PictureManagerUtilsLight.destroyManager();
        PreviewFrameCache.clear();
        releaseFrozenPreviewBitmap();
        CameraManager.unInit();
        if (isLightAliveMode()) {
            CameraManagerLight.unInit();
        }
        AudioUtils.destroyPlayer();
        stopRingProgress();
        if (mRingRotateAnimator != null) {
            mRingRotateAnimator.cancel();
            mRingRotateAnimator = null;
        }
        if (mRingSuccessAnimator != null) {
            mRingSuccessAnimator.cancel();
            mRingSuccessAnimator = null;
        }
        if (mSuccessFinishRunnable != null) {
            mUIHandler.removeCallbacks(mSuccessFinishRunnable);
            mSuccessFinishRunnable = null;
        }
        cancelVerifyTimeout();
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
                // 丢脸时必须刷新底部文案，否则会一直停留在上一条（如「请保持姿势不动」）
                showHint("quality_out", HINT_COLOR_THEME);
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
                if (handler != null && !isShowingCurrentPoseHint()) {
                    handler.flashDisplay(false, false);
                }
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
        // 动作间隔不再展示「很好！请再次正视摄像头」
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
            mDisplayedCheckHintResId = 0;
            mDisplayedCheckHintKey = null;
        }
    }

    /** 当前活体动作通过：不再展示「很好！请再次正视摄像头」类中间提示，直接进入下一动作。 */
    public void showPoseStepPassedBriefly() {
        // intentionally empty
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
                mDisplayedCheckHintResId = R.string.face_pose_fail_hint;
                mDisplayedCheckHintKey = null;
            }
        };
        if (Looper.myLooper() == Looper.getMainLooper()) {
            apply.run();
        } else {
            runOnUiThread(apply);
        }
    }

    private int resolvePoseHintTextId(int poseId) {
        if (!AEFacePack.getInstance().isAlivePose()) {
            return R.string.aeye_camera_notice;
        }
        if (poseId == AEFaceAlive.POSE_FACE_UP) {
            return R.string.aeye_face_up;
        }
        if (poseId == AEFaceAlive.POSE_FACE_DOWN) {
            return R.string.aeye_face_down;
        }
        if (poseId == AEFaceAlive.POSE_FACE_SHAKE) {
            return R.string.aeye_face_shake;
        }
        if (poseId == AEFaceAlive.POSE_MOUTH_OPEN) {
            return R.string.aeye_face_mouth;
        }
        if (poseId == AEFaceAlive.POSE_EYE_BLINK) {
            return R.string.aeye_face_blick;
        }
        return R.string.aeye_camera_notice;
    }

    private int resolvePoseHintAudioId(int poseId) {
        if (!AEFacePack.getInstance().isAlivePose()) {
            return R.raw.aeye_face;
        }
        if (poseId == AEFaceAlive.POSE_FACE_UP) {
            return R.raw.aeye_up;
        }
        if (poseId == AEFaceAlive.POSE_FACE_DOWN) {
            return R.raw.aeye_down;
        }
        if (poseId == AEFaceAlive.POSE_FACE_SHAKE) {
            return R.raw.aeye_shake;
        }
        if (poseId == AEFaceAlive.POSE_MOUTH_OPEN) {
            return R.raw.aeye_mouth;
        }
        if (poseId == AEFaceAlive.POSE_EYE_BLINK) {
            return R.raw.aeye_eye;
        }
        return R.raw.aeye_face;
    }

    private boolean isShowingCurrentPoseHint() {
        Handler active = isLightAliveMode() ? lightHandler : handler;
        if (active == null || tvCheckHint == null || tvCheckHint.getVisibility() != View.VISIBLE) {
            return false;
        }
        int curPos = isLightAliveMode()
                ? lightHandler.getCurPos()
                : handler.getCurPos();
        int expectedId = resolvePoseHintTextId(curPos);
        return expectedId != 0 && mDisplayedCheckHintResId == expectedId;
    }

    private void applyCheckHintText(int textResId, int hintColorKind) {
        if (tvCheckHint == null || mInPlaceFailUi || mInPlaceSuccessUi || textResId == 0) {
            return;
        }
        if (isUpAndroid6) {
            int color = ContextCompat.getColor(this, hintColorResId(hintColorKind));
            if (mDisplayedCheckHintResId == textResId
                    && tvCheckHint.getVisibility() == View.VISIBLE
                    && tvCheckHint.getCurrentTextColor() == color
                    && tvCheckHint.getCompoundDrawables()[0] == null) {
                return;
            }
            if (tvCheckHint.getCompoundDrawables()[0] != null) {
                clearCheckHintLeadingIcon();
            }
            if (tvCheckHint.getVisibility() != View.VISIBLE) {
                tvCheckHint.setVisibility(View.VISIBLE);
            }
            if (tvCheckHint.getCurrentTextColor() != color) {
                tvCheckHint.setTextColor(color);
            }
            tvCheckHint.setText(textResId);
            mDisplayedCheckHintResId = textResId;
            mDisplayedCheckHintKey = null;
        } else {
            showShortToast(getString(textResId));
        }
    }

    private int hintColorResId(int kind) {
        // 闪光层可见时按闪光亮度选字色，避免白底白字 / 彩底蓝字看不清
        if (faceBgView != null && faceBgView.getVisibility() == View.VISIBLE) {
            double luminance = 0.299 * currentR + 0.587 * currentG + 0.114 * currentB;
            return luminance >= 180 ? R.color.face_theme_primary : R.color.white;
        }
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
            if (isLightAliveMode()) {
                if (lightHandler != null && isDecode) {
                    if (faceDisplayed) {
                        showAlivePose(lightHandler.getCurPos(), false, false);
                    } else if (isFlashing()) {
                        // 闪光阶段人脸恢复：把「请将脸移入框内 / 请靠近一点」换回闪光文案
                        showHint("keep_face", HINT_COLOR_THEME);
                    }
                    mUIHandler.sendEmptyMessage(UI_MSG_TVENVHINT_HIDE);
                }
            } else if (handler != null && isDecode) {
                if (faceDisplayed) {
                    showAlivePose(handler.getCurPos(), false, false);
                }
                mUIHandler.sendEmptyMessage(UI_MSG_TVENVHINT_HIDE);
            }
        } else {
            if (isLightAliveMode()) {
                if (lightHandler != null && isDecode) {
                    showQualityHint(QUALITY_OUT, voice);
                }
            } else if (handler != null && isDecode) {
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
        if (Looper.myLooper() != Looper.getMainLooper()) {
            runOnUiThread(() -> showFaceOut(inRange));
            return;
        }
        if (inRange) {
            mLightSeqResetDone = false;
        } else if (isLightAliveMode()) {
            pauseLightFlashSequence();
            mLightSeqResetDone = true;
        }
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
            sFaceOK = mFaceOK;
            if (isLightAliveMode() && !isIsFirstHasFace()) {
                setIsFirstHasFace(true);
            }
            if (aliveMode == AEFaceParam.ALIVEMODE_LIGHT && inRange && !mLightFlashStarted) {
                scheduleBeginLightFlash();
            }
            showFaceStatus(inRange, true);
            updateScanRingForFace(inRange);
        } else if (aliveMode == AEFaceParam.ALIVEMODE_LIGHT && inRange && !mLightFlashStarted) {
            scheduleBeginLightFlash();
        }
    }

    public void showNoFace() {
        mFaceOK = -1;
        sFaceOK = mFaceOK;
        if (isLightAliveMode()) {
            pauseLightFlashSequence();
            mLightSeqResetDone = false;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showFaceStatus(false, false);
                    updateScanRingForFace(false);
                }
            });
            return;
        }
        showFaceStatus(false, false);
        updateScanRingForFace(false);
        if (mInPlaceFailUi || mInPlaceSuccessUi) {
            return;
        }
        FaceVerifyLogManager.uploadNoFace(getApplicationContext());
        scheduleNoFaceFailAfterDelay();
    }

    /** 动作阶段通过后进入炫彩；可能由 DecodeHandlerLight 后台线程回调，需切主线程。 */
    public void setMotionAliveSuc() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            runOnUiThread(this::setMotionAliveSuc);
            return;
        }
        showHint("keep", HINT_COLOR_THEME);
        beginLightFlashSequence();
    }

    /** 炫彩闪光层是否正在展示 */
    private boolean isFlashing() {
        return faceBgView != null && faceBgView.getVisibility() == View.VISIBLE;
    }

    /**
     * 检测到人脸但人脸过小：提示靠近一点，与「无人脸」的「请将脸移入框内」区分。
     * 需在 {@link #showFaceOut(boolean)} 之后调用，否则会被 QUALITY_OUT 文案覆盖。
     */
    public void showFaceTooFar() {
        showHint("face_far", HINT_COLOR_THEME);
    }

    public void showTipAfterHasFace() {
        if (aliveMode == AEFaceParam.ALIVEMODE_LIGHT) {
            showHint("aeye_camera_notice", HINT_COLOR_THEME);
        } else if (pose == AEFaceAlive.POSE_EYE_BLINK) {
            showHint("aeye_eye_blink", HINT_COLOR_THEME);
        } else {
            showHint("aeye_mouth", HINT_COLOR_THEME);
        }
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
            noteLivenessFailForQrRecord();
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
            noteLivenessFailForQrRecord();
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
        noteLivenessFailForQrRecord();
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

    public void finishActivityByOther(final int code, final String reason) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            runOnUiThread(() -> finishActivityByOther(code, reason));
            return;
        }
        stopRingProgress();
        markQrRecordAbnormalExit();
        FaceVerifyLogManager.uploadVerifyEnd(getApplicationContext(), false,
                TextUtils.isEmpty(reason) ? String.valueOf(code) : reason);
        final AEFaceInterface listener = AEFacePack.getInstance().getInterface();
        if (listener == null || m_hasFinishReturn) {
            m_hasFinishReturn = true;
            finish();
            return;
        }
        m_hasFinishReturn = true;
        PictureManagerUtils.getPictureManager().setCode(code);
        buildLivenessJsonAsync(json -> {
            AEFaceCallbackHelper.dispatchFinish(listener, code, json, reason);
            finish();
        });
    }

    public void finishActivityByTimeOut() {
        showInPlaceFailUi(true, null);
    }

    public void finishActivityByUserCancel() {
        stopRingProgress();
        markQrRecordAbnormalExit();
        FaceVerifyLogManager.uploadVerifyEnd(getApplicationContext(), false,
                getString(R.string.aeye_user_cancel));
        final AEFaceInterface listener = AEFacePack.getInstance().getInterface();
        if (listener == null || m_hasFinishReturn) {
            finish();
            return;
        }
        m_hasFinishReturn = true;
        PictureManagerUtils.getPictureManager().setCode(AEFaceParam.CODE_ERROR_CANCEL);
        buildLivenessJsonAsync(json -> {
            AEFaceCallbackHelper.dispatchFinish(listener, AEFacePack.ERROR_CANCEL, json);
            finish();
        });
    }

    public void finishActivityBySuccessful() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            runOnUiThread(this::finishActivityBySuccessful);
            return;
        }
        boolean isHook = DeviceSafeCheckUtils.isHook(this);
        if (isHook) {
            finishActivityByOther(AEFacePack.ERROR_DANGER_DEVICE, "");
            return;
        }
        if (mInPlaceSuccessUi) {
            return;
        }
        if (isLightAliveMode()) {
            finishLightAliveSuccessful();
            return;
        }
        mFaceVerifying = true;
        stopRingProgress();
        if (handler != null) {
            handler.cancelDecodeTask();
        }

        // 本地核验模式：不调用我方人脸核验接口，活体通过即视为成功，直接回调结果与图片数组。
        if (FaceVerifySession.isLocalVerifyOnly()) {
            onFaceVerifyPassed();
            return;
        }

        showInPlaceVerifying();

        buildLivenessJsonAsync(json -> {
            if (isFinishing()) {
                return;
            }
            if (TextUtils.isEmpty(json)) {
                onFaceVerifyFailed("采集结果组装失败");
                return;
            }
            FaceVerifyManager.submit(json, new FaceVerifyManager.Callback() {
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
        });
    }

    /**
     * 炫彩活体成功：组装 light JSON，可选 Thunder 校验，再走既有 FaceVerifyManager / 成功 UI。
     */
    private void finishLightAliveSuccessful() {
        mFaceVerifying = true;
        stopRingProgress();
        if (lightHandler != null) {
            lightHandler.cancelDecodeTask();
        }
        if (faceBgView != null) {
            hideFaceBgFlash();
        }
        try {
            CameraManagerLight.get(this).stopPreview();
        } catch (Exception ignored) {
        }

        Bitmap bitmap = AEyeLightAlive.getInstance().AEYE_CurrentSetImageData(4);
        if (bitmap == null) {
            finishActivityByOther(-15, "闪光颜色获取最佳图失败！");
            return;
        }
        // 与 demo 一致：用最后一帧预览 imgRect 放大到最佳图坐标系，保证五帧对齐口径统一
        Rect previewRect = PictureManagerUtilsLight.getPictureManager().getFaceRect();
        if (previewRect == null) {
            finishActivityByOther(-16, "获取最佳图人脸失败！");
            return;
        }
        int x = previewRect.centerX() * 2;
        int y = previewRect.centerY() * 2;
        int width = previewRect.width();
        Rect scaleRect = new Rect(x - width, y - width, x + width, y + width);
        Rect[] rects = new Rect[]{scaleRect};
        Log.e(TAG, "finishLightAlive frame=4, previewRect=" + previewRect
                + ", alignRect=" + scaleRect + ", img=" + bitmap.getWidth() + "x" + bitmap.getHeight()
                + ", colorSeq=" + AEFacePack.getInstance().getColorSeq());
        float[] mLandMark;
        try {
            mLandMark = AEyeLightAlive.getInstance().getBestBitLocation(rects, bitmap);
        } catch (Exception e) {
            e.printStackTrace();
            finishActivityByOther(AEFacePack.ERROR_FAIL, "关键点获取失败");
            return;
        }
        if (mLandMark == null) {
            finishActivityByOther(AEFacePack.ERROR_FAIL, "关键点为空");
            return;
        }
        AEyeLightAlive.getInstance().insetKeyPoints(4, mLandMark);
        int ret = AEyeLightAlive.getInstance().AEYE_GetImageData();
        Log.e(TAG, " finish light getImage ret : " + ret);
        if (ret != 0) {
            finishActivityByOther(AEFacePack.ERROR_FAIL, "炫彩算法返回失败 ret=" + ret);
            return;
        }
        Bitmap alignBitmap = AEyeLightAlive.getInstance().getAlignBitmap();
        Bitmap cuesBitmap = AEyeLightAlive.getInstance().getNormalCuesBitmap();
        String encodeDataBase64 = BitmapUtils.convertIconToString(cuesBitmap);
        String alignBitBase64 = BitmapUtils.convertIconToString(alignBitmap);
        final String lightJson = PictureManagerUtilsLight.getPictureManager()
                .getJsonString(encodeDataBase64, alignBitBase64);
        if (lightJson == null) {
            finishActivityByOther(AEFacePack.ERROR_FAIL, "炫彩结果组装失败");
            return;
        }
        mPendingLightJson = lightJson;

        if (FaceVerifySession.isLocalVerifyOnly()) {
            runOnUiThread(this::onFaceVerifyPassed);
            return;
        }

        // UI 在主线程展示；Thunder 组包/请求留在当前工作线程，避免大图 base64 堵主线程。
        runOnUiThread(() -> showInPlaceVerifying(false));
        maybeThunderCheckThenVerify(lightJson, cuesBitmap);
    }

    private void maybeThunderCheckThenVerify(final String lightJson, final Bitmap cuesBitmap) {
        final String seq = AEFacePack.getInstance().getColorSeq();
        // images[] 存的是 SM4 密文；facePics 解密后用 JPEG 上传以减小体积。
        final JSONArray facePics = buildThunderPlainFacePics(lightJson);
        if (facePics == null || facePics.length() == 0) {
            runOnUiThread(() -> {
                cancelVerifyTimeout();
                mFaceVerifying = false;
                showInPlaceFailUi(false, "炫彩人脸图组装失败", true);
            });
            return;
        }
        // alivePics 必须用 PNG：cues 图含算法色序编码，JPEG 有损会破坏数据 → 服务端报「颜色序列不对」。
        // facePics 仍用 JPEG 降低体积；demo 的 alivePics 亦为 PNG（iVBORw0KGgo...）。
        final String alivePic = BitmapUtils.convertIconToString(cuesBitmap);

        // isNewColorIntenface=true：只调新 faceIdent，炫彩字段随请求提交。
        // colorPics = 算法图（与老接口 thunderAliveCheck 的 alivePics 同数据），
        // facePic1~6 = 人脸原图（与动作活体一致）。
        if (AEFaceSdk.isNewColorIntenface()) {
            final JSONArray colorPics = new JSONArray();
            colorPics.put(alivePic);
            scheduleVerifyTimeout(FACE_VERIFY_UI_TIMEOUT_MS);
            submitFaceVerifyWithColor(seq, facePics, colorPics);
            return;
        }

        // isNewColorIntenface=false：只调老接口 thunderAliveCheck 测试炫彩核验，
        // 通过即视为核验成功，不再调用 faceIdent。
        ThunderAliveApi client = AEFacePack.getInstance().ensureThunderClient();
        if (client == null || TextUtils.isEmpty(seq)
                || !AEFacePack.getInstance().hasThunderCredentials()) {
            // 无 Thunder 凭证/色序（纯本地 Demo）：无法服务端验活，直接按通过处理
            Log.w(TAG, "maybeThunderCheckThenVerify: no thunder client/seq, pass locally");
            runOnUiThread(this::onFaceVerifyPassed);
            return;
        }
        final String facePic = facePics.optString(0, "");
        scheduleVerifyTimeout(FACE_VERIFY_UI_TIMEOUT_WITH_THUNDER_MS);
        client.checkAlive(this, seq, alivePic, facePic, facePics,
                (result, resp) -> {
                    onFaceVerifyPassed();
                    return 0;
                },
                (resp, msg) -> {
                    cancelVerifyTimeout();
                    mFaceVerifying = false;
                    showInPlaceFailUi(false, TextUtils.isEmpty(msg) ? "炫彩校验失败" : msg, true);
                    return 0;
                });
    }

    /**
     * 将炫彩采集 JSON 中的 SM4 加密 images 解密为 JPEG base64 数组，供 thunderAliveCheck 上传。
     */
    private JSONArray buildThunderPlainFacePics(String lightJson) {
        JSONArray out = new JSONArray();
        if (TextUtils.isEmpty(lightJson)) {
            return out;
        }
        try {
            JSONObject obj = new JSONObject(lightJson);
            JSONArray images = obj.optJSONArray("images");
            if (images == null || images.length() == 0) {
                return out;
            }
            int picnum = obj.optInt("picnum", 0);
            int n = picnum > 0 ? Math.min(picnum, images.length()) : images.length();
            final String key = "E3A03D4A1586F6952F0E699344D0F4E2";
            for (int i = 0; i < n; i++) {
                String enc = images.optString(i, null);
                if (TextUtils.isEmpty(enc)) {
                    continue;
                }
                Bitmap bmp = SMUtil.DataSM4Decode(key, enc);
                if (bmp == null || bmp.isRecycled()) {
                    continue;
                }
                out.put(BitmapUtils.convertJpegToString(bmp, 90));
            }
        } catch (Exception e) {
            Log.e(TAG, "buildThunderPlainFacePics: " + e.getMessage());
        }
        return out;
    }

    private void submitFaceVerifyWithJson(final String livenessJson) {
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

    /** 炫彩活体提交核验（新接口）：faceIdent 带 isColor/seq/colorPics 炫彩字段 */
    private void submitFaceVerifyWithColor(final String seq, final JSONArray facePics, final JSONArray colorPics) {
        FaceVerifyManager.submitColor(seq, facePics, colorPics, new FaceVerifyManager.Callback() {
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

    /** 动作完成，等待后台人脸核验结果（默认启动 faceIdent 短超时） */
    private void showInPlaceVerifying() {
        showInPlaceVerifying(true);
    }

    /**
     * @param scheduleDefaultTimeout true：启动 25s faceIdent 兜底；false：仅展示 UI，
     *                               由调用方按 Thunder/faceIdent 阶段自行 {@link #scheduleVerifyTimeout(long)}
     */
    private void showInPlaceVerifying(boolean scheduleDefaultTimeout) {
        freezePreviewFrame();
        hideVerifySubtitle();
        if (ivVoice != null) {
            ivVoice.setVisibility(View.GONE);
        }
        // 提交核验阶段显示旋转蓝弧：动作模式检测期已在转，炫彩/动作+炫彩需在此显式启动，
        // 否则「人脸核验中 / 核验通过」看不到圆弧提示。
        startVerifyingRingArc();
        if (tvCheckHint == null) {
            return;
        }
        tvCheckHint.setVisibility(View.VISIBLE);
        tvCheckHint.setText(R.string.face_verify_submitting);
        tvCheckHint.setTextColor(ContextCompat.getColor(this, R.color.face_theme_primary));
        clearCheckHintLeadingIcon();
        if (scheduleDefaultTimeout) {
            scheduleVerifyTimeout(FACE_VERIFY_UI_TIMEOUT_MS);
        }
        // scheduleDefaultTimeout=false 时不 cancel：调用方可能已在工作线程安排了 Thunder 长超时，
        // 若此处 cancel 会因 runOnUiThread 时序把刚设好的超时清掉。
    }

    /**
     * 进入「提交核验中」时，把扫描环对齐到与动作模式一致的取景态：可见 + 灰色轨道 + 进度归零。
     * 动作模式检测期已进入该态，此处对炫彩/动作+炫彩尤为必要——否则扫描环可能停留在旧状态，
     * 后续 {@link #animateScanRingToSuccess()} 的「核验通过」蓝弧填充/绿环无从显示。
     */
    private void startVerifyingRingArc() {
        if (scanRingMain == null || mInPlaceSuccessUi || mInPlaceFailUi) {
            return;
        }
        stopRingProgress();
        scanRingMain.setMode(ScanRingOverlayView.MODE_SCANNING);
        scanRingMain.setScanArcEnabled(false);
        scanRingMain.setProgress(0f);
        scanRingMain.setVisibility(View.VISIBLE);
    }

    /** 核验请求挂死时，超时切到失败 UI，避免一直停在「人脸核验中」 */
    private void scheduleVerifyTimeout(long timeoutMs) {
        cancelVerifyTimeout();
        final long delay = timeoutMs > 0 ? timeoutMs : FACE_VERIFY_UI_TIMEOUT_MS;
        mVerifyTimeoutRunnable = () -> {
            mVerifyTimeoutRunnable = null;
            if (isFinishing() || !mFaceVerifying || mInPlaceFailUi || mInPlaceSuccessUi) {
                return;
            }
            onFaceVerifyFailed(getString(R.string.face_verify_network_timeout));
        };
        mUIHandler.postDelayed(mVerifyTimeoutRunnable, delay);
    }

    private void cancelVerifyTimeout() {
        if (mVerifyTimeoutRunnable != null) {
            mUIHandler.removeCallbacks(mVerifyTimeoutRunnable);
            mVerifyTimeoutRunnable = null;
        }
    }

    private void onFaceVerifyPassed() {
        cancelVerifyTimeout();
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
        mUIHandler.postDelayed(mSuccessFinishRunnable, SUCCESS_RING_HOLD_MS);
    }

    private void onFaceVerifyFailed(String message) {
        cancelVerifyTimeout();
        mFaceVerifying = false;
        String failReason = TextUtils.isEmpty(message)
                ? getString(R.string.face_verify_fail_default)
                : message;
        markQrRecordNotPass();
        FaceVerifyLogManager.uploadVerifyEnd(getApplicationContext(), false, failReason);
        AEFacePack.getInstance().setPendingFailDetail(failReason);
        showInPlaceFailUi(false, failReason, true);
    }

    private void clearCheckHintLeadingIcon() {
        if (tvCheckHint != null) {
            tvCheckHint.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
        }
    }

    /** 检测成功：底部绿色「提交成功」+ ic_face_suc，隐藏语音按钮 */
    private void showInPlaceVerifySuccess() {
        freezePreviewFrame();
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
        if (mRingRotateAnimator != null) {
            mRingRotateAnimator.cancel();
        }
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
        final AEFaceInterface listener = AEFacePack.getInstance().getInterface();
        if (listener == null) {
            m_hasFinishReturn = true;
            finish();
            return;
        }
        m_hasFinishReturn = true;
        PictureManagerUtils.getPictureManager().setCode(AEFaceParam.CODE_SUCCESS);
        if (isLightAliveMode() && !TextUtils.isEmpty(mPendingLightJson)) {
            final String json = mPendingLightJson;
            AEFaceCallbackHelper.dispatchFinish(
                    AEFacePack.getInstance().getInterface(), AEFacePack.SUCCESS, json);
            finish();
            return;
        }
        buildLivenessJsonAsync(json -> {
            AEFaceCallbackHelper.dispatchFinish(listener, AEFacePack.SUCCESS, json);
            finish();
        });
    }

    /** 采集结果 JSON 序列化回调（在主线程触发）。 */
    private interface OnLivenessJsonReady {
        void onReady(String json);
    }

    /**
     * 在子线程序列化采集结果 JSON（含 Base64 图片数组，可达 MB 级），完成后回到主线程回调。
     * 避免在主线程 {@code JSONObject.toString()} 阻塞导致 ANR（华为 P40Pro 等高分辨率机型尤甚）。
     */
    private void buildLivenessJsonAsync(final OnLivenessJsonReady callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final String json = PictureManagerUtils.getPictureManager()
                        .getJsonString(RecognizeActivity.this);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        callback.onReady(json);
                    }
                });
            }
        }, "AEFace-BuildJson").start();
    }

    public void finishActivityByFail() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            runOnUiThread(this::finishActivityByFail);
            return;
        }
        MLog.d(TAG, "finishActivityByFail");
        noteLivenessFailForQrRecord();
        showInPlaceFailUi(false, null);
    }

    /**
     * 取景页内联失败/超时（粉环 + 红色标题 + 原因 + 重新核验/其他核验方式）。
     *
     * @param timeout      true=认证超时；false=活体/比对失败
     * @param customDetail 自定义原因文案，可为 null
     */
    public void showInPlaceFailUi(boolean timeout, String customDetail) {
        showInPlaceFailUi(timeout, customDetail, false);
    }

    /**
     * @param submitFailure true=faceIdent 提交阶段失败（标题「提交失败」）；false=活体等失败（标题「验证失败」）
     */
    public void showInPlaceFailUi(boolean timeout, String customDetail, boolean submitFailure) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            runOnUiThread(() -> showInPlaceFailUi(timeout, customDetail, submitFailure));
            return;
        }
        if (mInPlaceFailUi || mInPlaceSuccessUi) {
            return;
        }
        cancelVerifyTimeout();
        mFaceVerifying = false;
        freezePreviewFrame();
        cancelNoFaceFailTimer();
        mInPlaceFailUi = true;
        mInPlaceFailIsTimeout = timeout;
        mInPlaceFailIsSubmit = submitFailure && !timeout;
        stopRingProgress();
        if (countDown != null) {
            countDown.cancel();
        }
        // 炫彩失败时也必须停 lightHandler / 色序定时器，避免后台仍在切色或占着解码
        if (isLightAliveMode()) {
            timeHandler.removeMessages(MSG_CODE_UPDATE);
            timeHandler.removeMessages(MSG_CODE_START);
            timeHandler.removeMessages(MSG_CODE_END);
            timeHandler.removeMessages(MSG_CODE_SAVE);
            if (lightHandler != null) {
                lightHandler.cancelDecodeTask();
            }
            if (faceBgView != null) {
                hideFaceBgFlash();
            }
        } else if (handler != null) {
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
                    : getString(submitFailure
                    ? R.string.face_verify_fail_default
                    : R.string.face_fail_no_face_detail);
        }
        final int titleRes = timeout
                ? R.string.aeye_recog_timeout
                : (submitFailure ? R.string.face_verify_failed : R.string.face_liveness_failed);

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
        mInPlaceFailIsSubmit = false;
        mFaceVerifying = false;
        // 炫彩失败若发生在闪光阶段，闪光层可能仍盖在预览上；重试前清掉，避免重新预览被色光遮住
        if (faceBgView != null) {
            hideFaceBgFlash();
        }
        unfreezePreviewFrame();
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
            scanRingMain.setScanArcEnabled(false);
            scanRingMain.setProgress(0f);
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
        // 炫彩/动作+炫彩走 lightHandler，动作/静默走 handler。
        // 此前只判断 handler!=null，炫彩模式下 handler 为空，导致点「重新核验」不触发 prepareRecog，
        // 预览一直停在冻结的最后一帧、无法重新开始。这里按当前模式判断对应的 handler。
        boolean handlerReady = isLightAliveMode() ? (lightHandler != null) : (handler != null);
        if (handlerReady) {
            if (isLightAliveMode()) {
                // 先 Destroy，再 prepareRecogLight → AliveInit → 预览/解码（顺序不能反）
                destroyLightAliveForReinit();
                // 色序流水号一次性，沿用上一轮会被服务端判为「颜色序列不对」
                AEFacePack.getInstance().refreshLightColors(this, refreshed -> {
                    if (isFinishing() || isDestroyed() || m_hasFinishReturn) {
                        return;
                    }
                    initColorFromServer();
                    prepareRecog();
                });
                return;
            }
            prepareRecog();
        } else {
            Log.e(TAG, "onInPlaceFailRetry: handler not ready, light=" + isLightAliveMode());
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
            AEFaceCallbackHelper.dispatchFinish(AEFacePack.getInstance().getInterface(),
                    AEFacePack.ERROR_OTHER_VERIFY, null);
        }
        AEFacePack.getInstance().returnToHostAuthHome();
    }

    /**
     * 用户主动退出统一入口：
     * <ul>
     *   <li>页内失败/超时态：回传准确的 {@link AEFacePack#ERROR_TIMEOUT} / {@link AEFacePack#ERROR_FAIL}</li>
     *   <li>其余状态：按用户取消 {@link AEFacePack#ERROR_CANCEL} 处理</li>
     * </ul>
     */
    private void handleUserExit() {
        if (mInPlaceFailUi) {
            exitAfterInPlaceFail();
        } else {
            finishActivityByUserCancel();
        }
    }

    /**
     * 失败/超时态退出：向宿主回传准确的 {@link AEFacePack#ERROR_TIMEOUT} /
     * {@link AEFacePack#ERROR_FAIL}（区别于「其他核验方式」的 {@link AEFacePack#ERROR_OTHER_VERIFY}）。
     */
    private void exitAfterInPlaceFail() {
        if (m_hasFinishReturn) {
            finish();
            return;
        }
        stopRingProgress();
        final boolean timeout = mInPlaceFailIsTimeout;
        final boolean submitFail = mInPlaceFailIsSubmit;
        final String failDetail = timeout
                ? getString(R.string.face_fail_timeout_detail)
                : AEFacePack.getInstance().getPendingFailDetail();
        // 结束日志（会话内去重，超时场景在此补报）+ 二维码终态兜底（失败态已置未通过时为空操作）
        FaceVerifyLogManager.uploadVerifyEnd(getApplicationContext(), false,
                TextUtils.isEmpty(failDetail)
                        ? getString(timeout ? R.string.aeye_recog_timeout
                        : (submitFail ? R.string.face_verify_failed : R.string.face_liveness_failed))
                        : failDetail);
        markQrRecordAbnormalExit();
        final AEFaceInterface listener = AEFacePack.getInstance().getInterface();
        if (listener == null) {
            m_hasFinishReturn = true;
            finish();
            return;
        }
        m_hasFinishReturn = true;
        if (timeout) {
            if (handler != null) {
                listener.onPrompt(handler.getCurPos(), null);
            }
            PictureManagerUtils.getPictureManager().setCode(AEFaceParam.CODE_ERROR_TIME_OUT);
            buildLivenessJsonAsync(json -> {
                AEFaceCallbackHelper.dispatchFinish(listener, AEFacePack.ERROR_TIMEOUT, json);
                finish();
            });
        } else {
            PictureManagerUtils.getPictureManager().setCode(AEFaceParam.CODE_ERROR_ALIVE_FAILED);
            buildLivenessJsonAsync(json -> {
                AEFaceCallbackHelper.dispatchFinish(listener, AEFacePack.ERROR_FAIL, json, failDetail, submitFail);
                finish();
            });
        }
    }

    private void hideScanArcIfIdle() {
        if (scanRingMain == null || mFinish || m_hasFinishReturn) {
            return;
        }
        if (mInPlaceSuccessUi || mInPlaceFailUi || mFaceVerifying) {
            return;
        }
        if (scanRingMain.getMode() == ScanRingOverlayView.MODE_SCANNING
                && scanRingMain.getProgress() <= 0f) {
            scanRingMain.setScanArcEnabled(false);
        }
    }

    private void stopRingProgress() {
        if (scanRingMain != null) {
            scanRingMain.removeCallbacks(hideScanArcRunnable);
        }
        if (mRingRotateAnimator != null) {
            mRingRotateAnimator.cancel();
        }
        if (scanRingMain != null
                && scanRingMain.getMode() == ScanRingOverlayView.MODE_SCANNING
                && scanRingMain.getProgress() <= 0f) {
            scanRingMain.setScanArcEnabled(false);
        }
    }

    /** 仅展示灰色轨道，不启动蓝色旋转弧（动画时钟保持，避免下一动作重开时卡顿） */
    private void prepareScanRingOverlay() {
        if (scanRingMain == null) {
            return;
        }
        scanRingMain.removeCallbacks(hideScanArcRunnable);
        scanRingMain.setMode(ScanRingOverlayView.MODE_SCANNING);
        scanRingMain.setVisibility(View.VISIBLE);
        scanRingMain.setScanArcEnabled(false);
        scanRingMain.setProgress(0f);
    }

    /** 有人脸时启动蓝色旋转弧；无人脸时延迟隐藏，避免动作切换时停转一帧 */
    private void updateScanRingForFace(boolean hasFace) {
        if (mInPlaceSuccessUi || mInPlaceFailUi || mFaceVerifying || mFinish || m_hasFinishReturn) {
            return;
        }
        if (hasFace) {
            if (scanRingMain != null) {
                scanRingMain.removeCallbacks(hideScanArcRunnable);
            }
            startRingProgress();
        } else if (scanRingMain != null) {
            scanRingMain.removeCallbacks(hideScanArcRunnable);
            scanRingMain.postDelayed(hideScanArcRunnable, HIDE_SCAN_ARC_DEBOUNCE_MS);
        }
    }

    private void startRingProgress() {
        if (scanRingMain == null) {
            return;
        }
        if (mInPlaceSuccessUi || mInPlaceFailUi || mFaceVerifying) {
            return;
        }
        cancelNoFaceFailTimer();
        scanRingMain.removeCallbacks(hideScanArcRunnable);
        scanRingMain.setMode(ScanRingOverlayView.MODE_SCANNING);
        scanRingMain.setVisibility(View.VISIBLE);
        scanRingMain.setScanArcEnabled(true);
        if (mRingRotateAnimator != null && mRingRotateAnimator.isStarted()) {
            return;
        }
        if (mRingRotateAnimator == null) {
            mRingRotateAnimator = ValueAnimator.ofFloat(0f, 360f);
            mRingRotateAnimator.setDuration(1400);
            mRingRotateAnimator.setRepeatCount(ValueAnimator.INFINITE);
            mRingRotateAnimator.setInterpolator(new LinearInterpolator());
            mRingRotateAnimator.addUpdateListener(animation -> {
                if (scanRingMain == null || mFinish || m_hasFinishReturn) {
                    return;
                }
                if (mInPlaceSuccessUi || mInPlaceFailUi || mFaceVerifying) {
                    return;
                }
                if (!scanRingMain.isScanArcEnabled()) {
                    return;
                }
                scanRingMain.setArcStartAngle((Float) animation.getAnimatedValue());
            });
        }
        float current = scanRingMain.getArcStartAngle();
        long duration = mRingRotateAnimator.getDuration();
        if (duration > 0) {
            mRingRotateAnimator.setCurrentPlayTime(
                    (long) ((current / 360f) * duration));
        }
        mRingRotateAnimator.start();
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
            handleUserExit();
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

    /** 后台比对等未通过：上报 isPass=4，不传 failedType */
    private void markQrRecordNotPass() {
        markQrRecordNotPass(null);
    }

    /**
     * @param failedType 仅动作活体未通过 3 次时传 {@link QrRecordStatus.FailedType#LIVENESS_ACTION}；其它未通过为 null
     */
    private void markQrRecordNotPass(String failedType) {
        if (mQrRecordFinalized) {
            return;
        }
        mQrRecordFinalized = true;
        QrRecordStatusManager.update(QrRecordStatus.NOT_PASS, failedType);
    }

    /** 异常退出：isPass=2，必传 failedType=6（已取消） */
    private void markQrRecordAbnormalExit() {
        if (mQrRecordFinalized) {
            return;
        }
        mQrRecordFinalized = true;
        QrRecordStatusManager.update(QrRecordStatus.ABNORMAL_EXIT,
                QrRecordStatus.FailedType.CANCELLED);
    }

    /**
     * 累计动作活体未通过次数；满 3 次上报 {@code isPass=4} + {@code failedType=1}。
     * 超时、后台比对失败不计入。
     */
    private void noteLivenessFailForQrRecord() {
        mLivenessFailCount++;
        if (mLivenessFailCount == 3) {
            markQrRecordNotPass(QrRecordStatus.FailedType.LIVENESS_ACTION);
        }
    }

    // ========== 炫彩颜色序列 / 闪光 UI ==========

    private void initColorFromServer() {
        int firstR = 255, firstG = 255, firstB = 255;
        ColorInfo colorInfoWhite = DataUtil.getColorInfo(firstR, firstG, firstB, "白色");
        ColorInfo colorInfoBlack = DataUtil.getColorInfo(0, 0, 0, "黑色");
        colorInfoList.clear();
        colorForSoList.clear();
        colorInfoList.add(colorInfoWhite);
        if (AEFacePack.getInstance().getmColor1() != null) {
            colorInfoList.add(AEFacePack.getInstance().getmColor1());
        }
        if (AEFacePack.getInstance().getmColor2() != null) {
            colorInfoList.add(AEFacePack.getInstance().getmColor2());
        }
        if (AEFacePack.getInstance().getmColor3() != null) {
            colorInfoList.add(AEFacePack.getInstance().getmColor3());
        }
        colorInfoList.add(colorInfoBlack);
        colorForSo = (firstB << 16) + (firstG << 8) + firstR;
        currentIndex = 0;
        for (int i = 0; i < colorInfoList.size(); i++) {
            ColorInfo colorInfo = colorInfoList.get(i);
            int colorSo = (colorInfo.getB() << 16) + (colorInfo.getG() << 8) + colorInfo.getR();
            colorForSoList.add(colorSo);
        }
    }

    private void lightInit() {
        lightInitWithMask(drawOvalMask());
    }

    private void lightInitWithMask(Bitmap bitmapMask) {
        isAeyeLightInit = true;
        try {
            int ret = AEyeLightAlive.getInstance().AEYE_AliveInit(this, bitmapMask);
            if (ret != 0) {
                isAeyeLightInit = false;
            }
            String version;
            try {
                version = AEyeLightAlive.getInstance().AEYE_GetVersion();
            } catch (Throwable ignore) {
                version = "";
            }
            Log.e(TAG, "****lightInit ret=" + ret + " , version : " + version);
        } catch (UnsatisfiedLinkError e) {
            // 常见原因：设备选择了 arm64 进程，但 APK 里没有对应 ABI 的 libLightAlive.so
            isAeyeLightInit = false;
            Log.e(TAG, "LightAlive.so load failed, disable light mode.", e);
        }
    }

    private Bitmap drawOvalMask() {
        if (faceBgView == null) {
            return null;
        }
        // 优先用与取景圆孔对齐的镂空，保证 native mask 与界面一致
        syncFlashHoleToPreviewPanel();
        int canvasW = faceBgView.getWidth();
        int canvasH = faceBgView.getHeight();
        RectF oval = CheckFaceView.getOvalRect();
        if (canvasW <= 0 || canvasH <= 0) {
            Display display = getWindowManager().getDefaultDisplay();
            canvasW = display.getWidth();
            canvasH = display.getHeight();
        }
        if (oval == null || oval.width() <= 0 || oval.height() <= 0) {
            float cx = canvasW / 2f;
            float cy = canvasH / 3f;
            float rx = canvasW / 3f;
            float ry = rx * 1.2f;
            oval = new RectF(cx - rx, cy - ry, cx + rx, cy + ry);
        }
        Log.e(TAG, "canvasW： " + canvasW + ", canvasH : " + canvasH + " , oval : " + oval);
        BitmapView bitmapView = new BitmapView(canvasW, canvasH, oval);
        Bitmap bitmapOnDraw = bitmapView.onDraw();
        if (bitmapOnDraw == null) {
            return null;
        }
        if (screenWidth <= 0) {
            screenWidth = canvasW;
        }
        if (screenHeight <= 0) {
            screenHeight = canvasH;
        }
        int a = canvasW - screenWidth;
        int b = Math.max(0, a / 2);
        int cutW = Math.min(screenWidth, canvasW - b);
        int cutH = Math.min(screenHeight, canvasH);
        Bitmap cutBitmap = Bitmap.createBitmap(bitmapOnDraw, b, 0, cutW, cutH);
        return BitmapUtils.scaleBitmap(cutBitmap, mPreviewWidth, mPreviewHeight);
    }

    private int upDataColor() {
        int color = -1;
        int len = colorInfoList.size();
        if (currentIndex >= len) {
            return -1;
        }
        isChangeColor = 0;
        ColorInfo colorInfo = colorInfoList.get(currentIndex);
        if (colorInfo != null) {
            color = Color.rgb(colorInfo.getR(), colorInfo.getG(), colorInfo.getB());
            currentB = colorInfo.getB();
            currentG = colorInfo.getG();
            currentR = colorInfo.getR();
        }
        changeColor(color, timeHandler);
        return 0;
    }

    private void changeColor(int color, Handler timeHandler) {
        if (faceBgView != null) {
            faceBgView.setOutColor(color, timeHandler);
        }
        // 每切换一屏色光同步提示字色（白屏用深蓝，彩屏用白字）
        if (Looper.myLooper() == Looper.getMainLooper()) {
            applyFlashChromeTextColors(true);
        } else {
            runOnUiThread(() -> applyFlashChromeTextColors(true));
        }
    }

    private final Handler timeHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int ret;
            switch (msg.what) {
                case MSG_CODE_FINISH:
                    break;
                case MSG_CODE_START:
                    ret = upDataColor();
                    startRecogTime = System.currentTimeMillis();
                    FLogUtil.printLog("start*****************colorForSo: " + colorForSo);
                    if (ret == 0) {
                        timeHandler.sendEmptyMessageDelayed(MSG_CODE_SAVE, splitTime / 2);
                        timeHandler.sendEmptyMessageDelayed(MSG_CODE_UPDATE, splitTime);
                    }
                    break;
                case MSG_CODE_SAVE:
                    isSave = true;
                    break;
                case MSG_CODE_END:
                    isRecord = false;
                    break;
                case MSG_CODE_UPDATE:
                    timeHandler.removeMessages(MSG_CODE_UPDATE);
                    int index = currentIndex > 0 ? currentIndex : 0;
                    int count = getPicNumber(index);
                    if (count <= 15) {
                        Log.e(TAG, "update****************count : " + count
                                + " ===延迟===闪光颜色序列： " + index);
                        timeHandler.sendEmptyMessageDelayed(MSG_CODE_UPDATE, 300);
                        return;
                    }
                    if (currentIndex == 1) {
                        showHint("keep_face", HINT_COLOR_THEME);
                    }
                    ret = upDataColor();
                    if (ret == 0) {
                        timeHandler.sendEmptyMessageDelayed(MSG_CODE_SAVE, splitTime);
                        timeHandler.sendEmptyMessageDelayed(MSG_CODE_UPDATE, splitTime);
                    } else {
                        isRecord = false;
                        timeHandler.sendEmptyMessageDelayed(MSG_CODE_END, splitTime);
                    }
                    isFinish = false;
                    break;
                case MSG_CODE_COLOR_INDEX_UPDATE:
                    currentIndex++;
                    colorForSo = (currentB << 16) + (currentG << 8) + currentR;
                    FLogUtil.printLog("change color success=====currentIndex================"
                            + currentIndex + " , currentColor: " + colorForSo);
                    break;
                case MSG_CODE_AUDIO_DELAY:
                    timeHandler.removeMessages(MSG_CODE_AUDIO_DELAY);
                    curVoice = 0;
                    break;
                case MSG_CODE_PREPARE_DELAY:
                    prepareRecog();
                    break;
                default:
                    break;
            }
        }
    };
}
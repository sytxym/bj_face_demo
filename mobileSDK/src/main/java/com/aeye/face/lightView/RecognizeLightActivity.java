package com.aeye.face.lightView;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.aeye.aeyelib.AEyeLightAlive;
import com.aeye.android.config.ConfigData;
import com.aeye.android.uitls.BitmapUtils;
import com.aeye.face.AEFacePack;
import com.aeye.face.AEFaceParam;
import com.aeye.face.ui.FaceImmersiveStatusBar;
import com.aeye.face.camera.CameraConfigurationManagerLight;
import com.aeye.face.camera.CameraManagerLight;
import com.aeye.face.camera.CaptureActivityHandlerLight;
import com.aeye.face.config.IDConstants;
import com.aeye.face.uitls.AudioUtils;
import com.aeye.face.uitls.ColorInfo;
import com.aeye.face.uitls.DataUtil;
import com.aeye.face.uitls.DeviceSafeCheckUtils;
import com.aeye.face.uitls.FLogUtil;
import com.aeye.face.uitls.FileUtil;
import com.aeye.face.uitls.LoadingDialog;
import com.aeye.face.uitls.PictureManagerUtilsLight;
import com.aeye.face.uitls.ToastUtil;
import com.aeye.face.view.AutoFitSurfaceView;
import com.aeye.face.view.CountView;
import com.aeye.face.view.FaceView;
import com.aeye.face.view.RecognizeActivity;
import com.aeye.sdk.AEFaceAlive;
import com.aeye.sdk.AEFaceDetect;
import com.aeye.sdk.AEFaceQuality;
import com.sdk.core.R;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;


public class RecognizeLightActivity extends Activity implements
        SurfaceHolder.Callback {
    // 430104196312023024
    public static final String TAG = RecognizeLightActivity.class.getSimpleName();
    public static final int TIME_OUT = 0;
    public static final String CAMERA_DIRECTION = "Camera_Direction";
    public static final int QUALITY_SIDE = 10;
    public static final int QUALITY_OUT = 20;

    private TextView tvTop;
    /**
     * 认证期间的 提示
     **/
    private TextView tvCheckHint, tvEnvHint;
    //	private ImageView ivMovie;
    private RelativeLayout faceStatus;
    private FrameLayout introduceView;
    private TextView introduceBegin;
    private FaceView faceRect;
    /**
     * 认证倒计时
     **/
    private CountView tvRecogTimeCountdown;
    private ImageView ivReturn, ivNumber, ivVoice;
    /**
     * 获取Activity 的 Handler
     **/
    public CaptureActivityHandlerLight handler;
    private boolean hasSurface;
    private int cameraDirection = -1;
    private SharedPreferences spCameraInfo;
    /**
     * 记录服务器返回的 人脸识别是否成功
     **/
    private boolean isDecode = true;
    private boolean m_hasFinishReturn = false;
    private boolean mFinish = false;
    WakeLock m_WakeLock = null;

    public   long getStartRecogTime() {
        return startRecogTime;
    }

    private   long startRecogTime = 0;

    public static int getmFaceOK() {
        return mFaceOK;
    }

    private static int mFaceOK = 0;
    int textId = -1;
    int curVoice = 0;

    private MyCount countDown = null;
    private boolean voiceTriggle = false;
    private boolean faceDisplayed = false;
    int count = 1;

    private boolean introduceTriggle = false;

    private static final int MSG_CODE_START = 1001;
    private static final int MSG_CODE_UPDATE = 1005;
    private static final int MSG_CODE_SAVE = 1007;
    private static final int MSG_CODE_END = 1010;
    private static final int MSG_CODE_FINISH = 1020;
    /**
     * 用于界面颜色变换成功后颜色值设置，此颜色值用于传入so
     */
    public static final int MSG_CODE_COLOR_INDEX_UPDATE = 1021;
    /**控制语音播放间隔 ，两次语音太频繁***/
    public static final int MSG_CODE_AUDIO_DELAY= 1022;
    public static final int MSG_CODE_PREPARE_DELAY= 1023;
    private static final int REQUEST_VIDEO_PERMISSIONS = 100;
    private List<ColorInfo> colorInfoList = new ArrayList<>();
    private List<ColorInfo> allColorInfos = new ArrayList<>();


    public static int getSplitTime() {
        return splitTime;
    }

    private static int splitTime = 1000;

    public static int getCurrentIndex() {
        return currentIndex;
    }

    public static volatile int currentIndex;
    private volatile int currentR;
    private volatile int currentG;
    private volatile int currentB;

    public static int getColorForSo() {
        return colorForSo;
    }

    public  ArrayList<Integer> getColorForSoList() {
        return colorForSoList;
    }


    private   ArrayList<Integer> colorForSoList = new ArrayList<>();


    public static volatile  int  colorForSo;

    private volatile boolean isSave = false;
    private volatile boolean isRandom;
    public static  volatile boolean isRecord = false,isGetLastBitmap = false;
    public static boolean isFinish = false;
    private CheckFaceView faceBgView;

    private MediaRecorder mRecorder = null;
    private String path;
    private String fileName;
    public static String fileVideoPath;
    public static String imagesPath;
    private String fileTxtPath;

    String sessionId ="";
    public static int isChangeColor =0;
    public static int screenWidth = 0;
    public static int screenHeight = 0;

    public final static int mPreviewWidth = 1080;
    public final static int mPreviewHeight = 1920;
    boolean isAeyeLightInit = false;
    private String fileNameCollect;

    public static int getPicNumber(int key) {
        if(key<picNumber.length) {
            return picNumber[key];
        }else{
            return 0;
        }
    }

    public static void addPicNumber(int currentColor ,int  mPicCount) {
        Log.e(TAG, "addPicNumber****************count : "+mPicCount+" index : "+currentColor);
        picNumber[currentColor]= mPicCount;
    }

    /**
     * 清空已保存的颜色数目
     */
    public static void clearPicNumber( ) {
        for (int i = 0; i < 6; i++) {
            picNumber[i]= 0;
        }
    }

    private static int[] picNumber = new int[6];

    public static boolean isIsFirstHasFace() {
        return isFirstHasFace;
    }

    public static void setIsFirstHasFace(boolean isFirstHasFace) {
        RecognizeLightActivity.isFirstHasFace = isFirstHasFace;
    }

    private static  boolean isFirstHasFace = false;
    private static int aliveMode = AEFaceParam.ALIVEMODE_LIGHT;
    private static int pose = AEFaceAlive.POSE_EYE_BLINK;

    public static int getAliveMode() {
        return aliveMode;
    }

    public static void setAliveMode(int aliveMode) {
        RecognizeLightActivity.aliveMode = aliveMode;
    }

    public static int getPose() {
        return pose;
    }

    public static void setPose(int pose) {
        RecognizeLightActivity.pose = pose;
    }
    boolean isUpAndroid6 = true;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        FaceImmersiveStatusBar.install(this);
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.aeye_alive_recognize);
        FaceImmersiveStatusBar.bindToolbar(this,
                findViewById(R.id.face_toolbar),
                findViewById(R.id.face_toolbar_gap));
        AEFacePack.getInstance().registerFaceFlowActivity(this);
        isGetLastBitmap = false;
//        avcCodec = new AvcEncoder(480,640,25,8500*1000);
        if(getIntent().getStringExtra("name") !=null){
            fileNameCollect = getIntent().getStringExtra("name");
        }
        if (getIntent().hasExtra(AEFaceParam.ALIVEMODE))
            aliveMode =  getIntent().getIntExtra(AEFaceParam.ALIVEMODE, aliveMode);
        setIsFirstHasFace(false);
        initColorFormServer();
        videoPath();
        faceBgView = findViewById(R.id.facebgview);
        introduceView = (FrameLayout) findViewById(R.id.introduceView);

        introduceBegin = (TextView) findViewById(R.id.introduceBegin);
//        introduceBegin.setVisibility(View.INVISIBLE);
        introduceBegin.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                prepareRecog();
                introduceView.setVisibility(View.GONE);
            }
        });
        introduceTriggle = AEFacePack.getInstance().isShowIntroduce();
        if (introduceTriggle) {
            introduceView.setVisibility(View.VISIBLE);
        }else{
           timeHandler.sendEmptyMessageDelayed(MSG_CODE_PREPARE_DELAY,500);
        }

        tvTop = (TextView) findViewById(R.id.tvTop);
        /** 认证期间的 提示 **/
        tvCheckHint = (TextView) findViewById(R.id.tvCheckHint);
        tvEnvHint = (TextView) findViewById(R.id.tvEnvHint);
        /** 3秒倒计时 **/
        ivNumber = (ImageView) findViewById(R.id.ivNumber);
//		ivMovie = (ImageView) findViewById(MResource.getIdByName(R.id.movies);
        ivVoice = (ImageView) findViewById(R.id.ivVoice);

        faceStatus = (RelativeLayout) findViewById(R.id.faceStatus);
//        if (AEFacePack.getInstance().isWhiteBackgroud()) {
//            faceStatus.setBackgroundResource(R.drawable.aeye_selector_bg_white);
//        }

        ivReturn = (ImageView) findViewById(R.id.ivReturn);

        faceRect = (FaceView) findViewById(R.id.faceRect);
        /** 认证倒计时 **/
        tvRecogTimeCountdown = (CountView) findViewById(R.id.tvRecogTimeCountdown);

        spCameraInfo = getSharedPreferences(ConfigData.SP_CAMERA_INFO,
                Context.MODE_PRIVATE);

        tvTop.setBackgroundColor(AEFacePack.getInstance().getTopColor());

        String title = AEFacePack.getInstance().getTitle();
        if (title != null) {
            tvTop.setText(title);
        }
        tvCheckHint.setVisibility(View.GONE);

        CameraManagerLight.init(getApplication());

        hasSurface = false;
        initData();
        m_hasFinishReturn = false;

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
        setWindowBrightness(255);
        boolean ret =  SupportAvcCodec();
        Log.e(TAG," supporet Avc : "+ret);
        deletePIcTempFile();
        if(DeviceSafeCheckUtils.isDeviceUnSafe()){
            String unSafeStr = getString(R.string.aeye_safetip);
            showToast(unSafeStr);
            /**start 临时注释20230717，在活体接口中上传风险标签*****/
            finishActivityByOther(AEFacePack.ERROR_DANGER_DEVICE, "");
            return;
            /**end 临时注释20230717，在活体接口中上传风险标签*****/
        }

        if(aliveMode == AEFaceParam.ALIVEMODE_MOTION_LIGHT) {
            AEFaceAlive.getInstance().AEYE_Alive_setAliveParamVIS(1, AEFacePack.getInstance().getAliveLevel());
            // 使用安全的随机数生成器  2026.4.2
            SecureRandom secureRandom = new SecureRandom();
            int r = secureRandom.nextInt(2);
            if (r == 0) {
                  pose = AEFaceAlive.POSE_EYE_BLINK;
            } else {
                pose = AEFaceAlive.POSE_MOUTH_OPEN;
            }
        }
        clearPicNumber();
        int versionCode = Build.VERSION.SDK_INT;
        if(versionCode <24){
            isUpAndroid6 = false;
        }
    }

    private void deletePIcTempFile() {
        String filePath = "/sdcard/Aeye/lastcolor.txt";
        filePath = "/sdcard/FaceCollect/insertParam.txt";
        File file = new File(filePath);
        if(file.exists()){
            file.delete();
        }
        filePath = "/sdcard/FaceCollect/insertImage";
        FLogUtil.delDir(filePath);
    }

    private void initColorFormServer() {
        int firstR = 255;
        int firstG = 255;
        int firstB = 255;
        ColorInfo colorInfoWhite = DataUtil.getColorInfo(firstR, firstG, firstB, "白色");
        ColorInfo colorInfoBlack = DataUtil.getColorInfo(0, 0, 0, "黑色");
        colorInfoList.add(colorInfoWhite);
        if(AEFacePack.getInstance().getmColor1() !=null)
        {
            colorInfoList.add(AEFacePack.getInstance().getmColor1());
        }
        if(AEFacePack.getInstance().getmColor2() !=null)
        {
            colorInfoList.add(AEFacePack.getInstance().getmColor2());
        }if(AEFacePack.getInstance().getmColor3() !=null)
        {
            colorInfoList.add(AEFacePack.getInstance().getmColor3());
        }
        colorInfoList.add(colorInfoBlack);
        colorForSo = (firstB << 16) + (firstG << 8) + firstR;
        currentIndex =0;
        String filePath = "/sdcard/FaceCollect/" + fileNameCollect+ ".txt";
        for (int i = 0; i < colorInfoList.size(); i++) {
            String text = colorInfoList.get(i).getB() + "," + colorInfoList.get(i).getG()  + "," + colorInfoList.get(i).getR() ;
//            FileUtil.saveLogServer(filePath, text);
        }

        for (int i = 0; i < colorInfoList.size(); i++) {
            ColorInfo colorInfo = colorInfoList.get(i);
            int B = colorInfo.getB();
            int G = colorInfo.getG();
            int  R = colorInfo.getR();
            int colorSo =    (B << 16) + (G << 8) + R;
            colorForSoList.add(colorSo);
        }
    }

    private void lightInit() {
        isAeyeLightInit = true;
        Bitmap bitmapMask = drawOvalMask();
//        SaveOvalMaskBitmap(bitmapMask);
        int ret = AEyeLightAlive.getInstance().AEYE_AliveInit(this, bitmapMask);

        if(ret != 0){
            isAeyeLightInit = false;
        }
        Log.e(TAG, "****lightInit ret=" + ret+" , isLightInit  version  : "+AEyeLightAlive.getInstance().AEYE_GetVersion());
    }
    private void randomcolor(){
        //从12种默认颜色中选择三种颜色
        String[] colors = new String[]{"255,255,255" ,  "20,255,128" ,"255,20,20",
                "128,20,255","0,0,0"};
        for (int i = 0; i < colors.length; i++) {
            ColorInfo mColor = new ColorInfo();
            String []a=colors[i].split(",");
            mColor.setR(Integer.valueOf(a[0]));
            mColor.setG(Integer.valueOf(a[1]));
            mColor.setB(Integer.valueOf(a[2]));
            mColor.setName("默认颜色"+(i+1));
            mColor.setLight(255);
            colorInfoList.add(mColor);
        }
    }

    private void randomThreeColor() {
        //从12种默认颜色中选择三种颜色
        String[] colors = new String[]{"255,20,20" , "20,255,20" , "20,20,255" ,"255,255,20",
                "255,20,255","20,255,255","255,128,20","255,20,128","128,255,20",
                "128,20,255","20,255,128","20,128,255"};
        for (int i = 0; i < colors.length; i++) {
            ColorInfo mColor = new ColorInfo();
            String []a=colors[i].split(",");
            mColor.setR(Integer.valueOf(a[0]));
            mColor.setG(Integer.valueOf(a[1]));
            mColor.setB(Integer.valueOf(a[2]));
            mColor.setName("默认颜色"+(i+1));
            mColor.setLight(255);
            allColorInfos.add(mColor);
        }
        ArrayList<Integer> intList = getRandomList();
        saveLocalColor( intList);
    }
    private void saveLocalColor(ArrayList<Integer> intList) {
        ColorInfo colorInfoWhite = DataUtil.getColorInfo(255, 255, 255, "白色");
        ColorInfo colorInfoBlack = DataUtil.getColorInfo(0, 0, 0, "黑色");
        colorInfoList.add(colorInfoWhite);
        for (int i = 0; i < intList.size(); i++) {
            colorInfoList.add(allColorInfos.get(intList.get(i)));
        }
        colorInfoList.add(colorInfoBlack);
    }
    public ArrayList<Integer> getRandomList(){
        ArrayList<Integer> list = new ArrayList<>(Arrays.asList(0,1,2,3,4,5,6,7,8,9,11,10));
        //随机对象
        Random random = new Random();
        int size = list.size();
        Set<Integer> totals = new HashSet<Integer>();
        ArrayList<Integer> resultList = new ArrayList<>();
        while (totals.size() < 3) {//获取3个
            //随机再集合里取出元素，添加到新哈希集合
            totals.add((int) list.get(random.nextInt(size)));
        }
        Iterator iterator = totals.iterator();
        while (iterator.hasNext()) {
            int next = (int) iterator.next();
            resultList.add(next);
        }
        return resultList;
    }
    private boolean SupportAvcCodec(){
        if(Build.VERSION.SDK_INT>=18){
            for(int j = MediaCodecList.getCodecCount() - 1; j >= 0; j--){
                MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(j);

                String[] types = codecInfo.getSupportedTypes();
                for (int i = 0; i < types.length; i++) {
                    if (types[i].equalsIgnoreCase("video/avc")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }


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

    public void showAlivePose(int id, boolean voice, boolean anim) {
        Log.e(TAG,"showAlivePose******************"+id);
        if(id ==111) {
            if (!RecognizeLightActivity.this.isFinishing()) {
                timeHandler.sendEmptyMessageDelayed(MSG_CODE_UPDATE, splitTime);
                if (voice && voiceTriggle) {
                    handler.pauseDecode();
                    handler.postDelayed(new Runnable() {

                        @Override
                        public void run() {
                            if (!RecognizeLightActivity.this.isFinishing() && handler !=null) {
                                handler.resumeDecode();
                            }
                        }
                    }, 500);
                }
            }
        }
        tvCheckHint.setVisibility(View.VISIBLE);

    }

    public void stopTimer() {
        if (countDown != null) {
            countDown.cancel();
        }
    }

    public void restartTimer(long timeout) {
//        if (countDown != null) {
//            countDown.cancel();
//        } else {
//            countDown = new MyCount(handler);
//        }
//
//        countDown.init((int) timeout);
//        countDown.start();
//
//        if (AEFacePack.getInstance().isAliveOff() &&
//                AEFacePack.getInstance().isModelAllSide()) {
//            if (!handler.startOneSide()) {
//                //采集失败。
//                if (countDown != null) {
//                    countDown.cancel();
//                }
//                showMessageBox();
//            }
//        }
    }

    @Override
    /**初始化SurfaceView预览、倒计时*/
    protected void onResume() {
        super.onResume();
        AutoFitSurfaceView surfaceView = findViewById(R.id.preview_view1);
        surfaceView.setAspectRation(mPreviewWidth,mPreviewHeight);
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        setIsFirstHasFace(false);
        if (hasSurface) {
            initCamera(surfaceHolder);
        } else {
            surfaceHolder.addCallback(this);
            surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }
    }

    private void prepareRecog() {
        timeHandler.removeMessages(MSG_CODE_UPDATE);
        timeHandler.removeMessages(MSG_CODE_START);
        timeHandler.removeMessages(MSG_CODE_END);
        handler.startPreview();
        tvRecogTimeCountdown.setVisibility(View.INVISIBLE);
        Log.e(TAG,"prepare ================: "+ AEFacePack.getInstance().isShowPrepare());
//        if (AEFacePack.getInstance().isShowPrepare()) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvEnvHint.setVisibility(View.INVISIBLE);
                Drawable[] drawable = tvEnvHint.getCompoundDrawables();
                ((AnimationDrawable) drawable[0]).start();
                ivNumber.setVisibility(View.VISIBLE);
                AnimationDrawable anim = (AnimationDrawable) ivNumber.getBackground();
                anim.start();
            }
        });

        if (!AEFacePack.getInstance().isAliveOff() ||
                !AEFacePack.getInstance().isModelAllSide()) {
            showAlivePose(0, true, false);
        } else {
            showAlivePose(0, false, false);
        }
        showHint("aeye_camera_notice", Color.WHITE);
        if(!isAeyeLightInit)
        {
            lightInit();
        }
        handler.postDelayed(new Runnable() {

            @Override
            public void run() {
                int voiceId = R.raw.aeye_face;
                if (aliveMode == AEFaceParam.ALIVEMODE_LIGHT)
                    voiceId = R.raw.aeye_face;
                else {
                    if (pose == AEFaceAlive.POSE_EYE_BLINK) {
                        voiceId =  R.raw.aeye_eye;
                        showHint("aeye_eye_blink", 0);
                    } else {
                        voiceId = R.raw.aeye_mouth;
                        showHint("aeye_mouth", 0);
                    }
                }
                if (voiceTriggle && voiceId != 0   && curVoice !=voiceId) {
                    AudioUtils.playVoiceIdle(RecognizeLightActivity.this,voiceId);
                }
                startRecog();
            }
        }, 3*1000);
//        }
//        else {
//            startRecog();
//        }
        showFaceStatus(true, true);
    }
    //    AvcEncoder  avcCodec;
    private void startRecog() {
        handler.resetData();
        deletePIcTempFile();
        Log.e(TAG,"startRecog ===========================");
        timeHandler.removeMessages(MSG_CODE_UPDATE);
        timeHandler.removeMessages(MSG_CODE_START);
        timeHandler.removeMessages(MSG_CODE_END);

        initFirstColor();
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
            restartTimer(AEFacePack.getInstance().getRecogTime());
        }

        if (!AEFacePack.getInstance().isFaceAppearStartMode()) {
//            tvRecogTimeCountdown.setVisibility(View.VISIBLE);
        }

        ivNumber.setVisibility(View.GONE);
        handler.restartPreviewAndDecode();
//        startRecord();

        isRecord = true;
        PictureManagerUtilsLight.getPictureManager().resetPictureManager();
        timeHandler.removeMessages(MSG_CODE_UPDATE);
        timeHandler.removeMessages(MSG_CODE_START);
        timeHandler.removeMessages(MSG_CODE_END);
        timeHandler.sendEmptyMessage(MSG_CODE_START);
    }

    private void initFirstColor() {
        currentIndex= 0;
        int firstR = 255;
        int firstG = 255;
        int firstB = 255;
        colorForSo = (firstB << 16) + (firstG << 8) + firstR;
    }

    /**
     * 根据摄像头ID开启摄像头、初始CaptureActivityHandler(处理编码结果、网络请求超时结果)<BR/>
     * 预览Act里初始化一个CaptureActivityHandler来处理解码的消息<BR/>
     * CaptureActivityHandler里初始一个DecodeThread线程,该线程包括一个DecodeHandler处理图片解码消息
     * DecodeHandler处理后最终给CaptureActivityHandler发送消息处理
     */
    private void initCamera(SurfaceHolder surfaceHolder) {
        try {
            CameraManagerLight.get(this).openDriver(surfaceHolder, cameraDirection);// 开摄像头驱动
        } catch (Exception e) {
            e.printStackTrace();
            String str = getApplication().getString(R.string.aeye_camera_error);
            finishActivityByOther(AEFacePack.ERROR_CAMERA, str);
            return;
        }

        if (handler == null) {
            handler = new CaptureActivityHandlerLight(this);// 创建处理解码结果的Handler对象.该对象一创建就执行预览操作
        }
        if (!introduceTriggle) {
//            prepareRecog();
        }
    }

    public int getOrientation() {
        if (AEFacePack.getInstance().isSetCaptureOrientation()) {
            return AEFacePack.getInstance().getCaptureOrientation();
        } else {
            return CameraManagerLight.get(this).getOrientation(cameraDirection);
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
        CameraManagerLight.get(this).stopPreview();
        CameraManagerLight.get(this).closeDriver();
    }

    @Override
    protected void onStop() {
        super.onStop();
//        avcCodec.StopThread();
        closeProgressDialog();
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
        AEFacePack.getInstance().unregisterFaceFlowActivity(this);
        super.onDestroy();
        clearPicNumber();
        if (handler != null) {
            FLogUtil.printLog(" ondestroy 888888888888888888888888888888 ");
            handler.quitSynchronously();
            handler = null;
        }
        if (countDown != null) {
            countDown.cancel();
            countDown = null;
        }
        timeHandler.removeMessages(MSG_CODE_UPDATE);
        timeHandler.removeMessages(MSG_CODE_START);
        timeHandler.removeMessages(MSG_CODE_END);
        PictureManagerUtilsLight.destroyManager();
        CameraManagerLight.unInit();
        AudioUtils.destroyPlayer();
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
        mToast = Toast.makeText(RecognizeLightActivity.this,msg,Toast.LENGTH_LONG);
        mToast.setGravity(Gravity.CENTER_HORIZONTAL,0,200);
        mToast.show();
    }

    private String lastShowMessage ="";
    /**
     * 更新预览框上方文字提示信息
     */
    public void showHint(final String msg, final int color) {
        // final String sMsg = msg;
        // final boolean bSucceed = succeed;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String showMessage="";
                switch (msg) {
                    case "aeye_quality_out":
                        showMessage = getResources().getString(R.string.aeye_quality_out);
                        break;
                    case "aeye_camera_notice":
                        showMessage = getResources().getString(R.string.aeye_camera_notice);
                        break;
                    case "aeye_eye_blink":
                        showMessage = getResources().getString(R.string.aeye_face_blick);
                        break;
                    case "aeye_mouth":
                        showMessage = getResources().getString(R.string.aeye_face_mouth);
                        break;
                    case "keep":
                        showMessage = "屏幕即将闪烁，请保持姿势不动";
                        break;
                    case "keep_face":
                        showMessage = "请保持姿势不动";
                        break;
                    case "quality_out":
                        showMessage = getResources().getString(R.string.aeye_quality_out);
                        break;
                }
                if(tvCheckHint.getVisibility() != View.VISIBLE && isUpAndroid6) {
                    tvCheckHint.setVisibility(View.VISIBLE);
                }
                tvCheckHint.setVisibility(View.VISIBLE);
//                if (!isUpAndroid6) {
                if(!lastShowMessage.equalsIgnoreCase(showMessage)) {
                    lastShowMessage = showMessage;
                    if (isUpAndroid6) {

                        ColorStateList colorList = tvCheckHint.getTextColors();
                        int color = colorList.getDefaultColor();
                        //做两种颜色切换
                        if (color == -16711936)//绿色
                        {
                            tvCheckHint.setTextColor(Color.BLACK);
                        } else {
                            tvCheckHint.setTextColor(Color.GREEN);
                        }
                        String oldText = tvCheckHint.getText().toString();
                        if (!oldText.equalsIgnoreCase(showMessage))
                            tvCheckHint.setText(showMessage);
                    } else {
                        showShortToast(showMessage);
                    }
                }
            }
        });
    }

    public void setMotionAliveSuc(){
        showHint("keep",0);
    }
    public void showQualityHint(final int quality, final boolean voice) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int id = 0;
                int voiceId = 0;
                switch (quality) {
                    default:
                    case AEFaceQuality.QUALITY_UNKNOW:
                        return;
                    case AEFaceQuality.QUALITY_OK:
                        faceAppear();
                        break;
                    case AEFaceQuality.QUALITY_NEAR:
                        id = R.string.aeye_quality_near;
                        voiceId = R.raw.aeye_quality_near;
                        break;
                    case AEFaceQuality.QUALITY_FAR:
                        id = R.string.aeye_quality_far;
                        voiceId = R.raw.aeye_quality_far;
                        break;
                    case AEFaceQuality.QUALITY_BRIGHT:
                        id = R.string.aeye_quality_bright;
                        voiceId = R.raw.aeye_quality_bright;
                        break;
                    case AEFaceQuality.QUALITY_DARK:
                        id = R.string.aeye_quality_dim;
                        voiceId = R.raw.aeye_quality_dim;
                        break;
                    case AEFaceQuality.QUALITY_UNEVEN:
                        id = R.string.aeye_quality_uneven;
                        break;
                    case QUALITY_OUT:
                        id = R.string.aeye_quality_out;
                        voiceId = R.raw.aeye_out;
                        showHint("quality_out",0);
                        break;
                    case QUALITY_SIDE:
                        id = R.string.aeye_quality_side;
                        break;
                }

                if (textId != id) {
                    textId = id;
                    if (id == 0) {
                        tvEnvHint.setVisibility(View.INVISIBLE);
                        handler.flashDisplay(false, false);
                        return;
                    }

                    if (voiceTriggle && voiceId != 0 && voice && curVoice !=voiceId) {
                        AudioUtils.playVoiceIdle(getApplication(), voiceId);
                        curVoice = voiceId;
                        timeHandler.sendEmptyMessageDelayed(MSG_CODE_AUDIO_DELAY,3000);
                    }
                }
            }
        });
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


    public void dismissHint() {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                tvCheckHint.setVisibility(View.INVISIBLE);
            }
        });
    }

    private void showFaceStatus(boolean face, boolean voice) {
//        startRecog();
        faceStatus.setEnabled(face);
        if (face) {
            if (handler != null && isDecode) {
                if (faceDisplayed) {
                    showAlivePose(handler.getCurPos(), false, false);
                }
                tvEnvHint.setVisibility(View.INVISIBLE);
            }
        } else {
            if (handler != null && isDecode) {
                showQualityHint(QUALITY_OUT, voice);
            }
        }
    }

    private void faceAppear() {
        if (AEFacePack.getInstance().isFaceAppearStartMode() && !faceDisplayed) {
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    tvRecogTimeCountdown.setVisibility(View.VISIBLE);
                    showAlivePose(handler.getCurPos(), false, false);
                }
            });
            restartTimer(AEFacePack.getInstance().getMotionTime());
        }
        faceDisplayed = true;
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
            FLogUtil.printLog("has face &&&&&&&&&  face OK  "+mFaceOK);
            if(!isIsFirstHasFace()){
                setIsFirstHasFace(true);
//                startRecog();
            }

        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(!inRange)
                {
                    showFaceFarBox();
                }
                showFaceStatus(inRange, true);
            }
        });
    }

    public void showNoFace() {
        mFaceOK = -1;
        Log.e(TAG," no face *********************");
        startRecog();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                boolean showMessage = AEFacePack.getInstance().isStrictMode() && faceDisplayed;
                showMessage = false;
                showFaceStatus(false, !showMessage);
                if (showMessage) {
                    showMessageBox();
                }
            }
        });
    }

    public void showTipAfterHasFace() {
        if (aliveMode == AEFaceParam.ALIVEMODE_LIGHT)
            showHint("aeye_camera_notice", 0);
        else {
            if (pose == AEFaceAlive.POSE_EYE_BLINK) {
                showHint("aeye_eye_blink", 0);
            } else {
                showHint("aeye_mouth", 0);
            }
        }
    }
    public void showMessageBox() {
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
    }
    AlertDialog faceFarDialog;
    public void showFaceFarBox() {
        FLogUtil.printLog("show face far box!!!!");
        handler.cancelDecodeTask();
        timeHandler.removeMessages(MSG_CODE_UPDATE);
        timeHandler.removeMessages(MSG_CODE_START);
        timeHandler.removeMessages(MSG_CODE_END);
        if(faceFarDialog !=null && faceFarDialog.isShowing()){
            return;
        }
        AlertDialog.Builder alert = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_LIGHT);
        alert.setView(getLayoutInflater().inflate(R.layout.aeye_dialog_face_far, null));
        alert.setPositiveButton(getString(android.R.string.ok), null);
        alert.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                startRecog();
            }
        });
        faceFarDialog = alert.create();
        faceFarDialog.setCanceledOnTouchOutside(false);
        faceFarDialog.setVolumeControlStream(AudioManager.STREAM_MUSIC);
        faceFarDialog.show();
        Button btnPo = (Button) faceFarDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        btnPo.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
    }

    public void showTimeOutBox() {
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
    }

    public void showFaceRect(Rect rect, int width, int height,
                             boolean bMirror) {
        faceRect.drawFaceRect(rect, width, height, bMirror);
    }

    // ///////////////////////SurfaceHolder.Callback
    // 的三个重写方法///////////////////////////
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.e(TAG,"surfaceCreate *******************");
        if (!hasSurface) {
            Log.e(TAG,"surfaceCreate *******************"+hasSurface);
            hasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        Log.e(TAG,"surfaceChanged **********width : "+width+" ,height : "+height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;
        Log.e(TAG,"surfaceDestroyed **********holder : "+holder.hashCode());
    }

    public CaptureActivityHandlerLight getHandler() {
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
        if(handler !=null)
            handler.resetData();
        if (null != AEFacePack.getInstance().getInterface()) {
//            if (!m_hasFinishReturn) {
                AEFacePack.getInstance().getInterface().onFinish(code,
                        PictureManagerUtilsLight.getPictureManager().getJsonString(null));
//            }
        }
        m_hasFinishReturn = true;
        finish();
    }

    public void finishActivityByTimeOut() {
        if(handler !=null)
            handler.resetData();
        if (null != AEFacePack.getInstance().getInterface()) {
            if (!m_hasFinishReturn) {
                String str = getApplication().getString(R.string.aeye_recog_timeout);
                AEFacePack.getInstance().getInterface().onPrompt(handler.getCurPos(), null);
                AEFacePack.getInstance().getInterface().onFinish(AEFacePack.ERROR_TIMEOUT,
                        PictureManagerUtilsLight.getPictureManager().getJsonString(null));
            }
        }
        m_hasFinishReturn = true;
        finish();
    }

    public void finishActivityByUserCancel() {
        if(handler !=null)
            handler.resetData();
        if (null != AEFacePack.getInstance().getInterface()) {
            if (!m_hasFinishReturn) {
                m_hasFinishReturn = true;
                String str = getApplication().getString(R.string.aeye_user_cancel);
                AEFacePack.getInstance().getInterface().onFinish(AEFacePack.ERROR_CANCEL,
                        PictureManagerUtilsLight.getPictureManager().getJsonString(null));
            }
        }
        finish();
    }

    public void finishActivityBySuccessful() {

        if (null != AEFacePack.getInstance().getInterface()) {
            m_hasFinishReturn = true;
            int resId = R.string.aeye_alive_success;
            if (AEFacePack.getInstance().isAliveOff()) {
                resId = R.string.aeye_capture_success;
            }
            showProgressDialog("请稍候……");
            Bitmap bitmap = AEyeLightAlive.getInstance().AEYE_CurrentSetImageData(4);
            if(bitmap == null){
                finishActivityByOther(-15,"闪光颜色获取最佳图失败！");
                return;
            }
            Rect[] rects =  AEFaceDetect.getInstance().AEYE_FaceDetect(bitmap);
            if(rects == null){
                finishActivityByOther(-16,"获取最佳图人脸失败！");
                return;
            }

            float[] mLandMark = null;
            try {
                mLandMark = AEyeLightAlive.getInstance().getBestBitLocation(rects, bitmap);
            } catch (Exception e) {
                e.printStackTrace();
                AEFacePack.getInstance().getInterface().onFinish(AEFacePack.ERROR_FAIL,
                        PictureManagerUtilsLight.getPictureManager().getJsonString(null));
            }
            if (mLandMark != null) {
                AEyeLightAlive.getInstance().insetKeyPoints(4, mLandMark);
                CameraManagerLight.get(RecognizeLightActivity.this).stopPreview();
                int ret = AEyeLightAlive.getInstance().AEYE_GetImageData();
                Log.e(TAG, " finish getImage ret : " + ret);
                if (ret == 0) {//成功
//                    String str = getApplication().getString(resId);
                    //非加密的对齐图片
                    String filePath = "/sdcard/FaceCollect/" + fileNameCollect + ".jpg";
//            BitmapUtils.saveUserPicture(AEyeLightAlive.getInstance().getAlignBitmap(),AEFacePack.getInstance().getColorSeq()+"alignNoencode");
                    Bitmap alignBitmap = AEyeLightAlive.getInstance().getAlignBitmap();
//                saveBitmap(alignBitmap, fileNameCollect);
                    Bitmap cuesBitmap = AEyeLightAlive.getInstance().getNormalCuesBitmap();
//                saveBitmap(cuesBitmap, "cues");
                    String encodeDataBase64 = BitmapUtils.convertIconToString(cuesBitmap);
                    String alignBitBase64 = BitmapUtils.convertIconToString(alignBitmap);
                    String data = PictureManagerUtilsLight.getPictureManager().getJsonString(encodeDataBase64, alignBitBase64);
                    if(data !=null)
                    {
                        AEFacePack.getInstance().getInterface().onFinish(AEFacePack.SUCCESS,
                                data);
                    }else{
                        AEFacePack.getInstance().getInterface().onFinish(AEFacePack.ERROR_FAIL,
                                null);
                    }
                } else {
                    AEFacePack.getInstance().getInterface().onFinish(AEFacePack.ERROR_FAIL,
                            PictureManagerUtilsLight.getPictureManager().getJsonString(null));
                }
            }
        }
        if(handler !=null)
            handler.resetData();
        finish();
    }


    public void finishActivityByFail() {
//		countDown.cancel();
        if(handler !=null)
        handler.resetData();
        if (null != AEFacePack.getInstance().getInterface()) {
            m_hasFinishReturn = true;
            int resId = R.string.aeye_alive_fail;
            if (AEFacePack.getInstance().isAliveOff()) {
                resId = R.string.aeye_capture_fail;
            }
            String str = getApplication().getString(resId);
            AEFacePack.getInstance().getInterface().onFinish(AEFacePack.ERROR_FAIL,
                    PictureManagerUtilsLight.getPictureManager().getJsonString(null));
        }
        finish();
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
                    if (IDConstants.SIDE_NUM == PictureManagerUtilsLight.getPictureManager().getCurNum()) {
                        finishActivityBySuccessful();
                    }
                }
            } else {
                if (AEFacePack.getInstance().isNoticeTimeout()) {
                    if (faceDisplayed) {
                        showTimeOutBox();
                    } else {
                        showMessageBox();
                    }
                } else {
                    CameraManagerLight.get(RecognizeLightActivity.this).stopPreview();
                    finishActivityByTimeOut();
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


    private Handler timeHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int ret;
            switch (msg.what) {
                case MSG_CODE_FINISH:
                    finish();
//                    avcCodec.StopThread();
//                    postUpload();
                    break;
                case MSG_CODE_START:
                    ret = upDataColor();
                    startRecogTime = System.currentTimeMillis();
                    FLogUtil.printLog( "start*****************colorForSo: "+colorForSo);
                    if (ret == 0) {
                        timeHandler.sendEmptyMessageDelayed(MSG_CODE_SAVE, splitTime/2);
                        timeHandler.sendEmptyMessageDelayed(MSG_CODE_UPDATE, splitTime);
                    }
                    break;
                case MSG_CODE_SAVE:
                    isSave = true;
                    break;
                case MSG_CODE_END:
                    isRecord = false;
//                    avcCodec.StartEncoderThread();
                    showProgressDialog("正在检测……");
//                    ToastUtil.showToast(getApplicationContext(), "采集结束,2秒后页面将自动关闭");
                    timeHandler.sendEmptyMessageDelayed(MSG_CODE_FINISH, 2*1000);
//                    performJcodec();
//
                    break;
                case MSG_CODE_UPDATE:
                    timeHandler.removeMessages(MSG_CODE_UPDATE);
                    //如果这个颜色闪光采集的帧少于15帧，则延迟100ms变颜色
                    int index = 0;
                    if(currentIndex >0){
                        index = currentIndex;
                    }
                    int count = getPicNumber(index);
//                    Log.e(TAG, "update****************count : "+count+" index : "+index);
                    if(count<=15){
                        Log.e(TAG, "update****************count : "+count+" ===延迟===闪光颜色序列： "+ index);
                        timeHandler.sendEmptyMessageDelayed(MSG_CODE_UPDATE, 300);
                        return;
                    }
                    if(currentIndex==1){
                        showHint("keep_face",0);
                    }
                    ret = upDataColor();
                    if (ret == 0) {
                        Log.e(TAG, "update*****************11*****splitTime: "+splitTime);
                        timeHandler.sendEmptyMessageDelayed(MSG_CODE_SAVE, splitTime);
                        timeHandler.sendEmptyMessageDelayed(MSG_CODE_UPDATE, splitTime);
                    } else {
                        Log.e(TAG, "update*****************2222222");
//                        stopRecord();
                        isRecord = false;
                        timeHandler.sendEmptyMessageDelayed(MSG_CODE_END, splitTime);
                    }
                    isFinish = false;
//                    Log.e(TAG, "update  color *****************"+isFinish+" , isDecode : "+isDecode);
                    break;
                case MSG_CODE_COLOR_INDEX_UPDATE:
                    currentIndex++;
                    //BGR转成so中对应的值
                    colorForSo = (currentB << 16) + (currentG << 8) + currentR;
                    FLogUtil.printLog("change color success=====currentIndex================" + currentIndex+" , currentColor: "+colorForSo);
                    break;
                case MSG_CODE_AUDIO_DELAY:
                    timeHandler.removeMessages(MSG_CODE_AUDIO_DELAY);
                    curVoice = 0;
                    break;
                case MSG_CODE_PREPARE_DELAY:
                    prepareRecog();
                    break;
            }
        }
    };

    private int upDataColor() {
        int color = -1, light = -1;
        int len = colorInfoList.size();

        if (currentIndex >= len) {
            return -1;
        }
        isChangeColor =0;
        ColorInfo colorInfo = colorInfoList.get(currentIndex);
        if (colorInfo != null) {
            color = Color.rgb(colorInfo.getR(), colorInfo.getG(), colorInfo.getB());
            light = colorInfo.getLight();
            currentB = colorInfo.getB();
            currentG = colorInfo.getG();
            currentR = colorInfo.getR();
//            currentLight = colorInfo.getLight();
        }
        String text = currentB + "," + currentG + "," + currentR;
//        FileUtil.saveLogServer(fileTxtPath, text);
        changeColor(color,timeHandler);
        //真正变换界面颜色需要几十毫秒延迟，在延迟后再index+1 以及记录颜色值
//        timeHandler.sendEmptyMessageDelayed(MSG_CODE_COLOR_INDEX_UPDATE,30);

//        Log.e(TAG, "currentColor======B :" + currentB+" , G : "+currentG+" , R : "+currentR+" , colorForso : "+colorForSo);
        return 0;
    }
    private Bitmap drawOvalMask() {
//        String path =  "/sdcard/FaceCollect/ovalMask.jpg";
//        if(FileUtil.isFileExists(path)){
//            return;
//        }
        int canvasW = faceBgView.getCanvasWidth();
        int canvasH = faceBgView.getCanvasHeight();
        RectF oval = faceBgView.getOvalRect();
        Log.e(TAG, "canvasW： "+canvasW+", canvasH : "+canvasH+" , oval : "+oval);
        //canvasW： 1080, canvasH : 2259 , oval : RectF(216.0, 326.0, 864.0, 1180.0)
        double ratioX = (double) mPreviewWidth/ screenWidth;
        double ratioY = (double) mPreviewHeight/ screenHeight;

        ratioX = (double) screenWidth/canvasW;
        ratioY = (double) screenHeight/canvasH;
//        ovalReal = new RectF((float)(oval.left*ratioX),(float)(oval.top*ratioY),(float)(oval.right*ratioX),(float)(oval.bottom*ratioY));
//        BitmapView bitmapView = new BitmapView(mPreviewWidth,mPreviewHeight,ovalReal);
        BitmapView bitmapView = new BitmapView(canvasW,canvasH,oval);//画一个同屏幕大小的椭圆底图，后做预览大小的裁剪，再拉伸到1080*1920
        if(bitmapView !=null) {
//            Log.e(TAG, "ratioX： " + ratioX + ", ratioY : " + ratioY + " , real : " + ovalReal);
            Bitmap bitmapOnDraw = bitmapView.onDraw();
            int a = canvasW - screenWidth;
            int b = a / 2;
            Log.e(TAG," cutLeft : "+b+" , cut right : "+(canvasW-b)+", a : "+a);
            Bitmap cutBitmap = Bitmap.createBitmap(bitmapOnDraw, b, 0,screenWidth, screenHeight);
            Bitmap bitmapNew = BitmapUtils.scaleBitmap(cutBitmap,mPreviewWidth,mPreviewHeight);
            return bitmapNew;
        }else{
            return  null;
        }
    }

    //保存到本地
    public void SaveOvalMaskBitmap(Bitmap bitmap)
    {
        //存储路径
        File file = new File("/sdcard/FaceCollect");
        if(!file.exists())
            file.mkdirs();
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file.getPath() + "/ovalMask.jpg");
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
            fileOutputStream.close();
            System.out.println("saveBmp is here");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**
     * @param color
     * @param timeHandler
     */
    private void changeColor(int color, Handler timeHandler) {
        if (faceBgView != null) {
//            Log.e(TAG, "changeColor=" + System.currentTimeMillis());
            faceBgView.setOutColor(color,timeHandler);
        }
    }

    Camera camera;
    private void startRecord() {
        if (!hasSpace()) {
            return;
        }
        camera = CameraManagerLight.get(this).getCamera();
        try {
            if (camera != null ) {
                if(mRecorder !=null){
                    releaseRecoder();
                    mRecorder = null;
                }
                if (mRecorder == null) {
                    camera.unlock();
                    mRecorder = new MediaRecorder();
                    mRecorder.reset();
                    mRecorder.setCamera(camera);
                    mRecorder.setOrientationHint(270);
                    mRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
                    // Set output file format
                    mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                    mRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
                    CameraConfigurationManagerLight manager = CameraManagerLight.get(this).getConfigManager();
                    int[] videoSize = new int[]{manager.cameraResolution.x,manager.cameraResolution.y};
                    Log.e(TAG, "startRecord w = " + videoSize[0] + ", h =" + videoSize[1]);

                    mRecorder.setVideoSize(videoSize[0], videoSize[1]);
                    mRecorder.setVideoEncodingBitRate(5 * 1024 * 1024);
                    mRecorder.setVideoFrameRate(12);
                }
                videoPath();
                path = getSDPath();
                Log.e(TAG," getSDpath********************" );
                if (path != null) {
                    Log.e(TAG," fileVideoPath********************" +fileVideoPath);
                    mRecorder.setOutputFile(fileVideoPath);
                    mRecorder.prepare();
                    mRecorder.start();
                    timeHandler.removeMessages(MSG_CODE_UPDATE);
                    timeHandler.removeMessages(MSG_CODE_START);
                    timeHandler.removeMessages(MSG_CODE_END);
                    timeHandler.sendEmptyMessage(MSG_CODE_START);
                    Log.e(TAG," mRecorder.start()********************" );
                    if (timeHandler != null) {
                        timeHandler.sendEmptyMessage(99);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void videoPath() {
        path = getSDPath();
        Log.e(TAG, "getSDpath********************");

        if (path != null && !path.isEmpty()) {
            try {
                // 1. 定义允许的子目录名（白名单）
                String subDir = "FaceCollect";

                // 2. 校验子目录名是否合法（Android 文件系统安全）
                String dirRegex = "^[A-Za-z0-9_-]{1,32}$";
                if (!subDir.matches(dirRegex)) {
                    Log.e(TAG, "Invalid directory name: " + subDir);
                    return;
                }

                // 3. 校验基础路径是否安全
                String basePathRegex = "^(/[A-Za-z0-9_-]+)+$";
                String normalizedBasePath = path.replace(File.separator, "/");
                if (!normalizedBasePath.matches(basePathRegex)) {
                    Log.e(TAG, "Invalid base path: " + path);
                    return;
                }

                // 4. 安全路径拼接（避免使用 Paths，Android API 兼容性）
                File baseDir = new File(path);
                File targetDir = new File(baseDir, subDir);

                // 5. 路径规范化（Android 方式）
                String canonicalBasePath = baseDir.getCanonicalPath();
                String canonicalTargetPath = targetDir.getCanonicalPath();

                // 6. 路径遍历防护：确保目标路径在基础路径内
                if (!canonicalTargetPath.startsWith(canonicalBasePath)) {
                    Log.e(TAG, "Path traversal detected: " + canonicalTargetPath);
                    return;
                }

                // 7. 最终路径格式校验
                String finalPath = canonicalTargetPath;
                String normalizedFinalPath = finalPath.replace(File.separator, "/");
                if (!normalizedFinalPath.matches(basePathRegex)) {
                    Log.e(TAG, "Invalid final path format: " + finalPath);
                    return;
                }

                // 8. 创建目录（如果不存在）
                File fileDir = new File(finalPath);
                if (!fileDir.exists()) {
                    boolean created = fileDir.mkdirs();  // Android 中使用 mkdirs()
                    if (created) {
                        Log.d(TAG, "Directory created successfully: " + finalPath);
                        // Android 中设置目录权限（可选）
                        fileDir.setReadable(true, false);
                        fileDir.setWritable(true, true);
                        fileDir.setExecutable(true, false);
                    } else {
                        Log.e(TAG, "Failed to create directory: " + finalPath);
                    }
                } else {
                    Log.d(TAG, "Directory already exists: " + finalPath);
                }

            } catch (IOException e) {
                Log.e(TAG, "IO Exception in videoPath", e);
            } catch (Exception e) {
                Log.e(TAG, "Exception in videoPath", e);
            }
        }
    }


    public static boolean delDir(String dirPath) {
        File fDir = new File(dirPath);
        if (!fDir.exists()) {
            return true;
        } else {
            if (fDir.isDirectory()) {
                File[] fps = fDir.listFiles();
                File[] var3 = fps;
                int var4 = fps.length;

                for (int var5 = 0; var5 < var4; ++var5) {
                    File fp = var3[var5];
                    if (fp.isDirectory()) {
                        delDir(fp.getAbsolutePath());
                    } else {
                        fp.delete();
                    }
                }
            }

            fDir.delete();
            return true;
        }
    }
    private boolean hasSpace() {
        //判断sdcard存储空间是否满足文件的存储
        File sdcard_filedir = Environment.getExternalStorageDirectory();//得到sdcard的目录作为一个文件对象
        long usableSpace = sdcard_filedir.getUsableSpace();//获取文件目录对象剩余空间
        long totalSpace = sdcard_filedir.getTotalSpace();
        //将一个long类型的文件大小格式化成用户可以看懂的M，G字符串
        String usableSpace_str = Formatter.formatFileSize(getApplicationContext(), usableSpace);
        String totalSpace_str = Formatter.formatFileSize(getApplicationContext(), totalSpace);
        Log.e(TAG, "totalSpace =" + totalSpace_str + ",usableSpace =" + usableSpace_str);
        if (usableSpace < 1024 * 1024 * 1024 * 1) {
            ToastUtil.showToast(getApplicationContext(), "sdcard剩余空间不足,剩余空间为：" + usableSpace_str);
            return false;
        }
        return true;
    }

    private void stopRecord() {
        Log.e(TAG, "stopRecord");
        if (camera != null)
            camera.lock();
        releaseRecoder();
    }

    private void releaseRecoder() {
        if (mRecorder != null) {
            mRecorder.setOnErrorListener(null);
            mRecorder.setOnInfoListener(null);
            mRecorder.setPreviewDisplay(null);

            mRecorder.stop();
            mRecorder.release();
        }
    }

    private String getSDPath() {
        File sdDir = null;
        boolean sdCardExist = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED); // 判断sd卡是否存在
        if (sdCardExist) {
            sdDir = Environment.getExternalStorageDirectory();// 获取跟目录
            return sdDir.toString();
        }
        return null;
    }



    Dialog loadingDialog;
    private void showProgressDialog(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                closeProgressDialog();
                if (loadingDialog == null) {
                    loadingDialog = LoadingDialog.createLoadingDialog(RecognizeLightActivity.this, message+"...");
                    loadingDialog.setCancelable(true);
                    loadingDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            finish();
                        }
                    });
                    if(!RecognizeLightActivity.this.isFinishing() && loadingDialog !=null)
                    {
                        loadingDialog.show();
                    }
                }
            }
        });

    }

    /**
     * 关闭ProgressDialog
     */
    private void closeProgressDialog() {
        if (loadingDialog != null) {
            loadingDialog.dismiss();
            loadingDialog = null;
        }
    }

}
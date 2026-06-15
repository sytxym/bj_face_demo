package com.xym.testface;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.aeye.android.uitls.BitmapUtils;
import com.aeye.face.AEFaceBean;
import com.aeye.face.uitls.FLogUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.xym.testface.bean.HttpInterface;
import com.xym.testface.bean.LiveRequestEntity;
import com.xym.testface.bean.LiveResponseBean;
import com.xym.testface.utils.DeviceUtil;

public class ResultAliveActivity extends Activity implements OnClickListener {
    private Button btBack;
    private ImageView imgFrontFace, imgAliveA, imgAliveB, imgAliveC, imgAliveD, btReturn, imgError;
    private TextView msgAlive;

    private LinearLayout layoutProcess;

    //	private String strImage;
    private ProgressDialog mProgress;
    Bitmap bitmapFrontFace, bitmap0, bitmap1, bitmap2, bitmap3;
    private TextView failText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("terry","只做活体检测，不做人脸核验");
        setContentView(R.layout.activity_result_alive);

        btBack = (Button) findViewById(R.id.btBack);
        btBack.setOnClickListener(this);
        btReturn = (ImageView) findViewById(R.id.btReturn);
        btReturn.setOnClickListener(this);
        imgFrontFace = (ImageView) findViewById(R.id.imgFrontFace);
        imgAliveA = (ImageView) findViewById(R.id.imgAliveA);
        imgAliveB = (ImageView) findViewById(R.id.imgAliveB);
        imgAliveC = (ImageView) findViewById(R.id.imgAliveC);
        imgAliveD = (ImageView) findViewById(R.id.imgAliveD);
        imgError = findViewById(R.id.imgError);
        layoutProcess = (LinearLayout) findViewById(R.id.layoutProcess);

        msgAlive = (TextView) findViewById(R.id.msgAlive);

        parseDisplayData(getIntent());

        if (getIntent().getIntExtra("VALUE", 0) == 0) {
            new Handler().postDelayed(new Runnable() {

                @Override
                public void run() {
                    layoutProcess.setVisibility(View.GONE);
                }
            }, 1500);
        } else {
            layoutProcess.setVisibility(View.GONE);
        }


        initView();
    }

    private void parseDisplayData(Intent intent) {
        String alive = intent.getStringExtra("DATA");
        if (alive != null) {
            msgAlive.setText(alive);
        }
        Log.i("terry","alive:"+alive+"___VALUE:"+intent.getIntExtra("VALUE", -1));
        if (intent.getIntExtra("VALUE", -1) == 0) {
            displayImage(((DemoApplication) getApplication()).getSnapData());
        }
    }

    private void displayImage(String str) {
        try {
            Log.i("terry", "displayImage: " + str);
            AEFaceBean bean = JSON.parseObject(str, AEFaceBean.class);
//            save(bean, str);
            String aliveData = bean.getAlive(0);
            if (bean.alive != null) {
                for (int i = 1; i < bean.alive.length; i++) {
                    aliveData = aliveData + "|" + bean.getAlive(i);
                }
            }
            int imageCount = bean.images != null ? bean.images.length : 0;
            int aliveLen = bean.alive != null ? bean.alive.length : 0;
            Log.e("TAG", "***** picnum: " + bean.picnum
                    + ", images.length: " + imageCount
                    + ", alive.length: " + aliveLen);
//            startAlive(aliveData, bean.decryptKey);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Log.i("terry", "data: " + str);
//                        FileUtils.writeFile(str,System.currentTimeMillis()+".txt");
                        testAlive(str);
                        bitmapFrontFace = bean.getFrontFaceBitmap();
                        bitmap0 = bean.getAliveBitmap(0);
                        bitmap1 = bean.getAliveBitmap(1);
                        bitmap2 = bean.getAliveBitmap(2);
                        bitmap3 = bean.getAliveBitmap(3);
                        Message msg = new Message();
                        msg.what = 1;
                        mHandler.sendMessage(msg);
//                        save(bean,"1");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();

        } catch (Exception e) {
            msgAlive.setText(R.string.result_no_face_detected);
            e.printStackTrace();
        }
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    bindPreviewSlot(imgFrontFace, bitmapFrontFace);
                    bindPreviewSlot(imgAliveA, bitmap0);
                    bindPreviewSlot(imgAliveB, bitmap1);
                    bindPreviewSlot(imgAliveC, bitmap2);
                    bindPreviewSlot(imgAliveD, bitmap3);
                    break;
            }
        }
    };


    private void bindPreviewSlot(ImageView view, Bitmap bitmap) {
        if (bitmap != null) {
            view.setVisibility(View.VISIBLE);
            view.setImageBitmap(bitmap);
        } else {
            view.setVisibility(View.GONE);
        }
    }

    private void save(AEFaceBean bean, String str) {
        Bitmap frontFace = bean.getFrontFaceBitmap();
        if (frontFace != null) {
            BitmapUtils.saveBitmap(frontFace, "front_face");
        }
        if (bean.alive != null) {
            for (int i = 0; i < bean.alive.length; i++) {
                Bitmap bmp = bean.getAliveBitmap(i);
                if (bmp != null) {
                    BitmapUtils.saveBitmap(bmp, "alive_" + i);
                }
            }
        }
        if (bean.images != null) {
            for (int i = 0; i < bean.images.length; i++) {
                Bitmap bmp = bean.getFaceImage(i);
                if (bmp != null) {
                    BitmapUtils.saveBitmap(bmp, "face_" + i);
                }
            }
        }
        FLogUtil.saveLogServer(str);
    }

    @Override
    public void onClick(View arg0) {
        finish();
    }

    public void testAlive(String data) throws Exception {
        Log.e("ResultAlive_REQUEST", "data: " + data);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                msgAlive.setText("");
                mProgress = ProgressDialog.show(ResultAliveActivity.this, null, "检测中...");
                mProgress.setOnKeyListener(new OnKeyListener() {

                    @Override
                    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                            mProgress.dismiss();
                        }
                        return false;
                    }
                });
            }
        });

        LiveRequestEntity liveRequestEntity = new LiveRequestEntity();
//        liveRequestEntity.setSeq(DeviceUtil.getDeviceId(this));
        liveRequestEntity.setSn(DeviceUtil.getDeviceId(this));
        liveRequestEntity.setRiskType(DemoApplication.riskType);
        liveRequestEntity.setDeviceMode(DeviceUtil.getManufacturer() + "_" + DeviceUtil.getModel());
        liveRequestEntity.setParam(data);
        String json = JSON.toJSONString(liveRequestEntity);

        // ========== 请求日志 ==========
        String requestUrl = DemoApplication.serverAddr + "/alg-api/liveness/action";
        Log.e("ResultAlive_REQUEST", "========== postAlive2 请求开始 ==========");
        Log.e("ResultAlive_REQUEST", "接口URL: " + requestUrl);
        Log.e("ResultAlive_REQUEST", "请求参数(JSON): " + json);
        Log.e("ResultAlive_REQUEST", "========== 请求结束 ==========");

        ((DemoApplication) getApplication()).postAlive2(this, json, new HttpInterface() {
            @Override
            public int onResponse(LiveResponseBean liveResponseBean) {
                // ========== 响应日志 ==========
                Log.e("ResultAlive_RESPONSE", "========== postAlive2 响应开始 ==========");
                Log.e("ResultAlive_RESPONSE", "响应结果(JSON): " + JSON.toJSONString(liveResponseBean));
                Log.e("ResultAlive_RESPONSE", "result: " + liveResponseBean.getResult() + ", code: " + liveResponseBean.getCode());
                Log.e("ResultAlive_RESPONSE", "info: " + (liveResponseBean.getInfo() != null ? liveResponseBean.getInfo() : "") + ", score: " + liveResponseBean.getScores());
                Log.e("ResultAlive_RESPONSE", "========== 响应结束 ==========");

                if (mProgress != null && mProgress.isShowing()) {
                    mProgress.dismiss();
                }
                String seq = "\n流水号：" + liveResponseBean.getSeq();
                if(liveResponseBean.getDebugInfo() !=null ){
                    if(liveResponseBean.getDebugInfo().getElapse() !=null){
                        //数据包大小、验证时间
                        seq = seq+"\n验证时间: "+liveResponseBean.getDebugInfo().getElapse()+"毫秒";
                    }
                    if(liveResponseBean.getDebugInfo().getReqDataSize()!=null){
                        //数据包大小、验证时间
                        seq = seq+"\n数据包大小: "+liveResponseBean.getDebugInfo().getReqDataSize()+"字节";
                    }
                }
                if (liveResponseBean.getResult() == 0) {
                    if ("1".equals(liveResponseBean.getCode())) {
                        double score = 0;
                        if(liveResponseBean.getScores() !=null){
                            score = JSONObject.parseObject(liveResponseBean.getScores().toString()).getDouble("0");
                        }
                        msgAlive.setText("活体检测成功" + seq+" \n得分："+score);
                    } else {
                        String msg = "活体检测失败 code=" + liveResponseBean.getCode() + seq;
                        msgAlive.setText(msg);
                        if (liveResponseBean.getErrorPic() != null) {
                            msgAlive.setText(msg + "\n失败图：");
                            msgAlive.setTextColor(getResources().getColor(com.sdk.core.R.color.red));
                            Bitmap errorBit = BitmapUtils.convertStringToBitmap(liveResponseBean.getErrorPic());
                            imgError.setImageBitmap(errorBit);
                            imgError.setVisibility(View.VISIBLE);
                        }
                    }
                } else {
                    Toast.makeText(getApplication(), "错误 result=" + liveResponseBean.getResult(), Toast.LENGTH_LONG).show();
                }

                return 0;
            }

            @Override
            public int onPostFailed(int code, String message) {
                // ========== 错误日志 ==========
                Log.e("ResultAlive_ERROR", "========== postAlive2 请求失败 ==========");
                Log.e("ResultAlive_ERROR", "错误码: " + code + ", 错误信息: " + message);
                Log.e("ResultAlive_ERROR", "========== 失败结束 ==========");

                if (mProgress != null && mProgress.isShowing()) {
                    mProgress.dismiss();
                }
                return 0;
            }
        });
    }

//    public void startAlive(String photo, String decryptKey) throws Exception {
//        msgAlive.setText("");
//        mProgress = ProgressDialog.show(this, null, "检测中...");
//        mProgress.setOnKeyListener(new OnKeyListener() {
//
//            @Override
//            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
//                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
//                    mProgress.dismiss();
//                }
//                return false;
//            }
//        });
//        String str = " xmlns=\"http://aeye.com/aeye/schemas\"";
//        String data = "<anyRequest " + str + " ><head><userAccount>real</userAccount><userPassword>1</userPassword><appId>2</appId>" +
//                "<appKey>/alive/aliveCheck</appKey><channelCode>REAL</channelCode><businessCode>REAL</businessCode>" +
//                "</head><body><image>" + photo + "</image><decryptKey>" + decryptKey + "</decryptKey></body></anyRequest>";
//
//        // ========== 请求日志 ==========
//        String requestUrl = DemoApplication.serverAddr + "/alg-api/liveness/action";
//        Log.e("ResultAlive_REQUEST", "========== postAlive 请求开始 ==========");
//        Log.e("ResultAlive_REQUEST", "接口URL: " + requestUrl);
//        Log.e("ResultAlive_REQUEST", "请求参数(XML): " + data);
//        Log.e("ResultAlive_REQUEST", "========== 请求结束 ==========");
//
//        ((DemoApplication) getApplication()).postAlive(this, data, new AliveBean.Response() {
//            @Override
//            public int onResponse(int aliveResult, float minScore) {
//                // ========== 响应日志 ==========
//                Log.e("ResultAlive_RESPONSE", "========== postAlive 响应开始 ==========");
//                Log.e("ResultAlive_RESPONSE", "aliveResult: " + aliveResult + ", minScore: " + minScore);
//                Log.e("ResultAlive_RESPONSE", "检测结果: " + (aliveResult == 0 ? "成功" : "失败"));
//                Log.e("ResultAlive_RESPONSE", "========== 响应结束 ==========");
//
//                if (mProgress != null && mProgress.isShowing()) {
//                    mProgress.dismiss();
//                }
//                if (aliveResult == 0) {
//                    msgAlive.setText("活体检测成功");
//                } else {
//                    msgAlive.setText("活体检测失败");
//                }
//                return 0;
//            }
//        }, new AliveBean.WrongDeal() {
//            @Override
//            public int onPostFailed(int code, String message) {
//                // ========== 错误日志 ==========
//                Log.e("ResultAlive_ERROR", "========== postAlive 请求失败 ==========");
//                Log.e("ResultAlive_ERROR", "错误码: " + code + ", 错误信息: " + message);
//                Log.e("ResultAlive_ERROR", "========== 失败结束 ==========");
//
//                if (mProgress != null && mProgress.isShowing()) {
//                    mProgress.dismiss();
//                }
//                return 0;
//            }
//        });
//    }

    protected void onDestroy() {
        if (mProgress != null && mProgress.isShowing()) {
            mProgress.dismiss();
        }
        super.onDestroy();
    }

    private void initView() {
        failText = (TextView) findViewById(R.id.fail_text);
    }

    ;
}
package com.xym.testface;

import android.app.Activity;
import android.app.Application;
import android.widget.Toast;
import com.aeye.face.AEFaceSdk;
import com.alibaba.fastjson.JSON;
import com.xym.testface.bean.HttpInterface;
import com.xym.testface.bean.LiveResponseBean;
import com.xym.testface.utils.OkHttpClientFactory;
import java.io.IOException;
import java.util.ArrayList;
import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Demo 应用入口：初始化 SDK 接口根地址（甲方部署时修改此处或改为读取配置中心）。
 */
public class DemoApplication extends Application {

    private OkHttpClient mHttpClient;
    public static  String  serverAddr="https://ai-human.a-eye.cn"; // 模拟器访问电脑 localhost 用 10.0.2.2；真机调试改为电脑局域网 IP

    public static  String  gateWayUrl= "https://zshgapp.customs.gov.cn/empgatewayserver/interface/gateway.do";
    public static ArrayList<String> riskType = new ArrayList<>();
    private String snapData;
    @Override
    public void onCreate() {
        super.onCreate();

        AEFaceSdk.init(serverAddr,true);
        AEFaceSdk.setLogSource("4"); // 掌上海关 APP
        AEFaceSdk.setHttpLogEnabled(true);
//        AEFaceSdk.setGatewayUrl(gateWayUrl);
//        AEFaceSdk.setUseGateway(true);
        // USB 调试拦截：true=系统「USB 调试」打开就弹窗；本地联调可改为 false
        AEFaceSdk.setUsbDebugBlockEnabled(false);
        mHttpClient = OkHttpClientFactory.createOkHttpClient();
    }


    public int postAlive2(Activity activity, String data, HttpInterface httpInterface) {
        MediaType mediaType = MediaType.parse("application/json;charset=utf-8");
        RequestBody body = RequestBody.create(mediaType, data);
        Request request = new Request.Builder()
                .cacheControl(new CacheControl.Builder().noCache().build())
                .url(serverAddr+"/alg-api/liveness/action")
                .post(body)
                .build();
        mHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),
                                R.string.net_error, Toast.LENGTH_SHORT).show();
                        if (httpInterface != null) {
                            httpInterface.onPostFailed(-1, e.getMessage());
                        }
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String message=response.body().string();
                    LiveResponseBean bean = JSON.parseObject(message, LiveResponseBean.class);
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (bean != null && bean.getResult() == 0) {
                                if (httpInterface != null) {
                                    httpInterface.onResponse(bean);
                                }
                            } else {
                                int code = bean != null ? bean.getResult() : -1;
                                if (httpInterface != null) {
                                    httpInterface.onPostFailed(code, "header exception");
                                }
                                if (bean != null) {
                                    Toast.makeText(getApplicationContext(),
                                            "response error result=" + bean.getResult()
                                                    + (bean.getInfo() != null ? bean.getInfo() : ""),
                                            Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(getApplicationContext(),
                                            "response parse failed", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    });

                } else {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (httpInterface != null) {
                                httpInterface.onPostFailed(response.code(), response.message());
                            }
                            Toast.makeText(getApplicationContext(),
                                    "connect response exception", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
        return 0;
    }

    public String getSnapData() {
        return snapData;
    }

    public void setSnapData(String snapData) {
        this.snapData = snapData;
    }
}

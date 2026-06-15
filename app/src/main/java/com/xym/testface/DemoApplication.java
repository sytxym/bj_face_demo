package com.xym.testface;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;
import com.aeye.face.AEFaceSdk;
import com.aeye.face.uitls.DeviceSafeCheckUtils;
import com.alibaba.fastjson2.JSON;
import com.lahm.library.EasyProtectorLib;
import com.lahm.library.SecurityCheckUtil;
import com.lahm.library.VirtualApkCheckUtil;
import com.xym.testface.bean.HttpInterface;
import com.xym.testface.bean.LiveResponseBean;
import com.xym.testface.utils.OkHttpClientFactory;
import org.json.JSONArray;
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
    public static ArrayList<String> riskType = new ArrayList<>();
    private String snapData;
    @Override
    public void onCreate() {
        super.onCreate();

        AEFaceSdk.init(serverAddr);
        AEFaceSdk.setLogSource("5"); // 掌上海关 APP
        AEFaceSdk.setHttpLogEnabled(true); //关闭请求日志
        mHttpClient = OkHttpClientFactory.createOkHttpClient();
        ristTypeList(this);
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
                            if (bean !=null && bean.getResult()==0) {
                                httpInterface.onResponse(bean);
                            } else {
                                if (httpInterface != null) {
                                    httpInterface.onPostFailed(bean.getResult(), "header exception");
                                }
                                if(bean !=null && bean.getInfo()!=null )
                                    Toast.makeText(getApplicationContext(),
                                            "response error result="+bean.getResult()+(bean.getInfo()!=null?bean.getInfo():""), Toast.LENGTH_SHORT).show();
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


//    是否使用了VPN 1
//    是否使用了代理 2
//    是否是多开 3
//    是否模拟器  4
//    是否安装改机插件 5
//    是否root  6
//    是否debug  7
//    是否被HOOK  8
//    是否进程被注入  9
//    是否使用Magisk进行系统劫持  10
//    是否伪造地理位置   11
//    是否云模拟器  12
//    是否云真机   13
//    是否开发板设备  14
//    是否处于开发者模式  15
//    是否处于调试模式   16
//    是否进程被动态调试  17
//    是否高危ROM      18
    public static void ristTypeList(Context context){
        if(DeviceSafeCheckUtils.checkVPN(context)){
            String str = "使用了VPN";
            addRiskTypeIntoList(str);
        }
        if(DeviceSafeCheckUtils.isWifiProxy(context)){
            String str = "使用了代理";
            addRiskTypeIntoList(str);
        }
        VirtualApkCheckUtil singleInstance = VirtualApkCheckUtil.getSingleInstance();
        // callback 可传 null，方法会返回 boolean 检测结果
        if (singleInstance.checkByOriginApkPackageName(context, null) ||
                singleInstance.checkByCreateLocalServerSocket("a-eye", null) ||
                singleInstance.checkByHasSameUid(null) ||
                singleInstance.checkByMultiApkPackageName(null) ||
                singleInstance.checkByPrivateFilePath(context, null)
        ) {
            String str = "使用了多开";
            addRiskTypeIntoList(str);
        }
        if(EasyProtectorLib.checkIsRunningInEmulator(context, null)|| DeviceSafeCheckUtils.hasEmulatorAdb()
                || DeviceSafeCheckUtils.hasAppAnalysisPackage(context) || DeviceSafeCheckUtils.hasTaintClass()
                || DeviceSafeCheckUtils.isUserAMonkey()  || DeviceSafeCheckUtils.hasKnownDeviceId(context)
                || DeviceSafeCheckUtils.hasKnownPhoneNumber(context) || DeviceSafeCheckUtils.hasKnownImsi(context)
                || DeviceSafeCheckUtils.hasEmulatorBuild(context) || DeviceSafeCheckUtils.hasQEmuDrivers()
                || DeviceSafeCheckUtils.hasGenyFiles() || DeviceSafeCheckUtils.hasPipes()
        ){
            String str = "模拟器";
            addRiskTypeIntoList(str);
        }
        if(EasyProtectorLib.checkIsXposedExist()){
            String str = "安装改机插件";
            addRiskTypeIntoList(str);
        }
        boolean isRoot = EasyProtectorLib.checkIsRoot() ||
                DeviceSafeCheckUtils.checkBusybox() ||
                DeviceSafeCheckUtils.checkGetRootAuth() ||
                DeviceSafeCheckUtils.checkAccessRootData() || DeviceSafeCheckUtils.checkSuperuserApk() || DeviceSafeCheckUtils.checkRootPathSU();
        if(isRoot){
            String str = "已root";
            addRiskTypeIntoList(str);
        }

        if(SecurityCheckUtil.getSingleInstance().checkIsDebugVersion(context) || SecurityCheckUtil.getSingleInstance().checkIsDebuggerConnected()){
            String str = "debug版本";
            addRiskTypeIntoList(str);
        }
        if(EasyProtectorLib.checkHasLoadSO("Xpose") ||  EasyProtectorLib.checkHasLoadSO("substrate")
                || EasyProtectorLib.checkHasLoadSO("frida")
                || DeviceSafeCheckUtils.isHook(context)){
            String str = "被HOOK";
            addRiskTypeIntoList(str);
        }
        if(EasyProtectorLib.checkHasLoadSO("frida")){
            String str = "进程被注入";
            addRiskTypeIntoList(str);
        }
        if(DeviceSafeCheckUtils.checkMagisk() || DeviceSafeCheckUtils.checkForBinary(context,"magisk")
                ||   DeviceSafeCheckUtils.hasPackageNameInstalled(context,"com.topjohnwu.magisk")){
            String str = "使用Magisk进行系统劫持";
            addRiskTypeIntoList(str);
        }
        if (DeviceSafeCheckUtils.isMockLocation(context)) {
            String str = "伪造地理位置";
            addRiskTypeIntoList(str);
        }
//        if(){
//            String str = "云模拟器";
//            addRiskTypeIntoList(str);
//        }
        if(EasyProtectorLib.checkHasLoadSO("minicap")){
            String str = "云真机";
            addRiskTypeIntoList(str);
        }
        if(DeviceSafeCheckUtils.checkDeviceDebuggable()){
            String str = "开发板设备";
            addRiskTypeIntoList(str);
        }
        if(DeviceSafeCheckUtils.isOpenDevelop(context)){
            String str = "处于开发者模式";
            addRiskTypeIntoList(str);
        }
        if(DeviceSafeCheckUtils.isUsbAdbOpen(context)){
            String str = "处于调试模式";
            addRiskTypeIntoList(str);
        }

        if(EasyProtectorLib.checkIsBeingTracedByJava() || DeviceSafeCheckUtils.getroDebugProp()==1){
            String str = "进程被动态调试";
            addRiskTypeIntoList(str);
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean isTraceed = EasyProtectorLib.checkIsBeingTracedByJava();
                while (!isTraceed) {
                    try {//轮询是否被动态调试
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    isTraceed = EasyProtectorLib.checkIsBeingTracedByJava();
                    if (isTraceed) {
                        String str = "进程被动态调试";
                        addRiskTypeIntoList(str);
                        break;
                    }
                }
            }
        }).start();

        boolean isSystemDebug =  DeviceSafeCheckUtils.checkSystemUser() || DeviceSafeCheckUtils.checkDeviceDebuggable();
        if(  isSystemDebug){
            String str = "高危ROM";
            addRiskTypeIntoList(str);
        }
        Log.e("AeyeTAG",new JSONArray(riskType).toString());
    }

    private static void addRiskTypeIntoList(String str) {
        boolean isContain = false;
        if(riskType !=null && riskType.size()>0){
            for (int i = 0; i < riskType.size(); i++) {
                if(riskType.get(i).equalsIgnoreCase(str)){
                    isContain = true;
                    break;
                }
            }
        }
        if(!isContain) {
            riskType.add(str);
        }
    }

    public String getSnapData() {
        return snapData;
    }

    public void setSnapData(String snapData) {
        this.snapData = snapData;
    }
}

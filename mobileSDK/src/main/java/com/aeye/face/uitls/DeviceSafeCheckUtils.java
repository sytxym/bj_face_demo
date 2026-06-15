package com.aeye.face.uitls;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import androidx.annotation.IntRange;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * create by liulu at 2023/3/23
 **/
public class DeviceSafeCheckUtils {
    public static final String TAG = DeviceSafeCheckUtils.class.getSimpleName();
    public static boolean isDeviceUnSafe() {
        if(checkSystemUser()){return  true;};
//        if (checkDeviceDebuggable()){return true;}//check buildTags
        if (checkSuperuserApk()){return true;}//Superuser.apk
        if (checkRootPathSU()){return true;}//find su in some path
        if (checkRootWhichSU()){return true;}//find su use 'which'
        if (checkBusybox()){return true;}//find su use 'which'
        if (checkAccessRootData()){return true;}//find su use 'which'
        if (checkGetRootAuth()){return true;}//exec su
        return false;
    }

    /**
     * Android判断是否使用VPN
     */
   public static boolean   checkVPN(Context context) {
       ConnectivityManager connMgr = (ConnectivityManager)context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_VPN);
        boolean isVpnConn = networkInfo == null ? false : networkInfo.isConnected();
        return isVpnConn;

    }

    /*
     * 判断设备 是否使用代理上网
     */
    public  static boolean isWifiProxy(Context context) {
        final boolean IS_ICS_OR_LATER = Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;
        String proxyAddress;
        int proxyPort;
        if (IS_ICS_OR_LATER) {
            proxyAddress = System.getProperty("http.proxyHost");
            String portStr = System.getProperty("http.proxyPort");
            proxyPort = Integer.parseInt((portStr != null ? portStr : "-1"));
        } else {
            proxyAddress = android.net.Proxy.getHost(context);
            proxyPort = android.net.Proxy.getPort(context);
        }
        return (!TextUtils.isEmpty(proxyAddress)) && (proxyPort != -1);
    }

    public static boolean checkSystemUser(){
        String buildType = Build.TYPE;
        Log.i(TAG,"buildType="+buildType);
        if (buildType != null && buildType.contains("debug")) {
            return true;
        }
        return false;
    }
    public static boolean checkDeviceDebuggable(){
        String buildTags = Build.TAGS;
        if (buildTags != null && buildTags.contains("test-keys")) {
            Log.i(TAG,"buildTags="+buildTags);
            return true;
        }
        return false;
    }
    public static synchronized boolean checkGetRootAuth()
    {
        Process process = null;
        DataOutputStream os = null;
        try
        {
            Log.i(TAG,"to exec su");
            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes("exit\n");
            os.flush();
            int exitValue = process.waitFor();
            Log.i(TAG, "exitValue="+exitValue);
            if (exitValue == 0)
            {
                return true;
            } else
            {
                return false;
            }
        } catch (Exception e)
        {
            Log.i(TAG, "Unexpected error - Here is what I know: "
                    + e.getMessage());
            return false;
        } finally
        {
            try
            {
                if (os != null)
                {
                    os.close();
                }
                if(process !=null)
                process.destroy();
            } catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    public static synchronized boolean checkBusybox()
    {
        try
        {
            Log.i(TAG,"to exec busybox df");
            String[] strCmd = new String[] {"busybox","df"};
            ArrayList<String> execResult = executeCommand(strCmd);
            if (execResult != null){
                Log.i(TAG,"execResult="+execResult.toString());
                return true;
            }else{
                Log.i(TAG,"execResult=null");
                return false;
            }
        } catch (Exception e)
        {
            Log.i(TAG, "Unexpected error - Here is what I know: "
                    + e.getMessage());
            return false;
        }
    }

    public static synchronized boolean checkAccessRootData()
    {
        try
        {
            Log.i(TAG,"to write /data");
            String fileContent = "test_ok";
            Boolean writeFlag = writeFile("/data/su_test",fileContent);
            if (writeFlag){
                Log.i(TAG,"write ok");
            }else{
                Log.i(TAG,"write failed");
            }
            Log.i(TAG,"to read /data");
            String strRead = readFile("/data/su_test");
            Log.i(TAG,"strRead="+strRead);
            if(fileContent.equals(strRead)){
                return true;
            }else {
                return false;
            }
        } catch (Exception e)
        {
            Log.i(TAG, "Unexpected error - Here is what I know: "
                    + e.getMessage());
            return false;
        }
    }

    //写文件
    public static Boolean writeFile(String fileName,String message){
        try{
            FileOutputStream fout = new FileOutputStream(fileName);
            byte [] bytes = message.getBytes();
            fout.write(bytes);
            fout.close();
            return true;
        }
        catch(Exception e){
            e.printStackTrace();
            return false;
        }
    }
    //读文件
    public static String readFile(String fileName){
        File file = new File(fileName);
        try {
            FileInputStream fis= new FileInputStream(file);
            byte[] bytes = new byte[1024];
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            int len;
            while((len=fis.read(bytes))>0){
                bos.write(bytes, 0, len);
            }
            String result = new String(bos.toByteArray());
            Log.i(TAG, result);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    public static ArrayList<String> executeCommand(String[] shellCmd){
        String line = null;
        ArrayList<String> fullResponse = new ArrayList<String>();
        Process localProcess = null;
        try {
            Log.i(TAG,"to shell exec which for find su :");
            localProcess = Runtime.getRuntime().exec(shellCmd);
        } catch (Exception e) {
            return null;
        }
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(localProcess.getOutputStream()));
        BufferedReader in = new BufferedReader(new InputStreamReader(localProcess.getInputStream()));
        try {
            while ((line = in.readLine()) != null) {
                Log.i(TAG,"–> Line received: " + line);
                fullResponse.add(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.i(TAG,"–> Full response was: " + fullResponse);
        return fullResponse;
    }

    public static boolean checkRootWhichSU() {
        String[] strCmd = new String[] {"/system/xbin/which","su"};
        ArrayList<String> execResult = executeCommand(strCmd);
        if (execResult != null){
            Log.i(TAG,"execResult="+execResult.toString());
            return true;
        }else{
            Log.i(TAG,"execResult=null");
            return false;
        }
    }

    public static boolean checkRootPathSU()
    {
        File f=null;
        final String kSuSearchPaths[]={"/system/bin/","/system/xbin/","/system/sbin/","/sbin/","/vendor/bin/"};
        try{
            for(int i=0;i<kSuSearchPaths.length;i++)
            {
                f=new File(kSuSearchPaths[i]+"su");
                if(f!=null&&f.exists())
                {
                    Log.i(TAG,"find su in : "+kSuSearchPaths[i]);
                    return true;
                }
            }
        }catch(Exception e)
        {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean checkSuperuserApk(){
        try {
            File file = new File("/system/app/Superuser.apk");
            if (file.exists()) {
                Log.i(TAG,"/system/app/Superuser.apk exist");
                return true;
            }
        } catch (Exception e) { }
        return false;
    }

    public  static boolean isOpenDevelop(Context mContext){
        boolean devEnabled = (Settings.Global.getInt(mContext.getContentResolver(), Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,0)>0);
        return  devEnabled;

    }

    public  static boolean isUsbAdbOpen(Context mContext){
        boolean enableAdb = (Settings.Secure.getInt(mContext.getContentResolver(), Settings.Global.ADB_ENABLED, 0) > 0);
        return  enableAdb;
    }

    /**
     * 检测是否magisk
     * @return
     */
    public static synchronized boolean checkMagisk()
    {
        Process process = null;
        DataOutputStream os = null;
        try
        {
            Log.i(TAG,"to exec magisk");
            process = Runtime.getRuntime().exec("magisk");
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes("exit\n");
            os.flush();
            int exitValue = process.waitFor();
            Log.i(TAG, "exitValue="+exitValue);
            if (exitValue == 0)
            {
                return true;
            } else
            {
                return false;
            }
        } catch (Exception e)
        {
            Log.i(TAG, "Unexpected error - Here is what I know: "
                    + e.getMessage());
            return false;
        } finally
        {
            try
            {
                if (os != null)
                {
                    os.close();
                }
                if(process !=null)
                    process.destroy();
            } catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    public static boolean checkForBinary(Context context,String filename) {
        String[] pathsArray =  getPaths();
        boolean flag = false;
        for (String path : pathsArray) {
            String completePath = path + filename;
            File f = new File(path, filename);
            boolean fileExists = f.exists();
            if (fileExists) {
                return  true;
            }
        }
        return  false;
    }
    private static final String[] suPaths = {
            "/data/local/",
            "/data/local/bin/",
            "/data/local/xbin/",
            "/sbin/",
            "/su/bin/",
            "/system/bin/",
            "/system/bin/.ext/",
            "/system/bin/failsafe/",
            "/system/sd/xbin/",
            "/system/usr/we-need-root/",
            "/system/xbin/",
            "/cache/",
            "/data/",
            "/dev/"
    };
    private static String[] getPaths() {
        ArrayList<String> paths = new ArrayList<>(Arrays.asList(suPaths));
        String sysPaths = System.getenv("PATH");
        // If we can't get the path variable just return the static paths
        if (sysPaths == null || "".equals(sysPaths)) {
            return paths.toArray(new String[0]);
        }
        for (String path : sysPaths.split(":")) {
            if (!path.endsWith("/")) {
                path = path + '/';
            }
            if (!paths.contains(path)) {
                paths.add(path);
            }
        }

        return paths.toArray(new String[0]);
    }
    public static int getroDebugProp() {
        int debugProp;
        String roDebugObj = CommandUtil.getSingleInstance().getProperty("ro.debuggable");
        if (roDebugObj == null) debugProp = 1;
        else {
            if ("0".equals(roDebugObj)) debugProp = 0;
            else debugProp = 1;
        }
        return debugProp;
    }

    /**以下方法为判断是否模拟器****/
    public static boolean hasEmulatorAdb() {
        try {
            return hasAdbInEmulator();
        } catch (Exception exception) {
            exception.printStackTrace();
            return false;
        }
    }

    /**
     * 这个方法是通过读取/proc/net/tcp的信息来判断是否存在adb. 比如真机的的信息为0: 4604D20A:B512 A3D13AD8..., 而模拟器上的对应信息就是0: 00000000:0016 00000000:0000, 因为adb通常是反射到0.0.0.0这个ip上
     * @return
     * @throws IOException
     */
    private static boolean hasAdbInEmulator() throws IOException {
        boolean adbInEmulator = false;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream("/proc/net/tcp")), 1000);
            String line;
            // Skip column names
            reader.readLine();

            ArrayList<tcp> tcpList = new ArrayList<tcp>();

            while ((line = reader.readLine()) != null) {
                tcpList.add(tcp.create(line.split("\\W+")));
            }

            reader.close();

            // Adb is always bounce to 0.0.0.0 - though the port can change
            // real devices should be != 127.0.0.1
            int adbPort = -1;
            for (tcp tcpItem : tcpList) {
                if (tcpItem.localIp == 0) {
                    adbPort = tcpItem.localPort;
                    break;
                }
            }

            if (adbPort != -1) {
                for (tcp tcpItem : tcpList) {
                    if ((tcpItem.localIp != 0) && (tcpItem.localPort == adbPort)) {
                        adbInEmulator = true;
                    }
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        } finally {
            reader.close();
        }

        return adbInEmulator;
    }

    public static class tcp {

        public int id;
        public long localIp;
        public int localPort;
        public int remoteIp;
        public int remotePort;

        static tcp create(String[] params) {
            return new tcp(params[1], params[2], params[3], params[4], params[5], params[6], params[7], params[8],
                    params[9], params[10], params[11], params[12], params[13], params[14]);
        }

        public tcp(String id, String localIp, String localPort, String remoteIp, String remotePort, String state,
                   String tx_queue, String rx_queue, String tr, String tm_when, String retrnsmt, String uid,
                   String timeout, String inode) {
            this.id = Integer.parseInt(id, 16);
            this.localIp = Long.parseLong(localIp, 16);
            this.localPort = Integer.parseInt(localPort, 16);
        }
    }


    public static boolean hasPackageNameInstalled(Context context, String packageName) {
        PackageManager packageManager = context.getPackageManager();

        // In theory, if the package installer does not throw an exception, package exists
        try {
            packageManager.getInstallerPackageName(packageName);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    /**
     * 通过检测包名, 检测Taint类来判断是否安装有TaintDroid这个污点分析工具. 另外也还可以检测TaintDroid的一些成员变量
     * @param context
     * @return
     */
    public static boolean hasAppAnalysisPackage(Context context) {
        return hasPackageNameInstalled(context, "org.appanalysis");
    }
    public static boolean hasTaintClass() {
        try {
            Class.forName("dalvik.system.Taint");
            return true;
        }
        catch (ClassNotFoundException exception) {
            return false;
        }
    }

    public static boolean isUserAMonkey() {
        return ActivityManager.isUserAMonkey();
    }

    private static String[] known_device_ids = {"000000000000000", // Default emulator id
            "e21833235b6eef10", // VirusTotal id
            "012345678912345"};
    public static boolean hasKnownDeviceId(Context context) {
        try {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            String deviceId = telephonyManager.getDeviceId();
            for (String known_deviceId : known_device_ids) {
                if (known_deviceId.equalsIgnoreCase(deviceId)) {
                    return true;
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
            return  false;
        }
        return false;
    }

    private static String[] known_numbers = {
            "15555215554", // 模拟器默认电话号码 + VirusTotal
            "15555215556", "15555215558", "15555215560", "15555215562", "15555215564", "15555215566",
            "15555215568", "15555215570", "15555215572", "15555215574", "15555215576", "15555215578",
            "15555215580", "15555215582", "15555215584",};
    public static boolean hasKnownPhoneNumber(Context context) {
        try {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

            String phoneNumber = telephonyManager.getLine1Number();

            for (String number : known_numbers) {
                if (number.equalsIgnoreCase(phoneNumber)) {
                    return true;
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
            return  false;
        }
        return false;
    }


    private static String[] known_imsi_ids = {"310260000000000" // 默认IMSI编号
    };
    public static boolean hasKnownImsi(Context context) {
        try {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            String imsi = telephonyManager.getSubscriberId();

            for (String known_imsi : known_imsi_ids) {
                if (known_imsi.equalsIgnoreCase(imsi)) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }

    public static boolean hasEmulatorBuild(Context context) {
        String BOARD = android.os.Build.BOARD; // The name of the underlying board, like "unknown".
        // This appears to occur often on real hardware... that's sad
        // String BOOTLOADER = android.os.Build.BOOTLOADER; // The system bootloader version number.
        String BRAND = android.os.Build.BRAND; // The brand (e.g., carrier) the software is customized for, if any.
        // "generic"
        String DEVICE = android.os.Build.DEVICE; // The name of the industrial design. "generic"
        String HARDWARE = android.os.Build.HARDWARE; // The name of the hardware (from the kernel command line or
        // /proc). "goldfish"
        String MODEL = android.os.Build.MODEL; // The end-user-visible name for the end product. "sdk"
        String PRODUCT = android.os.Build.PRODUCT; // The name of the overall product.
        if ((BOARD.compareTo("unknown") == 0) /* || (BOOTLOADER.compareTo("unknown") == 0) */
                || (BRAND.compareTo("generic") == 0) || (DEVICE.compareTo("generic") == 0)
                || (MODEL.compareTo("sdk") == 0) || (PRODUCT.compareTo("sdk") == 0)
                || (HARDWARE.compareTo("goldfish") == 0)) {
            return true;
        }
        return false;
    }


    private static String[] known_qemu_drivers = {"goldfish"};
    /**
     * 读取驱动文件, 检查是否包含已知的qemu驱动
     *
     * @return {@code true} if any known drivers where found to exist or {@code false} if not.
     */
    public static boolean hasQEmuDrivers() {
        for (File drivers_file : new File[]{new File("/proc/tty/drivers"), new File("/proc/cpuinfo")}) {
            if (drivers_file.exists() && drivers_file.canRead()) {
                // We don't care to read much past things since info we care about should be inside here
                byte[] data = new byte[1024];
                try {
                    InputStream is = new FileInputStream(drivers_file);
                    is.read(data);
                    is.close();
                } catch (Exception exception) {
                    exception.printStackTrace();
                }

                String driver_data = new String(data);
                for (String known_qemu_driver : known_qemu_drivers) {
                    if (driver_data.indexOf(known_qemu_driver) != -1) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static String[] known_geny_files = {"/dev/socket/genyd", "/dev/socket/baseband_genyd"};
    /**
     * 检查是否存在已知的Genemytion环境文件
     *
     * @return {@code true} if any files where found to exist or {@code false} if not.
     */
    public static boolean hasGenyFiles() {
        for (String file : known_geny_files) {
            File geny_file = new File(file);
            if (geny_file.exists()) {
                return true;
            }
        }

        return false;
    }

    private static String[] known_pipes = {"/dev/socket/qemud", "/dev/qemu_pipe"};
    /**
     * 检查是否存在已知的QEMU使用的管道
     *
     * @return {@code true} if any pipes where found to exist or {@code false} if not.
     */
    public static boolean hasPipes() {
        for (String pipe : known_pipes) {
            File qemu_socket = new File(pipe);
            if (qemu_socket.exists()) {
                return true;
            }
        }

        return false;
    }
    /**判断是否模拟器 方法 end ****/


    /**
     * 是否存在多开应用
     * @param dataDir
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
   public static boolean isDualAppEx(String dataDir) {
        try {
            String simpleName = "wtf_jack";
            String testPath = dataDir + File.separator + simpleName;
            FileOutputStream fos = new FileOutputStream(testPath);
            FileDescriptor fileDescriptor = fos.getFD();
            Field fid_descriptor = fileDescriptor.getClass().getDeclaredField("descriptor");
            fid_descriptor.setAccessible(true);
            // 获取到 fd
            int fd = (Integer) fid_descriptor.get(fileDescriptor);
            // fd 反查真实路径
            String fdPath = String.format("/proc/self/fd/%d", fd);
            String realPath = Files.readSymbolicLink(Paths.get(fdPath)).toString();
            if (!realPath.substring(realPath.lastIndexOf(File.separator))
                    .equals(File.separator + simpleName)){
                // 文件名被修改
                return true;
            }
            // 尝试访问真实路径的父目录
            String fatherDirPath = realPath.replace(simpleName, "..");
            Log.d(TAG, "isDualAppEx: " + fatherDirPath);
            File fatherDir = new File(fatherDirPath);
            if (fatherDir.canRead()) {
                // 父目录可访问
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
        return false;
    }

    /**
     * 是否虚拟定位
     * @param context
     * @return
     */
    public static boolean isMockLocation(Context context) {
        boolean mockPermission = false;
        if (Build.VERSION.SDK_INT <= 22) {//6.0以下
            mockPermission = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.ALLOW_MOCK_LOCATION, 0) == 1;
        } else {
            try {
                LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    mockPermission = false;
                    return mockPermission;
                }
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                    }

                    @Override
                    public void onStatusChanged(String s, int i, Bundle bundle) {
                    }

                    @Override
                    public void onProviderEnabled(String s) {
                    }

                    @Override
                    public void onProviderDisabled(String s) {
                    }
                });
                mockPermission = true;
            } catch (SecurityException e) {
                mockPermission = false;
            }
        }
        return  mockPermission;
    }

    private static boolean findHookAppName(Context context) {
        PackageManager packageManager = context.getPackageManager();
        List<ApplicationInfo> applicationInfoList = packageManager
                .getInstalledApplications(PackageManager.GET_META_DATA);

        for (ApplicationInfo applicationInfo : applicationInfoList) {
            if (applicationInfo.packageName.equals("de.robv.android.xposed.installer")) {
                Log.wtf("HookDetection", "Xposed found on the system.");
                return true;
            }
            if (applicationInfo.packageName.equals("com.saurik.substrate")) {
                Log.wtf("HookDetection", "Substrate found on the system.");
                return true;
            }
        }
        return false;
    }

    private static boolean findHookAppFile() {
        try {
            Set<String> libraries = new HashSet<String>();
            String mapsFilename = "/proc/" + android.os.Process.myPid() + "/maps";
            BufferedReader reader = new BufferedReader(new FileReader(mapsFilename));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.endsWith(".so") || line.endsWith(".jar")) {
                    int n = line.lastIndexOf(" ");
                    libraries.add(line.substring(n + 1));
                }
            }
            reader.close();
            for (String library : libraries) {
                if (library.contains("com.saurik.substrate")) {
                    Log.wtf("HookDetection", "Substrate shared object found: " + library);
                    return true;
                }
                if (library.contains("XposedBridge.jar")) {
                    Log.wtf("HookDetection", "Xposed JAR found: " + library);
                    return true;
                }
            }
        } catch (Exception e) {
            Log.wtf("HookDetection", e.toString());
        }
        return false;
    }

    private static boolean findHookStack() {
        try {
            throw new Exception("findhook");
        } catch (Exception e) {

            // 读取栈信息
            // for(StackTraceElement stackTraceElement : e.getStackTrace()) {
            // Log.wtf("HookDetection", stackTraceElement.getClassName() + "->"+
            // stackTraceElement.getMethodName());
            // }

            int zygoteInitCallCount = 0;
            for (StackTraceElement stackTraceElement : e.getStackTrace()) {
                if (stackTraceElement.getClassName().equals("com.android.internal.os.ZygoteInit")) {
                    zygoteInitCallCount++;
                    if (zygoteInitCallCount == 2) {
                        Log.wtf("HookDetection", "Substrate is active on the device.");
                        return true;
                    }
                }
                if (stackTraceElement.getClassName().equals("com.saurik.substrate.MS$2")
                        && stackTraceElement.getMethodName().equals("invoked")) {
                    Log.wtf("HookDetection", "A method on the stack trace has been hooked using Substrate.");
                    return true;
                }
                if (stackTraceElement.getClassName().equals("de.robv.android.xposed.XposedBridge")
                        && stackTraceElement.getMethodName().equals("main")) {
                    Log.wtf("HookDetection", "Xposed is active on the device.");
                    return true;
                }
                if (stackTraceElement.getClassName().equals("de.robv.android.xposed.XposedBridge")
                        && stackTraceElement.getMethodName().equals("handleHookedMethod")) {
                    Log.wtf("HookDetection", "A method on the stack trace has been hooked using Xposed.");
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isHook(Context context) {
        if (findHookAppName(context) || findHookAppFile() || findHookStack()) {
            return true;
        }
        return false;
    }
}

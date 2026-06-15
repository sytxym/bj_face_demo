//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.aeye.android.uitls;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import com.aeye.android.libutils.ComplexUtil;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommonUtils {
    public CommonUtils() {
    }

    public static String longToString(long time) {
        Date date = new Date(time);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateString = formatter.format(date);
        return dateString;
    }

    public static String getDate( ) {
        Date date = new Date( );
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        String dateString = formatter.format(date);
        return dateString;
    }

    public static boolean isLegal(String content) {
        String str = "[a-zA-Z0-9_一-龥]*";
        Pattern p = Pattern.compile(str);
        Matcher m = p.matcher(content);
        return m.matches();
    }

    public static boolean isIPLegal(String ip) {
        String regEx = "[\\u4e00-\\u9fa5]";
        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(ip);
        return m.matches();
    }

    public static boolean isSFZ(String sfz) {
        Pattern idNumPattern = Pattern.compile("(\\d{14}[0-9a-zA-Z])|(\\d{17}[0-9a-zA-Z])");
        Matcher idNumMatcher = idNumPattern.matcher(sfz);
        return idNumMatcher.matches();
    }

    public static boolean isEmail(String email) {
        String str = "^([a-zA-Z0-9_\\-\\.]+)@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.)|(([a-zA-Z0-9\\-]+\\.)+))([a-zA-Z]{2,4}|[0-9]{1,3})(\\]?)$";
        Pattern p = Pattern.compile(str);
        Matcher m = p.matcher(email);
        return m.matches();
    }

    public static boolean isValidEmail(String mail) {
        Pattern pattern = Pattern.compile("^[A-Za-z0-9][\\w\\._]*[a-zA-Z0-9]+@[A-Za-z0-9-_]+\\.([A-Za-z]{2,4})");
        Matcher mc = pattern.matcher(mail);
        return mc.matches();
    }

    public static String getPhoneImei(Context mContext) {
        TelephonyManager telephonyManager = (TelephonyManager)mContext.getSystemService(Context.TELEPHONY_SERVICE);
        String imei = telephonyManager.getDeviceId();
        return imei;
    }

    public static boolean isMobileNO(String mobiles) {
        String telRegex = "[1][3589]\\d{9}";
        return TextUtils.isEmpty(mobiles) ? false : mobiles.matches(telRegex);
    }

    public static String getNetIpAddress(Context mContext) {
        String ip = null;
        WifiManager wifiManager = (WifiManager)mContext.getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            try {
                Enumeration en = NetworkInterface.getNetworkInterfaces();

                while(en.hasMoreElements()) {
                    NetworkInterface intf = (NetworkInterface)en.nextElement();
                    Enumeration enumIpAddr = intf.getInetAddresses();

                    while(enumIpAddr.hasMoreElements()) {
                        InetAddress inetAddress = (InetAddress)enumIpAddr.nextElement();
                        if (!inetAddress.isLoopbackAddress()) {
                            ip = inetAddress.getHostAddress().toString();
                        }
                    }
                }
            } catch (SocketException var7) {
                Log.e("WifiPreference", var7.toString());
            }
        } else {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            int ipAddress = wifiInfo.getIpAddress();
            ip = intToIp(ipAddress);
        }

        return ip;
    }

    private static String intToIp(int i) {
        return (i & 255) + "." + (i >> 8 & 255) + "." + (i >> 16 & 255) + "." + (i >> 24 & 255);
    }

    public static void showInputMethod(Context context, EditText view) {
        InputMethodManager imm = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
        view.setFocusable(true);
        view.setFocusableInTouchMode(true);
        view.requestFocus();
        imm.showSoftInput(view, 0);
    }

    public static float[] arrayByteToFloat(byte[] b) {
        float[] disDate = new float[b.length / 4];
        ComplexUtil.getInstance().ByteToFloat(b, disDate);
        return disDate;
    }

    public static byte[] arrayFloatToByte(float[] f) {
        byte[] b = new byte[f.length * 4];
        ComplexUtil.getInstance().FloatToByte(b, f);
        return b;
    }

    public static float[] getStringToFloat(String srcStr) {
        byte[] b = Base64.decode(srcStr, 2);
        float[] disDate = new float[b.length / 4];
        ComplexUtil.getInstance().ByteToFloat(b, disDate);
        return disDate;
    }

    public static String getFloatToString(float[] f) {
        byte[] b = new byte[f.length * 4];
        ComplexUtil.getInstance().FloatToByte(b, f);
        String dst = Base64.encodeToString(b, 2);
        return dst;
    }
}

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.aeye.android.libutils;

public class ComplexUtil {
    protected static ComplexUtil m_Instance;

    static {
        System.loadLibrary("ComplexOperate");
        m_Instance = null;
    }

    public ComplexUtil() {
    }

    public native boolean YUVToBitmap(byte[] var1, int[] var2, int var3, int var4, int var5);

    public native boolean YUY2ToBitmap(byte[] var1, int[] var2, int var3, int var4, int var5);

    public native boolean YUVToYR(byte[] var1, byte[] var2, int var3, int var4, int var5, int var6);

    public native boolean YUY2ToYR(byte[] var1, byte[] var2, int var3, int var4, int var5, int var6);

    public native boolean YUY2ToV(byte[] var1, byte[] var2, int var3, int var4);

    public native boolean YUVToBitmapR(byte[] var1, int[] var2, int var3, int var4, int var5, int var6);

    public native byte[] BitmapToYR(int[] var1, int var2, int var3);

    public native boolean FloatToByte(byte[] var1, float[] var2);

    public native boolean ByteToFloat(byte[] var1, float[] var2);

    public native byte[] arrayIntToByte(int[] var1);

    public native int[] arrayByteToInt(byte[] var1);

    public static ComplexUtil getInstance() {
        if (m_Instance == null) {
            m_Instance = new ComplexUtil();
        }

        return m_Instance;
    }
}

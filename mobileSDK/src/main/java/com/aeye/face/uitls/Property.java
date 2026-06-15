//package com.aeye.face.uitls;
//
//import android.content.Context;
//
///**
// * create by liulu at 2023/7/11
// **/
//public class Property {
//    public String name;
//    public String seek_value;
//
//    public Property(String name, String seek_value) {
//        this.name = name;
//        this.seek_value = seek_value;
//    }
//
//    /**
//     * 已知属性, 格式为 [属性名, 属性值], 用于判定当前是否为QEMU环境
//     */
//    private static Property[] known_props = {new Property("init.svc.qemud", null),
//            new Property("init.svc.qemu-props", null), new Property("qemu.hw.mainkeys", null),
//            new Property("qemu.sf.fake_camera", null), new Property("qemu.sf.lcd_density", null),
//            new Property("ro.bootloader", "unknown"), new Property("ro.bootmode", "unknown"),
//            new Property("ro.hardware", "goldfish"), new Property("ro.kernel.android.qemud", null),
//            new Property("ro.kernel.qemu.gles", null), new Property("ro.kernel.qemu", "1"),
//            new Property("ro.product.device", "generic"), new Property("ro.product.model", "sdk"),
//            new Property("ro.product.name", "sdk"),
//            new Property("ro.serialno", null)};
//    /**
//     * 一个阈值, 因为所谓"已知"的模拟器属性并不完全准确, 有可能出现假阳性结果, 因此保持一定的阈值能让检测效果更好
//     */
//    private static int MIN_PROPERTIES_THRESHOLD = 0x5;
//
//    /**
//     * 尝试通过查询指定的系统属性来检测QEMU环境, 最后跟阈值比较得出检测结果.
//     *
//     * @param context A {link Context} object for the Android application.
//     * @return {@code true} if enough properties where found to exist or {@code false} if not.
//     */
//    public boolean hasQEmuProps(Context context) {
//        int found_props = 0;
//
//        for (Property property : known_props) {
//            String property_value = getProp(context, property.name);
//            // See if we expected just a non-null
//            if ((property.seek_value == null) && (property_value != null)) {
//                found_props++;
//            }
//            // See if we expected a value to seek
//            if ((property.seek_value != null) && (property_value.indexOf(property.seek_value) != -1)) {
//                found_props++;
//            }
//
//        }
//
//        if (found_props >= MIN_PROPERTIES_THRESHOLD) {
//            return true;
//        }
//
//        return false;
//    }
//}

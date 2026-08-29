# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Fastjson 1.2.x：保留实现；忽略桌面/Spring 可选 Codec（Guava、AWT、Joda 等）
-keep class com.alibaba.fastjson.** { *; }
-dontwarn com.alibaba.fastjson.**
-dontwarn com.google.common.**
-dontwarn java.awt.**
-dontwarn javax.money.**
-dontwarn org.javamoney.**
-dontwarn org.joda.time.**
-dontwarn springfox.**

# 人脸 SDK AAR：宿主 minify 时二次 R8 会改 JNI 字段名
# libzhysm4.so GetFieldID(Sm2Keys.priKey) — 改名后 native Abort
-keep class com.aeye.sm.** { *; }
-keep class com.aeye.sm4.** { *; }
-keep class com.aeye.sdk.** { *; }
-keep class com.aeye.aeyelib.** { *; }
-keep class com.aeye.android.** { *; }
-dontwarn org.bouncycastle.**
-dontwarn com.aeye.**

# Demo 老接口 /alg-api/liveness/action：Fastjson 按字段名序列化/反序列化。
# minify 后 LiveRequestEntity 字段被改名会打出 {}，LiveResponseBean 解析为 null 再 NPE。
-keep class com.xym.testface.bean.** { *; }
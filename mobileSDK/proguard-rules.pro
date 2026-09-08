# mobileSDK Release 混淆：内部实现可改名，对外 API / JNI / 组件必须保留。
# 宿主调用面（掌上海关 FaceVerifyHelper / FaceSdkInit + Demo）：
#   AEFaceSdk、AEFaceVerifyFlow(+Callback)、AEFacePack、AEFaceInterface、
#   FaceUserInfo、FaceActionConfig、FaceActionOptions、
#   FaceUniResultCodes、FaceUniResultMapper、
#   DeviceSafeCheckUtils、AEFaceBean、FLogUtil、BitmapUtils

-keepattributes SourceFile,LineNumberTable,InnerClasses,EnclosingMethod,Signature,*Annotation*
-renamesourcefileattribute SourceFile

# ---------- 宿主 / Demo 会直接 import 的对外 API ----------
-keep class com.aeye.face.AEFaceSdk { *; }
-keep class com.aeye.face.AEFaceVerifyFlow { *; }
-keep interface com.aeye.face.AEFaceVerifyFlow$Callback { *; }
-keep class com.aeye.face.AEFacePack { *; }
-keep interface com.aeye.face.AEFaceInterface { *; }
-keep class com.aeye.face.AEFaceParam { *; }
-keep class com.aeye.face.AEFaceBean { *; }

-keep class com.aeye.face.verify.FaceUserInfo { *; }
-keep class com.aeye.face.verify.FaceUserInfo$Builder { *; }
-keep class com.aeye.face.config.FaceActionConfig { *; }
-keep class com.aeye.face.config.FaceActionOptions { *; }
-keep class com.aeye.face.config.FaceActionOptions$Builder { *; }

-keep class com.aeye.face.callback.FaceUniResultCodes { *; }
-keep class com.aeye.face.callback.FaceUniResultMapper { *; }

-keep class com.aeye.face.uitls.DeviceSafeCheckUtils { *; }
-keep class com.aeye.face.uitls.FLogUtil { *; }
-keep class com.aeye.android.uitls.BitmapUtils { *; }

# ---------- Manifest 组件：类名写死在 AndroidManifest ----------
-keep class com.aeye.face.view.RecognizeActivity { *; }
-keep class com.aeye.face.confirm.InfoConfirmActivity { *; }
-keep class com.aeye.face.confirm.AgreementWebActivity { *; }
-keep class com.aeye.face.service.InitService { *; }

# ---------- layout XML 自定义 View（inflate 按全限定名找类） ----------
-keep class com.aeye.face.view.FaceView { *; }
-keep class com.aeye.face.confirm.AgreementWebView { *; }
-keep class com.aeye.face.view.CountView { *; }
-keep class com.aeye.face.view.ScanRingOverlayView { *; }
-keep class com.aeye.face.view.AutoFitSurfaceView { *; }
-keep class com.aeye.face.lightView.CheckFaceView { *; }
-keep class com.aeye.face.lightView.BitmapView { *; }

# ---------- Bundle Serializable ----------
-keep class com.aeye.face.uitls.ColorInfo { *; }

# ---------- Fastjson 反射字段名 ----------
-keep class com.aeye.face.api.model.** { *; }
-keep class com.alibaba.fastjson.** { *; }
-dontwarn com.alibaba.fastjson.**
-dontwarn com.alibaba.fastjson2.**
# 1.2.x 内置可选 Codec（Guava/AWT/Joda/Moneta/Swagger），Android 无这些类
-dontwarn com.google.common.**
-dontwarn java.awt.**
-dontwarn javax.money.**
-dontwarn org.javamoney.**
-dontwarn org.joda.time.**
-dontwarn springfox.**

# ---------- JNI：.so 按 Java 类名+方法名查找 ----------
-keepclasseswithmembernames class * {
    native <methods>;
}
-keep class com.aeye.android.uitls.LicenseUtils { *; }
-keep class com.aeye.android.libutils.ComplexUtil { *; }
-keep class com.aeye.android.face.UtilMtcnn { *; }
-keep class com.aeye.aeyelib.ALightNative { *; }

# 算法 jar / native 封装：整包保留，避免 so 对不上
-keep class com.aeye.sdk.** { *; }
-keep interface com.aeye.sdk.** { *; }
-keep class com.aeye.aeyelib.** { *; }
-keep class com.aeye.mylibrary.** { *; }
-keep class com.aeye.sm.** { *; }
-keep class com.aeye.sm4.** { *; }
-keep class com.aeye.android.** { *; }

# ---------- 网关国密 ----------
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# R
-keepclassmembers class **.R$* {
    public static <fields>;
}

-dontwarn com.aeye.**

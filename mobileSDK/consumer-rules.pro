# 随 AAR 提供给宿主。宿主 minifyEnabled=true 时自动合并。
# 只 keep 宿主会引用、以及二次 R8 可能误裁的部分（入口 / 组件 / JNI / XML View）。

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

-keep class com.aeye.face.view.RecognizeActivity { *; }
-keep class com.aeye.face.confirm.InfoConfirmActivity { *; }
-keep class com.aeye.face.confirm.AgreementWebActivity { *; }
-keep class com.aeye.face.service.InitService { *; }

-keep class com.aeye.face.view.FaceView { *; }
-keep class com.aeye.face.confirm.AgreementWebView { *; }
-keep class com.aeye.face.view.CountView { *; }
-keep class com.aeye.face.view.ScanRingOverlayView { *; }
-keep class com.aeye.face.view.AutoFitSurfaceView { *; }
-keep class com.aeye.face.lightView.CheckFaceView { *; }
-keep class com.aeye.face.lightView.BitmapView { *; }

-keepclasseswithmembernames class * {
    native <methods>;
}
-keep class com.aeye.sdk.** { *; }
-keep class com.aeye.aeyelib.** { *; }
-keep class com.aeye.android.** { *; }
# libzhysm4.so JNI GetFieldID("priKey")，宿主二次 R8 改字段名会 Abort
-keep class com.aeye.sm.** { *; }
-keep class com.aeye.sm4.** { *; }
-keep class com.aeye.mylibrary.** { *; }

-keep class com.alibaba.fastjson.** { *; }
-dontwarn com.alibaba.fastjson.**
# Fastjson 1.2.x 可选 Codec，Android 无对应实现，R8 full mode 须忽略
-dontwarn com.google.common.**
-dontwarn java.awt.**
-dontwarn javax.money.**
-dontwarn org.javamoney.**
-dontwarn org.joda.time.**
-dontwarn springfox.**

-dontwarn com.aeye.**
-dontwarn org.bouncycastle.**

# mobileSDK Release 混淆规则
# 目标：保留 com.aeye 下完整包名与类名，供宿主通过 AAR 直接引用；仅裁剪未使用代码。

# 保留行号，便于崩溃栈定位
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# 不再把未 keep 的类重打包到 com.zhy（否则 AAR 里看不到原始包结构）
# -repackageclasses 'com.zhy'

# ---------- SDK 全部业务包（face / android / sdk / aeyelib / mylibrary / sm）----------
-keep class com.aeye.face.** { *; }
-keep interface com.aeye.face.** { *; }

-keep class com.aeye.android.** { *; }
-keep class com.aeye.sdk.** { *; }
-keep interface com.aeye.sdk.** { *; }

-keep class com.aeye.aeyelib.** { *; }
-keep class com.aeye.mylibrary.** { *; }
-keep class com.aeye.sm.** { *; }
-keep class com.aeye.sm4.** { *; }

# 内部回调接口（编译后为 Outer$Inner，宿主实现匿名类时需要保留签名）
-keep interface com.aeye.face.config.FaceActionConfigRepository$FetchCallback { *; }
-keep interface com.aeye.face.config.FaceActionConfigLoader$Callback { *; }
-keep class com.aeye.face.AEFaceSdk { *; }
-keep class com.aeye.face.AEFaceVerifyFlow { *; }
-keep interface com.aeye.face.AEFaceVerifyFlow$Callback { *; }

# AndroidManifest 声明的组件（与上面 face.** 重叠，显式列出便于维护）
-keep class com.aeye.face.view.RecognizeActivity { *; }
-keep class com.aeye.face.lightView.RecognizeLightActivity { *; }
-keep class com.aeye.face.confirm.InfoConfirmActivity { *; }
-keep class com.aeye.face.service.InitService { *; }

# R 资源
-keepclassmembers class **.R$* {
    public static <fields>;
}

# 本地算法 jar 反射/JNI 调用（若存在）
-dontwarn com.aeye.**

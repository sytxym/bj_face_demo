# mobileSDK AAR 打包说明

无需打开 Android Studio，在项目根目录执行脚本或 Gradle 命令即可打包 Release AAR。

## 环境要求

| 依赖 | 说明 |
|------|------|
| JDK | 17 及以上（`java -version` 检查） |
| Android SDK | compileSdk 34，不必安装 Android Studio |
| SDK 路径 | 配置 `local.properties` 或环境变量 `ANDROID_HOME` |

`local.properties` 示例（项目根目录，通常不提交 Git）：

```properties
sdk.dir=C\:\\Users\\你的用户名\\AppData\\Local\\Android\\Sdk
```

## 一键打包（推荐）

Windows 在项目根目录执行（任选一种）：

```bat
build_aar.bat
```

或在资源管理器中 **双击** `build_aar.bat`（窗口会停留，按任意键关闭）。

> **说明**：`build_aar.bat` 使用英文输出，避免 Windows 下 UTF-8 中文导致脚本无法运行。

脚本会执行 Release 打包，并将 AAR 复制到 `dist/` 目录。

## 手动打包

```bat
gradlew.bat :mobileSDK:assembleRelease
```

Git Bash / Linux / macOS：

```bash
./gradlew :mobileSDK:assembleRelease
```

## AAR 输出路径

Gradle 原始输出：

```
mobileSDK/build/outputs/aar/
```

文件名格式（见 `mobileSDK/build.gradle`）：

```
customs-face-sdk-android-v4.18-yyyyMMddHHmm.aar
```

示例：

```
mobileSDK/build/outputs/aar/customs-face-sdk-android-v4.18-202508241030.aar
```

执行 `build_aar.bat` 后，最新 AAR 会额外复制到：

```
dist/
```

## 清理构建产物

```bat
gradlew.bat clean
```

## 打包 Demo APK（可选）

```bat
gradlew.bat :app:assembleRelease
```

APK 输出：

```
app/build/outputs/apk/release/
```

## 常见问题

| 现象 | 处理 |
|------|------|
| `SDK location not found` | 创建 `local.properties` 或设置 `ANDROID_HOME` |
| `JAVA_HOME is not set` | 安装 JDK 17+ 并配置 `JAVA_HOME` |
| 首次构建很慢 | Gradle 在下载依赖与 SDK 组件，属正常现象 |
| 内网无法访问 Google | 配置 Maven / SDK 内网镜像，或由 CI 预构建后交付 AAR |

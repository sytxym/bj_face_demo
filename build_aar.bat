@echo off
setlocal EnableExtensions

echo ===== Build mobileSDK Release AAR =====
echo.

cd /d "%~dp0"

if not exist "gradlew.bat" (
    echo [ERROR] gradlew.bat not found. Run this script from project root.
    goto :fail
)

where java >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java not found. Install JDK 17+ and set JAVA_HOME.
    goto :fail
)

if not exist "local.properties" (
    if not defined ANDROID_HOME (
        echo [ERROR] Android SDK not configured.
        echo Create local.properties in project root, example:
        echo   sdk.dir=C\:\\Android\\Sdk
        echo Or set ANDROID_HOME environment variable.
        goto :fail
    )
)

call gradlew.bat :mobileSDK:assembleRelease --no-daemon
if errorlevel 1 (
    echo.
    echo [ERROR] Gradle build failed.
    goto :fail
)

set "AAR_DIR=mobileSDK\build\outputs\aar"
dir /b "%AAR_DIR%\customs-face-sdk-android-*.aar" >nul 2>&1
if errorlevel 1 (
    echo.
    echo [ERROR] AAR not found in %AAR_DIR%
    goto :fail
)

if not exist "dist" mkdir "dist"
for /f "delims=" %%F in ('dir /b /o-d "%AAR_DIR%\customs-face-sdk-android-*.aar" 2^>nul') do (
    copy /Y "%AAR_DIR%\%%F" "dist\" >nul
    echo.
    echo ===== Build SUCCESS =====
    echo Gradle output: %AAR_DIR%\%%F
    echo Copied to:      dist\%%F
    goto :done_copy
)
echo.
echo [ERROR] Release AAR not found in %AAR_DIR%
goto :fail

:done_copy

echo.
pause
exit /b 0

:fail
echo.
pause
exit /b 1

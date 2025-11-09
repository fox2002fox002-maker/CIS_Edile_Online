@echo off
echo ==============================
echo Building Release APK ...
echo ==============================
gradlew.bat assembleRelease
if %errorlevel% neq 0 (
    echo ❌ Build failed!
    pause
    exit /b %errorlevel%
)
echo.
echo ✅ Done! APK generated at: app\build\outputs\apk\release\CIS_Edile_Online.apk
pause

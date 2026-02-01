@echo off
setlocal

call gradlew.bat :app:assembleDebug
if errorlevel 1 exit /b 1

echo.
echo APK лежит тут:
echo app\build\outputs\apk\debug\
echo.
endlocal

#!/usr/bin/env bash
set -euo pipefail

chmod +x ./gradlew
./gradlew :app:assembleDebug

APK_PATH=$(ls -1 app/build/outputs/apk/debug/*.apk | head -n 1)

echo ""
echo "Готово! APK для установки: ${APK_PATH}"
echo ""

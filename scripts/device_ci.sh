#!/usr/bin/env bash
# Keep execution and artifact collection in one shell; preserve the test result.
set +e
./gradlew connectedDebugAndroidTest --stacktrace
test_result=$?
mkdir -p screenshots
adb pull /sdcard/Pictures/QuotaPalTest/. ./screenshots/ || true
adb shell dumpsys appwidget > screenshots/widget-host-diagnostics.txt
adb logcat -d -s AndroidRuntime GlanceAppWidget AppWidgetServiceImpl > screenshots/device-diagnostics.txt
if [[ "$test_result" -eq 0 && -n "${RELEASE_APK:-}" ]]; then
  bash scripts/release_smoke.sh "$RELEASE_APK" > screenshots/release-smoke.log 2>&1
  test_result=$?
  cat screenshots/release-smoke.log
  adb logcat -d -s AndroidRuntime > screenshots/release-runtime.txt
fi
exit "$test_result"

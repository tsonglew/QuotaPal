#!/usr/bin/env bash
# Keep execution and artifact collection in one shell; preserve the test result.
set +e
./gradlew connectedDebugAndroidTest --stacktrace
test_result=$?
mkdir -p screenshots
adb pull /sdcard/Pictures/QuotaPalTest/. ./screenshots/ || true
adb shell dumpsys appwidget > screenshots/widget-host-diagnostics.txt
adb logcat -d -s AndroidRuntime GlanceAppWidget AppWidgetServiceImpl > screenshots/device-diagnostics.txt
exit "$test_result"

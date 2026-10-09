#!/usr/bin/env bash
# Android 17 can expose boot_completed before framework services accept commands.
set -euo pipefail
: "${ANDROID_HOME:?}" "${DEVICE_API:?}" "${DEVICE_TARGET:?}"
mkdir -p screenshots
emulator_pid=''
cleanup() {
  result=$?
  trap - EXIT
  adb -e logcat -d > screenshots/emulator-logcat.txt 2>&1 || true
  adb -e emu kill >/dev/null 2>&1 || true
  if [[ -n "$emulator_pid" ]]; then wait "$emulator_pid" || true; fi
  exit "$result"
}
trap cleanup EXIT
sdkmanager --install emulator "system-images;android-${DEVICE_API};${DEVICE_TARGET};x86_64"
printf 'no\n' | avdmanager create avd --force -n quotapal-ci \
  --package "system-images;android-${DEVICE_API};${DEVICE_TARGET};x86_64" --device pixel_6
"$ANDROID_HOME/emulator/emulator" -avd quotapal-ci -no-window -no-audio -no-boot-anim \
  -no-snapshot -memory 4096 -cores 2 -gpu swiftshader_indirect > screenshots/emulator-startup.txt 2>&1 &
emulator_pid=$!
python3 scripts/wait_for_android.py "$ANDROID_HOME/platform-tools/adb" 300
adb -e shell input keyevent 82
adb -e shell settings put system screen_off_timeout 2147483647
for setting in window_animation_scale transition_animation_scale animator_duration_scale; do
  adb -e shell settings put global "$setting" 0
done
bash scripts/device_ci.sh

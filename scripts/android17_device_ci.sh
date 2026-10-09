#!/usr/bin/env bash
# Android 17 can expose boot_completed before framework services accept commands.
set -euo pipefail
: "${ANDROID_HOME:?}" "${DEVICE_API:?}" "${DEVICE_TARGET:?}"
mkdir -p screenshots
emulator_pid=''
cleanup() {
  result=$?
  trap - EXIT
  # A dead/offline emulator can leave adb waiting indefinitely even for logcat.
  python3 scripts/bounded_command.py 10 adb -e logcat -d > screenshots/emulator-logcat.txt 2>&1 || true
  python3 scripts/bounded_command.py 10 adb -e logcat -d -b crash -t 200 > screenshots/emulator-crashes.txt 2>&1 || true
  cat screenshots/emulator-crashes.txt
  python3 scripts/bounded_command.py 5 adb -e emu kill >/dev/null 2>&1 || true
  if [[ -n "$emulator_pid" ]]; then
    kill "$emulator_pid" 2>/dev/null || true
    sleep 2
    kill -KILL "$emulator_pid" 2>/dev/null || true
    wait "$emulator_pid" || true
  fi
  if [[ -f screenshots/emulator-startup.txt ]]; then cat screenshots/emulator-startup.txt; fi
  exit "$result"
}
trap cleanup EXIT
for install_attempt in 1 2 3; do
  echo "Installing Android emulator packages (attempt $install_attempt/3)"
  if sdkmanager --install platform-tools emulator "system-images;android-${DEVICE_API};${DEVICE_TARGET};x86_64"; then break; fi
  if [[ "$install_attempt" == 3 ]]; then exit 1; fi
  sleep "$((install_attempt * 2))"
done
export PATH="$ANDROID_HOME/platform-tools:$PATH"
adb version
# 37.2.12 reproducibly aborts SurfaceFlinger on the API 37 guest. Keep the
# guest/API and full test scope; pin the official API-37-capable host runtime.
runtime_root=$(mktemp -d "${RUNNER_TEMP:-/tmp}/quotapal-emulator.XXXXXX")
runtime_archive="$runtime_root/emulator.zip"
curl --fail --location --retry 2 --max-time 300 \
  https://dl.google.com/android/repository/emulator-linux_x64-15917651.zip -o "$runtime_archive"
printf '%s  %s\n' 95771e0ae431897b2a4bd2d97fa095f29a8b0624a7b216baf529f9306161c266 "$runtime_archive" | sha256sum --check
unzip -q "$runtime_archive" -d "$runtime_root"
emulator_binary="$runtime_root/emulator/emulator"
"$emulator_binary" -version
avd_root=$(mktemp -d "${RUNNER_TEMP:-/tmp}/quotapal-avd.XXXXXX")
export ANDROID_USER_HOME="$avd_root"
export ANDROID_EMULATOR_HOME="$avd_root"
export ANDROID_AVD_HOME="$avd_root/avd"
mkdir -p "$ANDROID_AVD_HOME"
printf 'no\n' | avdmanager create avd --force -n quotapal-ci \
  --package "system-images;android-${DEVICE_API};${DEVICE_TARGET};x86_64" --device pixel_6 \
  --path "$ANDROID_AVD_HOME/quotapal-ci.avd"
test -s "$ANDROID_AVD_HOME/quotapal-ci.ini"
printf '\ndisk.dataPartition.size=4G\n' >> "$ANDROID_AVD_HOME/quotapal-ci.avd/config.ini"
"$emulator_binary" -list-avds
"$emulator_binary" -avd quotapal-ci -no-window -no-audio -no-boot-anim \
  -no-snapshot -partition-size 4096 -memory 4096 -cores 2 -gpu software > screenshots/emulator-startup.txt 2>&1 &
emulator_pid=$!
sleep 2
if ! kill -0 "$emulator_pid" 2>/dev/null; then
  echo 'Emulator process exited before framework readiness' >&2
  exit 1
fi
python3 scripts/wait_for_android.py "$ANDROID_HOME/platform-tools/adb" 300 --configure
adb -e shell df -h /data | tee screenshots/emulator-data-space.txt
DEVICE_NATIVE_TESTS=1 bash scripts/device_ci.sh

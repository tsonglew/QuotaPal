#!/usr/bin/env bash
# Owned CI emulators only. Synthetic requests stay inside the test runner.
set -euo pipefail
mkdir -p screenshots
adb -e shell am instrument -r -w -e class com.tsonglew.quotapal.NetworkConstraintDeviceTest \
  -e networkProbe true com.tsonglew.quotapal.test/com.tsonglew.quotapal.QuotaTestRunner \
  > screenshots/network-probe.txt
cat screenshots/network-probe.txt
grep -q '^OK (1 test)' screenshots/network-probe.txt
grep -q '^INSTRUMENTATION_STATUS_CODE: 0' screenshots/network-probe.txt
grep -q '^INSTRUMENTATION_CODE: -1' screenshots/network-probe.txt

#!/usr/bin/env python3
"""Wait for actual framework services, not only the emulator's boot property."""
import subprocess
import sys
import time


def ready(adb):
    def shell(*args):
        result = subprocess.run([adb, '-e', 'shell', *args], capture_output=True, text=True, timeout=10)
        return result.returncode == 0, result.stdout.strip()

    ok, boot = shell('getprop', 'sys.boot_completed')
    if not ok or boot != '1':
        return False
    for service in ('input', 'settings', 'package', 'activity'):
        ok, status = shell('service', 'check', service)
        if not ok or status != f'Service {service}: found':
            return False
    ok, package = shell('pm', 'path', 'android')
    if not ok or not package.startswith('package:'):
        return False
    ok, _ = shell('settings', 'get', 'global', 'window_animation_scale')
    if not ok:
        return False
    ok, _ = shell('input', 'keyevent', '0')
    return ok


def main():
    adb, timeout = sys.argv[1], float(sys.argv[2])
    deadline = time.monotonic() + timeout
    stable = 0
    while time.monotonic() < deadline:
        try:
            stable = stable + 1 if ready(adb) else 0
        except (subprocess.SubprocessError, OSError):
            stable = 0
        if stable >= 3:
            print('Android framework services ready', flush=True)
            return
        time.sleep(2)
    raise SystemExit('Android framework services did not become ready before timeout')


if __name__ == '__main__':
    main()

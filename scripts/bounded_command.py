"""Bound diagnostic commands so an offline device cannot stall CI cleanup."""
import subprocess
import sys


def run(arguments, timeout):
    try:
        return subprocess.run(arguments, timeout=timeout, check=False).returncode
    except subprocess.TimeoutExpired:
        return 124


if __name__ == '__main__':
    raise SystemExit(run(sys.argv[2:], float(sys.argv[1])))

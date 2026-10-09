import importlib.util
from pathlib import Path
import sys
import time
import unittest

spec = importlib.util.spec_from_file_location('bounded_command', Path(__file__).parents[1] / 'bounded_command.py')
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class BoundedCommandTest(unittest.TestCase):
    def test_hung_command_is_killed_and_returns_timeout(self):
        started = time.monotonic()
        self.assertEqual(124, module.run([sys.executable, '-c', 'import time; time.sleep(60)'], .1))
        self.assertLess(time.monotonic() - started, 5)

    def test_command_failure_is_preserved(self):
        self.assertEqual(7, module.run([sys.executable, '-c', 'raise SystemExit(7)'], 5))

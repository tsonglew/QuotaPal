import unittest
from scripts.native_device_tests import verify


def report(passed=18, skipped=2):
    return ''.join('INSTRUMENTATION_STATUS_CODE: 1\nINSTRUMENTATION_STATUS_CODE: 0\n' for _ in range(passed)) + ''.join(
        'INSTRUMENTATION_STATUS_CODE: 1\nINSTRUMENTATION_STATUS_CODE: -3\n' for _ in range(skipped)) + 'INSTRUMENTATION_CODE: -1\n'


class NativeDeviceTests(unittest.TestCase):
    def test_full_suite_with_conditional_skips(self):
        self.assertEqual(18, verify(report()))

    def test_failures_with_successful_shell_exit_rejected(self):
        with self.assertRaises(ValueError):
            verify(report() + 'INSTRUMENTATION_STATUS_CODE: -2\n')

    def test_truncated_or_insufficient_suite_rejected(self):
        for value in (report(0, 20), report(17), report().replace('INSTRUMENTATION_CODE: -1', ''),
                      report() + 'INSTRUMENTATION_STATUS_CODE: 1\n'):
            with self.subTest(value=value), self.assertRaises(ValueError):
                verify(value)

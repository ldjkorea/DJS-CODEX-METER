"""Offline negative tests: boundary gates and reviewed source pins must fail closed."""
from pathlib import Path
import importlib.util
import ast
import json
import subprocess
import tempfile
from types import SimpleNamespace
import unittest

import baseline_subprocess
from lan_review import strip_lan_bytes
from stable311_review import PINS, REVIEWED_COMMIT, reviewed_source, strip_runtime_delta

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('response_bounds', Path(__file__).with_name('verify-release-response-bounds.py'))
bounds = importlib.util.module_from_spec(spec)
spec.loader.exec_module(bounds)


class ReleaseValidationTest(unittest.TestCase):
    def test_pending_summary_cannot_report_success(self):
        script = Path(__file__).with_name('run-stable311-validation.py').read_text(encoding='utf-8')
        function = next(node for node in ast.parse(script).body if isinstance(node, ast.FunctionDef) and node.name == 'summary')
        with tempfile.TemporaryDirectory(dir=ROOT / 'android/build') as directory:
            scope = {'out': Path(directory), 'json': json, 'a': SimpleNamespace(focused=True), 'results': [{'passed': True}]}
            exec(compile(ast.Module(body=[function], type_ignores=[]), '<summary-contract>', 'exec'), scope)
            scope['summary']()
            pending = json.loads((Path(directory) / 'RESULTS.json').read_text())
            self.assertFalse(pending['required_gates_passed'])
            self.assertFalse(pending['completed'])
            scope['summary'](completed=True)
            finished = json.loads((Path(directory) / 'RESULTS.json').read_text())
            self.assertTrue(finished['required_gates_passed'])
            self.assertIsNone(finished['all_legacy_checks_passed'])

    def test_budget_boundaries(self):
        for size, reserve, expected in [
            (bounds.LEGACY_LIMIT - 1, 0, 0), (bounds.LEGACY_LIMIT, 0, 0),
            (bounds.LEGACY_LIMIT + 1, 0, 1),
            (bounds.LEGACY_LIMIT - bounds.DEFAULT_RESERVE, bounds.DEFAULT_RESERVE, 0),
            (bounds.LEGACY_LIMIT - bounds.DEFAULT_RESERVE + 1, bounds.DEFAULT_RESERVE, 2),
        ]:
            with self.subTest(size=size, reserve=reserve):
                raw = b' ' * (size - 2) + b'[]'
                self.assertEqual(expected, bounds.budget(raw, reserve)[0])

    def test_budget_measures_utf8_bytes(self):
        text = ' ' * (bounds.LEGACY_LIMIT - 6) + '["한"]'
        self.assertLessEqual(len(text), bounds.LEGACY_LIMIT)
        self.assertGreater(len(text.encode('utf-8')), bounds.LEGACY_LIMIT)
        self.assertEqual(1, bounds.budget(text.encode('utf-8'), 0)[0])

    def test_invalid_budget_input(self):
        for raw in [b'{}', b'{"message":"rate limit"}', b'[', b'\xff']:
            with self.subTest(raw=raw), self.assertRaises(ValueError):
                bounds.budget(raw)
        for reserve in [-1, bounds.LEGACY_LIMIT + 1]:
            with self.subTest(reserve=reserve), self.assertRaises(ValueError):
                bounds.budget(b'[]', reserve)

    def test_pins_match_approved_commit_and_reject_mutation(self):
        for path in PINS:
            with self.subTest(path=path):
                approved = subprocess.check_output(['git', 'show', REVIEWED_COMMIT + ':' + path], cwd=ROOT)
                self.assertTrue(reviewed_source(path, approved))
                self.assertTrue(reviewed_source(path, approved.replace(b'\r\n', b'\n').replace(b'\n', b'\r\n')))
                with self.assertRaises(AssertionError):
                    reviewed_source(path, approved + b' ')
        self.assertFalse(reviewed_source('android/shared/src/main/java/NewUnreviewed.java', b'anything'))

    def test_runtime_fences_preserve_all_other_original_bytes(self):
        for name in ['OAuthClient', 'RefreshScheduler', 'UsageRefreshJobService']:
            path = 'android/app/src/main/java/dev/bennett/codexmeter/' + name + '.java'
            current = (ROOT / path).read_text(encoding='utf-8')
            original = baseline_subprocess.check_output(['git', 'show', '7de3b0cf103f7bfba8464eeb4bd97557c4454c99:' + path]).replace(b'\r\n', b'\n')
            undo = lambda text: strip_lan_bytes(path, strip_runtime_delta(path, text).encode()).replace(b'\r\n', b'\n')
            with self.subTest(path=path):
                self.assertEqual(original, undo(current))
                self.assertNotEqual(original, undo(current + '\n// unreviewed change\n'))
                with self.assertRaises(AssertionError):
                    strip_runtime_delta(path, current.replace('TrialDemo.active', 'TrialDemo.changed').replace('TrialDemo.ACCOUNT', 'TrialDemo.CHANGED'))


if __name__ == '__main__':
    unittest.main(verbosity=2)

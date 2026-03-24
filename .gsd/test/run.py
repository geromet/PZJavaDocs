#!/usr/bin/env python3
"""
Comprehensive automated testing framework for PZ Lua API Viewer.
Run: python .gsd/test/run.py
"""

from __future__ import annotations

import json
import subprocess
import sys
import time
from datetime import datetime
from pathlib import Path
from types import SimpleNamespace
from urllib.request import urlopen

from playwright.sync_api import sync_playwright

# Add parent to path for imports
TEST_DIR = Path(__file__).resolve().parent
PROJECT_DIR = TEST_DIR.parent.parent
sys.path.insert(0, str(TEST_DIR))
sys.path.insert(0, str(TEST_DIR.parent))

from config import REPORT_DIR, SERVER_SCRIPT, SERVER_URL  # noqa: E402
import detail  # noqa: E402
import globals as globals_tests  # noqa: E402
import inheritance  # noqa: E402
import navigation  # noqa: E402
import regression  # noqa: E402
import s02_features  # noqa: E402
import s08_navigation_state  # noqa: F401,E402
import search  # noqa: E402

LEGACY_GROUPS = [
    (
        "S02 Feature Tests",
        [
            s02_features.test_middle_click_new_tab,
            s02_features.test_hover_preview_card,
        ],
    ),
    (
        "Navigation Tests",
        [
            navigation.test_back_forward_buttons,
            navigation.test_nav_hash_change,
        ],
    ),
    (
        "Search Tests",
        [
            search.test_search_basic,
            search.test_search_scope_toggle,
        ],
    ),
    (
        "Globals Tests",
        [
            globals_tests.test_globals_panel,
            globals_tests.test_globals_search,
        ],
    ),
    (
        "Detail View Tests",
        [
            detail.test_class_detail_panel,
            detail.test_copy_fqn,
        ],
    ),
    (
        "Inheritance Tests",
        [inheritance.test_inheritance_chain],
    ),
    (
        "Regression Tests",
        [
            regression.test_tab_navigation,
            regression.test_class_link_clicks,
        ],
    ),
]

S07_COMMAND = [sys.executable, "-m", "pytest", ".gsd/test/s07_ux_polish.py", "-q"]
S08_COMMAND = [sys.executable, "-m", "pytest", ".gsd/test/s08_navigation_state.py", "-q"]
all_results: list[dict] = []


class ManagedServer:
    def __init__(self, script: Path, url: str):
        self.script = script
        self.url = url
        self.proc: subprocess.Popen[str] | None = None

    def __enter__(self):
        self.proc = subprocess.Popen(
            [sys.executable, str(self.script)],
            cwd=PROJECT_DIR,
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
            text=True,
        )

        deadline = time.time() + 20
        last_error = None
        while time.time() < deadline:
            try:
                with urlopen(self.url, timeout=1) as response:
                    if response.status == 200:
                        return self
            except Exception as exc:  # pragma: no cover - startup polling only
                last_error = exc
                time.sleep(0.25)

        self.close()
        raise RuntimeError(f"Server did not become ready: {last_error}")

    def __exit__(self, exc_type, exc, tb):
        self.close()

    def close(self):
        if not self.proc:
            return
        self.proc.terminate()
        try:
            self.proc.wait(timeout=5)
        except subprocess.TimeoutExpired:
            self.proc.kill()
            self.proc.wait(timeout=5)
        finally:
            self.proc = None


class BrowserHarness:
    def __enter__(self):
        self.playwright = sync_playwright().start()
        self.browser = self.playwright.chromium.launch(headless=True)
        self.context = self.browser.new_context(
            viewport={"width": 1920, "height": 1080},
            user_agent="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
        )
        self.page = self.context.new_page()
        self.page.goto(SERVER_URL + "/")
        return self

    def __exit__(self, exc_type, exc, tb):
        self.context.close()
        self.browser.close()
        self.playwright.stop()


class LegacyBrowserShim(SimpleNamespace):
    @property
    def context(self):
        return self.contexts[0]


def inject_browser_globals(browser):
    shim = LegacyBrowserShim(contexts=[browser.context])
    for module in (s02_features, navigation, search, globals_tests, detail, inheritance, regression):
        module.browser = shim


def run_legacy_tests(browser):
    total_groups = len(LEGACY_GROUPS) + 2
    for index, (group_name, tests) in enumerate(LEGACY_GROUPS, start=1):
        print(f"\n[{index}/{total_groups}] Running {group_name}...")
        for test_func in tests:
            page = browser.context.new_page()
            try:
                result = test_func(page)
                print_result(result)
            finally:
                for extra_page in list(browser.context.pages):
                    if extra_page is not page:
                        extra_page.close()
                page.close()


def run_pytest_module(index: int, total: int, name: str, command: list[str]):
    print(f"\n[{index}/{total}] Running {name}...")
    started = time.perf_counter()
    proc = subprocess.run(
        command,
        cwd=PROJECT_DIR,
        capture_output=True,
        text=True,
    )
    duration = round(time.perf_counter() - started, 2)

    if proc.stdout:
        print(proc.stdout.strip())
    if proc.stderr:
        print(proc.stderr.strip())

    result = {
        "name": name.lower().replace(" ", "-"),
        "passed": proc.returncode == 0,
        "message": "Standalone pytest module passed" if proc.returncode == 0 else (proc.stdout + "\n" + proc.stderr).strip()[-4000:],
        "command": " ".join(command),
        "duration_seconds": duration,
    }
    print_result(result)


def run_s07_pytest():
    run_pytest_module(len(LEGACY_GROUPS) + 1, len(LEGACY_GROUPS) + 2, "S07 UX Polish Tests", S07_COMMAND)


def run_s08_pytest():
    run_pytest_module(len(LEGACY_GROUPS) + 2, len(LEGACY_GROUPS) + 2, "S08 Navigation State Tests", S08_COMMAND)


def run_all_tests():
    """Run all tests and generate report."""
    print("\n" + "=" * 60)
    print("PZ Lua API Viewer — Automated Test Suite")
    print("=" * 60)
    print(f"\nTest run started at: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")

    with ManagedServer(SERVER_SCRIPT, SERVER_URL):
        with BrowserHarness() as browser:
            inject_browser_globals(browser)
            run_legacy_tests(browser)
        run_s07_pytest()
        run_s08_pytest()

    _save_report()

    passed = sum(1 for r in all_results if r["passed"])
    total = len(all_results)
    print("\n" + "=" * 60)
    print(f"Test Summary: {passed}/{total} tests passed")
    print("=" * 60)

    return all(r["passed"] for r in all_results)


def print_result(result):
    """Print a test result."""
    status = "PASS" if result["passed"] else "FAIL"
    print(f"  [{status}] {result['name']}: {result.get('message', 'N/A')}")
    all_results.append(result)


def _save_report():
    """Save a JSON report of all test results."""
    REPORT_DIR.mkdir(parents=True, exist_ok=True)

    screenshots = []
    for result in all_results:
        screenshot = result.get("screenshot")
        if screenshot and isinstance(screenshot, dict) and "path" in screenshot:
            screenshots.append({"test": result["name"], **screenshot})

    report = {
        "run_date": datetime.now().isoformat(),
        "total_tests": len(all_results),
        "passed": sum(1 for r in all_results if r["passed"]),
        "failed": sum(1 for r in all_results if not r["passed"]),
        "results": all_results,
        "screenshots": screenshots,
    }

    json_path = REPORT_DIR / "report.json"
    json_path.write_text(json.dumps(report, indent=2))
    print(f"\n[INFO] Report saved to {json_path}")


if __name__ == "__main__":
    success = run_all_tests()
    sys.exit(0 if success else 1)

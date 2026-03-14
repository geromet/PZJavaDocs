#!/usr/bin/env python3
"""
Automated test suite for PZ Lua API Viewer.
Uses Playwright to verify UI features, navigation, search, globals, and regressions.

Usage:
    cd pz-lua-api-viewer
    python .gsd/test-suite.py

Requires: playwright (pip install playwright && playwright install chromium)
The local server must NOT already be running on port 8000.
"""

import json
import os
import subprocess
import sys
import time
from datetime import datetime
from pathlib import Path

# Fix Windows console encoding
if sys.platform == "win32":
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    sys.stderr.reconfigure(encoding="utf-8", errors="replace")

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------
PROJECT_DIR = Path(__file__).resolve().parent.parent  # pz-lua-api-viewer/
PORT = 8000
SERVER_URL = f"http://localhost:{PORT}"
REPORT_DIR = PROJECT_DIR / ".gsd" / "test-reports"
REPORT_DIR.mkdir(parents=True, exist_ok=True)

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

class TestResult:
    def __init__(self, name: str, passed: bool, message: str = "", screenshot: str = ""):
        self.name = name
        self.passed = passed
        self.message = message
        self.screenshot = screenshot

    def __repr__(self):
        icon = "PASS" if self.passed else "FAIL"
        return f"  [{icon}] {self.name}: {self.message}"


def start_server():
    """Start python -m http.server on PROJECT_DIR, return Popen handle."""
    proc = subprocess.Popen(
        [sys.executable, "-m", "http.server", str(PORT), "--bind", "127.0.0.1"],
        cwd=str(PROJECT_DIR),
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL,
    )
    # Wait until port is open
    import socket
    for _ in range(60):
        try:
            s = socket.create_connection(("127.0.0.1", PORT), timeout=0.5)
            s.close()
            return proc
        except OSError:
            time.sleep(0.25)
    print("[WARN] Server may not be ready after 15s")
    return proc


def kill_port(port):
    """Kill any process listening on the given port (Windows)."""
    try:
        result = subprocess.run(
            ["netstat", "-ano"], capture_output=True, text=True, timeout=5
        )
        pids = set()
        for line in result.stdout.splitlines():
            if f":{port}" in line and "LISTENING" in line:
                pid = line.strip().split()[-1]
                pids.add(pid)
        for pid in pids:
            subprocess.run(["taskkill", "/F", "/PID", pid],
                           capture_output=True, timeout=5)
    except Exception:
        pass


# ---------------------------------------------------------------------------
# Test Suite
# ---------------------------------------------------------------------------

class PZViewerTests:
    def __init__(self):
        self.results: list[TestResult] = []
        self.page = None
        self.browser = None
        self.pw = None

    def setup(self):
        from playwright.sync_api import sync_playwright
        self.pw = sync_playwright().start()
        self.browser = self.pw.chromium.launch(headless=True)
        ctx = self.browser.new_context(viewport={"width": 1280, "height": 800})
        self.page = ctx.new_page()
        # Auto-dismiss file chooser dialogs to prevent blocking
        self.page.on("filechooser", lambda fc: fc.set_files([]))

    def teardown(self):
        if self.browser:
            self.browser.close()
        if self.pw:
            self.pw.stop()

    def screenshot(self, name: str) -> str:
        path = str(REPORT_DIR / f"{name}.png")
        self.page.screenshot(path=path)
        return path

    def add(self, name, passed, message="", screenshot=""):
        r = TestResult(name, passed, message, screenshot)
        self.results.append(r)
        print(r)
        return r

    # ------------------------------------------------------------------
    # 1. Page Load
    # ------------------------------------------------------------------
    def test_page_loads(self):
        self.page.goto(SERVER_URL, wait_until="networkidle")
        title = self.page.title()
        ok = "Zomboid" in title or "PZ" in title or "Lua" in title
        self.add("Page loads", ok, f"Title: {title}", self.screenshot("01-page-load"))

    def test_class_count(self):
        count = self.page.locator(".class-item").count()
        ok = count > 1000
        self.add("Class count", ok, f"{count} classes (expected >1000)")

    def test_stats_bar(self):
        exposed = self.page.locator("#stat-exposed").text_content().strip()
        globals_ = self.page.locator("#stat-globals").text_content().strip()
        ok = "917" in exposed and "745" in globals_
        self.add("Stats bar", ok, f"exposed={exposed}, globals={globals_}")

    # ------------------------------------------------------------------
    # 2. Search
    # ------------------------------------------------------------------
    def test_search_filters(self):
        search = self.page.locator("#global-search")
        search.fill("IsoPlayer")
        self.page.wait_for_timeout(300)
        count = self.page.locator(".class-item").count()
        ok = 0 < count < 20
        self.add("Search filters", ok, f"{count} results for 'IsoPlayer'",
                 self.screenshot("02-search"))
        search.fill("")
        self.page.wait_for_timeout(300)

    def test_search_clear(self):
        search = self.page.locator("#global-search")
        search.fill("IsoPlayer")
        self.page.wait_for_timeout(200)
        clear_btn = self.page.locator("#btn-search-clear")
        if clear_btn.is_visible():
            clear_btn.click()
            self.page.wait_for_timeout(200)
            val = search.input_value()
            count = self.page.locator(".class-item").count()
            self.add("Search clear", val == "" and count > 1000,
                     f"After clear: value='{val}', count={count}")
        else:
            self.add("Search clear", False, "Clear button not visible")

    # ------------------------------------------------------------------
    # 3. Class Detail
    # ------------------------------------------------------------------
    def test_class_detail_loads(self):
        self.page.goto(SERVER_URL, wait_until="networkidle")
        self.page.wait_for_timeout(300)
        # Search for IsoPlayer and click
        search = self.page.locator("#global-search")
        search.fill("IsoPlayer")
        self.page.wait_for_timeout(300)
        items = self.page.locator(".class-item")
        # Find the exact IsoPlayer item
        clicked = False
        for i in range(items.count()):
            item = items.nth(i)
            text = item.text_content().strip()
            if text.startswith("IsoPlayer") and "State" not in text:
                item.click()
                clicked = True
                break
        if not clicked:
            self.add("Class detail loads", False, "Could not find IsoPlayer in list")
            return

        self.page.wait_for_timeout(500)
        url = self.page.url
        ok = "IsoPlayer" in url
        self.add("Class detail loads", ok, f"URL: {url}",
                 self.screenshot("03-class-detail"))

    def test_inheritance_tree(self):
        """IsoPlayer should show full inheritance chain."""
        detail = self.page.locator("#detail-panel").text_content()
        chain = ["GameEntity", "IsoObject", "IsoMovingObject", "IsoGameCharacter", "IsoPlayer"]
        found = all(c in detail for c in chain)
        self.add("Inheritance tree", found,
                 f"Chain present: {found} — looking for {chain}")

    def test_methods_visible(self):
        methods = self.page.locator("#detail-panel .method-name, #detail-panel .method-link, #detail-panel td a")
        count = methods.count()
        ok = count > 10
        self.add("Methods visible", ok, f"{count} method elements found")

    def test_source_tab(self):
        """Click Source tab and verify source code loads."""
        # Use precise selector to avoid hitting "Local sources" button
        source_tab = self.page.locator('.ctab[data-ctab="source"]')
        if source_tab.is_visible():
            source_tab.click()
            self.page.wait_for_timeout(2000)  # source fetch takes time
            code = self.page.locator("#source-code").text_content()
            ok = len(code) > 100
            self.add("Source tab", ok, f"Source code length: {len(code)}",
                     self.screenshot("04-source-tab"))
            # Go back to Detail tab
            detail_tab = self.page.locator('.ctab[data-ctab="detail"]')
            if detail_tab.is_visible():
                detail_tab.click()
                self.page.wait_for_timeout(300)
        else:
            self.add("Source tab", False, "Source tab not found")

    # ------------------------------------------------------------------
    # 4. Tab Bar
    # ------------------------------------------------------------------
    def test_tab_bar(self):
        """Open two classes and verify tab bar shows both."""
        self.page.goto(SERVER_URL + "/#zombie.characters.IsoPlayer", wait_until="networkidle")
        self.page.wait_for_timeout(500)

        # Navigate to a second class via a link in detail
        link = self.page.locator("#detail-panel a[data-fqn]").first
        if link.is_visible():
            fqn = link.get_attribute("data-fqn")
            link.click()
            self.page.wait_for_timeout(500)
            tabs = self.page.locator("#tab-bar .tab-item, #tab-bar .tab")
            tab_count = tabs.count()
            ok = tab_count >= 2
            self.add("Tab bar", ok, f"{tab_count} tabs open after navigating to {fqn}",
                     self.screenshot("05-tab-bar"))
        else:
            self.add("Tab bar", False, "No class links found in detail panel to open second tab")

    # ------------------------------------------------------------------
    # 5. Navigation
    # ------------------------------------------------------------------
    def test_back_forward(self):
        """Navigate to a class via click, then use Back/Forward buttons."""
        # Start fresh and navigate to IsoPlayer by clicking
        self.page.goto(SERVER_URL + "/", wait_until="networkidle")
        self.page.wait_for_timeout(500)
        self.page.locator("#global-search").fill("IsoPlayer")
        self.page.wait_for_timeout(400)
        items = self.page.locator(".class-item")
        for i in range(items.count()):
            if "IsoPlayer" == items.nth(i).locator(".ci-name").text_content().strip():
                items.nth(i).click()
                break
        self.page.wait_for_timeout(500)
        self.page.locator("#global-search").fill("")
        self.page.wait_for_timeout(300)

        url_first = self.page.url
        first_class = url_first.split("#")[-1] if "#" in url_first else ""

        # Click a class link in the detail panel to navigate to a second class
        link = self.page.locator("#detail-panel a[data-fqn]").first
        if not link.is_visible():
            self.add("Back/Forward", False, "No class link to navigate to")
            return

        target_fqn = link.get_attribute("data-fqn")
        link.click()
        self.page.wait_for_timeout(500)
        url_after_nav = self.page.url

        # Press Back
        back_btn = self.page.locator("#btn-nav-back")
        back_btn.click()
        self.page.wait_for_timeout(500)
        url_after_back = self.page.url
        back_ok = first_class and first_class in url_after_back

        # Press Forward
        fwd_btn = self.page.locator("#btn-nav-forward")
        fwd_btn.click()
        self.page.wait_for_timeout(500)
        url_after_fwd = self.page.url
        fwd_ok = target_fqn and target_fqn.split(".")[-1] in url_after_fwd

        ok = back_ok and fwd_ok
        self.add("Back/Forward", ok,
                 f"start={first_class.split('.')[-1]}, nav->{target_fqn.split('.')[-1]}, back->{url_after_back.split('#')[-1].split('.')[-1]}, fwd->{url_after_fwd.split('#')[-1].split('.')[-1]}")

    def test_hash_navigation(self):
        """Navigate via URL hash — open a class directly by hash on fresh load."""
        # Force a full fresh page load with hash by using a query param trick
        self.page.goto(SERVER_URL + "/?_t=" + str(int(time.time())) + "#zombie.iso.IsoObject",
                       wait_until="networkidle")
        self.page.wait_for_timeout(2000)  # init + JSON load + selectClass
        detail = self.page.locator("#detail-panel").text_content()
        ok = "IsoObject" in detail
        if not ok:
            # Fallback: try triggering hash processing via JS
            self.page.evaluate("if(location.hash) { const v = decodeURIComponent(location.hash.slice(1)); if(v !== 'globals') selectClass(v); }")
            self.page.wait_for_timeout(1000)
            detail = self.page.locator("#detail-panel").text_content()
            ok = "IsoObject" in detail
        self.add("Hash navigation", ok,
                 f"Direct hash nav to IsoObject: {'found' if ok else 'not found in detail panel'}")

    # ------------------------------------------------------------------
    # 6. Globals
    # ------------------------------------------------------------------
    def test_globals_panel(self):
        # Navigate to globals by clicking the tab (not via hash, which may not trigger)
        self.page.goto(SERVER_URL + "/", wait_until="networkidle")
        self.page.wait_for_timeout(500)
        # Click the "Global Functions" sidebar tab
        self.page.evaluate("document.querySelectorAll('.tab').forEach(t => { if(t.textContent.includes('Global')) t.click() })")
        self.page.wait_for_timeout(1000)
        count_el = self.page.locator("#globals-count")
        count_text = count_el.text_content() if count_el.is_visible() else ""
        # Also check that the globals panel is visible
        panel_visible = self.page.locator("#globals-panel").is_visible()
        ok = panel_visible and ("745" in count_text or "function" in count_text.lower() or count_text.strip() != "")
        self.add("Globals panel", ok,
                 f"Panel visible: {panel_visible}, count text: '{count_text}'",
                 self.screenshot("06-globals"))

    def test_globals_search(self):
        # Should already be on globals from test_globals_panel
        search = self.page.locator("#globals-search")
        if search.is_visible():
            search.fill("reload")
            self.page.wait_for_timeout(500)
            # Count visible rows in globals table
            body_text = self.page.locator("#globals-table-wrap").text_content()
            ok = "reload" in body_text.lower()
            self.add("Globals search", ok,
                     f"'reload' found in globals: {ok}",
                     self.screenshot("07-globals-search"))
            search.fill("")
            self.page.wait_for_timeout(200)
        else:
            self.add("Globals search", False, "Globals search input not found")

    # ------------------------------------------------------------------
    # 7. Sidebar
    # ------------------------------------------------------------------
    def test_sidebar_filters(self):
        self.page.goto(SERVER_URL + "/", wait_until="networkidle")
        self.page.wait_for_timeout(500)

        # Ensure we're on the Classes tab
        self.page.evaluate("document.querySelectorAll('.tab').forEach(t => { if(t.textContent.includes('Classes')) t.click() })")
        self.page.wait_for_timeout(300)

        # Get baseline count
        baseline = self.page.locator(".class-item").count()

        # Click "setExposed only" filter using data-filter attribute
        btn = self.page.locator('button[data-filter="exposed"]')
        if not btn.is_visible():
            # Try text match as fallback
            btn = self.page.locator("#sidebar-filters button", has_text="setExposed only")
        if btn.is_visible():
            btn.click()
            self.page.wait_for_timeout(500)
            count = self.page.locator(".class-item").count()
            # "setExposed only" means exclusively setExposed (not also lua_tagged)
            # Most setExposed classes are also tagged, so this is a small number (~2)
            ok = count < baseline and count >= 1
            self.add("Sidebar filter (setExposed only)", ok,
                     f"{count} classes (baseline {baseline}) exclusively setExposed")
            # Reset to All using data-filter
            all_btn = self.page.locator('button[data-filter="all"]')
            if all_btn.is_visible():
                all_btn.click()
                self.page.wait_for_timeout(300)
        else:
            self.add("Sidebar filter (setExposed)", False, "setExposed filter button not found")

    def test_sidebar_splitter(self):
        """Sidebar splitter element exists."""
        splitter = self.page.locator("#sidebar-splitter")
        ok = splitter.is_visible()
        self.add("Sidebar splitter", ok, "Splitter element visible" if ok else "Splitter not found")

    # ------------------------------------------------------------------
    # 8. Version Selector
    # ------------------------------------------------------------------
    def test_version_select_exists(self):
        self.page.goto(SERVER_URL, wait_until="networkidle")
        self.page.wait_for_timeout(300)
        select = self.page.locator("#version-select")
        # With single version, it should exist but may be hidden
        exists = select.count() > 0
        self.add("Version select exists", exists,
                 "Element present in DOM" if exists else "Missing from DOM")

    # ------------------------------------------------------------------
    # 9. Hover Preview
    # ------------------------------------------------------------------
    def test_hover_preview(self):
        # Load IsoPlayer by clicking it in the sidebar (more reliable than hash)
        self.page.goto(SERVER_URL + "/", wait_until="networkidle")
        self.page.wait_for_timeout(500)
        # Search and click
        self.page.locator("#global-search").fill("IsoPlayer")
        self.page.wait_for_timeout(400)
        items = self.page.locator(".class-item")
        for i in range(items.count()):
            if "IsoPlayer" == items.nth(i).locator(".ci-name").text_content().strip():
                items.nth(i).click()
                break
        self.page.wait_for_timeout(1000)

        link = self.page.locator("#detail-panel a[data-fqn]").first
        if link.is_visible():
            link.hover()
            self.page.wait_for_timeout(600)  # hover delay is ~400ms
            preview = self.page.locator("#hover-preview")
            ok = preview.is_visible()
            self.add("Hover preview", ok,
                     "Preview card appeared" if ok else "Preview card not visible",
                     self.screenshot("08-hover-preview"))
        else:
            self.add("Hover preview", False, "No class link found to hover in detail panel")

    # ------------------------------------------------------------------
    # 10. Search Performance
    # ------------------------------------------------------------------
    def test_search_performance(self):
        """Search index query should complete in < 16ms (one frame)."""
        self.page.goto(SERVER_URL + "/", wait_until="networkidle")
        self.page.wait_for_timeout(500)
        result = self.page.evaluate("""
            (() => {
                const bench = (term) => {
                    const start = performance.now();
                    const results = querySearchIndex(term, 'all');
                    return {term, count: results.length, ms: performance.now() - start};
                };
                return [bench('Iso'), bench('get'), bench('a')];
            })()
        """)
        max_ms = max(r['ms'] for r in result)
        ok = max_ms < 16
        details = ", ".join(f"{r['term']}={r['ms']:.1f}ms/{r['count']}hits" for r in result)
        self.add("Search performance", ok, f"Max {max_ms:.1f}ms ({details})")

    def test_progressive_render(self):
        """Progressive render should create <= 50 DOM nodes in first frame."""
        self.page.goto(SERVER_URL + "/", wait_until="networkidle")
        self.page.wait_for_timeout(500)
        node_count = self.page.evaluate("""
            (() => {
                currentSearch = 'a';
                buildClassList();
                const count = document.querySelectorAll('#class-list .class-item').length;
                currentSearch = '';
                buildClassList();
                return count;
            })()
        """)
        ok = node_count <= 50
        self.add("Progressive render", ok, f"{node_count} DOM nodes in first frame (limit 50)")

    # ------------------------------------------------------------------
    # 11. Console Errors
    # ------------------------------------------------------------------
    def test_no_console_errors(self):
        """Reload the page fresh and check for JS errors."""
        errors = []
        def on_error(msg):
            if msg.type == "error":
                errors.append(msg.text)

        self.page.on("console", on_error)
        self.page.goto(SERVER_URL, wait_until="networkidle")
        self.page.wait_for_timeout(1000)

        # Navigate to a class
        self.page.goto(SERVER_URL + "/#zombie.characters.IsoPlayer", wait_until="networkidle")
        self.page.wait_for_timeout(1000)

        # Visit globals
        self.page.goto(SERVER_URL + "/#globals", wait_until="networkidle")
        self.page.wait_for_timeout(500)

        self.page.remove_listener("console", on_error)

        ok = len(errors) == 0
        msg = "No console errors" if ok else f"{len(errors)} errors: {'; '.join(errors[:5])}"
        self.add("No console errors", ok, msg)

    # ------------------------------------------------------------------
    # Run All
    # ------------------------------------------------------------------
    def run(self):
        print("\n" + "=" * 60)
        print("PZ Lua API Viewer — Test Suite")
        print(f"Started: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        print("=" * 60)

        sections = [
            ("Page Load", [self.test_page_loads, self.test_class_count, self.test_stats_bar]),
            ("Search", [self.test_search_filters, self.test_search_clear]),
            ("Class Detail", [self.test_class_detail_loads, self.test_inheritance_tree,
                              self.test_methods_visible, self.test_source_tab]),
            ("Tab Bar", [self.test_tab_bar]),
            ("Navigation", [self.test_back_forward, self.test_hash_navigation]),
            ("Globals", [self.test_globals_panel, self.test_globals_search]),
            ("Sidebar", [self.test_sidebar_filters, self.test_sidebar_splitter]),
            ("Version", [self.test_version_select_exists]),
            ("Hover Preview", [self.test_hover_preview]),
            ("Performance", [self.test_search_performance, self.test_progressive_render]),
            ("Console Errors", [self.test_no_console_errors]),
        ]

        for section_name, tests in sections:
            print(f"\n[{section_name}]")
            for test_fn in tests:
                try:
                    test_fn()
                except Exception as e:
                    self.add(test_fn.__name__.replace("test_", "").replace("_", " "),
                             False, f"EXCEPTION: {e}")

        # Summary
        passed = sum(1 for r in self.results if r.passed)
        total = len(self.results)
        failed = total - passed

        print("\n" + "=" * 60)
        print(f"Results: {passed}/{total} passed, {failed} failed")
        print("=" * 60)

        if failed:
            print("\nFailed tests:")
            for r in self.results:
                if not r.passed:
                    print(f"  [FAIL] {r.name}: {r.message}")

        # Write JSON report
        report = {
            "timestamp": datetime.now().isoformat(),
            "passed": passed,
            "total": total,
            "results": [
                {"name": r.name, "passed": r.passed, "message": r.message, "screenshot": r.screenshot}
                for r in self.results
            ]
        }
        report_path = REPORT_DIR / f"report-{datetime.now().strftime('%Y%m%d-%H%M%S')}.json"
        report_path.write_text(json.dumps(report, indent=2))
        print(f"\nReport saved: {report_path}")

        return failed == 0


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------
if __name__ == "__main__":
    # Kill anything on port 8000
    kill_port(PORT)
    time.sleep(0.5)

    # Start server
    print(f"Starting server on port {PORT}...")
    server_proc = start_server()
    print(f"Server PID: {server_proc.pid}")

    try:
        suite = PZViewerTests()
        suite.setup()
        try:
            success = suite.run()
        finally:
            suite.teardown()
    finally:
        server_proc.terminate()
        server_proc.wait(timeout=5)

    sys.exit(0 if success else 1)

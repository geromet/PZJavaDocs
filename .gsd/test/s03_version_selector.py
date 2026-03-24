#!/usr/bin/env python3
"""S03 version-selector tests: dropdown visibility, data-version-active attribute, ?v= param, 404 fallback."""

from __future__ import annotations

import json
import subprocess
import time
from pathlib import Path
from urllib.request import urlopen

import pytest
from playwright.sync_api import expect, sync_playwright

PROJECT_DIR = Path(__file__).resolve().parent.parent.parent
SERVER_SCRIPT = PROJECT_DIR / "server" / "serve.py"
SERVER_URL = "http://127.0.0.1:8000"

MULTI_VERSION_MANIFEST = json.dumps([
    {"id": "r964", "label": "Build r964", "file": "versions/lua_api_r964.json"},
    {"id": "r900", "label": "Build r900", "file": "versions/lua_api_r964.json"},
])


@pytest.fixture(scope="module")
def server_process():
    proc = subprocess.Popen(
        ["python", str(SERVER_SCRIPT)],
        cwd=PROJECT_DIR,
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL,
    )

    deadline = time.time() + 20
    last_error = None
    while time.time() < deadline:
        try:
            with urlopen(SERVER_URL, timeout=1) as response:
                if response.status == 200:
                    yield proc
                    proc.terminate()
                    try:
                        proc.wait(timeout=5)
                    except subprocess.TimeoutExpired:
                        proc.kill()
                    return
        except Exception as exc:
            last_error = exc
            time.sleep(0.25)

    proc.terminate()
    raise RuntimeError(f"Server did not become ready: {last_error}")


@pytest.fixture()
def page(server_process):
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        pg = browser.new_page(viewport={"width": 1440, "height": 1000})
        yield pg
        browser.close()


def _open(page, path: str = "/"):
    """Navigate and wait for loading to complete."""
    page.goto(f"{SERVER_URL}{path}")
    page.locator("#loading").wait_for(state="hidden", timeout=15000)


# ── Test 1: Single version → dropdown hidden, no data attribute ───────────

def test_single_version_dropdown_hidden(page):
    """With only 1 entry in versions.json, dropdown is hidden and has no data-version-active."""
    _open(page, "/")

    sel = page.locator("#version-select")
    expect(sel).to_be_hidden()

    has_attr = page.evaluate(
        "() => document.getElementById('version-select').hasAttribute('data-version-active')"
    )
    assert has_attr is False, "data-version-active should NOT be present with single version"


# ── Test 2: Multi-version → dropdown visible with data attribute ──────────

def test_multi_version_dropdown_visible(page):
    """With 2+ entries in versions.json, dropdown is visible with correct options and attribute."""
    page.route(
        "**/versions/versions.json",
        lambda route: route.fulfill(
            status=200,
            content_type="application/json",
            body=MULTI_VERSION_MANIFEST,
        ),
    )

    _open(page, "/")

    sel = page.locator("#version-select")
    expect(sel).to_be_visible(timeout=5000)

    option_count = sel.locator("option").count()
    assert option_count == 2, f"Expected 2 options, got {option_count}"

    active = page.evaluate(
        "() => document.getElementById('version-select').dataset.versionActive"
    )
    assert active == "r964", f"Expected data-version-active='r964', got '{active}'"


# ── Test 3: ?v= param selects the correct version ────────────────────────

def test_v_param_selects_version(page):
    """Navigating with ?v=r900 selects that version in the dropdown and sets the attribute."""
    page.route(
        "**/versions/versions.json",
        lambda route: route.fulfill(
            status=200,
            content_type="application/json",
            body=MULTI_VERSION_MANIFEST,
        ),
    )

    _open(page, "/?v=r900")

    sel = page.locator("#version-select")
    expect(sel).to_be_visible(timeout=5000)

    selected_value = sel.input_value()
    assert selected_value == "r900", f"Expected dropdown value 'r900', got '{selected_value}'"

    active = page.evaluate(
        "() => document.getElementById('version-select').dataset.versionActive"
    )
    assert active == "r900", f"Expected data-version-active='r900', got '{active}'"


# ── Test 4: versions.json 404 → graceful fallback ────────────────────────

def test_versions_json_404_graceful_fallback(page):
    """When versions.json returns 404, app still boots, dropdown is hidden, no attribute."""
    page.route(
        "**/versions/versions.json",
        lambda route: route.fulfill(status=404, body=""),
    )

    _open(page, "/")

    sel = page.locator("#version-select")
    expect(sel).to_be_hidden()

    has_attr = page.evaluate(
        "() => document.getElementById('version-select').hasAttribute('data-version-active')"
    )
    assert has_attr is False, "data-version-active should NOT be present on 404 fallback"

    # Verify the app actually loaded — class list should have children
    class_count = page.locator("#class-list .class-item").count()
    assert class_count > 0, f"Expected class list to have items after fallback, got {class_count}"

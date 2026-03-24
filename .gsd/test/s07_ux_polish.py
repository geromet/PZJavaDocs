#!/usr/bin/env python3
"""S07 UX polish tests: hover prefetch and inspectable loading state."""

from __future__ import annotations

import subprocess
import time
from pathlib import Path
from urllib.request import urlopen

import pytest
from playwright.sync_api import sync_playwright, expect

PROJECT_DIR = Path(__file__).resolve().parent.parent.parent
SERVER_SCRIPT = PROJECT_DIR / "server" / "serve.py"
SERVER_URL = "http://127.0.0.1:8000"


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
        except Exception as exc:  # pragma: no cover - startup polling only
            last_error = exc
            time.sleep(0.25)

    proc.terminate()
    raise RuntimeError(f"Server did not become ready: {last_error}")


@pytest.fixture()
def page(server_process):
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page(viewport={"width": 1440, "height": 1000})
        page.goto(f"{SERVER_URL}/#zombie.characters.IsoPlayer")
        page.locator("#loading").wait_for(state="hidden", timeout=10000)
        yield page
        browser.close()


def _pick_prefetch_link(page):
    page.locator('[data-ctab="source"]').click()
    expect(page.locator("#source-panel")).to_have_attribute("data-source-state", "ready", timeout=10000)

    handle = page.evaluate(
        """
        () => {
          document.querySelectorAll('[data-prefetch-test-target="1"]').forEach((el) => {
            el.removeAttribute('data-prefetch-test-target');
          });
          const links = Array.from(document.querySelectorAll('#source-panel a.src-class-ref[data-fqn]'));
          for (const link of links) {
            const rect = link.getBoundingClientRect();
            const visible = rect.width > 0 && rect.height > 0;
            const cls = API?.classes?.[link.dataset.fqn];
            if (visible && cls?.source_file) {
              link.setAttribute('data-prefetch-test-target', '1');
              return {
                fqn: link.dataset.fqn,
                sourceFile: cls.source_file,
              };
            }
          }
          return null;
        }
        """
    )
    assert handle, "Expected at least one visible source-panel class link with a source file"
    return handle


@pytest.mark.prefetch
def test_hover_prefetch_marks_pending_then_ready(page):
    target = _pick_prefetch_link(page)
    source_file = target["sourceFile"]

    def maybe_delay_route(route):
        if route.request.url.endswith(source_file):
            time.sleep(0.8)
        route.continue_()

    page.route("**/sources/**", maybe_delay_route)

    page.locator('[data-prefetch-test-target="1"]').hover()
    expect(page.locator("#hover-preview")).to_be_visible(timeout=2000)

    expect(page.locator("#hover-preview")).to_have_attribute("data-prefetch-state", "pending")
    expect(page.locator("#hover-preview")).to_have_attribute("data-prefetch-path", source_file)
    expect(page.locator("#hover-preview")).to_have_attribute("data-prefetch-state", "ready", timeout=3000)


@pytest.mark.loading_state
@pytest.mark.failure
@pytest.mark.diagnostics
def test_source_panel_exposes_error_state_and_message(page):
    missing_path = "missing/DefinitelyNotThere.java"

    page.evaluate("(path) => showSourceByPath(path)", missing_path)

    expect(page.locator("#source-panel")).to_have_attribute("data-source-state", "error", timeout=3000)
    expect(page.locator("#source-panel")).to_have_attribute("data-source-path", missing_path)
    expect(page.locator("#source-loading")).to_have_attribute("data-source-state", "error")
    expect(page.locator("#source-code")).to_contain_text("Source not available")
    expect(page.locator("#source-code")).to_contain_text(missing_path)

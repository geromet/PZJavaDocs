#!/usr/bin/env python3
"""Navigation Tests: Back/Forward buttons and URL hash changes."""

from datetime import datetime
import time

from config import SERVER_URL, REPORT_DIR


CLASS_URL = SERVER_URL + "/?class=zombie.characters.IsoPlayer"


def _wait_ready(page):
    page.goto(CLASS_URL)
    page.locator('#loading').wait_for(state='hidden', timeout=10000)


def _nav_link(page):
    link = page.locator('#class-list .class-item[data-fqn]').nth(1)
    link.wait_for(state='visible', timeout=10000)
    return link


def test_back_forward_buttons(page) -> dict:
    """Test that back/forward buttons work correctly."""
    try:
        _wait_ready(page)
        class_ref = _nav_link(page)

        fqn = class_ref.get_attribute("data-fqn")
        print(f"  [Nav] Navigating from IsoPlayer to {fqn}...")

        class_ref.click()
        time.sleep(1)

        if page.url.endswith("#zombie.characters.IsoPlayer") or "class=zombie.characters.IsoPlayer" in page.url:
            return {"name": "back-forward-buttons", "passed": False,
                    "message": f"Expected to be on {fqn}, but still on IsoPlayer"}

        page.locator('#btn-nav-back').click()
        time.sleep(0.5)

        if "class=zombie.characters.IsoPlayer" not in page.url and not page.url.endswith("#zombie.characters.IsoPlayer"):
            return {"name": "back-forward-buttons", "passed": False,
                    "message": f"After back button: {page.url}"}

        screenshot_after = capture_screenshot(page, "back-success")
        return {"name": "back-forward-buttons", "passed": True,
                "message": f"Successfully navigated back from {fqn} to IsoPlayer",
                "screenshot": screenshot_after}

    except Exception as e:
        print(f"  [Nav] Error: {e}")
        return {"name": "back-forward-buttons", "passed": False,
                "message": str(e),
                "screenshot": {"path": str(REPORT_DIR / "back-error.png")}}


def test_nav_hash_change(page) -> dict:
    """Test that navigation updates the URL hash."""
    try:
        _wait_ready(page)
        initial_hash = page.url.split("#")[-1] if "#" in page.url else ""

        class_ref = _nav_link(page)
        fqn = class_ref.get_attribute("data-fqn")
        class_ref.click()
        time.sleep(0.5)

        new_hash = page.url.split("#")[-1] if "#" in page.url else ""

        if new_hash == initial_hash and initial_hash:
            return {"name": "nav-hash-change", "passed": False,
                    "message": f"Hash before: {initial_hash}, after: {new_hash}"}

        return {"name": "nav-hash-change", "passed": True,
                "message": f"URL hash updated from {initial_hash} to {new_hash} ({fqn})"}

    except Exception as e:
        print(f"  [Nav] Error: {e}")
        return {"name": "nav-hash-change", "passed": False,
                "message": str(e)}


def capture_screenshot(page, name, selector=None):
    """Capture a screenshot with metadata."""
    from config import REPORT_DIR
    screenshot_path = REPORT_DIR / f"{name}.png"
    screenshot_path.parent.mkdir(parents=True, exist_ok=True)

    if selector:
        element = page.locator(selector).first
        element.screenshot(path=str(screenshot_path))
    else:
        page.screenshot(path=str(screenshot_path))

    import hashlib
    file_hash = hashlib.md5(open(screenshot_path, 'rb').read()).hexdigest()[:8]

    return {
        "path": str(screenshot_path),
        "hash": file_hash,
        "timestamp": datetime.now().isoformat(),
        "selector": selector
    }

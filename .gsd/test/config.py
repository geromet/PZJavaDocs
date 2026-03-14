#!/usr/bin/env python3
"""Test configuration and utilities."""

import os
from pathlib import Path
from playwright.sync_api import sync_playwright
from datetime import datetime
import tempfile
import shutil

# Configuration - use absolute paths based on where this file lives
BASE_DIR = Path(__file__).resolve().parent.parent.parent.parent  # .../projectzomboid/pz-lua-api-viewer
PROJECT_DIR = BASE_DIR / "pz-lua-api-viewer"
REPORT_DIR = PROJECT_DIR / ".gsd" / "test-reports"
SERVER_SCRIPT = PROJECT_DIR / "serve.py"
SERVER_PORT = 8000
SERVER_URL = f"http://localhost:{SERVER_PORT}"


def setup_server():
    """Start the local server."""
    if not SERVER_SCRIPT.exists():
        print(f"[FAIL] Server script not found: {SERVER_SCRIPT}")
        return False
    
    # Kill any existing server on this port
    import subprocess
    try:
        result = subprocess.run(
            ["netstat", "-ano"], capture_output=True, text=True
        )
        for line in result.stdout.splitlines():
            if ":8000" in line:
                pid = line.split()[-1]
                subprocess.run(["taskkill", "/F", "/PID", pid], check=True)
    except Exception:
        pass
    
    # Start server
    os.chdir(PROJECT_DIR)
    proc = subprocess.Popen(
        [os.sys.executable, str(SERVER_SCRIPT)],
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        cwd=str(PROJECT_DIR)
    )
    
    # Wait for server to be ready
    max_wait = 30
    start_time = datetime.now()
    while datetime.now() - start_time < max_wait:
        try:
            response = requests.get("http://127.0.0.1:8000/", timeout=1)
            if response.status_code == 200:
                print(f"[INFO] Server is ready at {SERVER_URL}")
                return True
        except Exception:
            pass
        import time
        time.sleep(0.5)
    
    print("[WARN] Server may not be ready yet, continuing anyway...")
    return False


import requests

# Re-import after setup_server runs
from playwright.sync_api import sync_playwright, expect, BrowserContext

# Initialize Playwright
browser = None
context = None


def init_browser():
    """Initialize the browser."""
    global browser, context
    browser = sync_playwright().start()
    viewport_size = {"width": 1920, "height": 1080}
    context = browser.chromium.launch(headless=True, viewport=viewport_size, user_agent="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
    page = context.new_page()
    page.goto(SERVER_URL + "/")
    return page


def capture_screenshot(page: BrowserContext, name: str, selector=None):
    """Capture a screenshot with metadata."""
    screenshot_path = REPORT_DIR / f"{name}.png"
    screenshot_path.parent.mkdir(parents=True, exist_ok=True)
    
    if selector:
        element = page.locator(selector).first
        element.screenshot(path=str(screenshot_path))
    else:
        page.screenshot(path=str(screenshot_path))
    
    # Generate hash for comparison
    file_hash = hashlib.md5(open(screenshot_path, 'rb').read()).hexdigest()[:8]
    
    return {
        "path": str(screenshot_path),
        "hash": file_hash,
        "timestamp": datetime.now().isoformat(),
        "selector": selector
    }


import hashlib

# Utility functions for tests
class TestResult:
    """Represents a test result."""
    def __init__(self, name: str, passed: bool, message: str = "", screenshot: dict = None):
        self.name = name
        self.passed = passed
        self.message = message
        self.screenshot = screenshot
    
    def to_dict(self):
        return {"name": self.name, "passed": self.passed, "message": self.message, "screenshot": self.screenshot}
    
    def __repr__(self):
        status = "PASS" if self.passed else "FAIL"
        return f"{status}: {self.name}"

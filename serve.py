"""Serve the PZ Lua API Viewer locally on http://localhost:8000"""
import http.server
import webbrowser
import os

PORT = 8000
os.chdir(os.path.dirname(os.path.abspath(__file__)))

print(f"Serving at http://localhost:{PORT}")
print("Press Ctrl+C to stop.\n")
webbrowser.open(f"http://localhost:{PORT}")

http.server.test(
    HandlerClass=http.server.SimpleHTTPRequestHandler,
    port=PORT,
    bind="127.0.0.1",
)

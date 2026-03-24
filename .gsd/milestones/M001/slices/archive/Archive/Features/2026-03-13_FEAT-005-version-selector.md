> **COMPLETED 2026-03-13** — Version dropdown, versioned JSON output, ?v= URL param. Shipped in bf40c8f.

# FEAT-005: Version selector and multi-version API storage

**Status:** Planned
**Priority:** Low–Medium
**Complexity:** High

## Description

Allow users to switch between multiple versions of the PZ API (e.g. B41, B42, future updates) without leaving the viewer. Each version has its own `lua_api.json`.

## Motivation

PZ updates frequently change the API. Modders targeting older multiplayer (B41) need to see the B41 API even after B42 ships.

## Proposed Design

### Extractor side
- `extract_lua_api.py` writes `lua_api_<build>.json` where `<build>` is read from a version file or detected from PZ's `ProjectZomboid.exe` metadata.
- `pz-lua-api-viewer/versions/` directory holds multiple JSON files.
- A `versions.json` manifest lists available versions with labels and file names.

### Viewer side
- A version dropdown in the toolbar (top bar, near the search field).
- On change, re-runs `init()` with the newly loaded `lua_api.json`.
- `location.hash` is updated with `?v=B42#ClassName` so links are version-aware.
- Diff mode (stretch): highlight methods/fields that changed between versions.

## Notes

- Large JSON files (~5 MB each) may be slow to load on GitHub Pages without compression. Consider gzip serving via `_headers` file if using Netlify/Cloudflare Pages.
- This feature is low priority until PZ reaches a stable 1.0 with frequent API-breaking updates.

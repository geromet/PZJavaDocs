---
id: S03
parent: M001
milestone: M001
provides:
  - detect_pz_version() extractor helper — reads SVNRevision.txt to tag API output by build revision
  - Versioned JSON output — versions/lua_api_r964.json + versions/versions.json manifest
  - "#version-select" toolbar dropdown — visible when manifest has ≥2 versions, hidden otherwise
  - "?v=<id>" URL query param — selects version on load, preserved across navigation
  - Full-page-reload version switching — avoids complex in-memory state reset
  - data-version-active diagnostic attribute on #version-select
  - Playwright test suite (.gsd/test/s03_version_selector.py) — 4 tests covering single/multi/param/404 scenarios
requires: []
affects: []
key_files:
  - extract_lua_api.py
  - js/app.js
  - index.html
  - app.css
  - versions/versions.json
  - versions/lua_api_r964.json
  - .gsd/test/s03_version_selector.py
key_decisions:
  - Full page reload for version switch — avoids complex global state reset across all modules
  - Dropdown hidden when manifest absent or ≤1 entry — no visible change for single-version deploys
  - Unknown ?v= param falls back to first manifest entry (graceful degradation)
  - SVNRevision.txt as primary version source (single integer, no parsing); yields "r964"
  - versions.json is a list of {id, label, file} — future entries appended without restructuring
  - Route-intercept pattern for multi-version Playwright tests (inject fake 2-entry manifest pointing at real r964 JSON)
patterns_established:
  - detect_pz_version() in extractor: SVNRevision.txt → bat file → "unknown" fallback
  - Async IIFE at top of app.js for version-aware API loading
  - setupVersionDropdown(manifest, currentId) called from loader after API ready
  - data-version-active attribute as the observable contract for which API version is loaded
observability_surfaces:
  - "#version-select[data-version-active]" — reflects active version ID; absent when no manifest or single entry
  - "document.querySelector('#version-select').dataset.versionActive" in browser console
  - Playwright test suite validates all four scenarios automatically
drill_down_paths:
  - .gsd/milestones/M001/slices/S03/tasks/T01-SUMMARY.md
  - .gsd/milestones/M001/slices/S03/tasks/T02-SUMMARY.md
  - .gsd/milestones/M001/slices/S03/tasks/T03-SUMMARY.md
  - .gsd/milestones/M001/slices/S03/tasks/T04-SUMMARY.md
duration: ~3h
verification_result: passed
completed_at: 2026-03-24
---

# S03: Version Selector

**Extractor detects PZ build revision and writes versioned API JSON; frontend loads version manifest, shows a toolbar dropdown for ≥2 versions, and switches via full page reload with `?v=<id>` URL param; `data-version-active` diagnostic attribute and 4 Playwright tests verify the full flow.**

## What Happened

**T01 — Extractor versioned output.** Added `detect_pz_version()` to `extract_lua_api.py`, which reads `SVNRevision.txt` (current build: r964) with bat-file and "unknown" fallbacks. After extraction, the script writes `versions/lua_api_r964.json` (versioned copy) and creates/updates `versions/versions.json` (manifest of `{id, label, file}` entries, newest first). Root `lua_api.json` is kept unchanged for single-file backward compatibility.

**T02 — Frontend dropdown and switching.** Replaced the top-level `fetch('./lua_api.json').then(...)` chain in `app.js` with an async IIFE that fetches `versions/versions.json`, selects the right versioned file (honoring `?v=` query param), and falls back to `lua_api.json` on 404. Added `<select id="version-select">` to `index.html` with CSS styling. `setupVersionDropdown()` hides the select when manifest is absent or has ≤1 entry; otherwise populates options and shows it. Switching triggers a full page reload with `?v=<newId>` plus existing hash (so the currently open class reopens in the new version).

**T03 — Diagnostic attribute and Playwright tests.** Wired `sel.dataset.versionActive = currentId` in `setupVersionDropdown()` (deleted when hidden). Created `.gsd/test/s03_version_selector.py` with 4 tests: single-version hidden, multi-version visible (route-intercepted fake manifest), `?v=` param selection, and 404 graceful fallback. All pass.

**T04 — Summary and UAT.** Replaced auto-generated stub summary and UAT with real compressed artifacts.

## Verification

- `pytest .gsd/test/s03_version_selector.py -v` — 4/4 passed
- `grep -q 'versionActive' js/app.js` — confirmed attribute wired
- Summary contains no stub markers (verified by grep check)

## Deviations

None.

## Known Limitations

- Only one real version (r964) exists in the manifest currently — multi-version behavior is tested via route intercepts but not exercised with production data until a second PZ build is extracted.
- Version switching uses full page reload, which is intentional but means brief flash of unloaded state.

## Follow-ups

- When a second PZ build becomes available, run the extractor again to populate a real multi-version manifest.

## Files Created/Modified

- `extract_lua_api.py` — added `detect_pz_version()` + versioned output block
- `js/app.js` — async IIFE loader, `setupVersionDropdown()`, `data-version-active` attribute
- `index.html` — added `<select id="version-select">`
- `app.css` — `#version-select` styles
- `versions/versions.json` — version manifest (new)
- `versions/lua_api_r964.json` — versioned API data (new, ~5.9MB)
- `.gsd/test/s03_version_selector.py` — 4 Playwright tests (new)
- `.gsd/milestones/M001/slices/S03/S03-SUMMARY.md` — real summary replacing stub
- `.gsd/milestones/M001/slices/S03/S03-UAT.md` — real UAT replacing stub

## Forward Intelligence

### What the next slice should know
- `versions/versions.json` is the manifest contract: a JSON list of `{id, label, file}` objects, newest first. The frontend fetches it on every page load and silently falls back to `lua_api.json` on failure.
- `data-version-active` on `#version-select` is the runtime inspection surface for which API version is loaded. Absent means single-file mode or fetch failure.
- Version switching uses full page reload (`location.href` assignment), so there is no in-memory state to clean up — all modules reinitialize from scratch.

### What's fragile
- The route-intercept pattern for multi-version tests depends on the exact URL path `versions/versions.json` — if the manifest location changes, tests break silently (they'd get real responses instead of intercepted ones).
- `setupVersionDropdown()` must be called after the DOM is ready and after the manifest is fetched — reordering the async IIFE flow could break dropdown visibility.

### Authoritative diagnostics
- `pytest .gsd/test/s03_version_selector.py -v` — exercises all four version selector scenarios against a live local server.
- `document.querySelector('#version-select').dataset.versionActive` in browser console — shows active version ID or undefined.

### What assumptions changed
- No assumptions changed — the feature was planned before S03 and implementation matched the design.

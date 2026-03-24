---
id: T03
parent: S03
milestone: M001
provides:
  - data-version-active diagnostic attribute on #version-select
  - Playwright test suite for version selector (4 tests)
key_files:
  - js/app.js
  - .gsd/test/s03_version_selector.py
key_decisions:
  - Route-intercept pattern for multi-version testing (inject fake 2-entry manifest pointing both entries at the real r964 JSON to avoid needing a second data file)
patterns_established:
  - data-version-active attribute as the observable contract for which API version is loaded
observability_surfaces:
  - "#version-select[data-version-active]" — reflects active version ID; absent when no manifest or single entry
duration: 15m
verification_result: passed
completed_at: 2026-03-24
blocker_discovered: false
---

# T03: Add diagnostic data attribute and Playwright tests for version selector

**Added `data-version-active` attribute to `#version-select` and wrote 4 Playwright tests covering single-version hidden, multi-version visible, `?v=` param selection, and 404 graceful fallback.**

## What Happened

Modified `setupVersionDropdown()` in `js/app.js` to set `sel.dataset.versionActive = currentId` when the manifest has ≥2 entries, and `delete sel.dataset.versionActive` in the early-return branch (no manifest or ≤1 entry). Created `.gsd/test/s03_version_selector.py` with 4 tests following the s09 server fixture pattern — module-scoped server process, per-test Playwright page, route intercepts for multi-version and 404 scenarios.

## Verification

- `pytest .gsd/test/s03_version_selector.py -v` — 4/4 passed
- `grep -q 'versionActive' js/app.js` — confirmed attribute wired in source

## Verification Evidence

| # | Command | Exit Code | Verdict | Duration |
|---|---------|-----------|---------|----------|
| 1 | `pytest .gsd/test/s03_version_selector.py -v` | 0 | ✅ pass | 4.36s |
| 2 | `grep -q 'versionActive' js/app.js` | 0 | ✅ pass | <0.1s |

## Diagnostics

- Runtime: `document.querySelector('#version-select').dataset.versionActive` in browser console returns active version ID or `undefined`
- Test: `pytest .gsd/test/s03_version_selector.py -v` exercises all four scenarios
- Absence of `data-version-active` means single-file mode or versions.json fetch failed

## Deviations

None — task plan matched local reality exactly.

## Known Issues

None.

## Files Created/Modified

- `js/app.js` — added `sel.dataset.versionActive` set/delete in `setupVersionDropdown()`
- `.gsd/test/s03_version_selector.py` — new: 4 Playwright tests for version selector

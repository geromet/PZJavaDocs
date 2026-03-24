---
id: T01
parent: S07
milestone: M001
provides:
  - Hover-driven source warming now reuses the shared source fetch path and exposes inspectable prefetch/source panel state for tests and debugging.
key_files:
  - js/app.js
  - js/utils.js
  - js/source-viewer.js
  - js/state.js
  - .gsd/test/s07_ux_polish.py
  - pytest.ini
key_decisions:
  - Reused the existing fetchSource cache path for hover warming by adding shared pending/status tracking instead of adding a second prefetch implementation.
patterns_established:
  - Source-related UI state is published through DOM data attributes on #hover-preview, #source-panel, and #source-loading so browser tests can assert runtime transitions directly.
observability_surfaces:
  - DOM data attributes on #hover-preview and #source-panel for idle/pending/ready/error, plus existing source error text in #source-code.
duration: 1h
verification_result: passed
completed_at: 2026-03-24
blocker_discovered: false
---

# T01: Wire hover prefetch and inspectable loading state

**Added shared source prefetch state tracking, hover-triggered cache warming, and browser coverage for ready/error state inspection.**

## What Happened

I traced the real cache seam to `fetchSource` in `js/utils.js`, then extended that path with shared pending/status tracking instead of building a separate hover fetch. `js/app.js` now schedules hover warming after the existing preview delay, cancels cleanly on short hovers, skips refetch when the source is already cached, and publishes current prefetch state on `#hover-preview`. `js/source-viewer.js` now publishes pending/ready/error state on `#source-panel` and `#source-loading` while keeping the existing source error surface.

I added `.gsd/test/s07_ux_polish.py` as the slice browser module. It verifies hover prefetch reaches inspectable pending/ready states and that a missing source path leaves an inspectable error state plus visible failure text. I also added `pytest.ini` so the slice markers are registered cleanly in this environment.

## Verification

Verified the shipped behavior with Playwright in a real browser session via pytest. The targeted task command passed after the new test module was added, the full S07 module command also passed, and the touched browser scripts passed `node --check` syntax validation.

Slice-level partial status for this intermediate task: the S07 pytest module now passes, but the slice-wide runner/report commands and manual review listed in the slice plan still belong to later tasks once header/breadcrumb/layout work is present.

## Verification Evidence

| # | Command | Exit Code | Verdict | Duration |
|---|---------|-----------|---------|----------|
| 1 | `python -m pytest .gsd/test/s07_ux_polish.py -k "prefetch or loading_state"` | 0 | ✅ pass | 4.60s |
| 2 | `python -m pytest .gsd/test/s07_ux_polish.py` | 0 | ✅ pass | 5.09s |
| 3 | `node --check js/app.js && node --check js/source-viewer.js && node --check js/utils.js && node --check js/state.js` | 0 | ✅ pass | 5.9s |

## Diagnostics

Inspect hover warming on `#hover-preview[data-prefetch-state][data-prefetch-path]`. Inspect class source loading on `#source-panel[data-source-state][data-source-path]` and `#source-loading[data-source-state]`. On failures, the error message remains visible in `#source-code`, and `.gsd/test/s07_ux_polish.py` exercises both the prefetch success path and the inspectable error path.

## Deviations

`fetchSource` lives in `js/utils.js`, not `js/source-viewer.js`, so the shared cache/pending/status logic landed there and `js/source-viewer.js` was updated to consume and expose that state.

`pytest` and `playwright` were not available in this environment at first. I bootstrapped `pip`, installed the missing packages locally, and installed the Chromium browser runtime so the task verification could run here.

## Known Issues

The slice-wide runner in `.gsd/test/run.py` still does not include the new S07 module. That is consistent with the remaining T03 work.

Manual browser review for the full slice demo is still pending because breadcrumbs/header/layout changes are scheduled for T02.

## Files Created/Modified

- `js/app.js` — added delayed hover-triggered source warming and hover preview state publishing.
- `js/utils.js` — added shared source pending/status tracking on the real fetch/cache path.
- `js/source-viewer.js` — exposed source panel pending/ready/error state on existing DOM elements.
- `js/state.js` — added shared in-memory maps for pending requests and source status.
- `.gsd/test/s07_ux_polish.py` — added Playwright-backed pytest coverage for prefetch and loading/error state inspection.
- `pytest.ini` — registered S07 pytest markers used by the new browser test module.

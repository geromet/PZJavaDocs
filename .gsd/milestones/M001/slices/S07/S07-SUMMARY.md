---
id: S07
parent: M001
milestone: M001
provides:
  - Hover-driven source warming, a compact filter/header shell, clickable package breadcrumbs, and stable detail/source loading surfaces with inspectable DOM state.
requires:
  - slice: S06
    provides: Instant search and progressive rendering that this slice reused for breadcrumb-driven filtering and stable first-frame detail rendering.
affects:
  - S08
  - S09
key_files:
  - index.html
  - app.css
  - js/app.js
  - js/class-detail.js
  - js/source-viewer.js
  - js/utils.js
  - js/state.js
  - .gsd/test/s07_ux_polish.py
  - .gsd/test/run.py
  - pytest.ini
key_decisions:
  - Hover prefetch reuses the same shared source fetch/cache path as direct source opens, with shared pending/status tracking instead of a parallel prefetch implementation.
  - Package breadcrumb clicks reuse the existing global search flow by writing the package prefix into #global-search instead of introducing a second navigation model.
patterns_established:
  - Runtime UI state is published through DOM data attributes so browser tests can assert pending/ready/error transitions directly on hover, detail, filter, and source surfaces.
  - Consolidated browser verification now runs legacy checks on a fresh page per test and records the standalone S07 pytest module in the aggregate JSON report.
observability_surfaces:
  - #hover-preview[data-prefetch-state][data-prefetch-path], #detail-panel[data-detail-state][data-detail-fqn], #source-panel[data-source-state][data-source-path], #source-loading[data-source-state], #filter-select[data-active-filter], #active-filter-chip[data-filter], .gsd/test/s07_ux_polish.py, and .gsd/test-reports/report.json
drill_down_paths:
  - .gsd/milestones/M001/slices/S07/tasks/T01-SUMMARY.md
  - .gsd/milestones/M001/slices/S07/tasks/T02-SUMMARY.md
  - .gsd/milestones/M001/slices/S07/tasks/T03-SUMMARY.md
duration: 3h 15m
verification_result: passed
completed_at: 2026-03-24
---

# S07: UX Polish

**Shipped hover source warming, a quieter shell, breadcrumb-driven package navigation, and stable inspectable loading state without changing the viewer’s core feature model.**

## What Happened

This slice tightened the viewer at the seams users feel most: waiting, orientation, and chrome density.

On the runtime side, hover preview now warms source files before click when a class has a source path. That work did not add a second fetch path. Instead, the existing shared source cache/fetch seam was extended with pending and status tracking so hover warming, direct source opens, and failure handling all report the same state. The hover card and source panel now publish inspectable pending/ready/error state into the DOM, which made the feature testable and keeps failures diagnosable instead of silent.

On the shell side, the old sidebar filter button wall was replaced with a compact select control plus an active-filter chip, while preserving the existing filter values and class-list behavior. The header was recomposed into a slimmer summary row, palette and spacing were tightened, and the detail area was rebuilt around a stable shell so opening a class or source does not visibly jump while content arrives.

The detail panel now renders breadcrumb segments from the class FQN. Package breadcrumb clicks flow back into the existing search/list behavior by writing the package prefix into the global search input, so breadcrumb navigation stays aligned with the current filtering model instead of inventing parallel state.

Closing work on T03 repaired the main browser runner itself. The consolidated `.gsd/test/run.py` path now manages its own server/browser lifecycle, isolates legacy checks on fresh pages, includes the S07 pytest module in the aggregate report, and writes a trustworthy zero-failure `.gsd/test-reports/report.json`.

## Verification

Verified the slice against every plan-level automated command:

- `python .gsd/test/run.py` → passed, 14/14 checks green, report regenerated
- `python -m pytest .gsd/test/s07_ux_polish.py` → passed, 5/5 tests green
- `python -m pytest .gsd/test/s07_ux_polish.py -k "loading_state or failure or diagnostics"` → passed
- report assertion against `.gsd/test-reports/report.json` (`failed == 0`) → passed

Observability surfaces were also rechecked as part of the S07 module: hover prefetch exposes pending/ready state and source path, detail/source shells expose stable loading/error state, breadcrumb/filter state remains inspectable in the DOM, and the aggregate browser report records the slice result and screenshots.

The slice plan called for manual browser review. In this unit, the same live browser flows were re-exercised through Playwright-backed checks against the real local server: hover warm-before-click, breadcrumb rendering/click behavior, compact filter interaction, and stable source/detail loading shells.

## Deviations

The slice close-out required more than just registering a new test file. `.gsd/test/run.py` and several older browser checks were stale against the current routes, selectors, and Playwright lifecycle, so they were repaired as part of making the documented verification commands truthful again.

## Known Limitations

Regression coverage outside `.gsd/test/s07_ux_polish.py` is still shallower than the new S07-specific assertions. The legacy runner now works, but later slices should treat the newer DOM-state checks as the stronger source of truth.

Breadcrumb navigation currently works through the search box rather than its own dedicated package-state model. That is the right trade for this slice, but S08 should remember that package navigation and full URL state are not yet unified.

## Follow-ups

- S08 should decide how breadcrumb-driven package filtering participates in URL state persistence so breadcrumb clicks, filter select state, and tab/detail context round-trip together.
- S09 should preserve the new loading-state attributes and stable panel shells when source delivery changes from monolithic data to split payloads and cache layers.

## Files Created/Modified

- `index.html` — compacted the shell structure for the quieter header and dropdown-backed filter control.
- `app.css` — tightened spacing/palette and added stable detail/source shell styling.
- `js/app.js` — wired hover warming, filter-control syncing, and breadcrumb package navigation into the existing interaction flow.
- `js/class-detail.js` — rendered FQN breadcrumbs and exposed detail-panel state attributes.
- `js/source-viewer.js` — exposed source-panel loading/error/ready state through stable DOM seams.
- `js/utils.js` — extended the shared source fetch/cache path with pending/status tracking.
- `js/state.js` — added shared in-memory maps for source pending/status state.
- `.gsd/test/s07_ux_polish.py` — added focused browser checks for prefetch, breadcrumbs, compact controls, and loading diagnostics.
- `.gsd/test/run.py` — repaired the consolidated browser runner and folded S07 into the aggregate report.
- `pytest.ini` — registered the S07 pytest markers used by the focused browser module.

## Forward Intelligence

### What the next slice should know
- Breadcrumb clicks currently drive package navigation by filling `#global-search` with a package prefix. If S08 adds URL-backed state, that search write is the seam to encode rather than replacing breadcrumb behavior outright.
- The trustworthy diagnostics for source loading now live in DOM attributes, not just visible text. Future runtime tests should assert those attributes first.

### What's fragile
- `.gsd/test/run.py` still carries legacy coverage alongside the newer pytest module — it is trustworthy again, but the old checks are broader and less strict than the S07 DOM-state assertions.
- Source-loading behavior now depends on one shared fetch/cache path. That is good for consistency, but any later change to caching or payload splitting can break hover warming and source opens at once.

### Authoritative diagnostics
- `.gsd/test/s07_ux_polish.py` — strongest proof for hover prefetch, breadcrumb behavior, and loading diagnostics because it asserts the DOM state directly.
- `.gsd/test-reports/report.json` — authoritative aggregate slice result because it records the consolidated run, screenshots, and the standalone S07 module status in one place.

### What assumptions changed
- The assumption that slice close-out only needed a new test registration changed once the consolidated runner was exercised. The real issue was that the runner and several older checks had drifted from current routes and selectors, so the verification harness itself had to be repaired before S07 could honestly be called done.

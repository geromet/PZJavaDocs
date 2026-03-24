# S08: Navigation State

**Goal:** Make the viewer’s navigation state shareable and resumable by encoding the active browsing context in the URL and by exposing a recently viewed classes control backed by durable local state.
**Demo:** Open a class, change the filter and search, switch to the source sub-tab, copy the URL, and load it in a fresh page — the viewer restores the same search/filter/class/tab state. Use the recent classes control to jump back to a previously viewed class without re-searching.

## Must-Haves

- URL state covers the active top-level tab, selected class or globals view, class search text, active class filter, and the active content sub-tab when a class is open.
- Loading a URL with encoded state restores that state on first paint without breaking the existing version selector (`?v=`) or tab/history behavior.
- The viewer publishes inspectable navigation-state diagnostics through DOM data attributes so tests and future agents can tell what state was parsed, applied, or rejected.
- Recently viewed classes are stored in localStorage with deduplication and bounded length, rendered through a real UI control, and selecting a recent class reuses existing navigation/tab wiring.
- Existing automated coverage still passes, and slice-specific browser tests cover both happy-path restoration and at least one inspectable diagnostic/failure-path case.

## Proof Level

- This slice proves: integration
- Real runtime required: yes
- Human/UAT required: yes

## Verification

- `python .gsd/test/run.py`
- `python -m pytest .gsd/test/s08_navigation_state.py`
- `python -m pytest .gsd/test/s08_navigation_state.py -k "restore or recent or diagnostics or failure"`
- `python - <<'PY'
import json
from pathlib import Path
report = json.loads(Path('.gsd/test-reports/report.json').read_text())
assert report['failed'] == 0, report
print('report-ok')
PY`
- Manual browser review on the local server confirms: copying the current URL into a fresh tab restores search/filter/class/sub-tab state, and the recent classes control shows the newest selections first and navigates correctly.

## Observability / Diagnostics

- Runtime signals: DOM data attributes describing the last serialized/applied navigation state, recent-classes count/source, and whether URL restoration succeeded, fell back, or rejected invalid input.
- Inspection surfaces: browser DOM inspection on the main shell/control elements, Playwright assertions in `.gsd/test/s08_navigation_state.py`, and `.gsd/test-reports/report.json` from the consolidated runner.
- Failure visibility: invalid or unresolvable URL state leaves an inspectable status/error marker instead of silently failing; recent-list state remains inspectable even when the requested class is missing.
- Redaction constraints: diagnostics may expose filter/search/class identifiers already visible in the UI/URL, but must not surface local filesystem handles or any clipboard/local-source secrets.

## Integration Closure

- Upstream surfaces consumed: `index.html` viewer shell controls, `js/app.js` state/history/search/tab orchestration, existing DOM state markers from `js/class-detail.js` and `js/source-viewer.js`, `.gsd/test/run.py` browser runner, and the local server harness under `.gsd/test/`.
- New wiring introduced in this slice: `js/app.js` serializes/restores shareable URL state, recent-view persistence hooks into class selection/tab activation, and the new recent control is composed into the existing header/sidebar shell.
- What remains before the milestone is truly usable end-to-end: S09 still needs payload and repeat-visit performance work; nothing else should block everyday navigation/shareability once this slice lands.

## Tasks

- [x] **T01: Serialize and restore shareable URL navigation state** `est:2h`
  - Why: This is the core contract of S08. The URL needs to become the authoritative shareable representation of the current browsing state without fighting the existing hash/tab/history logic.
  - Files: `js/app.js`, `.gsd/test/s08_navigation_state.py`, `.gsd/test/run.py`
  - Do: Refactor the existing hash-only navigation flow in `js/app.js` into explicit state serialize/parse/apply helpers that preserve `?v=` while encoding filter/search/top-level tab/content tab/class selection in the URL; update URL state whenever those controls change without full reload; restore state on initial load in an order that respects API load, existing tabs, globals mode, and source/detail activation; and expose DOM-visible nav-state diagnostics so invalid or partial URL input is inspectable in tests.
  - Verify: `python -m pytest .gsd/test/s08_navigation_state.py -k "restore or diagnostics or failure"`
  - Done when: Opening a crafted URL restores the expected state in a fresh page, invalid state produces an inspectable fallback/error marker instead of a silent mismatch, and the new browser checks run from the named S08 test module.
- [x] **T02: Add recently viewed classes UI and persistence** `est:1.5h`
  - Why: Recent classes are the second user-facing outcome of the slice, and they should reuse the restored navigation model rather than introducing a parallel path.
  - Files: `index.html`, `app.css`, `js/app.js`, `.gsd/test/s08_navigation_state.py`
  - Do: Add a compact recent-classes control near the existing search/navigation chrome, back it with a localStorage list that is deduplicated and capped, update it from real class-selection events only, reuse existing `selectClass`/tab activation behavior when a recent item is chosen, and expose inspectable DOM state for item count and empty/error/fallback cases so browser checks can assert the control without scraping visual styling.
  - Verify: `python -m pytest .gsd/test/s08_navigation_state.py -k "recent or diagnostics"`
  - Done when: Selecting classes builds a bounded most-recent-first list, reselecting a class moves it to the top instead of duplicating it, the control is operable from the rendered UI, and recent-class navigation restores the expected class/source/detail view.
- [x] **T03: Close slice verification and runner integration for navigation state** `est:45m`
  - Why: S08 touches runtime navigation and local persistence. The slice should finish with one trustworthy verification entrypoint and aligned planning docs, not ad hoc test commands.
  - Files: `.gsd/test/run.py`, `.gsd/test/s08_navigation_state.py`, `.gsd/milestones/M001/slices/S08/S08-PLAN.md`
  - Do: Register the S08 browser module in the consolidated runner, run the full suite plus the standalone S08 module, fix any runner/report drift uncovered by the new stateful tests, and keep the slice plan’s verification commands and touched file paths aligned with reality after implementation.
  - Verify: `python .gsd/test/run.py && python -m pytest .gsd/test/s08_navigation_state.py`
  - Done when: The consolidated suite includes S08, the standalone S08 module passes, and this plan still names real files and executable commands.

## Files Likely Touched

- `index.html`
- `app.css`
- `js/app.js`
- `.gsd/test/s08_navigation_state.py`
- `.gsd/test/run.py`

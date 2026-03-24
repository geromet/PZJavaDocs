# S03: Version Selector

**Goal:** Close S03 by adding automated verification, observability, and a real slice summary for the already-implemented version selector feature (TASK-018 / FEAT-005). T01 (extractor versioned output) and T02 (frontend dropdown) are complete — this plan addresses the remaining gaps.
**Demo:** `pytest .gsd/test/s03_version_selector.py -v` passes all tests; `#version-select` exposes `data-version-active` for runtime inspection; doctor-created placeholder summary replaced with real compressed summary.

## Must-Haves

- `data-version-active` attribute on `#version-select` reflecting the currently loaded version (or absent when single-file mode)
- Playwright test covering: dropdown hidden with single version, dropdown visible with ≥2 manifest entries, `?v=<id>` param selects correct version, graceful fallback when versions.json absent
- At least one failure-path test: versions.json 404 → app still boots, dropdown hidden
- Doctor-created placeholder S03-SUMMARY.md replaced with real summary

## Proof Level

- This slice proves: integration — version switch fetches new data and renders correctly in the live browser
- Real runtime required: yes (Playwright against local server)
- Human/UAT required: no (automated tests sufficient)

## Verification

- `pytest .gsd/test/s03_version_selector.py -v` — all tests pass
- Failure-path test: versions.json 404 route intercept → app loads, dropdown hidden, no JS errors
- `grep -q 'versionActive' js/app.js` — diagnostic attribute wired

## Observability / Diagnostics

- Runtime signals: `#version-select[data-version-active]` reflects the active version ID (or absent when no manifest)
- Inspection surfaces: `document.querySelector('#version-select').dataset.versionActive` in browser console; Playwright test assertions
- Failure visibility: When versions.json fetch fails, dropdown remains hidden (`display:none`), app falls through to monolithic or split-index load — visible in network tab and absence of `data-version-active`
- Redaction constraints: none

## Integration Closure

- Upstream surfaces consumed: `versions/versions.json` manifest, `js/app.js` version-aware loader (already wired by prior T01/T02)
- New wiring introduced in this slice: `data-version-active` attribute on `#version-select`, Playwright test file
- What remains before the milestone is truly usable end-to-end: S05, S06 and any remaining unblocked slices

## Tasks

- [x] **T01: Extractor versioned output** `est:1h`
  - Why: Implements the server-side half of FEAT-005 / TASK-018
  - Files: `extract_lua_api.py`
  - Do: Already complete — see T01-SUMMARY.md
  - Verify: `test -f versions/versions.json && test -f versions/lua_api_r964.json`
  - Done when: Both files exist (verified)

- [x] **T02: Frontend version dropdown and switching** `est:2h`
  - Why: Implements the UI half of FEAT-005 / TASK-018
  - Files: `js/app.js`, `index.html`, `app.css`
  - Do: Already complete — see T02-SUMMARY.md
  - Verify: `grep -q 'setupVersionDropdown' js/app.js`
  - Done when: Version-aware loader and dropdown exist in app.js (verified)

- [x] **T03: Add diagnostic data attribute and Playwright tests for version selector** `est:45m`
  - Why: S03 implementation is done but has no automated tests and no diagnostic surface for future agents. Pre-flight requires observability and failure-path verification.
  - Files: `js/app.js`, `.gsd/test/s03_version_selector.py`
  - Do: (1) In `setupVersionDropdown()`, set `sel.dataset.versionActive = currentId` when manifest has entries, remove attribute when hidden. (2) Write `.gsd/test/s03_version_selector.py` with pytest + Playwright: test dropdown hidden with single version, dropdown visible when manifest has ≥2 entries (use route intercept to inject fake manifest), `?v=` param selects correct version, versions.json 404 → graceful fallback. Follow patterns from `.gsd/test/s09_load_perf.py`.
  - Verify: `pytest .gsd/test/s03_version_selector.py -v`
  - Done when: All 4+ tests pass; `grep -q 'versionActive' js/app.js` succeeds

- [x] **T04: Write real S03 summary and commit** `est:15m`
  - Why: Doctor-created placeholder summary needs replacement; slice work needs to be committed and pushed.
  - Files: `.gsd/milestones/M001/slices/S03/S03-SUMMARY.md`, `.gsd/milestones/M001/slices/S03/S03-UAT.md`
  - Do: (1) Replace S03-SUMMARY.md with real compressed summary covering all T01–T03 work. (2) Replace S03-UAT.md with real smoke-test checklist. (3) Commit and push to liability-machine.
  - Verify: `! grep -q 'placeholder' .gsd/milestones/M001/slices/S03/S03-SUMMARY.md`
  - Done when: Summary is real (not placeholder); changes pushed to liability-machine

## Files Likely Touched

- `js/app.js`
- `.gsd/test/s03_version_selector.py`
- `.gsd/milestones/M001/slices/S03/S03-SUMMARY.md`
- `.gsd/milestones/M001/slices/S03/S03-UAT.md`

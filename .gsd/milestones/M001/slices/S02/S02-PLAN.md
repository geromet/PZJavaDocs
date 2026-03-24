# S02: Tab Enhancements

**Goal:** Ship FEAT-007 (middle-click to open in new tab) and FEAT-014 (hover preview card). Both depend on the tab system (TASK-012, already shipped in S00/foundation).
**Demo:** Middle-clicking a class link in the sidebar/source/inheritance opens a new tab. Hovering a class link for 400ms shows a preview card with class name, method count, and inheritance line.

## Must-Haves

- Middle-click (button 1) or Ctrl+click on any class link opens that class in a new tab without navigating the current tab (FEAT-007)
- Hover preview card appears after 400ms on class reference links; disappears on mouseout or click (FEAT-014)
- Preview card shows: simple name, FQN, method count, first 3 methods alphabetically
- Preview card stays within viewport (no overflow off right/bottom edge)
- Existing left-click navigation unaffected

## Proof Level

- This slice proves: operational
- Real runtime required: yes
- Human/UAT required: no

## Verification

- `python .gsd/test/s02_features.py` — middle-click new tab and hover preview card tests pass
- `grep -q "openNewTab" js/state.js` — middle-click utility present
- `grep -q "hover-preview" js/app.js` — hover preview logic present
- `grep -q "hover-preview" index.html` — preview container div present

## Observability / Diagnostics

- Runtime signals: `#hover-preview` div gains `data-status="visible"` / hidden state via CSS display toggle; tab count observable via `tabs` array in `state.js`
- Inspection surfaces: Browser devtools Elements panel for `#hover-preview` position/content; Console for JS errors on middle-click or hover
- Failure visibility: Console errors if `openNewTab()` receives an unknown FQN; hover card not appearing after 400ms indicates broken event delegation
- Redaction constraints: none

## Integration Closure

- Upstream surfaces consumed: `js/state.js` (tab system, `openNewTab`), `js/class-list.js` (sidebar click handlers), `js/app.js` (delegated event handlers, hover logic)
- New wiring introduced in this slice: `openNewTab()` in state.js called from multiple click handlers; IIFE hover preview logic in `setupEvents()`; `#hover-preview` container in `index.html`
- What remains before the milestone is truly usable end-to-end: S03 (version selector), S05 (resizable sidebar)

## Tasks

- [x] **T01: Middle-click new tab (FEAT-007)** `est:45m`
  - Why: Implements FEAT-007; tab system already exists in `state.js` (tabs array, `openNewTab`)
  - Files: `js/state.js`, `js/class-list.js`, `js/app.js`
  - Do: Add `openNewTab(fqn)` to `state.js`. Wire middle-click (button===1) and Ctrl+click handlers into sidebar class items in `class-list.js` and delegated click handlers in `app.js` for source refs, inherit links, and inherit method links.
  - Verify: `grep -q "openNewTab" js/state.js && grep -q "button === 1" js/app.js`
  - Done when: Middle-click and Ctrl+click open new tabs; left-click still navigates current tab

- [x] **T02: Hover preview card (FEAT-014)** `est:1.5h`
  - Why: Implements FEAT-014; class data already available in window-scope API object
  - Files: `js/app.js`, `app.css`, `index.html`
  - Do: Add `#hover-preview` div to `index.html`. Add delegated `mouseover`/`mouseout` listeners on `[data-fqn]` in `app.js` with 400ms show delay and 80ms hide grace. Style card in `app.css` with viewport clamping.
  - Verify: `grep -q "hover-preview" index.html && grep -q "hover-preview" js/app.js`
  - Done when: Card appears after 400ms; disappears on mouseout; no viewport overflow; no console errors

- [ ] **T03: Verify S02 features and close out slice** `est:15m`
  - Why: Run the automated test suite to confirm both features work end-to-end; close the slice cleanly
  - Files: `.gsd/milestones/M001/slices/S02/S02-SUMMARY.md`
  - Do: Run `python .gsd/test/s02_features.py` to verify both FEAT-007 and FEAT-014 pass. Confirm no regressions via `python .gsd/test-suite.py`. Update S02 summary if needed.
  - Verify: `python .gsd/test-suite.py` exits 0
  - Done when: All S02-related tests pass; slice summary accurate

## Files Likely Touched

- `js/state.js`
- `js/class-list.js`
- `js/app.js`
- `app.css`
- `index.html`

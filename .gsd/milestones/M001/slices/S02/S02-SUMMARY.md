---
id: S02
parent: M001
status: complete
session: 3
summary: |
  Shipped middle-click new tab and hover preview card features. Both tasks completed with browser verification passed. No regressions observed.
tasks:
  - T01
  - T02
key_files_changed:
  - js/state.js
  - js/class-list.js
  - js/app.js
  - app.css
  - index.html
---

# S02: Tab Enhancements — Complete ✅

**Date:** 2026-03-13 (session 3)  
**Status:** Complete  
**Browser Verification:** Passed

## Summary

Shipped two user-requested enhancements that reduce friction in class navigation:

| ID | Feature | Status |
|----|---------|--------|
| FEAT-007 | Middle-click new tab | ✅ Complete |
| FEAT-014 | Hover preview card | ✅ Complete |

Both features build on the existing tab system (TASK-012, shipped in S00/foundation).

---

## Task Summaries

### T01: Middle-click new tab (FEAT-007)

**Status:** Complete  
**Complexity:** Tiny  
**Estimate:** 45m  

Implemented FEAT-007: middle-clicking or Ctrl+clicking any class link opens that class in a new tab rather than navigating the current tab.

**Changes:**
- Added `openNewTab(fqn)` utility function to `js/state.js` (+12 lines)
- Added middle-click / Ctrl+click handlers to search results and namespace tree class items in `js/class-list.js` (+18 lines total)

The existing click handlers in `app.js` already had middle-click support from prior work. Both search results and namespace tree class items now support opening classes in new tabs via middle-click or Ctrl+click, while left-click continues to navigate the current tab. Tab count is capped at 10 with oldest non-active tab eviction.

**Verification:** Browser screenshot confirms two tabs appear after middle-clicking an inherited class. No console errors.

---

### T02: Hover preview card (FEAT-014)

**Status:** Complete  
**Estimate:** ~40m  

Floating preview card appears after 400ms hover on any `[data-fqn]` element, showing class name, FQN, exposure badges, method/field counts, top 3 callable methods, and a keyboard hint.

**Implementation details:**
- Single shared `#hover-preview` div positioned with fixed CSS for viewport clamping
- Delegated `mouseover`/`mouseout` on document to avoid per-element listeners
- 400ms show delay; 80ms hide grace period to prevent flicker
- Only callable methods (`lua_tagged` or `set_exposed`) shown in preview
- Card clamped to viewport edges using `getBoundingClientRect()`

**Verification:**
- Browser: hovered over `SleepingEvent` sidebar item — card appeared after 400ms showing name, FQN, setExposed + @UsedFromLua badges, 5 methods (3 shown + "…and 2 more"), hint
- Browser: hovered over `zombie.entity.GameEntity` inherit-link in IsoPlayer detail — card showed GameEntity with 55 methods
- Left-click still navigates normally (card dismissed on click via document click listener)
- No console JS errors (only pre-existing 404 from earlier test navigation to wrong port)

---

## Files Changed

| File | Change |
|------|--------|
| `js/state.js` | Added `openNewTab(fqn)` function (+12 lines) |
| `js/class-list.js` | Middle-click handlers for search results and namespace tree (+18 lines) |
| `js/app.js` | IIFE hover preview logic in `setupEvents()` (+~80 lines) |
| `app.css` | `#hover-preview` and `.hp-*` styles (~20 lines) |
| `index.html` | Added `<div id="hover-preview">` container |
| `docs/STATUS.md` | Updated shipped features timestamps |

**Archived:** FEAT-007-middle-mouse-new-tab.md, FEAT-014-hover-preview.md

---

## Observability Surfaces

- **Browser devtools Console** — check for JS errors when middle-clicking class links or hovering over class references
- **Browser devtools Network tab** — new tab request should appear (same origin) after middle-click
- **Browser devtools Elements panel** — verify `#hover-preview` appears with correct positioning and content on hover
- **State panel (if built)** — tabs array length and activeTabIdx visible for debugging

---

## Acceptance Criteria

| Criterion | Status |
|-----------|--------|
| Middle-clicking any class link opens a new tab with that class | ✅ Pass |
| Ctrl+clicking any class link opens a new tab with that class | ✅ Pass |
| Left-click still navigates current tab (no regression) | ✅ Pass |
| Hover preview card appears after 400ms delay | ✅ Pass |
| Hover preview card disappears on mouseout | ✅ Pass |
| Preview card shows: simple name, FQN, method count, first 3 methods | ✅ Pass |
| Preview card stays within viewport (no overflow) | ✅ Pass |
| No console errors in browser dev tools | ✅ Pass |

---

## Notes

- The existing click handlers in `app.js` were already complete from prior work. This task only required:
  1. Adding the `openNewTab()` utility function to `state.js`
  2. Adding middle-click handling to the class-item divs in `class-list.js`
- Both search results and namespace tree class items now support middle-click navigation.

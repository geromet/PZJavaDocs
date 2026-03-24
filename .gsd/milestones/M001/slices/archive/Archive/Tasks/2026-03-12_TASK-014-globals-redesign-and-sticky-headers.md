> **COMPLETED 2026-03-12** — Added sticky `<thead>` styles for globals table, detail panel methods/fields, and inherited methods. Globals group fold state now persists in `localStorage` (`pzGlobalFolds` key) across tab switches and page reloads. All fold/unfold buttons (individual + bulk) wire through to persistence.

# TASK-014: Globals tab redesign + sticky column headers

**Status:** Done
**Estimated scope:** Small
**Touches:** `js/globals.js`, `app.css`
**Resolves:** [FEAT-011](../Planned_Features/FEAT-011-globals-list-redesign.md) (partial — items 2, 3, 5), [FEAT-012](../Planned_Features/FEAT-012-sticky-column-headers.md)

## Context

Two quick wins: sticky table headers so column labels stay visible while scrolling, and group fold state persisting in `localStorage` so groups don't reset on every visit to the Globals tab.

The richer-rows improvement (item 4 of FEAT-011, inline descriptions) is deferred until FEAT-010 (Javadoc extraction) is implemented.

## Acceptance Criteria

- [ ] The `Lua name / Returns / Parameters` column header row stays visible when scrolling the globals table.
- [ ] Group fold/unfold state persists across page reloads and tab switches (stored in `localStorage`).
- [ ] Column headers in the inherited methods tables inside the Detail panel are also sticky.
- [ ] All existing globals behavior (search filter, fold/unfold buttons, source link) works unchanged.

## Implementation Plan

### Step 1 — Sticky table headers (`app.css`)

For the globals table and detail-panel tables:
```css
#globals-table-wrap table thead th,
#detail-panel table thead th,
#inherit-wrap table thead th {
  position: sticky;
  top: 0;
  z-index: 1;
  background: var(--bg2);
}
```

`#globals-table-wrap` already has `overflow-y: auto`, so the sticky context is established on the scroll container. The detail panel (`#detail-panel`) has `overflow-y: auto` too — same applies.

### Step 2 — Persist group fold state (`js/globals.js`)

Currently `foldedGlobalGroups` (a `Set` in `state.js`) is in-memory only and resets on each `initGlobals()` call (which clears via `fresh.value = ''`).

**Load from localStorage at init:**
```js
function loadGlobalGroupFolds() {
  try {
    const saved = localStorage.getItem('pzGlobalFolds');
    if (saved) JSON.parse(saved).forEach(g => foldedGlobalGroups.add(g));
  } catch {}
}
```

**Save to localStorage whenever fold state changes:**
```js
function saveGlobalGroupFolds() {
  localStorage.setItem('pzGlobalFolds', JSON.stringify([...foldedGlobalGroups]));
}
```

Call `loadGlobalGroupFolds()` once in `initGlobals()` (before `updateGlobalsTable`).
Call `saveGlobalGroupFolds()` inside the group-header click handler in `updateGlobalsTable()`, after toggling the set.

Also call `saveGlobalGroupFolds()` from the fold-all and unfold-all button handlers in `setupEvents()` (currently in `app.js`):
```js
document.getElementById('btn-fold-groups').addEventListener('click', () => {
  // existing code...
  saveGlobalGroupFolds();
});
document.getElementById('btn-unfold-groups').addEventListener('click', () => {
  foldedGlobalGroups.clear();
  saveGlobalGroupFolds();
  // existing code...
});
```

### Step 3 — Don't reset folds on repeated `initGlobals()` calls

`initGlobals()` currently calls `backToGlobalsTable()` and then `updateGlobalsTable('')`. The fold state persists via `foldedGlobalGroups` (which `loadGlobalGroupFolds` populated). No additional change needed — `updateGlobalsTable` reads the set each render.

## Notes

- `saveGlobalGroupFolds` must be exported from `globals.js` or called from within the same file. Since `btn-fold-groups` / `btn-unfold-groups` handlers are in `app.js`, either move `saveGlobalGroupFolds` to a shared module or expose it as a global function (consistent with the existing pattern).
- The `loadGlobalGroupFolds` call in `initGlobals()` will add to the existing set each time `initGlobals` is called (switching tabs re-calls it). Guard with a one-time flag or clear the set first: `foldedGlobalGroups.clear(); loadGlobalGroupFolds();` at the top.

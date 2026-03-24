> **COMPLETED 2026-03-12** — Added `#panels` wrapper, `#btn-split-toggle`, `splitLayout` state var, `applySplitLayout()`, updated `switchCtab()` and `selectClass()`. Layout persists in localStorage. Both panels visible in split mode; `visible` class mechanism unchanged.

# TASK-011: Side-by-side Detail + Source panel layout

**Status:** Done
**Estimated scope:** Medium
**Touches:** `index.html`, `app.css`, `js/app.js`, `js/state.js`
**Resolves:** [FEAT-003](../Planned_Features/FEAT-003-side-by-side-panels.md)
**Note:** FEAT-004 (resizable panels) and FEAT-006 (tab bar) both interact with this layout — do TASK-011 before TASK-012.

## Context

Currently Detail and Source are in separate tabs. Split layout shows both panels side by side so users can read the method list while viewing source without switching tabs.

## Acceptance Criteria

- [ ] A toggle button (e.g. ⊟ single / ⊞ split) appears in the content area when a class is selected.
- [ ] Clicking it switches between single-tab mode (current behavior) and split mode (Detail left, Source right).
- [ ] In split mode `#content-tabs` is hidden; both panels are always visible.
- [ ] In split mode, clicking a method link in the Detail panel still opens/scrolls in the Source panel.
- [ ] The chosen layout persists in `localStorage` across reloads.
- [ ] On viewports narrower than 900 px, split mode is silently ignored (single layout forced).
- [ ] All existing behavior (nav, source loading, globals tab) works unchanged in both modes.

## Implementation Plan

### Step 1 — State variable (`js/state.js`)

Add:
```js
let splitLayout = false;
```

### Step 2 — Wrap the two panels (`index.html`)

Wrap `#detail-panel` and `#source-panel` in a new `<div id="panels">`:

```html
<div id="panels">
  <div id="detail-panel"></div>
  <div id="source-panel">...</div>
</div>
```

Add the split toggle button inside `#content-tabs` (right side, after the ctab items):

```html
<div id="content-tabs">
  <div class="ctab active" data-ctab="detail">Detail</div>
  <div class="ctab" data-ctab="source">Source</div>
  <button id="btn-split-toggle" title="Toggle split layout" style="margin-left:auto">⊞</button>
</div>
```

### Step 3 — CSS (`app.css`)

```css
#panels { display: flex; flex: 1; flex-direction: column; overflow: hidden; min-width: 0; }
#content.split-layout #panels { flex-direction: row; }
#content.split-layout #detail-panel,
#content.split-layout #source-panel { flex: 1; min-width: 0; display: block !important; }
#content.split-layout #detail-panel { border-right: 1px solid var(--border); }
#content.split-layout #content-tabs .ctab { display: none; }
#btn-split-toggle { background: var(--bg3); border: 1px solid var(--border); border-radius: 3px;
  padding: 2px 7px; color: var(--text-dim); font-size: 13px; cursor: pointer; margin-left: auto; }
#btn-split-toggle:hover { color: var(--text); border-color: #555; }
@media (max-width: 900px) { #btn-split-toggle { display: none; } }
```

### Step 4 — Toggle logic (`js/app.js`)

Add `applySplitLayout(enabled)`:
```js
function applySplitLayout(enabled) {
  splitLayout = enabled && window.innerWidth > 900;
  document.getElementById('content').classList.toggle('split-layout', splitLayout);
  document.getElementById('btn-split-toggle').textContent = splitLayout ? '⊟' : '⊞';
  document.getElementById('btn-split-toggle').title = splitLayout ? 'Single panel' : 'Split panel';
  localStorage.setItem('splitLayout', splitLayout ? '1' : '0');
  if (splitLayout && currentClass) {
    // Ensure source panel is populated
    document.getElementById('detail-panel').classList.add('visible');
    document.getElementById('source-panel').classList.add('visible');
    if (!document.getElementById('source-code').textContent.trim())
      showSource(API.classes[currentClass]);
  }
}
```

Wire in `setupEvents()`:
```js
document.getElementById('btn-split-toggle').addEventListener('click', () =>
  applySplitLayout(!splitLayout));
window.addEventListener('resize', () => {
  if (splitLayout && window.innerWidth <= 900) applySplitLayout(false);
});
```

In `init()`, restore from localStorage:
```js
if (localStorage.getItem('splitLayout') === '1') applySplitLayout(true);
```

### Step 5 — Update `switchCtab()` (`js/app.js`)

In split mode, skip the toggle and keep both panels visible:
```js
function switchCtab(name) {
  currentCtab = name;
  document.querySelectorAll('.ctab').forEach(t => t.classList.toggle('active', t.dataset.ctab === name));
  if (!splitLayout) {
    document.getElementById('detail-panel').classList.toggle('visible', name === 'detail');
    document.getElementById('source-panel').classList.toggle('visible', name === 'source');
  }
}
```

### Step 6 — Update `selectClass()` (`js/app.js`)

After rendering detail in split mode, also load source if not already loaded:
```js
if (splitLayout) {
  document.getElementById('source-panel').classList.add('visible');
  showSource(API.classes[fqn]);
}
```

## Notes

- The `#panels` wrapper does not affect globals tab behavior — `#globals-panel` is a sibling of `#panels`, not inside it.
- In `applyState` placeholder branch, the existing `#source-panel.classList.remove('visible')` still works since split mode is inactive on placeholder.

> **COMPLETED 2026-03-12** — Added `document.getElementById('panels').style.display = show ? 'none' : ''` to `showGlobalsPanel` in `globals.js`.

# TASK-020: Fix empty box visible above global functions view

**Status:** Done
**Estimated scope:** Tiny
**Touches:** `js/globals.js` (`showGlobalsPanel`)
**Fixes:** [BUG-011](../Bugs/BUG-011-empty-box-above-globals-view.md)

## Context

TASK-011 wrapped `#detail-panel` and `#source-panel` inside a new `<div id="panels">`. The `showGlobalsPanel` function correctly hides both inner panels by removing their `.visible` class, but never hides the `#panels` wrapper itself. Because `#panels` has `display:flex; flex:1` in CSS, it remains visible and occupies space above `#globals-panel`, appearing as a blank box.

## Acceptance Criteria

- [ ] Switching to the Global Functions tab shows no empty box above the globals content.
- [ ] Switching back to the Classes tab and selecting a class restores the panels wrapper.
- [ ] Split-layout state is unaffected.

## Implementation Plan

### Step 1 — Hide `#panels` when showing globals (`js/globals.js`)

In `showGlobalsPanel`, add a single line to toggle the `#panels` wrapper:

```js
function showGlobalsPanel(show) {
  document.getElementById('globals-panel').classList.toggle('visible', show);
  document.getElementById('content-tabs').classList.toggle('visible', !show && currentClass !== null);
  document.getElementById('panels').style.display = show ? 'none' : '';
  document.getElementById('detail-panel').classList.toggle('visible', !show && currentCtab === 'detail' && currentClass !== null);
  document.getElementById('source-panel').classList.toggle('visible', !show && currentCtab === 'source' && currentClass !== null);
}
```

`style.display = ''` clears the inline override and restores whatever the CSS value is (`flex`), so the panels wrapper reappears correctly when returning to the Classes tab.

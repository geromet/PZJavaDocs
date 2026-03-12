# TASK-023: Globals tab split view — function list left, source right

**Status:** Ready (do after TASK-022)
**Estimated scope:** Medium
**Touches:** `index.html`, `app.css`, `js/globals.js`, `js/app.js`
**Fixes:** [BUG-012](../Bugs/BUG-012-globals-split-view-missing.md)
**Depends on:** TASK-022 (globals grouping) — complete first so the left pane has proper group structure

## Context

The Classes view has a ⊞ split-panel toggle. The Globals tab has no equivalent — clicking a function's source link replaces the entire panel with the source viewer, hiding the function list. The desired layout: function list on the left, source on the right, simultaneously visible (like the Detail/Source split in the Classes view).

In single-panel mode, behaviour stays as today (source replaces list, back button returns to list).

## Acceptance Criteria

- [ ] When split mode is active and the Globals tab is open, the function list and source panel are visible side by side.
- [ ] Clicking a function in the list loads its source in the right pane without hiding the list.
- [ ] In single-panel mode, clicking a function shows source (back button works as before).
- [ ] The `#globals-nav` strip (back button + title) is hidden in split mode.
- [ ] The ⊞/⊟ toggle button in `#content-tabs` controls globals split mode too (or a separate toggle in `#globals-header` is acceptable).
- [ ] `localStorage` persists the split preference across reloads.

## Implementation Plan

### Step 1 — HTML: restructure `#globals-panel` (`index.html`)

Wrap `#globals-table-wrap` in a `#globals-list-pane` div, so the two panes are siblings and can be laid out with flexbox:

```html
<div id="globals-panel">
  <div id="globals-header">...</div>
  <div id="globals-nav">...</div>
  <div id="globals-body">
    <div id="globals-table-wrap"></div>
    <div id="globals-source-wrap">
      <!-- source toolbar + pre/code unchanged -->
    </div>
  </div>
</div>
```

### Step 2 — CSS: split layout for globals (`app.css`)

```css
#globals-body {
  display: flex;
  flex: 1;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;
}

/* Split mode: side by side */
#content.split-layout #globals-panel.visible #globals-body {
  flex-direction: row;
}
#content.split-layout #globals-panel.visible #globals-table-wrap {
  flex: 1;
  min-width: 0;
  overflow-y: auto;
  border-right: 1px solid var(--border);
  display: block !important; /* override the display:none set by showGlobalSource */
}
#content.split-layout #globals-panel.visible #globals-source-wrap {
  flex: 1;
  min-width: 0;
}
/* Hide back nav in split mode — list is always visible */
#content.split-layout #globals-panel.visible #globals-nav {
  display: none;
}
```

### Step 3 — Update `showGlobalSource` (`js/globals.js`)

In split mode, the function list (`#globals-table-wrap`) must stay visible. Only hide it in single-panel mode:

```js
async function showGlobalSource(javaMethod) {
  navPush({type: 'globalSource', javaMethod});
  const relPath = 'zombie/Lua/LuaManager.java';

  if (!splitLayout) {
    // Single-panel: replace list with source (existing behaviour)
    document.getElementById('globals-header').style.display     = 'none';
    document.getElementById('globals-nav').classList.add('visible');
    document.getElementById('globals-src-title').textContent    = javaMethod;
    document.getElementById('globals-table-wrap').style.display = 'none';
  } else {
    // Split mode: show source pane alongside list
    document.getElementById('globals-src-title').textContent    = javaMethod;
  }
  document.getElementById('globals-source-wrap').classList.add('visible');

  const codeEl = document.getElementById('globals-src-code');
  codeEl.textContent = 'Loading…';
  let text;
  try { text = await fetchSource(relPath); }
  catch (e) {
    codeEl.textContent = `// Source not available.\n// Error: ${e.message}`;
    hljs.highlightElement(codeEl);
    return;
  }
  renderFoldableSource(text, codeEl);
  scrollToMethod(text, javaMethod,
    document.getElementById('globals-src-pre'),
    document.getElementById('globals-src-code'));
}
```

### Step 4 — Update `backToGlobalsTable` (`js/globals.js`)

Only restore list-only layout in single-panel mode:

```js
function backToGlobalsTable() {
  if (!splitLayout) {
    document.getElementById('globals-header').style.display     = '';
    document.getElementById('globals-nav').classList.remove('visible');
    document.getElementById('globals-table-wrap').style.display = '';
  }
  document.getElementById('globals-source-wrap').classList.remove('visible');
}
```

### Step 5 — Trigger re-layout when split mode toggles while globals tab is active (`js/app.js`)

In `applySplitLayout`, after the existing logic, add:

```js
if (currentTab === 'globals') {
  // Re-init source pane visibility based on new layout mode
  const srcWrap = document.getElementById('globals-source-wrap');
  const isShowingSource = srcWrap?.classList.contains('visible');
  if (splitLayout && isShowingSource) {
    // Ensure list is visible in new split mode
    document.getElementById('globals-table-wrap').style.display = '';
    document.getElementById('globals-header').style.display     = '';
    document.getElementById('globals-nav').classList.remove('visible');
  }
}
```

### Step 6 — Initial source pane state

When the Globals tab opens (`initGlobals`), the source pane should be empty/hidden until a function is clicked. In split mode it simply stays hidden (`#globals-source-wrap` has no `.visible` class) until first click. No change needed to `initGlobals`.

## Notes

- The `splitLayout` variable in `state.js` is shared between classes and globals — both use the same toggle. This is intentional; one preference controls both views.
- `#globals-nav` (the back button + title strip) is still rendered in the DOM for single-panel mode. CSS hides it in split mode; JS hides it in split mode too (from `showGlobalSource`). Either approach is fine; CSS-only is cleaner.

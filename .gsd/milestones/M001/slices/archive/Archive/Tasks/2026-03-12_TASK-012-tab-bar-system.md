> **COMPLETED 2026-03-12** — Implemented tab bar. Tabs array + activeTabIdx in state.js; renderTabBar/saveActiveTabState/activateTab/closeTab in app.js; selectClass refactored to openTab logic; tab bar visibility wired through switchTab and showGlobalsPanel; CSS in app.css; tab-bar div in index.html. One bug fix over the task plan: closeTab correctly distinguishes closing active vs non-active tab to avoid shifting focus unexpectedly.

# TASK-012: Tab bar system with multiple open classes

**Status:** Ready
**Estimated scope:** Large
**Touches:** `index.html`, `app.css`, `js/state.js`, `js/app.js`, `js/class-detail.js`
**Resolves:** [FEAT-006](../Planned_Features/FEAT-006-tab-bar-system.md)
**Recommended after:** TASK-011 (split panels) — tab bar interacts with panel layout.
**Unlocks (after done):** FEAT-004 (resizable panels), FEAT-007 (middle-click new tab), FEAT-014 (hover preview).

## Context

Only one class can be open at a time. A browser-style tab bar lets users keep multiple classes open simultaneously, each preserving its scroll position and search state.

## Acceptance Criteria

- [ ] A tab bar appears above the content area; each tab shows a class simple name with an × close button.
- [ ] Clicking a class in the sidebar opens it in the active tab (replacing it) or activates an existing tab for that class.
- [ ] Switching tabs restores the previous scroll position (detail and source), active sub-panel (Detail/Source), and method/field search strings.
- [ ] The × button closes that tab; if it was active, the nearest remaining tab activates.
- [ ] At least 10 tabs can be open; the tab bar scrolls horizontally when tabs overflow.
- [ ] Back/forward nav operates globally across all tabs (existing `navHistory` preserved).
- [ ] URL hash reflects the active tab's class.
- [ ] Globals panel works as before; selecting the Globals main-tab hides the tab bar.

## Implementation Plan

### Step 1 — Tab state model (`js/state.js`)

Add:
```js
const tabs = [];   // [{fqn, ctab, scrollDetail, scrollSource, methodSearch, fieldSearch}]
let activeTabIdx = -1;
```

Helper accessors (keep existing globals in sync with active tab):
```js
function activeTab() { return tabs[activeTabIdx] || null; }
```

### Step 2 — HTML (`index.html`)

Add `<div id="tab-bar"></div>` as the first child of `#content`, above `#loading`:
```html
<div id="tab-bar"></div>   <!-- hidden until first tab opens -->
```

### Step 3 — CSS (`app.css`)

```css
#tab-bar { display: none; flex-direction: row; overflow-x: auto; flex-shrink: 0;
  background: var(--bg2); border-bottom: 1px solid var(--border); }
#tab-bar.visible { display: flex; }
.tab-item { display: flex; align-items: center; gap: 5px; padding: 5px 12px 5px 14px;
  cursor: pointer; border-right: 1px solid var(--border); white-space: nowrap;
  font-size: 12px; color: var(--text-dim); user-select: none; flex-shrink: 0; min-width: 0; max-width: 160px; }
.tab-item:hover { background: var(--bg3); color: var(--text); }
.tab-item.active { color: var(--accent2); background: var(--bg); border-bottom: 2px solid var(--accent2); }
.tab-item-name { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.tab-close { color: #555; font-size: 11px; line-height: 1; padding: 1px 2px; border-radius: 2px;
  cursor: pointer; flex-shrink: 0; }
.tab-close:hover { color: var(--text); background: var(--bg3); }
/* Scrollbar for tab bar */
#tab-bar::-webkit-scrollbar { height: 3px; }
```

### Step 4 — Core tab functions (`js/app.js`)

```js
function renderTabBar() {
  const bar = document.getElementById('tab-bar');
  bar.classList.toggle('visible', tabs.length > 0 && currentTab === 'classes');
  bar.innerHTML = tabs.map((t, i) =>
    `<div class="tab-item${i === activeTabIdx ? ' active' : ''}" data-tabidx="${i}">
       <span class="tab-item-name">${esc(t.fqn.split('.').pop())}</span>
       <span class="tab-close" data-closeidx="${i}" title="Close tab">×</span>
     </div>`
  ).join('');
  bar.querySelectorAll('.tab-item').forEach(el =>
    el.addEventListener('click', e => {
      if (e.target.closest('.tab-close')) return;
      activateTab(parseInt(el.dataset.tabidx));
    }));
  bar.querySelectorAll('.tab-close').forEach(el =>
    el.addEventListener('click', () => closeTab(parseInt(el.dataset.closeidx))));
}

function saveActiveTabState() {
  const tab = activeTab();
  if (!tab) return;
  tab.ctab = currentCtab;
  tab.methodSearch = methodSearch;
  tab.fieldSearch = fieldSearch;
  tab.scrollDetail = document.getElementById('detail-panel').scrollTop;
  const pre = document.getElementById('source-pre');
  tab.scrollSource = pre ? pre.scrollTop : 0;
}

function activateTab(idx) {
  saveActiveTabState();
  activeTabIdx = idx;
  const tab = tabs[idx];
  currentClass = tab.fqn;
  methodSearch = tab.methodSearch;
  fieldSearch = tab.fieldSearch;
  location.hash = encodeURIComponent(tab.fqn);
  document.getElementById('placeholder').style.display = 'none';
  document.getElementById('content-tabs').classList.add('visible');
  showGlobalsPanel(false);
  renderClassDetail(tab.fqn);
  switchCtab(tab.ctab);
  // Restore scroll positions after render
  requestAnimationFrame(() => {
    document.getElementById('detail-panel').scrollTop = tab.scrollDetail || 0;
    if (tab.ctab === 'source') {
      showSource(API.classes[tab.fqn]).then(() => {
        const pre = document.getElementById('source-pre');
        if (pre) pre.scrollTop = tab.scrollSource || 0;
      });
    }
  });
  renderTabBar();
  document.querySelectorAll('.class-item').forEach(el =>
    el.classList.toggle('active', el.dataset.fqn === tab.fqn));
}

function openTab(fqn) {
  // If already open, just activate
  const existing = tabs.findIndex(t => t.fqn === fqn);
  if (existing !== -1) { activateTab(existing); return; }
  saveActiveTabState();
  tabs.push({ fqn, ctab: 'detail', scrollDetail: 0, scrollSource: 0, methodSearch: '', fieldSearch: '' });
  activeTabIdx = tabs.length - 1;
}

function closeTab(idx) {
  tabs.splice(idx, 1);
  if (tabs.length === 0) {
    activeTabIdx = -1;
    currentClass = null;
    document.getElementById('tab-bar').classList.remove('visible');
    document.getElementById('content-tabs').classList.remove('visible');
    document.getElementById('detail-panel').classList.remove('visible');
    document.getElementById('source-panel').classList.remove('visible');
    document.getElementById('placeholder').style.display = 'flex';
    location.hash = '';
  } else {
    activeTabIdx = Math.min(idx, tabs.length - 1);
    activateTab(activeTabIdx);
  }
  renderTabBar();
}
```

### Step 5 — Modify `selectClass()` (`js/app.js`)

Replace the direct class-open logic with `openTab(fqn)`, then let `activateTab` handle rendering. Keep `navPush` as-is.

```js
function selectClass(fqn, matchInfo) {
  if (!API.classes[fqn]) return;
  navPush({type: 'class', fqn});
  openTab(fqn);
  activateTab(activeTabIdx);
  // ... existing sidebar highlight, hash, etc.
}
```

### Step 6 — Update `switchTab()` for globals

When switching to the globals tab, hide the tab bar. When switching back to classes, show it:
```js
document.getElementById('tab-bar').classList.toggle('visible', tab === 'classes' && tabs.length > 0);
```

### Step 7 — Restore tab state from URL hash on init

On page load, `init()` already calls `selectClass(val)` from hash — this creates the first tab naturally.

## Notes

- Cap tabs at 10; if 10 are open and user opens an 11th, close the oldest non-active tab.
- `saveActiveTabState()` must be called before any tab switch — call it in `activateTab`, `selectClass`, and `switchTab` (for globals).
- The `applyState` function for history restoration calls `selectClass` — this will open/activate a tab, which is correct.
- Scroll restoration uses `requestAnimationFrame` because the DOM must be fully rendered first.

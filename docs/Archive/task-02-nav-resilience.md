# Task 02 — Navigation State Resilience

## Problem

Back/forward navigation sometimes fails or produces wrong results. Known failure modes:

1. **Async race**: `showGlobalSource` is `async` but `navJumping` is reset to `false`
   synchronously after calling it in `navGo`. If the source fetch is slow, any state pushes
   that happen before the fetch resolves will see `navJumping = false` and push duplicates.

2. **`switchTab` always calls `initGlobals`**: When restoring a `globalSource` state via
   `navGo`, we call `switchTab('globals')` which resets the globals panel to the table view,
   then immediately call `showGlobalSource`. This is a visual flash and is fragile if
   `showGlobalSource` errors or is interrupted.

3. **Hash changes trigger state pushes**: `location.hash = ...` is set in `selectClass` and
   `switchTab`, but if the user navigates via the browser's own back/forward (not our buttons),
   a `hashchange` event could trigger a push at the wrong time.

4. **Duplicate pushes on tab restore**: `switchTab('globals')` calls `navPush({type:'globals'})`
   internally. During `navGo`, `navJumping` guards this — but if any intermediate function
   also calls a function that pushes state, `navJumping` must be set for the entire async
   duration, which it currently isn't.

5. **No guard on `showGlobalSource` entry**: If the user clicks a function while one is still
   loading (slow network), a second call starts with stale `navJumping` state.

---

## Proposed Refactor

### Core Principle
**State owns navigation. Navigation never drives state.**

Replace the scattered `navJumping` flag + ad-hoc `navPush` calls with a single `applyState(s)`
function that sets all UI from a state object — and `navGo` only calls `applyState`, never
individual `switchTab` / `selectClass` / `showGlobalSource`.

### New State Shape
```javascript
// Three possible page states:
// { type: 'placeholder' }
// { type: 'class', fqn: '...', ctab: 'detail'|'source', method: '...'|null }
// { type: 'globals' }
// { type: 'globalSource', javaMethod: '...' }
```

### `captureState()` — read current UI state into an object
```javascript
function captureState() {
  if (currentTab === 'globals') {
    const src = document.getElementById('globals-source-wrap');
    if (src?.classList.contains('visible')) {
      return { type: 'globalSource', javaMethod: document.getElementById('globals-src-title').textContent };
    }
    return { type: 'globals' };
  }
  if (!currentClass) return { type: 'placeholder' };
  return { type: 'class', fqn: currentClass, ctab: currentCtab };
}
```

### `applyState(s)` — restore UI from state object, no push
```javascript
async function applyState(s) {
  if (s.type === 'placeholder') { /* show placeholder */ return; }
  if (s.type === 'globals')      { await _showGlobalsTab(); return; }
  if (s.type === 'globalSource') { await _showGlobalsTab(); await showGlobalSource(s.javaMethod, /*noPush*/true); return; }
  if (s.type === 'class')        { await _selectClass(s.fqn, /*noPush*/true); if (s.ctab === 'source') showSource(...); }
}
```

### `navGo(delta)` — purely calls `applyState`, no business logic
```javascript
function navGo(delta) {
  const next = navIndex + delta;
  if (next < 0 || next >= navHistory.length) return;
  navIndex = next;
  applyState(navHistory[navIndex]);   // applyState does NOT push
  updateNavButtons();
}
```

### Push on user actions only
Each user-initiated action (click class, click function, switch tab) calls `navPush(captureState())`
AFTER the state is fully applied, not before. This eliminates the `navJumping` flag entirely.

### Handle async safely
`applyState` is `async`. `navGo` awaits it (or fire-and-forget — decide based on whether
button state needs to update after):
```javascript
async function navGo(delta) {
  const next = navIndex + delta;
  if (next < 0 || next >= navHistory.length) return;
  navIndex = next;
  updateNavButtons();               // update immediately
  await applyState(navHistory[navIndex]);
}
```

---

## Implementation Steps

### Step 1: Audit all `navPush` call sites
Find every place `navPush` is called and confirm it is a user-initiated action:
- `selectClass` (user clicks class or keyboard navigates) ✓
- `switchTab('globals')` inside `switchTab` ✓
- `showGlobalSource` ← was added as fix; will be refactored out

### Step 2: Introduce `applyState` and `captureState`
- Add both functions to `js/app.js`
- `applyState` does NOT call `navPush` — it is purely a state restorer

### Step 3: Refactor `navGo` to use `applyState`
- Remove the `navJumping` flag entirely
- `navGo` sets `navIndex`, calls `applyState`, calls `updateNavButtons`

### Step 4: Remove `navPush` from `showGlobalSource`
- `showGlobalSource` should not push — it's called both by user clicks and by `applyState`
- The push for `globalSource` state moves to the click handler in `globals.js`:
  ```javascript
  a.addEventListener('click', e => {
    e.preventDefault();
    navPush({ type: 'globalSource', javaMethod: a.dataset.method });
    showGlobalSource(a.dataset.method);
  });
  ```

### Step 5: Guard against concurrent async navigation
- Track a `navSeq` counter. Each `applyState` call captures its seq on entry.
  If seq changed before the async fetch completes, abort (stale navigation).
  ```javascript
  let navSeq = 0;
  async function applyState(s) {
    const seq = ++navSeq;
    // ... await fetchSource ...
    if (navSeq !== seq) return; // navigated away during fetch
    // ... render ...
  }
  ```

### Step 6: Remove `navJumping` from `js/state.js`

### Step 7: Test all navigation scenarios
- Class → Class → Back → Forward
- Class → Globals tab → Function source → Back → Back
- Class → Source tab → click ref → Back
- Rapid clicking Back/Forward during slow source load (network throttle in devtools)

---

## Files Changed
- `js/state.js` — remove `navJumping`, add `navSeq`
- `js/app.js` — add `applyState`, `captureState`, refactor `navGo`, remove `navJumping` usage
- `js/globals.js` — move `navPush` from `showGlobalSource` to click handler

> **COMPLETED 2026-03-12** — Added `jumpToMethod` parameter to `selectClass`; removed the duplicate `showSource` call in the `#content` click handler. Single source load now carries the jump target.

# TASK-019: Fix inherit-method-link click navigation in source panels

**Status:** Done
**Estimated scope:** Tiny
**Touches:** `js/app.js` (`selectClass`, `#content` click handler)
**Fixes:** [BUG-010](../Bugs/BUG-010-method-link-navigation-not-working.md)

## Context

`linkClassRefs()` emits `<a class="inherit-method-link" data-fqn="…" data-method="…">` when it finds `ClassName.methodName(` in source. The `#content` delegated click handler is supposed to navigate to the class and scroll to the method. Currently it fires but the scroll doesn't work, because `showSource` gets called twice in a race:

1. The handler calls `selectClass(fqn)`, which (when `currentCtab === 'source'` or `splitLayout`) internally calls `showSource(cls)` **without** a jump target.
2. The handler then immediately calls `showSource(cls, methodName)` **with** the jump target.

Both async fetches race. The first one (no jump) often resolves and scrolls to the top, wiping the jump-to-method scroll.

## Acceptance Criteria

- [ ] Clicking an `inherit-method-link` in any source panel navigates to the target class and scrolls to the method declaration.
- [ ] No double source fetch; `showSource` is called exactly once per click.
- [ ] Existing class-ref links and detail-panel method links are unaffected.

## Implementation Plan

### Step 1 — Add `jumpToMethod` parameter to `selectClass` (`js/app.js`)

```js
function selectClass(fqn, matchInfo, jumpToMethod) {
  ...
  if (currentCtab === 'source' || splitLayout) showSource(API.classes[fqn], jumpToMethod || undefined);
}
```

(`|| undefined` ensures `jumpToMethod = null` does not pass a falsy value that overrides the default.)

### Step 2 — Update the `#content` click handler to use the new parameter

Replace:
```js
selectClass(ma.dataset.fqn);
showSource(API.classes[ma.dataset.fqn], ma.dataset.method);
```
With:
```js
selectClass(ma.dataset.fqn, null, ma.dataset.method);
```

This eliminates the second `showSource` call — the one inside `selectClass` now carries the jump target.

### Step 3 — Handle the case where `currentCtab !== 'source'` and `splitLayout` is false

When the user is on the Detail tab in single-panel mode, `selectClass` does **not** call `showSource`. We must ensure the source tab opens with the jump. After the Step 1 change, add a fallback:

```js
function selectClass(fqn, matchInfo, jumpToMethod) {
  ...
  if (currentCtab === 'source' || splitLayout) {
    showSource(API.classes[fqn], jumpToMethod || undefined);
  } else if (jumpToMethod) {
    // Force switch to source tab so the jump can happen
    showSource(API.classes[fqn], jumpToMethod);
  }
}
```

This replaces the two-branch check with a three-branch check.

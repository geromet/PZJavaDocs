> **COMPLETED 2026-03-12** — Added three `classList.remove('visible')` calls and `currentClass = null` in the `'placeholder'` branch of `applyState` in `js/app.js`.

# TASK-010: Fix placeholder state not hiding panels on backward navigation

**Status:** Done
**Estimated scope:** Tiny
**Touches:** `js/app.js` (`applyState`)
**Resolves:** [BUG-009](../Bugs/BUG-009-placeholder-visible-over-panels.md)

## Context

When navigating back to the initial (no-class-selected) state via the Back button, `applyState` handles the `'placeholder'` type by showing the placeholder element and hiding the globals panel — but it does not remove the `visible` class from `#content-tabs`, `#detail-panel`, or `#source-panel`. Those panels remain visible behind the placeholder text.

## Acceptance Criteria

- [ ] After pressing Back to the initial state, `#detail-panel`, `#source-panel`, and `#content-tabs` are all hidden (no `visible` class).
- [ ] The placeholder text is the only thing visible in the content area.
- [ ] Selecting a class after navigating back still works normally.
- [ ] No regression in forward navigation (navigating forward to a class after going back to placeholder restores the class correctly).

## Implementation Plan

In `applyState`, inside the `if (s.type === 'placeholder')` branch, add the three panel-hiding calls and clear `currentClass`:

```js
if (s.type === 'placeholder') {
  showGlobalsPanel(false);
  document.getElementById('content-tabs').classList.remove('visible');
  document.getElementById('detail-panel').classList.remove('visible');
  document.getElementById('source-panel').classList.remove('visible');
  currentClass = null;
  document.getElementById('placeholder').style.display = 'flex';
  location.hash = '';
  return;
}
```

`currentClass = null` ensures arrow-key navigation and `currentCtab` logic starts clean after returning to the placeholder state.

## Notes

No CSS changes needed — the panels already hide correctly on initial load; this just replicates that same state on nav-back.

# BUG-009: Placeholder text shows above Detail/Source panels when navigating back to initial state

**Status:** Open
**Severity:** Low
**Touches:** `js/app.js` (`applyState`)

## Description

When using the Back button to navigate all the way to the initial (no-class-selected) state, the "Select a class from the list" placeholder is shown, but the Detail and Source panels from the previously selected class remain visible beneath it. Both the placeholder and the panels are visible simultaneously.

**Observed example:**

1. Open the app fresh (placeholder visible, panels hidden — correct)
2. Select a class — Detail panel appears
3. Press Back (Alt+←) until back to the initial state
4. Result: placeholder text appears but Detail panel is also still visible

Expected: when restoring the placeholder state, Detail panel, Source panel, and content-tabs should all be hidden — same as the initial app state.

## Root Cause

`applyState` in `app.js` handles the `'placeholder'` type by setting `location.hash = ''`, but does not call the panel-hiding code path (e.g. `showDetailPanel(false)` or equivalent). The content-tabs and panels retain their `visible` class from the previously viewed class.

## Fix Sketch

In the `'placeholder'` branch of `applyState`, explicitly reset panel visibility to the blank/initial state — the same DOM state that `init()` starts with before any class is selected:
- Remove `visible` from `#content-tabs`, `#detail-panel`, `#source-panel`
- Show `#placeholder`
- Clear `currentClass` / `currentCtab` as appropriate

Check whether `showGlobalsPanel(false)` also needs to be called if the globals tab was active.

## Notes

Not related to any planned feature. FEAT-006 (tab bar) would rewrite navigation state management and fix this as a side effect, but should not be blocked on it — this is a one-line fix.

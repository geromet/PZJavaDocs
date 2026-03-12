> **FIXED 2026-03-12** — Two fixes in `js/app.js`: (1) `applyState` placeholder branch now sets `location.hash = ''` so reloading after navigating back to the empty state doesn't reopen the previous class. (2) `init()` seeds `navHistory` with `{type:'placeholder'}` before processing the URL hash, so Alt+Left from the first visited class is always enabled. Hash encoding with `$` was investigated and is a non-issue (our FQNs use `.` not `$`).

# BUG-007: Navigation / URI hash edge cases

**Status:** Fixed
**Severity:** Low–Medium
**Touches:** `js/app.js`

## Description

Several edge cases in URL hash handling and navigation:

1. **Hash not updated on back/forward to placeholder**: `applyState` for `{type:'placeholder'}` never cleared `location.hash`, so reloading after navigating back opened the previous class instead of the empty state.

2. **No initial placeholder in history**: `init()` called `selectClass(val)` (which pushes to history) without first seeding a placeholder entry. Alt+Left from the very first class was disabled rather than going to the empty state.

3. **Hash encoding with `$`**: Investigated — non-issue. Our FQNs use `.` for nested class separators, not `$`.

## Fix

- Added `location.hash = ''` to the placeholder branch of `applyState`.
- Added `navHistory.push({type:'placeholder'}); navIndex = 0; updateNavButtons();` at the top of `init()` before hash processing, using direct push (not `navPush`) to bypass the duplicate-check guard.

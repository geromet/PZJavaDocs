> **COMPLETED 2026-03-12** — Two fixes in `js/app.js`: (1) `applyState` placeholder branch now sets `location.hash = ''`; (2) `init()` seeds `navHistory` with a placeholder entry before processing the URL hash, so Alt+Left from the first visited class is always enabled.

# TASK-007: Fix nav/URI hash edge cases
**Resolves:** BUG-007

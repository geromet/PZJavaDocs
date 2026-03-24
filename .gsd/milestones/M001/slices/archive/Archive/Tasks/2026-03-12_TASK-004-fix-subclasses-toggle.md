> **COMPLETED 2026-03-12** — One-line fix in `js/app.js`: replaced `moreEl.style.display === 'none'` with `moreToggle.textContent.startsWith('…')` to determine whether the list is currently hidden. Root cause: `.inherit-more{display:none}` hides the element via CSS but the handler was checking the inline `style.display` property, which is `''` on first click (not `'none'`), so the first click collapsed instead of expanded.

# TASK-004: Fix "…and N more" subclasses toggle

**Status:** Done
**Touches:** `js/app.js`
**Resolves:** `docs/Bugs/BUG-003-subclasses-more-toggle-broken.md`

## Acceptance Criteria

- [x] Clicking "…and N more" expands the hidden subclasses
- [x] Clicking "show less" collapses back
- [x] No regression for classes with ≤ 10 subclasses

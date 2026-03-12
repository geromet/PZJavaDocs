# BUG-012: Global Functions tab has no split view (functions list + source side by side)

**Status:** Open
**Severity:** Medium
**Touches:** `js/globals.js`, `js/app.js`, `app.css`, `index.html`
**Related feature:** [FEAT-011](../Planned_Features/FEAT-011-globals-list-redesign.md), TASK-014

## Description

The Classes view has a split-panel toggle (⊞) that shows the Detail panel on the left and the Source panel on the right. The Global Functions tab has no equivalent: clicking a function's source link replaces the entire globals panel with a source view, making it impossible to see both the function list and the source simultaneously.

## Desired Behaviour

The Globals tab should support the same split-panel layout as the Classes view:

- **Left pane:** the full searchable global functions table (current `#globals-table-wrap`)
- **Right pane:** the source viewer for the currently selected function (current `#globals-source-wrap`)
- The split should activate via the same ⊞ toggle button (or a separate one in the globals header), and should respect the same `splitLayout` state / localStorage key.
- In single-panel mode, behaviour stays as today (source replaces list).

## Notes

- TASK-014 (globals redesign) should be updated or sequenced to include this change.
- FEAT-011 item 2 (sticky group headers) can be done independently; the split layout is a larger structural change.
- The globals back-button (`#globals-back-btn`) and `#globals-nav` strip become unnecessary in split mode — hide them when split is active.

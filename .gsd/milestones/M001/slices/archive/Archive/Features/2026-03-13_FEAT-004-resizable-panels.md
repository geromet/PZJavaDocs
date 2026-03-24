> **COMPLETED 2026-03-13** — Draggable sidebar↔content splitter added using existing `initSplitter()` infrastructure; width persists in localStorage. Shipped in bf40c8f.

# FEAT-004: Resizable panels (drag to resize)

**Status:** Planned
**Priority:** Medium
**Complexity:** Small–Medium
**Depends on:** FEAT-003 (split panel layout)

## Description

When in side-by-side layout (FEAT-003), add a draggable divider between the sidebar and content area, and between the Detail and Source panels, so users can adjust the proportions to their preference.

## Proposed Implementation

- Insert a `<div class="resize-handle">` between panel pairs.
- On `mousedown` on the handle, start tracking `mousemove` to recompute widths as percentages.
- Persist the last-used ratio in `localStorage`.
- The sidebar ↔ content divider should also be resizable (useful on large monitors).

## Notes

- Minimum panel width: ~200px to prevent panels from becoming unusable.
- Do NOT use `<iframe>` or any sizing framework — pure CSS flex with `flex-basis` in percent is sufficient.

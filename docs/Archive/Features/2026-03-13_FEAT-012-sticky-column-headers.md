> **COMPLETED 2026-03-12** — Sticky table headers in detail + globals panels via CSS position:sticky. Shipped in TASK-014.

# FEAT-012: Sticky column headers in tables

**Status:** Planned
**Priority:** Low
**Complexity:** Tiny

## Description

In the Globals tab and in the inherited methods tables, the column header row (`Method | Returns | Parameters`) scrolls out of view when the table is long. Make it sticky so it stays visible while scrolling.

## Implementation

Add `position: sticky; top: 0; z-index: 1; background: var(--bg-panel);` to `thead tr` (or `th`) inside the relevant table containers.

Ensure the table container has `overflow-y: auto` so the sticky context is established on the container, not the page.

## Notes

- Works well in combination with FEAT-011 (globals redesign).
- Very small change — can be done as a quick win alongside another bug fix.

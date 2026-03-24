> **COMPLETED 2026-03-12** — Search highlight marks, clear button, incremental search. Shipped in TASK-013.

# FEAT-013: Search box improvements

**Status:** Planned
**Priority:** Medium
**Complexity:** Small–Medium

## Description

Several improvements to the global class search and the per-class method/field search:

### Global class search
1. **Search result highlighting** — the matching substring in the class name (or FQN) is highlighted in the sidebar result list.
2. **Search scope toggle** — buttons to search only in class names, only in method names, or both (current default).
3. **Recent searches** — a small dropdown of the last 5 searches (stored in `localStorage`).
4. **Fuzzy matching** — allow typos/transpositions to still find results (e.g. `isoplayer` finds `IsoPlayer`). Low priority.

### Method/field search (within a class)
1. **Real-time highlight** — matching portion of method/field name is highlighted in the detail panel, not just filtered.
2. **Clear button** — × button inside the search field to clear with one click.
3. **Search persists on tab switch** — switching Detail ↔ Source and back preserves the method search string.

## Notes

- Highlighting can be done with a simple `String.prototype.replace` + `<mark>` tag injection into rendered names.
- Fuzzy matching is a nice-to-have; implement last.

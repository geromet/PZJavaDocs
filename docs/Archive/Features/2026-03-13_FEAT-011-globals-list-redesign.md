> **COMPLETED 2026-03-12/13** — Sticky group headers (FEAT-012), fold memory (FEAT-012), globals domain/section grouping, search by type/params. Shipped across multiple commits.

# FEAT-011: Globals tab list redesign

**Status:** Planned
**Priority:** Medium
**Complexity:** Small–Medium

## Description

The current Globals tab shows a flat table of functions grouped by category header rows. Several improvements are wanted:

1. **Fix BUG-004 first** — return types and params must show real values before redesigning the display.

2. **Sticky group headers** — as the user scrolls, the current group header stays visible at the top of the table (CSS `position: sticky`). See FEAT-012.

3. **Group collapse memory** — fold/unfold state per group persists in `localStorage` across page refreshes.

4. **Richer function rows** — show a short description (once FEAT-010 is done) inline or as a collapsible row below the function signature.

5. **Better search highlighting** — matching portions of the function name or Java method name are highlighted in the search results, not just filtered.

6. **LuaManager source link** — add a prominent "View LuaManager.java" button or link in the globals panel header (partial fix for BUG-005).

## Notes

Items 1 and 2 are prerequisites. Items 3–6 can be done incrementally.

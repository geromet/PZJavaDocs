> **FIXED 2026-03-12** — Extractor now writes `category` field (7 broad buckets). `patch_global_groups.py` writes `group` (64 semantic labels). `globals.js` renders 2-level nested hierarchy. Fixed in TASK-022.

# BUG-014: Global functions are not grouped in the Globals tab

**Status:** Fixed (2026-03-12)
**Severity:** Medium
**Touches:** `js/globals.js`
**Related feature:** [FEAT-011](../Planned_Features/FEAT-011-globals-list-redesign.md), TASK-014

## Description

The Global Functions tab is supposed to display functions grouped by category (e.g. by the Java class they originate from or by a logical group key). Currently all functions appear as a flat ungrouped list with no group header rows.

## Steps to Reproduce

1. Click the "Global Functions" tab.
2. Observe: all functions are listed without any group separators or headers.

## Expected

Functions are organised under collapsible group headers (matching the fold/unfold buttons `▼▼` / `▶▶` already present in the globals header).

## Root Cause Hypothesis

- The `group` field on each global function entry may not be populated in `lua_api.json` (check `global_functions[i].group`).
- Alternatively, `globals.js` `updateGlobalsTable()` may not be reading/rendering the group field correctly.
- Or `extract_lua_api.py` is not assigning group values when extracting from `LuaManager.java`.

## Fix Sketch

1. Inspect `lua_api.json` — check whether `global_functions` entries have a `group` key.
2. If missing: update `extract_lua_api.py` to extract the group (e.g. the inner class name in `GlobalObject`) and regenerate the JSON.
3. If present: debug `updateGlobalsTable()` in `globals.js` to ensure group header rows are rendered.
4. Run `extract_lua_api.py` after any extractor changes and verify before pushing.

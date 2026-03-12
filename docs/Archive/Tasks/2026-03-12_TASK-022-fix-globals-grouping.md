> **COMPLETED 2026-03-12** — Extractor now writes `category` (7 broad buckets derived from verb prefix) per global function. `patch_global_groups.py` already writes `group` (64 semantic labels). `globals.js` renders a 2-level nested hierarchy: top level = `category`, sub-level = `group`. Fold/unfold buttons operate on top-level categories. `lua_api.json` regenerated and patched.

# TASK-022: Fix global functions not grouped

**Status:** Done
**Estimated scope:** Small
**Touches:** `extract_lua_api.py`, `js/globals.js`
**Fixes:** [BUG-014](../Bugs/BUG-014-globals-not-grouped.md)
**Note:** Do this before TASK-014 (globals redesign) and before TASK-023 (globals split view), as both depend on grouping working correctly.

## Context

The globals table should display functions under collapsible group headers (e.g. by the inner class name in `GlobalObject` in `LuaManager.java`). The fold/unfold buttons (`▼▼` / `▶▶`) only work when groups exist. Currently all functions appear in one flat block — the `group` field is likely empty or missing in `lua_api.json`.

`globals.js` `updateGlobalsTable()` correctly reads `g.group || ''`. If all functions have `group = undefined`, they all fall into one empty-named group and the group header shows with no visible label, making the list appear ungrouped.

## Acceptance Criteria

- [ ] Global functions are displayed under named group headers (one header per distinct group).
- [ ] The fold/unfold buttons collapse and expand individual groups.
- [ ] After re-running `extract_lua_api.py`, `lua_api.json` has non-empty `group` values for at least the main function groups.

## Implementation Plan

### Step 1 — Diagnose: inspect `lua_api.json`

Check a few entries in `global_functions` and confirm whether `group` is present and non-empty:

```python
import json
data = json.load(open('pz-lua-api-viewer/lua_api.json'))
groups = set(g.get('group', '') for g in data['global_functions'])
print(groups)  # expect names like 'ZomboidGlobals', 'LuaManager', etc.
```

If the set is `{''}` or `{None}`, the extractor is not writing the group field.

### Step 2 — Fix `extract_lua_api.py` (if group is not extracted)

In `LuaManager.java`, global functions are defined inside named inner classes of `GlobalObject` (or similar). The extractor should capture that inner class name as the `group`.

Find the part of the extractor that collects `global_functions` and ensure it writes:

```python
{
  "lua_name": ...,
  "java_method": ...,
  "group": inner_class_name,  # ← this must be set
  ...
}
```

After fixing, re-run:

```
cd projectzomboid/
python pz-lua-api-viewer/extract_lua_api.py
```

Verify the output: `groups` set should contain multiple distinct non-empty names.

### Step 3 — Fix `globals.js` if the group field exists but is not rendered

If Step 1 shows groups ARE in the JSON but the UI still doesn't group them, inspect `updateGlobalsTable()` in `globals.js`. Confirm:
- The sort order of `fns` groups functions by their `group` value (currently unsorted — a sort step may be needed so all entries with the same group are adjacent).

If entries are not sorted by group, add a sort:

```js
const fns = API.global_functions
  .map((g, i) => ({g, i}))
  .filter(({g}) => !s || ...)
  .sort((a, b) => (a.g.group || '').localeCompare(b.g.group || ''));
```

### Step 4 — Run `prepare_sources.py` only if source files changed

This task does not change `.java` files in `sources/`, so `prepare_sources.py` is not needed unless the extractor change also touches `_source_index`.

---
id: T01
parent: S01
milestone: M001
provides:
  - Build-time precomputed class maps in lua_api.json (_class_by_simple_name, _source_only_paths)
  - Fast-path init() in app.js using precomputed data with backwards-compatible fallback
  - Fixed SRC_ROOT in extract_lua_api.py to point to sources/ subdir (was broken)
key_files:
  - pz-lua-api-viewer/extract_lua_api.py
  - extract_lua_api.py
  - pz-lua-api-viewer/js/app.js
  - pz-lua-api-viewer/lua_api.json
key_decisions:
  - Since the extractor could not run (sources were in sources/ not zombie/ at root), patched the JSON in-place with a one-off Python script, then also fixed the extractor SRC_ROOT so future runs work correctly
  - SRC_ROOT split into _PROJ_ROOT (projectzomboid/) and SRC_ROOT (projectzomboid/sources/) in both extractor copies
patterns_established:
  - Precomputed lookup maps in JSON: compute at build time, consume directly at runtime with fallback
observability_surfaces:
  - browser console: API._class_by_simple_name entries count visible via browser_evaluate
duration: ~25 minutes
verification_result: passed
completed_at: 2026-03-13
blocker_discovered: false
---

# T01: Build-time precomputation (TASK-017)

**Added `_class_by_simple_name` and `_source_only_paths` to `lua_api.json`; `init()` now uses a fast O(1) assign instead of iterating all 1096 FQNs on every page load.**

## What Happened

Implemented TASK-017 per the task plan:

1. **Extractor fix (blocker):** Both copies of `extract_lua_api.py` had `SRC_ROOT` pointing to the projectzomboid root, but the actual Java sources live under `sources/`. `LuaManager.java` was not found at `zombie/Lua/LuaManager.java` relative to root. Fixed by splitting into `_PROJ_ROOT` and `SRC_ROOT = _PROJ_ROOT / "sources"` in both extractor copies.

2. **Precomputed fields in extractor (Step 6):** Added computation of `_class_by_simple_name` (simple name → [fqn, ...]) and `_source_only_paths` (source-only entries not in the API) before JSON assembly in both `pz-lua-api-viewer/extract_lua_api.py` and `extract_lua_api.py`.

3. **Fast-path in `init()` (`js/app.js`):** Replaced the O(n) loop with `Object.assign(classBySimpleName, API._class_by_simple_name)` when the precomputed field is present, with the original loop as fallback for older JSON.

4. **JSON regeneration:** Since the extractor was broken at the time of initial attempt, patched the existing `lua_api.json` in-place via a one-off Python script (reading existing class+source_index data and computing the maps). Then ran the fixed extractor end-to-end to regenerate a fresh JSON with all fields.

## Verification

- Extractor ran end-to-end: `python pz-lua-api-viewer/extract_lua_api.py` — exit code 0, "Done." printed, 1096 classes, 745 globals, 2105 source-only entries.
- Slice check: `python -c "import json; d=json.load(open('pz-lua-api-viewer/lua_api.json')); print('ok' if '_class_by_simple_name' in d else 'MISSING')"` → `ok`
- Browser: navigated to http://localhost:8765, confirmed 1096 classes listed, clicked SleepingEvent, detail panel opened with methods/fields.
- JS fast-path confirmed: `API._class_by_simple_name` has 936 entries (936 unique simple names covering 1096 FQNs).
- 4/4 browser_assert checks passed: url, class count, detail panel visible.

## Diagnostics

In browser console, run:
```js
Object.keys(API._class_by_simple_name).length  // 936
API._class_by_simple_name['IsoPlayer']          // ['zombie.characters.IsoPlayer']
Object.keys(API._source_only_paths).length      // 2105
```

## Deviations

- **SRC_ROOT fix** was not in the task plan but was required to make the extractor runnable. The existing `lua_api.json` had been generated with a working environment previously; the sources/ subdir structure was the actual layout.
- Both the `pz-lua-api-viewer/extract_lua_api.py` and root `extract_lua_api.py` were updated (the task plan only mentioned one, but both are identical copies that both needed the fix).

## Known Issues

None.

## Files Created/Modified

- `pz-lua-api-viewer/extract_lua_api.py` — Added `_PROJ_ROOT`/`SRC_ROOT` split fix + `_class_by_simple_name`/`_source_only_paths` computation in Step 6
- `extract_lua_api.py` — Same fixes (root-level copy)
- `pz-lua-api-viewer/js/app.js` — Fast-path `init()` using precomputed fields with fallback
- `pz-lua-api-viewer/lua_api.json` — Regenerated with new keys (936 simple-name entries, 2105 source-only paths)
- `docs/Tasks/TASK-017-build-time-precomputation.md` → archived to `docs/Archive/Tasks/`
- `docs/STATUS.md` — Marked TASK-017 done

> **COMPLETED 2026-03-12** — Added Step 2.5 to `extract_lua_api.py`. After the helper functions are defined, parses LuaManager.java with javalang, finds the `GlobalObject` inner class, builds a `{java_name → {return_type, params}}` map, and merges it into `global_functions`. Graceful fallback to `'?'`/`[]` if parse fails. Requires `lua_api.json` regen.

# TASK-005: Fix globals table showing `?` return types and `—` params

**Status:** Done
**Touches:** `extract_lua_api.py`
**Resolves:** `docs/Bugs/BUG-004-globals-missing-types.md`

## Acceptance Criteria

- [x] `return_type` and `params` fields added to all global function entries (after JSON regen)
- [x] Graceful fallback to `'?'` / `[]` if LuaManager.java fails to parse
- [ ] Full verification requires regenerating `lua_api.json` and checking the Globals tab

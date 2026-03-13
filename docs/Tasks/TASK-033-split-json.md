# TASK-033: Split lua_api.json into Index + Per-Class Detail

**Parent:** FEAT-015 (McMaster speed & clarity)
**Priority:** High — but blocked
**Blocked by:** TASK-024 (search index must handle new format)
**Blocks:** TASK-035 (service worker cache strategy depends on file structure)

## Problem

We load a 6MB JSON file on every page visit (~300KB gzipped, but still ~100ms parse time). The user only views 5-10 classes per session, but we load all 1,096 upfront.

## Plan

### 1. Generate split files from `extract_lua_api.py`

Output:
- `api/index.json` (~50KB) — contains:
  - `_meta`
  - `_extends_map`, `_interface_extends`, `_interface_paths`
  - `_class_by_simple_name`, `_source_only_paths`, `_source_index`
  - Per class: just `{ fqn, simple_name, set_exposed, lua_tagged, is_enum, method_count, field_count, has_tagged_methods, package }`
- `api/classes/{fqn}.json` — one file per class with full methods/fields/etc.
- `api/globals.json` — global functions (separate file since they're only needed on the Globals tab)

### 2. Update viewer to lazy-load

- `app.js`: Load `api/index.json` at startup (fast)
- `class-list.js`: Build sidebar from index data (no methods/fields needed for list)
- `class-detail.js`: On `selectClass()`, fetch `api/classes/{fqn}.json` if not cached
- `globals.js`: Fetch `api/globals.json` on tab switch
- Search index (TASK-024): Must build from index.json or from a separate search manifest

### 3. Keep `lua_api.json` as fallback

For backwards compatibility and local dev, keep generating the single file too. The viewer checks for `api/index.json` first, falls back to `lua_api.json`.

## Files

- Modified: `extract_lua_api.py` (generate split files)
- Modified: `js/app.js` (lazy loading)
- Modified: `js/class-list.js` (work from index)
- Modified: `js/class-detail.js` (fetch on demand)
- Modified: `js/globals.js` (fetch on demand)
- New: `api/` directory structure

## Acceptance Criteria

- [ ] Initial page load fetches only index.json (~50KB)
- [ ] Class detail loads on demand with no perceptible delay (< 100ms)
- [ ] Search still works (from index or separate manifest)
- [ ] Globals tab loads its own data
- [ ] Falls back to single lua_api.json if api/ directory doesn't exist
- [ ] `extract_lua_api.py` generates both formats

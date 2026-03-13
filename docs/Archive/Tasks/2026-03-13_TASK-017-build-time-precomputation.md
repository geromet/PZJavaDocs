> **COMPLETED 2026-03-13** — Added `_class_by_simple_name` and `_source_only_paths` to `extract_lua_api.py` output. Updated `init()` in `app.js` to use fast path with fallback. Fixed extractor `SRC_ROOT` to `sources/` subdir. JSON regenerated and verified.

# TASK-017: Build-time precomputation for faster startup

**Status:** Done
**Estimated scope:** Medium
**Touches:** `extract_lua_api.py` (or new `build.py`), `js/app.js`, `js/state.js`
**Resolves:** [FEAT-008](../Planned_Features/FEAT-008-build-time-precomputation.md)

## Context

At every page load, `init()` builds `classBySimpleName` (a reverse-map over all 1096 class FQNs) and resolves `sourceOnlyPaths` from `_source_index`. This is fast on desktop but measurable on mobile. Moving it to build time eliminates the work entirely.

Profile first: if startup is already < 50 ms with the prebuilt map, the gain may not be worth the complexity. If fast enough, consider skipping this task.

## Acceptance Criteria

- [ ] `lua_api.json` (or a sidecar `lua_api_prebuilt.json`) includes `_class_by_simple_name: {simple: [fqn, ...]}` and `_source_only_paths: {simple: path}` precomputed fields.
- [ ] `init()` in `app.js` uses the precomputed fields directly when present, skipping the `Object.keys()` loop.
- [ ] Falls back to computing on the fly if the fields are absent (backwards compatibility).
- [ ] Existing behavior is unchanged — same links, same class list, same everything.

## Implementation Plan

### Step 1 — Add precomputed fields to extractor output (`extract_lua_api.py`)

In step 6 (JSON assembly), compute and add two new top-level keys:

```python
# Build _class_by_simple_name
class_by_simple_name = {}
for fqn in classes_output:
    simple = fqn.split('.')[-1]
    class_by_simple_name.setdefault(simple, []).append(fqn)

# Build _source_only_paths (source index entries whose simple name is NOT in the API)
source_only_paths = {
    simple: path
    for simple, path in source_index.items()
    if simple not in class_by_simple_name
}

output['_class_by_simple_name'] = class_by_simple_name
output['_source_only_paths']    = source_only_paths
```

### Step 2 — Use precomputed fields in `init()` (`js/app.js`)

Replace the existing build loops:

```js
// Before (slow path):
for (const fqn of Object.keys(API.classes)) {
  const simple = fqn.split('.').pop();
  (classBySimpleName[simple] = classBySimpleName[simple] || []).push(fqn);
}
for (const [simple, path] of Object.entries(API._source_index || {})) {
  if (!classBySimpleName[simple]) sourceOnlyPaths[simple] = path;
}

// After (fast path with fallback):
if (API._class_by_simple_name) {
  Object.assign(classBySimpleName, API._class_by_simple_name);
  Object.assign(sourceOnlyPaths,   API._source_only_paths || {});
} else {
  // fallback — original loop
  for (const fqn of Object.keys(API.classes)) {
    const simple = fqn.split('.').pop();
    (classBySimpleName[simple] = classBySimpleName[simple] || []).push(fqn);
  }
  for (const [simple, path] of Object.entries(API._source_index || {})) {
    if (!classBySimpleName[simple]) sourceOnlyPaths[simple] = path;
  }
}
```

### Step 3 — Regenerate JSON

```
cd projectzomboid/
python pz-lua-api-viewer/extract_lua_api.py
```

## Notes

- `Object.assign` into the pre-declared empty objects is fine since `classBySimpleName` and `sourceOnlyPaths` are declared as `{}` in `state.js`.
- JSON size increase is small — `_class_by_simple_name` is essentially the inverse index already implicit in the class keys, ~50 KB uncompressed.
- If startup profiling shows this is already negligible, skip this task and archive FEAT-008.

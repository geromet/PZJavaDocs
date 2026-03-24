> **COMPLETED 2026-03-13** — Build-time precomputation of class lookups in JSON; fast-path init(). Shipped via TASK-017.

# FEAT-008: Build-time precomputation for faster startup

**Status:** Planned
**Priority:** Low
**Complexity:** Medium

## Description

Move expensive startup work from the browser to the build step (`extract_lua_api.py` or a new `build.py`). Currently the browser computes `classBySimpleName`, sorts class lists, and resolves inheritance chains every page load.

## What to Precompute

- **`classBySimpleName`** — simple name → [FQN, …] lookup. Already implicit in JSON key structure, but a pre-built reverse map avoids the `Object.keys()` + `split('.').pop()` loop.
- **Subclass list** — already computed in the extractor, but could include transitive subclasses.
- **Full implements list (transitive)** — walk `_interface_extends` and cache per-class result so `buildImplGroups()` is just a lookup.
- **Sorted method/field lists** — pre-sort by name so the frontend just renders.

## Implementation

Add an optional `python build.py` step that reads `lua_api.json` and writes `lua_api_prebuilt.json` with the additional precomputed fields. The viewer detects which format is available and uses it.

## Notes

- Current startup is fast enough for now (< 200ms on modern hardware). This becomes worthwhile if the JSON grows significantly or if mobile support is desired.
- Do not over-engineer: precompute only what profiling shows is slow.

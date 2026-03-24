> **COMPLETED 2026-03-12** — Added `get_nested_types()` to extractor and wired `nested_types` into every class entry. Added `buildNestedGroups()` and `renderNestedGroups()` to `js/class-detail.js`, wired after `renderImplGroups` in `renderInheritHeader`. Added `.tag-class` and `.tag-interface` badge styles to `app.css`. Requires `lua_api.json` regen to populate `nested_types` data.

# TASK-003: Add nested classes section to the detail panel

**Status:** Done
**Estimated scope:** Medium
**Touches:** `extract_lua_api.py`, `js/class-detail.js`, `app.css`
**Implements:** `docs/Planned_Features/FEAT-001-nested-classes-display.md`

## Acceptance Criteria

- [x] Detail panel shows a "Nested Classes" section when the class has any nested types
- [x] Groups: "Declared in ClassName" + "Inherited from X"
- [x] Each nested type shows name + kind badge (class / interface / enum)
- [x] Each item links via ifaceLink (API link / source link / plain span)
- [x] Section absent when no nested types in chain
- [x] Inherited nested types deduplicated
- [ ] Full verification requires regenerating lua_api.json and testing in browser

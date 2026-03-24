> **COMPLETED 2026-03-12** — Added `_interface_paths: {fqn → path}` to the extractor output (new block after step 4.7). Updated `ifaceLink()` in `js/class-detail.js` to check `API._interface_paths?.[fqn]` before falling back to `API._source_index?.[simple]`. This cleanly handles simple-name collisions between interface names and API class names. **Requires `lua_api.json` to be regenerated** before the fix is visible in the browser.

# TASK-002: Fix missing links for implemented interfaces (direct and transitive)

**Status:** Done
**Estimated scope:** Small
**Touches:** `extract_lua_api.py`, `js/class-detail.js`
**Resolves:** `docs/Bugs/BUG-002-transitive-interface-links-not-shown.md`

## Acceptance Criteria

- [x] Direct interfaces for `IsoGameCharacter` are clickable links (after JSON regen)
- [x] Transitive interfaces in sub-rows are clickable links (after JSON regen)
- [x] Any interface with no source file still renders as a plain span (graceful fallback)
- [ ] Full verification requires regenerating `lua_api.json` and testing in browser

> **FIXED 2026-03-12** — `_interface_paths` map added to extractor; `ifaceLink()` updated to use FQN-keyed lookup. See TASK-002 archive entry. Requires `lua_api.json` regen.

# BUG-002: Implemented interfaces not linked (direct and transitive)

**Status:** Fixed (pending JSON regen)
**Severity:** Medium
**Area:** Frontend — `js/class-detail.js` (`ifaceLink`, `renderImplGroups`)

## Root Cause

`ifaceLink(fqn)` checked `API._source_index?.[simple]` (simple name key). `_source_index` excludes any simple name that collides with an API class name. Interface FQNs whose simple name matches an API class were therefore silently dropped to plain text. Fixed with a separate `_interface_paths: {fqn → path}` map that uses FQN as key, eliminating all collision issues.

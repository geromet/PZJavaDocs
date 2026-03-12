# BUG-002: Transitive implemented interfaces not linked

**Status:** Open
**Severity:** Medium
**Area:** Frontend — `js/class-detail.js` (`ifaceLink`, `renderImplGroups`)

## Description

In the "All Implemented Interfaces" section, interfaces that come from the transitive chain (via superclass or via interface extends) appear as plain unlinked text instead of clickable source links.

## Example

For `IsoGameCharacter`, the "direct" group shows `ILuaGameCharacter` (linked), but the sub-row "via ILuaGameCharacter: ILuaGameCharacterAttachedItems, ..." shows those sub-interfaces as plain text.

The user report: *"We want links to the implemented interfaces. Currently we don't show them for non direct ones."*

## Expected

Every interface in every group — direct, via-chain, and via-interface-extends — should render as a clickable source link if a `.java` file exists for it, or as an API link if it is in `API.classes`.

## Root Cause (hypothesis)

`ifaceLink(fqn)` checks `API._source_index?.[simple]`. The `_source_index` only contains classes whose **simple name is not already in `api_simple_names`** (the set of API class simple names). If an interface's simple name collides with an API class simple name, it is excluded from `_source_index` and `ifaceLink` falls through to the plain span.

Alternatively, the interface `.java` file may not have been included in `_source_index` because the interface was discovered during step 4.7 (after `_source_index` is built in step 5), so its source path was never recorded.

## Fix Direction

1. Check in `extract_lua_api.py`: does `_source_index` contain the transitive interface simple names (e.g., `ILuaGameCharacterAttachedItems`, `IGrappleable`, `IAnimationVariableSource`)?
2. If missing: extend `_source_index` to include all interfaces reachable in `_interface_extends` values.
3. If present: debug why `ifaceLink` is not finding them (possible simple-name collision or capitalisation issue).

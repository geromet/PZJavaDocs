> **FIXED 2026-03-12** — Option B implemented: added a "LuaManager" button to the Globals tab header (`index.html` + `js/globals.js`). Clicking it calls `showSourceByPath('zombie/Lua/LuaManager.java')`. No extractor change required.

# BUG-005: LuaManager not visible in the class list

**Status:** Fixed
**Severity:** Low
**Touches:** `extract_lua_api.py`

## Description

`zombie.Lua.LuaManager` does not appear in the class list, even though it is the source of all global Lua functions and is a heavily referenced class. Users looking for global function source code have no direct class entry to navigate to.

## Steps to Reproduce

1. Search for "LuaManager" in the class search box.
2. No result appears.

## Root Cause

`LuaManager` is not `setExposed` and is not tagged `@UsedFromLua` directly, so it never enters `all_classes` in the extractor. It only appears as a source path (`zombie/Lua/LuaManager.java`) referenced by the globals panel.

## Fix Options

**Option A:** Add `LuaManager` to `all_classes` in the extractor as a special synthetic entry. Risks cluttering the class list with internal classes.

**Option B (chosen):** Add a visible link to LuaManager's source from the Globals tab header. Surgical, no extractor change.

**Option C:** Include any class in `_source_index` in the class list under a new "Source only" filter category. Most general but requires UI filter work.

> **FIXED 2026-03-12** — See TASK-005 archive entry. Requires `lua_api.json` regen.

# BUG-004: Globals table showing `?` return types and `—` params

**Root Cause:** Step 1 of the extractor used regex to extract global function names from `@LuaMethod` annotations but never parsed the method signatures (return type, parameters). The `global_functions` list entries had only `lua_name` and `java_method` fields. Fixed by adding Step 2.5 which parses LuaManager.java's `GlobalObject` class with javalang.

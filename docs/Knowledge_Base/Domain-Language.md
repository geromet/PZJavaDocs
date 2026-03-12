# Domain Language

Glossary of terms used across the project. Use these names consistently in code, comments, and documents.

## Project Zomboid Exposure Systems

| Term | Definition |
|------|-----------|
| **setExposed** | `LuaManager.setExposed(Foo.class)` — registers a Java class so all its public non-static methods are callable from Kahlua (PZ's Lua runtime). The real runtime gate. |
| **@UsedFromLua** | Annotation on a class or method marking developer intent for Lua use. Documentation only — no runtime effect. |
| **lua_tagged** | Our internal field name for `@UsedFromLua`. |
| **set_exposed** | Our internal field name for the `setExposed()` registration. |
| **callable** | A method reachable from Lua: either `lua_tagged=true`, or (`set_exposed=true` and `static=false`). |
| **global function** | A method annotated `@LuaMethod(global=true)` on `GlobalObject` inner class in `LuaManager.java`. Exposed as a bare Lua function. |
| **Kahlua** | The Lua runtime embedded in Project Zomboid. |

## Data Model Terms

| Term | Definition |
|------|-----------|
| **FQN** | Fully Qualified Name — `zombie.characters.IsoPlayer`. Used as the primary key for classes. |
| **simple name** | The last segment of a FQN — `IsoPlayer`. |
| **API class** | A class present in `all_classes` / `API.classes` — either setExposed or @UsedFromLua. |
| **source-only class** | A class with a `.java` source file but not in the API. Listed in `_source_index`. |
| **non-API intermediate** | A class in the inheritance chain between two API classes but not itself in the API (e.g. `IsoLivingCharacter`). Tracked in `_extends_map`. |
| **_extends_map** | JSON map: non-API class FQN → parent class FQN. Fills inheritance chain gaps. |
| **_interface_extends** | JSON map: interface FQN → [parent interface FQNs]. Used for transitive implements display. |
| **_source_index** | JSON map: simple name → relative `.java` path for source-only classes. |

## Frontend Terms

| Term | Definition |
|------|-----------|
| **currentClass** | FQN of the class currently displayed in the detail panel. |
| **currentCtab** | Active content sub-tab: `'detail'` or `'source'`. |
| **currentTab** | Active main tab: `'classes'` or `'globals'`. |
| **navHistory** | Array of nav state objects. |
| **navIndex** | Pointer into `navHistory` for back/forward. |
| **_restoringState** | Flag set during `applyState()` to suppress `navPush` calls. |
| **filteredResults** | Current flat list of `{fqn, cls, score, matchInfo}` from the class list. |
| **inherit-wrap** | DOM element holding the inherited methods section below the main methods list. |

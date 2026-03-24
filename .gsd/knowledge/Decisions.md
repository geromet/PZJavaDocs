# Architectural Decisions

Record of significant decisions made during development. Captures the "why" so we don't relitigate closed questions.

---

## ADR-001: Navigation uses _restoringState flag, not noPush parameters

**Date:** 2026-03
**Status:** Accepted

**Context:** Navigation-aware functions (`selectClass`, `switchTab`, `showGlobalSource`) needed to behave differently when called from `applyState` (no history write) vs. user interaction (write history). Originally implemented with `noPush` boolean params threaded through call chains.

**Decision:** Single module-level `_restoringState` flag in `state.js`. `navPush` checks it and is a no-op when set. `applyState` sets/clears it in a `try/finally` block. All other call sites push unconditionally.

**Consequences:** Adding new navigation actions requires zero thought about the flag — they just call `navPush`. `noPush` params no longer exist in any function signature.

---

## ADR-002: Inheritance gaps filled by _extends_map, not stub entries in all_classes

**Date:** 2026-03
**Status:** Accepted

**Context:** Some classes in the inheritance chain (e.g. `IsoLivingCharacter`) are neither setExposed nor @UsedFromLua, so they have no entry in `all_classes`. This breaks chain rendering.

**Decision:** Separate `_extends_map` (non-API FQN → parent FQN) built by BFS in step 4.6. Frontend uses `API._extends_map?.[cur]` as fallback when walking chains. Keeps `all_classes` clean — only real API classes.

**Rejected alternative:** Add stub entries to `all_classes` with a `stub: true` flag. Would pollute the class list and require filtering everywhere.

---

## ADR-003: Interface extends tracked separately from class extends

**Date:** 2026-03
**Status:** Accepted

**Context:** Java interfaces can extend multiple interfaces. JavaDocs lists all transitively implemented interfaces; we initially only showed direct `implements` clauses.

**Decision:** `_interface_extends` map (interface FQN → [parent interface FQNs]) built by BFS in step 4.7, seeded from all interfaces appearing in class `implements` lists. Frontend walks this to build grouped implements display.

**Why separate from _extends_map:** `_extends_map` is 1:1 (single parent per class). Interface extends is 1:many. Different data shape, different traversal semantics.

---

## ADR-004: Implements displayed grouped by inheritance chain, not flat

**Date:** 2026-03
**Status:** Accepted

**Context:** JavaDocs lists all implemented interfaces as one long flat list. For classes like `IsoGameCharacter` this is 20+ items with no indication of where each came from.

**Decision:** Group by class in the chain ("direct", "via IsoMovingObject", "via IsoObject"). Within each group, show sub-rows for interfaces that came through an interface's own extends chain ("via ILuaGameCharacter: ILuaGameCharacterAttachedItems, ..."). Deduplicate across all groups.

---

## ADR-005: Pre-shipped sources in sources/ are legal per ToS 2.1

**Date:** 2026-03
**Status:** Accepted

**Context:** GitHub Pages needs source files available without a local PZ install.

**Decision:** Ship ~3889 `.java` source files in `sources/`. The Indie Stone ToS section 2.1 explicitly permits distributing base game files for non-commercial purposes that promote PZ.

---

## ADR-006: No direct pushes to main branch

**Date:** 2026-03
**Status:** Accepted

**Context:** `main` is the GitHub Pages deployment branch. Pushing triggers a rebuild and immediately affects the live site.

**Decision:** All development happens on feature branches (currently `liability-machine`). Claude must never push directly to `main`.

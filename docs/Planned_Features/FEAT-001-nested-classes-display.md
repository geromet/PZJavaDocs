# FEAT-001: Nested classes display in the detail panel

**Priority:** Medium
**Area:** Extractor + Frontend (`extract_lua_api.py`, `js/class-detail.js`, `app.css`)
**Depends on:** Inheritance chain data (already available)

## Goal

Show nested classes and interfaces declared inside a class, and nested classes inherited from ancestors, in the detail panel — grouped by inheritance chain.

## Design

Add a **Nested Classes** section to the class detail panel, below Fields, containing:

### Group 1 — Declared in `ClassName`
All nested `class` / `interface` / `enum` declarations directly inside the class body.

```
▼ Declared in IsoGameCharacter
  ILuaGameCharacter (interface)   IsoGameCharacterType (enum)
```

### Group 2+ — Inherited from ancestors
Nested classes/interfaces introduced by each ancestor, grouped by that ancestor (same dedup-by-seen pattern as implemented interfaces).

```
▼ From IsoMovingObject
  ...

▼ From IsoObject
  ...
```

Each item links to the nested class if it's in `API.classes`, or to its source file via `_source_index`, or renders as a plain span.

## Extractor Changes

In step 3/4 (class parsing), when building a class entry, also capture **top-level nested type declarations**:

```python
def get_nested_types(cls_node):
    nested = []
    for member in (cls_node.body or []):
        if isinstance(member, (javalang.tree.ClassDeclaration,
                               javalang.tree.InterfaceDeclaration,
                               javalang.tree.EnumDeclaration)):
            nested.append({
                "name": member.name,
                "kind": "interface" if isinstance(member, javalang.tree.InterfaceDeclaration)
                        else "enum" if isinstance(member, javalang.tree.EnumDeclaration)
                        else "class",
                "fqn": parent_fqn + "." + member.name,
            })
    return nested
```

Add `"nested_types": [...]` to each class entry (empty list if none).

## Frontend Changes

- New `buildNestedGroups(cls, fqn)` — mirrors `buildImplGroups`: walks extends chain, deduplicates, groups by source class.
- New `renderNestedGroups(groups)` — renders each group as a collapsible section.
- Each nested item rendered via `ifaceLink`-style helper (API link → source link → plain span), with a small badge showing `class` / `interface` / `enum`.
- Section only rendered if any groups are non-empty.

## Open Questions

- Should nested classes that are themselves API classes also appear in the main class list? (Probably yes — they already do if setExposed, since FQN is derived from path.)
- Depth limit? Only show top-level nested types, not nested-nested.

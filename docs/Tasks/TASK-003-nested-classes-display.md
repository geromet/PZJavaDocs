# TASK-003: Add nested classes section to the detail panel

**Status:** Ready
**Estimated scope:** Medium
**Touches:** `extract_lua_api.py`, `js/class-detail.js`, `app.css`
**Implements:** `docs/Planned_Features/FEAT-001-nested-classes-display.md`

## Context

Java classes can contain nested class/interface/enum declarations. These are not currently shown anywhere in the detail panel. Users browsing `IsoGameCharacter` have no way to discover e.g. `IsoGameCharacter.IsoGameCharacterType` without reading the raw source.

## Acceptance Criteria

- [ ] Detail panel shows a "Nested Classes" section when the class has any nested types
- [ ] Groups: "Declared in ClassName" + "Inherited from X" (one group per ancestor that introduces new nested types)
- [ ] Each nested type shows name + kind badge (`class` / `interface` / `enum`)
- [ ] Each item is a clickable link: API link if in `API.classes`, source link if in `_source_index`, plain span otherwise
- [ ] Section is absent when there are no nested types anywhere in the chain
- [ ] Inherited nested types are deduplicated (same `seen` set pattern as `buildImplGroups`)

## Implementation Plan

### Step 1 — Extractor: capture nested types per class

In `build_class_entry()` (or inline in steps 3/4), after building methods/fields, add:

```python
def get_nested_types(cls_node, parent_fqn):
    nested = []
    for member in (cls_node.body or []):
        if isinstance(member, (javalang.tree.ClassDeclaration,
                               javalang.tree.InterfaceDeclaration,
                               javalang.tree.EnumDeclaration)):
            kind = ("interface" if isinstance(member, javalang.tree.InterfaceDeclaration)
                    else "enum" if isinstance(member, javalang.tree.EnumDeclaration)
                    else "class")
            nested.append({
                "name": member.name,
                "kind": kind,
                "fqn":  parent_fqn + "." + member.name,
            })
    return nested
```

Add to each class entry: `"nested_types": get_nested_types(cls, fqn)` (empty list if none).

For `EnumDeclaration`, `cls.body` is an `EnumBody`; access `cls.body.declarations` for nested members instead of `cls.body` directly.

### Step 2 — Frontend: buildNestedGroups

Mirror `buildImplGroups` exactly but using `nested_types` instead of `implements`:

```javascript
function buildNestedGroups(cls, fqn) {
  const seen   = new Set();
  const groups = [];
  let curFqn   = fqn;
  let curCls   = cls;
  let isDirect = true;
  const clsVisited = new Set();

  while (curFqn && !clsVisited.has(curFqn)) {
    clsVisited.add(curFqn);
    const types = (curCls?.nested_types || []).filter(t => !seen.has(t.fqn));
    types.forEach(t => seen.add(t.fqn));
    if (types.length)
      groups.push({ fromFqn: curFqn, isDirect, types });
    curFqn  = curCls?.extends || API._extends_map?.[curFqn];
    curCls  = API.classes[curFqn];
    isDirect = false;
  }
  return groups;
}
```

### Step 3 — Frontend: renderNestedGroups

```javascript
function renderNestedGroups(groups) {
  if (!groups.length) return '';
  const rows = groups.map(g => {
    const label = g.isDirect
      ? `Declared in ${esc(g.fromFqn.split('.').pop())}`
      : `Inherited from ${esc(g.fromFqn.split('.').pop())}`;
    const items = g.types.map(t => {
      const badge = `<span class="tag tag-${t.kind}">${t.kind}</span>`;
      return ifaceLink(t.fqn) + badge;  // ifaceLink already handles API/source/plain
    }).join(' &nbsp; ');
    return `<div class="impl-group">
      <span class="impl-group-header">${label}:</span>
      <span class="impl-items">${items}</span>
    </div>`;
  }).join('');

  return `<div class="inherit-meta impl-section">
    <span class="inherit-label">Nested Classes:</span>${rows}
  </div>`;
}
```

### Step 4 — Wire into renderInheritHeader

After `renderImplGroups(implGroups)`:

```javascript
const nestedGroups = buildNestedGroups(cls, fqn);
html += renderNestedGroups(nestedGroups);
```

Update the early-exit guard to include nested groups.

### Step 5 — CSS

Add `tag-interface` and `tag-enum` badge styles if not present. Reuse existing `.tag` and `.tag-enum` classes; add `.tag-interface` and `.tag-class` to match.

### Step 6 — Rebuild JSON and verify

Run extractor, copy JSON, test in browser with:
- `IsoGameCharacter` (likely has nested types)
- `LuaManager` (has `GlobalObject` inner class)
- A class with no nested types (section should be absent)

## Notes

- `ifaceLink` already handles all three link states — reuse it as-is for nested type items.
- The `fqn` for a nested class is `OuterClass.InnerClass` which follows the same dot-separated convention. `fqn_to_path` already handles inner classes via the fallback search loop.

# Task 03 — Inheritance Display

## Goal
Show class inheritance information in the detail panel, loosely following Oracle Javadoc style.

**Prerequisite:** Task 01 (source coverage) should be done first — a broader class map means
more inheritance links can be resolved.

---

## Data to Extract (`extract_lua_api.py`)

Add three new optional fields to each class entry in `lua_api.json`:

```json
{
  "extends": "zombie.iso.IsoMovingObject",
  "implements": ["zombie.interfaces.IHaveItem", "zombie.interfaces.IDrainable"],
  "subclasses": ["zombie.characters.IsoPlayer", "zombie.characters.IsoZombie"]
}
```

### Extraction approach

**`extends` and `implements`** — parse the class declaration line from raw source:
```
public class IsoGameCharacter extends IsoMovingObject implements IHaveItem, IDrainable {
```

Regex on the raw `.java` text (more robust than javalang AST for our decompiled sources):
```python
DECL_RE = re.compile(
    r'\bclass\s+\w+\s*'
    r'(?:<[^>]*>)?\s*'                          # optional generics
    r'(?:extends\s+([\w.<>, ]+?))?\s*'          # optional extends
    r'(?:implements\s+([\w.<>, ]+?))?\s*\{'     # optional implements
)
```

- Extract simple names, resolve via `import_map` + `build_global_simple_map` fallback (from task 01)
- Include unresolved names as-is (e.g. `java.lang.Enum`) — still useful for display
- Omit `extends` if resolved FQN is `java.lang.Object`

**`subclasses`** — build as inverse pass after all classes processed:
```python
for fqn, cls in all_classes.items():
    parent = cls.get('extends')
    if parent and parent in all_classes:
        all_classes[parent].setdefault('subclasses', []).append(fqn)
```
Sort `subclasses` list alphabetically.

---

## Detail Panel Display (`js/class-detail.js`)

### Inheritance header block (top of detail panel, before fields/methods)

Follow Javadoc layout:

```
┌─────────────────────────────────────────────────┐
│ java.lang.Object                                │
│   └─ zombie.iso.IsoObject                       │
│        └─ zombie.iso.IsoMovingObject            │
│             └─ zombie.iso.IsoGameCharacter  ◀   │
│                                                 │
│ All Implemented Interfaces:                     │
│   IHaveItem, IDrainable, IClothingItem          │
│                                                 │
│ Direct Known Subclasses:                        │
│   IsoPlayer, IsoZombie                          │
└─────────────────────────────────────────────────┘
```

- Inheritance chain: walk `extends` upward until no parent or parent not in API.
  Render as a stacked tree with `└─` prefix and indentation.
  Current class is **bold** or marked with a distinct style.
- Implemented interfaces: comma-separated. Each name is a link if resolvable in `API.classes`.
- Direct known subclasses: comma-separated links, same style.
- Omit entire block if class has no `extends`, no `implements`, no `subclasses`.

### Inherited methods section (below own methods)

Walk the `extends` chain. For each ancestor in `API.classes`:
- Collect ancestor's methods that are NOT overridden in the current class.
  Override check: `child.methods.some(m => m.name === ancestorMethod.name)` (name-only, no sig).
- Render as a `.method-group` with label:
  ```
  ▼ Methods inherited from class zombie.iso.IsoMovingObject
  ```
  Where the class name is a clickable link (`selectClass(fqn)`).
- Group body: compact comma-separated method names, each a link that navigates to the
  ancestor class and scrolls to the method in source.
- Stop walking chain at the first ancestor NOT in `API.classes` (no data to show).
- Respect `methodSearch`: if methodSearch is active, filter the comma list and hide groups
  with zero matches.
- Non-callable inherited methods: respect `showNonCallable` / `hide-noncallable` toggle.
  A method is callable if: `m.lua_tagged || (!m.static && ancestor.set_exposed)`.

---

## UI / CSS (`app.css`)

```css
/* Inheritance header */
.inherit-header { margin-bottom: 12px; font-size: 12px; }
.inherit-tree   { font-family: monospace; line-height: 1.6; margin-bottom: 6px; }
.inherit-tree-current { font-weight: bold; color: var(--text); }
.inherit-tree-item    { color: var(--text-dim); }
.inherit-meta   { margin-top: 4px; }
.inherit-label  { color: var(--text-dim); font-size: 11px; margin-right: 4px; }
.inherit-link   { color: var(--accent2); text-decoration: none; cursor: pointer; }
.inherit-link:hover { text-decoration: underline; }

/* Inherited method groups — reuse .method-group, add modifier */
.method-group.inherited .group-label { color: var(--text-dim); font-style: italic; }
.inherited-methods-list { padding: 4px 8px; font-size: 12px; line-height: 1.8; }
.inherited-methods-list a { color: var(--accent2); text-decoration: none; margin-right: 8px; }
.inherited-methods-list a:hover { text-decoration: underline; }
```

---

## Filter Interaction

- **Callable filter**: keep as own-callables only (don't consider inherited). Inherited callables
  are a bonus, not a filter signal.
- **Callable toggle button**: inherited callable methods count toward "X non-callable hidden"
  message. Adjust `refreshMethods` accordingly.
- **Search**: `methodSearch` applies to inherited method names in their group's compact list.

---

## Key Test Cases

| Class | Expected |
|-------|----------|
| `zombie.iso.IsoZombie` | Deep chain (Object→IsoObject→IsoMovingObject→IsoGameCharacter→IsoZombie), many inherited groups |
| `zombie.iso.IsoPlayer` | IsoGameCharacter group should appear |
| `zombie.iso.IsoObject` | Has `subclasses` list (very long — test "N classes" truncation) |
| Any `enum` class | Shows `extends java.lang.Enum` (not in API, display only — no link) |
| Interface implementor | Shows `implements` line |
| A class with no inheritance info | Entire header block hidden |

---

## Implementation Steps

1. **`extract_lua_api.py`**: add `DECL_RE`, extract `extends`/`implements`, build `subclasses` post-pass, re-run
2. **`js/class-detail.js`**: add `renderInheritHeader(cls)` → HTML string for the top block
3. **`js/class-detail.js`**: add `renderInheritedMethods(cls)` → walk chain, build groups
4. **Both**: integrate into `renderClassDetail` / `refreshMethods` call flow
5. **`app.css`**: add inheritance styles
6. **Test**: IsoZombie chain, enum, interface, base class with many subclasses

---

## Notes

- `subclasses` on `IsoObject` or `IsoMovingObject` will be a very long list. Consider:
  - Show all (scroll), or
  - Show first 10 + "… and N more" toggle
- Inherited methods on `IsoGameCharacter` will be hundreds. The compact list format
  (comma-separated, no table rows) is essential to keep it usable.
- If `extends` points to a class not in `API.classes` (e.g. `java.lang.Enum`), show the
  name in the tree but don't make it a link. Continue chain walking stops there.

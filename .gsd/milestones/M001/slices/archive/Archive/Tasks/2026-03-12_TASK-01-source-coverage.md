# Task 01 — Source Coverage & Deep Call Linking

## Problem A: Missing Classes in Source Linking

Classes like `GameClient`, `CoopMaster`, `GlobalObject` appear in LuaManager method bodies
but are not linked in the source viewer. They exist in the decompiled sources but are not
in `API.classes`, so `classBySimpleName` doesn't know about them.

### Root Cause
`extract_lua_api.py` only adds a class to `all_classes` if it passes the exposure check
(`set_exposed` or `@UsedFromLua`). Classes that are *referenced by* LuaManager methods but
are not themselves exposed are invisible to the viewer.

`classBySimpleName` is built from `API.classes` only — so unlisted classes can't be linked
even if their source file exists in `sources/`.

### Fix: Separate "API classes" from "source-linkable classes"

**Option A (preferred) — add a `sources_only` index to the JSON:**
- After building `all_classes`, scan all `refs[]` arrays across every global function
- Collect FQNs that appear as refs but are not in `all_classes`
- Resolve them to source file paths (same logic as existing source_file assignment)
- Emit a second top-level key in `lua_api.json`:
  ```json
  "_source_index": {
    "GameClient":  "zombie/network/GameClient.java",
    "CoopMaster":  "zombie/network/CoopMaster.java",
    "GlobalObject": "zombie/Lua/LuaManager.java"
  }
  ```
  (simple_name → relative path, enough for `fetchSource` + navigation)
- Also include any class appearing in ANY method body ref scan (not just globals), to
  cover refs surfaced by `linkClassRefs` in the class source viewer

**Option B — broaden `all_classes` to include all referenced types:**
- Riskier: would balloon `lua_api.json` and pollute the class list
- Not preferred

**Frontend change:**
- In `init()` in `app.js`, merge `_source_index` into `classBySimpleName`:
  ```javascript
  for (const [simple, path] of Object.entries(API._source_index || {})) {
    if (!classBySimpleName[simple]) {
      classBySimpleName[simple] = [{fqn: simple, sourcePath: path, sourceOnly: true}];
    }
  }
  ```
- When a `src-class-ref` is clicked and the target has `sourceOnly: true`:
  - Don't navigate to a class detail page (it has no API entry)
  - Instead open a raw source panel for that file (reuse `showSource`-like logic)
  - Or simply omit the link if no useful destination exists — simplest safe fallback

**Simplest safe fallback (start here):**
- Just add the missing classes to `classBySimpleName` with their source path
- On click, if no `API.classes[fqn]` entry, open source panel directly via `fetchSource(path)`
  and show it in a modal or the existing source panel with a "source only" header

---

## Problem B: Import Resolution Gaps

LuaManager imports some classes via wildcard (`import zombie.network.*`) or they're in the
same package, so `import_map` doesn't contain them.

### Fix in `extract_lua_api.py`
After building `import_map` from explicit imports, do a second-pass fallback:
```python
# For each token [A-Z]\w+ found in a method body that isn't in import_map,
# search all_sources for a file named Token.java and resolve its package.
# Cache results in a fallback_map to avoid re-scanning.
```
This is relatively cheap if done once per token type (not per method).
Alternatively, pre-build a `simple_to_fqn` map from ALL `.java` files found under
`zombie/` before processing LuaManager — just parse the `package` declaration of each file.

**Recommended approach:**
```python
def build_global_simple_map(root_dir):
    """Scan all .java files, map simple class name → FQN from package + filename."""
    m = {}
    for path in glob.glob(f'{root_dir}/**/*.java', recursive=True):
        pkg_match = re.search(r'^package\s+([\w.]+);', open(path).read(200), re.M)
        if pkg_match:
            simple = os.path.splitext(os.path.basename(path))[0]
            m[simple] = pkg_match.group(1) + '.' + simple
    return m
```
Use this as a fallback when `import_map.get(name)` misses.

---

## Problem C: Deep Method Call Linking

Currently `ErosionMain` is linked in:
```java
ErosionMain.getInstance().snowCheck();
```
But `getInstance()` return type and `snowCheck()` are not linked.

### What We Want
Every identifier in a call chain that can be resolved should be clickable.
- `ErosionMain` → navigate to ErosionMain class ✓ (already works)
- `.getInstance()` → ideally navigate to ErosionMain, scroll to `getInstance` method
- `.snowCheck()` → navigate to whatever `getInstance()` returns (ErosionMain itself here),
  scroll to `snowCheck`

### Approach: Extraction-time method-to-class map
This requires knowing what type `getInstance()` returns — that needs type inference,
which is expensive. But we can handle the common pattern cheaply:

**Static factory pattern** (`Type.getInstance()` returning `Type`):
- During extraction, for each class, record static methods whose return type matches
  the class itself or a known subclass
- Emit a top-level `_method_index` in JSON: `"methodName" → [fqn, ...]`

**In the source viewer** (post-link pass):
After `linkClassRefs`, do a second pass on `.hljs-title.function_` spans (highlight.js
marks method names), check against `_method_index`, and wrap matches as method links.
This is inherently ambiguous (same method name on multiple classes) but acceptable for
navigation purposes — show the list of candidate classes if ambiguous.

### Scope Decision
- Phase 1 (this task): fix class linking (Problems A + B) — concrete and high value
- Phase 2 (later, maybe task 03 or separate): method-level linking — more complex,
  consider whether the ambiguity is acceptable before implementing

---

## Implementation Steps

### Step 1: Build global simple→FQN map in extractor
- Add `build_global_simple_map(root)` to `extract_lua_api.py`
- Use as fallback in `extract_class_refs` after `import_map` miss
- Re-run extractor, check that `GameClient`, `CoopMaster`, `GlobalObject` now appear in refs

### Step 2: Emit `_source_index` in JSON
- After all processing, collect all unique FQNs appearing in any `refs[]`
  that are NOT in `all_classes`
- Resolve each to a relative `.java` path using `build_global_simple_map` results
- Emit as `API._source_index = { SimpleName: "relative/path.java", ... }`

### Step 3: Frontend — merge `_source_index` into `classBySimpleName`
- In `init()` in `app.js`, after building `classBySimpleName` from API classes,
  iterate `API._source_index` and add any simple name not already present
- Store as `{ fqn: simpleName, sourcePath: path, sourceOnly: true }` sentinel

### Step 4: Frontend — handle `sourceOnly` clicks
- In the `src-class-ref` click handler in `app.js`:
  ```javascript
  const entry = classBySimpleName[simpleName]?.[0];
  if (entry?.sourceOnly) {
    // show raw source panel without class detail
    showSourceByPath(entry.sourcePath);
  } else {
    switchTab('classes'); selectClass(entry.fqn);
  }
  ```
- `showSourceByPath(path)` — thin wrapper around existing `showSource` infrastructure,
  shows source panel with a "(source only — not in Lua API)" note in header

### Step 5: prepare_sources.py — ensure referenced sources are copied
- Currently copies sources only for classes in `all_classes`
- Extend to also copy sources for all paths in `_source_index`

### Step 6: Re-run, test
- Open `canInviteFriends` global → `GameClient` and `CoopMaster` should now be linked
- Open `getFriendsList` global → `GlobalObject` should be linked
- Click a sourceOnly ref → source panel opens showing the file

---

## Files Changed
- `extract_lua_api.py` — Steps 1, 2
- `prepare_sources.py` — Step 5
- `js/app.js` — Steps 3, 4
- `js/source-viewer.js` — Step 4 (`showSourceByPath`)
- `app.css` — minor: "source only" header style if needed
- Re-generate `lua_api.json`, re-run `prepare_sources.py`

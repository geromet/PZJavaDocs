# TASK-002: Fix missing links for transitive implemented interfaces

**Status:** Ready
**Estimated scope:** Small
**Touches:** `extract_lua_api.py`, possibly `js/class-detail.js`
**Resolves:** `docs/Bugs/BUG-002-transitive-interface-links-not-shown.md`

## Context

The "All Implemented Interfaces" section groups interfaces by inheritance chain. Direct interfaces often link correctly, but transitive ones (from interface extends chains, or from ancestor classes) appear as plain unlinked text.

`ifaceLink(fqn)` checks `API._source_index?.[simple]`. If the simple name isn't in `_source_index`, it falls through to a plain span. The likely cause: `_source_index` excludes simple names that collide with API class simple names — but the interfaces discovered in step 4.7 (`_interface_extends` values) were never explicitly added.

## Acceptance Criteria

- [ ] All interfaces in the "via ILuaGameCharacter:" sub-row for `IsoGameCharacter` are clickable links
- [ ] All interfaces in "via IsoObject", "via IsoMovingObject" groups are clickable links
- [ ] Clicking any interface link opens its source file in the source panel
- [ ] Any interface with no source file still renders as a plain span (graceful fallback)

## Implementation Plan

### Step 1 — Diagnose in Python

Run a quick check to see which transitive interfaces are missing from `_source_index`:

```python
import json, pathlib
api = json.loads(pathlib.Path("lua_api.json").read_text())
si  = api['_source_index']
ie  = api['_interface_extends']

all_transitive = set()
for parents in ie.values():
    all_transitive.update(parents)

print("Transitive interfaces missing from _source_index:")
for fqn in sorted(all_transitive):
    simple = fqn.split('.')[-1]
    if simple not in si and fqn not in api['classes']:
        print(f"  {fqn}")
```

### Step 2 — Fix in extractor

In `extract_lua_api.py` step 5 (`build_source_index`), the `source_index` is filtered by:

```python
source_index = {simple: path for simple, path in all_source_files.items()
                if simple not in api_simple_names}
```

`api_simple_names` is `{v["simple_name"] for v in all_classes.values()}`.

If an interface's simple name collides with an API class simple name, it gets excluded.
Fix: after building `source_index`, add any missing interface FQNs from `_interface_extends`:

```python
for iface_fqn in _iface_visited:  # all interfaces seen in step 4.7
    simple = iface_fqn.rsplit('.', 1)[-1]
    if simple not in source_index and iface_fqn not in all_classes:
        # find its path
        java_file_s, _ = fqn_to_path(iface_fqn)
        if java_file_s and java_file_s.exists():
            rel = str(java_file_s.relative_to(SRC_ROOT)).replace("\\", "/")
            source_index[simple] = rel
```

### Step 3 — Rebuild and copy JSON

```
python pz-lua-api-viewer/extract_lua_api.py
cp lua_api.json pz-lua-api-viewer/lua_api.json
```

### Step 4 — Verify in browser

Open `IsoGameCharacter` and check the "All Implemented Interfaces" section. All sub-row interfaces should be underlined links.

## Notes

- If Step 1 shows no missing interfaces, the bug is in the frontend (`ifaceLink` logic) — check for a simple-name capitalisation mismatch or a different code path producing the unlinked spans.
- `_iface_visited` is available after step 4.7 in the extractor; use it as the definitive set of all known interface FQNs.

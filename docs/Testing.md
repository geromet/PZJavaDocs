# Testing

How to verify changes are correct. No automated test suite exists; these are manual verification procedures.

## Extractor changes

After running `extract_lua_api.py`, check the summary output against expected baselines:

```
Total classes:    ~1096
Total methods:    ~19099
setExposed:        ~917
@UsedFromLua only: ~179
Global functions:  745
Unresolved:         57
Parse errors:        5
```

Any significant change (±10%) warrants investigation before committing.

### Spot-checking inheritance data

```python
import json, pathlib
api = json.loads(pathlib.Path("lua_api.json").read_text())

# Check a known chain
cur = 'zombie.characters.IsoPlayer'
while cur:
    in_api = cur in api['classes']
    extends = api['classes'].get(cur, {}).get('extends') or api['_extends_map'].get(cur)
    print(f"  {cur.split('.')[-1]} ({'API' if in_api else 'map'}) -> {extends}")
    cur = extends
```

Expected: IsoPlayer → IsoLivingCharacter (map) → IsoGameCharacter (API) → IsoMovingObject (API) → IsoObject (API) → GameEntity (API)

### Spot-checking interface extends

```python
ie = api['_interface_extends']
# ILuaGameCharacter should extend 7 interfaces
assert len(ie.get('zombie.characters.ILuaGameCharacter', [])) == 7
```

## Frontend changes

1. Start the local server: `python serve.py` (opens http://localhost:8000)
2. Navigate to a class with known inheritance (e.g. `IsoGameCharacter`, `IsoPlayer`).
3. Verify:
   - Inheritance tree shows full chain to root
   - "All Implemented Interfaces" groups match expected grouping
   - Non-API classes (IsoLivingCharacter) show as source links, not plain text
   - Inherited methods section shows tables with return type and params
4. Test navigation:
   - Click a class → Back → should return to previous class
   - Click an inherited method link → Back → should return to the class you were on
   - Click Globals, click a function, click Back → should return to Globals table
   - Forward after Back should work symmetrically
5. Test search: type a method name → class list filters → click a result → method search pre-fills.

## Regression checks after nav refactor

The `_restoringState` flag must never be set during user-initiated navigation. Signs it's leaking:
- Back/Forward buttons stay disabled after user clicks
- Clicking a class doesn't update the URL hash
- Clicking Back navigates to the wrong place

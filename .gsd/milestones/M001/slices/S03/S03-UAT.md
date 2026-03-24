# S03: Version Selector — UAT

**Milestone:** M001
**Written:** 2026-03-24

## Preconditions
- App served locally (e.g., `python -m http.server` from project root)
- `versions/versions.json` exists with at least one entry
- `versions/lua_api_r964.json` exists

## Smoke Tests

### 1. Single version — dropdown hidden
1. Ensure `versions/versions.json` has exactly 1 entry.
2. Open the app in a browser (no `?v=` param).
3. **Expected:** App loads normally, class list renders, `#version-select` dropdown is not visible.

### 2. Multiple versions — dropdown visible
1. Edit `versions/versions.json` to have ≥2 entries (e.g., duplicate the r964 entry with id "r900").
2. Reload the app.
3. **Expected:** `#version-select` dropdown appears in the toolbar with both entries listed.

### 3. URL param selects version
1. With ≥2 entries in the manifest, navigate to `?v=r964`.
2. **Expected:** The dropdown shows "Build r964" selected. `document.querySelector('#version-select').dataset.versionActive` returns `"r964"`.

### 4. Version switch preserves hash
1. With ≥2 entries, open a class (e.g., click `IsoPlayer` so the hash is `#zombie.characters.IsoPlayer`).
2. Switch to the other version via the dropdown.
3. **Expected:** Page reloads with `?v=<newId>#zombie.characters.IsoPlayer` — the same class is open in the new version.

### 5. Missing versions.json — graceful fallback
1. Rename or delete `versions/versions.json`.
2. Reload the app (no `?v=` param).
3. **Expected:** App loads normally using root `lua_api.json`, dropdown is hidden, no JS console errors.

### 6. Unknown ?v= param — graceful degradation
1. With the manifest present, navigate to `?v=nonexistent`.
2. **Expected:** App loads using the first manifest entry (r964). Dropdown shows first entry selected. No errors.

## Edge Cases

### Manifest with empty array
1. Set `versions/versions.json` to `[]`.
2. Reload the app.
3. **Expected:** Dropdown hidden, app falls back to `lua_api.json`.

## Failure Signals
- JS console errors on load → version-aware loader has a bug
- Dropdown visible with only 1 version → `setupVersionDropdown()` guard is broken
- `data-version-active` attribute present when dropdown is hidden → attribute cleanup logic is broken
- Page shows blank content after version switch → reload URL construction is wrong

## Automated Coverage
All smoke tests except #4 (hash preservation) and #6 (unknown param) are covered by `pytest .gsd/test/s03_version_selector.py -v`. Those two are lightweight manual checks.

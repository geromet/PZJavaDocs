> **COMPLETED 2026-03-13** — Extractor detects build via SVNRevision.txt (r964); writes versions/lua_api_r964.json + versions/versions.json. Frontend: async version-aware loader, #version-select dropdown (hidden when ≤1 version), ?v= URL param preserved across navigation. Shipped in bf40c8f.

# TASK-018: Version selector and multi-version API storage

**Status:** Ready
**Estimated scope:** Large
**Touches:** `extract_lua_api.py`, `index.html`, `app.css`, `js/app.js`, `js/state.js`
**Resolves:** [FEAT-005](../Planned_Features/FEAT-005-version-selector.md)

## Context

Modders targeting B41 (stable multiplayer) need the B41 API even after B42 ships. This feature lets users switch between pre-extracted API versions without leaving the viewer.

This is the most infrastructure-heavy feature. Implement after TASK-012 (tabs) since version switching interacts with per-tab state.

## Acceptance Criteria

- [ ] A version dropdown appears in the toolbar. On a fresh GitHub Pages deploy with one version, it shows only that version (no visible change from current).
- [ ] If `versions/versions.json` lists multiple versions, the dropdown shows all with their labels.
- [ ] Selecting a version reloads the API data and re-initialises the viewer with the new data.
- [ ] `?v=B42#ClassName` URL form works: loading with that query string selects the correct version and class.
- [ ] The extractor writes `lua_api_<build>.json` (not `lua_api.json`) when a version can be detected; `versions.json` is updated/created.

## Implementation Plan

### Step 1 — Extractor: versioned output (`extract_lua_api.py`)

Detect the PZ build number from `ProjectZomboid.exe` file metadata or from a known version file in the PZ install. Fall back to `"unknown"` if undetectable.

```python
def detect_pz_version(src_root):
    # Option A: read from version.txt or similar
    vf = src_root.parent / 'ProjectZomboid64.bat'  # or version file
    # Option B: use pywin32 / subprocess to read PE version — skip on non-Windows
    # Fallback:
    return 'B42'  # or prompt user / read from env var PZ_VERSION
```

If version is known, write to `pz-lua-api-viewer/versions/lua_api_<version>.json`.
Update `pz-lua-api-viewer/versions/versions.json`:
```json
[
  {"id": "B42", "label": "Build 42", "file": "versions/lua_api_B42.json"},
  {"id": "B41", "label": "Build 41 (MP)", "file": "versions/lua_api_B41.json"}
]
```

Continue writing `pz-lua-api-viewer/lua_api.json` (latest/default) unchanged, so the viewer works without a `versions.json`.

### Step 2 — Viewer: load version manifest (`js/app.js`)

Replace the single `fetch('./lua_api.json')` with a version-aware loader:

```js
async function loadApi() {
  // Check for ?v= query param
  const params = new URLSearchParams(location.search);
  const vParam = params.get('v');

  // Try to load versions manifest
  let versionsManifest = null;
  try {
    const r = await fetch('./versions/versions.json');
    if (r.ok) versionsManifest = await r.json();
  } catch {}

  // Determine which file to load
  let apiFile = './lua_api.json';
  if (versionsManifest && vParam) {
    const entry = versionsManifest.find(v => v.id === vParam);
    if (entry) apiFile = './' + entry.file;
  } else if (versionsManifest && versionsManifest.length > 0) {
    apiFile = './' + versionsManifest[0].file;
  }

  const r = await fetch(apiFile);
  if (!r.ok) throw new Error(r.statusText);
  const data = await r.json();
  return { data, versionsManifest };
}
```

### Step 3 — Version dropdown (`index.html` + `js/app.js` + `app.css`)

Add to `#header`:
```html
<select id="version-select" style="display:none"></select>
```

After loading the manifest, populate and show the dropdown:
```js
function setupVersionDropdown(manifest, currentId) {
  const sel = document.getElementById('version-select');
  if (!manifest || manifest.length <= 1) return; // hide if only one version
  sel.style.display = '';
  sel.innerHTML = manifest.map(v =>
    `<option value="${esc(v.id)}"${v.id === currentId ? ' selected' : ''}>${esc(v.label)}</option>`
  ).join('');
  sel.addEventListener('change', () => {
    const params = new URLSearchParams(location.search);
    params.set('v', sel.value);
    location.search = params.toString(); // triggers full page reload with new version
  });
}
```

CSS:
```css
#version-select { background: var(--bg3); border: 1px solid var(--border); border-radius: 4px;
  color: var(--text); font-size: 12px; padding: 4px 8px; cursor: pointer; }
```

### Step 4 — URL: encode version in hash navigation

When version is active, prefix navigation URLs: `location.search = '?v=B42'` is set at page load; hash updates (`location.hash = ...`) still work alongside query params. No changes needed to hash logic.

### Step 5 — Regenerate and commit versioned files

After implementing: extract B42 (current) as `versions/lua_api_B42.json`, create `versions/versions.json`. Add both to the repo. Add `versions/` to `.gitignore` exemption if needed.

## Notes

- Version switching uses a full page reload (`location.search = ...`) — simplest approach, avoids re-init complexity. The alternative (re-calling `init()` with new data) requires resetting all global state which is error-prone.
- The single-version fallback (`lua_api.json`) ensures existing deployments without `versions/` keep working identically.
- This task is large. Consider splitting: Part A (extractor versioned output + versions.json) and Part B (viewer dropdown + URL version param) can be done in separate commits.

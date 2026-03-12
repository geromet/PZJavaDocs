> **COMPLETED 2026-03-12** — Added content-type guard in `fetchSource` (`utils.js`) to reject HTML responses. Added early-return in `showSource` (`source-viewer.js`) when `cls.source_file` is empty, showing a friendly message instead. Root cause: `zombie.characters.skills.Perk` and `.Perks` are nested types with no standalone `.java` file.

# TASK-021: Fix source panel showing directory listing HTML

**Status:** Done
**Estimated scope:** Small
**Touches:** `js/utils.js` or `js/source-viewer.js` (`fetchSource`), possibly `pz-lua-api-viewer/extract_lua_api.py`
**Fixes:** [BUG-013](../Bugs/BUG-013-source-panel-shows-directory-listing.md)

## Context

Selecting `zombie.characters.skills` and opening the Source tab shows raw directory-listing HTML inside the `<code>` block. The fetch resolves successfully (HTTP 200) but returns an HTML page from the dev server or GitHub Pages instead of a `.java` file. The source viewer renders the HTML verbatim.

## Acceptance Criteria

- [ ] For classes whose `source_file` path is invalid or missing, the source panel shows a human-readable error message rather than HTML.
- [ ] The content-type guard catches any future cases of the same class of bug.
- [ ] `zombie.characters.skills` either shows correct source or a clear "Source not available" message.

## Implementation Plan

### Step 1 — Investigate the root cause

1. Open `lua_api.json` and find the entry for `zombie.characters.skills`.
2. Check its `source_file` field. Likely it is a directory path (e.g. `zombie/characters/skills`) rather than a `.java` file path.
3. If `source_file` ends without `.java`, that is the root cause — the extractor mapped it to a directory.

If `zombie.characters.skills` is a package-level class (e.g. a `package-info.java` annotation class or a generated class), it may have no real source file and the extractor should set `source_file: null` for it.

### Step 2 — Fix `fetchSource` to detect HTML responses (`js/utils.js` or `js/source-viewer.js`)

Find the `fetchSource` function and add a content-type check:

```js
async function fetchSource(relPath) {
  // ... existing URL resolution (localDirHandle path or ./sources/ fallback) ...
  const res = await fetch(url);
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  const ct = res.headers.get('content-type') || '';
  if (ct.includes('text/html')) {
    throw new Error('Got a directory listing — source file path may be incorrect');
  }
  return res.text();
}
```

This guard will show the "Source not available" error message instead of rendering directory HTML for any class with a bad path, now or in the future.

### Step 3 — Fix the extractor (if root cause is bad `source_file`)

In `extract_lua_api.py`, find where `source_file` is set for each class. Ensure it only assigns a `.java` path when one is confirmed to exist, and sets `null` (or omits the field) otherwise.

**After any extractor change:** re-run the extractor and verify `zombie.characters.skills` has a correct or null `source_file` before pushing.

```
cd projectzomboid/
python pz-lua-api-viewer/extract_lua_api.py
```

### Step 4 — Run `prepare_sources.py` if new source files are needed

If the fix involves copying new `.java` files to `sources/`:

```
cd pz-lua-api-viewer/
python prepare_sources.py
```

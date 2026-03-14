# S09: Load Performance

**Goal:** Ship TASK-033 (split JSON), TASK-034 (inline critical CSS), TASK-035 (service worker). Reduce initial load from 6MB to ~200KB; make repeat visits instant.
**Demo:** First visit loads in under 2 seconds on a 3G connection. Repeat visit loads from service worker cache — essentially instant.

## Must-Haves

- `lua_api.json` split into: `lua_api_index.json` (~200KB, class list + metadata) and `lua_api_detail/<fqn>.json` per class
- Extractor generates both formats; existing single-file format still works as fallback
- Search index works with the index-only JSON
- Detail panel fetches per-class JSON on demand (with loading indicator)
- Critical CSS inlined in `<head>`; remaining CSS loaded async
- Service worker caches index JSON, CSS, JS on first visit; serves from cache on repeat
- All existing tests pass

## Tasks

- [ ] **T01: Split lua_api.json (TASK-033)** `est:2h`
  - Do: Modify `extract_lua_api.py` to also write `lua_api_index.json` (classes with simple metadata: FQN, set_exposed, lua_tagged, is_enum, method count, field count) and `detail/<fqn>.json` (full class data). Update `js/app.js` to load index first, then fetch detail on demand. Search index builds from the index JSON.
  - Files: `extract_lua_api.py`, `js/app.js`, `js/search-index.js`, `js/class-detail.js`
  - Blocked by: S06 (search index must work with new format)

- [ ] **T02: Inline critical CSS (TASK-034)** `est:45m`
  - Do: Extract above-the-fold CSS (header, sidebar skeleton, loading spinner) into inline `<style>` in `<head>`. Load `app.css` via `<link rel="preload" as="style">` + onload swap. Ensure no FOUC.
  - Files: `index.html`, `app.css`
  - Blocked by: S07 (don't inline CSS that's about to change)

- [ ] **T03: Service worker (TASK-035)** `est:1.5h`
  - Do: Register `sw.js` from `index.html`. Cache strategy: cache-first for static assets (CSS, JS, index JSON), network-first for detail JSON. Include version in cache name for clean updates.
  - Files: new `sw.js`, `index.html`
  - Blocked by: T01 (cache strategy depends on file structure)

- [ ] **T04: Update tests and commit S09** `est:15m`
  - Files: `.gsd/test-suite.py`, `extract_lua_api.py`

## Files Likely Touched

- `extract_lua_api.py` — JSON splitting
- `js/app.js` — lazy loading, SW registration
- `js/search-index.js` — work with index-only JSON
- `js/class-detail.js` — fetch detail on demand
- `index.html` — inline CSS, SW registration
- `app.css` — split critical vs non-critical
- New: `sw.js`

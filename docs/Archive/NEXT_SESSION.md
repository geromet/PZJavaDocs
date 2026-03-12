# Next Session — Task Index

Work through these in order. Each file is self-contained.

| # | File | Task | Status | Touches |
|---|------|------|--------|---------|
| 1 | `task-01-source-coverage.md` | Fix missing classes + deep call linking | ✅ Done (phase 1) | `extract_lua_api.py`, `prepare_sources.py`, `js/app.js`, `js/state.js`, `js/source-viewer.js` |
| 2 | `task-02-nav-resilience.md`  | Refactor navigation state to be resilient | ✅ Done | `js/state.js`, `js/app.js`, `js/globals.js` |
| 3 | `task-03-inheritance.md`     | Inheritance display (extends/implements/subclasses/inherited methods) | ✅ Done | `extract_lua_api.py`, `js/class-detail.js`, `app.css`, `js/app.js` |

## Task 01 — What was done
- Added `build_source_index()` to `extract_lua_api.py` — scans all .java files under SRC_ROOT
  (skipping pz-lua-api-viewer/) and emits `_source_index: {SimpleName → relative/path.java}` in JSON
- `_source_index` covers 2105 source-linkable classes NOT already in the API
- `prepare_sources.py` now also copies `_source_index` files (total: ~3889 source files)
- `js/state.js`: added `sourceOnlyPaths = {}` map
- `js/app.js init()`: merges `API._source_index` into `sourceOnlyPaths`
- `js/source-viewer.js linkClassRefs()`: links source-only refs with `data-source-path` attribute
- `js/source-viewer.js showSourceByPath()`: renders source-only files in the source panel
- `js/app.js click handler`: routes `data-source-path` clicks to `showSourceByPath()`

## Task 01 — Phase 2 (not yet done)
See task-01-source-coverage.md Problem B (wildcard import fallback) and Problem C (method-level linking).
These are lower priority — phase 1 covers the main class-linking gap.

## Task 02 — What was done
- `js/state.js`: removed `navJumping`, added `navSeq` counter
- `js/app.js`: added `captureState()` (reads current UI into state object)
- `js/app.js`: added `applyState(s)` (restores UI from state, never pushes nav)
  - `globalSource` case sets tab active directly, skips `initGlobals()` to avoid table-view flash
- `js/app.js`: `navGo` is now async, just sets navIndex and calls `applyState`
- `js/app.js`: `navPush` no longer has `navJumping` guard; duplicate check covers fqn+javaMethod
- `js/app.js`: `selectClass(fqn, matchInfo, noPush)` — guards navPush with noPush param
- `js/app.js`: `switchTab(tab, noPush)` — guards navPush with noPush param
- `js/app.js`: src-class-ref click now uses `switchTab('classes', noPush=true)` to avoid double-push
- `js/globals.js`: `showGlobalSource(javaMethod, noPush)` — navPush moved to click handler

## Task 03 — What was done
- `extract_lua_api.py`: new inheritance pass (step 4.5) after all_classes is built
  - `get_file_import_map(java_file)` — reads per-file imports from cached javalang tree
  - `resolve_simple(name, imap, pkg, all_cls)` — resolves simple names to FQN
  - `_global_simple_to_fqn` — global fallback map from path structure
  - Adds `extends` (str), `implements` (list), `subclasses` (list, sorted) to class entries
  - 891 classes with extends, 160 with implements, 54 with subclasses
- `js/class-detail.js`: added `renderInheritHeader(cls, fqn)` and `renderInheritedMethods(cls, fqn, filterStr)`
  - Inheritance tree walks chain upward; renders top-to-bottom with └─ indentation
  - Subclasses truncated to 10 with "…and N more" toggle
  - Inherited methods: callable-only, filtered by methodSearch, compact comma-separated links
  - `refreshMethods` now takes `fqn` param and populates `inherit-wrap`
- `app.css`: inheritance header and inherited-methods-list styles
- `js/app.js`: delegated click handlers for `inherit-link`, `inherit-method-link`, `inherit-more-toggle`

All three tasks complete. This branch is ready to test and merge.

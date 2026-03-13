# Decisions Register

<!-- Append-only. Never edit or remove existing rows.
     To reverse a decision, add a new row that supersedes it.
     Read this file at the start of any planning or research phase. -->

| # | When | Scope | Decision | Choice | Rationale | Revisable? |
|---|------|-------|----------|--------|-----------|------------|
| 1 | 2026-03 | Navigation | History suppression during state restore | `_restoringState` flag in `state.js` | Single flag, no param threading, zero cognitive overhead for new nav actions | No |
| 2 | 2026-03 | Data model | Inheritance gaps | `_extends_map` separate from `all_classes` | Keeps class list clean — only real API classes; gap-filling is a separate lookup | No |
| 3 | 2026-03 | Data model | Interface extends | `_interface_extends` separate from `_extends_map` | 1:many vs 1:1 data shape; different traversal semantics | No |
| 4 | 2026-03 | UI | Implements display | Grouped by class in chain, not flat | Flat list of 20+ items is unusable; grouping shows provenance | Low |
| 5 | 2026-03 | Legal | Pre-shipped Java sources | Ship in `sources/` (~3889 files) | Indie Stone ToS 2.1 permits distributing base game files for non-commercial PZ promotion | No |
| 6 | 2026-03 | Git | Branch discipline | Never push to `main` | `main` auto-deploys to GitHub Pages | No |
| 7 | 2026-03-13 | GSD setup | GSD root | `.gsd/` at `projectzomboid/` (CWD), not inside `pz-lua-api-viewer/` | GSD resolves `.gsd/` relative to CWD; CWD is `projectzomboid/`; placing it here lets auto-mode work without `cd` | Low |
| 8 | 2026-03-13 | GSD setup | Shared docs | `pz-lua-api-viewer/docs/` remains the shared doc system; GSD roadmap/slices/tasks live in `.gsd/milestones/` | Claude Code and GSD both need `docs/`; GSD planning artifacts are separate from shared operational docs | No |
| 9 | 2026-03-13 | M001 structure | Task granularity | Each former TASK-NNN becomes one GSD task within a slice | Existing task files have enough detail; slice wraps related tasks into a demoable unit | Low |
| 10 | 2026-03-13 | extract_lua_api.py | Source root path | Split SRC_ROOT into _PROJ_ROOT (projectzomboid/) and SRC_ROOT (_PROJ_ROOT/sources/) | Java sources live under sources/ subdir, not at projectzomboid/ root; hardcoded paths were broken | When source layout changes |
| 11 | 2026-03-13 | lua_api.json / app.js | Build-time precomputation strategy | Inject _class_by_simple_name + _source_only_paths into JSON at extract time; app.js fast-path with fallback | Eliminates O(n) loop on every page load; backwards compatible with old JSON | No — fallback handles old JSON forever |

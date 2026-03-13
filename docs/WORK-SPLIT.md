# Work Split — 2026-03-12

Coordinated task assignments between Claude Code and GSD (Pi) to avoid file conflicts and blocking.

## Claude Code — UI shell & navigation

| Task | Summary | Key Files |
|------|---------|-----------|
| TASK-012 | Tab bar system | js/app.js, js/state.js, index.html, app.css |
| TASK-014 | Globals sticky headers + fold memory | js/globals.js, app.css |
| TASK-015 | Wildcard import fallback | js/source-viewer.js |

Work order: 012 first (foundational), then 014 and 015 in any order.

## GSD (Pi) — Content rendering & extractor

| Task | Summary | Key Files |
|------|---------|-----------|
| TASK-013 | Search highlight + clear buttons | js/class-list.js, js/class-detail.js, index.html, app.css |
| TASK-016 | Javadoc extraction + display | extract_lua_api.py, js/class-detail.js, app.css |
| TASK-017 | Build-time precomputation | extract_lua_api.py, js/app.js |

Work order: 013 first (no dependencies), then 016 (also touches class-detail.js), then 017.

## Deferred

| Task | Summary | Blocked by |
|------|---------|------------|
| TASK-018 | Version selector | TASK-012 (needs tab bar) |

Assign after both batches above are complete.

## File Ownership

This split was designed so neither agent needs to edit the other's files:

| File | Owner |
|------|-------|
| js/app.js | Claude Code (TASK-012); GSD small init change (TASK-017) — coordinate |
| js/state.js | Claude Code |
| js/globals.js | Claude Code |
| js/source-viewer.js | Claude Code |
| js/class-list.js | GSD |
| js/class-detail.js | GSD |
| extract_lua_api.py | GSD |
| index.html | Both (different DOM regions — no conflict) |
| app.css | Both (append-only, different sections — no conflict) |

**Note on js/app.js:** Both agents touch this file. TASK-012 (Claude) restructures `selectClass()` and adds tab functions. TASK-017 (GSD) adds a small change to `init()`. GSD should do TASK-017 **after** Claude finishes TASK-012 to avoid merge conflicts, or coordinate on the exact lines changed.

## Rules

- Both agents commit to `liability-machine` branch.
- Never push to `main`.
- Check this doc and `STATUS.md` at the start of each session.
- When a task is done, archive it and update `STATUS.md` per the usual workflow.

# Project Status

**Last updated:** 2026-03-13 (session 3)

This is the live tracker. Update it when:
- A bug is filed, fixed, or closed → update Open Bugs
- A task is created, started, or completed → update Active Tasks
- A feature is added or reprioritised → update Planned Features
- After a session ends, verify the tables reflect reality

---

## Open Bugs

*None*

---

## Active Tasks (priority order)

| ID | File | Priority | Summary |
|----|------|----------|---------|
| TASK-016 | [TASK-016](Tasks/TASK-016-javadoc-extraction.md) | **Blocked** | Javadoc extraction — decompiled sources have 0 Javadoc blocks |
| TASK-024 | [TASK-024](Tasks/TASK-024-search-index-debounce.md) | **High** | Search index + debounced progressive rendering |
| TASK-025 | [TASK-025](Tasks/TASK-025-virtual-sidebar.md) | **High** | Virtualized sidebar list |
| TASK-026 | [TASK-026](Tasks/TASK-026-hover-prefetch.md) | **High** | Prefetch class data + source on hover |
| TASK-027 | [TASK-027](Tasks/TASK-027-declutter-header-filters.md) | **Medium** | Collapse header stats + filter dropdown |
| TASK-028 | [TASK-028](Tasks/TASK-028-color-typography.md) | **Medium** | Reduce color palette + tighten typography |
| TASK-029 | [TASK-029](Tasks/TASK-029-layout-stability.md) | **Medium** | Zero layout shift |
| TASK-030 | [TASK-030](Tasks/TASK-030-breadcrumbs.md) | **Low** | Breadcrumb trail in detail panel |
| TASK-031 | [TASK-031](Tasks/TASK-031-url-state.md) | **Medium** | Full UI state in URL |
| TASK-032 | [TASK-032](Tasks/TASK-032-recently-viewed.md) | **Low** | Recently viewed classes |
| TASK-033 | [TASK-033](Tasks/TASK-033-split-json.md) | **High** — blocked by TASK-024 | Split lua_api.json into index + per-class detail |
| TASK-034 | [TASK-034](Tasks/TASK-034-inline-critical-css.md) | **Low** — blocked by TASK-027, TASK-028 | Inline critical CSS + async load rest |
| TASK-035 | [TASK-035](Tasks/TASK-035-service-worker.md) | **Low** — blocked by TASK-033 | Service worker for repeat-visit caching |

---

## Planned Features — Shipped

| ID | Shipped | Summary |
|----|---------|---------|
| ~~FEAT-004~~ | 2026-03-13 (session 3) | ~~Draggable sidebar resize splitter~~ |
| ~~FEAT-005~~ | 2026-03-13 (session 3) | ~~Version selector + multi-version API~~ |
| ~~FEAT-006~~ | ~~2026-03-12~~ | ~~Tab bar system~~ |
| ~~FEAT-007~~ | 2026-03-13 (session 3) | ~~Middle-click to open in new tab~~ |
| ~~FEAT-008~~ | 2026-03-13 (session 3) | ~~Build-time precomputation~~ |
| ~~FEAT-011~~ | ~~2026-03-12~~ | ~~Globals sticky headers~~ |
| ~~FEAT-012~~ | ~~2026-03-12~~ | ~~Globals fold memory~~ |
| ~~FEAT-013~~ | ~~2026-03-12~~ | ~~Search highlight + clear~~ |
| ~~FEAT-014~~ | 2026-03-13 (session 3) | ~~Hover preview card~~ |

## Planned Features — Remaining

| ID | File | Blocked by | Summary |
|----|------|------------|---------|
| FEAT-010 | [FEAT-010](Planned_Features/FEAT-010-comments-descriptions.md) | TASK-016 (blocked) | Javadoc/LuaDoc display in detail panel — waiting on original PZ sources |
| FEAT-015 | [FEAT-015](Planned_Features/FEAT-015-mcmaster-speed-clarity.md) | — | McMaster-Carr speed & clarity overhaul (TASK-024 through TASK-035) |

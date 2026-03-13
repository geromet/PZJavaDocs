# FEAT-015: McMaster-Carr Speed & Clarity Overhaul

**Filed:** 2026-03-13
**Priority:** High
**Inspiration:** https://www.mcmaster.com/ — known for fastest e-commerce UX on the web

## Goal

Make the PZ Lua API Viewer feel as fast and clear to navigate as McMaster-Carr's catalog. Key principles:
- **Instant perceived load** — show usable UI before all data arrives
- **Prefetch on hover** — start loading before the click
- **Zero layout shift** — panels never jump or resize unexpectedly
- **Instant search** — results appear as you type with no perceptible delay
- **Minimal chrome** — let the content structure the experience
- **Keyboard-first** — devs/modders live on the keyboard

## Task Breakdown

All tasks filed as TASK-024 through TASK-035. See individual task files for details.

### Phase 1: Instant Search (biggest daily friction)
- TASK-024: Search index + debounced progressive rendering

### Phase 2: DOM Performance
- TASK-025: Virtualized sidebar list

### Phase 3: Hover Prefetch
- TASK-026: Prefetch class data + source on hover

### Phase 4: Declutter UI
- TASK-027: Collapse header stats + filter dropdown
- TASK-028: Reduce color palette + tighten typography

### Phase 5: Layout Stability
- TASK-029: Zero layout shift (saved widths, skeleton loaders, fixed columns)

### Phase 6: Navigation Clarity
- TASK-030: Breadcrumb trail in detail panel
- TASK-031: Full UI state in URL (filter, search, tab)
- TASK-032: Recently viewed classes

### Phase 7: Load Performance
- TASK-033: Split lua_api.json into index + per-class detail
- TASK-034: Inline critical CSS + async load rest
- TASK-035: Service worker for repeat-visit caching

## Dependency Graph

```
TASK-024 (search index)          — no deps, safe to start
TASK-025 (virtual sidebar)       — no deps, safe to start
TASK-026 (hover prefetch)        — no deps, safe to start
TASK-027 (declutter header)      — no deps, safe to start
TASK-028 (color/typography)      — no deps, safe to start (CSS only)
TASK-029 (layout stability)      — no deps, safe to start (CSS + minor JS)
TASK-030 (breadcrumbs)           — no deps, safe to start
TASK-031 (URL state)             — no deps, safe to start
TASK-032 (recently viewed)       — no deps, safe to start
TASK-033 (split JSON)            — blocked by TASK-024 (search index must work with new format)
TASK-034 (inline critical CSS)   — after TASK-027/028 (don't inline CSS that's about to change)
TASK-035 (service worker)        — after TASK-033 (cache strategy depends on file structure)
```

## Files Likely Touched

- `js/class-list.js` — sidebar rendering, search
- `js/app.js` — events, init, navigation
- `js/class-detail.js` — detail panel
- `js/source-viewer.js` — source loading
- `app.css` — all styling
- `index.html` — header layout, inline CSS
- `extract_lua_api.py` — JSON splitting (TASK-033)
- New: `js/search-index.js`, `sw.js`

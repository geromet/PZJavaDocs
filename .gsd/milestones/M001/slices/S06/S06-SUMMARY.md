---
id: S06
parent: M001
provides:
  - Pre-computed search index (js/search-index.js)
  - Progressive rendering (first 50 results in first frame)
  - Event delegation on #class-list (no per-item listeners)
  - Search query < 3ms, full render < 7ms (was 25ms)
  - 21/21 automated tests passing
key_files:
  - js/search-index.js
  - js/class-list.js
  - js/app.js
  - .gsd/test-suite.py
key_decisions:
  - "Deferred full virtual scrolling — progressive render already solves perceived lag"
  - "Search index pre-lowercases all strings at build time to avoid hot-path allocations"
  - "Event delegation on #class-list replaces per-item click listeners"
patterns_established:
  - "Progressive rendering pattern: first N items immediately, rest via requestAnimationFrame"
  - "Pre-computed index pattern: build once at init, query on keystroke"
verification_result: pass
completed_at: 2026-03-14T01:12:00Z
---

# S06: Instant Search & DOM Performance

**Pre-computed search index cuts search to <3ms; progressive rendering keeps first frame at 50 DOM nodes.**

## What Happened

Built `js/search-index.js` that pre-computes lowercase names for all 1096 classes, their methods, and fields at init time. Search queries hit the index directly — no `.toLowerCase()` calls per keystroke, no iteration over method arrays.

Added progressive rendering: search results render the first 50 items in the initial frame, then batch the rest via `requestAnimationFrame`. This keeps the first render under 7ms even for broad queries like 'a' (1050 results).

Replaced per-item click handlers with event delegation on `#class-list`, which works for both search results and the package tree view.

## Deviations

T02 (full virtual scrolling) was deferred. The progressive rendering already solves the user-visible problem — first frame renders in <7ms with 50 nodes. Full virtual scrolling would keep the DOM at ~50 nodes during scroll too, but adds significant complexity (scroll position management, package tree virtual layout) for marginal gain. Can revisit if the sidebar proves sluggish on lower-end machines.

## Files Created/Modified

- `js/search-index.js` — NEW: pre-computed search index
- `js/class-list.js` — Progressive rendering, removed per-item listeners
- `js/app.js` — buildSearchIndex() call, event delegation setup
- `index.html` — Added search-index.js script, bumped cache versions to v=3
- `.gsd/test-suite.py` — Added search performance + progressive render tests

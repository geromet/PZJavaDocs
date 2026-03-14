# S06: Instant Search & DOM Performance

**Goal:** Make search feel instant and sidebar rendering fast. Currently search iterates all 1096 classes + their methods/fields on every keystroke, and the sidebar renders all 1096 DOM nodes at once.
**Demo:** Type in the search box — results appear within one frame. Scroll the sidebar — no lag even with all 1096 classes visible.

## Must-Haves

- Search results appear within 16ms (one frame) of typing
- Debounced input (150ms) prevents excessive recomputation
- Search index is pre-built at init time — keystroke handler only queries the index
- Sidebar uses virtual scrolling — only visible items are DOM-rendered
- Package tree fold/unfold still works with virtual scrolling
- All existing tests still pass (19/19)

## Proof Level

- Real runtime required: yes (perceived latency is the metric)
- Measurable: console.time around search, DOM node count before/after
- Human/UAT: type rapidly in search, verify no stutter

## Verification

- `performance.now()` around search: < 16ms for 'Iso' query
- DOM node count in `#class-list`: < 100 when not all visible (virtual)
- All 19 existing tests pass
- Rapid typing in search box produces no visible lag

## Tasks

- [x] **T01: Build pre-computed search index** `est:1.5h`
  - Why: TASK-024 core — current search does O(n*m) string matching per keystroke
  - Files: new `js/search-index.js`, `js/app.js` (init), `js/class-list.js` (query)
  - Do:
    1. Create `js/search-index.js` with `buildSearchIndex(API)` that pre-computes:
       - Lowercase class simple names and FQNs
       - Lowercase method names per class (joined for substring matching)
       - Lowercase field names per class
       - A flat sorted array for fast prefix matching
    2. Call `buildSearchIndex(API)` in `init()` after API loads
    3. Replace `scoreClass()` to query the index instead of iterating methods/fields
    4. Add 150ms debounce to search input handler in `app.js`
    5. Progressive rendering: render first 50 results immediately, rest on idle
  - Verify: `console.time('search')` shows < 16ms; typing 'Iso' shows results instantly
  - Done when: Search feels instant; no regression in existing tests

- [ ] **T02: Virtual scrolling for sidebar** `est:2h`
  - Why: TASK-025 — rendering 1096 DOM nodes causes initial paint delay and scroll jank
  - Files: `js/class-list.js`, `app.css`
  - Do:
    1. When not searching: keep the package tree as-is (it's naturally virtualized by folding)
    2. When searching (flat list): implement virtual scroll:
       - Measure item height (fixed at ~32px)
       - Render only items visible in the `#class-list` scroll viewport + buffer
       - Use a spacer div for total height
       - Re-render on scroll (debounced via requestAnimationFrame)
    3. Maintain click handlers via event delegation on `#class-list` (already partially done)
    4. Ensure keyboard navigation (arrow keys) still scrolls to the right item
  - Verify: Search for 'get' (many results); DOM nodes in `#class-list` < 100; scroll is smooth
  - Done when: Virtual scroll works for search results; tree mode unchanged; all tests pass

- [ ] **T03: Update tests and commit S06** `est:15m`
  - Why: Add search performance test; verify no regressions
  - Files: `.gsd/test-suite.py`
  - Do: Add test for search speed (measure JS execution time via `page.evaluate`). Run full suite. Commit.
  - Verify: 20+ tests pass
  - Done when: Committed and pushed to `liability-machine`

## Files Likely Touched

- New: `js/search-index.js`
- `js/class-list.js` — virtual scroll + index query
- `js/app.js` — debounce, init index
- `index.html` — add `<script src="js/search-index.js">`
- `app.css` — virtual scroll spacer styles
- `.gsd/test-suite.py` — new performance test

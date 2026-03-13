# TASK-024: Search Index + Debounced Progressive Rendering

**Parent:** FEAT-015 (McMaster speed & clarity)
**Priority:** High — biggest daily-use friction
**Blocked by:** Nothing
**Blocks:** TASK-033 (split JSON — search index must work with new format)

## Problem

Search re-scores all 1,096 classes and rebuilds the entire sidebar DOM on every keystroke. `scoreClass()` in `class-list.js` does a linear scan over all classes, their methods, and their fields. The DOM rebuild creates 1,000+ elements synchronously.

## Plan

### 1. Build a search index at load time (`js/search-index.js`)

After `API` is loaded in `app.js`, build:
- A **prefix map**: `Map<string, Set<fqn>>` — maps lowercase prefixes (2+ chars) of class simple names to FQNs
- A **method index**: `Map<string, Set<fqn>>` — maps lowercase method name substrings to the FQNs that contain them
- A **field index**: `Map<string, Set<fqn>>` — same for fields

Construction time budget: < 50ms (profile it).

### 2. Replace `scoreClass()` with index lookups

Instead of iterating all classes per keystroke:
1. Look up candidates from the prefix map
2. Score only the candidates (not all 1,096)
3. Fall back to full scan only for queries < 2 chars

### 3. Debounce search input

- Debounce `buildClassList()` by 120ms on keystroke
- But: if the query is a prefix match (just extending the previous query), filter the *existing* results instead of rebuilding

### 4. Progressive rendering

- Render the first 30 results immediately
- Render remaining results in batches of 50 using `requestIdleCallback` (or `requestAnimationFrame` fallback)
- Show a "Loading more results..." indicator if there are batches pending

## Files

- New: `js/search-index.js`
- Modified: `js/class-list.js` (scoring, rendering)
- Modified: `js/app.js` (debounce, index init)
- Modified: `index.html` (add script tag)

## Acceptance Criteria

- [ ] Typing a 3-char query shows results in < 50ms (measured via `performance.now()`)
- [ ] Backspace/clear and re-type doesn't cause visible lag
- [ ] Search results are identical to current behavior (same scoring, same order)
- [ ] Progressive rendering shows first results without waiting for all to render
- [ ] Index build time < 50ms logged to console on init

---
estimated_steps: 4
estimated_files: 3
skills_used: []
---

# T01: Middle-click new tab (FEAT-007)

**Slice:** S02 — Tab Enhancements
**Milestone:** M001

## Description

Implement FEAT-007: middle-clicking or Ctrl+clicking any class link opens that class in a new tab rather than navigating the current tab. The tab system already exists in `state.js`.

## Steps

1. Add `openNewTab(fqn)` utility to `js/state.js` that creates a new tab entry, switches to it, and loads the class — cap at 10 tabs with oldest non-active eviction.
2. Wire middle-click (`event.button === 1`) and Ctrl+click (`event.ctrlKey && event.button === 0`) handlers into sidebar class items in `js/class-list.js` for both search results and namespace tree items.
3. Add the same middle-click / Ctrl+click detection to delegated click handlers in `js/app.js` for source class refs (`a.src-class-ref`), inherit links (`a.inherit-link[data-fqn]`), and inherited method links (`a.inherit-method-link[data-fqn]`). Call `openNewTab(fqn)` and `preventDefault()`.
4. Verify left-click still navigates the current tab without regression.

## Must-Haves

- [x] Middle-clicking any class link opens a new tab with that class
- [x] Ctrl+clicking any class link opens a new tab with that class
- [x] Left-click still navigates current tab (no regression)
- [x] No console errors in browser dev tools

## Verification

- `grep -q "openNewTab" js/state.js` — utility function exists
- `grep -c "button === 1" js/app.js` returns >= 3 — middle-click handlers wired in multiple locations
- `grep -q "openNewTab" js/class-list.js` — sidebar middle-click wired

## Observability Impact

- Signals added/changed: Tab count in `state.js` tabs array increases when `openNewTab()` called; active tab index updates
- How a future agent inspects this: Check `tabs.length` in browser console; inspect tab bar DOM for new tab elements
- Failure state exposed: Console error if `openNewTab()` receives an FQN not in `API.classes`

## Inputs

- `js/state.js` — existing tab system with tabs array and tab management functions
- `js/class-list.js` — sidebar click handlers for search results and namespace tree
- `js/app.js` — delegated click handlers for source refs, inherit links, inherit method links

## Expected Output

- `js/state.js` — new `openNewTab(fqn)` function added
- `js/class-list.js` — middle-click / Ctrl+click handlers added to class items
- `js/app.js` — middle-click / Ctrl+click detection added to existing delegated handlers

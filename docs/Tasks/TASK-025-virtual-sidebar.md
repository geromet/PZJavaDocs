# TASK-025: Virtualized Sidebar List

**Parent:** FEAT-015 (McMaster speed & clarity)
**Priority:** High — biggest DOM performance win
**Blocked by:** Nothing
**Blocks:** Nothing

## Problem

The sidebar creates a DOM element for every class (1,096 in tree mode, potentially all in search mode). Only ~30-40 are visible at any time. This wastes memory and makes initial render + filter switches slow.

## Plan

### 1. Virtual scroll for flat search results

When searching (flat list mode):
- Calculate total height from `filteredResults.length * ITEM_HEIGHT`
- Set a spacer div to that height
- On scroll, calculate which items are visible (with 5-item overscan above/below)
- Render only those items into a positioned container
- Recycle/update DOM nodes on scroll instead of creating new ones

`ITEM_HEIGHT` = measure a single `.class-item` height (likely 36-40px).

### 2. Tree mode — keep current approach but with lazy expansion

Tree mode already folds packages, which limits visible items. Don't virtualize tree mode — the folding already handles it. But:
- When a package has > 100 classes, render only the first 50 + "Show N more..." button
- This handles edge cases like `zombie.` root package

### 3. Keep scroll position on re-render

Store `scrollTop` before rebuilding, restore after. Currently lost on filter change.

## Files

- Modified: `js/class-list.js` (virtual scroll logic)
- Modified: `app.css` (scroll container positioning)

## Acceptance Criteria

- [ ] DOM node count in sidebar never exceeds 80 elements during search
- [ ] Scrolling through all 1,096 search results is smooth (60fps)
- [ ] Active class highlight works correctly with virtual scroll
- [ ] Clicking a virtual item selects the correct class
- [ ] Keyboard navigation (↑/↓) works through virtual items
- [ ] `scrollIntoView` for the active class works

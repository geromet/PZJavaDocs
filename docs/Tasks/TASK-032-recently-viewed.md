# TASK-032: Recently Viewed Classes

**Parent:** FEAT-015 (McMaster speed & clarity)
**Priority:** Low
**Blocked by:** Nothing
**Blocks:** Nothing

## Problem

When the search bar is empty, users see the full class tree. There's no quick way to jump back to a class they were looking at earlier (other than browser back or remembering the name). McMaster shows recently/frequently accessed items.

## Plan

### 1. Track recently viewed classes

Store in `localStorage` as a JSON array of FQNs, capped at 15.
- On `selectClass()`, push the FQN to the front (deduplicate)
- On page load, read the list

### 2. Show when search is focused and empty

When the user focuses the search bar and it's empty:
- Show a dropdown below the search bar with "Recently Viewed" header
- List the last 10 viewed classes with their simple name + package
- Clicking one calls `selectClass()`
- The dropdown hides on blur, on typing, or on Escape

### 3. Keyboard navigation

- `↓` from empty search focuses first recent item
- `↑`/`↓` navigates the list
- `Enter` selects
- `Escape` closes

## Files

- Modified: `js/app.js` (localStorage read/write, dropdown logic)
- Modified: `app.css` (dropdown styles)
- Modified: `index.html` (dropdown container)

## Acceptance Criteria

- [ ] Recently viewed classes appear when search is focused + empty
- [ ] List shows up to 10 items, most recent first
- [ ] Clicking or pressing Enter on an item navigates to that class
- [ ] Dropdown doesn't appear when search has text
- [ ] Data persists across page reloads
- [ ] Keyboard navigable

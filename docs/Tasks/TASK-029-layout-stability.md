# TASK-029: Zero Layout Shift

**Parent:** FEAT-015 (McMaster speed & clarity)
**Priority:** Medium
**Blocked by:** Nothing
**Blocks:** Nothing

## Problem

Content panels resize and jump when switching between classes or tabs. The sidebar width isn't applied until JS runs, causing a flash. Table columns shift width based on content.

## Plan

### 1. Apply saved sidebar width before first paint

Add an inline `<script>` in `index.html` (before body renders) that reads `localStorage.getItem('splitW-sidebar')` and sets `#sidebar { flex: 0 0 Npx }` via a `<style>` element. No FOUC.

### 2. Skeleton loaders for detail panel

When a class is selected but detail hasn't rendered yet:
- Show a skeleton with grey pulse-animated bars for: class name, badges, methods table header, 5 method rows
- This reserves the space and prevents jump when real content loads

### 3. Fixed table column widths

Set explicit widths on method table columns:
- Return type: 100px
- Method name: flex
- Parameters: 200px

This prevents columns from shifting as different classes have different-length names.

### 4. Stable panel transitions

When switching content tabs (Detail ↔ Source):
- Don't hide/show panels — use `visibility: hidden` + `position: absolute` so the layout doesn't reflow
- Or: keep both rendered, toggle `display` but preserve dimensions with `min-height`

## Files

- Modified: `index.html` (inline script for sidebar width)
- Modified: `app.css` (skeleton, fixed columns, panel transitions)
- Modified: `js/class-detail.js` (skeleton rendering)

## Acceptance Criteria

- [ ] Sidebar width correct on first paint (no flash from default → saved width)
- [ ] Selecting a class shows skeleton immediately, then populates
- [ ] Table columns don't shift when switching between classes
- [ ] Tab switching (Detail ↔ Source) doesn't cause visible layout jump

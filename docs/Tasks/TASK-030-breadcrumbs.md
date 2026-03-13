# TASK-030: Breadcrumb Trail in Detail Panel

**Parent:** FEAT-015 (McMaster speed & clarity)
**Priority:** Low
**Blocked by:** Nothing
**Blocks:** Nothing

## Problem

When viewing a class like `zombie.characters.IsoPlayer`, there's no visual indication of where you are in the package hierarchy. McMaster shows a category breadcrumb trail for spatial orientation.

## Plan

### 1. Add breadcrumb bar above class header

When a class is selected, show:
`zombie › characters › IsoPlayer`

- Each package segment is clickable
- Clicking a segment scrolls the sidebar to that package and expands it
- The final segment (class name) is not clickable (you're already there)

### 2. Styling

- Small font (12px), grey, sits right above the class name
- Use `›` as separator
- Segments turn blue on hover

## Files

- Modified: `js/class-detail.js` (render breadcrumb)
- Modified: `app.css` (breadcrumb styles)

## Acceptance Criteria

- [ ] Breadcrumb appears above class name in detail panel
- [ ] Clicking a package segment expands that package in sidebar and scrolls to it
- [ ] Works correctly for deeply nested packages (5+ levels)
- [ ] Doesn't add visual noise — subtle, grey, only active on hover

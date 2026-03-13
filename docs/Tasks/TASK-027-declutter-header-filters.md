# TASK-027: Collapse Header Stats + Filter Dropdown

**Parent:** FEAT-015 (McMaster speed & clarity)
**Priority:** Medium — biggest clarity win
**Blocked by:** Nothing
**Blocks:** TASK-034 (inline CSS — don't inline CSS that's about to change)

## Problem

The header bar has 8+ elements competing for attention: title, back/forward, search, 3 stat counters, version select, folder button. Below it, 7 filter buttons add another visual row. McMaster's lesson: let content breathe. The top bar should be: logo, search, essential actions.

## Plan

### 1. Simplify header

Move to header:
- Logo/title (left)
- Back/Forward buttons
- Search bar (takes most space)
- Version select (if multiple versions)
- Local sources button

Move OUT of header:
- Stat counters → move into sidebar header area (above filter buttons)

### 2. Replace filter buttons with dropdown

Replace the 7 always-visible filter buttons with:
- A single "Filter ▾" button
- Clicking it opens a dropdown/popover with the filter options
- When a filter is active (not "All"), show a small colored badge on the button: "Filter: setExposed ▾"
- Dropdown closes on selection or click-outside

### 3. Fold depth controls

Keep fold depth controls but move them inline with the class count row: `320 classes  ▶▶ depth 2 ◀◀`

## Files

- Modified: `index.html` (header restructure)
- Modified: `app.css` (dropdown styles, header layout)
- Modified: `js/app.js` (dropdown open/close logic, event handlers)

## Acceptance Criteria

- [ ] Header has only: title, nav buttons, search, version select, folder button
- [ ] Stats visible in sidebar area (not header)
- [ ] Filter dropdown opens/closes cleanly
- [ ] Active filter shown as badge text on the button
- [ ] Dropdown closes on click-outside and on selection
- [ ] Keyboard accessible (Enter to open, arrow keys to navigate, Escape to close)
- [ ] Fold depth controls moved to class count row

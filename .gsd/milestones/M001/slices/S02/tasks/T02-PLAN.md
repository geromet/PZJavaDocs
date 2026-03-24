---
estimated_steps: 5
estimated_files: 3
skills_used: []
---

# T02: Hover preview card (FEAT-014)

**Slice:** S02 — Tab Enhancements
**Milestone:** M001

## Description

Implement FEAT-014: a floating preview card appears after 400ms hover on any `[data-fqn]` element, showing class name, FQN, exposure badges, method/field counts, and top 3 callable methods.

## Steps

1. Add a `<div id="hover-preview"></div>` container to `index.html`, hidden by default.
2. In `js/app.js` `setupEvents()`, add delegated `mouseover`/`mouseout` listeners on `document` that detect `[data-fqn]` targets. Use a 400ms show delay and 80ms hide grace period to prevent flicker.
3. On show: look up the class in the global `API` object, build preview HTML (simple name, FQN, exposure badges, method/field counts, first 3 callable methods alphabetically, keyboard hint), position the card near the cursor using `getBoundingClientRect()` clamped to viewport edges.
4. Style the `#hover-preview` card and `.hp-*` child classes in `app.css` — dark theme consistent with existing UI, subtle shadow, compact layout.
5. Dismiss card on any click via a document click listener. Verify left-click navigation still works.

## Must-Haves

- [x] Hover preview appears after 400ms delay on `[data-fqn]` elements
- [x] Card disappears on mouseout (with 80ms grace) or on click
- [x] Shows: simple name, FQN, method count, first 3 callable methods
- [x] Card stays within viewport (clamped positioning)
- [x] No console errors

## Verification

- `grep -q "hover-preview" index.html` — container div exists
- `grep -q "hover-preview" js/app.js` — hover logic wired
- `grep -q "#hover-preview" app.css` — styles present

## Observability Impact

- Signals added/changed: `#hover-preview` div visibility toggles on hover; DOM content updates with class preview data
- How a future agent inspects this: Check `#hover-preview` in Elements panel; verify `display` style transitions from `none` to visible
- Failure state exposed: Card not appearing after 400ms indicates broken event delegation or missing `[data-fqn]` attributes

## Inputs

- `js/app.js` — existing `setupEvents()` function where hover logic is added
- `app.css` — existing stylesheet to extend
- `index.html` — HTML structure to add preview container

## Expected Output

- `js/app.js` — IIFE hover preview logic added to `setupEvents()` (~80 lines)
- `app.css` — `#hover-preview` and `.hp-*` styles added (~20 lines)
- `index.html` — `<div id="hover-preview">` container added

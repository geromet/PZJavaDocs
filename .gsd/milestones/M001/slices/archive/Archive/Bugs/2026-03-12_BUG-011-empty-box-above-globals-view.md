> **FIXED 2026-03-12** — `showGlobalsPanel` now hides `#panels` wrapper when showing globals. Fixed in TASK-020.

# BUG-011: Empty box visible above global functions view

**Status:** Fixed (2026-03-12)
**Severity:** Low
**Touches:** `index.html`, `app.css`, `js/globals.js` or `js/app.js`

## Description

When the Global Functions tab is active, an empty/blank box is rendered above the globals content. It has no content and should not be visible.

## Steps to Reproduce

1. Open the viewer.
2. Click the "Global Functions" tab.
3. Observe an empty box above the globals table.

## Expected

No empty box. The globals header (search input + fold buttons) appears immediately at the top of the content area.

## Root Cause Hypothesis

A panel element that belongs to the Classes view (`#content-tabs`, `#detail-panel`, or `#source-panel`) is not being hidden when switching to the Globals tab, leaving a visible empty container above `#globals-panel`.

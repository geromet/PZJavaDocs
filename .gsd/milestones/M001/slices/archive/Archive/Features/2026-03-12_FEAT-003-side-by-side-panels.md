# FEAT-003: Side-by-side panel layout

**Status:** Planned
**Priority:** High
**Complexity:** Medium

## Description

Allow the Detail panel and Source panel to be shown simultaneously side by side, rather than requiring the user to switch between Detail and Source tabs.

## Motivation

When exploring a class, users frequently need to cross-reference the method list with the source code. The current tab-switching workflow is friction-heavy.

## Proposed UX

- A layout toggle button (e.g. ⊟ / ⊞ icon) in the content area header switches between:
  - **Single panel** (current behavior — tabs for Detail / Source)
  - **Split panel** — Detail on the left, Source on the right
- In split mode the Detail/Source tabs disappear (both are always visible).
- The split ratio defaults to 50/50 but is user-adjustable (see FEAT-004).

## Notes

- This is a prerequisite for FEAT-004 (resizable panels) and complements FEAT-006 (tab bar system).
- The sidebar should remain unchanged regardless of panel layout.
- On narrow screens (< 900px) fall back to single-panel mode automatically.

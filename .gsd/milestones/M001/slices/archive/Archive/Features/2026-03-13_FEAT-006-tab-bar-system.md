> **COMPLETED 2026-03-12** — Tab bar system shipped in TASK-012 (0921427).

# FEAT-006: Tab bar system with multiple open classes

**Status:** Planned
**Priority:** High
**Complexity:** High

## Description

Replace the single-class view with a browser-style tab bar that allows multiple classes to be open simultaneously. Each tab shows a class (or the globals panel). Users can switch between tabs without losing their scroll position or method search.

## Motivation

When exploring inheritance chains or cross-referencing related classes (e.g. `IsoPlayer` vs `IsoGameCharacter`), users must navigate back and forth, losing their place each time. Tabs eliminate this entirely.

## Proposed UX

- Tab bar appears above the content area (Detail + Source panels).
- Clicking a class in the sidebar opens it in the current tab (default) or in a new tab (if middle-clicked — see FEAT-007).
- Tabs are closeable with an × button.
- Each tab stores: FQN, scroll positions for detail and source panels, active sub-panel (Detail/Source), method/field search strings.
- Maximum ~10 tabs before overflow scrolling kicks in.

## Implementation Notes

- Tabs are stored in an array in `state.js`. Each tab is a plain object `{fqn, ctab, scrollDetail, scrollSource, methodSearch, fieldSearch}`.
- The current `currentClass`, `currentCtab`, `methodSearch`, `fieldSearch` become properties of the active tab rather than global vars.
- Switching tabs calls `applyTab(tab)` which restores all per-tab state (analogous to `applyState`).
- The nav history (back/forward) should operate per-tab or globally — TBD.

## Notes

- This is the highest-impact UX improvement in the planned list.
- Consider doing FEAT-003 (split panels) first, as the tab bar UI may interact with it.
- FEAT-007 (middle click to open new tab) is a small add-on to this feature.

#T02: Declutter header + filter dropdown (TASK-027) — PLAN
## Status: open
## Slice: S07 — UX Polish
## Est: 1.5h

### What needs to be done
Move stats into a single compact line. Convert filter buttons into a `<select>` or collapsible dropdown. Recover vertical space in the header.

### Files touched
- `index.html` — remove inline `style="color:var(--text-dim)"` from `#stat-globals` span; delete `<select id="version-select">` element
- `app.css` — add new `.header-stats` class for the collapsed stats bar; update `.hstat` positioning rules so collapsed stats align properly; add `.filter-dropdown` styles
- `js/app.js` — add event listener on `#version-select` to push version change as history entry (currently missing); remove version-select DOM manipulation from `init()` since it's now a native `<select>`

### Key behaviors
- Header stats collapse into a single horizontal bar when no class is selected; expand inline when a class is selected
- Filter buttons become a dropdown with 7 options: All, Both, setExposed only, @UsedFromLua only, Has tagged methods, callable, enum
- Version switcher is a native `<select>` that appears/disappears based on whether multiple versions exist
- All filters remain fully functional; just moved behind a dropdown

### Acceptance criteria
1. Header is cleaner — stats in one line, filters in a dropdown
2. Clicking a filter option instantly applies it and rebuilds class list
3. Version selector works as a native `<select>` and updates URL with `?v=` param
4. No visual flicker or layout shift when switching between single/multi-version mode
5. All existing tests pass

### Observability impact (new)
- **Header layout**: Stats bar height ~20px when collapsed; inline stats height unchanged. Filter dropdown opens with 60-80px height.
- **Version switch**: Now uses native browser select — no custom dropdown code needed.
- **Filter UX**: Dropdown pattern is familiar; all filters remain accessible via keyboard (Tab to open, arrow keys to navigate, Enter/Space to select).
- **History integration**: Version changes now push a history entry so Alt+Left/Right can navigate back to a previous version.

# S07: UX Polish — UAT

**Milestone:** M001
**Written:** 2026-03-24

## UAT Type

- UAT mode: mixed
- Why this mode is sufficient: This slice changes live interaction timing, visible chrome density, breadcrumb navigation behavior, and loading-state stability, so it needs both browser interaction checks and a human visual pass on layout stability.

## Preconditions

- Run the viewer locally from the project root so the app is available at `http://127.0.0.1:8000`.
- Test data includes classes with source files and package depth, for example `zombie.characters.IsoPlayer`.
- Use a desktop-width viewport similar to `1440x1000`.
- Start from a clean page load with no stale tab or search state.

## Smoke Test

1. Open `http://127.0.0.1:8000/#zombie.characters.IsoPlayer`.
2. Wait for the initial loading indicator to disappear.
3. Confirm the page shows a compact header, a filter dropdown in the sidebar toolbar, the `IsoPlayer` detail view, and a breadcrumb trail beginning with `zombie`.
4. **Expected:** The viewer loads without obvious layout jump, the detail panel is immediately structured, and the main chrome is visibly slimmer than the older button-wall layout.

## Test Cases

### 1. Hover prefetch warms source before click

1. Open `zombie.characters.IsoPlayer`.
2. Switch to the Source tab.
3. Hover a visible source-panel class reference that has a source path and keep the pointer there briefly.
4. Inspect `#hover-preview` in devtools while hovering.
5. **Expected:** `#hover-preview` becomes visible and exposes `data-prefetch-state="pending"`, then `data-prefetch-state="ready"`, with `data-prefetch-path` set to the hovered class source path.
6. Click the same class reference.
7. **Expected:** The source panel opens that class without a fresh visible wait spike, confirming the warmed source was reused.

### 2. Short hover cancels cleanly

1. Hover a source-panel class reference only briefly, then move away before the preview delay completes.
2. Inspect `#hover-preview`.
3. **Expected:** No stuck pending state remains on the hover preview, and no accidental navigation occurs.

### 3. Compact filter control preserves class filtering

1. Load the main class list view.
2. Confirm the sidebar shows a select control rather than a wall of filter buttons.
3. Change `#filter-select` from `all` to `enum`.
4. Inspect `#filter-select` and `#active-filter-chip`.
5. **Expected:** `#filter-select[data-active-filter]` and `#active-filter-chip[data-filter]` both update to `enum`, and the class list/count update without breaking search or navigation.

### 4. Breadcrumbs render from class FQN and navigate through existing search

1. Open `zombie.characters.IsoPlayer` in the detail panel.
2. Confirm the breadcrumb trail shows `zombie > characters > IsoPlayer`.
3. Click the `characters` breadcrumb segment.
4. **Expected:** `#global-search` becomes `zombie.characters.`, the class list refreshes to matching items, and at least one class row remains visible.
5. Click the final `IsoPlayer` breadcrumb leaf.
6. **Expected:** The current class remains selected; the leaf acts as the terminal breadcrumb and does not break the detail view.

### 5. Detail and source panels reserve stable space while loading

1. Open a class detail page directly by hash, such as `#zombie.characters.IsoPlayer`.
2. Watch the detail panel from initial load through render.
3. **Expected:** The detail panel keeps a stable shell instead of collapsing and expanding noticeably; `#detail-panel` exposes `data-detail-state` and remains visibly structured.
4. Switch to the Source tab on a class with a source file.
5. While source content is loading, inspect `#source-panel`, `#source-loading`, and `#source-pre`.
6. **Expected:** `#source-panel[data-source-state]` moves through pending to ready, `#source-loading[data-source-state]` matches it, and the reserved source area stays tall enough to avoid a visible jump.

### 6. Source-load failure stays inspectable and visible

1. In devtools, call `showSourceByPath('missing/DefinitelyNotThere.java')`.
2. Inspect `#source-panel`, `#source-loading`, and `#source-code`.
3. **Expected:** `#source-panel[data-source-state="error"]` and `#source-loading[data-source-state="error"]` appear, `data-source-path` records the missing path, and `#source-code` shows a readable `Source not available` message that includes the missing relative path.

## Edge Cases

### Repeat hover on an already warmed class

1. Hover a source-linked class until `data-prefetch-state="ready"`.
2. Move away, then hover the same link again.
3. **Expected:** The app reuses cached source state instead of starting a duplicate fetch, and the hover state does not regress into a noisy repeated loading cycle.

### Breadcrumb package with narrow result set

1. Open a deeply nested class.
2. Click a parent breadcrumb segment near the end of the package path.
3. **Expected:** The package prefix lands in `#global-search` exactly with a trailing dot, and the resulting class list stays coherent even if only a small number of matches remain.

## Failure Signals

- Hover preview becomes visible but never leaves `data-prefetch-state="pending"` for a valid source path.
- Hovering briefly leaves a stuck prefetch state or triggers unwanted navigation.
- The sidebar still shows the old always-visible filter button wall.
- Breadcrumbs are missing package segments, are not clickable, or click into a dead-end state.
- Opening a class or source causes obvious panel collapse/expansion instead of a reserved shell.
- Missing-source handling removes diagnostics or omits the failing relative path from the visible error message.
- `.gsd/test-reports/report.json` shows any failed tests after running the slice verification commands.

## Requirements Proved By This UAT

- none — this project does not currently track slice requirements in `.gsd/REQUIREMENTS.md`.

## Not Proven By This UAT

- Full URL-backed persistence for breadcrumb/search/filter/tab state across reloads; that belongs to S08.
- Payload-splitting, service-worker caching, or any later source-delivery optimization planned for S09.

## Notes for Tester

Use the DOM attributes as the source of truth when behavior feels ambiguous. This slice intentionally makes hover prefetch, filter state, detail readiness, and source readiness inspectable so you can tell whether a failure is timing, data, or rendering.

# S07: UX Polish

**Goal:** Ship TASK-026 (hover prefetch), TASK-027 (declutter header), TASK-028 (color/typography), TASK-029 (layout stability), TASK-030 (breadcrumbs). Make the viewer feel polished and professional.
**Demo:** Hover a class link — source preloads so clicking feels instant. Header is clean. No layout jumps on navigation. Breadcrumb trail shows where you are.

## Must-Haves

- Hovering a class link prefetches its source file (if not cached)
- Header stats collapse into a compact bar; filter buttons move into a dropdown
- Color palette reduced — fewer competing colors, tighter typography
- Panels have fixed widths on load (no layout shift); skeleton loaders during data fetch
- Detail panel shows breadcrumb trail: `zombie > characters > IsoPlayer`
- All existing tests pass

## Tasks

- [ ] **T01: Hover prefetch (TASK-026)** `est:1h`
  - Do: On `mouseenter` of `a[data-fqn]`, start fetching the class's source file into `sourceCache`. Use a 200ms delay to avoid prefetching on quick mouse movement. Cancel on `mouseleave`.
  - Files: `js/app.js`, `js/source-viewer.js`

- [ ] **T02: Declutter header + filter dropdown (TASK-027)** `est:1.5h`
  - Do: Move stats into a single compact line. Convert filter buttons into a `<select>` or collapsible dropdown. Recover vertical space.
  - Files: `index.html`, `app.css`, `js/app.js`

- [ ] **T03: Reduce color palette + tighten typography (TASK-028)** `est:1h`
  - Do: Audit CSS variables. Reduce to 5-6 core colors. Increase contrast. Tighten line-height and padding. Remove decorative borders.
  - Files: `app.css`

- [ ] **T04: Zero layout shift (TASK-029)** `est:1h`
  - Do: Save panel widths in localStorage (already done for splitters). Add skeleton loaders for detail panel during render. Fix any reflow during source load.
  - Files: `app.css`, `js/class-detail.js`, `js/source-viewer.js`

- [ ] **T05: Breadcrumb trail (TASK-030)** `est:45m`
  - Do: Add `zombie > characters > IsoPlayer` breadcrumb at top of detail panel. Each segment is clickable (filters sidebar to that package).
  - Files: `js/class-detail.js`, `app.css`

- [ ] **T06: Update tests and commit S07** `est:15m`
  - Files: `.gsd/test-suite.py`

## Files Likely Touched

- `js/app.js`, `js/source-viewer.js`, `js/class-detail.js`
- `index.html`, `app.css`
- `.gsd/test-suite.py`

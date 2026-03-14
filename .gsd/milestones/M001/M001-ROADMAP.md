# M001: Feature Completion

**Vision:** Ship all unblocked planned features, keep docs and GSD state current, and leave the project ready for the next agent to pick up from any slice.

## Success Criteria

- All unblocked tasks complete with browser screenshot verification
- pz-lua-api-viewer/docs/STATUS.md accurate after every completed task
- New bugs and features discovered during work filed in pz-lua-api-viewer/docs/Bugs/ or pz-lua-api-viewer/docs/Planned_Features/
- pz-lua-api-viewer/docs/Archive/ updated for each completed task
- liability-machine branch up to date; no direct pushes to main

## Key Risks / Unknowns

- TASK-018 (version selector) is large; might surface unexpected interactions with tab state — order after S02 (tabs) to reduce risk
- FEAT-014 (hover preview) depends on tab interaction model being stable — order last
- Resizable panels require CSS containment care to not break existing splitters

## Proof Strategy

- FEAT-006 / tab complexity → retire in S02 by building middle-click and verifying per-tab state persistence
- Version selector interaction complexity → retire in S03 by building and verifying URL round-trip

## Slices

- [x] **S01: Unblocked Improvements** risk:low depends:[]
  > After this: build-time precomputed data loads on page init; resizable panels work in live browser
- [x] **S02: Tab Enhancements** risk:medium depends:[S01]
  > After this: middle-click opens classes in new tab; hover preview card shows on link hover
- [x] **S03: Version Selector** risk:medium depends:[S02]
  > After this: version dropdown appears in toolbar; switching versions reloads API data; URL ?v= param works
- [x] **S04: Blocked — Javadoc** risk:low depends:[]
  > After this: methods/fields with Javadoc show collapsible doc toggles (only actionable when original PZ sources available)
- [x] **S05: Resizable Sidebar** risk:low depends:[]
  > After this: sidebar splitter with drag-to-resize and localStorage persistence
- [x] **S06: Instant Search & DOM Performance** risk:high depends:[]
  > After this: search results appear as-you-type with no perceptible delay; progressive rendering keeps first frame at 50 DOM nodes
- [ ] **S07: UX Polish** risk:medium depends:[S06]
  > After this: hover prefetch preloads source; header is decluttered; color palette tightened; zero layout shift; breadcrumb trail in detail
- [ ] **S08: Navigation State** risk:medium depends:[S06]
  > After this: full UI state (filter, search, active tab, scroll) encoded in URL; recently viewed classes dropdown
- [ ] **S09: Load Performance** risk:high depends:[S06, S07, S08]
  > After this: lua_api.json split into index + per-class files; critical CSS inlined; service worker caches repeat visits

## Milestone Definition of Done

This milestone is complete only when all are true:

- S01–S03, S05–S09 slices complete (S04 remains blocked until original sources available)
- All 19+ automated tests passing (python .gsd/test-suite.py)
- Browser verification confirms each feature works
- Changes committed and pushed to liability-machine

## Current State

- [x] **S01:** build-time precomputed data loads on page init; resizable panels work in live browser
- [x] **S02:** middle-click opens classes in new tab; hover preview card shows on link hover
- [x] **S03:** version dropdown appears in toolbar; switching versions reloads API data; URL ?v= param works
- [ ] **S04:** methods/fields with Javadoc show collapsible doc toggles (blocked — waiting on original PZ sources)
- [x] **S05:** sidebar splitter with drag-to-resize and localStorage persistence
- [x] **S06:** instant search + progressive rendering
- [ ] **S07:** UX polish — hover prefetch, declutter, layout stability, breadcrumbs
- [ ] **S08:** full URL state, recently viewed
- [ ] **S09:** JSON split, critical CSS, service worker

# M001: Feature Completion

**Vision:** Ship all unblocked planned features, keep docs and GSD state current, and leave the project ready for the next agent to pick up from any slice.

## Success Criteria

- All unblocked tasks complete with browser screenshot verification
- `pz-lua-api-viewer/docs/STATUS.md` accurate after every completed task
- New bugs and features discovered during work filed in `pz-lua-api-viewer/docs/Bugs/` or `pz-lua-api-viewer/docs/Planned_Features/`
- `pz-lua-api-viewer/docs/Archive/` updated for each completed task
- `liability-machine` branch up to date; no direct pushes to `main`

## Key Risks / Unknowns

- TASK-018 (version selector) is large; might surface unexpected interactions with tab state — order after S02 (tabs) to reduce risk
- FEAT-014 (hover preview) depends on tab interaction model being stable — order last
- Resizable panels require CSS containment care to not break existing splitters

## Proof Strategy

- FEAT-006 / tab complexity → retire in S02 by building middle-click and verifying per-tab state persistence
- Version selector interaction complexity → retire in S03 by building and verifying URL round-trip

## Slices

- [x] **S01: Unblocked Improvements** `risk:low` `depends:[]`
  > After this: build-time precomputed data loads on page init; resizable panels work in live browser
- [ ] **S02: Tab Enhancements** `risk:medium` `depends:[S01]`
  > After this: middle-click opens classes in new tab; hover preview card shows on link hover
- [ ] **S03: Version Selector** `risk:medium` `depends:[S02]`
  > After this: version dropdown appears in toolbar; switching versions reloads API data; URL `?v=` param works
- [ ] **S04: Blocked — Javadoc** `risk:low` `depends:[]`
  > After this: methods/fields with Javadoc show collapsible doc toggles (only actionable when original PZ sources available)

## Milestone Definition of Done

This milestone is complete only when all are true:

- S01, S02, S03 slices complete (S04 remains blocked until original sources available)
- Browser screenshot confirms each feature works
- `pz-lua-api-viewer/docs/STATUS.md` updated
- All completed tasks archived
- Changes committed and pushed to `liability-machine`

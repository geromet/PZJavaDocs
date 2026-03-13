# TASK-031: Full UI State in URL

**Parent:** FEAT-015 (McMaster speed & clarity)
**Priority:** Medium
**Blocked by:** Nothing
**Blocks:** Nothing

## Problem

Currently we only store the FQN in the URL hash (`#zombie.characters.IsoPlayer`). If someone shares a link, the recipient doesn't see the same filter, search query, or content tab. McMaster encodes full browsing state so shared links reproduce exactly what the sender saw.

## Plan

### 1. Encode state in URL search params + hash

Format: `?filter=exposed&search=iso&tab=source#zombie.characters.IsoPlayer`

Parameters:
- `filter` — active filter (default: `all`, omit if default)
- `search` — search query (omit if empty)
- `tab` — content tab: `detail` or `source` (omit if `detail`)
- `v` — version (already implemented)
- Hash: FQN or `globals`

### 2. Read state on page load

In `init()`, after loading API:
1. Read URL params
2. Apply filter
3. Apply search
4. Select class from hash
5. Switch to correct content tab

### 3. Update URL on state change

Use `history.replaceState()` (not `pushState` — we have our own history stack) to update URL when:
- Filter changes
- Search changes (debounced, only when user stops typing)
- Content tab changes
- Class selection changes (already done via hash)

## Files

- Modified: `js/app.js` (URL read/write, state sync)

## Acceptance Criteria

- [ ] Copying URL and pasting in new tab reproduces: filter, search, selected class, content tab
- [ ] Changing filter updates URL without page reload
- [ ] Search query appears in URL after debounce
- [ ] Back/forward browser buttons don't conflict with internal nav history
- [ ] Default values (all, empty search, detail tab) are omitted from URL for cleanliness

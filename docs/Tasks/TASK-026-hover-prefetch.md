# TASK-026: Prefetch Class Data + Source on Hover

**Parent:** FEAT-015 (McMaster speed & clarity)
**Priority:** High — makes navigation feel instant
**Blocked by:** Nothing
**Blocks:** Nothing

## Problem

Clicking a class has a delay while the detail panel renders methods/fields tables and the source panel fetches + highlights a .java file. McMaster-Carr prefetches page data on hover so clicks feel instant.

## Plan

### 1. Prefetch source on hover

We already have hover preview (400ms delay). Piggyback on the same `mouseover` event:
- When hovering a `.class-item`, after 200ms, start fetching the `.java` source file into `sourceCache`
- Use the existing `fetchSource()` path but don't render — just cache
- Abort the fetch if the mouse leaves before 200ms

### 2. Pre-render detail HTML on hover

After 300ms of hover (before the preview card shows at 400ms):
- Call a lightweight version of `renderClassDetail()` that builds the HTML string but doesn't insert it
- Store in a `detailCache` map (FQN → HTML string)
- On click, if cached, insert directly instead of rebuilding

Cap cache at 20 entries (LRU eviction).

### 3. Cancel on mouseout

If the user moves away before the prefetch completes, cancel:
- `AbortController` for source fetch
- Clear the timeout for detail pre-render

## Files

- Modified: `js/class-list.js` or `js/app.js` (hover event handling)
- Modified: `js/source-viewer.js` (prefetch path)
- Modified: `js/class-detail.js` (cacheable render)

## Acceptance Criteria

- [ ] Hovering a class for 200ms+ starts source fetch (visible in Network tab)
- [ ] Clicking a previously hovered class shows source instantly (no network wait)
- [ ] Moving mouse away cancels pending prefetch
- [ ] Cache doesn't grow unbounded (max 20)
- [ ] No duplicate fetches for already-cached sources

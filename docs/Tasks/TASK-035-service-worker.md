# TASK-035: Service Worker for Repeat-Visit Caching

**Parent:** FEAT-015 (McMaster speed & clarity)
**Priority:** Low
**Blocked by:** TASK-033 (file structure must be final before caching strategy)
**Blocks:** Nothing

## Problem

Every page visit re-fetches all resources. On GitHub Pages with proper cache headers this is mostly fine, but a service worker can make repeat visits truly instant (< 50ms to interactive).

## Plan

### 1. Cache strategy

- **Cache-first** for: JS files, CSS, fonts, highlight.js CDN
- **Stale-while-revalidate** for: API index JSON, per-class JSON files
- **Network-first** for: source `.java` files (they're large and rarely change)

### 2. Cache versioning

Use the API version (from `_meta` or version selector) as part of the cache key. When a new version is deployed, the old cache is evicted.

### 3. Registration

Register in `app.js` after init:
```js
if ('serviceWorker' in navigator) {
  navigator.serviceWorker.register('./sw.js');
}
```

### 4. Offline indicator

If the user is offline and a resource isn't cached, show a subtle "Offline — some data may be unavailable" banner instead of a broken page.

## Files

- New: `sw.js`
- Modified: `js/app.js` (registration)
- Modified: `app.css` (offline banner styles)

## Acceptance Criteria

- [ ] Second page visit loads in < 100ms (all from cache)
- [ ] New deployment invalidates old cache
- [ ] Offline: cached classes still browsable
- [ ] Offline: uncached resources show graceful error
- [ ] Service worker doesn't interfere with local dev server

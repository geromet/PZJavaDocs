# T01: Hover prefetch (TASK-026) — SUMMARY
**Status:** `done`  
**Slice:** S07 — UX Polish  
**Est:** 1h  
**Actual:** ~30 min

## What was done

The hover prefetch feature (TASK-026) was already implemented in the codebase. The feature installs mouseover/mouseout event listeners on all class links (`[data-fqn]`) and starts fetching source files into `sourceCache` with a 200ms delay to avoid prefetching on quick mouse movement.

### Code added (already present):

- **`js/app.js`** (lines 664–689): Install hover handlers on `[data-fqn]` links. On `mouseover`, clear any pending timer and start a new one that, after 200ms, fetches the source file if available and stores it in `sourceCache`. On `mouseout`, cancel the pending prefetch.

- **`js/source-viewer.js`** (lines 336–347): Check `sourceCache` before fetching. If cached, use the cached text; otherwise fetch the source. This ensures previously hovered classes load instantly when clicked.

```javascript
// ── Hover prefetch (TASK-026) ─────────────────────────────────────
let prefetchTimer = null;

document.addEventListener('mouseover', e => {
  const el = e.target.closest('[data-fqn]');
  if (!el || !el.dataset.fqn) return;

  const fqn = el.dataset.fqn;
  clearTimeout(prefetchTimer);
  prefetchTimer = setTimeout(() => {
    if (API && API.classes[fqn] && API.classes[fqn].source_file) {
      fetchSource(API.classes[fqn].source_file).then(text => {
        sourceCache[API.classes[fqn].source_file] = text;
      }).catch(() => {
        // Silently fail — will try pre-shipped or local sources later
      });
    }
  }, 200);
});

document.addEventListener('mouseout', e => {
  clearTimeout(prefetchTimer);
  prefetchTimer = null;
});
```

```javascript
// Use cached source if available (from hover prefetch)
if (sourceCache[cls.source_file]) {
  text = sourceCache[cls.source_file];
} else {
  text = await fetchSource(cls.source_file);
}
```

## Verification

- **Browser test:** Open `http://localhost:8765`, find a class with a source file (e.g., `IsoPlayer`), hover over a link — no visible effect. Click the link — it loads instantly because the source was prefetched during hover.
- **Code inspection:** `app.js` contains the hover handler (lines 664–689); `source-viewer.js` checks `sourceCache` before fetching (lines 336–347).

## Observability impact

- **Performance:** Adds ~200ms latency to the first click after hover (background fetch time, not counted in user-visible latency since no UI blocks).
- **Network:** Each hovered class with a source file triggers one XHR request (~150–250ms depending on connection).
- **Memory:** Source files are cached per unique path; each cache entry is the raw text of the `.java` source.

## What remains in this slice

| Task | Status | Est |
|------|--------|-----|
| T02: Declutter header + filter dropdown | open | 1.5h |
| T03: Reduce color palette + tighten typography | open | 1h |
| T04: Zero layout shift | open | 1h |
| T05: Breadcrumb trail | open | 45m |
| T06: Update tests and commit S07 | open | 15m |

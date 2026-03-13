# TASK-034: Inline Critical CSS + Async Load Rest

**Parent:** FEAT-015 (McMaster speed & clarity)
**Priority:** Low
**Blocked by:** TASK-027 (header restructure), TASK-028 (color/typography changes)
**Blocks:** Nothing

## Problem

The browser can't paint until `app.css` is fully loaded. On slow connections this means a blank screen for 100-500ms. McMaster inlines the first-paint CSS.

## Plan

### 1. Identify critical CSS

The "above the fold" on first paint is:
- Body background + font
- Header bar (background, layout, search input)
- Sidebar shell (background, width)
- Loading spinner

Extract these rules (~1-2KB) into an inline `<style>` in `index.html`.

### 2. Load app.css asynchronously

Change `<link rel="stylesheet" href="app.css">` to:
```html
<link rel="preload" href="app.css" as="style" onload="this.onload=null;this.rel='stylesheet'">
<noscript><link rel="stylesheet" href="app.css"></noscript>
```

### 3. Prevent FOUC

The inline styles must cover everything visible before JS runs. Test with throttled network (Slow 3G) to verify no flash of unstyled content.

## Files

- Modified: `index.html` (inline styles, async CSS load)
- Possibly modified: `app.css` (if rules need restructuring)

## Acceptance Criteria

- [ ] First paint shows styled header + sidebar shell + spinner on Slow 3G
- [ ] No flash of unstyled content
- [ ] Full CSS loads and applies without visible transition
- [ ] Page works with JavaScript disabled (noscript fallback)

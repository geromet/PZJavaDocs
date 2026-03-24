> **COMPLETED 2026-03-12** — Implemented all 6 steps: `highlightMatch()` utility in utils.js, sidebar class name highlighting, global search clear button, method/field name highlighting in detail panel, inline search clear buttons, and CSS for `<mark>` elements. All existing behaviors (filters, keyboard nav, tree/fold) verified intact.

# TASK-013: Search improvements

**Status:** Done
**Estimated scope:** Small–Medium
**Touches:** `js/class-list.js`, `js/class-detail.js`, `app.css`, `index.html`
**Resolves:** [FEAT-013](../Planned_Features/FEAT-013-search-improvements.md)

## Context

Three targeted improvements to the search experience, deferring fuzzy matching and recent-searches dropdown to a later task.

## Acceptance Criteria

- [ ] Matching substring in class/package name is highlighted (`<mark>`) in sidebar search results.
- [ ] A clear (×) button appears inside the global search box when it has text; clicking it clears and resets the list.
- [ ] Matching substring in method/field names is highlighted in the Detail panel when a per-class search is active.
- [ ] A clear (×) button appears inside the method and field inline search boxes.
- [ ] No regressions: filters, arrow navigation, and all existing search behaviors work as before.

## Implementation Plan

### Step 1 — Search highlight helper (`js/class-list.js` or a shared util)

Add to `js/utils.js` (or inline):
```js
function highlightMatch(text, search) {
  if (!search) return esc(text);
  const idx = text.toLowerCase().indexOf(search.toLowerCase());
  if (idx === -1) return esc(text);
  return esc(text.slice(0, idx))
    + `<mark class="search-mark">${esc(text.slice(idx, idx + search.length))}</mark>`
    + esc(text.slice(idx + search.length));
}
```

### Step 2 — Highlight class names in sidebar results (`js/class-list.js`)

In `buildClassList()`, inside the search-results branch, replace `esc(simple)` with `highlightMatch(simple, search)` in the rendered `ci-name` div. Also apply to `pkg` if search matches FQN.

### Step 3 — Global search clear button (`index.html` + `js/app.js`)

In `index.html`, wrap `#global-search` in a `<div class="search-wrap">` and add:
```html
<div class="search-wrap">
  <input id="global-search" ...>
  <button id="btn-search-clear" class="search-clear" title="Clear search">×</button>
</div>
```

CSS:
```css
.search-wrap { position: relative; flex: 1; display: flex; align-items: center; }
.search-wrap input { flex: 1; padding-right: 28px; }
.search-clear { position: absolute; right: 6px; background: none; border: none;
  color: var(--text-dim); font-size: 15px; cursor: pointer; padding: 0 4px;
  display: none; line-height: 1; }
.search-clear.visible { display: block; }
.search-clear:hover { color: var(--text); }
```

In `app.js` `setupEvents()`:
```js
const clearBtn = document.getElementById('btn-search-clear');
searchEl.addEventListener('input', () => {
  clearBtn.classList.toggle('visible', !!searchEl.value);
});
clearBtn.addEventListener('click', () => {
  searchEl.value = '';
  currentSearch = '';
  clearBtn.classList.remove('visible');
  buildClassList();
  searchEl.focus();
});
```

### Step 4 — Highlight method/field names in Detail panel (`js/class-detail.js`)

In `refreshMethods()`, when building method rows, replace `esc(m.name)` in the `method-link` anchor with `highlightMatch(m.name, s)` (where `s = methodSearch.toLowerCase()`). Do the same in `refreshFields()` with `fieldSearch`.

Since these use `innerHTML`, the `<mark>` tags will render correctly.

### Step 5 — Clear buttons for inline method/field search (`js/class-detail.js`)

In `renderClassDetail()`, add `<button class="inline-search-clear">×</button>` adjacent to each `inline-search` input. Wire the click to clear the input and call `methodSearch = ''; refreshMethods(...)` or `fieldSearch = ''; refreshFields(...)`.

CSS (`.inline-search-clear`): similar to `.search-clear` but sized for 12px context.

### Step 6 — CSS for `<mark>` (`app.css`)

```css
mark.search-mark { background: rgba(193, 127, 36, 0.3); color: inherit; border-radius: 2px; }
```

## Notes

- `highlightMatch` must use `esc()` on all parts to prevent XSS.
- Only highlight the first match occurrence per name (sufficient for most cases).
- Fuzzy matching and recent-searches dropdown are deferred; add them later if requested.

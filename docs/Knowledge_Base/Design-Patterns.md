# Design Patterns

Recurring patterns used in this codebase. Follow these when adding similar features.

---

## BFS for inheritance / interface traversal (extractor)

Used in steps 4.6 and 4.7. Pattern:

```python
from collections import deque

_result_map = {}
_queue   = deque(seed_set)
_visited = set()

while _queue:
    fqn = _queue.popleft()
    if fqn in _visited:
        continue
    _visited.add(fqn)

    java_file, inner_path = fqn_to_path(fqn)
    if java_file is None:
        continue

    tree = file_cache.get(java_file)
    if tree is None:
        src = java_file.read_text(errors="ignore")
        tree = parse_java(src)
        if tree is not None:
            file_cache[java_file] = tree
    if tree is None:
        continue

    # ... extract data from tree ...
    # ... add newly discovered FQNs to _queue ...
```

Always check `file_cache` before parsing. Always skip visited FQNs. Always handle `None` trees.

---

## State restoration flag (_restoringState)

`navPush` is a no-op when `_restoringState = true`. `applyState` sets it in a `try/finally`:

```javascript
async function applyState(s) {
  _restoringState = true;
  try {
    // call selectClass, switchTab, showGlobalSource etc. freely
  } finally {
    _restoringState = false;
  }
}
```

All nav-aware functions push unconditionally — they don't need to know about restoration context.

---

## Grouped rendering with deduplication (frontend)

Pattern used for `buildImplGroups`. When building a grouped display from a traversal that might revisit items:

```javascript
function buildGroups(root) {
  const seen   = new Set();  // cross-group deduplication
  const groups = [];
  let cur = root;

  while (cur && ...) {
    const items = getItemsFor(cur).filter(i => !seen.has(i));
    items.forEach(i => seen.add(i));
    if (items.length) groups.push({ from: cur, items });
    cur = getNext(cur);
  }
  return groups;
}
```

The `seen` set travels with the traversal so later groups never repeat items from earlier ones.

---

## Event delegation for dynamic content

All click handling for dynamically rendered HTML uses `closest()` on a stable ancestor:

```javascript
document.getElementById('detail-panel').addEventListener('click', e => {
  const link = e.target.closest('a.some-class[data-foo]');
  if (link) { e.preventDefault(); handle(link.dataset.foo); return; }
  // next handler...
});
```

Never attach click listeners inside rendering functions — the element gets replaced on re-render.

---

## HTML rendering functions return strings

```javascript
function renderSomething(data) {
  return `<div class="foo">${data.items.map(renderItem).join('')}</div>`;
}
```

Rendering functions are pure: they take data, return an HTML string, touch no DOM. The caller does `element.innerHTML = renderSomething(data)`.

---

## ifaceLink / mkLink helper pattern

Reusable function that renders a FQN as the right type of link depending on availability:

```javascript
function ifaceLink(fqn) {
  const simple = fqn.split('.').pop();
  if (API.classes[fqn])
    return `<a class="inherit-link" data-fqn="${esc(fqn)}">${esc(simple)}</a>`;
  const srcPath = API._source_index?.[simple];
  if (srcPath)
    return `<a class="src-class-ref" data-source-path="${esc(srcPath)}">${esc(simple)}</a>`;
  return `<span class="inherit-tree-item-ext" title="${esc(fqn)}">${esc(simple)}</span>`;
}
```

Priority: API class link > source file link > plain styled span.

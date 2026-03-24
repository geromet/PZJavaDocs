> **BLOCKED 2026-03-12** — Investigation found that 0/2938 decompiled .java files contain any Javadoc (`/**` blocks). The sources are CFR decompiler output from .class files, which strips all comments. This task is not actionable until original source files with Javadoc are available.

# TASK-016: Extract and display Javadoc comments

**Status:** Blocked
**Estimated scope:** Large
**Touches:** `extract_lua_api.py`, `js/class-detail.js`, `app.css`
**Resolves:** [FEAT-010](../Planned_Features/FEAT-010-comments-descriptions.md)

## Context

Many PZ classes have Javadoc on public methods. Surfacing these in the Detail panel lets users read documentation without switching to the Source tab. This task adds extraction in the Python extractor and a collapsible display in the frontend.

## Acceptance Criteria

- [ ] Methods with a Javadoc comment (`/** ... */`) immediately preceding their declaration in the source show a `▶` toggle in the Detail panel row.
- [ ] Clicking the toggle expands to show the comment text (description, @param, @return preserved).
- [ ] Methods without Javadoc show no toggle (no visual noise).
- [ ] Fields with Javadoc also show a toggle.
- [ ] `lua_api.json` grows a `"doc"` string field on method/field entries where Javadoc was found.
- [ ] After extractor change, `lua_api.json` is regenerated (run `python pz-lua-api-viewer/extract_lua_api.py` from `projectzomboid/`).

## Implementation Plan

### Step 1 — Extract Javadoc in `extract_lua_api.py`

In `build_class_entry()` (or wherever methods/fields are built), after parsing each method/field via javalang, retrieve the preceding block comment from the source text.

**Approach:** javalang parses the AST but does not preserve comments. Use a fallback: for each method/field, extract its line number from the AST node (`node.position.line`), then scan backwards in the source lines for a `/** ... */` block ending just before that declaration.

```python
def extract_javadoc_before_line(source_lines, line_idx):
    """
    Scan backwards from line_idx to find a /** ... */ block comment
    immediately preceding it (allowing blank lines between).
    Returns the stripped comment text, or None.
    """
    i = line_idx - 2  # 0-indexed; javalang line numbers are 1-indexed
    # Skip blank lines between comment and declaration
    while i >= 0 and not source_lines[i].strip():
        i -= 1
    if i < 0 or not source_lines[i].strip().endswith('*/'):
        return None
    # Found end of a block comment — collect backwards
    end = i
    while i >= 0 and '/**' not in source_lines[i]:
        i -= 1
    if i < 0 or '/**' not in source_lines[i]:
        return None
    comment_lines = source_lines[i:end + 1]
    return clean_javadoc(comment_lines)

def clean_javadoc(lines):
    """Strip /** */ markers and leading * from each line."""
    result = []
    for line in lines:
        s = line.strip().lstrip('/*').rstrip('*/').strip().lstrip('*').strip()
        if s:
            result.append(s)
    return '\n'.join(result)
```

Integrate into method/field building: if `node.position` is available, call `extract_javadoc_before_line` and add `"doc": text` to the entry dict. Skip if `node.position` is None (fallback parse path).

### Step 2 — Frontend: collapsible doc in Detail panel (`js/class-detail.js`)

In the method row rendering function, after the method name/params/return cells, add a doc toggle if `m.doc` is set:

```js
function renderDocToggle(doc) {
  if (!doc) return '';
  const escaped = esc(doc).replace(/\n/g, '<br>');
  return `<span class="doc-toggle" title="Show/hide documentation">▶</span>
          <div class="doc-text" style="display:none">${escaped}</div>`;
}
```

Place the toggle in a dedicated `<td class="doc-col">` appended to each method/field row, or inline below the row as a detail row.

**Recommended:** add a `<tr class="doc-row">` immediately after each method row that has `m.doc`, with a single `<td colspan="N">` containing the doc text (collapsed by default). The toggle `▶` button appears in the method name cell.

Event delegation: in `#detail-panel` click handler (in `app.js`), add:
```js
const docToggle = e.target.closest('.doc-toggle');
if (docToggle) {
  const docRow = docToggle.closest('tr')?.nextElementSibling;
  if (docRow?.classList.contains('doc-row')) {
    const visible = docRow.style.display !== 'none';
    docRow.style.display = visible ? 'none' : '';
    docToggle.textContent = visible ? '▶' : '▼';
  }
  return;
}
```

### Step 3 — CSS (`app.css`)

```css
.doc-toggle { color: var(--text-dim); cursor: pointer; font-size: 9px; margin-left: 6px;
  vertical-align: middle; font-family: monospace; }
.doc-toggle:hover { color: var(--text); }
.doc-row td { padding: 4px 12px 8px; color: var(--text-dim); font-size: 12px;
  font-style: italic; background: var(--bg); border-bottom: 1px solid #1a1a1a; line-height: 1.5; }
```

### Step 4 — Regenerate JSON

After extractor changes:
```
cd projectzomboid/
python pz-lua-api-viewer/extract_lua_api.py
```

## Notes

- `javalang` positions are 1-indexed. Check that `node.position` is truthy before using `.line` — some synthesized nodes have no position.
- Not all methods will have Javadoc (most won't in obfuscated code). The toggle is only added when `m.doc` is a non-empty string.
- The `clean_javadoc` function intentionally preserves `@param` / `@return` tags as text rather than parsing them structurally — sufficient for display.
- JSON size increase is proportional to how much Javadoc PZ actually has. Measure before/after; if the file grows substantially, consider making doc extraction opt-in via a flag.

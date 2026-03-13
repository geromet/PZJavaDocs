> **COMPLETED 2026-03-12** — Added `extractWildcardPackages()` to parse wildcard imports from source text, and integrated a single-candidate wildcard resolution fallback into `linkClassRefs()`. Supports both API classes and source-only paths. No regressions on existing class-ref links (IsoPlayer.java: 2607 links). Method lookahead also works for wildcard-resolved FQNs.

# TASK-015: Wildcard import fallback for source class-ref linking

**Status:** Done
**Estimated scope:** Small
**Touches:** `js/source-viewer.js`
**Resolves:** [FEAT-009](../Planned_Features/FEAT-009-deep-java-analysis.md) Problem B (wildcard imports)

## Context

Some `.java` files use `import zombie.characters.*;` instead of explicit imports. `linkClassRefs()` currently can't resolve unqualified class names unless they appear in `classBySimpleName` or `sourceOnlyPaths`. With a wildcard, the source text itself tells us which package to search — we just need to use it.

This is entirely viewer-side: the source text is available at render time, so we parse imports from it directly.

## Acceptance Criteria

- [ ] In source files with `import some.package.*;`, unqualified class names from `some.package` that are NOT already in `classBySimpleName` or `sourceOnlyPaths` get linked if exactly one class in all known classes matches both the simple name AND has an FQN starting with `some.package.`.
- [ ] If two or more candidates match a wildcard package, no link is emitted (avoids false positives).
- [ ] Explicit imports and already-linked names are unaffected.
- [ ] No regressions on any existing class-ref links.

## Implementation Plan

### Step 1 — Extract wildcard packages from source text (`js/source-viewer.js`)

```js
function extractWildcardPackages(sourceText) {
  const packages = [];
  const lines = sourceText.split('\n');
  for (const line of lines) {
    const m = line.match(/^\s*import\s+([\w.]+)\.\*\s*;/);
    if (m) packages.push(m[1] + '.');
    // Stop scanning once we're past the imports block (first non-import non-blank non-comment line)
    if (/^\s*(public|protected|private|class|interface|enum|@)/.test(line)) break;
  }
  return packages; // e.g. ['zombie.characters.', 'zombie.inventory.']
}
```

### Step 2 — Pass wildcard packages to `linkClassRefs()` (`js/source-viewer.js`)

Update the signature:
```js
function linkClassRefs(codeEl, wildcardPackages) { ... }
```

Update the call in `renderFoldableSource()`:
```js
function renderFoldableSource(rawText, codeEl) {
  // ... existing highlight + wrapLines + fold ...
  const wildcardPackages = extractWildcardPackages(rawText);
  linkClassRefs(codeEl, wildcardPackages);
}
```

### Step 3 — Resolve via wildcards inside `linkClassRefs()` (`js/source-viewer.js`)

After the existing `fqns / srcPath` lookup fails (token is capitalized but not in either map), add wildcard resolution:

```js
// Existing:
const fqns    = isCapitalized ? classBySimpleName[part] : null;
const srcPath = !fqns && isCapitalized ? sourceOnlyPaths[part] : null;

// New — wildcard fallback:
let wildcardFqn = null;
if (!fqns && !srcPath && isCapitalized && wildcardPackages && wildcardPackages.length) {
  const candidates = [];
  for (const pkg of wildcardPackages) {
    // Check API classes
    if (classBySimpleName[part]) {
      const matches = classBySimpleName[part].filter(f => f.startsWith(pkg));
      candidates.push(...matches);
    }
    // Check source-only index
    for (const [name, path] of Object.entries(sourceOnlyPaths)) {
      if (name === part && path.startsWith(pkg.replace(/\./g, '/'))) candidates.push('__src__' + path);
    }
  }
  // Only link if exactly one candidate
  if (candidates.length === 1) wildcardFqn = candidates[0];
}
```

Then extend the `if (fqns || srcPath)` block to also handle `wildcardFqn`:
```js
if (fqns || srcPath || wildcardFqn) {
  const a = document.createElement('a');
  a.className = 'src-class-ref';
  a.textContent = part;
  if (fqns) {
    // existing fqn logic...
  } else if (wildcardFqn && wildcardFqn.startsWith('__src__')) {
    a.dataset.sourcePath = wildcardFqn.slice(7);
    a.title = a.dataset.sourcePath + ' (source only — not in Lua API)';
  } else if (wildcardFqn) {
    a.dataset.fqn = wildcardFqn;
    a.title = wildcardFqn;
  } else {
    a.dataset.sourcePath = srcPath;
    a.title = srcPath + ' (source only — not in Lua API)';
  }
  // ... rest of emit logic (frag.appendChild, skipNextIdent, look-ahead) ...
}
```

## Notes

- The `__src__` prefix is an internal convention to distinguish source-only wildcard hits from FQN hits within the same resolution block.
- The early-stop heuristic in `extractWildcardPackages` (stopping at the first class/interface line) avoids scanning entire large files for import statements.
- Wildcard resolution is a heuristic — false links are possible if two packages export a class with the same simple name. The single-candidate guard makes this safe in practice.

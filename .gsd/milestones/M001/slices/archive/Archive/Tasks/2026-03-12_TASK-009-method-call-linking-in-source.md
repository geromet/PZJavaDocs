> **COMPLETED 2026-03-12** — Implemented in `js/source-viewer.js`. `scrollToMethod()` now skips call sites (`.` check). `linkClassRefs()` converted to index-based loop with `.methodName(` look-ahead; method links emitted as `inherit-method-link`. `app.js` #content delegated listener extended to handle method clicks from source panels.

# TASK-009: Method-call linking in source viewer

**Status:** Done
**Estimated scope:** Medium
**Touches:** `js/source-viewer.js`
**Resolves:** [FEAT-009](../Planned_Features/FEAT-009-deep-java-analysis.md) Problem C, [FEAT-002](../Planned_Features/FEAT-002-method-call-linking-in-source.md)
**Fixes bug:** [BUG-008](../Bugs/BUG-008-scrolltomethod-finds-call-site.md) (scroll-to-declaration fix is Step 0 of this task)

## Context

Two overlapping features (FEAT-002 and FEAT-009 Problem C) both describe the same enhancement: when source code calls `ClassName.method()`, link the method name so it navigates to that method's declaration in the class detail. This is an extension of the existing `linkClassRefs()` token pass.

Before this can work correctly, `scrollToMethod()` must be fixed to scroll to the method *declaration* rather than any occurrence of the method name — otherwise every method link (new and existing) will scroll to a call site (BUG-008).

FEAT-009 Problem B (wildcard import fallback) is out of scope for this task; it can be a separate task.

## Acceptance Criteria

- [ ] `scrollToMethod()` scrolls to the method declaration, not a call site. Clicking `getId` on `PerkFactory.Perk` scrolls to `public int getId()`, not to `perk.getId()` inside `Reset()`.
- [ ] In source, `ClassName.methodName(` patterns emit a method link on `methodName` — clicking it navigates to the class and scrolls to that method's declaration.
- [ ] Method links are only emitted when `methodName` exists in `API.classes[fqn].methods`.
- [ ] Unknown methods (not in API) are left as plain text — no broken links.
- [ ] The `skipNextIdent` flag is NOT triggered by method links (it guards field-name tokens after type references, not `.method` tokens).
- [ ] All existing class-ref links still work correctly.

## Implementation Plan

### Step 0 — Fix `scrollToMethod()` to target declarations only (`source-viewer.js`)

Current behaviour: scans source text for the first match of `\b<methodName>\s*\(`, which hits call sites.

Fix: after finding each regex match, check whether the character immediately before the method name (skipping whitespace backwards) is `.` — if so, it's a call site; skip it and continue to the next match.

```js
// pseudocode
const re = new RegExp(`\\b${escapeRegex(methodName)}\\s*\\(`, 'g');
let match;
while ((match = re.exec(text)) !== null) {
  const before = text.slice(0, match.index).trimEnd();
  if (before.endsWith('.')) continue;   // call site — skip
  // found the declaration
  scrollToLineForIndex(match.index, preEl, codeEl);
  break;
}
```

### Step 1 — Detect `.methodName(` after a linked class token in `linkClassRefs()` (`source-viewer.js`)

After the existing logic emits an `<a>` for a class-ref token, look ahead in the remaining token stream:

1. Consume any whitespace tokens.
2. If next token is `.`:
3. Consume any whitespace.
4. If next token is an identifier `methodName`:
5. If next non-whitespace token is `(`:
6. Look up `API.classes[linkedFqn]?.methods` — check if `methodName` is present.
7. If yes: emit `methodName` as a `<a class="inherit-method-link" data-fqn="<fqn>" data-method="<methodName>">` link (same format as method links in the detail panel).
8. If no: emit as plain text.

The `.` itself and parenthesis `(` remain as plain text.

### Step 2 — Verify `skipNextIdent` is not affected

The `skipNextIdent` flag must only fire for the identifier immediately after a standalone class-ref (the variable/parameter name). In the `.methodName(` pattern the `.` resets `skipNextIdent` before `methodName` is reached — confirm this is already the case, since `.` is a non-whitespace non-identifier token and the current logic resets on such tokens.

### Step 3 — Update FEAT-002 and FEAT-009 feature files

Add `**Status:** Implemented in TASK-009` to both feature files once done.

## Notes

- Only handle `ClassName.method(` (static-style or chained from a known linked class). Instance-style `variable.method(` requires type inference — out of scope.
- If `linkClassRefs` already consumed the `.` as a reset-token for `skipNextIdent`, the look-ahead in Step 1 cannot rewind. Implement look-ahead as a forward scan, not a rewind.
- Test with `PerkFactory.Perk.getId`, `IsoPlayer.getInstance`, and `RadioData.getTranslatorNames` to cover typical patterns.

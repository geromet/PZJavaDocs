> **FIXED 2026-03-12** — Both loops in `scrollToMethod()` now skip lines where the method name is preceded by `.` (call site). Fixed as Step 0 of TASK-009.

# BUG-008: scrollToMethod scrolls to a call site instead of the method declaration

**Status:** Fixed (2026-03-12)
**Severity:** Medium
**Touches:** `js/source-viewer.js` (`scrollToMethod`)
**Related feature:** [FEAT-009](../Planned_Features/FEAT-009-deep-java-analysis.md), [FEAT-002](../Planned_Features/FEAT-002-method-call-linking-in-source.md)
**Note:** This bug is a prerequisite fix inside TASK-009 (method-call linking). It will be resolved as part of that task, not in isolation.

## Description

Clicking a method name in the Detail panel (e.g. `getId` on `PerkFactory.Perk`) opens the Source panel and scrolls to the wrong location — a *call site* inside another method body rather than the method's own declaration.

**Observed example:**

Clicking `getId` scrolls to inside `Reset()`:
```java
public static void Reset() {
    nextPerkId = 0;
    for (int i = PerkByIndex.length - 1; i >= 0; --i) {
        Perk perk = PerkByIndex[i];
        if (perk == null) continue;
        if (perk.isCustom()) {
            PerkList.remove(perk);
            PerkById.remove(perk.getId());   ← lands here
```

Expected: scroll to `public int getId() {` — the declaration.

## Root Cause

`scrollToMethod()` in `source-viewer.js` searches source text for the first occurrence of the method name string. It finds `perk.getId()` (a call site) before the declaration because the call appears earlier in the file (inside `Reset()`, which is declared before `getId()`). The regex does not distinguish between declarations and invocations.

## Fix Sketch

Change the search pattern inside `scrollToMethod()` to only match method *declarations*. A declaration line has the pattern:

```
<modifiers> <return-type> methodName(
```

while a call site has the pattern:

```
<identifier>. methodName(
    or
methodName(   (standalone call)
```

A reliable heuristic: skip any match where the character immediately before the method name (ignoring whitespace) is `.` — that is always a call site, not a declaration.

Regex approach: match `\b<methodName>\s*\(` and then discard hits where the preceding non-whitespace character is `.`.

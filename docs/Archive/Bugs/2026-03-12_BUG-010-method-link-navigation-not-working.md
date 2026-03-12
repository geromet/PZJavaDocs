> **FIXED 2026-03-12** — `selectClass` now accepts `jumpToMethod`; handler passes it directly. Eliminates the double `showSource` race. Fixed in TASK-019.

# BUG-010: ClassName.methodName( links emitted in source view but do not navigate

**Status:** Fixed (2026-03-12)
**Severity:** Medium
**Touches:** `js/source-viewer.js` (`linkClassRefs`), `js/app.js` (`inherit-method-link` handler)
**Related feature:** [FEAT-002](../Archive/Features/2026-03-12_FEAT-002-method-call-linking-in-source.md), [FEAT-009](../Planned_Features/FEAT-009-deep-java-analysis.md)

## Description

The source viewer emits `<a class="inherit-method-link">` anchors for `ClassName.methodName(` patterns in Java source. Clicking these links is supposed to navigate to the target class and scroll to the method declaration. Currently the click has no effect (or navigates to the class but does not scroll to the method).

## Steps to Reproduce

1. Open any class that has source available.
2. Switch to the Source tab.
3. Find a line such as `SomeClass.someMethod(` where `SomeClass` is a known API class with a linked method.
4. Click the `someMethod` link.

## Expected

Navigates to `SomeClass`, opens Source tab, scrolls to the declaration of `someMethod`.

## Actual

Nothing happens, or only the class changes without scrolling to the method.

## Root Cause Hypothesis

The `inherit-method-link` click handler in `app.js` was added as part of TASK-009. Possible issues:
- The handler may not be reached because the delegated listener on `#content` is blocked by an earlier condition.
- `showSource` may be called without waiting for the source to load before attempting the scroll.
- The `data-fqn` / `data-method` attributes may not be set correctly on the emitted anchor.

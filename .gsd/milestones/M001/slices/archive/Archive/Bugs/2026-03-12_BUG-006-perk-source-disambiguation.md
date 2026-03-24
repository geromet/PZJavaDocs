> **FIXED 2026-03-12** — In `linkClassRefs()` (`js/source-viewer.js`), when `classBySimpleName[token]` has multiple FQNs, now picks the one whose penultimate segment is lowercase (top-level class in a package) over uppercase (nested class inside another class). `zombie.characters.skills.Perk` is preferred over `zombie.characters.skills.PerkFactory.Perk`.

# BUG-006: Perk class opens wrong source file (disambiguation failure)

**Status:** Fixed
**Severity:** Medium
**Touches:** `js/source-viewer.js`

## Description

When viewing the `Perk` class (FQN: `zombie.characters.skills.Perk`), clicking the Source tab or a method link opens the wrong file. The class list may also show two entries for `Perk` from different packages if disambiguation is not applied.

The issue is related to `classBySimpleName` having multiple FQNs for the simple name `Perk` (e.g. `zombie.characters.skills.Perk` vs `PerkFactory.Perk` as a nested class).

## Root Cause

`classBySimpleName['Perk']` contains multiple FQNs. `linkClassRefs()` always picked `fqns[0]` regardless of which was the more appropriate match. For nested class FQNs, the penultimate path segment is an uppercase class name, making them distinguishable from top-level classes.

## Fix

In the `if (fqns)` branch of `linkClassRefs()`, use `fqns.find()` to prefer the FQN where the penultimate segment starts with a lowercase letter (indicating a package, not an outer class name).

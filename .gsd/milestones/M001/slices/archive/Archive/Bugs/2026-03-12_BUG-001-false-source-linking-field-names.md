> **FIXED 2026-03-12** — `skipNextIdent` heuristic added to `linkClassRefs()` in `js/source-viewer.js`. See TASK-001 archive entry.

# BUG-001: Source viewer falsely links field/variable names as class references

**Status:** Fixed
**Severity:** High
**Area:** Source Viewer (`js/source-viewer.js` — `linkClassRefs`)

## Description

The source viewer tokenises the source text and links any word that matches a known class simple name. It does not understand Java syntax, so it links **identifiers** (field names, variable names, parameter names) that happen to share a name with a class.

## Example

```java
public static final Perk Fitness = new Perk("Fitness");
```

`Fitness` is the **field name** (identifier), not a type reference. But it matches `zombie.characters.BodyDamage.Fitness`, so it gets linked incorrectly. `Perk` (the actual type) gets linked correctly.

## Root Cause

`linkClassRefs()` links every token that matches a simple class name with no surrounding-context check. Java syntax rule: in `<Type> <name>`, the second token is always an identifier, not a type.

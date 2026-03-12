# BUG-001: Source viewer falsely links field/variable names as class references

**Status:** Open
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

## Fix Direction

After linking a token as a type reference, skip linking the **immediately following** identifier token — it is a name, not a type.

Specifically, in the tokeniser loop:
- When a token is matched and linked as a class reference, set a `skipNext` flag.
- When `skipNext` is true, emit the next identifier token as plain text and clear the flag.
- Reset `skipNext` on non-identifier tokens (punctuation, keywords, etc.) — e.g. `Perk[]` or `Perk<T>` should still allow the name after `>` or `]` to be skipped.

This is a **heuristic**, not full parsing. It covers the common cases:
- `ClassName varName`
- `ClassName varName =`
- `static ClassName varName`

Known limitation: won't help with generic types (`Map<K, V> name`), but those are rare in this codebase and a false-negative (no link) is better than a false-positive (wrong link).

## Notes

- The user's quote: *"The first string is the type, the second string is the name. We need to link the types. Linking names is a far future low priority feature."*
- Do not attempt to link field/variable names to their class in this fix.

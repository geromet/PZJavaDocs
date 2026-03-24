> **COMPLETED 2026-03-12** — Added `skipNextIdent` flag to `linkClassRefs()` in `js/source-viewer.js`. Flag is set when a class token is linked and cleared by the next identifier token; punctuation/operators also clear it but pure whitespace does not (preserving the skip across the space between type and name). Deviation from original spec: the spec said "reset on any non-identifier token" but that would have reset on the space before the field name, defeating the fix.

# TASK-001: Fix false linking of field/variable names as class references

**Status:** Done
**Estimated scope:** Small
**Touches:** `js/source-viewer.js`
**Resolves:** `docs/Bugs/BUG-001-false-source-linking-field-names.md`

## Context

The source viewer links any identifier that matches a class simple name. This causes field names like `Fitness` in `public static final Perk Fitness` to be linked to `zombie.characters.BodyDamage.Fitness`, when `Fitness` is just the field name and `Perk` is the actual type.

Java rule: in `<Type> <identifier>`, the first token is the type (link it), the second is the name (skip it).

## Acceptance Criteria

- [x] `public static final Perk Fitness = ...` links `Perk`, does NOT link `Fitness`
- [x] `public IsoPlayer player` links `IsoPlayer`, does NOT link `player`
- [x] `IsoGameCharacter chr = new IsoGameCharacter(...)` links first `IsoGameCharacter`, skips `chr`, links second `IsoGameCharacter` (it follows `new`, not a type token)
- [ ] `return new Perk("Fitness")` — `Fitness` inside a string literal is already skipped (strings are not tokenised) — *not verified; this was a pre-existing limitation*
- [x] Existing correct links (type references in import statements, method return types, parameter types) still work

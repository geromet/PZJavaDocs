# TASK-001: Fix false linking of field/variable names as class references

**Status:** Ready
**Estimated scope:** Small
**Touches:** `js/source-viewer.js`
**Resolves:** `docs/Bugs/BUG-001-false-source-linking-field-names.md`

## Context

The source viewer links any identifier that matches a class simple name. This causes field names like `Fitness` in `public static final Perk Fitness` to be linked to `zombie.characters.BodyDamage.Fitness`, when `Fitness` is just the field name and `Perk` is the actual type.

Java rule: in `<Type> <identifier>`, the first token is the type (link it), the second is the name (skip it).

## Acceptance Criteria

- [ ] `public static final Perk Fitness = ...` links `Perk`, does NOT link `Fitness`
- [ ] `public IsoPlayer player` links `IsoPlayer`, does NOT link `player`
- [ ] `IsoGameCharacter chr = new IsoGameCharacter(...)` links first `IsoGameCharacter`, skips `chr`, links second `IsoGameCharacter` (it follows `new`, not a type token)
- [ ] `return new Perk("Fitness")` — `Fitness` inside a string literal is already skipped (strings are not tokenised)
- [ ] Existing correct links (type references in import statements, method return types, parameter types) still work

## Implementation Plan

Open `js/source-viewer.js` and find `linkClassRefs()`. The function walks tokens and checks each against known class names.

Add a `skipNextIdent` boolean, initially `false`.

In the token loop:

1. **If `skipNextIdent` is `true` and the current token is an identifier:**
   - Emit the token as plain text (no link).
   - Set `skipNextIdent = false`.
   - Continue.

2. **If the current token is matched as a class reference and linked:**
   - Set `skipNextIdent = true`.
   - Continue.

3. **On any non-identifier token** (punctuation like `.`, `(`, `[`, `<`, `;`, operators, keywords):
   - Set `skipNextIdent = false`.
   - This ensures `Map<String, IsoPlayer>` still links `IsoPlayer` (the `>` and `,` reset the flag before `IsoPlayer`).

### Edge cases to preserve

- `new ClassName(` — the `new` keyword is not an identifier, so `skipNextIdent` is false when `ClassName` is reached. ✓
- `return ClassName.method(` — `.` resets the flag before `ClassName`. ✓
- `ClassName[]` — `[` resets flag, so an array type like `Perk[] perks` would skip `perks`. ✓
  Actually `[` follows `ClassName`, not precedes it, so flag is set by `ClassName`, then `[` resets it, then `]` is punctuation, then `perks` sees flag=false → `perks` would be checked against class names normally. This is fine — `perks` is unlikely to match a class name.
- `(ClassName param)` — `(` resets flag, `ClassName` sets it, `)` or next token after the name clears it. ✓

## Notes

- If the tokeniser uses a regex split rather than a character-by-character walk, the same logic applies: track whether the previous **matched class token** was the immediately preceding identifier token with no intervening punctuation.
- Do NOT change how strings or comments are handled — they are already excluded from linking.
- After the fix, test with `IsoGameCharacter.java` (has many field declarations) and `Perk.java` (the exact failing example).

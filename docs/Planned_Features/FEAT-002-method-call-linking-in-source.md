# FEAT-002: Link method calls on already-linked type references in source viewer

**Priority:** Low
**Area:** Source Viewer (`js/source-viewer.js`)
**Depends on:** BUG-001 fix (correct type linking must be reliable first)

## Goal

When the source viewer renders `RadioData.getTranslatorNames(language)` and `RadioData` is already linked to its class, also link `getTranslatorNames` — navigating to that method's source line in `RadioData.java`.

Similarly: `RadioAPI.getInstance()` — `getInstance` should link to the method in `RadioAPI`.

## Design

After the current tokeniser links a class reference token, the pattern `ClassName.methodName(` should additionally wrap `methodName` in a method link. This is an **extension of the existing token pass**, not a full Java parser.

### Link target
`inherit-method-link` with `data-fqn="<class FQN>"` and `data-method="<methodName>"` — same as the inherited method table links. Clicking navigates to the class and scrolls to the method in source.

### Heuristic
Token sequence: `[linked-class-ref]` → `.` → `[identifier]` → `(`

When this pattern is detected, wrap the identifier in a method link pointing to the previously linked class.

### Limitations
- Only works for static-style calls `ClassName.method()` — not `obj.method()` (would require type inference).
- Only links methods known in `API.classes[fqn].methods`. Unknown methods (private, inherited-not-in-API) are left unlinked.
- Constructor calls `new ClassName(...)` already get the class linked; linking the constructor name specifically is out of scope.

## Notes

- User quote: *"Links RadioData, but should also link getTranslatorNames"* and *"Links RadioAPI but should also link getInstance"*
- Linking variable **names** (`Perk Fitness` → `Fitness`) is explicitly deferred: *"Linking names is a far future low priority feature."*
- This feature should only be started after BUG-001 is fixed, since false class links would produce false method links.

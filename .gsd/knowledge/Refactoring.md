# Refactoring

Guidelines for when and how to refactor in this project.

## When to refactor

Refactor when:
- The same pattern appears 3+ times with slight variations (extract a helper)
- A function takes more than ~3 parameters (group into an object, or reconsider the design)
- A bug was caused by structural fragility, not just a typo (the `noPush` → `_restoringState` migration is the canonical example)
- A new feature would require duplicating existing logic

Do **not** refactor when:
- The change was not requested
- The code works and there's no bug or upcoming feature that stresses it
- It would only make the code "cleaner" by style preference without reducing future risk

## Extractor refactoring rules

- Keep steps numbered and sequential. Don't split into helper files.
- `file_cache` is shared state — never clear it mid-run. Additions are fine.
- BFS/queue patterns for inheritance traversal are established — follow the same pattern (see steps 4.6, 4.7).
- `resolve_simple()` is the single point for resolving short class names to FQNs. Don't duplicate its logic.

## Frontend refactoring rules

- State is in `state.js`. If a new piece of state is needed, add it there with a comment.
- Nav-aware functions call `navPush` unconditionally. The `_restoringState` flag handles suppression transparently.
- Rendering functions (`renderXxx`) return HTML strings. They must not mutate DOM.
- DOM mutation (`document.getElementById`, `innerHTML =`, etc.) happens in the calling function or event handler.
- Click delegation over per-element listeners. Add new delegated cases to the existing handlers in `app.js` rather than attaching listeners inside rendering functions.

## Naming stability

Changing a CSS class name or a `data-*` attribute name is a breaking change if it's used in:
- CSS rules
- Event handler selectors (`e.target.closest(...)`)
- JS DOM queries

Always search all files before renaming. Prefer additive changes (add the new name, remove the old one after verifying).

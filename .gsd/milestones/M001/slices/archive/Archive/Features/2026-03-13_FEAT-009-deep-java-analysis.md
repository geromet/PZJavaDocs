> **COMPLETED 2026-03-12/13** — Problem B (wildcard imports) and Problem C (method-level call linking) both implemented in source-viewer.js. Archived as docs cleanup.

# FEAT-009: Deeper Java analysis (wildcard imports, method-level linking)

**Status:** Problem C implemented in TASK-009 (2026-03-12). Problem B (wildcard imports) still planned.
**Priority:** Medium
**Complexity:** High
**Related:** `docs/Archive/task-01-source-coverage.md` Phase 2 (Problems B and C)

## Description

Two analysis gaps remain after Task 01's phase 1:

### Problem B — Wildcard import fallback

When a `.java` file has `import zombie.characters.*;`, the current extractor cannot resolve unqualified type names to specific classes. This means some source class-ref links are missing even when the `.java` file for the target class is available.

**Fix:** For each unresolved token, scan all `_source_index` classes in the same package group as each wildcard import. Prefer the match with the correct package. This is a heuristic and may produce false links, so only apply it when there is a single candidate.

### Problem C — Method-level call linking in source

When source code calls `SomeClass.method()` or `instance.method()`, the viewer only links `SomeClass` as a type reference, not the method itself. Deep linking to the specific method would be more useful.

**Fix:** In `linkClassRefs()`, after linking a class token, detect if the next token is `.` followed by an identifier, and if so, emit the identifier as a `data-fqn + data-method` combined link that opens the class and scrolls to that method.

## Notes

- Problem C is lower priority — the current behavior (link the class, user finds the method) is acceptable.
- Problem B may cause noisy false-positives. Add a confidence threshold: only link if there is exactly one candidate.

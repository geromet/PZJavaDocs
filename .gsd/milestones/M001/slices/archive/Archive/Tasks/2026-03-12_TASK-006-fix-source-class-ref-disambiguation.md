> **COMPLETED 2026-03-12** — Added best-FQN picker inside the `if (fqns)` branch of `linkClassRefs()`. Prefers FQNs where the penultimate segment is lowercase (top-level class) over uppercase (nested class). Single-candidate names unchanged.

# TASK-006: Fix source class-ref disambiguation for nested classes
**Resolves:** BUG-006

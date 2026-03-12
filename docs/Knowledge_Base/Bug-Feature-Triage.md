# Bug–Feature Triage Workflow

When a new bug is filed, before creating a task to fix it, check whether an existing planned feature covers the same ground. This avoids writing a small patch that gets thrown away when the feature lands.

## The Check

After writing a bug file, scan `docs/Planned_Features/` and ask:

> "If this feature were implemented, would the bug be fixed or made irrelevant as a natural consequence?"

If **yes**: note the relationship in both the bug file and the feature file (`**Related bug:**` line), then follow the decision tree below.

If **no**: file the bug normally. Create a task when ready to fix it.

## Decision Tree

```
New bug confirmed
      │
      ▼
Is there a planned feature that would fix it?
      │
    ┌─┴──────────┐
   YES            NO
    │              │
    ▼              ▼
Is the feature    Create a bug task
unblocked and     when ready to fix.
medium+ priority?
    │
  ┌─┴───────────┐
 YES             NO
  │               │
  ▼               ▼
Create a task    File the bug.
for the FEATURE  Note the blocking
(not the bug).   feature in the bug.
Include the bug  Create a standalone
fix as a step    bug task when the
in the task plan.feature clears.
```

## Linking Convention

In the **bug file**, add:
```
**Related feature:** [FEAT-NNN](../Planned_Features/FEAT-NNN-slug.md)
**Note:** Will be resolved as part of TASK-NNN, not in isolation.
```

In the **feature/task file**, add:
```
**Fixes bug:** [BUG-NNN](../Bugs/BUG-NNN-slug.md)
```

## Current Known Relationships

| Bug | Related Feature | Status |
|-----|----------------|--------|
| [BUG-008](../Bugs/BUG-008-scrolltomethod-finds-call-site.md) | [FEAT-009](../Planned_Features/FEAT-009-deep-java-analysis.md) Problem C, [FEAT-002](../Planned_Features/FEAT-002-method-call-linking-in-source.md) | Bug fix bundled into TASK-009 |
| [BUG-009](../Bugs/BUG-009-placeholder-visible-over-panels.md) | [FEAT-006](../Planned_Features/FEAT-006-tab-bar-system.md) (would fix as side-effect) | Fix independently — FEAT-006 too large to block on |

## When to Update This File

- When a new bug is filed and a feature relationship is found → add a row to the table.
- When the bug or feature is archived → update the Status column or remove the row.

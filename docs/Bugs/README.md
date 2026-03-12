# Bugs

One file per confirmed defect. File a bug here when something is reproducibly broken and won't be fixed in the same session it was found.

**When to read this folder:** When investigating a known issue, or to pick the next thing to fix.
**When to write here:** When a new bug is confirmed. Also update `docs/STATUS.md`.
**When to move a file:** Move to `docs/Archive/` when the bug is fixed. Add a one-line fix summary at the top of the file and update `docs/STATUS.md`.

## Before Working on Bugs

**First: check if a planned feature covers this bug.** See [`docs/Knowledge_Base/Bug-Feature-Triage.md`](../Knowledge_Base/Bug-Feature-Triage.md) for the full decision tree. If an unblocked medium+ priority feature would fix the bug as a natural consequence, create a task for the feature (not the bug), and bundle the fix into that task's plan.

If no feature applies, do not work on a bug directly. First create a Task in `docs/Tasks/`:

1. Read the bug file(s) fully.
2. Read the relevant source code to confirm the root cause.
3. **Group related bugs into a single Task** if they touch the same file or share a root cause — one commit is better than three.
4. Write the Task with a concrete implementation plan and acceptance criteria (one checkbox per bug resolved).
5. Then work the Task as normal.

## File Naming

`BUG-NNN-short-slug.md` (next available number, lowercase slug, hyphens)

## Template

```markdown
# BUG-NNN: Short Title

**Status:** Open | Fixed (date)
**Severity:** Critical | High | Medium | Low
**Touches:** file(s) involved

## Description
What goes wrong and where.

## Steps to Reproduce
1. ...

## Root Cause
(fill in when known)

## Fix Sketch
Enough detail to write a Task from.
```

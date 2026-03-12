# Tasks

Concrete, ready-to-execute tasks for Claude. A task file should contain everything needed to complete the work in one session without asking clarifying questions.

## File naming
`TASK-NNN-short-description.md`

## Template
```
# TASK-NNN: Short Title

**Status:** Ready | In Progress | Done
**Estimated scope:** Small | Medium | Large
**Touches:** list of files likely to change

## Context
Why this task exists, what problem it solves.

## Acceptance Criteria
- [ ] ...
- [ ] ...

## Implementation Plan
Step-by-step. Enough detail that Claude can execute without guessing.

## Notes / Gotchas
Known edge cases, things to watch out for.
```

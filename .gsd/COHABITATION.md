# Cohabitation Agreement — Pi & Claude Code

Two AI coding agents share this project. This file defines the rules of engagement.

## Who lives here

| Agent | Config location | Instruction file | Primary tool |
|-------|----------------|-----------------|--------------|
| **Claude Code** | `.claude/` | `pz-lua-api-viewer/CLAUDE.md` | Anthropic CLI |
| **Pi (GSD)** | `.gsd/` | `.gsd/PI.md` + `.gsd/milestones/` | GSD agent harness |

## Repository location

The **only** git repo is `pz-lua-api-viewer/`. The parent directory `projectzomboid/` is the PZ game install — it must NEVER have a `.git` directory, `.gitignore`, or any project files. All git operations must run with CWD set to `pz-lua-api-viewer/`.

## Shared resources

- **Source code** (`pz-lua-api-viewer/`) — both agents read and write the same files.
- **Project docs** (`pz-lua-api-viewer/docs/`) — STATUS.md, tasks, bugs, knowledge base. Both agents use these.
- **Git history** — both agents commit to the same branch.

## Boundaries

1. **Don't touch each other's config.** Pi never modifies `.claude/` or `CLAUDE.md`. Claude Code never modifies `.gsd/`.
2. **Same conventions.** Both agents follow the same project conventions defined in `pz-lua-api-viewer/docs/Knowledge_Base/`:
   - `Style-Guide.md` for code style
   - `Design-Patterns.md` for recurring patterns
   - `Working-Agreement.md` for workflow rules
   - `Decisions.md` for architectural decisions
3. **Shared operational docs.** Both agents read `pz-lua-api-viewer/docs/Tasks/`, `docs/Bugs/`, `docs/Planned_Features/`, `docs/STATUS.md`. Pi additionally owns `.gsd/milestones/` for planning structure. Claude Code does not modify `.gsd/milestones/`.
4. **No duplication.** If a task is in the GSD roadmap, it doesn't also need a separate legacy `docs/Tasks/` file — the GSD slice plan is authoritative. Existing legacy task files remain for their detailed implementation notes.
5. **Commit attribution.** Claude Code uses its co-author tag. Pi uses its own. This makes `git log` clear about who did what.
6. **Branch discipline.** Both agents obey the same rule: never push to `main`. Work on `liability-machine` or feature branches.

## Conflict avoidance

- The user directs which agent works on what. Agents don't compete for tasks.
- If one agent's changes break something, the user decides who fixes it.
- Both agents should read `docs/STATUS.md` at the start of a session to understand current state, regardless of who last updated it.

## When in doubt

Follow the project docs in `pz-lua-api-viewer/docs/Knowledge_Base/`. Those are the single source of truth for how this project operates. This cohabitation file only adds the rules specific to two agents sharing a workspace.

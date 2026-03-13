# PZ Lua API Viewer — Pi (GSD) Working Instructions

## Cohabitation Notice

This project is shared between **Claude Code** (via `.claude/` and `pz-lua-api-viewer/CLAUDE.md`) and **Pi / GSD** (via `.gsd/`). Both agents work on the same codebase. Never modify `.claude/`, `pz-lua-api-viewer/CLAUDE.md`, or Claude Code's settings.

## Directory Structure — READ THIS FIRST

```
ProjectZomboid/
├── projectzomboid/              ← PZ game install dir (NOT a git repo)
│   ├── sources/                 ← Decompiled Java sources (read by extract_lua_api.py)
│   └── pz-lua-api-viewer/      ← THE GIT REPO (this project)
│       ├── .git/
│       ├── sources/             ← Pre-shipped .java copies for GitHub Pages
│       ├── extract_lua_api.py
│       ├── lua_api.json
│       ├── index.html, app.css, js/
│       └── docs/, .gsd/, .claude/
```

**Critical rules:**
- The **only** git repo is `pz-lua-api-viewer/`. The parent `projectzomboid/` must NEVER have a `.git` directory or any project files.
- Never run `git init` or `git clone` in `projectzomboid/`. Never create project files (`.gitignore`, `.gitattributes`, `README.md`, etc.) at the parent level.
- CWD for all git operations and file editing must be `pz-lua-api-viewer/`, not the parent.
- There are **two** `sources/` directories: `projectzomboid/sources/` (full decompiled sources, read by the extractor) and `pz-lua-api-viewer/sources/` (subset shipped to GitHub Pages). Do not confuse them.

## GSD Structure

All GSD planning artifacts live in `.gsd/` inside `pz-lua-api-viewer/`:

```
.gsd/
  PROJECT.md          ← living doc — what the project is right now
  DECISIONS.md        ← append-only architectural decisions register
  STATE.md            ← quick-glance current status
  QUEUE.md            ← append-only milestone queue
  milestones/
    M001/
      M001-CONTEXT.md
      M001-ROADMAP.md
      slices/
        S01/  S01-PLAN.md   ← Build-time precomputation (TASK-017)
        S02/  S02-PLAN.md   ← Middle-click tabs + hover preview (FEAT-007, FEAT-014)
        S03/  S03-PLAN.md   ← Version selector (TASK-018 / FEAT-005)
        S04/  S04-PLAN.md   ← Javadoc (TASK-016 / FEAT-010) — BLOCKED
```

**At the start of every session or task:** Read `STATE.md` first, then the active slice plan.

## Project Overview

A static web app for browsing the Project Zomboid Lua API. Deployed to GitHub Pages from the `main` branch. All development on feature branch `liability-machine`.

**NEVER push directly to `main`** — it auto-deploys to GitHub Pages.

## Key Files

| File | Purpose |
|------|---------|
| `pz-lua-api-viewer/extract_lua_api.py` | Run from `projectzomboid/` to regenerate `lua_api.json` |
| `pz-lua-api-viewer/lua_api.json` | ~6MB extracted API — written directly by extractor |
| `pz-lua-api-viewer/index.html` | Single-file HTML shell |
| `pz-lua-api-viewer/app.css` | All styles |
| `pz-lua-api-viewer/js/state.js` | Global state variables |
| `pz-lua-api-viewer/js/app.js` | Init, navigation, event setup |
| `pz-lua-api-viewer/js/class-detail.js` | Detail panel renderer |
| `pz-lua-api-viewer/js/source-viewer.js` | Source panel renderer + class-ref linking |
| `pz-lua-api-viewer/js/globals.js` | Globals tab |
| `pz-lua-api-viewer/sources/` | 971+ pre-shipped .java files |

## Running Locally

```bash
cd pz-lua-api-viewer
python server.py
# → http://localhost:8765
```

## Regenerating the API

```bash
# From projectzomboid/ (CWD):
python pz-lua-api-viewer/extract_lua_api.py
```

Expected baselines: ~1096 classes, ~19099 methods, ~745 globals.
After any extractor change, regenerate immediately before committing.

## Shared Project Docs

`pz-lua-api-viewer/docs/` is shared between both agents:

| What | Where |
|------|-------|
| Live project status | `pz-lua-api-viewer/docs/STATUS.md` |
| Bugs | `pz-lua-api-viewer/docs/Bugs/BUG-NNN-slug.md` |
| Planned features | `pz-lua-api-viewer/docs/Planned_Features/FEAT-NNN-slug.md` |
| Active tasks (legacy) | `pz-lua-api-viewer/docs/Tasks/TASK-NNN-slug.md` |
| Completed work | `pz-lua-api-viewer/docs/Archive/` |
| Reference knowledge | `pz-lua-api-viewer/docs/Knowledge_Base/` |

## Working Through the GSD Roadmap (Auto Mode)

1. **Read `STATE.md`** — find the active slice and task.
2. **Read the active slice plan** (e.g. `.gsd/milestones/M001/slices/S01/S01-PLAN.md`).
3. **For each task in the slice:**
   - Read the corresponding legacy task file if one exists (e.g. `pz-lua-api-viewer/docs/Tasks/TASK-017-build-time-precomputation.md`) — it has detailed implementation plans.
   - Implement per the plan.
   - After any extractor change: regenerate `lua_api.json`.
   - Take browser screenshots to verify (start server first: `cd pz-lua-api-viewer && python server.py`).
   - Mark the task checkbox in the slice plan (`[ ]` → `[x]`).
   - If the task resolves a legacy task file: prepend `> **COMPLETED YYYY-MM-DD** — ...` blockquote, then `cd pz-lua-api-viewer && python docs/archive.py docs/Tasks/TASK-NNN-slug.md`.
   - Update `pz-lua-api-viewer/docs/STATUS.md`.
   - Update `STATE.md`.
4. **When all slice tasks are complete:**
   - Write a slice summary to `.gsd/milestones/M001/slices/SNN/SNN-SUMMARY.md`.
   - Mark the slice checkbox in `M001-ROADMAP.md`.
   - Update `STATE.md` to point at the next slice.
   - Commit and push: `cd pz-lua-api-viewer && git add -A && git commit -m "..." --trailer "Co-Authored-By: Pi (GSD) <noreply@gsd.dev>" && git push`
5. **If a task is blocked:** note the blocker in `STATE.md` and the slice plan, skip to the next unblocked task.
6. **Creating new bugs:** file in `pz-lua-api-viewer/docs/Bugs/BUG-NNN-slug.md`. Check `pz-lua-api-viewer/docs/Knowledge_Base/Bug-Feature-Triage.md` first.
7. **Creating new planned features:** file in `pz-lua-api-viewer/docs/Planned_Features/FEAT-NNN-slug.md` and add to the appropriate future slice plan.
8. **After discovering new work:** add to the appropriate GSD slice plan or create a new slice/milestone as needed.

## Testing

- **Before committing any frontend change:** start the server and take a browser screenshot.
- **After extractor changes:** run the extractor; check output baselines (±10% triggers investigation).
- See `pz-lua-api-viewer/docs/Knowledge_Base/Testing.md` for full verification procedures.

## Commit / PR Rules

- Branch from `liability-machine` for new work.
- One logical change per commit.
- Never `--no-verify` or `--force` push.
- Co-author tag: `Co-Authored-By: Pi (GSD) <noreply@gsd.dev>`
- PR `liability-machine → main` when a batch of work is shippable (ask user first).

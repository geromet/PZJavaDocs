# PZ Lua API Viewer — Claude Working Instructions

## Project Overview

A static web app for browsing the Project Zomboid Lua API. The viewer is deployed to GitHub Pages from the `main` branch. All development happens on feature branches (currently `liability-machine`).

**IMPORTANT: Do not push directly to `main`.** The site auto-deploys on push. Create a PR from your feature branch instead.

## Key Files

| File | Purpose |
|------|---------|
| `extract_lua_api.py` | Run from `../` (projectzomboid/) to regenerate `lua_api.json` |
| `lua_api.json` | ~6MB extracted API — written here directly by `extract_lua_api.py` |
| `index.html` | Single-file HTML shell (~720 lines) |
| `app.css` | All styles |
| `js/state.js` | Global state variables |
| `js/app.js` | Init, navigation, event setup |
| `js/class-detail.js` | Detail panel renderer |
| `js/source-viewer.js` | Source panel renderer + class-ref linking |
| `js/globals.js` | Globals tab |
| `prepare_sources.py` | Copies .java sources to `sources/` for GitHub Pages |
| `sources/` | 971+ pre-shipped .java files (15 MB) |

## Running Locally

Use the Python dev server (port 8765):

```
cd pz-lua-api-viewer
python server.py
```

Then open `http://localhost:8765`.

## Regenerating the API

**Run this after any change to `extract_lua_api.py`.** Do not skip it — the viewer reads the JSON at load time, so code changes to the extractor have no effect until the JSON is regenerated and copied.

```
cd projectzomboid/          # parent of pz-lua-api-viewer/
python pz-lua-api-viewer/extract_lua_api.py
```

The extractor writes directly to `pz-lua-api-viewer/lua_api.json` — no copy step needed.

Also run `prepare_sources.py` if the set of .java source files in `_source_index` or `_interface_paths` has changed (i.e., new files need to be available on GitHub Pages):

```
cd pz-lua-api-viewer/
python prepare_sources.py
```

## Architecture Notes

- **No build step.** Plain ES6 modules loaded directly. `index.html` loads each `js/*.js` file as `<script src>`.
- **Navigation** uses a manual history stack (`navHistory[]` in `state.js`). `navPush` is a no-op when `_restoringState = true`; `applyState` sets that flag in try/finally so restoration never writes history.
- **Source linking** (`linkClassRefs()` in `source-viewer.js`) walks tokens and emits `<a>` tags for matching class names. Post-link fix needed: `skipNextIdent` to avoid linking field names (see TASK-001).
- **`_extends_map`** in the JSON bridges non-API intermediates in the inheritance chain (e.g. `IsoLivingCharacter` which is not setExposed).
- **`_interface_extends`** maps interface FQN → parent interface FQNs for transitive implements display.
- **`_source_index`** maps simple class name → relative `.java` path for classes not in the API.

## docs/ Folder

`docs/` holds all persistent project knowledge. Each subfolder has a `README.md` explaining its purpose and templates. The canonical place to see what's currently open is **`docs/STATUS.md`** — check it at the start of a session and update it when work completes.

Quick reference:

| What | Where |
|------|-------|
| Something is broken | `docs/Bugs/BUG-NNN-slug.md` |
| Feature wanted but not scheduled | `docs/Planned_Features/FEAT-NNN-slug.md` |
| Work starting right now | `docs/Tasks/TASK-NNN-slug.md` |
| Work is done | Move the file to `docs/Archive/`, add completion note, update `docs/STATUS.md` |
| Stable reference facts | `docs/Knowledge_Base/` |
| Session-only scratch | `docs/Temp/` (gitignored) |

**Do NOT pile up Tasks.** If work isn't imminent, it belongs in Bugs or Planned_Features. A task file means "we are working on this now."

**When filing a new bug:** check `docs/Knowledge_Base/Bug-Feature-Triage.md` first — it has a decision tree for whether to create a standalone bug task or bundle the fix into an existing planned feature task.

## Working Through Tasks

When the user says "work on the tasks" (or similar), do this without further clarification:

1. **Read `docs/STATUS.md`** — it has the current open bugs, active tasks, and planned features list.
2. **Work through tasks in ID order** (TASK-001 first), unless the user specifies otherwise or a dependency requires a different order.
3. **For each task:**
   - Read the task file fully before touching any code.
   - Read every file the task says it touches.
   - Implement per the plan; verify acceptance criteria.
   - If the task modified `extract_lua_api.py`, run the extractor and copy the JSON immediately (see "Regenerating the API").
   - When done, prepend a `> **COMPLETED YYYY-MM-DD** — ...` blockquote at the very top of the task file (full content preserved — never replace with a stub), then run: `python docs/archive.py docs/Tasks/TASK-NNN-slug.md`
   - If the task resolves a bug, prepend a `> **FIXED YYYY-MM-DD** — ...` blockquote at the top of the bug file (same rule: full content), then run: `python docs/archive.py docs/Bugs/BUG-NNN-slug.md`
   - Update `docs/STATUS.md` — remove the task from "Active Tasks" and the bug from "Open Bugs".
4. **If a task is blocked** (missing data, unclear spec, prerequisite not met), note the blocker in the task file, leave it in `docs/Tasks/`, and move to the next task.
5. **After all tasks** in a session are done:
   - Stage and commit all changes: `git add -A && git commit -m "..."`
   - Push to the remote: `git push`
   - Then ask the user if they want to pick new tasks from the feature/bug lists or create new task files.

## Commit / PR Rules

- Branch from `liability-machine` for new work.
- One logical change per commit.
- Never `--no-verify` or `--force` push.
- PR `liability-machine → main` when a batch of work is shippable.

# Archive

Completed or superseded documents kept for historical reference. Nothing here is actively maintained — do not edit archived files.

**When to read this folder:** When you need context on how something was implemented, or why a decision was made.
**When to write here:** When completing a Task or fixing a Bug — use the archive script (see below).

## Subfolders

| Subfolder | Contains |
|-----------|----------|
| `Bugs/`   | Archived BUG-* files |
| `Tasks/`  | Archived TASK-* files |
| `Features/` | Archived FEAT-* files |

## How to Archive a File

Always use the script — it handles the date prefix and correct subfolder automatically:

```
python docs/archive.py docs/Tasks/TASK-006-foo.md
python docs/archive.py docs/Bugs/BUG-003-bar.md
python docs/archive.py docs/Planned_Features/FEAT-002-baz.md
```

The file is renamed to `YYYY-MM-DD_ORIGINAL-NAME.md` and moved to the correct subfolder. The source file is removed.

## Conventions

- Before archiving, prepend a `> **COMPLETED/FIXED YYYY-MM-DD** — ...` blockquote at the very top of the file.
- Keep the full original content — never replace it with a stub.
- Do not delete archived files.
- After archiving, update `docs/STATUS.md` to remove the item from its active list.

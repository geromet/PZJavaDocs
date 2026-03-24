"""
archive.py — Move a docs file into the correct Archive subfolder with a date prefix.

Usage (from pz-lua-api-viewer/):
    python docs/archive.py docs/Tasks/TASK-006-foo.md
    python docs/archive.py docs/Bugs/BUG-003-bar.md
    python docs/archive.py docs/Planned_Features/FEAT-002-baz.md

The file is renamed to YYYY-MM-DD_ORIGINAL-NAME.md and placed in:
    docs/Archive/Tasks/      for TASK-* files
    docs/Archive/Bugs/       for BUG-* files
    docs/Archive/Features/   for FEAT-* files
    docs/Archive/            for anything else

The source file is removed after the move.
"""

import sys
import shutil
import pathlib
import datetime

ARCHIVE_ROOT = pathlib.Path(__file__).parent / "Archive"

SUBFOLDER_MAP = {
    "TASK": "Tasks",
    "BUG":  "Bugs",
    "FEAT": "Features",
}

def main():
    if len(sys.argv) < 2:
        print("Usage: python docs/archive.py <path-to-file>")
        sys.exit(1)

    src = pathlib.Path(sys.argv[1])
    if not src.exists():
        print(f"Error: {src} does not exist")
        sys.exit(1)

    today = datetime.date.today().strftime("%Y-%m-%d")
    stem  = src.stem   # filename without extension
    suffix = src.suffix

    # Determine subfolder from filename prefix
    prefix = stem.split("-")[0].upper()
    subfolder = SUBFOLDER_MAP.get(prefix, "")
    dest_dir = ARCHIVE_ROOT / subfolder
    dest_dir.mkdir(parents=True, exist_ok=True)

    # Build destination filename: YYYY-MM-DD_ORIGINAL-NAME.ext
    dest_name = f"{today}_{src.name}"
    dest = dest_dir / dest_name

    if dest.exists():
        print(f"Warning: {dest} already exists - skipping")
        sys.exit(1)

    shutil.move(str(src), str(dest))
    print(f"Archived: {src} -> {dest}")

if __name__ == "__main__":
    main()

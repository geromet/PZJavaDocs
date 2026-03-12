"""
Copies the decompiled .java source files for all classes in lua_api.json
into the pz-lua-api-viewer/sources/ directory so they can be served
as static files on GitHub Pages.

The Indie Stone's Terms (section 2.1) permit distributing game files
for non-commercial purposes that promote Project Zomboid.
See: https://projectzomboid.com/blog/support/terms-conditions/

Run this script from the projectzomboid/ source root, or set SRC_ROOT below.
Re-run it after a game update to refresh the sources.
"""

import json
import pathlib
import shutil

SRC_ROOT   = pathlib.Path(__file__).parent.parent  # projectzomboid/
VIEWER_DIR = pathlib.Path(__file__).parent          # pz-lua-api-viewer/
API_FILE   = SRC_ROOT / "lua_api.json"
SOURCES_DIR = VIEWER_DIR / "sources"

print(f"Reading {API_FILE}...")
api = json.loads(API_FILE.read_text(encoding="utf-8"))

copied = 0
missing = 0

# Always include LuaManager.java (source for all global functions)
for rel in ["zombie/Lua/LuaManager.java"]:
    src = SRC_ROOT / rel
    dest = SOURCES_DIR / rel
    if src.exists():
        dest.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(src, dest)
        copied += 1
    else:
        missing += 1

for fqn, cls in api["classes"].items():
    src_rel = cls.get("source_file")
    if not src_rel:
        missing += 1
        continue

    src_path  = SRC_ROOT / src_rel
    dest_path = SOURCES_DIR / src_rel

    if not src_path.exists():
        missing += 1
        continue

    dest_path.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(src_path, dest_path)
    copied += 1

# Also copy source-only files referenced by _source_index
for simple, src_rel in api.get("_source_index", {}).items():
    src_path  = SRC_ROOT / src_rel
    dest_path = SOURCES_DIR / src_rel

    if not src_path.exists():
        missing += 1
        continue

    if dest_path.exists():
        continue  # already copied (shared file with an API class)

    dest_path.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(src_path, dest_path)
    copied += 1

print(f"Copied:  {copied} source files -> {SOURCES_DIR}")
print(f"Missing: {missing}")
print("Done.")

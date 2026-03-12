> **FIXED 2026-03-12** — `showSource` returns early with a friendly message when `cls.source_file` is empty. `fetchSource` rejects `text/html` responses with an error. Root cause: `zombie.characters.skills.Perk` / `.Perks` are nested types with no standalone `.java` file. Fixed in TASK-021.

# BUG-013: Source panel displays a directory listing HTML page instead of source code

**Status:** Fixed (2026-03-12)
**Severity:** High
**Touches:** `js/source-viewer.js` (source fetch logic), possibly `prepare_sources.py` or `sources/` directory structure

## Description

For the class `zombie.characters.skills` (and possibly other package-level names), the Source panel renders the raw HTML of a directory listing instead of Java source code:

```
<!DOCTYPE HTML>
<html lang="en">
<head>
<meta charset="utf-8">
<title>Directory listing for /sources/</title>
</head>
<body>
<h1>Directory listing for /sources/</h1>
...
```

## Steps to Reproduce

1. Select the class `zombie.characters.skills` (or navigate directly).
2. Switch to the Source tab.

## Expected

The `.java` source file for the class is displayed with syntax highlighting.

## Actual

The raw HTML of a server directory listing is rendered inside the source `<code>` block.

## Root Cause Hypothesis

The path resolution for this class resolves to a directory URL (e.g. `sources/zombie/characters/skills/`) rather than a `.java` file. The dev server (and GitHub Pages) returns an HTML directory index for bare directory URLs. The source viewer does not detect a non-Java response and renders the HTML verbatim.

Possible causes:
1. The class entry's `source_file` path in `lua_api.json` is missing the filename (ends at the directory).
2. The `_source_index` entry for this class maps to a directory path instead of a `.java` file.
3. The source fetch URL is constructed incorrectly for this class.

## Fix Sketch

- Check the `source_file` / `_source_index` value for `zombie.characters.skills` in `lua_api.json`.
- In the source viewer fetch, validate that the response `Content-Type` is not `text/html` before rendering; if it is, show an error message instead.
- Run `prepare_sources.py` and verify the correct `.java` file is present under `sources/`.

# Style Guide

Coding conventions for this project.

## JavaScript

- **`'use strict'`** at the top of every `.js` file.
- **No framework, no build step.** Plain ES2020 JS loaded via `<script>` tags. Keep it that way.
- **Global state lives in `state.js`**, declared with `let` or `const`. No state scattered across modules.
- **Functions are global** (no module system). File boundaries are organisational only.
- **HTML generation:** use template literals with the `esc()` helper for all user-supplied strings. Never interpolate raw values into HTML.
- **Event delegation** over per-element listeners for dynamically generated content (method rows, class items, etc.).
- **Async:** use `async/await`. No raw Promise chains.
- **Naming:** camelCase for variables and functions. No underscores except for internal/private convention (`_restoringState`, `_extends_map`).

## Python (extractor)

- Target: Python 3.9+ (uses `pathlib.is_relative_to()`).
- **One script, top-to-bottom.** `extract_lua_api.py` runs as a plain script, not a module.
- Steps are numbered and separated by comment banners: `# --- Step N: Description ---`.
- **`file_cache`** is the single parse cache. Always go through `get_tree()` for setExposed/UsedFromLua classes, and `file_cache.get()` for subsequent lookups in later steps.
- Use `pathlib.Path` everywhere for file paths. Never string-concatenate paths.
- Print progress for any loop over >100 items.

## HTML / CSS

- **Single `index.html`**, no build.
- CSS variables for all colours — never hardcode hex values in rules, use `var(--token)`.
- BEM-lite naming: `.block-element` or `.block--modifier`. Keep it flat; avoid deep nesting.
- Responsive is not a goal — this is a developer tool, desktop only.

## Markdown docs

- Use the templates in `docs/Bugs/README.md`, `docs/Tasks/README.md`, etc.
- File names: `KEBAB-CASE.md` for Knowledge_Base, `TYPE-NNN-description.md` for tracked items.
- One blank line between sections.

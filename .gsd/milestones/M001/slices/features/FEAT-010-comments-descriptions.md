# FEAT-010: Extract and display Javadoc / LuaDoc comments

**Status:** Planned
**Priority:** Medium
**Complexity:** High

## Description

Extract Javadoc comments from the Java source and display them in the Detail panel alongside methods and fields, so users can read documentation without opening the Source tab.

## Motivation

Many PZ classes have meaningful Javadoc comments (particularly on public API methods). Showing these inline removes the need to scroll through source code just to read a description.

## Proposed Display

- In the Detail panel method row, show a collapsed ▶ summary toggle after the method signature.
- Expanding it shows the full Javadoc (params, returns, description).
- Fields with comments also show a collapsible description.

## Extractor Changes

- In `extract_lua_api.py`, when parsing each method via `javalang`, extract the preceding block comment (`/** ... */`) if present.
- Store as `"doc": "..."` on each method/field entry.
- Strip `@param`, `@return` tags but preserve their text.

## Notes

- Javadoc is not always present, especially in obfuscated or generated code. Graceful fallback: no doc = no toggle shown.
- This is a significant extractor and frontend change. Prioritise after tabs (FEAT-006) since tabs don't conflict with this.
- Possible extension: also pull `@LuaMethod` annotation `doc=""` attribute if present.

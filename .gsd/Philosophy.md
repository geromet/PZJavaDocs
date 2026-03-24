# Philosophy

Core principles that guide design and implementation decisions.

## The tool exists for one reason
Make it fast and easy to understand the PZ Lua API — what's callable, what it does, and how it fits together. Every feature decision should trace back to this.

## Prefer data over code
When a display problem can be solved by richer data from the extractor, prefer that over complex frontend logic. The extractor runs once; the frontend runs constantly. Clean data produces clean UI.

## No build step, ever
The viewer is a static HTML file plus plain JS and CSS. This makes it trivially deployable anywhere (GitHub Pages, local file, USB stick) and auditable by anyone. No bundler, no transpiler, no framework.

## The extractor is the source of truth
The frontend trusts the JSON. It does not re-parse Java, re-read files, or second-guess the extractor's output. If something is wrong in the display, the fix usually belongs in the extractor, not the frontend.

## Progressive disclosure
Show the most important information first. Details (source, inherited methods, transitive interfaces) are available but not forced. The class list, method names, and callability are tier-1 information. Parameter types and source navigation are tier-2.

## Graceful degradation
The viewer should work without a local PZ install (pre-shipped sources in `sources/`). Source links that can't resolve should fail quietly, not break the UI.

## Nav history should mirror intent, not implementation
Pressing Back should return the user to where they consciously navigated to — not to an intermediate state created by an internal function call. This is why `_restoringState` exists and why `noPush` parameters were removed.

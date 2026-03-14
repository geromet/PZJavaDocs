# S08: Navigation State

**Goal:** Ship TASK-031 (full UI state in URL) and TASK-032 (recently viewed classes). Make the viewer's state fully shareable and resumable.
**Demo:** Copy the URL, send it to someone — they see the same class, same filter, same search. Click the recent classes dropdown — jump back to previously viewed classes.

## Must-Haves

- URL encodes: active class, active filter, search query, active tab (classes/globals), ctab (detail/source)
- Pasting a URL with state restores all encoded state
- Recently viewed classes stored in localStorage (last 20)
- Dropdown in header or sidebar shows recent classes; clicking one navigates there
- All existing tests pass

## Tasks

- [ ] **T01: Full UI state in URL (TASK-031)** `est:1.5h`
  - Do: Encode state in URL query params + hash. Update URL on every state change without full reload (use `history.replaceState`). On load, parse URL and restore state. Format: `?filter=exposed&search=Iso&tab=classes&ctab=source#zombie.characters.IsoPlayer`
  - Files: `js/app.js`, `js/state.js`

- [ ] **T02: Recently viewed classes (TASK-032)** `est:1h`
  - Do: On `selectClass()`, push to a localStorage-backed list (max 20, deduplicated, most recent first). Add a small dropdown button near the search bar. Clicking shows the list; clicking an entry navigates.
  - Files: `js/app.js`, `index.html`, `app.css`

- [ ] **T03: Update tests and commit S08** `est:15m`
  - Files: `.gsd/test-suite.py`

## Files Likely Touched

- `js/app.js`, `js/state.js`
- `index.html`, `app.css`
- `.gsd/test-suite.py`

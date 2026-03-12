# Working Agreement

How Claude and the developer collaborate on this project.

## Branch discipline
- **Never push to `main`.** It deploys to GitHub Pages immediately.
- Feature work goes on a named branch (currently `liability-machine`).
- Ask before creating a new branch or merging.

## Commits
- Commit after each logical unit of work, not after every file edit.
- Commit messages: imperative mood, short subject, body explains *why* not *what*.
- Co-author tag: `Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>`

## Extractor runs
- After any change to `extract_lua_api.py`, run it to regenerate `lua_api.json`.
- Copy the result to `pz-lua-api-viewer/lua_api.json` before committing.
- Always check the summary output (class counts, extends counts) against the previous run to catch regressions.

## Testing
- Test locally with `serve.py` before committing frontend changes.
- For extractor changes, run a quick Python sanity check verifying the changed data (see `docs/Knowledge_Base/Testing.md`).

## Scope discipline
- Do not refactor, clean up, or add features that weren't requested.
- If a requested change reveals a nearby problem, flag it — don't silently fix it.
- Ask before making changes to `index.html` structure or CSS variables (shared tokens).

## Documents
- Task files in `docs/Tasks/` should be complete enough to execute in one session without questions.
- Archive documents when done; don't leave completed task files in the root.
- Use `docs/Temp/` for throwaway scripts and data dumps; it's gitignored.

## Known constraints
- `javalang` can't parse Java 14+ switch expressions — `strip_method_bodies()` fallback handles most cases. 5 files still fail parsing entirely (SpriteModel.java, ItemKey.java, etc.).
- `is_relative_to()` requires Python 3.9+.
- GitHub Pages serves from `main` branch root of `pz-lua-api-viewer/`.

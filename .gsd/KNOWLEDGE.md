# Project Knowledge

Append-only register of project-specific rules, patterns, and lessons learned.
Agents read this before every unit. Add entries when you discover something worth remembering.

## Rules

| # | Scope | Rule | Why | Added |
|---|-------|------|-----|-------|

## Patterns

| # | Pattern | Where | Notes |
|---|---------|-------|-------|
| P001 | Publish runtime UI status through DOM data attributes and assert those seams in browser tests. | `#hover-preview`, `#detail-panel`, `#source-panel`, `#source-loading`, `#filter-select`, `#active-filter-chip`, `.gsd/test/s07_ux_polish.py` | This is now the authoritative way to verify pending/ready/error transitions for hover prefetch, detail loading, source loading, and compact filter state. |

## Lessons Learned

| # | What Happened | Root Cause | Fix | Scope |
|---|--------------|------------|-----|-------|
| L001 | The documented `.gsd/test/run.py` slice verification path could not be trusted until late in S07. | The runner and several older browser checks had drifted from current hash routes, selectors, and Playwright lifecycle expectations. | Repair the consolidated runner, give each legacy check a fresh page, and keep the stronger slice-specific pytest module alongside the aggregate report. | Browser verification and future slice close-out work |

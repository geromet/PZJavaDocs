> **FIXED 2026-03-12** — See TASK-004 archive entry.

# BUG-003: "…and N more" subclasses toggle not expanding

**Root Cause:** `.inherit-more{display:none}` set via CSS, but handler checked `moreEl.style.display === 'none'` (inline style). Inline style is `''` until explicitly set, so the first click evaluated `hidden = false` and hid the element. Fixed by checking toggle text content instead.

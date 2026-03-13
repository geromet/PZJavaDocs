# TASK-028: Reduce Color Palette + Tighten Typography

**Parent:** FEAT-015 (McMaster speed & clarity)
**Priority:** Medium — visual clarity
**Blocked by:** Nothing
**Blocks:** TASK-034 (inline CSS)

## Problem

We use 6+ distinct colors (orange, green, blue, purple, teal, red). McMaster uses essentially 2. Too many colors dilute the signal — nothing stands out when everything is colored. We also use ~8 different font sizes (9px to 19px), creating visual noise.

## Plan

### 1. Color consolidation

Reduce to 3 semantic colors + grey:
- **Accent** (orange `#c17f24`): API class names, active states, primary brand
- **Interactive** (blue `#4a9eff`): clickable links, method refs, source cross-refs
- **Success/tag** (green `#6abf69`): keep for @UsedFromLua dots only — don't use as a text color elsewhere
- **Grey** spectrum: all metadata, secondary text, borders

Remove or merge:
- Purple (`#8888ff` for enums) → use a blue variant or just a text label
- Teal (`#50c8a0` for callable) → use blue or grey
- The various badge border colors → simplify to 2 styles

### 2. Typography tightening

Standardize to 4 font sizes:
- **Base:** 14px (body text, table cells) — keep current
- **Small:** 12px (metadata, badges, secondary info)
- **Tiny:** 10px (dot labels, counts) — use sparingly
- **Heading:** 16px (class name in detail panel)

Remove: 9px, 11px, 13px, 15px, 19px as distinct steps. Round everything to the 4-size scale.

### 3. Increase contrast for readability

- `--text-dim: #777` is low contrast on `#1a1a1a` (ratio ~3.5:1). Bump to `#999` (~5:1).
- Ensure all text meets WCAG AA (4.5:1 for normal text).

## Files

- Modified: `app.css` (color variables, font sizes, badge styles)

## Acceptance Criteria

- [ ] No more than 3 non-grey colors used across the UI
- [ ] Font sizes reduced to 4 distinct values
- [ ] All text meets WCAG AA contrast ratio (4.5:1)
- [ ] Enum tag still visually distinct without purple
- [ ] Dots in sidebar still clearly distinguishable
- [ ] Overall feel: cleaner, less noisy, more professional

---
estimated_steps: 3
estimated_files: 1
skills_used: []
---

# T03: Verify S02 features and close out slice

**Slice:** S02 — Tab Enhancements
**Milestone:** M001

## Description

Run the automated test suite to confirm both FEAT-007 (middle-click new tab) and FEAT-014 (hover preview card) pass end-to-end. Confirm no regressions in the broader test suite. Update the S02 summary if anything needs correction.

## Steps

1. Run `python .gsd/test/s02_features.py` and confirm both `test_middle_click_new_tab` and `test_hover_preview_card` pass.
2. Run `python .gsd/test-suite.py` and confirm no regressions across the full suite.
3. Review `.gsd/milestones/M001/slices/S02/S02-SUMMARY.md` — verify it accurately reflects the shipped state. Update if any details are stale.

## Must-Haves

- [ ] `s02_features.py` tests pass for both middle-click and hover preview
- [ ] Full test suite passes with no regressions
- [ ] S02 summary is accurate

## Verification

- `python .gsd/test-suite.py` exits with 0 status
- `test -f .gsd/milestones/M001/slices/S02/S02-SUMMARY.md` — summary exists and is non-empty

## Inputs

- `.gsd/test/s02_features.py` — existing S02 feature tests
- `.gsd/test-suite.py` — full project test suite
- `.gsd/milestones/M001/slices/S02/S02-SUMMARY.md` — existing slice summary

## Expected Output

- `.gsd/milestones/M001/slices/S02/S02-SUMMARY.md` — verified accurate or updated

# S07: UX Polish

**Goal:** Tighten the viewer’s interaction and presentation without changing its feature model: hover should warm the source cache before click, the header/sidebar chrome should use less vertical space, detail rendering should expose stable loading state with no avoidable layout jump, and the detail view should show package breadcrumbs.
**Demo:** Hover a class link for a beat and the next source open is already prefetched; the header uses compact stats plus a filter dropdown instead of a button wall; opening a class shows stable detail/source regions with inspectable loading state; the detail panel starts with a breadcrumb trail such as `zombie > characters > IsoPlayer`.

## Must-Haves

- Hovering a class link starts delayed prefetch of that class source when a source path exists, reuses `sourceCache`, and exposes inspectable prefetch/loading state for verification.
- Header stats collapse into a compact summary and sidebar filters move from the always-visible button wall to a decluttered dropdown/control without losing the current filtering capability.
- The palette/typography pass reduces competing accents, keeps contrast readable, and removes obvious chrome clutter in header, sidebar, and detail sections.
- Detail/source regions reserve stable space on load and during source fetch, with skeleton/loading affordances instead of visible jumps.
- The detail panel renders clickable breadcrumbs derived from the class FQN and wires them into existing class-list filtering/navigation behavior.
- Existing automated coverage still passes, and slice-specific automated checks cover the new UI/runtime behavior.

## Proof Level

- This slice proves: integration
- Real runtime required: yes
- Human/UAT required: yes

## Verification

- `python .gsd/test/run.py`
- `python -m pytest .gsd/test/s07_ux_polish.py`
- `python -m pytest .gsd/test/s07_ux_polish.py -k "loading_state or failure or diagnostics"`
- `python - <<'PY'
import json
from pathlib import Path
report = json.loads(Path('.gsd/test-reports/report.json').read_text())
assert report['failed'] == 0, report
print('report-ok')
PY`
- Manual browser review on local server confirms: hover prefetch marks state before click, breadcrumbs render and are clickable, and no visible header/detail layout jump occurs while opening a class.

## Observability / Diagnostics

- Runtime signals: DOM state attributes/classes for hover prefetch and panel loading (for example on `#hover-preview`, `#source-panel`, or `#detail-panel`) plus existing source-load error text when fetch fails.
- Inspection surfaces: browser DOM inspection, Playwright assertions in `.gsd/test/s07_ux_polish.py`, and `.gsd/test-reports/report.json` screenshots/results.
- Failure visibility: current prefetch state, loading/skeleton visibility, and source fetch failure message remain inspectable in the rendered UI when hover warming or source loading does not complete.
- Redaction constraints: no secrets or local filesystem paths beyond already-visible source-relative paths should be surfaced in new diagnostics.

## Integration Closure

- Upstream surfaces consumed: `index.html` header/sidebar structure, `js/app.js` delegated interaction flow, `js/source-viewer.js` source caching/fetch path, `js/class-detail.js` detail rendering, `.gsd/test/*.py` browser harness.
- New wiring introduced in this slice: hover preview events trigger source prefetch through the shared source-loading path; header/sidebar controls are recomposed in `index.html` + `js/app.js`; breadcrumbs hook detail rendering back into existing navigation/filter behavior.
- What remains before the milestone is truly usable end-to-end: S08 must persist more navigation state in the URL; S09 must reduce payload size and improve asset loading.

## Tasks

- [x] **T01: Wire hover prefetch and inspectable loading state** `est:1.5h`
  - Why: This closes the runtime part of the slice. Prefetch has to reuse the real source-loading path, and the UI needs inspectable state so failures are diagnosable instead of feeling random.
  - Files: `js/app.js`, `js/source-viewer.js`, `.gsd/test/s07_ux_polish.py`, `.gsd/test/run.py`
  - Do: Add delayed hover prefetch for `[data-fqn]` links using the existing delegated hover flow and `sourceCache`, cancel short hovers cleanly, avoid duplicate fetches, and expose durable DOM state for pending/success/error so automated tests can assert it. Extend the browser tests to cover hover warming and at least one diagnostic/failure-state assertion.
  - Verify: `python -m pytest .gsd/test/s07_ux_polish.py -k "prefetch or loading_state"`
  - Done when: Hovering an eligible class link warms source without navigation, repeat hovers reuse cache instead of starting new fetches, and tests can inspect the state transition when prefetch/source load succeeds or fails.
- [ ] **T02: Recompose header, filters, breadcrumbs, and stable layout styling** `est:2h`
  - Why: The visual clutter and layout-shift work all sit on the same markup/CSS seam. Doing them together avoids planning against a header/sidebar structure that changes again one task later.
  - Files: `index.html`, `app.css`, `js/app.js`, `js/class-detail.js`
  - Do: Replace the always-on sidebar filter button wall with a compact control/dropdown backed by the current filter state, compress header stats into a quieter summary row, tighten the palette/typography tokens, add breadcrumb rendering and click behavior in the detail panel, and reserve stable panel/detail loading space with skeleton or placeholder styling rather than late layout expansion.
  - Verify: `python -m pytest .gsd/test/s07_ux_polish.py -k "header or breadcrumbs or layout"`
  - Done when: The header/sidebar consume less vertical space, filters still work through the new control, breadcrumbs render from FQN segments and navigate/filter correctly, and opening a class shows stable reserved space instead of obvious panel jumps.
- [ ] **T03: Close slice verification and docs for UX polish** `est:45m`
  - Why: This slice changes visible behavior across interaction, styling, and diagnostics. The finishing task proves the whole slice together and leaves the test entrypoint current for later slices.
  - Files: `.gsd/test/run.py`, `.gsd/test/s07_ux_polish.py`, `.gsd/milestones/M001/slices/S07/S07-PLAN.md`
  - Do: Register the new S07 browser test module in the main test runner, run the full suite, fix any mismatches between the plan and actual verification targets, and ensure the slice plan still matches the shipped file paths and commands.
  - Verify: `python .gsd/test/run.py && python -m pytest .gsd/test/s07_ux_polish.py`
  - Done when: The full automated suite passes with the S07 coverage included, the standalone S07 tests pass, and the slice plan’s verification commands match reality.

## Files Likely Touched

- `index.html`
- `app.css`
- `js/app.js`
- `js/class-detail.js`
- `js/source-viewer.js`
- `.gsd/test/s07_ux_polish.py`
- `.gsd/test/run.py`

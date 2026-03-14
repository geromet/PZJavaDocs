# Automated Browser Testing Framework

This directory contains automated browser tests for the PZ Lua API Viewer using Playwright.

## Quick Start

### Run All Tests (Recommended)

```bash
python .gsd/test/run.py
```

This will:
1. Start the local server on port 8765
2. Run all test suites
3. Generate an HTML report in `.gsd/test-reports/`
4. Open the report in your browser

### Run Individual Test Suites

```bash
# Via run.py
python -c "from config import *; from s02_features import *; run_tests()"

# Via Playwright CLI
npx playwright test --project chromium
```

## Test Files

| File | Description |
|------|-------------|
| `config.py` | Test configuration, utilities, and helper functions |
| `run.py` | Main runner that orchestrates all tests |
| `s02_features.py` | S02 feature tests: middle-click new tab, hover preview card |
| `navigation.py` | Back/forward button functionality |
| `search.py` | Search input and scope toggle |
| `globals.py` | Globals panel functionality |
| `detail.py` | Class detail panel and method/field clicking |
| `inheritance.py` | Inheritance chain display |
| `regression.py` | Regression tests for no-regression verification |

## Test Coverage

### S02 Features (Implemented)
- **Middle-click new tab**: Middle-clicking a class reference link opens it in a new browser tab
- **Hover preview card**: Hovering over a class link shows a preview card with key details

### Navigation
- Back/Forward button functionality
- URL hash changes when navigating between classes

### Search
- Basic search input
- Search scope toggle (all vs current tab)

### Globals Panel
- Display of global functions
- Clicking a global function navigates to its detail view

### Detail Panel
- Method and field links
- Inheritance chain display
- Copy FQN to clipboard

### Regression
- Tab navigation (Classes <-> Globals)
- Class reference link clicking

## Test Output

Test results are saved to `.gsd/test-reports/` as HTML files. Each test generates:
- A screenshot of the tested element
- Pass/fail status
- Any error messages

View reports: `npx playwright show-report`

## Browser Requirements

- Chromium (or Chrome) must be installed
- Playwright will automatically download browser binaries

## Development Workflow

1. Make changes to the viewer
2. Run tests: `python .gsd/test/run.py`
3. Check HTML reports for visual verification
4. Fix any regressions

## Notes

- Tests run against a local server on port 8765
- Each test captures screenshots for visual regression detection
- Tests are designed to catch UI/UX regressions, not functional bugs

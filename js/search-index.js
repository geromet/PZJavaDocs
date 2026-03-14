'use strict';

/**
 * Pre-computed search index for instant class/method/field lookup.
 *
 * Built once at init from API data. Each entry stores lowercased names
 * so keystroke handlers never call .toLowerCase() on hot paths.
 */

let _searchEntries = []; // [{fqn, simpleLower, fqnLower, methodsLower, fieldsLower, cls}]

function buildSearchIndex(api) {
  const entries = [];
  for (const [fqn, cls] of Object.entries(api.classes)) {
    const parts = fqn.split('.');
    const simple = parts[parts.length - 1];
    entries.push({
      fqn,
      simple,
      simpleLower: simple.toLowerCase(),
      fqnLower:    fqn.toLowerCase(),
      // Join all method names with \0 separator for fast substring search
      methodsLower: cls.methods.map(m => m.name.toLowerCase()).join('\0'),
      fieldsLower:  cls.fields.map(f => f.name.toLowerCase()).join('\0'),
      // Cache method/field counts for match info
      methodNames: cls.methods.map(m => m.name.toLowerCase()),
      fieldNames:  cls.fields.map(f => f.name.toLowerCase()),
      cls,
    });
  }
  // Pre-sort by simple name for stable display order
  entries.sort((a, b) => a.simpleLower.localeCompare(b.simpleLower));
  _searchEntries = entries;
}

/**
 * Query the search index. Returns scored results matching the given term
 * and filter. Much faster than iterating API.classes because all strings
 * are pre-lowercased.
 *
 * @param {string} search - Raw search string (will be lowercased)
 * @param {string} filter - Current filter value (e.g. 'all', 'exposed')
 * @returns {{fqn: string, cls: object, score: number, matchInfo: string|null}[]}
 */
function querySearchIndex(search, filter) {
  const s = search.toLowerCase();
  const results = [];

  for (const entry of _searchEntries) {
    // Apply filter
    if (!passesCurrentFilter(entry.fqn, entry.cls)) continue;

    // Score using pre-lowercased strings
    let score = 0;
    if (entry.simpleLower === s)            score = 100;
    else if (entry.simpleLower.startsWith(s)) score = 80;
    else if (entry.simpleLower.includes(s))   score = 60;
    else if (entry.fqnLower.includes(s))      score = 40;
    else {
      // Check methods/fields using the pre-joined string (fast substring check)
      const hasMethod = entry.methodsLower.includes(s);
      const hasField  = entry.fieldsLower.includes(s);
      if (hasMethod) {
        const mm = entry.methodNames.filter(n => n.includes(s)).length;
        score = 20 + Math.min(mm, 10);
      } else if (hasField) {
        const fm = entry.fieldNames.filter(n => n.includes(s)).length;
        score = 10 + Math.min(fm, 5);
      }
    }

    if (score === 0) continue;

    // Build match info string (only for method/field matches)
    let matchInfo = null;
    if (score < 40) {
      const mm = entry.methodNames.filter(n => n.includes(s)).length;
      const fm = entry.fieldNames.filter(n => n.includes(s)).length;
      const parts = [];
      if (mm > 0) parts.push(`${mm} method${mm > 1 ? 's' : ''}`);
      if (fm > 0) parts.push(`${fm} field${fm > 1 ? 's' : ''}`);
      if (parts.length) matchInfo = parts.join(', ');
    }

    results.push({fqn: entry.fqn, cls: entry.cls, score, matchInfo});
  }

  // Sort: score desc, then name asc
  results.sort((a, b) => {
    if (b.score !== a.score) return b.score - a.score;
    return a.fqn.split('.').pop().toLowerCase().localeCompare(b.fqn.split('.').pop().toLowerCase());
  });

  return results;
}

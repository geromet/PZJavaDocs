'use strict';

function passesCurrentFilter(fqn, cls) {
  if (currentFilter === 'both'               && !(cls.set_exposed && cls.lua_tagged))    return false;
  if (currentFilter === 'exposed'            && !(cls.set_exposed && !cls.lua_tagged))   return false;
  if (currentFilter === 'tagged'             && !(cls.lua_tagged && !cls.set_exposed))   return false;
  if (currentFilter === 'enum'               && !cls.is_enum)                            return false;
  if (currentFilter === 'has-tagged-methods' && !hasTaggedMethods(cls))                  return false;
  if (currentFilter === 'callable'           && !hasCallableMethods(cls))                return false;
  return true;
}

function hasTaggedMethods(cls) { return cls.methods.some(m => m.lua_tagged); }

function hasCallableMethods(cls) {
  return cls.methods.some(m => m.lua_tagged || (!m.static && cls.set_exposed));
}

function scoreClass(fqn, cls, search) {
  if (!search) return 1;
  const s      = search.toLowerCase();
  const simple = fqn.split('.').pop().toLowerCase();
  const fqnL   = fqn.toLowerCase();
  if (simple === s)          return 100;
  if (simple.startsWith(s)) return 80;
  if (simple.includes(s))   return 60;
  if (fqnL.includes(s))     return 40;
  const mm = cls.methods.filter(m => m.name.toLowerCase().includes(s)).length;
  const fm = cls.fields.filter(f  => f.name.toLowerCase().includes(s)).length;
  if (mm > 0) return 20 + Math.min(mm, 10);
  if (fm > 0) return 10 + Math.min(fm, 5);
  return 0;
}

// ── Namespace tree ────────────────────────────────────────────────────────
function buildPackageTree() {
  const root = {children: {}, classes: []};
  for (const [fqn, cls] of Object.entries(API.classes)) {
    if (!passesCurrentFilter(fqn, cls)) continue;
    const parts = fqn.split('.');
    let node = root;
    for (let i = 0; i < parts.length - 1; i++) {
      const part = parts[i];
      if (!node.children[part]) {
        node.children[part] = {name: part, path: parts.slice(0, i + 1).join('.'), children: {}, classes: []};
      }
      node = node.children[part];
    }
    node.classes.push({fqn, cls});
  }
  return root;
}

function countClasses(node) {
  let n = node.classes.length;
  for (const c of Object.values(node.children)) n += countClasses(c);
  return n;
}

function renderPackageTreeHTML(node, depth) {
  let html = '';
  const children = Object.values(node.children).sort((a, b) => a.name.localeCompare(b.name));
  for (const child of children) {
    const total = countClasses(child);
    if (!total) continue;
    const folded = foldedPackages.has(child.path) || (depth >= foldDepth && !openedPackages.has(child.path));
    html += `<div class="pkg-node">
      <div class="pkg-label" data-path="${esc(child.path)}">
        <span class="pkg-arrow">${folded ? '▶' : '▼'}</span>
        <span class="pkg-name">${esc(child.name)}</span>
        <span class="pkg-count">${total}</span>
      </div>
      <div class="pkg-contents${folded ? ' pkg-folded' : ''}">
        ${renderPackageTreeHTML(child, depth + 1)}
      </div>
    </div>`;
  }
  html += renderClassItemsHTML(node.classes, false);
  return html;
}

function renderClassItemsHTML(classes, showPkg) {
  if (!classes.length) return '';
  const sorted = [...classes].sort((a, b) => a.fqn.split('.').pop().localeCompare(b.fqn.split('.').pop()));
  return sorted.map(({fqn, cls}) => {
    const simple = fqn.split('.').pop();
    const htm    = hasTaggedMethods(cls);
    const pkg    = showPkg ? `<div class="ci-pkg">${esc(fqn.split('.').slice(0, -1).join('.'))}</div>` : '';
    return `<div class="class-item${fqn === currentClass ? ' active' : ''}" data-fqn="${esc(fqn)}">
      <div class="ci-text">
        <div class="ci-name">${esc(simple)}${cls.is_enum ? ` <span class="tag tag-enum">enum</span>` : ''}</div>
        ${pkg}
      </div>
      <div class="ci-dots">
        <span class="dot ${cls.set_exposed ? 'dot-exposed' : 'dot-empty'}" title="${cls.set_exposed ? 'setExposed' : 'not setExposed'}"></span>
        <span class="dot ${cls.lua_tagged  ? 'dot-tagged'  : 'dot-empty'}" title="${cls.lua_tagged  ? '@UsedFromLua' : 'not @UsedFromLua'}"></span>
        <span class="dot ${htm             ? 'dot-blue'    : 'dot-empty'}" title="${htm             ? 'has tagged methods' : 'no tagged methods'}"></span>
      </div>
    </div>`;
  }).join('');
}

function collectClassesFromTree(node, out) {
  const children = Object.values(node.children).sort((a, b) => a.name.localeCompare(b.name));
  for (const c of children) collectClassesFromTree(c, out);
  const sorted = [...node.classes].sort((a, b) => a.fqn.split('.').pop().localeCompare(b.fqn.split('.').pop()));
  sorted.forEach(({fqn, cls}) => out.push({fqn, cls, score: 1, matchInfo: null}));
}

function expandPackagePath(fqn) {
  const parts = fqn.split('.');
  for (let i = 1; i < parts.length; i++) foldedPackages.delete(parts.slice(0, i).join('.'));
}

function togglePackage(path) {
  const depth = path.split('.').length - 1;
  if (foldedPackages.has(path)) {
    // manually closed → open it
    foldedPackages.delete(path);
    if (depth >= foldDepth) openedPackages.add(path);
  } else if (openedPackages.has(path)) {
    // explicitly opened beyond depth → close it
    openedPackages.delete(path);
  } else if (depth >= foldDepth) {
    // folded by depth → open it explicitly
    openedPackages.add(path);
  } else {
    // open normally → manually close
    foldedPackages.add(path);
  }
  buildClassList();
}

function getMaxPackageDepth() {
  let max = 0;
  for (const fqn of Object.keys(API.classes)) max = Math.max(max, fqn.split('.').length - 1);
  return max;
}

function updateDepthLabel() {
  const label = document.getElementById('fdc-depth-label');
  if (!label) return;
  label.textContent = foldDepth === Infinity ? 'all open' : `depth ${foldDepth}`;
}

function adjustFoldDepth(delta) {
  const maxD = getMaxPackageDepth();
  if (delta < 0) foldDepth = foldDepth === Infinity ? maxD - 1 : Math.max(0, foldDepth - 1);
  else           foldDepth = foldDepth >= maxD ? Infinity : foldDepth + 1;
  foldedPackages.clear();
  // Prune any explicitly opened packages that are now within the new depth
  for (const p of [...openedPackages]) {
    if (p.split('.').length - 1 >= foldDepth) openedPackages.delete(p);
  }
  updateDepthLabel();
  buildClassList();
}

// ── Progressive rendering ─────────────────────────────────────────────────
let _pendingRenderResults = null;
let _pendingRenderRafId  = null;

function renderSearchResultsBatch(list, results, startIdx, search, batchSize) {
  const frag = document.createDocumentFragment();
  const end = Math.min(startIdx + batchSize, results.length);
  for (let i = startIdx; i < end; i++) {
    const {fqn, cls, matchInfo} = results[i];
    const parts  = fqn.split('.');
    const simple = parts.pop();
    const pkg    = parts.join('.');
    const htm    = hasTaggedMethods(cls);
    const div    = document.createElement('div');
    div.className = 'class-item' + (fqn === currentClass ? ' active' : '');
    div.dataset.fqn = fqn;
    if (matchInfo) div.dataset.matchInfo = matchInfo;
    div.innerHTML =
      `<div class="ci-text"><div class="ci-name">${highlightMatch(simple, search)}${cls.is_enum ? ` <span class="tag tag-enum">enum</span>` : ''}</div><div class="ci-pkg">${highlightMatch(pkg, search)}</div></div>` +
      `<div class="ci-right"><div class="ci-dots">` +
      `<span class="dot ${cls.set_exposed ? 'dot-exposed' : 'dot-empty'}"></span>` +
      `<span class="dot ${cls.lua_tagged  ? 'dot-tagged'  : 'dot-empty'}"></span>` +
      `<span class="dot ${htm             ? 'dot-blue'    : 'dot-empty'}"></span>` +
      `</div>${matchInfo ? `<span class="ci-match">${matchInfo}</span>` : ''}</div>`;
    frag.appendChild(div);
  }
  list.appendChild(frag);
  return end;
}

// ── Class list ────────────────────────────────────────────────────────────
function buildClassList() {
  const search = currentSearch.trim();

  // Cancel any pending progressive render
  if (_pendingRenderRafId) {
    cancelAnimationFrame(_pendingRenderRafId);
    _pendingRenderRafId = null;
  }

  if (search) {
    // Use pre-computed search index for fast lookup
    const results = querySearchIndex(search, currentFilter);
    filteredResults = results;
    document.getElementById('class-count').textContent = `${results.length} classes`;

    const list = document.getElementById('class-list');
    list.innerHTML = '';

    // Progressive rendering: first 50 immediately, rest in animation frames
    const FIRST_BATCH = 50;
    const SUBSEQUENT_BATCH = 100;
    renderSearchResultsBatch(list, results, 0, search, FIRST_BATCH);

    if (results.length > FIRST_BATCH) {
      let idx = FIRST_BATCH;
      function renderMore() {
        // Bail if search changed while we were rendering
        if (currentSearch.trim() !== search) return;
        idx = renderSearchResultsBatch(list, results, idx, search, SUBSEQUENT_BATCH);
        if (idx < results.length) {
          _pendingRenderRafId = requestAnimationFrame(renderMore);
        }
      }
      _pendingRenderRafId = requestAnimationFrame(renderMore);
    }
    return;
  }

  // Namespace tree when not searching
  const root = buildPackageTree();
  filteredResults = [];
  collectClassesFromTree(root, filteredResults);
  document.getElementById('class-count').textContent = `${filteredResults.length} classes`;

  const list = document.getElementById('class-list');
  list.innerHTML = renderPackageTreeHTML(root, 0);
  // Click handlers are managed via event delegation on #class-list (see app.js)
}

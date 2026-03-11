'use strict';

// ── Load API ──────────────────────────────────────────────────────────────
fetch('./lua_api.json')
  .then(r => { if (!r.ok) throw new Error(r.statusText); return r.json(); })
  .then(data => {
    API = data;
    document.getElementById('loading').style.display = 'none';
    document.getElementById('placeholder').style.display = 'flex';
    init();
  })
  .catch(e => {
    document.getElementById('loading').innerHTML =
      `<div style="color:#e06c75;text-align:center">Failed to load lua_api.json<br><small>${e.message}</small></div>`;
  });

function init() {
  const m = API._meta;
  document.getElementById('stat-exposed').textContent = m.set_exposed_count;
  document.getElementById('stat-tagged').textContent  = m.lua_tagged_count;
  document.getElementById('stat-globals').textContent = `${m.total_global_functions} globals`;

  // Build simple-name → [fqn, …] lookup for global source ref detection
  for (const fqn of Object.keys(API.classes)) {
    const simple = fqn.split('.').pop();
    if (!classBySimpleName[simple]) classBySimpleName[simple] = [];
    classBySimpleName[simple].push(fqn);
  }

  buildClassList();
  setupEvents();
  if (location.hash) {
    const val = decodeURIComponent(location.hash.slice(1));
    if (val === 'globals') switchTab('globals');
    else selectClass(val);
  }
}

// ── Navigation ────────────────────────────────────────────────────────────
function navigateList(dir) {
  if (!filteredResults.length || currentTab !== 'classes') return;
  const idx  = filteredResults.findIndex(r => r.fqn === currentClass);
  const next = idx === -1
    ? (dir > 0 ? 0 : filteredResults.length - 1)
    : (idx + dir + filteredResults.length) % filteredResults.length;
  const {fqn, matchInfo} = filteredResults[next];
  selectClass(fqn, matchInfo);
}

// ── Class selection ───────────────────────────────────────────────────────
function selectClass(fqn, matchInfo) {
  if (!API.classes[fqn]) return;
  currentClass = fqn;

  // Auto-expand the package path in tree mode
  if (!currentSearch.trim()) {
    expandPackagePath(fqn);
    buildClassList();
  }

  showNonCallable = false;

  if (matchInfo && currentSearch) {
    const s   = currentSearch.toLowerCase();
    const cls = API.classes[fqn];
    methodSearch = cls.methods.some(m => m.name.toLowerCase().includes(s)) ? currentSearch : '';
    fieldSearch  = cls.fields.some(f  => f.name.toLowerCase().includes(s)) ? currentSearch : '';
  } else { methodSearch = ''; fieldSearch = ''; }

  location.hash = encodeURIComponent(fqn);
  document.querySelectorAll('.class-item').forEach(el => el.classList.toggle('active', el.dataset.fqn === fqn));
  document.querySelector('.class-item.active')?.scrollIntoView({block: 'nearest'});

  document.getElementById('placeholder').style.display = 'none';
  showGlobalsPanel(false);
  document.getElementById('content-tabs').classList.add('visible');
  renderClassDetail(fqn);
  if (currentCtab === 'source') showSource(API.classes[fqn]);
}

// ── Content tab switching ─────────────────────────────────────────────────
function switchCtab(name) {
  currentCtab = name;
  document.querySelectorAll('.ctab').forEach(t => t.classList.toggle('active', t.dataset.ctab === name));
  document.getElementById('detail-panel').classList.toggle('visible', name === 'detail');
  document.getElementById('source-panel').classList.toggle('visible', name === 'source');
}

// ── Tab switching ─────────────────────────────────────────────────────────
function switchTab(tab) {
  currentTab = tab;
  document.querySelectorAll('.tab').forEach(t => t.classList.toggle('active', t.dataset.tab === tab));
  document.getElementById('sidebar').style.display = tab === 'classes' ? 'flex' : 'none';
  if (tab === 'globals') { initGlobals(); location.hash = 'globals'; }
  else {
    showGlobalsPanel(false);
    if (currentClass) renderClassDetail(currentClass);
    else document.getElementById('placeholder').style.display = 'flex';
  }
}

// ── Events ────────────────────────────────────────────────────────────────
function setupEvents() {
  // Local folder picker
  document.getElementById('btn-folder').addEventListener('click', async () => {
    if (!('showDirectoryPicker' in window)) {
      alert('Your browser does not support the File System Access API.\nTry Chrome or Edge.\n\nPre-shipped sources will be used instead.');
      return;
    }
    try {
      localDirHandle = await window.showDirectoryPicker({mode: 'read'});
      Object.keys(sourceCache).forEach(k => delete sourceCache[k]);
      const btn = document.getElementById('btn-folder');
      btn.textContent = `✓ ${localDirHandle.name}`;
      btn.classList.add('loaded');
      if (currentClass && currentCtab === 'source') showSource(API.classes[currentClass]);
    } catch { /* user cancelled */ }
  });

  // Main tabs
  document.querySelectorAll('.tab').forEach(t =>
    t.addEventListener('click', () => switchTab(t.dataset.tab)));

  // Content sub-tabs
  document.querySelectorAll('.ctab').forEach(t =>
    t.addEventListener('click', () => {
      if (!currentClass) return;
      if (t.dataset.ctab === 'source') showSource(API.classes[currentClass]);
      else switchCtab('detail');
    }));

  // Filter buttons
  document.querySelectorAll('.filter-btn').forEach(btn =>
    btn.addEventListener('click', () => {
      currentFilter = btn.dataset.filter;
      document.querySelectorAll('.filter-btn').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      buildClassList();
    }));

  // Fold depth controls
  document.getElementById('btn-fold-all-pkg').addEventListener('click', () => {
    foldDepth = 0; foldedPackages.clear(); openedPackages.clear(); updateDepthLabel(); buildClassList();
  });
  document.getElementById('btn-unfold-all-pkg').addEventListener('click', () => {
    foldDepth = Infinity; foldedPackages.clear(); openedPackages.clear(); updateDepthLabel(); buildClassList();
  });
  document.getElementById('btn-fold-less').addEventListener('click', () => adjustFoldDepth(-1));
  document.getElementById('btn-fold-more').addEventListener('click', () => adjustFoldDepth(+1));

  // Global group fold buttons
  document.getElementById('btn-fold-groups').addEventListener('click', () => {
    document.querySelectorAll('#globals-table-wrap .globals-group-header').forEach(hdr => {
      foldedGlobalGroups.add(hdr.dataset.group);
    });
    updateGlobalsTable(document.getElementById('globals-search')?.value || '');
  });
  document.getElementById('btn-unfold-groups').addEventListener('click', () => {
    foldedGlobalGroups.clear();
    updateGlobalsTable(document.getElementById('globals-search')?.value || '');
  });

  // Globals back button
  document.getElementById('globals-back-btn').addEventListener('click', backToGlobalsTable);

  // Source toolbar — class source
  document.getElementById('src-fold-all').addEventListener('click', () =>
    foldAllInEl(document.getElementById('source-code'), 'all'));
  document.getElementById('src-unfold-all').addEventListener('click', () =>
    unfoldAllInEl(document.getElementById('source-code')));
  document.getElementById('src-fold-methods').addEventListener('click', () =>
    foldAllInEl(document.getElementById('source-code'), 'methods'));

  // Source toolbar — global source
  document.getElementById('gsrc-fold-all').addEventListener('click', () =>
    foldAllInEl(document.getElementById('globals-src-code'), 'all'));
  document.getElementById('gsrc-unfold-all').addEventListener('click', () =>
    unfoldAllInEl(document.getElementById('globals-src-code')));
  document.getElementById('gsrc-fold-methods').addEventListener('click', () =>
    foldAllInEl(document.getElementById('globals-src-code'), 'methods'));

  // Global search bar
  let searchTimer;
  const searchEl = document.getElementById('global-search');
  searchEl.addEventListener('input', e => {
    clearTimeout(searchTimer);
    searchTimer = setTimeout(() => { currentSearch = e.target.value; buildClassList(); }, 150);
  });
  searchEl.addEventListener('keydown', e => {
    if (e.key === 'ArrowDown') { e.preventDefault(); navigateList(+1); }
    if (e.key === 'ArrowUp')   { e.preventDefault(); navigateList(-1); }
    if (e.key === 'Enter')     { e.preventDefault(); searchEl.blur(); }
  });

  // Keyboard shortcuts
  document.addEventListener('keydown', e => {
    const tag = document.activeElement.tagName;
    if (e.key === '/' && tag !== 'INPUT') { e.preventDefault(); searchEl.focus(); }
    if (e.key === 'Escape' && tag === 'INPUT') document.activeElement.blur();
    if (tag !== 'INPUT' && currentTab === 'classes') {
      if (e.key === 'ArrowDown') { e.preventDefault(); navigateList(+1); }
      if (e.key === 'ArrowUp')   { e.preventDefault(); navigateList(-1); }
    }
  });

  // Delegated click for detail panel: method links + group label folding
  document.getElementById('detail-panel').addEventListener('click', e => {
    // Method / constructor source links
    const a = e.target.closest('a.method-link[data-method]');
    if (a) { e.preventDefault(); showSource(API.classes[currentClass], a.dataset.method); return; }

    // Group label → fold/unfold that group
    const groupLabel = e.target.closest('.method-group .group-label');
    if (groupLabel) {
      const group = groupLabel.closest('.method-group');
      if (group) {
        group.classList.toggle('folded');
        const arrow = groupLabel.querySelector('.group-arrow');
        if (arrow) arrow.textContent = group.classList.contains('folded') ? '▶' : '▼';
      }
    }
  });
}

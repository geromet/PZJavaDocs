'use strict';

// ── Load API (version-aware) ───────────────────────────────────────────────
(async function loadApi() {
  // Check for ?v= query param
  const params   = new URLSearchParams(location.search);
  const vParam   = params.get('v');

  // Try to load versions manifest (gracefully absent)
  let versionsManifest = null;
  try {
    const r = await fetch('./versions/versions.json');
    if (r.ok) versionsManifest = await r.json();
  } catch { /* no versions manifest — single-file mode */ }

  // Determine which API file to fetch
  let apiFile       = './lua_api.json';
  let activeVersion = null;
  if (versionsManifest && versionsManifest.length > 0) {
    // Honour ?v= param; fall back to first (newest) entry
    const match = vParam ? versionsManifest.find(v => v.id === vParam) : null;
    const entry = match || versionsManifest[0];
    apiFile       = './' + entry.file;
    activeVersion = entry.id;
  }

  try {
    const r = await fetch(apiFile);
    if (!r.ok) throw new Error(`${r.status} ${r.statusText}`);
    API = await r.json();
    document.getElementById('loading').style.display = 'none';
    document.getElementById('placeholder').style.display = 'flex';
    setupVersionDropdown(versionsManifest, activeVersion);
    init();
  } catch (e) {
    document.getElementById('loading').innerHTML =
      `<div style="color:#e06c75;text-align:center">Failed to load API data<br><small>${e.message}</small></div>`;
  }
})();

// ── Version dropdown ───────────────────────────────────────────────────────
function setupVersionDropdown(manifest, currentId) {
  const sel = document.getElementById('version-select');
  if (!manifest || manifest.length <= 1) {
    sel.style.display = 'none';
    return;
  }
  sel.innerHTML = manifest.map(v =>
    `<option value="${esc(v.id)}"${v.id === currentId ? ' selected' : ''}>${esc(v.label)}</option>`
  ).join('');
  sel.style.display = '';
  sel.addEventListener('change', () => {
    const p = new URLSearchParams(location.search);
    p.set('v', sel.value);
    // Preserve existing hash (currently open class) across version switch
    const hash = location.hash;
    location.href = `${location.pathname}?${p.toString()}${hash}`;
    // Push version change to history so Alt+Left/Right can navigate back
    navPush({type: 'versionSwitch', version: sel.value});
  });
}

function init() {
  const m = API._meta;
  const statsContainer = document.getElementById('header-stats');
  if (statsContainer) {
    const exposed = document.createElement('span');
    exposed.className = 'hstat';
    exposed.innerHTML = `<span class="hstat-dot" style="background:var(--col-exposed)"></span><span id="stat-exposed">…</span> setExposed`;
    statsContainer.appendChild(exposed);
    const tagged = document.createElement('span');
    tagged.className = 'hstat';
    tagged.innerHTML = `<span class="hstat-dot" style="background:var(--col-tagged)"></span><span id="stat-tagged">…</span> @UsedFromLua`;
    statsContainer.appendChild(tagged);
    const globals = document.createElement('span');
    globals.id = 'stat-globals';
    globals.style.color = 'var(--text-dim)';
    globals.textContent = `${m.total_global_functions} globals`;
    statsContainer.appendChild(globals);
  }

  // Build simple-name → [fqn, …] lookup for source class-ref linking
  // Fast path: use precomputed maps from lua_api.json if present
  if (API._class_by_simple_name) {
    Object.assign(classBySimpleName, API._class_by_simple_name);
    Object.assign(sourceOnlyPaths,   API._source_only_paths || {});
  } else {
    // Fallback: compute on the fly (backwards compatibility with older JSON)
    for (const fqn of Object.keys(API.classes)) {
      const simple = fqn.split('.').pop();
      (classBySimpleName[simple] = classBySimpleName[simple] || []).push(fqn);
    }
    for (const [simple, path] of Object.entries(API._source_index || {})) {
      if (!classBySimpleName[simple]) {
        sourceOnlyPaths[simple] = path;
      }
    }
  }

  buildSearchIndex(API);
  buildClassList();
  setupEvents();
  initSplitter('sidebar-splitter', 'sidebar',       'splitW-sidebar');
  initSplitter('classes-splitter', 'detail-panel', 'splitW-classes');
  initSplitter('globals-splitter', 'globals-left',  'splitW-globals');
  if (localStorage.getItem('splitLayout') === '1') applySplitLayout(true);
  // Seed history with a placeholder entry so Alt+Left from the first class
  // can always navigate back to the "no class selected" state.
  navHistory.push({type: 'placeholder'});
  navIndex = 0;
  updateNavButtons();
  if (location.hash) {
    const val = decodeURIComponent(location.hash.slice(1));
    if (val === 'globals') switchTab('globals');
    else selectClass(val);
  }
  
  // Version selector — change event listener
  const versionSelect = document.getElementById('version-select');
  if (versionSelect) {
    versionSelect.addEventListener('change', () => {
      const p = new URLSearchParams(location.search);
      p.set('v', versionSelect.value);
      const hash = location.hash;
      location.href = `${location.pathname}?${p.toString()}${hash}`;
    });
  }

  // Filter dropdown — close when clicking outside
  const filterDropdown = document.getElementById('filter-dropdown');
  document.getElementById('sidebar-filters').addEventListener('click', (e) => {
    if (e.target.closest('.filter-dropdown')) return;
    filterDropdown.classList.remove('visible');
  });
}

// ── Resizable splitters ───────────────────────────────────────────────────
function initSplitter(splitterId, leftId, storageKey) {
  const splitter = document.getElementById(splitterId);
  const leftEl   = document.getElementById(leftId);
  if (!splitter || !leftEl) return;
  const saved = localStorage.getItem(storageKey);
  if (saved) leftEl.style.flex = `0 0 ${saved}px`;
  let startX, startW;
  splitter.addEventListener('mousedown', e => {
    startX = e.clientX;
    startW = leftEl.getBoundingClientRect().width;
    splitter.classList.add('dragging');
    document.body.style.userSelect = 'none';
    document.body.style.cursor = 'col-resize';
    e.preventDefault();
  });
  document.addEventListener('mousemove', e => {
    if (!splitter.classList.contains('dragging')) return;
    const w = Math.max(150, Math.min(startW + e.clientX - startX, window.innerWidth - 200));
    leftEl.style.flex = `0 0 ${w}px`;
    localStorage.setItem(storageKey, w);
  });
  document.addEventListener('mouseup', () => {
    if (!splitter.classList.contains('dragging')) return;
    splitter.classList.remove('dragging');
    document.body.style.userSelect = '';
    document.body.style.cursor = '';
  });
}

// ── Page history ──────────────────────────────────────────────────────────
function navPush(state) {
  // Suppressed during applyState (history restoration must never write history)
  if (_restoringState) return;
  // Drop any forward entries after current position
  navHistory.splice(navIndex + 1);
  // Skip duplicate of the current top entry
  const top = navHistory[navHistory.length - 1];
  if (top && top.type === state.type && top.fqn === state.fqn && top.javaMethod === state.javaMethod) return;
  navHistory.push(state);
  navIndex = navHistory.length - 1;
  updateNavButtons();
}

function captureState() {
  if (currentTab === 'globals') {
    const srcWrap = document.getElementById('globals-source-wrap');
    if (srcWrap?.classList.contains('has-source')) {
      return { type: 'globalSource', javaMethod: document.getElementById('globals-src-title').textContent };
    }
    return { type: 'globals' };
  }
  if (!currentClass) return { type: 'placeholder' };
  return { type: 'class', fqn: currentClass, ctab: currentCtab };
}

async function applyState(s) {
  const seq = ++navSeq;
  _restoringState = true;
  try {
    if (s.type === 'placeholder') {
      showGlobalsPanel(false);
      document.getElementById('content-tabs').classList.remove('visible');
      document.getElementById('detail-panel').classList.remove('visible');
      document.getElementById('source-panel').classList.remove('visible');
      currentClass = null;
      document.getElementById('placeholder').style.display = 'flex';
      location.hash = '';
      return;
    }
    if (s.type === 'globals') {
      switchTab('globals');
      return;
    }
    if (s.type === 'globalSource') {
      // Set globals tab active without calling initGlobals (avoids table-view flash)
      currentTab = 'globals';
      document.querySelectorAll('.tab').forEach(t => t.classList.toggle('active', t.dataset.tab === 'globals'));
      document.getElementById('sidebar').style.display = 'none';
      showGlobalsPanel(true);
      document.getElementById('placeholder').style.display = 'none';
      await showGlobalSource(s.javaMethod);
      return;
    }
    if (s.type === 'class') {
      switchTab('classes');
      selectClass(s.fqn, null);
      return;
    }
  } finally {
    _restoringState = false;
  }
}

async function navGo(delta) {
  const next = navIndex + delta;
  if (next < 0 || next >= navHistory.length) return;
  navIndex = next;
  updateNavButtons();
  await applyState(navHistory[navIndex]);
}

function updateNavButtons() {
  const back    = document.getElementById('btn-nav-back');
  const forward = document.getElementById('btn-nav-forward');
  if (back)    back.disabled    = navIndex <= 0;
  if (forward) forward.disabled = navIndex >= navHistory.length - 1;
}

// ── List keyboard navigation ───────────────────────────────────────────────
function navigateList(dir) {
  if (!filteredResults.length || currentTab !== 'classes') return;
  const idx  = filteredResults.findIndex(r => r.fqn === currentClass);
  const next = idx === -1
    ? (dir > 0 ? 0 : filteredResults.length - 1)
    : (idx + dir + filteredResults.length) % filteredResults.length;
  const {fqn, matchInfo} = filteredResults[next];
  selectClass(fqn, matchInfo);
}

// ── Tab bar ───────────────────────────────────────────────────────────────
function renderTabBar() {
  const bar = document.getElementById('tab-bar');
  bar.classList.toggle('visible', tabs.length > 0 && currentTab === 'classes');
  bar.innerHTML = tabs.map((t, i) =>
    `<div class="tab-item${i === activeTabIdx ? ' active' : ''}" data-tabidx="${i}">
       <span class="tab-item-name">${esc(t.fqn.split('.').pop())}</span>
       <span class="tab-close" data-closeidx="${i}" title="Close tab">×</span>
     </div>`
  ).join('');
  bar.querySelectorAll('.tab-item').forEach(el =>
    el.addEventListener('click', e => {
      if (e.target.closest('.tab-close')) return;
      activateTab(parseInt(el.dataset.tabidx));
    }));
  bar.querySelectorAll('.tab-close').forEach(el =>
    el.addEventListener('click', () => closeTab(parseInt(el.dataset.closeidx))));
}

function saveActiveTabState() {
  const tab = activeTab();
  if (!tab) return;
  tab.ctab        = currentCtab;
  tab.methodSearch = methodSearch;
  tab.fieldSearch  = fieldSearch;
  tab.scrollDetail = document.getElementById('detail-panel').scrollTop;
  const pre = document.getElementById('source-pre');
  tab.scrollSource = pre ? pre.scrollTop : 0;
}

function activateTab(idx) {
  // Save the tab we're leaving before switching (skip if already on this tab)
  if (idx !== activeTabIdx) saveActiveTabState();
  activeTabIdx = idx;
  const tab = tabs[idx];
  currentClass  = tab.fqn;
  methodSearch  = tab.methodSearch;
  fieldSearch   = tab.fieldSearch;
  location.hash = encodeURIComponent(tab.fqn);
  document.getElementById('placeholder').style.display = 'none';
  document.getElementById('content-tabs').classList.add('visible');
  showGlobalsPanel(false);
  renderClassDetail(tab.fqn);
  switchCtab(tab.ctab);
  // Restore scroll after render; also load source panel if needed
  requestAnimationFrame(() => {
    document.getElementById('detail-panel').scrollTop = tab.scrollDetail || 0;
    if (tab.ctab === 'source' || splitLayout) {
      showSource(API.classes[tab.fqn]).then(() => {
        if (tab.ctab === 'source') {
          const pre = document.getElementById('source-pre');
          if (pre) pre.scrollTop = tab.scrollSource || 0;
        }
      });
    }
  });
  renderTabBar();
  document.querySelectorAll('.class-item').forEach(el =>
    el.classList.toggle('active', el.dataset.fqn === tab.fqn));
  document.querySelector('.class-item.active')?.scrollIntoView({block: 'nearest'});
}

function closeTab(idx) {
  tabs.splice(idx, 1);
  if (tabs.length === 0) {
    activeTabIdx = -1;
    currentClass = null;
    document.getElementById('tab-bar').classList.remove('visible');
    document.getElementById('content-tabs').classList.remove('visible');
    document.getElementById('detail-panel').classList.remove('visible');
    document.getElementById('source-panel').classList.remove('visible');
    document.getElementById('placeholder').style.display = 'flex';
    location.hash = '';
    renderTabBar();
    return;
  }
  if (idx === activeTabIdx) {
    // Closed the active tab — activate the nearest remaining tab
    activeTabIdx = Math.min(idx, tabs.length - 1);
    activateTab(activeTabIdx);
  } else {
    // Closed a non-active tab — just shift the active index if needed and re-render
    if (idx < activeTabIdx) activeTabIdx--;
    renderTabBar();
  }
}

// ── Class selection ───────────────────────────────────────────────────────
function selectClass(fqn, matchInfo, jumpToMethod) {
  if (!API.classes[fqn]) return;

  navPush({type: 'class', fqn});

  // Auto-expand the package path in tree mode
  if (!currentSearch.trim()) {
    expandPackagePath(fqn);
    buildClassList();
  }

  showNonCallable = false;

  const existing = tabs.findIndex(t => t.fqn === fqn);
  if (existing !== -1) {
    // Class already open — activate its tab (restores saved state)
    activateTab(existing);
  } else {
    // Save current tab state before creating a new one
    saveActiveTabState();

    // Compute method/field search for the new tab from matchInfo
    let ms = '', fs = '';
    if (matchInfo && currentSearch) {
      const s   = currentSearch.toLowerCase();
      const cls = API.classes[fqn];
      ms = cls.methods.some(m => m.name.toLowerCase().includes(s)) ? currentSearch : '';
      fs = cls.fields.some(f  => f.name.toLowerCase().includes(s)) ? currentSearch : '';
    }

    // Cap at 10 tabs — evict the oldest non-active tab
    if (tabs.length >= 10) {
      const dropIdx = tabs.findIndex((_, i) => i !== activeTabIdx);
      if (dropIdx !== -1) {
        tabs.splice(dropIdx, 1);
        if (activeTabIdx > dropIdx) activeTabIdx--;
      }
    }

    tabs.push({ fqn, ctab: 'detail', scrollDetail: 0, scrollSource: 0, methodSearch: ms, fieldSearch: fs });
    activeTabIdx = tabs.length - 1;
    activateTab(activeTabIdx);
  }

  if (jumpToMethod) showSource(API.classes[fqn], jumpToMethod);
}

// ── Content tab switching ─────────────────────────────────────────────────
function switchCtab(name) {
  currentCtab = name;
  document.querySelectorAll('.ctab').forEach(t => t.classList.toggle('active', t.dataset.ctab === name));
  if (splitLayout) {
    // Both panels always visible in split mode
    document.getElementById('detail-panel').classList.add('visible');
    document.getElementById('source-panel').classList.add('visible');
  } else {
    document.getElementById('detail-panel').classList.toggle('visible', name === 'detail');
    document.getElementById('source-panel').classList.toggle('visible', name === 'source');
  }
}

// ── Split layout ──────────────────────────────────────────────────────────
function applySplitLayout(enabled) {
  splitLayout = enabled && window.innerWidth > 900;
  document.getElementById('content').classList.toggle('split-layout', splitLayout);
  const btn = document.getElementById('btn-split-toggle');
  btn.textContent = splitLayout ? '⊟' : '⊞';
  btn.title = splitLayout ? 'Switch to single panel' : 'Switch to split panel';
  localStorage.setItem('splitLayout', splitLayout ? '1' : '0');
  if (currentClass) {
    if (splitLayout) {
      document.getElementById('detail-panel').classList.add('visible');
      document.getElementById('source-panel').classList.add('visible');
      if (!document.getElementById('source-code').textContent.trim())
        showSource(API.classes[currentClass]);
    } else {
      switchCtab(currentCtab);
    }
  }
}

// ── Tab switching ─────────────────────────────────────────────────────────
function switchTab(tab) {
  currentTab = tab;
  document.querySelectorAll('.tab').forEach(t => t.classList.toggle('active', t.dataset.tab === tab));
  document.getElementById('sidebar').style.display = tab === 'classes' ? 'flex' : 'none';
  document.getElementById('tab-bar').classList.toggle('visible', tab === 'classes' && tabs.length > 0);
  if (tab === 'globals') {
    initGlobals();
    location.hash = 'globals';
    navPush({type: 'globals'});
  } else {
    showGlobalsPanel(false);
    if (currentClass) renderClassDetail(currentClass);
    else document.getElementById('placeholder').style.display = 'flex';
  }
}

// ── Events ────────────────────────────────────────────────────────────────
function setupEvents() {
  // Back / forward buttons
  document.getElementById('btn-nav-back').addEventListener('click',    () => navGo(-1));
  document.getElementById('btn-nav-forward').addEventListener('click', () => navGo(+1));

  // Split layout toggle (classes tab only)
  document.getElementById('btn-split-toggle').addEventListener('click', () => applySplitLayout(!splitLayout));
  window.addEventListener('resize', () => { if (splitLayout && window.innerWidth <= 900) applySplitLayout(false); });

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
    btn.addEventListener('click', (e) => {
      const dropdown = document.getElementById('filter-dropdown');
      // Toggle dropdown visibility when clicking the dropdown button
      if (btn.dataset.filter === 'dropdown') {
        dropdown.classList.toggle('visible');
        e.preventDefault();
        return;
      }
      currentFilter = btn.dataset.filter;
      document.querySelectorAll('.filter-btn').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      buildClassList();
    }));

  // Event delegation for class list (search results + tree items)
  document.getElementById('class-list').addEventListener('click', e => {
    const item = e.target.closest('.class-item');
    if (item && item.dataset.fqn) {
      if (e.button === 1 || e.ctrlKey) {
        openNewTab(item.dataset.fqn);
        e.preventDefault();
        return;
      }
      selectClass(item.dataset.fqn, item.dataset.matchInfo || null);
      return;
    }
    const label = e.target.closest('.pkg-label');
    if (label && label.dataset.path) {
      togglePackage(label.dataset.path);
    }
  });

  // Filter dropdown
  document.getElementById('filter-dropdown').addEventListener('change', () => {
    currentFilter = document.getElementById('filter-dropdown').value;
    document.querySelectorAll('.filter-btn').forEach(b => b.classList.remove('active'));
    // Mark the selected filter button as active
    const btns = document.querySelectorAll('.filter-btn[data-filter]');
    btns.forEach(btn => {
      if (btn.dataset.filter === currentFilter) {
        btn.classList.add('active');
      }
    });
    buildClassList();
  });

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
    const wrap = document.getElementById('globals-table-wrap');
    wrap.querySelectorAll('.globals-dom-header').forEach(hdr => foldedGlobalGroups.add(hdr.dataset.domkey));
    wrap.querySelectorAll('.globals-sec-header').forEach(hdr => foldedGlobalGroups.add(hdr.dataset.seckey));
    wrap.querySelectorAll('.globals-grp-header').forEach(hdr => foldedGlobalGroups.add(hdr.dataset.grpkey));
    saveGlobalGroupFolds();
    updateGlobalsTable(document.getElementById('globals-search')?.value || '');
  });
  document.getElementById('btn-unfold-groups').addEventListener('click', () => {
    foldedGlobalGroups.clear();
    saveGlobalGroupFolds();
    updateGlobalsTable(document.getElementById('globals-search')?.value || '');
  });

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
  const searchClearBtn = document.getElementById('btn-search-clear');
  searchEl.addEventListener('input', e => {
    clearTimeout(searchTimer);
    searchClearBtn.classList.toggle('visible', !!searchEl.value);
    searchTimer = setTimeout(() => { currentSearch = e.target.value; buildClassList(); }, 150);
  });
  searchClearBtn.addEventListener('click', () => {
    searchEl.value = '';
    currentSearch = '';
    searchClearBtn.classList.remove('visible');
    buildClassList();
    searchEl.focus();
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
    // Alt+Left / Alt+Right for back/forward (common browser convention)
    if (e.altKey && e.key === 'ArrowLeft')  { e.preventDefault(); navGo(-1); }
    if (e.altKey && e.key === 'ArrowRight') { e.preventDefault(); navGo(+1); }
  });

  // ── Hover preview card (FEAT-014) ──────────────────────────────────────
  (function () {
    const card  = document.getElementById('hover-preview');
    let timer   = null;
    let lastFqn = null;

    function showCard(fqn, anchorEl) {
      const cls = API && API.classes && API.classes[fqn];
      if (!cls) return;
      lastFqn = fqn;

      // Build content
      const badges = [
        cls.set_exposed ? '<span class="hp-badge hp-badge-exposed">setExposed</span>' : '',
        cls.lua_tagged  ? '<span class="hp-badge hp-badge-tagged">@UsedFromLua</span>'  : '',
        cls.is_enum     ? '<span class="hp-badge hp-badge-enum">enum</span>'            : '',
      ].filter(Boolean).join('');

      const methodCount = cls.methods ? cls.methods.length : 0;
      const fieldCount  = cls.fields  ? cls.fields.length  : 0;

      // First 3 callable methods alphabetically (lua_tagged first, then others)
      const sorted = (cls.methods || [])
        .filter(m => m.lua_tagged || cls.set_exposed)
        .sort((a, b) => a.name < b.name ? -1 : a.name > b.name ? 1 : 0)
        .slice(0, 3);
      const methodsHtml = sorted.length
        ? `<div class="hp-methods">
             <div class="hp-methods-label">Methods</div>
             ${sorted.map(m => `<div class="hp-method">${esc(m.name)}(…)</div>`).join('')}
             ${methodCount > 3 ? `<div style="font-size:10px;color:var(--text-dim)">…and ${methodCount - 3} more</div>` : ''}
           </div>`
        : '';

      const parent = cls.extends ? cls.extends.split('.').pop() : null;
      const extendsHtml = parent
        ? `<div class="hp-stat">Extends <span>${esc(parent)}</span></div>`
        : '';

      card.innerHTML = `
        <div class="hp-name">${esc(cls.simple_name || fqn.split('.').pop())}</div>
        <div class="hp-fqn">${esc(fqn)}</div>
        ${badges ? `<div class="hp-badges">${badges}</div>` : ''}
        <div class="hp-stat">Methods: <span>${methodCount}</span> &nbsp; Fields: <span>${fieldCount}</span></div>
        ${extendsHtml}
        ${methodsHtml}
        <div class="hp-hint">Click to open · Ctrl+click or middle-click for new tab</div>
      `;

      // Position near the anchor element, clamped to viewport
      const rect = anchorEl.getBoundingClientRect();
      const vw   = window.innerWidth;
      const vh   = window.innerHeight;
      // Preferred: just below the link
      let top  = rect.bottom + 6;
      let left = rect.left;

      card.style.visibility = 'hidden';
      card.style.display    = 'block';
      const cw = card.offsetWidth;
      const ch = card.offsetHeight;
      card.style.display    = '';
      card.style.visibility = '';

      // Clamp right edge
      if (left + cw > vw - 8) left = vw - cw - 8;
      if (left < 8) left = 8;
      // If not enough space below, flip above
      if (top + ch > vh - 8) top = rect.top - ch - 6;
      if (top < 8) top = 8;

      card.style.left = `${left}px`;
      card.style.top  = `${top}px`;
      card.classList.add('visible');
    }

    function hideCard() {
      clearTimeout(timer);
      timer   = null;
      lastFqn = null;
      card.classList.remove('visible');
    }

    document.addEventListener('mouseover', e => {
      const el = e.target.closest('[data-fqn]');
      if (!el) return;
      const fqn = el.dataset.fqn;
      if (!fqn || fqn === lastFqn) return;
      clearTimeout(timer);
      timer = setTimeout(() => showCard(fqn, el), 400);
    });

    document.addEventListener('mouseout', e => {
      const el = e.target.closest('[data-fqn]');
      if (!el) return;
      // Only hide if we're actually leaving the element (not moving to a child)
      if (!el.contains(e.relatedTarget)) {
        clearTimeout(timer);
        timer = null;
        // Small grace period so the card doesn't flicker when moving to a child text node
        if (card.classList.contains('visible')) {
          timer = setTimeout(hideCard, 80);
        }
      }
    });

    // Clicking anything hides the card
    document.addEventListener('click', hideCard);
    // Scrolling hides the card
    document.addEventListener('scroll', hideCard, true);
  })();

  // ── Hover prefetch (TASK-026) ─────────────────────────────────────
  let prefetchTimer = null;

  // Install hover handlers on all class links — start fetching source files
  document.addEventListener('mouseover', e => {
    const el = e.target.closest('[data-fqn]');
    if (!el || !el.dataset.fqn) return;

    const fqn = el.dataset.fqn;
    clearTimeout(prefetchTimer);
    prefetchTimer = setTimeout(() => {
      if (API && API.classes[fqn] && API.classes[fqn].source_file) {
        fetchSource(API.classes[fqn].source_file).then(text => {
          sourceCache[API.classes[fqn].source_file] = text;
        }).catch(() => {
          // Silently fail — will try pre-shipped or local sources later
        });
      }
    }, 200);
  });

  // Cancel prefetch on mouse leave
  document.addEventListener('mouseout', e => {
    clearTimeout(prefetchTimer);
    prefetchTimer = null;
  });

  // ── Hover prefetch (TASK-026) ─────────────────────────────────────

  // Delegated click for source class refs and method links in source panels
  document.getElementById('content').addEventListener('click', e => {
    const a = e.target.closest('a.src-class-ref');
    if (a) {
      // Middle-click or Ctrl+click → open in new tab
      if (e.button === 1 || e.ctrlKey) {
        if (a.dataset.fqn) { openNewTab(a.dataset.fqn); e.preventDefault(); return; }
      }
      e.preventDefault();
      if (a.dataset.sourcePath) {
        showSourceByPath(a.dataset.sourcePath);
      } else if (a.dataset.fqn) {
        switchTab('classes'); selectClass(a.dataset.fqn);
      }
      return;
    }
    // Method links inside source panels — same action as inherit-method-link in detail
    // panel, but scoped to avoid double-handling with the detail-panel listener.
    const ma = e.target.closest('a.inherit-method-link[data-fqn]');
    if (ma && !ma.closest('#detail-panel')) {
      // Middle-click or Ctrl+click → open in new tab
      if (e.button === 1 || e.ctrlKey) {
        if (ma.dataset.fqn) { openNewTab(ma.dataset.fqn); e.preventDefault(); return; }
      }
      e.preventDefault();
      selectClass(ma.dataset.fqn, null, ma.dataset.method);
    }
  });

  // Delegated click for detail panel: method links, inherit links, group folding
  document.getElementById('detail-panel').addEventListener('click', e => {
    // Method / constructor source links
    const a = e.target.closest('a.method-link[data-method]');
    if (a) { e.preventDefault(); showSource(API.classes[currentClass], a.dataset.method); return; }

    // Inheritance header — class links
    const inheritLink = e.target.closest('a.inherit-link[data-fqn]');
    if (inheritLink) {
      // Middle-click or Ctrl+click → open in new tab
      if (e.button === 1 || e.ctrlKey) {
        if (inheritLink.dataset.fqn) { openNewTab(inheritLink.dataset.fqn); e.preventDefault(); return; }
      }
      e.preventDefault();
      selectClass(inheritLink.dataset.fqn);
      return;
    }

    // Inherited method links — navigate to ancestor class and scroll to method in source
    const inheritMethod = e.target.closest('a.inherit-method-link[data-fqn]');
    if (inheritMethod) {
      // Middle-click or Ctrl+click → open in new tab
      if (e.button === 1 || e.ctrlKey) {
        if (inheritMethod.dataset.fqn) { openNewTab(inheritMethod.dataset.fqn); e.preventDefault(); return; }
      }
      e.preventDefault();
      const targetFqn = inheritMethod.dataset.fqn;
      const method    = inheritMethod.dataset.method;
      selectClass(targetFqn);
      showSource(API.classes[targetFqn], method);
      return;
    }

    // "…and N more" subclasses toggle
    const moreToggle = e.target.closest('.inherit-more-toggle');
    if (moreToggle) {
      const moreEl = moreToggle.nextElementSibling;
      if (moreEl?.classList.contains('inherit-more')) {
        // Use toggle text to determine state — avoids the CSS vs inline-style mismatch
        // where style.display==='' even when CSS hides the element via display:none.
        const hidden = moreToggle.textContent.startsWith('…');
        moreEl.style.display = hidden ? '' : 'none';
        moreToggle.textContent = hidden
          ? 'show less'
          : `…and ${moreToggle.dataset.count} more`;
      }
      return;
    }

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

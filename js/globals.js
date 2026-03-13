'use strict';

function loadGlobalGroupFolds() {
  try {
    const saved = localStorage.getItem('pzGlobalFolds');
    if (saved) JSON.parse(saved).forEach(g => foldedGlobalGroups.add(g));
  } catch {}
}

function saveGlobalGroupFolds() {
  localStorage.setItem('pzGlobalFolds', JSON.stringify([...foldedGlobalGroups]));
}

function showGlobalsPanel(show) {
  document.getElementById('globals-panel').classList.toggle('visible', show);
  document.getElementById('content-tabs').classList.toggle('visible', !show && currentClass !== null);
  document.getElementById('panels').style.display = show ? 'none' : '';
  document.getElementById('detail-panel').classList.toggle('visible', !show && currentCtab === 'detail' && currentClass !== null);
  document.getElementById('source-panel').classList.toggle('visible', !show && currentCtab === 'source' && currentClass !== null);
  document.getElementById('tab-bar').classList.toggle('visible', !show && tabs.length > 0);
}

function initGlobals() {
  showGlobalsPanel(true);
  backToGlobalsTable();
  document.getElementById('placeholder').style.display = 'none';
  foldedGlobalGroups.clear();
  loadGlobalGroupFolds();
  updateGlobalsTable('');
  const inp   = document.getElementById('globals-search');
  const fresh = inp.cloneNode(true);
  inp.parentNode.replaceChild(fresh, inp);
  fresh.value = '';
  fresh.addEventListener('input', () => updateGlobalsTable(fresh.value));
  fresh.focus();
}

function updateGlobalsTable(filter) {
  const s = filter.toLowerCase();

  // Source-order maps so the natural LuaManager.java ordering is preserved.
  const domainOrder = {}, sectionOrder = {}, groupOrder = {};
  API.global_functions.forEach((g, i) => {
    const dom = g.domain  || 'Other';
    const sec = dom + '/' + (g.section || 'Other');
    const grp = g.group   || 'Other';
    if (!(dom in domainOrder))  domainOrder[dom]  = i;
    if (!(sec in sectionOrder)) sectionOrder[sec] = i;
    if (!(grp in groupOrder))   groupOrder[grp]   = i;
  });

  const fns = API.global_functions
    .map((g, i) => ({g, i}))
    .filter(({g}) => !s || g.lua_name.toLowerCase().includes(s) || g.java_method.toLowerCase().includes(s))
    .sort((a, b) => {
      const da = a.g.domain  || 'Other', db = b.g.domain  || 'Other';
      const sa = da + '/' + (a.g.section || 'Other'), sb = db + '/' + (b.g.section || 'Other');
      const ga = a.g.group   || 'Other', gb = b.g.group   || 'Other';
      return (domainOrder[da]  ?? a.i) - (domainOrder[db]  ?? b.i)
          || (sectionOrder[sa] ?? a.i) - (sectionOrder[sb] ?? b.i)
          || (groupOrder[ga]   ?? a.i) - (groupOrder[gb]   ?? b.i)
          || a.g.lua_name.localeCompare(b.g.lua_name);
    });

  document.getElementById('globals-count').textContent = `${fns.length} functions`;

  let lastDomain = null, lastSection = null, lastGroup = null;
  let rows = '';
  for (const {g} of fns) {
    const domain  = g.domain  || 'Other';
    const section = g.section || 'Other';
    const group   = g.group   || 'Other';
    const domKey  = 'DOM:' + domain;
    const secKey  = 'SEC:' + domain + '/' + section;
    const grpKey  = 'GRP:' + group;
    const domFolded = foldedGlobalGroups.has(domKey);
    const secFolded = foldedGlobalGroups.has(secKey);
    const grpFolded = foldedGlobalGroups.has(grpKey);

    if (domain !== lastDomain) {
      rows += `<tr class="globals-dom-header" data-domkey="${esc(domKey)}">
        <td colspan="3"><span class="ggh-arrow">${domFolded ? '▶' : '▼'}</span>
        <span class="ggh-name">${esc(domain)}</span></td></tr>`;
      lastDomain = domain; lastSection = null; lastGroup = null;
    }
    if (section !== lastSection) {
      rows += `<tr class="globals-sec-header" data-domkey="${esc(domKey)}" data-seckey="${esc(secKey)}"${domFolded ? ' style="display:none"' : ''}>
        <td colspan="3" style="padding-left:10px"><span class="ggh-arrow">${secFolded ? '▶' : '▼'}</span>
        <span class="ggh-name" style="font-weight:600">${esc(section)}</span></td></tr>`;
      lastSection = section; lastGroup = null;
    }
    if (group !== lastGroup) {
      const grpHidden = domFolded || secFolded;
      rows += `<tr class="globals-grp-header" data-domkey="${esc(domKey)}" data-seckey="${esc(secKey)}" data-grpkey="${esc(grpKey)}"${grpHidden ? ' style="display:none"' : ''}>
        <td colspan="3" style="padding-left:20px"><span class="ggh-arrow">${grpFolded ? '▶' : '▼'}</span>
        <span class="ggh-name" style="font-weight:normal;color:var(--accent)">${esc(group)}</span></td></tr>`;
      lastGroup = group;
    }
    const fnHidden = domFolded || secFolded || grpFolded;
    const alias = g.java_method !== g.lua_name
      ? `<span style="color:var(--text-dim);font-size:11px;margin-left:8px">← ${esc(g.java_method)}</span>`
      : '';
    rows += `<tr class="gfn-row" data-domkey="${esc(domKey)}" data-seckey="${esc(secKey)}" data-grpkey="${esc(grpKey)}"${fnHidden ? ' style="display:none"' : ''}>
      <td style="padding-left:30px"><a class="gfn-link" data-method="${esc(g.java_method)}">${esc(g.lua_name)}</a>${alias}</td>
      <td><span class="return-type">${esc(g.return_type || '?')}</span></td>
      <td><span class="params-cell">${renderParams(g.params) || '<span style="color:#444">—</span>'}</span></td>
    </tr>`;
  }

  document.getElementById('globals-table-wrap').innerHTML =
    `<table style="margin-top:4px"><thead><tr><th>Lua name</th><th>Returns</th><th>Parameters</th></tr></thead><tbody>${rows}</tbody></table>`;

  const wrap = document.getElementById('globals-table-wrap');

  // Domain-level fold/unfold
  wrap.querySelectorAll('.globals-dom-header').forEach(hdr => {
    hdr.addEventListener('click', () => {
      const domKey = hdr.dataset.domkey;
      if (foldedGlobalGroups.has(domKey)) foldedGlobalGroups.delete(domKey);
      else foldedGlobalGroups.add(domKey);
      saveGlobalGroupFolds();
      const folded = foldedGlobalGroups.has(domKey);
      hdr.querySelector('.ggh-arrow').textContent = folded ? '▶' : '▼';
      wrap.querySelectorAll(`[data-domkey="${domKey}"]`).forEach(r => {
        if (r === hdr) return;
        if (folded) {
          r.style.display = 'none';
        } else if (r.classList.contains('globals-sec-header')) {
          r.style.display = '';
          r.querySelector('.ggh-arrow').textContent = foldedGlobalGroups.has(r.dataset.seckey) ? '▶' : '▼';
        } else if (r.classList.contains('globals-grp-header')) {
          r.style.display = foldedGlobalGroups.has(r.dataset.seckey) ? 'none' : '';
          if (r.style.display === '')
            r.querySelector('.ggh-arrow').textContent = foldedGlobalGroups.has(r.dataset.grpkey) ? '▶' : '▼';
        } else {
          r.style.display = (foldedGlobalGroups.has(r.dataset.seckey) || foldedGlobalGroups.has(r.dataset.grpkey)) ? 'none' : '';
        }
      });
    });
  });

  // Section-level fold/unfold
  wrap.querySelectorAll('.globals-sec-header').forEach(hdr => {
    hdr.addEventListener('click', () => {
      const secKey = hdr.dataset.seckey;
      if (foldedGlobalGroups.has(secKey)) foldedGlobalGroups.delete(secKey);
      else foldedGlobalGroups.add(secKey);
      saveGlobalGroupFolds();
      const folded = foldedGlobalGroups.has(secKey);
      hdr.querySelector('.ggh-arrow').textContent = folded ? '▶' : '▼';
      wrap.querySelectorAll(`[data-seckey="${secKey}"]`).forEach(r => {
        if (r === hdr) return;
        if (folded) {
          r.style.display = 'none';
        } else if (r.classList.contains('globals-grp-header')) {
          r.style.display = '';
          r.querySelector('.ggh-arrow').textContent = foldedGlobalGroups.has(r.dataset.grpkey) ? '▶' : '▼';
        } else {
          r.style.display = foldedGlobalGroups.has(r.dataset.grpkey) ? 'none' : '';
        }
      });
    });
  });

  // Group-level fold/unfold
  wrap.querySelectorAll('.globals-grp-header').forEach(hdr => {
    hdr.addEventListener('click', () => {
      const grpKey = hdr.dataset.grpkey;
      if (foldedGlobalGroups.has(grpKey)) foldedGlobalGroups.delete(grpKey);
      else foldedGlobalGroups.add(grpKey);
      saveGlobalGroupFolds();
      const folded = foldedGlobalGroups.has(grpKey);
      hdr.querySelector('.ggh-arrow').textContent = folded ? '▶' : '▼';
      wrap.querySelectorAll(`.gfn-row[data-grpkey="${grpKey}"]`).forEach(r => r.style.display = folded ? 'none' : '');
    });
  });

  wrap.querySelectorAll('.gfn-link').forEach(a => {
    a.addEventListener('click', e => {
      e.preventDefault();
      showGlobalSource(a.dataset.method);
    });
  });
}

async function showGlobalSource(javaMethod) {
  navPush({type: 'globalSource', javaMethod});
  const relPath = 'zombie/Lua/LuaManager.java';
  document.getElementById('globals-src-title').textContent = javaMethod;
  document.getElementById('globals-source-wrap').classList.add('has-source');
  document.getElementById('gsrc-toolbar').style.display = '';

  const codeEl = document.getElementById('globals-src-code');
  codeEl.textContent = 'Loading…';

  let text;
  try { text = await fetchSource(relPath); }
  catch (e) {
    codeEl.textContent = `// Source not available.\n// Error: ${e.message}`;
    hljs.highlightElement(codeEl);
    return;
  }

  renderFoldableSource(text, codeEl);
  scrollToMethod(text, javaMethod,
    document.getElementById('globals-src-pre'),
    document.getElementById('globals-src-code'));
}

function backToGlobalsTable() {
  document.getElementById('globals-source-wrap').classList.remove('has-source');
  document.getElementById('gsrc-toolbar').style.display = 'none';
  document.getElementById('globals-src-title').textContent = '';
  document.getElementById('globals-src-code').textContent = '';
}

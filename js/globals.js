'use strict';

function showGlobalsPanel(show) {
  document.getElementById('globals-panel').classList.toggle('visible', show);
  document.getElementById('content-tabs').classList.toggle('visible', !show && currentClass !== null);
  document.getElementById('panels').style.display = show ? 'none' : '';
  document.getElementById('detail-panel').classList.toggle('visible', !show && currentCtab === 'detail' && currentClass !== null);
  document.getElementById('source-panel').classList.toggle('visible', !show && currentCtab === 'source' && currentClass !== null);
}

function initGlobals() {
  showGlobalsPanel(true);
  backToGlobalsTable();
  document.getElementById('placeholder').style.display = 'none';
  updateGlobalsTable('');
  const inp   = document.getElementById('globals-search');
  const fresh = inp.cloneNode(true);
  inp.parentNode.replaceChild(fresh, inp);
  fresh.value = '';
  fresh.addEventListener('input', () => updateGlobalsTable(fresh.value));
  fresh.focus();
  document.getElementById('btn-luamgr-src').onclick = () =>
    showSourceByPath('zombie/Lua/LuaManager.java');
}

function updateGlobalsTable(filter) {
  const s   = filter.toLowerCase();
  const fns = API.global_functions
    .map((g, i) => ({g, i}))
    .filter(({g}) => !s || g.lua_name.toLowerCase().includes(s) || g.java_method.toLowerCase().includes(s))
    .sort((a, b) => {
      const ca = a.g.category || 'Other', cb = b.g.category || 'Other';
      const ga = a.g.group    || '',      gb = b.g.group    || '';
      return ca.localeCompare(cb) || ga.localeCompare(gb) || a.g.lua_name.localeCompare(b.g.lua_name);
    });

  document.getElementById('globals-count').textContent = `${fns.length} functions`;

  let lastGroup = null, lastSubgroup = null;
  let rows = '';
  for (const {g} of fns) {
    const group    = g.category || 'Other';
    const subgroup = g.group    || group;
    const catKey   = 'CAT:' + group;
    const subKey   = 'SUB:' + group + '/' + subgroup; // category-scoped to avoid cross-category collisions
    const catFolded = foldedGlobalGroups.has(catKey);
    const subFolded = foldedGlobalGroups.has(subKey);

    if (group !== lastGroup) {
      rows += `<tr class="globals-cat-header" data-catkey="${esc(catKey)}">
        <td colspan="3"><span class="ggh-arrow">${catFolded ? '▶' : '▼'}</span>
        <span class="ggh-name">${esc(group)}</span></td></tr>`;
      lastGroup = group;
      lastSubgroup = null; // force subgroup header re-emit
    }
    if (subgroup !== lastSubgroup) {
      const hidden = catFolded;
      rows += `<tr class="globals-sub-header" data-catkey="${esc(catKey)}" data-subkey="${esc(subKey)}"${hidden ? ' style="display:none"' : ''}>
        <td colspan="3" style="padding-left:18px"><span class="ggh-arrow">${subFolded ? '▶' : '▼'}</span>
        <span class="ggh-name" style="font-weight:normal;color:var(--text-dim)">${esc(subgroup)}</span></td></tr>`;
      lastSubgroup = subgroup;
    }
    const hidden = catFolded || subFolded;
    const alias  = g.java_method !== g.lua_name
      ? `<span style="color:var(--text-dim);font-size:11px;margin-left:8px">← ${esc(g.java_method)}</span>`
      : '';
    rows += `<tr class="gfn-row" data-catkey="${esc(catKey)}" data-subkey="${esc(subKey)}"${hidden ? ' style="display:none"' : ''}>
      <td style="padding-left:28px"><a class="gfn-link" data-method="${esc(g.java_method)}">${esc(g.lua_name)}</a>${alias}</td>
      <td><span class="return-type">${esc(g.return_type || '?')}</span></td>
      <td><span class="params-cell">${renderParams(g.params) || '<span style="color:#444">—</span>'}</span></td>
    </tr>`;
  }

  document.getElementById('globals-table-wrap').innerHTML =
    `<table style="margin-top:4px"><thead><tr><th>Lua name</th><th>Returns</th><th>Parameters</th></tr></thead><tbody>${rows}</tbody></table>`;

  const wrap = document.getElementById('globals-table-wrap');

  wrap.querySelectorAll('.globals-cat-header').forEach(hdr => {
    hdr.addEventListener('click', () => {
      const catKey = hdr.dataset.catkey;
      if (foldedGlobalGroups.has(catKey)) foldedGlobalGroups.delete(catKey);
      else foldedGlobalGroups.add(catKey);
      const folded = foldedGlobalGroups.has(catKey);
      hdr.querySelector('.ggh-arrow').textContent = folded ? '▶' : '▼';
      // Hide/show all rows and sub-headers in this category
      wrap.querySelectorAll(`[data-catkey="${catKey}"]`).forEach(r => {
        if (r === hdr) return;
        if (folded) {
          r.style.display = 'none';
        } else {
          // Sub-headers always visible when category is open
          if (r.classList.contains('globals-sub-header')) {
            r.style.display = '';
            const arrow = r.querySelector('.ggh-arrow');
            if (arrow) arrow.textContent = foldedGlobalGroups.has(r.dataset.subkey) ? '▶' : '▼';
          } else {
            // Function rows: only show if their sub-group is not folded
            r.style.display = foldedGlobalGroups.has(r.dataset.subkey) ? 'none' : '';
          }
        }
      });
    });
  });

  wrap.querySelectorAll('.globals-sub-header').forEach(hdr => {
    hdr.addEventListener('click', () => {
      const subKey = hdr.dataset.subkey;
      if (foldedGlobalGroups.has(subKey)) foldedGlobalGroups.delete(subKey);
      else foldedGlobalGroups.add(subKey);
      const folded = foldedGlobalGroups.has(subKey);
      hdr.querySelector('.ggh-arrow').textContent = folded ? '▶' : '▼';
      wrap.querySelectorAll(`.gfn-row[data-subkey="${subKey}"]`).forEach(r => r.style.display = folded ? 'none' : '');
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
  document.getElementById('globals-header').style.display     = 'none';
  document.getElementById('globals-nav').classList.add('visible');
  document.getElementById('globals-src-title').textContent    = javaMethod;
  document.getElementById('globals-table-wrap').style.display = 'none';
  document.getElementById('globals-source-wrap').classList.add('visible');

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
  document.getElementById('globals-header').style.display     = '';
  document.getElementById('globals-nav').classList.remove('visible');
  document.getElementById('globals-table-wrap').style.display = '';
  document.getElementById('globals-source-wrap').classList.remove('visible');
}

'use strict';

function showGlobalsPanel(show) {
  document.getElementById('globals-panel').classList.toggle('visible', show);
  document.getElementById('content-tabs').classList.toggle('visible', !show && currentClass !== null);
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
}

function updateGlobalsTable(filter) {
  const s   = filter.toLowerCase();
  const fns = API.global_functions
    .map((g, i) => ({g, i}))
    .filter(({g}) => !s || g.lua_name.toLowerCase().includes(s) || g.java_method.toLowerCase().includes(s));

  document.getElementById('globals-count').textContent = `${fns.length} functions`;

  let lastGroup = null;
  let rows = '';
  for (const {g} of fns) {
    const group = g.group || '';
    if (group !== lastGroup) {
      const folded = foldedGlobalGroups.has(group);
      rows += `<tr><td colspan="4" class="globals-group-header" data-group="${esc(group)}">
        <span class="ggh-arrow">${folded ? '▶' : '▼'}</span>
        <span class="ggh-name">${esc(group)}</span>
      </td></tr>`;
      lastGroup = group;
    }
    const folded = foldedGlobalGroups.has(group);
    const alias  = g.java_method !== g.lua_name
      ? `<span style="color:var(--text-dim);font-size:11px;margin-left:8px">← ${esc(g.java_method)}</span>`
      : '';
    rows += `<tr data-group="${esc(group)}"${folded ? ' style="display:none"' : ''}>
      <td><a class="gfn-link" data-method="${esc(g.java_method)}">${esc(g.lua_name)}</a>${alias}</td>
      <td><span class="return-type">${esc(g.return_type || '?')}</span></td>
      <td><span class="params-cell">${renderParams(g.params) || '<span style="color:#444">—</span>'}</span></td>
    </tr>`;
  }

  document.getElementById('globals-table-wrap').innerHTML =
    `<table style="margin-top:4px"><thead><tr><th>Lua name</th><th>Returns</th><th>Parameters</th></tr></thead><tbody>${rows}</tbody></table>`;

  document.querySelectorAll('#globals-table-wrap .globals-group-header').forEach(hdr => {
    hdr.addEventListener('click', () => {
      const group = hdr.dataset.group;
      if (foldedGlobalGroups.has(group)) foldedGlobalGroups.delete(group);
      else foldedGlobalGroups.add(group);
      const folded = foldedGlobalGroups.has(group);
      hdr.querySelector('.ggh-arrow').textContent = folded ? '▶' : '▼';
      const wrap = document.getElementById('globals-table-wrap');
      wrap.querySelectorAll(`tr[data-group="${group}"]`).forEach(r => r.style.display = folded ? 'none' : '');
    });
  });

  document.querySelectorAll('#globals-table-wrap .gfn-link').forEach(a => {
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

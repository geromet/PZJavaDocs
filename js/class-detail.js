'use strict';

function renderClassDetail(fqn) {
  const cls    = API.classes[fqn];
  const simple = fqn.split('.').pop();
  const panel  = document.getElementById('detail-panel');

  panel.innerHTML = `
    <div class="class-header">
      <h2>${esc(simple)}</h2>
      <div class="fqn">${esc(fqn)}</div>
      <div class="badges">
        <span class="badge ${cls.set_exposed ? 'badge-exposed' : 'badge-no'}">${cls.set_exposed ? 'setExposed' : 'not setExposed'}</span>
        <span class="badge ${cls.lua_tagged  ? 'badge-tagged'  : 'badge-no'}">${cls.lua_tagged  ? '@UsedFromLua' : 'not @UsedFromLua'}</span>
        ${cls.is_enum ? '<span class="badge badge-enum">enum</span>' : ''}
      </div>
      <div class="source-path">${esc(cls.source_file)}</div>
    </div>
    ${renderInheritHeader(cls, fqn)}
    ${(cls.constructors || []).length ? `
    <div class="section">
      <div class="section-header">
        <h3>Constructors</h3><span class="count">${(cls.constructors || []).length}</span>
      </div>
      <div id="constructors-wrap">${renderConstructorsTable(cls)}</div>
    </div>` : ''}
    <div class="section">
      <div class="section-header">
        <h3>Methods</h3><span class="count" id="method-count">${cls.methods.length}</span>
        <span id="noncallable-btn-wrap"></span>
        ${cls.methods.length ? `<input class="inline-search" id="method-search-inp" type="text" placeholder="Filter…" value="${esc(methodSearch)}" autocomplete="off">` : ''}
      </div>
      <div id="methods-wrap"></div>
      <div id="inherit-wrap"></div>
    </div>
    <div class="section">
      <div class="section-header">
        <h3>${cls.is_enum ? 'Constants' : 'Fields'}</h3><span class="count" id="field-count">${cls.fields.length}</span>
        ${cls.fields.length && !cls.is_enum ? `<input class="inline-search" id="field-search-inp" type="text" placeholder="Filter…" value="${esc(fieldSearch)}" autocomplete="off">` : ''}
      </div>
      <div id="fields-wrap"></div>
    </div>`;

  switchCtab('detail');
  refreshMethods(cls, fqn);
  refreshFields(cls);

  const mi = document.getElementById('method-search-inp');
  if (mi) { mi.addEventListener('input', () => { methodSearch = mi.value; refreshMethods(cls, fqn); }); if (methodSearch) mi.focus(); }
  const fi = document.getElementById('field-search-inp');
  if (fi) { fi.addEventListener('input', () => { fieldSearch  = fi.value; refreshFields(cls);  }); if (fieldSearch && !methodSearch) fi.focus(); }
}

function refreshMethods(cls, fqn) {
  const s      = methodSearch.toLowerCase();
  const tagged = cls.methods.filter(m =>  m.lua_tagged && (!s || m.name.toLowerCase().includes(s)));
  const other  = cls.methods.filter(m => !m.lua_tagged && (!s || m.name.toLowerCase().includes(s)));

  // Non-callable = static other methods + instance other methods when not setExposed
  const nonCallableCount = other.filter(m => m.static || !cls.set_exposed).length;

  document.getElementById('method-count').textContent = tagged.length + other.length;

  const wrap = document.getElementById('methods-wrap');
  wrap.innerHTML = renderMethodGroups(tagged, other, cls.set_exposed);
  wrap.classList.toggle('hide-noncallable', !showNonCallable);

  const inheritWrap = document.getElementById('inherit-wrap');
  if (inheritWrap && fqn) inheritWrap.innerHTML = renderInheritedMethods(cls, fqn, s);

  // Update non-callable toggle button
  const btnWrap = document.getElementById('noncallable-btn-wrap');
  if (btnWrap) {
    if (nonCallableCount > 0) {
      const btn = document.createElement('button');
      btn.id = 'btn-noncallable-toggle';
      btn.className = 'noncallable-toggle';
      btn.textContent = `${showNonCallable ? 'Hide' : 'Show'} non-callable (${nonCallableCount})`;
      btn.addEventListener('click', () => {
        showNonCallable = !showNonCallable;
        wrap.classList.toggle('hide-noncallable', !showNonCallable);
        btn.textContent = `${showNonCallable ? 'Hide' : 'Show'} non-callable (${nonCallableCount})`;
      });
      btnWrap.innerHTML = '';
      btnWrap.appendChild(btn);
    } else {
      btnWrap.innerHTML = '';
    }
  }
}

function refreshFields(cls) {
  const s      = fieldSearch.toLowerCase();
  const fields = cls.fields.filter(f => !s || f.name.toLowerCase().includes(s) || f.type.toLowerCase().includes(s));
  document.getElementById('field-count').textContent = fields.length;
  document.getElementById('fields-wrap').innerHTML   = renderFieldsTable(fields, cls.is_enum);
}

function renderConstructorsTable(cls) {
  const ctors = cls.constructors || [];
  if (!ctors.length) return '<div class="empty-msg">No public constructors</div>';
  const simple = cls.simple_name;
  const rows = ctors.map(c => {
    const dot = c.lua_tagged
      ? `<span class="dot dot-tagged" style="display:inline-block;margin-left:5px;vertical-align:middle" title="@UsedFromLua"></span>`
      : '';
    return `<tr>
      <td><a class="method-link" data-method="${esc(simple)}" data-is-ctor="1">${esc(simple)}</a>${dot}</td>
      <td><span class="params-cell">(${renderParams(c.params)})</span></td>
    </tr>`;
  }).join('');
  return `<table><thead><tr><th>Constructor</th><th>Parameters</th></tr></thead><tbody>${rows}</tbody></table>`;
}

function renderMethodGroups(tagged, other, setExposed) {
  if (!tagged.length && !other.length) return `<div class="empty-msg">No methods</div>`;
  const taggedInst   = tagged.filter(m => !m.static);
  const taggedStatic = tagged.filter(m =>  m.static);
  const otherInst    = other.filter(m => !m.static);
  const otherStatic  = other.filter(m =>  m.static);

  // callable=true → visible by default; callable=false → hidden by .hide-noncallable
  const mkGroup = (labelClass, content, tableHtml, callable) =>
    `<div class="method-group${callable ? '' : ' non-callable-group'}">
      <div class="group-label ${labelClass}"><span class="group-arrow">▼</span>${content}</div>
      <div class="group-body">${tableHtml}</div>
    </div>`;

  let html = '';
  if (taggedInst.length)
    html += mkGroup('tagged-label', `<span class="dot dot-tagged"></span> @UsedFromLua (${taggedInst.length})`, renderMethodsTable(taggedInst), true);
  if (taggedStatic.length)
    html += mkGroup('tagged-label', `<span class="dot dot-tagged"></span> @UsedFromLua — static (${taggedStatic.length})`, renderMethodsTable(taggedStatic), true);
  if (otherInst.length) {
    const lbl = setExposed ? `setExposed — callable (${otherInst.length})` : `Not setExposed (${otherInst.length})`;
    html += mkGroup('other-label', `<span class="dot dot-empty"></span> ${lbl}`, renderMethodsTable(otherInst), setExposed);
  }
  if (otherStatic.length) {
    const lbl = setExposed ? `setExposed — static, not Lua-callable (${otherStatic.length})` : `Not setExposed — static (${otherStatic.length})`;
    html += mkGroup('other-label', `<span class="dot dot-empty"></span> ${lbl}`, renderMethodsTable(otherStatic), false);
  }
  return html;
}

function renderMethodsTable(methods) {
  if (!methods.length) return '';
  const rows = methods.map(m => {
    const staticTag = m.static ? `<span class="tag-static" style="margin-left:5px">static</span>` : '';
    return `<tr>
      <td><a class="method-link" data-method="${esc(m.name)}">${esc(m.name)}</a>${staticTag}</td>
      <td><span class="return-type">${esc(m.return_type)}</span></td>
      <td><span class="params-cell">${renderParams(m.params) || '<span style="color:#444">—</span>'}</span></td>
    </tr>`;
  }).join('');
  return `<table><thead><tr><th>Method</th><th>Returns</th><th>Parameters</th></tr></thead><tbody>${rows}</tbody></table>`;
}

// ── Interface helpers ─────────────────────────────────────────────────────

function ifaceLink(fqn) {
  const simple = fqn.split('.').pop();
  if (API.classes[fqn])
    return `<a class="inherit-link" data-fqn="${esc(fqn)}">${esc(simple)}</a>`;
  const srcPath = API._source_index?.[simple];
  if (srcPath)
    return `<a class="src-class-ref" data-source-path="${esc(srcPath)}" title="${esc(fqn)}">${esc(simple)}</a>`;
  return `<span class="inherit-tree-item-ext" title="${esc(fqn)}">${esc(simple)}</span>`;
}

// Returns [{directFqn, extensions:[fqn,...]}] for all interface-extends reachable
// from a single directly-implemented interface. Updates `seen` in place.
function collectIfaceExtensions(rootFqn, seen) {
  const viaRows = [];  // {label, items:[fqn,...]}
  const queue   = [[rootFqn, rootFqn]]; // [current, root]
  while (queue.length) {
    const [cur, root] = queue.shift();
    const parents = API._interface_extends?.[cur] || [];
    const newItems = parents.filter(p => !seen.has(p));
    newItems.forEach(p => seen.add(p));
    if (newItems.length) {
      const rootSimple = root.split('.').pop();
      // Merge into existing row for the same root, or add new
      const existing = viaRows.find(r => r.root === rootFqn);
      if (existing) existing.items.push(...newItems);
      else viaRows.push({root: rootFqn, label: rootSimple, items: newItems});
      newItems.forEach(p => queue.push([p, rootFqn]));
    }
  }
  return viaRows;
}

// Builds groups [{fromFqn, isDirect, directItems, viaRows}]
// walking the full extends chain and deduplicating across groups.
function buildImplGroups(cls, fqn) {
  const seen   = new Set();
  const groups = [];
  let curFqn   = fqn;
  let curCls   = cls;
  let isDirect = true;
  const clsVisited = new Set();

  while (curFqn && !clsVisited.has(curFqn)) {
    clsVisited.add(curFqn);
    const rawImpls = (curCls?.implements) || [];
    const direct   = rawImpls.filter(i => !seen.has(i));
    direct.forEach(i => seen.add(i));

    if (direct.length) {
      const viaRows = [];
      for (const ifFqn of direct) {
        const rows = collectIfaceExtensions(ifFqn, seen);
        viaRows.push(...rows);
      }
      groups.push({fromFqn: curFqn, isDirect, directItems: direct, viaRows});
    }

    curFqn  = curCls?.extends || API._extends_map?.[curFqn];
    curCls  = API.classes[curFqn];
    isDirect = false;
  }
  return groups;
}

function renderImplGroups(groups) {
  if (!groups.length) return '';
  const total = groups.reduce((n, g) =>
    n + g.directItems.length + g.viaRows.reduce((m, r) => m + r.items.length, 0), 0);

  const rows = groups.map(g => {
    const label = g.isDirect
      ? `direct (${g.directItems.length})`
      : `via ${esc(g.fromFqn.split('.').pop())} (${g.directItems.length})`;
    const directHtml = g.directItems.map(ifaceLink).join(', ');
    const viaHtml    = g.viaRows.map(r =>
      `<div class="impl-via-row"><span class="impl-via-label">via ${esc(r.label)}:</span>${r.items.map(ifaceLink).join(', ')}</div>`
    ).join('');
    return `<div class="impl-group">
      <span class="impl-group-header">${label}:</span><span class="impl-items">${directHtml}</span>${viaHtml}
    </div>`;
  }).join('');

  return `<div class="inherit-meta impl-section">
    <span class="inherit-label">All Implemented Interfaces (${total}):</span>${rows}
  </div>`;
}

// ─────────────────────────────────────────────────────────────────────────────

function renderInheritHeader(cls, fqn) {
  const hasExtends    = !!cls.extends;
  const implGroups    = buildImplGroups(cls, fqn);
  const hasSubclasses = (cls.subclasses || []).length > 0;
  if (!hasExtends && !implGroups.length && !hasSubclasses) return '';

  let html = '<div class="inherit-header">';

  // Inheritance tree: walk chain upward, render top-to-bottom
  if (hasExtends) {
    const chain = [];
    let cur = fqn;
    const visited = new Set();
    while (cur && !visited.has(cur)) {
      chain.unshift(cur);
      visited.add(cur);
      cur = API.classes[cur]?.extends ?? API._extends_map?.[cur];
    }
    if (cur && !visited.has(cur)) chain.unshift(cur); // root not in API

    html += '<div class="inherit-tree">';
    for (let i = 0; i < chain.length; i++) {
      const indent  = '\u00a0\u00a0'.repeat(i);
      const arrow   = i > 0 ? '\u2514\u2500 ' : '';
      const cfqn    = chain[i];
      const csimple = cfqn.split('.').pop();
      if (cfqn === fqn) {
        html += `<div class="inherit-tree-item">${esc(indent + arrow)}<span class="inherit-tree-current">${esc(csimple)}</span></div>`;
      } else if (API.classes[cfqn]) {
        html += `<div class="inherit-tree-item">${esc(indent + arrow)}<a class="inherit-link" data-fqn="${esc(cfqn)}">${esc(cfqn)}</a></div>`;
      } else {
        html += `<div class="inherit-tree-item">${esc(indent + arrow)}<span class="inherit-tree-item-ext">${esc(cfqn)}</span></div>`;
      }
    }
    html += '</div>';
  }

  // Implemented interfaces — grouped by chain position and interface extends
  html += renderImplGroups(implGroups);

  // Direct known subclasses (truncated with toggle)
  if (hasSubclasses) {
    const subs    = cls.subclasses;
    const MAX     = 10;
    const mkLink  = f => {
      if (API.classes[f])
        return `<a class="inherit-link" data-fqn="${esc(f)}">${esc(f.split('.').pop())}</a>`;
      const srcPath = API._source_index?.[f.split('.').pop()];
      if (srcPath)
        return `<a class="src-class-ref" data-source-path="${esc(srcPath)}" title="${esc(f)}">${esc(f.split('.').pop())}</a>`;
      return `<span class="inherit-tree-item-ext" title="${esc(f)}">${esc(f.split('.').pop())}</span>`;
    };
    let subHtml;
    if (subs.length <= MAX) {
      subHtml = subs.map(mkLink).join(', ');
    } else {
      const shown   = subs.slice(0, MAX).map(mkLink).join(', ');
      const rest    = subs.slice(MAX).map(mkLink).join(', ');
      const restCnt = subs.length - MAX;
      subHtml = `${shown}, <span class="inherit-more-toggle" data-count="${restCnt}">…and ${restCnt} more</span>`
               + `<span class="inherit-more">, ${rest}</span>`;
    }
    html += `<div class="inherit-meta"><span class="inherit-label">Direct subclasses:</span>${subHtml}</div>`;
  }

  html += '</div>';
  return html;
}

function renderInheritedMethods(cls, fqn, filterStr) {
  if (!cls.extends) return '';
  const ownNames = new Set(cls.methods.map(m => m.name));
  const s = (filterStr || '').toLowerCase();
  let html = '';
  let ancestorFqn = cls.extends;
  const visited = new Set([fqn]);

  while (ancestorFqn && !visited.has(ancestorFqn)) {
    visited.add(ancestorFqn);
    const anc = API.classes[ancestorFqn];
    if (!anc) {
      // Non-API intermediate: follow chain via _extends_map without rendering
      ancestorFqn = API._extends_map?.[ancestorFqn];
      continue;
    }

    // Only callable: lua_tagged, or (setExposed and not static)
    let inherited = anc.methods.filter(m =>
      !ownNames.has(m.name) &&
      (m.lua_tagged || (!m.static && anc.set_exposed))
    );
    if (s) inherited = inherited.filter(m => m.name.toLowerCase().includes(s));

    if (inherited.length > 0) {
      const rows = inherited.map(m => {
        const staticTag = m.static ? `<span class="tag-static" style="margin-left:5px">static</span>` : '';
        return `<tr>
          <td><a class="inherit-method-link" data-fqn="${esc(ancestorFqn)}" data-method="${esc(m.name)}">${esc(m.name)}</a>${staticTag}</td>
          <td><span class="return-type">${esc(m.return_type)}</span></td>
          <td><span class="params-cell">${renderParams(m.params) || '<span style="color:#444">—</span>'}</span></td>
        </tr>`;
      }).join('');
      const table = `<table><thead><tr><th>Method</th><th>Returns</th><th>Parameters</th></tr></thead><tbody>${rows}</tbody></table>`;
      html += `<div class="method-group inherited">
        <div class="group-label other-label"><span class="group-arrow">▼</span>Methods inherited from <a class="inherit-link" data-fqn="${esc(ancestorFqn)}">${esc(ancestorFqn.split('.').pop())}</a> <span style="color:var(--text-dim);font-size:11px">${esc(ancestorFqn)}</span></div>
        <div class="group-body">${table}</div>
      </div>`;
    }

    ancestorFqn = anc.extends;
  }
  return html;
}

function renderFieldsTable(fields, isEnum) {
  if (!fields.length) return `<div class="empty-msg">No fields</div>`;
  if (isEnum) {
    return `<div style="padding:8px 0;line-height:2.2">${fields.map(f => `<span class="field-name" style="margin-right:12px">${esc(f.name)}</span>`).join('')}</div>`;
  }
  return `<table><thead><tr><th>Field</th><th>Type</th></tr></thead><tbody>${
    fields.map(f => {
      const dot       = f.lua_tagged ? `<span class="dot dot-tagged" style="display:inline-block;margin-left:5px;vertical-align:middle" title="@UsedFromLua"></span>` : '';
      const staticTag = f.static     ? `<span class="tag-static" style="margin-left:5px">static</span>` : '';
      return `<tr><td><span class="field-name">${esc(f.name)}</span>${dot}${staticTag}</td><td><span class="field-type">${esc(f.type)}</span></td></tr>`;
    }).join('')
  }</tbody></table>`;
}

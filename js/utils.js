'use strict';

function esc(s) {
  return String(s || '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

function highlightMatch(text, search) {
  if (!search) return esc(text);
  const idx = text.toLowerCase().indexOf(search.toLowerCase());
  if (idx === -1) return esc(text);
  return esc(text.slice(0, idx))
    + `<mark class="search-mark">${esc(text.slice(idx, idx + search.length))}</mark>`
    + esc(text.slice(idx + search.length));
}

function renderParams(params) {
  return (params || []).map(p =>
    `<span class="param-type">${esc(p.type)}</span> <span class="param-name">${esc(p.name)}</span>`
  ).join(', ');
}

async function getLocalFileHandle(relPath) {
  if (!localDirHandle) return null;
  const parts = relPath.split('/');
  let dir = localDirHandle;
  try {
    for (let i = 0; i < parts.length - 1; i++) dir = await dir.getDirectoryHandle(parts[i]);
    return await dir.getFileHandle(parts[parts.length - 1]);
  } catch { return null; }
}

async function fetchSource(relPath) {
  if (sourceCache[relPath]) return sourceCache[relPath];
  const fh = await getLocalFileHandle(relPath);
  if (fh) { const text = await (await fh.getFile()).text(); sourceCache[relPath] = text; return text; }
  const resp = await fetch('./sources/' + relPath);
  if (!resp.ok) throw new Error(`Source not found: ${relPath}`);
  const ct = resp.headers.get('content-type') || '';
  if (ct.includes('text/html')) throw new Error(`Path resolved to a directory listing — source file unavailable: ${relPath}`);
  const text = await resp.text();
  sourceCache[relPath] = text;
  return text;
}

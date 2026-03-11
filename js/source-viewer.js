'use strict';

function buildFoldRegions(text) {
  const lines = text.split('\n');
  const regions = [];
  const stack = [];
  const MIN_FOLD_LINES = 4;

  for (let i = 0; i < lines.length; i++) {
    const raw = lines[i];
    let inStr = false, inChar = false, inLineC = false, inBlockC = false;
    let opens = 0, closes = 0;

    for (let j = 0; j < raw.length; j++) {
      const c = raw[j];
      if (inLineC) break;
      if (inBlockC) { if (c === '*' && raw[j+1] === '/') { inBlockC = false; j++; } continue; }
      if (inStr)    { if (c === '\\') j++; else if (c === '"') inStr = false; continue; }
      if (inChar)   { if (c === '\\') j++; else if (c === "'") inChar = false; continue; }
      if (c === '/' && raw[j+1] === '/') { inLineC = true; continue; }
      if (c === '/' && raw[j+1] === '*') { inBlockC = true; j++; continue; }
      if (c === '"') { inStr = true; continue; }
      if (c === "'") { inChar = true; continue; }
      if (c === '{') opens++;
      if (c === '}') closes++;
    }
    for (let k = 0; k < opens; k++)  stack.push({startLine: i, type: classifyLine(raw)});
    for (let k = 0; k < closes; k++) {
      if (stack.length > 0) {
        const {startLine, type} = stack.pop();
        if (i - startLine >= MIN_FOLD_LINES) regions.push({startLine, endLine: i, type});
      }
    }
  }
  return regions;
}

function classifyLine(line) {
  const t = line.trim();
  if (/\b(class|interface|enum)\b/.test(t)) return 'class';
  if (/^(if|else|for|while|try|catch|finally|switch|do)\b/.test(t)) return 'block';
  if (t.includes('(') && t.includes(')')) return 'method';
  return 'other';
}

function wrapLines(html) {
  // Track open token spans so we can close/reopen them at each line break,
  // which ensures multi-line comments and strings are highlighted correctly.
  const openSpans = [];
  let result = `<span class="src-line" id="L1">`;
  let n = 1, inTag = false, tagBuf = '';
  for (let i = 0; i < html.length; i++) {
    const c = html[i];
    if (c === '<') { inTag = true; tagBuf = c; }
    else if (inTag) {
      tagBuf += c;
      if (c === '>') {
        inTag = false;
        result += tagBuf;
        if (tagBuf.startsWith('</span')) { openSpans.pop(); }
        else if (tagBuf.startsWith('<span') && !tagBuf.endsWith('/>')) { openSpans.push(tagBuf); }
        tagBuf = '';
      }
    } else if (c === '\n') {
      const closes  = '</span>'.repeat(openSpans.length);
      const reopens = openSpans.join('');
      result += closes + `</span><span class="src-line" id="L${++n}">` + reopens;
    } else { result += c; }
  }
  return result + '</span>';
}

function injectFoldRegions(codeEl, regions) {
  // Build line map from the already-set innerHTML
  const lineMap = new Map();
  codeEl.querySelectorAll('.src-line').forEach(el => {
    const n = parseInt(el.id.slice(1));
    if (!isNaN(n)) lineMap.set(n, el);
  });

  // Process bottom-to-top so DOM insertions don't shift siblings
  const sorted = [...regions].sort((a, b) => b.startLine - a.startLine);

  for (const {startLine, endLine, type} of sorted) {
    const startEl = lineMap.get(startLine + 1);
    const endEl   = lineMap.get(endLine + 1);
    if (!startEl || !endEl || startEl.querySelector('.fold-btn')) continue;

    // Add fold button + ellipsis to start line
    const btn = document.createElement('span');
    btn.className = 'fold-btn'; btn.textContent = '▼';
    btn.dataset.foldId = startLine + 1;
    startEl.insertBefore(btn, startEl.firstChild);
    startEl.classList.add('fold-hdr');
    startEl.dataset.foldEnd = endLine + 1;
    startEl.dataset.foldType = type;

    const ell = document.createElement('span');
    ell.className = 'fold-ellipsis'; ell.textContent = ' … }';
    startEl.appendChild(ell);

    // Wrap body: all siblings between startEl and endEl (exclusive)
    const wrapper = document.createElement('span');
    wrapper.className = 'fold-region';
    wrapper.id = `FR${startLine + 1}`;

    const toWrap = [];
    let cur = startEl.nextSibling;
    while (cur && cur !== endEl) { toWrap.push(cur); cur = cur.nextSibling; }
    if (toWrap.length) {
      startEl.parentNode.insertBefore(wrapper, toWrap[0]);
      toWrap.forEach(n => wrapper.appendChild(n));
    }
  }

  // Single delegated click handler — remove old one first to avoid duplicates
  if (codeEl._foldHandler) codeEl.removeEventListener('click', codeEl._foldHandler);
  codeEl._foldHandler = e => {
    const btn = e.target.closest('.fold-btn');
    if (!btn) return;
    const id     = parseInt(btn.dataset.foldId);
    const hdr    = lineMap.get(id);
    const region = codeEl.querySelector(`#FR${id}`);
    if (!region) return;
    const folded = region.classList.toggle('folded');
    btn.textContent = folded ? '▶' : '▼';
    if (hdr) hdr.classList.toggle('folded', folded);
  };
  codeEl.addEventListener('click', codeEl._foldHandler);
}

function renderFoldableSource(rawText, codeEl) {
  // hljs sets data-highlighted="yes" and skips re-highlighting if present —
  // remove it so reused elements are highlighted fresh on every load.
  codeEl.removeAttribute('data-highlighted');
  codeEl.textContent = rawText;
  hljs.highlightElement(codeEl);
  codeEl.innerHTML = wrapLines(codeEl.innerHTML);
  const regions = buildFoldRegions(rawText);
  if (regions.length) injectFoldRegions(codeEl, regions);
}

function foldAllInEl(codeEl, mode) {
  codeEl.querySelectorAll('.fold-region').forEach(r => {
    if (mode === 'methods') {
      const id    = r.id.replace('FR', '');
      const hdrEl = codeEl.querySelector(`#L${id}`);
      if (!hdrEl || hdrEl.dataset.foldType !== 'method') return;
    }
    r.classList.add('folded');
  });
  codeEl.querySelectorAll('.fold-hdr').forEach(hdr => {
    if (mode === 'methods' && hdr.dataset.foldType !== 'method') return;
    hdr.classList.add('folded');
    const btn = hdr.querySelector('.fold-btn');
    if (btn) btn.textContent = '▶';
  });
}

function unfoldAllInEl(codeEl) {
  codeEl.querySelectorAll('.fold-region').forEach(r => r.classList.remove('folded'));
  codeEl.querySelectorAll('.fold-hdr').forEach(hdr => {
    hdr.classList.remove('folded');
    const btn = hdr.querySelector('.fold-btn');
    if (btn) btn.textContent = '▼';
  });
}

async function showSource(cls, jumpToMethod) {
  switchCtab('source');
  const toolbar   = document.getElementById('source-toolbar');
  const loadingEl = document.getElementById('source-loading');
  const codeEl    = document.getElementById('source-code');
  const preEl     = document.getElementById('source-pre');

  toolbar.style.display = 'none';
  loadingEl.style.display = 'block';
  codeEl.textContent = '';

  let text;
  try {
    text = await fetchSource(cls.source_file);
  } catch (e) {
    loadingEl.style.display = 'none';
    codeEl.textContent = `// Source not available.\n// Run prepare_sources.py, or click "📁 Local sources"\n// and pick your projectzomboid folder.\n\n// Error: ${e.message}`;
    hljs.highlightElement(codeEl);
    return;
  }

  loadingEl.style.display = 'none';
  renderFoldableSource(text, codeEl);
  toolbar.style.display = '';

  if (jumpToMethod) scrollToMethod(text, jumpToMethod, preEl, codeEl);
  else preEl.scrollTop = 0;
}

function scrollToMethod(sourceText, methodName, panelEl, codeEl) {
  if (!panelEl) panelEl = document.getElementById('source-pre');
  if (!codeEl)  codeEl  = document.getElementById('source-code');

  const lines = sourceText.split('\n');
  const re = new RegExp(`(?:public|protected|private|static|\\s)\\s+\\S[^\\n]*?\\b${methodName}\\s*\\(`);
  let lineIdx = -1;
  for (let i = 0; i < lines.length; i++) { if (re.test(lines[i])) { lineIdx = i; break; } }
  if (lineIdx === -1) {
    for (let i = 0; i < lines.length; i++) { if (lines[i].includes(methodName + '(')) { lineIdx = i; break; } }
  }
  if (lineIdx === -1) return;

  const lineEl = codeEl.querySelector(`#L${lineIdx + 1}`);
  if (lineEl) {
    // Auto-unfold parent fold regions
    let p = lineEl.parentElement;
    while (p && p !== codeEl) {
      if (p.classList.contains('fold-region')) {
        p.classList.remove('folded');
        const id = p.id.replace('FR', '');
        const hdr = codeEl.querySelector(`#L${id}`);
        if (hdr) { hdr.classList.remove('folded'); const b = hdr.querySelector('.fold-btn'); if (b) b.textContent = '▼'; }
      }
      p = p.parentElement;
    }
    lineEl.scrollIntoView({behavior: 'auto', block: 'center'});
    lineEl.classList.add('highlight-line');
    setTimeout(() => lineEl.classList.remove('highlight-line'), 2000);
  } else {
    const lh = parseFloat(getComputedStyle(codeEl).lineHeight) || 19;
    panelEl.scrollTop = Math.max(0, lineIdx * lh - 80);
  }
}

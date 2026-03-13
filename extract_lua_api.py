"""
Extracts the Project Zomboid Lua API from decompiled Java sources.

Two exposure systems:
1. setExposed(Foo.class) in LuaManager -> all public non-static methods callable from Lua
2. @LuaMethod(global=true) in GlobalObject -> global Lua functions

Annotation:
- @UsedFromLua on a class/method = developer-tagged as intended for Lua use

Both are tracked independently:
  set_exposed: true  -> registered in setExposed(), actually callable from Lua
  lua_tagged:  true  -> has @UsedFromLua annotation (developer intent)

Output: lua_api.json
"""

import re
import json
import pathlib
import javalang

_PROJ_ROOT = pathlib.Path("E:/SteamLibrary/steamapps/common/ProjectZomboid/projectzomboid")
SRC_ROOT   = _PROJ_ROOT / "sources"   # decompiled Java sources live here
LUA_MGR    = SRC_ROOT / "zombie/Lua/LuaManager.java"
OUT_FILE   = _PROJ_ROOT / "pz-lua-api-viewer/lua_api.json"

# ---------------------------------------------------------------------------
# Step 1: Parse LuaManager.java — get setExposed FQNs and @LuaMethod globals
# ---------------------------------------------------------------------------
print("Reading LuaManager.java...")
lua_mgr_src = LUA_MGR.read_text(errors="ignore")

import_map = {}
for fqn in re.findall(r'^import\s+([\w.]+);', lua_mgr_src, re.MULTILINE):
    simple = fqn.rsplit(".", 1)[-1]
    import_map[simple] = fqn

def resolve_fqn(name, imap):
    parts = name.split(".")
    base = imap.get(parts[0])
    if base:
        return ".".join([base] + parts[1:]) if len(parts) > 1 else base
    return name

raw_exposed = re.findall(r'this\.setExposed\((\w[\w.]*?)\.class\)', lua_mgr_src)
set_exposed_fqns = set(resolve_fqn(n, import_map) for n in raw_exposed)
print(f"  setExposed entries: {len(set_exposed_fqns)}")

lua_method_blocks = re.findall(
    r'@LuaMethod\(([^)]+)\)\s*\n\s*(?:public\s+)?(?:static\s+)?[\w<>\[\]]+\s+(\w+)\s*\(',
    lua_mgr_src
)
_CATEGORY_MAP = {
    # Query
    'get': 'Query', 'is': 'Query', 'has': 'Query', 'can': 'Query',
    'check': 'Query', 'find': 'Query', 'count': 'Query', 'list': 'Query',
    # Mutation
    'set': 'Mutation', 'add': 'Mutation', 'remove': 'Mutation',
    'delete': 'Mutation', 'create': 'Mutation', 'update': 'Mutation',
    'clear': 'Mutation', 'reset': 'Mutation', 'clone': 'Mutation',
    'replace': 'Mutation', 'change': 'Mutation', 'insert': 'Mutation',
    # Network
    'send': 'Network', 'sync': 'Network', 'request': 'Network',
    'ban': 'Network', 'kick': 'Network', 'connect': 'Network',
    'disconnect': 'Network', 'broadcast': 'Network',
    # Storage
    'load': 'Storage', 'save': 'Storage', 'reload': 'Storage',
    'cache': 'Storage', 'read': 'Storage', 'write': 'Storage',
    # UI
    'show': 'UI', 'hide': 'UI', 'toggle': 'UI', 'screen': 'UI',
    'render': 'UI', 'draw': 'UI', 'display': 'UI',
    # System
    'do': 'System', 'stop': 'System', 'start': 'System', 'end': 'System',
    'instance': 'System', 'process': 'System', 'execute': 'System',
    'run': 'System', 'init': 'System', 'debug': 'System', 'back': 'System',
    'activate': 'System', 'deactivate': 'System', 'enable': 'System',
    'disable': 'System', 'trigger': 'System', 'call': 'System',
    'steam': 'System', 'breakpoint': 'System',
}

def _method_group(lua_name):
    """Return (category, subgroup) for a global function's display grouping.
    Category is a broad bucket; subgroup is the verb prefix of the method name."""
    m = re.match(r'^([a-z]+)', lua_name)
    prefix = m.group(1) if m else 'other'
    category = _CATEGORY_MAP.get(prefix, 'Other')
    return category, prefix

global_functions = []
for attrs, java_name in lua_method_blocks:
    if 'global=true' not in attrs.replace(' ', ''):
        continue
    name_match = re.search(r'name\s*=\s*"([^"]+)"', attrs)
    lua_name = name_match.group(1) if name_match else java_name
    cat, _sub = _method_group(lua_name)
    global_functions.append({"lua_name": lua_name, "java_method": java_name, "category": cat})
print(f"  @LuaMethod global functions: {len(global_functions)}")

# ---------------------------------------------------------------------------
# Step 2: Source parsing helpers
# ---------------------------------------------------------------------------

def strip_method_bodies(src):
    """Strip method bodies so javalang can parse files with Java 14+ switch expressions."""
    result = []
    i = 0
    n = len(src)
    in_string = in_char = in_line_comment = in_block_comment = False

    while i < n:
        c = src[i]

        if in_line_comment:
            result.append(c)
            if c == '\n': in_line_comment = False
            i += 1; continue

        if in_block_comment:
            result.append(c)
            if c == '*' and i + 1 < n and src[i+1] == '/':
                result.append('/'); i += 2; in_block_comment = False
            else:
                i += 1
            continue

        if in_string:
            result.append(c)
            if c == '\\' and i + 1 < n: result.append(src[i+1]); i += 2
            elif c == '"': in_string = False; i += 1
            else: i += 1
            continue

        if in_char:
            result.append(c)
            if c == '\\' and i + 1 < n: result.append(src[i+1]); i += 2
            elif c == "'": in_char = False; i += 1
            else: i += 1
            continue

        if c == '/' and i + 1 < n:
            if src[i+1] == '/': in_line_comment = True; result.append(c); i += 1; continue
            if src[i+1] == '*': in_block_comment = True; result.append(c); i += 1; continue

        if c == '"': in_string = True; result.append(c); i += 1; continue
        if c == "'": in_char = True; result.append(c); i += 1; continue

        if c == '{':
            preceding = ''.join(result).rstrip()
            last = preceding[-1] if preceding else ''
            if last in (')', '>'):
                depth = 1
                result.append('{'); i += 1
                while i < n and depth > 0:
                    ch = src[i]
                    if ch == '{': depth += 1
                    elif ch == '}':
                        depth -= 1
                        if depth == 0: result.append('}'); i += 1; break
                    elif ch == '"':
                        i += 1
                        while i < n:
                            sc = src[i]
                            if sc == '\\': i += 2; continue
                            if sc == '"': i += 1; break
                            i += 1
                        continue
                    elif ch == "'":
                        i += 1
                        while i < n:
                            sc = src[i]
                            if sc == '\\': i += 2; continue
                            if sc == "'": i += 1; break
                            i += 1
                        continue
                    i += 1
                continue
            else:
                result.append(c); i += 1; continue

        result.append(c); i += 1

    return ''.join(result)


def parse_java(src):
    try:
        return javalang.parse.parse(src)
    except Exception:
        try:
            return javalang.parse.parse(strip_method_bodies(src))
        except Exception:
            return None


def type_to_str(t):
    if t is None: return "void"
    if isinstance(t, javalang.tree.BasicType): return t.name
    if isinstance(t, javalang.tree.ReferenceType):
        s = t.name
        if t.arguments:
            args = ", ".join(type_to_str(a.type) if hasattr(a, 'type') else "?" for a in t.arguments)
            s += f"<{args}>"
        if t.sub_type: s += "." + type_to_str(t.sub_type)
        return s
    if isinstance(t, javalang.tree.ArrayType): return type_to_str(t.type) + "[]"
    return str(t)


def has_annotation(node, name):
    return any(a.name == name for a in (node.annotations or []))


def get_public_methods(cls):
    methods = []
    for m in (cls.methods or []):
        mods = m.modifiers or set()
        if "public" not in mods or "static" in mods:
            continue
        params = [{"type": type_to_str(p.type), "name": p.name} for p in (m.parameters or [])]
        methods.append({
            "name": m.name,
            "return_type": type_to_str(m.return_type),
            "params": params,
            "lua_tagged": has_annotation(m, "UsedFromLua"),
        })
    return methods


def get_nested_types(cls_node, parent_fqn):
    nested = []
    if isinstance(cls_node, javalang.tree.EnumDeclaration):
        body = cls_node.body.declarations if cls_node.body else []
    else:
        body = cls_node.body or []
    for member in body:
        if isinstance(member, javalang.tree.ClassDeclaration):
            kind = "class"
        elif isinstance(member, javalang.tree.InterfaceDeclaration):
            kind = "interface"
        elif isinstance(member, javalang.tree.EnumDeclaration):
            kind = "enum"
        else:
            continue
        nested.append({"name": member.name, "kind": kind, "fqn": parent_fqn + "." + member.name})
    return nested


def get_public_fields(cls):
    fields = []
    for f in (cls.fields or []):
        mods = f.modifiers or set()
        if "public" not in mods or "static" in mods:
            continue
        lua_tagged = has_annotation(f, "UsedFromLua")
        for decl in f.declarators:
            fields.append({"name": decl.name, "type": type_to_str(f.type), "lua_tagged": lua_tagged})
    return fields


def fqn_to_path(fqn):
    parts = fqn.split(".")
    for i in range(len(parts), 0, -1):
        candidate = SRC_ROOT / pathlib.Path(*parts[:i]).with_suffix(".java")
        if candidate.exists():
            return candidate, parts[i:]
        if i >= 2:
            sub = SRC_ROOT / pathlib.Path(*parts[:i]) / (parts[i-1] + ".java")
            if sub.exists():
                return sub, parts[i:]
    return None, []


def find_class_in_tree(tree, inner_path):
    all_types = {}
    for _, node in tree.filter(javalang.tree.ClassDeclaration): all_types[node.name] = node
    for _, node in tree.filter(javalang.tree.InterfaceDeclaration): all_types[node.name] = node
    for _, node in tree.filter(javalang.tree.EnumDeclaration): all_types[node.name] = node
    if not inner_path:
        for _, node in tree.filter(javalang.tree.ClassDeclaration): return node
        for _, node in tree.filter(javalang.tree.EnumDeclaration): return node
        return None
    return all_types.get(inner_path[-1])


def build_class_entry(fqn, cls, set_exposed):
    is_enum = isinstance(cls, javalang.tree.EnumDeclaration)
    enum_constants = []
    if is_enum and hasattr(cls, 'body') and cls.body:
        for const in (cls.body.constants or []):
            enum_constants.append({"name": const.name, "type": cls.name, "lua_tagged": False})
    java_file, _ = fqn_to_path(fqn)
    source = str(java_file.relative_to(SRC_ROOT)).replace("\\", "/") if java_file else ""
    return {
        "simple_name": fqn.rsplit(".", 1)[-1],
        "source_file": source,
        "set_exposed": set_exposed,           # registered in LuaManager.setExposed() -> actually callable
        "lua_tagged": has_annotation(cls, "UsedFromLua"),  # has @UsedFromLua annotation -> developer intent
        "is_enum": is_enum,
        "methods": get_public_methods(cls) if not is_enum else [],
        "fields": (enum_constants + get_public_fields(cls)) if is_enum else get_public_fields(cls),
        "nested_types": get_nested_types(cls, fqn),
    }


# ---------------------------------------------------------------------------
# Step 2.5: Enrich global_functions with return types and params
# ---------------------------------------------------------------------------
print("Enriching global function signatures...")
_lua_mgr_tree = parse_java(lua_mgr_src)
if _lua_mgr_tree:
    for _, _go_cls in _lua_mgr_tree.filter(javalang.tree.ClassDeclaration):
        if _go_cls.name == 'GlobalObject':
            _go_sig_map = {
                m.name: {
                    "return_type": type_to_str(m.return_type),
                    "params": [{"type": type_to_str(p.type), "name": p.name}
                               for p in (m.parameters or [])],
                }
                for m in (_go_cls.methods or [])
            }
            for gf in global_functions:
                sig = _go_sig_map.get(gf['java_method'], {})
                gf['return_type'] = sig.get('return_type', '?')
                gf['params']      = sig.get('params', [])
            break
# Ensure all entries have the fields even if parse failed or GlobalObject not found
for gf in global_functions:
    gf.setdefault('return_type', '?')
    gf.setdefault('params', [])
_enriched = sum(1 for gf in global_functions if gf['return_type'] != '?')
print(f"  Enriched {_enriched}/{len(global_functions)} globals with type info")

# ---------------------------------------------------------------------------
# Step 3: Process setExposed classes
# ---------------------------------------------------------------------------
print("Parsing setExposed classes...")
all_classes = {}   # fqn -> entry
parse_errors = []
file_cache = {}    # path -> tree | None

def get_tree(java_file):
    if java_file not in file_cache:
        src = java_file.read_text(errors="ignore")
        tree = parse_java(src)
        if tree is None:
            parse_errors.append(str(java_file.relative_to(SRC_ROOT)).replace("\\", "/"))
        file_cache[java_file] = tree
    return file_cache[java_file]

unresolved = []
for i, fqn in enumerate(sorted(set_exposed_fqns)):
    if i % 50 == 0: print(f"  {i}/{len(set_exposed_fqns)}...")
    java_file, inner_path = fqn_to_path(fqn)
    if java_file is None:
        unresolved.append({"fqn": fqn, "reason": "file_not_found"})
        continue
    tree = get_tree(java_file)
    if tree is None:
        unresolved.append({"fqn": fqn, "reason": "parse_error"})
        continue
    cls = find_class_in_tree(tree, inner_path)
    if cls is None:
        unresolved.append({"fqn": fqn, "reason": "class_not_found"})
        continue
    all_classes[fqn] = build_class_entry(fqn, cls, set_exposed=True)

print(f"  setExposed resolved: {len(all_classes)}")

# ---------------------------------------------------------------------------
# Step 4: Scan all .java files for @UsedFromLua classes NOT in setExposed
# ---------------------------------------------------------------------------
print("Scanning for @UsedFromLua classes not in setExposed...")
lua_tagged_only = 0
_viewer_dir = SRC_ROOT / "pz-lua-api-viewer"
all_java_files = [p for p in SRC_ROOT.rglob("*.java")
                  if not p.is_relative_to(_viewer_dir)]
for i, java_file in enumerate(all_java_files):
    if i % 200 == 0: print(f"  {i}/{len(all_java_files)}...")
    src = java_file.read_text(errors="ignore")
    if "@UsedFromLua" not in src:
        continue
    tree = get_tree(java_file)
    if tree is None:
        continue
    # Derive package from file path
    rel = java_file.relative_to(SRC_ROOT)
    pkg_parts = list(rel.parts[:-1])  # directory parts = package parts
    for _, cls in list(tree.filter(javalang.tree.ClassDeclaration)) + \
                  list(tree.filter(javalang.tree.EnumDeclaration)) + \
                  list(tree.filter(javalang.tree.InterfaceDeclaration)):
        if not has_annotation(cls, "UsedFromLua"):
            continue
        fqn = ".".join(pkg_parts + [cls.name])
        if fqn in all_classes:
            continue  # already added as setExposed
        all_classes[fqn] = build_class_entry(fqn, cls, set_exposed=False)
        lua_tagged_only += 1

print(f"  @UsedFromLua-only (not setExposed): {lua_tagged_only}")

# ---------------------------------------------------------------------------
# Step 4.5: Extract inheritance info (extends, implements) and build subclasses
# ---------------------------------------------------------------------------
print("Extracting inheritance info...")

def get_file_import_map(java_file):
    """Build simple_name → FQN from the file's explicit (non-wildcard) imports."""
    tree = file_cache.get(java_file)
    if tree is None:
        return {}
    imap = {}
    for imp in (tree.imports or []):
        if not imp.static and not getattr(imp, 'wildcard', False):
            fqn_i = imp.path
            imap[fqn_i.rsplit('.', 1)[-1]] = fqn_i
    return imap

def resolve_simple(name, imap, pkg, all_cls):
    """Resolve a simple/short class name to FQN."""
    base = name.split('<')[0].strip()   # strip generics
    if base in imap:
        return imap[base]
    same_pkg = (pkg + '.' + base) if pkg else base
    if same_pkg in all_cls:
        return same_pkg
    if base in _global_simple_to_fqn:
        return _global_simple_to_fqn[base]
    return base  # stdlib / unknown — return as-is

# Global simple→FQN from path structure: zombie/network/GameClient.java → zombie.network.GameClient
# Built now (before step 5) by scanning source files directly.
_global_simple_to_fqn = {}
for _path in SRC_ROOT.rglob("*.java"):
    try:
        _path.relative_to(SRC_ROOT / "pz-lua-api-viewer")
        continue
    except ValueError:
        pass
    _rel = str(_path.relative_to(SRC_ROOT)).replace("\\", "/")
    _simple = _path.stem
    if _simple not in _global_simple_to_fqn:
        _global_simple_to_fqn[_simple] = _rel.removesuffix(".java").replace("/", ".")

for entry_fqn, entry in list(all_classes.items()):
    java_file, inner_path = fqn_to_path(entry_fqn)
    if java_file is None:
        continue
    tree = file_cache.get(java_file)
    if tree is None:
        continue

    all_types = {}
    for _, node in tree.filter(javalang.tree.ClassDeclaration):   all_types[node.name] = node
    for _, node in tree.filter(javalang.tree.InterfaceDeclaration): all_types[node.name] = node
    for _, node in tree.filter(javalang.tree.EnumDeclaration):     all_types[node.name] = node

    cls_name = inner_path[-1] if inner_path else entry['simple_name']
    cls = all_types.get(cls_name)
    if cls is None:
        continue

    imap = get_file_import_map(java_file)
    pkg  = '.'.join(entry_fqn.split('.')[:-1])

    # extends (ClassDeclaration only — not enums, not interfaces)
    if isinstance(cls, javalang.tree.ClassDeclaration) and cls.extends:
        resolved = resolve_simple(cls.extends.name, imap, pkg, all_classes)
        if resolved not in ('java.lang.Object', 'Object'):
            entry['extends'] = resolved

    # implements
    impl_list = getattr(cls, 'implements', None) or []
    if impl_list:
        impls = [resolve_simple(i.name, imap, pkg, all_classes) for i in impl_list]
        if impls:
            entry['implements'] = impls

# ---------------------------------------------------------------------------
# Step 4.6: Build _extends_map for non-API intermediate classes in chains
# ---------------------------------------------------------------------------
# BFS from any extends value not in all_classes, walking upward until we hit
# a known class or a stdlib/unresolvable root.
from collections import deque

_extends_map = {}  # non-API fqn → parent fqn
_resolve_queue = deque()
_resolve_visited = set()

for entry in all_classes.values():
    parent = entry.get('extends')
    if parent and parent not in all_classes:
        _resolve_queue.append(parent)

while _resolve_queue:
    fqn_r = _resolve_queue.popleft()
    if fqn_r in _resolve_visited or fqn_r in all_classes:
        continue
    _resolve_visited.add(fqn_r)

    java_file_r, inner_path_r = fqn_to_path(fqn_r)
    if java_file_r is None:
        continue

    tree_r = file_cache.get(java_file_r)
    if tree_r is None:
        src_r = java_file_r.read_text(errors="ignore")
        tree_r = parse_java(src_r)
        if tree_r is not None:
            file_cache[java_file_r] = tree_r
    if tree_r is None:
        continue

    all_types_r = {}
    for _, node in tree_r.filter(javalang.tree.ClassDeclaration):
        all_types_r[node.name] = node

    cls_name_r = inner_path_r[-1] if inner_path_r else fqn_r.rsplit('.', 1)[-1]
    cls_r = all_types_r.get(cls_name_r)
    if cls_r is None or not isinstance(cls_r, javalang.tree.ClassDeclaration) or not cls_r.extends:
        continue

    imap_r  = get_file_import_map(java_file_r)
    pkg_r   = '.'.join(fqn_r.split('.')[:-1])
    parent_r = resolve_simple(cls_r.extends.name, imap_r, pkg_r, all_classes)
    if parent_r not in ('java.lang.Object', 'Object'):
        _extends_map[fqn_r] = parent_r
        if parent_r not in all_classes:
            _resolve_queue.append(parent_r)

print(f"  Non-API extends-map entries: {len(_extends_map)}")

# Build subclasses inverse map — include both API children and non-API intermediates
for entry_fqn, entry in all_classes.items():
    parent = entry.get('extends')
    if parent and parent in all_classes:
        all_classes[parent].setdefault('subclasses', []).append(entry_fqn)
# Also add non-API intermediates as subclasses of their API parents
for child_fqn, parent_fqn in _extends_map.items():
    if parent_fqn in all_classes:
        all_classes[parent_fqn].setdefault('subclasses', []).append(child_fqn)
for entry in all_classes.values():
    if 'subclasses' in entry:
        entry['subclasses'].sort()

print(f"  Classes with extends:    {sum(1 for v in all_classes.values() if 'extends' in v)}")
print(f"  Classes with implements: {sum(1 for v in all_classes.values() if 'implements' in v)}")
print(f"  Classes with subclasses: {sum(1 for v in all_classes.values() if 'subclasses' in v)}")

# ---------------------------------------------------------------------------
# Step 4.7: Build _interface_extends map — interface FQN → [parent interface FQNs]
# BFS from every interface that appears in any class's implements list.
# ---------------------------------------------------------------------------
print("Extracting interface extends chains...")

_interface_extends = {}  # interface_fqn → [parent_interface_fqn, ...]
_iface_queue   = deque()
_iface_visited = set()

for entry in all_classes.values():
    for iface in (entry.get('implements') or []):
        if iface not in _iface_visited:
            _iface_queue.append(iface)

while _iface_queue:
    iface_fqn = _iface_queue.popleft()
    if iface_fqn in _iface_visited:
        continue
    _iface_visited.add(iface_fqn)

    java_file_i, inner_path_i = fqn_to_path(iface_fqn)
    if java_file_i is None:
        continue

    tree_i = file_cache.get(java_file_i)
    if tree_i is None:
        src_i = java_file_i.read_text(errors="ignore")
        tree_i = parse_java(src_i)
        if tree_i is not None:
            file_cache[java_file_i] = tree_i
    if tree_i is None:
        continue

    iface_name_i = inner_path_i[-1] if inner_path_i else iface_fqn.rsplit('.', 1)[-1]
    imap_i = get_file_import_map(java_file_i)
    pkg_i  = '.'.join(iface_fqn.split('.')[:-1])

    iface_node_i = None
    for _, node in tree_i.filter(javalang.tree.InterfaceDeclaration):
        if node.name == iface_name_i:
            iface_node_i = node
            break
    if iface_node_i is None:
        continue

    ext_i = getattr(iface_node_i, 'extends', None) or []
    if not ext_i:
        continue

    resolved_i = []
    for e in ext_i:
        r = resolve_simple(e.name, imap_i, pkg_i, all_classes)
        resolved_i.append(r)
        if r not in _iface_visited:
            _iface_queue.append(r)

    if resolved_i:
        _interface_extends[iface_fqn] = resolved_i

print(f"  Interfaces with extends: {len(_interface_extends)}")

# Build FQN → path map for all visited interfaces that are NOT in the API.
# Using FQN as the key avoids simple-name collisions with API classes.
_interface_paths = {}
for _iface_fqn in _iface_visited:
    if _iface_fqn not in all_classes:
        _jf, _ = fqn_to_path(_iface_fqn)
        if _jf and _jf.exists():
            _interface_paths[_iface_fqn] = str(_jf.relative_to(SRC_ROOT)).replace("\\", "/")
print(f"  Interface source paths:  {len(_interface_paths)}")

# ---------------------------------------------------------------------------
# Step 5: Build _source_index — source-linkable classes NOT in the API
# ---------------------------------------------------------------------------
print("Building source index...")

def build_source_index(root):
    """Map simple class name -> relative .java path for all .java files under root.
    Skips the pz-lua-api-viewer/ subtree to avoid double-counting copied sources."""
    viewer_dir = root / "pz-lua-api-viewer"
    index = {}
    for path in root.rglob("*.java"):
        try:
            path.relative_to(viewer_dir)
            continue  # inside pz-lua-api-viewer/, skip
        except ValueError:
            pass
        simple = path.stem
        if simple not in index:
            index[simple] = str(path.relative_to(root)).replace("\\", "/")
    return index

all_source_files = build_source_index(SRC_ROOT)
api_simple_names = {v["simple_name"] for v in all_classes.values()}
source_index = {simple: path for simple, path in all_source_files.items()
                if simple not in api_simple_names}
print(f"  Source-only entries: {len(source_index)}")

# ---------------------------------------------------------------------------
# Step 6: Save
# ---------------------------------------------------------------------------
# Build _class_by_simple_name: reverse map simple → [fqn, ...]
class_by_simple_name = {}
for fqn in all_classes:
    simple = fqn.split('.')[-1]
    class_by_simple_name.setdefault(simple, []).append(fqn)

# Build _source_only_paths: source index entries whose simple name is NOT in the API
source_only_paths = {
    simple: path
    for simple, path in source_index.items()
    if simple not in class_by_simple_name
}

api = {
    "classes": all_classes,
    "global_functions": global_functions,
    "_source_index": source_index,
    "_class_by_simple_name": class_by_simple_name,
    "_source_only_paths": source_only_paths,
    "_extends_map": _extends_map,
    "_interface_extends": _interface_extends,
    "_interface_paths": _interface_paths,
    "unresolved": unresolved,
    "_meta": {
        "total_classes": len(all_classes),
        "set_exposed_count": sum(1 for v in all_classes.values() if v["set_exposed"]),
        "lua_tagged_count": sum(1 for v in all_classes.values() if v["lua_tagged"]),
        "both_count": sum(1 for v in all_classes.values() if v["set_exposed"] and v["lua_tagged"]),
        "set_exposed_only_count": sum(1 for v in all_classes.values() if v["set_exposed"] and not v["lua_tagged"]),
        "lua_tagged_only_count": sum(1 for v in all_classes.values() if not v["set_exposed"] and v["lua_tagged"]),
        "total_global_functions": len(global_functions),
        "total_methods": sum(len(v["methods"]) for v in all_classes.values()),
        "total_fields": sum(len(v["fields"]) for v in all_classes.values()),
        "unresolved_count": len(unresolved),
        "parse_errors": parse_errors,
    },
}

print(f"\nSummary:")
print(f"  Total classes:         {api['_meta']['total_classes']}")
print(f"  setExposed + tagged:   {api['_meta']['both_count']}")
print(f"  setExposed only:       {api['_meta']['set_exposed_only_count']}")
print(f"  lua_tagged only:       {api['_meta']['lua_tagged_only_count']}")
print(f"  Global functions:      {api['_meta']['total_global_functions']}")
print(f"  Total methods:         {api['_meta']['total_methods']}")
print(f"  Unresolved:            {api['_meta']['unresolved_count']}")
print(f"  Parse errors:          {len(parse_errors)}")

print(f"\nWriting {OUT_FILE}...")
OUT_FILE.write_text(json.dumps(api, indent=2), encoding="utf-8")
print("Done.")

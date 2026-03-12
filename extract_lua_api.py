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

SRC_ROOT = pathlib.Path("E:/SteamLibrary/steamapps/common/ProjectZomboid/projectzomboid")
LUA_MGR  = SRC_ROOT / "zombie/Lua/LuaManager.java"
OUT_FILE = SRC_ROOT / "lua_api.json"

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
global_functions = []
for attrs, java_name in lua_method_blocks:
    if 'global=true' not in attrs.replace(' ', ''):
        continue
    name_match = re.search(r'name\s*=\s*"([^"]+)"', attrs)
    lua_name = name_match.group(1) if name_match else java_name
    global_functions.append({"lua_name": lua_name, "java_method": java_name})
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
    }


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
all_java_files = list(SRC_ROOT.rglob("*.java"))
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

# Build subclasses inverse map (must run after _global_simple_to_fqn is populated,
# which happens in step 5 — but we only need all_classes keys here so it's fine)
for entry_fqn, entry in all_classes.items():
    parent = entry.get('extends')
    if parent and parent in all_classes:
        all_classes[parent].setdefault('subclasses', []).append(entry_fqn)
for entry in all_classes.values():
    if 'subclasses' in entry:
        entry['subclasses'].sort()

print(f"  Classes with extends:    {sum(1 for v in all_classes.values() if 'extends' in v)}")
print(f"  Classes with implements: {sum(1 for v in all_classes.values() if 'implements' in v)}")
print(f"  Classes with subclasses: {sum(1 for v in all_classes.values() if 'subclasses' in v)}")

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
api = {
    "classes": all_classes,
    "global_functions": global_functions,
    "_source_index": source_index,
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

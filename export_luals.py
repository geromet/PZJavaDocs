#!/usr/bin/env python3
"""Export normalized Project Zomboid API data as LuaLS/LuaCATS definition files."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
from pathlib import Path
from typing import Any

SCHEMA_VERSION = 1
DEFAULT_INPUT = Path("lua_api.json")
DEFAULT_OUTPUT_DIR = Path("generated/luals")

_LUA_KEYWORDS = {
    "and", "break", "do", "else", "elseif", "end", "false", "for", "function",
    "goto", "if", "in", "local", "nil", "not", "or", "repeat", "return", "then",
    "true", "until", "while",
}
_IDENTIFIER_RE = re.compile(r"^[A-Za-z_][A-Za-z0-9_]*$")
_NUMERIC_INTEGER_TYPES = {
    "byte", "short", "int", "long",
    "Byte", "Short", "Integer", "Long",
    "java.lang.Byte", "java.lang.Short", "java.lang.Integer", "java.lang.Long",
}
_NUMERIC_NUMBER_TYPES = {
    "float", "double", "Float", "Double",
    "java.lang.Float", "java.lang.Double",
}
_STRING_TYPES = {
    "char", "Character", "String", "CharSequence",
    "java.lang.Character", "java.lang.String", "java.lang.CharSequence",
}
_BOOLEAN_TYPES = {"boolean", "Boolean", "java.lang.Boolean"}
_ANY_TYPES = {"?", "Object", "java.lang.Object"}
_SEQUENCE_TYPES = {
    "List", "ArrayList", "LinkedList", "Collection", "Iterable", "Set", "HashSet",
    "java.util.List", "java.util.ArrayList", "java.util.LinkedList",
    "java.util.Collection", "java.lang.Iterable", "java.util.Set", "java.util.HashSet",
}
_MAP_TYPES = {
    "Map", "HashMap", "LinkedHashMap", "TreeMap",
    "java.util.Map", "java.util.HashMap", "java.util.LinkedHashMap", "java.util.TreeMap",
}
_OPTIONAL_TYPES = {"Optional", "java.util.Optional"}


def _validate_build_id(build_id: str) -> str:
    build_id = build_id.strip()
    if not build_id:
        raise ValueError("build_id must not be empty")
    if "\n" in build_id or "\r" in build_id:
        raise ValueError("build_id must be a single line")
    return build_id


def _safe_identifier(name: str, fallback: str) -> str:
    name = str(name or "").strip()
    if not name:
        return fallback
    if not _IDENTIFIER_RE.fullmatch(name) or name in _LUA_KEYWORDS:
        sanitized = re.sub(r"[^A-Za-z0-9_]", "_", name)
        if not sanitized or sanitized[0].isdigit():
            sanitized = "_" + sanitized
        if sanitized in _LUA_KEYWORDS or not _IDENTIFIER_RE.fullmatch(sanitized):
            sanitized = "_" + sanitized
        name = sanitized
    return name or fallback


def _safe_type_atom(name: str) -> str:
    name = str(name or "").strip().replace("$", ".")
    if not name:
        return "any"
    parts = []
    for i, part in enumerate(name.split(".")):
        part = _safe_identifier(part, f"Type{i}")
        parts.append(part)
    return ".".join(parts)


def _split_generic_args(text: str) -> list[str]:
    args: list[str] = []
    start = 0
    depth = 0
    for i, ch in enumerate(text):
        if ch == "<":
            depth += 1
        elif ch == ">":
            depth = max(0, depth - 1)
        elif ch == "," and depth == 0:
            args.append(text[start:i].strip())
            start = i + 1
    args.append(text[start:].strip())
    return [arg for arg in args if arg]


def _split_generic(java_type: str) -> tuple[str, list[str]] | None:
    java_type = java_type.strip()
    lt = java_type.find("<")
    if lt < 0 or not java_type.endswith(">"):
        return None
    base = java_type[:lt].strip()
    inner = java_type[lt + 1:-1].strip()
    return base, _split_generic_args(inner)


def java_type_to_luacats(java_type: Any, type_name_map: dict[str, str] | None = None) -> str:
    """Map extractor Java type strings to conservative LuaCATS types."""
    type_name_map = type_name_map or {}
    raw = str(java_type or "?").strip()
    if not raw:
        return "any"

    if raw.endswith("[]"):
        return f"{java_type_to_luacats(raw[:-2], type_name_map)}[]"

    if raw in _ANY_TYPES:
        return "any"
    if raw in _BOOLEAN_TYPES:
        return "boolean"
    if raw in _NUMERIC_INTEGER_TYPES:
        return "integer"
    if raw in _NUMERIC_NUMBER_TYPES:
        return "number"
    if raw in _STRING_TYPES:
        return "string"
    if raw == "void":
        return "nil"

    if raw.startswith("? extends "):
        return java_type_to_luacats(raw[len("? extends "):], type_name_map)
    if raw.startswith("? super "):
        return java_type_to_luacats(raw[len("? super "):], type_name_map)

    generic = _split_generic(raw)
    if generic:
        base, args = generic
        mapped_args = [java_type_to_luacats(arg, type_name_map) for arg in args]
        if base in _SEQUENCE_TYPES and mapped_args:
            return f"{mapped_args[0]}[]"
        if base in _MAP_TYPES and len(mapped_args) >= 2:
            return f"table<{mapped_args[0]}, {mapped_args[1]}>"
        if base in _OPTIONAL_TYPES and mapped_args:
            return f"{mapped_args[0]}|nil"
        return f"{type_name_map.get(base, _safe_type_atom(base))}<{', '.join(mapped_args)}>"

    return type_name_map.get(raw, _safe_type_atom(raw))


def _build_type_name_map(classes: dict[str, Any]) -> dict[str, str]:
    result: dict[str, str] = {}
    used: dict[str, str] = {}
    for fqn in sorted(classes):
        base = _safe_type_atom(fqn)
        candidate = base
        prior = used.get(candidate)
        if prior is not None and prior != fqn:
            suffix = hashlib.sha1(fqn.encode("utf-8")).hexdigest()[:8]
            candidate = f"{base}_{suffix}"
        result[fqn] = candidate
        used[candidate] = fqn
    return result


def _receiver_name(type_name: str) -> str:
    digest = hashlib.sha1(type_name.encode("utf-8")).hexdigest()[:12]
    return f"_pz_{digest}"


def _normalize_params(params: Any, type_name_map: dict[str, str]) -> list[tuple[str, str]]:
    result: list[tuple[str, str]] = []
    used: set[str] = set()
    for i, param in enumerate(params or []):
        if not isinstance(param, dict):
            param = {}
        base = _safe_identifier(param.get("name", ""), f"arg{i + 1}")
        name = base
        n = 2
        while name in used:
            name = f"{base}_{n}"
            n += 1
        used.add(name)
        result.append((name, java_type_to_luacats(param.get("type", "?"), type_name_map)))
    return result


def _field_name(name: Any) -> str:
    name = str(name or "")
    if _IDENTIFIER_RE.fullmatch(name) and name not in _LUA_KEYWORDS:
        return name
    return f"[{json.dumps(name, ensure_ascii=False)}]"


def _signature_sort_key(method: dict[str, Any]) -> tuple[Any, ...]:
    params = method.get("params") or []
    return (
        str(method.get("name", "")),
        tuple(str(p.get("type", "?")) for p in params if isinstance(p, dict)),
        str(method.get("return_type", "?")),
    )


def _emit_signature_annotations(
    lines: list[str],
    entry: dict[str, Any],
    type_name_map: dict[str, str],
) -> list[str]:
    params = _normalize_params(entry.get("params"), type_name_map)
    for name, typ in params:
        lines.append(f"---@param {name} {typ}")
    ret = java_type_to_luacats(entry.get("return_type", "?"), type_name_map)
    if ret != "nil":
        lines.append(f"---@return {ret}")
    return [name for name, _ in params]


def _emit_class(
    lines: list[str],
    fqn: str,
    entry: dict[str, Any],
    type_name_map: dict[str, str],
) -> None:
    type_name = type_name_map[fqn]
    parents: list[str] = []
    extends = entry.get("extends")
    if extends:
        parents.append(java_type_to_luacats(extends, type_name_map))
    for iface in entry.get("implements") or []:
        mapped = java_type_to_luacats(iface, type_name_map)
        if mapped not in parents:
            parents.append(mapped)

    # Keep each generated receiver local in its own lexical scope. A full Project
    # Zomboid corpus contains far more than Lua's 200 simultaneously active locals.
    lines.append("do")
    class_line = f"---@class {type_name}"
    if parents:
        class_line += ": " + ", ".join(parents)
    lines.append(class_line)

    for field in sorted(
        (f for f in (entry.get("fields") or []) if isinstance(f, dict)),
        key=lambda f: str(f.get("name", "")),
    ):
        lines.append(
            f"---@field {_field_name(field.get('name'))} "
            f"{java_type_to_luacats(field.get('type', '?'), type_name_map)}"
        )

    receiver = _receiver_name(type_name)
    lines.append(f"local {receiver} = {{}}")

    for method in sorted(
        (m for m in (entry.get("methods") or []) if isinstance(m, dict)),
        key=_signature_sort_key,
    ):
        method_name = str(method.get("name", ""))
        arg_names = _emit_signature_annotations(lines, method, type_name_map)
        args = ", ".join(arg_names)
        if _IDENTIFIER_RE.fullmatch(method_name) and method_name not in _LUA_KEYWORDS:
            lines.append(f"function {receiver}:{method_name}({args}) end")
        else:
            # Preserve unusual Java names without allowing generated Lua syntax injection.
            quoted = json.dumps(method_name, ensure_ascii=False)
            lines.append(f"{receiver}[{quoted}] = function({args}) end")

    lines.append("end")
    lines.append("")


def _emit_global_function(
    lines: list[str],
    entry: dict[str, Any],
    type_name_map: dict[str, str],
) -> None:
    lua_name = str(entry.get("lua_name", ""))
    arg_names = _emit_signature_annotations(lines, entry, type_name_map)
    args = ", ".join(arg_names)
    if _IDENTIFIER_RE.fullmatch(lua_name) and lua_name not in _LUA_KEYWORDS:
        lines.append(f"function {lua_name}({args}) end")
    else:
        quoted = json.dumps(lua_name, ensure_ascii=False)
        lines.append(f"_G[{quoted}] = function({args}) end")


def render_library(api: dict[str, Any], build_id: str) -> str:
    build_id = _validate_build_id(build_id)
    classes = api.get("classes")
    globals_ = api.get("global_functions")
    if not isinstance(classes, dict):
        raise ValueError("input API must contain an object-valued 'classes' field")
    if not isinstance(globals_, list):
        raise ValueError("input API must contain an array-valued 'global_functions' field")

    type_name_map = _build_type_name_map(classes)
    lines = [
        "---@meta _",
        "",
        "-- Generated by export_luals.py. Do not edit by hand.",
        f"-- Project Zomboid API build: {build_id}",
        f"-- Export schema: {SCHEMA_VERSION}",
        "",
    ]

    for fqn in sorted(classes):
        entry = classes[fqn]
        if not isinstance(entry, dict):
            continue
        _emit_class(lines, fqn, entry, type_name_map)

    if globals_:
        lines.extend([
            "-- Global Lua functions",
            "",
        ])
        for entry in sorted(
            (g for g in globals_ if isinstance(g, dict)),
            key=lambda g: (
                str(g.get("lua_name", "")),
                tuple(str(p.get("type", "?")) for p in (g.get("params") or []) if isinstance(p, dict)),
                str(g.get("return_type", "?")),
            ),
        ):
            _emit_global_function(lines, entry, type_name_map)
            lines.append("")

    return "\n".join(lines).rstrip() + "\n"


def build_metadata(api: dict[str, Any], build_id: str) -> dict[str, Any]:
    build_id = _validate_build_id(build_id)
    classes = api.get("classes")
    globals_ = api.get("global_functions")
    if not isinstance(classes, dict) or not isinstance(globals_, list):
        raise ValueError("input API must contain 'classes' object and 'global_functions' array")
    return {
        "schema_version": SCHEMA_VERSION,
        "project_zomboid_build": build_id,
        "source": "lua_api.json",
        "library_file": "library.lua",
        "class_count": len(classes),
        "global_function_count": len(globals_),
    }


def export_api(input_path: Path, output_dir: Path, build_id: str) -> None:
    api = json.loads(input_path.read_text(encoding="utf-8"))
    if not isinstance(api, dict):
        raise ValueError("input API root must be a JSON object")

    library = render_library(api, build_id)
    metadata = build_metadata(api, build_id)

    output_dir.mkdir(parents=True, exist_ok=True)
    (output_dir / "library.lua").write_text(library, encoding="utf-8", newline="\n")
    (output_dir / "metadata.json").write_text(
        json.dumps(metadata, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
        newline="\n",
    )


def _parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Export lua_api.json as a deterministic LuaLS/LuaCATS definition library."
    )
    parser.add_argument("--input", type=Path, default=DEFAULT_INPUT, help="Path to lua_api.json")
    parser.add_argument(
        "--output-dir",
        type=Path,
        default=DEFAULT_OUTPUT_DIR,
        help="Generated LuaLS library directory",
    )
    parser.add_argument(
        "--build-id",
        required=True,
        help="Explicit Project Zomboid build/version represented by the input snapshot",
    )
    return parser.parse_args()


def main() -> int:
    args = _parse_args()
    export_api(args.input, args.output_dir, args.build_id)
    print(f"Wrote LuaLS definitions to {args.output_dir}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

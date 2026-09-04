#!/usr/bin/env python3
"""Compare two generated PZJavaDocs lua_api.json snapshots deterministically."""

from __future__ import annotations

import argparse
import json
from pathlib import Path
from typing import Any, Iterable


def _params(member: dict[str, Any]) -> list[dict[str, str]]:
    return [
        {"type": str(param.get("type", "?")), "name": str(param.get("name", ""))}
        for param in member.get("params", [])
    ]


def _method_shape(method: dict[str, Any]) -> dict[str, Any]:
    return {
        "name": str(method.get("name", "")),
        "params": _params(method),
        "return_type": str(method.get("return_type", "?")),
        "lua_tagged": bool(method.get("lua_tagged", False)),
    }


def _method_key(method: dict[str, Any]) -> tuple[str, tuple[str, ...]]:
    return (
        str(method.get("name", "")),
        tuple(str(param.get("type", "?")) for param in method.get("params", [])),
    )


def _field_shape(field: dict[str, Any]) -> dict[str, Any]:
    return {
        "name": str(field.get("name", "")),
        "type": str(field.get("type", "?")),
        "lua_tagged": bool(field.get("lua_tagged", False)),
    }


def _class_shape(cls: dict[str, Any]) -> dict[str, Any]:
    return {
        "extends": cls.get("extends"),
        "implements": sorted(str(value) for value in cls.get("implements", [])),
        "set_exposed": bool(cls.get("set_exposed", False)),
        "lua_tagged": bool(cls.get("lua_tagged", False)),
        "is_enum": bool(cls.get("is_enum", False)),
    }


def _global_shape(global_fn: dict[str, Any]) -> dict[str, Any]:
    result = {
        "lua_name": str(global_fn.get("lua_name", "")),
        "java_method": str(global_fn.get("java_method", "")),
        "params": _params(global_fn),
        "return_type": str(global_fn.get("return_type", "?")),
    }
    for optional in ("category", "group", "domain", "section"):
        if optional in global_fn:
            result[optional] = global_fn[optional]
    return result


def _record(
    entity_kind: str,
    entity: str,
    change_kind: str,
    *,
    member_kind: str | None = None,
    member: str | None = None,
    before: Any = None,
    after: Any = None,
) -> dict[str, Any]:
    record: dict[str, Any] = {
        "entity_kind": entity_kind,
        "entity": entity,
        "change_kind": change_kind,
    }
    if member_kind is not None:
        record["member_kind"] = member_kind
    if member is not None:
        record["member"] = member
    if before is not None:
        record["before"] = before
    if after is not None:
        record["after"] = after
    return record


def _method_label(shape: dict[str, Any]) -> str:
    params = ", ".join(param["type"] for param in shape["params"])
    return f'{shape["name"]}({params})'


def _compare_methods(
    fqn: str,
    old_methods: Iterable[dict[str, Any]],
    new_methods: Iterable[dict[str, Any]],
) -> list[dict[str, Any]]:
    changes: list[dict[str, Any]] = []
    old_map = {_method_key(method): _method_shape(method) for method in old_methods}
    new_map = {_method_key(method): _method_shape(method) for method in new_methods}

    for key in sorted(old_map.keys() & new_map.keys()):
        before, after = old_map[key], new_map[key]
        if before != after:
            changes.append(_record(
                "class", fqn, "changed",
                member_kind="method", member=_method_label(after),
                before=before, after=after,
            ))

    old_only = set(old_map) - set(new_map)
    new_only = set(new_map) - set(old_map)

    # Pair only a unique removed/added signature with the same name. That recognizes
    # ordinary parameter changes without guessing through ambiguous overload sets.
    old_by_name: dict[str, list[tuple[str, tuple[str, ...]]]] = {}
    new_by_name: dict[str, list[tuple[str, tuple[str, ...]]]] = {}
    for key in old_only:
        old_by_name.setdefault(key[0], []).append(key)
    for key in new_only:
        new_by_name.setdefault(key[0], []).append(key)

    paired_old: set[tuple[str, tuple[str, ...]]] = set()
    paired_new: set[tuple[str, tuple[str, ...]]] = set()
    for name in sorted(set(old_by_name) & set(new_by_name)):
        olds, news = old_by_name[name], new_by_name[name]
        if len(olds) == 1 and len(news) == 1:
            old_key, new_key = olds[0], news[0]
            changes.append(_record(
                "class", fqn, "changed",
                member_kind="method", member=name,
                before=old_map[old_key], after=new_map[new_key],
            ))
            paired_old.add(old_key)
            paired_new.add(new_key)

    for key in sorted(old_only - paired_old):
        shape = old_map[key]
        changes.append(_record(
            "class", fqn, "removed",
            member_kind="method", member=_method_label(shape), before=shape,
        ))
    for key in sorted(new_only - paired_new):
        shape = new_map[key]
        changes.append(_record(
            "class", fqn, "added",
            member_kind="method", member=_method_label(shape), after=shape,
        ))
    return changes


def _compare_fields(
    fqn: str,
    old_fields: Iterable[dict[str, Any]],
    new_fields: Iterable[dict[str, Any]],
) -> list[dict[str, Any]]:
    changes: list[dict[str, Any]] = []
    old_map = {str(field.get("name", "")): _field_shape(field) for field in old_fields}
    new_map = {str(field.get("name", "")): _field_shape(field) for field in new_fields}

    for name in sorted(old_map.keys() | new_map.keys()):
        before, after = old_map.get(name), new_map.get(name)
        if before is None:
            changes.append(_record("class", fqn, "added", member_kind="field", member=name, after=after))
        elif after is None:
            changes.append(_record("class", fqn, "removed", member_kind="field", member=name, before=before))
        elif before != after:
            changes.append(_record("class", fqn, "changed", member_kind="field", member=name, before=before, after=after))
    return changes


def compare_snapshots(
    old: dict[str, Any],
    new: dict[str, Any],
    old_id: str,
    new_id: str,
) -> dict[str, Any]:
    if not old_id.strip() or not new_id.strip():
        raise ValueError("snapshot identities must be explicit and non-empty")

    changes: list[dict[str, Any]] = []
    old_classes = old.get("classes", {})
    new_classes = new.get("classes", {})

    for fqn in sorted(set(old_classes) | set(new_classes)):
        before, after = old_classes.get(fqn), new_classes.get(fqn)
        if before is None:
            changes.append(_record("class", fqn, "added", after=_class_shape(after)))
            continue
        if after is None:
            changes.append(_record("class", fqn, "removed", before=_class_shape(before)))
            continue

        before_shape, after_shape = _class_shape(before), _class_shape(after)
        if before_shape != after_shape:
            changes.append(_record("class", fqn, "changed", before=before_shape, after=after_shape))
        changes.extend(_compare_fields(fqn, before.get("fields", []), after.get("fields", [])))
        changes.extend(_compare_methods(fqn, before.get("methods", []), after.get("methods", [])))

    old_globals = {
        str(item.get("lua_name", "")): _global_shape(item)
        for item in old.get("global_functions", [])
    }
    new_globals = {
        str(item.get("lua_name", "")): _global_shape(item)
        for item in new.get("global_functions", [])
    }
    for name in sorted(set(old_globals) | set(new_globals)):
        before, after = old_globals.get(name), new_globals.get(name)
        if before is None:
            changes.append(_record("global", name, "added", after=after))
        elif after is None:
            changes.append(_record("global", name, "removed", before=before))
        elif before != after:
            changes.append(_record("global", name, "changed", before=before, after=after))

    changes.sort(key=lambda item: (
        item["entity_kind"],
        item["entity"],
        item.get("member_kind", ""),
        item.get("member", ""),
        item["change_kind"],
    ))
    return {
        "schema_version": 1,
        "old_snapshot": old_id,
        "new_snapshot": new_id,
        "changes": changes,
    }


def serialize_diff(diff: dict[str, Any]) -> str:
    return json.dumps(diff, indent=2, sort_keys=True, ensure_ascii=False) + "\n"


def main() -> int:
    parser = argparse.ArgumentParser(description="Compare two generated PZJavaDocs API snapshots.")
    parser.add_argument("old_snapshot", type=Path)
    parser.add_argument("new_snapshot", type=Path)
    parser.add_argument("--old-id", required=True, help="Explicit old Project Zomboid build/snapshot identity.")
    parser.add_argument("--new-id", required=True, help="Explicit new Project Zomboid build/snapshot identity.")
    parser.add_argument("--out", type=Path, default=Path("api_diff.json"))
    args = parser.parse_args()

    old = json.loads(args.old_snapshot.read_text(encoding="utf-8"))
    new = json.loads(args.new_snapshot.read_text(encoding="utf-8"))
    diff = compare_snapshots(old, new, args.old_id, args.new_id)
    args.out.write_text(serialize_diff(diff), encoding="utf-8")
    print(f"Wrote {len(diff['changes'])} API changes to {args.out}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

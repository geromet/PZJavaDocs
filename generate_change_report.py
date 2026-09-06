#!/usr/bin/env python3
"""Generate reproducible PZJavaDocs snapshots, diff, and summary for two source trees."""

from __future__ import annotations

import argparse
import json
import os
import shutil
import subprocess
import sys
import tempfile
from collections import Counter
from contextlib import contextmanager
from pathlib import Path
from typing import Any


REPO_ROOT = Path(__file__).resolve().parent
EXTRACTOR = REPO_ROOT / "extract_lua_api.py"
COMPARATOR = REPO_ROOT / "compare_api.py"
OUTPUT_NAMES = ("old-lua-api.json", "new-lua-api.json", "api-diff.json", "summary.json")
LOCK_NAME = ".pzjavadocs-report.lock"


def _non_empty(value: str, label: str) -> str:
    normalized = value.strip()
    if not normalized:
        raise ValueError(f"{label} must be explicit and non-empty")
    if any(ord(char) < 32 or ord(char) == 127 for char in normalized):
        raise ValueError(f"{label} must not contain control characters")
    return normalized


@contextmanager
def _reserve_output_dir(output_dir: Path):
    resolved = output_dir.resolve()
    if not resolved.is_dir():
        raise ValueError(f"output directory does not exist or is not a directory: {resolved}")
    lock_path = resolved / LOCK_NAME
    try:
        lock_fd = os.open(lock_path, os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600)
    except FileExistsError as exc:
        raise ValueError(f"another report run owns the output directory: {resolved}") from exc
    os.close(lock_fd)
    try:
        occupied = [name for name in OUTPUT_NAMES if (resolved / name).exists()]
        if occupied:
            raise ValueError("refusing to overwrite report artifacts: " + ", ".join(occupied))
        yield resolved
    finally:
        lock_path.unlink(missing_ok=True)


def _publish(staging: Path, output_dir: Path) -> None:
    created: list[Path] = []
    try:
        for name in OUTPUT_NAMES:
            destination = output_dir / name
            with (staging / name).open("rb") as source, destination.open("xb") as target:
                created.append(destination)
                shutil.copyfileobj(source, target)
    except BaseException:
        for path in created:
            path.unlink(missing_ok=True)
        raise


def _run(command: list[str]) -> None:
    subprocess.run(command, cwd=REPO_ROOT, check=True)


def _load_json(path: Path) -> dict[str, Any]:
    payload = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(payload, dict):
        raise ValueError(f"expected a JSON object in {path}")
    return payload


def build_summary(old: dict[str, Any], new: dict[str, Any], diff: dict[str, Any]) -> dict[str, Any]:
    changes = diff.get("changes", [])
    if not isinstance(changes, list):
        raise ValueError("canonical diff changes must be a list")
    by_kind = Counter(str(change.get("change_kind", "unknown")) for change in changes)
    by_entity = Counter(str(change.get("entity_kind", "unknown")) for change in changes)
    return {
        "schema_version": 1,
        "old_snapshot": diff.get("old_snapshot"),
        "new_snapshot": diff.get("new_snapshot"),
        "old_counts": {
            "classes": len(old.get("classes", {})),
            "global_functions": len(old.get("global_functions", [])),
        },
        "new_counts": {
            "classes": len(new.get("classes", {})),
            "global_functions": len(new.get("global_functions", [])),
        },
        "change_counts": {
            "total": len(changes),
            "by_change_kind": dict(sorted(by_kind.items())),
            "by_entity_kind": dict(sorted(by_entity.items())),
        },
    }


def generate_report(
    old_src_root: Path,
    old_build_id: str,
    new_src_root: Path,
    new_build_id: str,
    output_dir: Path,
) -> dict[str, Any]:
    old_src_root = old_src_root.resolve()
    new_src_root = new_src_root.resolve()
    old_build_id = _non_empty(old_build_id, "old build ID")
    new_build_id = _non_empty(new_build_id, "new build ID")
    with _reserve_output_dir(output_dir) as reserved_output:
        with tempfile.TemporaryDirectory(prefix="pzjavadocs-report-") as staging_name:
            staging = Path(staging_name)
            old_snapshot = staging / OUTPUT_NAMES[0]
            new_snapshot = staging / OUTPUT_NAMES[1]
            diff_path = staging / OUTPUT_NAMES[2]
            summary_path = staging / OUTPUT_NAMES[3]

            _run([sys.executable, str(EXTRACTOR), "--src-root", str(old_src_root), "--output", str(old_snapshot), "--build-id", old_build_id])
            _run([sys.executable, str(EXTRACTOR), "--src-root", str(new_src_root), "--output", str(new_snapshot), "--build-id", new_build_id])
            _run([sys.executable, str(COMPARATOR), str(old_snapshot), str(new_snapshot), "--old-id", old_build_id, "--new-id", new_build_id, "--out", str(diff_path)])

            summary = build_summary(_load_json(old_snapshot), _load_json(new_snapshot), _load_json(diff_path))
            summary_path.write_text(json.dumps(summary, indent=2, sort_keys=True) + "\n", encoding="utf-8")
            _publish(staging, reserved_output)
    return summary


def main() -> int:
    parser = argparse.ArgumentParser(description="Extract and compare two explicit Project Zomboid source builds.")
    parser.add_argument("--old-src-root", type=Path, required=True)
    parser.add_argument("--old-build-id", required=True)
    parser.add_argument("--new-src-root", type=Path, required=True)
    parser.add_argument("--new-build-id", required=True)
    parser.add_argument("--output-dir", type=Path, required=True)
    args = parser.parse_args()
    try:
        summary = generate_report(args.old_src_root, args.old_build_id, args.new_src_root, args.new_build_id, args.output_dir)
    except (OSError, ValueError, subprocess.CalledProcessError) as exc:
        parser.error(str(exc))
    print(f"Wrote {summary['change_counts']['total']} changes and summary to {args.output_dir.resolve()}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

#!/usr/bin/env python3
"""Exercise generated definitions through a real Lua Language Server instance."""

from __future__ import annotations

import argparse
import json
import os
import select
import subprocess
import tempfile
import time
from pathlib import Path
from typing import Any

from export_luals import export_api


def sample_api() -> dict[str, Any]:
    return {
        "classes": {
            "zombie.characters.IsoGameCharacter": {
                "simple_name": "IsoGameCharacter",
                "fields": [{"name": "baseField", "type": "String"}],
                "methods": [],
            },
            "zombie.util.IHuman": {
                "simple_name": "IHuman",
                "fields": [],
                "methods": [],
            },
            "zombie.characters.IsoPlayer": {
                "simple_name": "IsoPlayer",
                "extends": "zombie.characters.IsoGameCharacter",
                "implements": ["zombie.util.IHuman"],
                "fields": [
                    {"name": "username", "type": "String"},
                    {"name": "tags", "type": "List<String>"},
                ],
                "methods": [
                    {
                        "name": "say",
                        "return_type": "void",
                        "params": [{"name": "text", "type": "String"}],
                    },
                    {
                        "name": "find",
                        "return_type": "String",
                        "params": [{"name": "id", "type": "int"}],
                    },
                    {
                        "name": "find",
                        "return_type": "String",
                        "params": [{"name": "name", "type": "String"}],
                    },
                ],
            },
        },
        "global_functions": [
            {
                "lua_name": "getPlayer",
                "java_method": "getPlayer",
                "return_type": "zombie.characters.IsoPlayer",
                "params": [],
            }
        ],
    }


class Client:
    def __init__(self, server: Path, workspace: Path, log_dir: Path) -> None:
        self.workspace = workspace
        self.process = subprocess.Popen(
            [str(server), f"--logpath={log_dir}", "--loglevel=warn"],
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            bufsize=0,
        )
        if self.process.stdin is None or self.process.stdout is None:
            raise RuntimeError("failed to open LuaLS stdio pipes")
        self.next_id = 1

    def send(self, message: dict[str, Any]) -> None:
        raw = json.dumps(message, separators=(",", ":")).encode("utf-8")
        payload = f"Content-Length: {len(raw)}\r\n\r\n".encode("ascii") + raw
        assert self.process.stdin is not None
        self.process.stdin.write(payload)
        self.process.stdin.flush()

    def notify(self, method: str, params: dict[str, Any]) -> None:
        self.send({"jsonrpc": "2.0", "method": method, "params": params})

    def request(self, method: str, params: dict[str, Any], timeout: float = 30.0) -> Any:
        request_id = self.next_id
        self.next_id += 1
        self.send({"jsonrpc": "2.0", "id": request_id, "method": method, "params": params})
        deadline = time.monotonic() + timeout
        while True:
            message = self._read(deadline)
            if message.get("id") == request_id and "method" not in message:
                if "error" in message:
                    raise RuntimeError(f"LuaLS {method} failed: {message['error']}")
                return message.get("result")
            self._handle_server_request(message)

    def _read(self, deadline: float) -> dict[str, Any]:
        assert self.process.stdout is not None
        remaining = deadline - time.monotonic()
        if remaining <= 0:
            raise TimeoutError("timed out waiting for LuaLS")
        ready, _, _ = select.select([self.process.stdout], [], [], remaining)
        if not ready:
            raise TimeoutError("timed out waiting for LuaLS")

        headers: dict[str, str] = {}
        while True:
            line = self.process.stdout.readline()
            if not line:
                stderr = b""
                if self.process.stderr is not None:
                    stderr = self.process.stderr.read() or b""
                raise RuntimeError(
                    "LuaLS exited unexpectedly: "
                    + stderr.decode("utf-8", errors="replace")
                )
            if line in (b"\r\n", b"\n"):
                break
            key, value = line.decode("ascii").split(":", 1)
            headers[key.lower()] = value.strip()

        length = int(headers["content-length"])
        body = self.process.stdout.read(length)
        if len(body) != length:
            raise RuntimeError("truncated LuaLS protocol message")
        return json.loads(body.decode("utf-8"))

    def _handle_server_request(self, message: dict[str, Any]) -> None:
        if "method" not in message or "id" not in message:
            return
        params = message.get("params") or {}
        if message["method"] == "workspace/configuration":
            result: Any = [{} for _ in (params.get("items") or [])]
        elif message["method"] == "workspace/workspaceFolders":
            result = [{"uri": self.workspace.as_uri(), "name": self.workspace.name}]
        else:
            result = None
        self.send({"jsonrpc": "2.0", "id": message["id"], "result": result})

    def close(self) -> None:
        if self.process.poll() is not None:
            return
        try:
            self.request("shutdown", {}, timeout=5.0)
            self.notify("exit", {})
            self.process.wait(timeout=5.0)
        except Exception:
            self.process.terminate()
            try:
                self.process.wait(timeout=3.0)
            except subprocess.TimeoutExpired:
                self.process.kill()
                self.process.wait(timeout=3.0)


def completion_labels(result: Any) -> set[str]:
    if isinstance(result, dict):
        result = result.get("items", [])
    if not isinstance(result, list):
        return set()
    return {
        str(item["label"])
        for item in result
        if isinstance(item, dict) and "label" in item
    }


def definition_uris(result: Any) -> set[str]:
    if result is None:
        return set()
    entries = result if isinstance(result, list) else [result]
    uris: set[str] = set()
    for entry in entries:
        if not isinstance(entry, dict):
            continue
        for key in ("uri", "targetUri"):
            if isinstance(entry.get(key), str):
                uris.add(entry[key])
    return uris


def run(server: Path) -> None:
    if not server.is_file():
        raise FileNotFoundError(server)

    with tempfile.TemporaryDirectory(prefix="pzjavadocs-luals-") as tmp:
        root = Path(tmp)
        workspace = root / "workspace"
        library_dir = workspace / "library"
        log_dir = root / "logs"
        workspace.mkdir()
        log_dir.mkdir()

        api_path = root / "api.json"
        api_path.write_text(json.dumps(sample_api()), encoding="utf-8")
        export_api(api_path, library_dir, "luals-smoke")

        source = (
            "local player = getPlayer()\n"
            "local inherited = player.baseField\n"
            "local tags = player.tags\n"
            "local byId = player:find(1)\n"
            "local byName = player:find(\"name\")\n"
            "print(inherited, tags, byId, byName)\n"
        )
        smoke_path = workspace / "smoke.lua"
        smoke_path.write_text(source, encoding="utf-8")
        (workspace / ".luarc.json").write_text(
            json.dumps(
                {
                    "runtime.version": "Lua 5.1",
                    "workspace.checkThirdParty": False,
                }
            ),
            encoding="utf-8",
        )

        client = Client(server, workspace, log_dir)
        try:
            initialized = client.request(
                "initialize",
                {
                    "processId": os.getpid(),
                    "rootUri": workspace.as_uri(),
                    "capabilities": {
                        "workspace": {
                            "configuration": True,
                            "workspaceFolders": True,
                        }
                    },
                    "workspaceFolders": [
                        {"uri": workspace.as_uri(), "name": workspace.name}
                    ],
                },
            )
            if not isinstance(initialized, dict):
                raise AssertionError("LuaLS initialize did not return capabilities")
            client.notify("initialized", {})
            client.notify(
                "textDocument/didOpen",
                {
                    "textDocument": {
                        "uri": smoke_path.as_uri(),
                        "languageId": "lua",
                        "version": 1,
                        "text": source,
                    }
                },
            )

            expected = {"baseField", "username", "tags", "find", "say"}
            completion_line = source.splitlines()[1]
            completion_char = completion_line.index("player.") + len("player.")
            deadline = time.monotonic() + 20.0
            labels: set[str] = set()
            while time.monotonic() < deadline:
                labels = completion_labels(
                    client.request(
                        "textDocument/completion",
                        {
                            "textDocument": {"uri": smoke_path.as_uri()},
                            "position": {"line": 1, "character": completion_char},
                            "context": {"triggerKind": 1},
                        },
                    )
                )
                if expected <= labels:
                    break
                time.sleep(0.5)
            missing = expected - labels
            if missing:
                raise AssertionError(
                    f"LuaLS completion missing generated members: {sorted(missing)}; "
                    f"sample={sorted(labels)[:40]}"
                )

            definition_char = source.splitlines()[0].index("getPlayer") + 2
            definitions = client.request(
                "textDocument/definition",
                {
                    "textDocument": {"uri": smoke_path.as_uri()},
                    "position": {"line": 0, "character": definition_char},
                },
            )
            expected_uri = library_dir.joinpath("library.lua").as_uri()
            uris = definition_uris(definitions)
            if expected_uri not in uris:
                raise AssertionError(
                    f"getPlayer did not navigate to generated library: {sorted(uris)}"
                )

            print(
                "LuaLS smoke passed:",
                "completion=class+inheritance+collection+overloaded-method",
                "navigation=global-definition",
            )
        finally:
            client.close()


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--server",
        type=Path,
        required=True,
        help="Path to bin/lua-language-server",
    )
    args = parser.parse_args()
    run(args.server.resolve())
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

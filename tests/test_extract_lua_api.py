import json
import pathlib
import subprocess
import sys
import tempfile
import unittest


REPO_ROOT = pathlib.Path(__file__).resolve().parents[1]
EXTRACTOR = REPO_ROOT / "extract_lua_api.py"
COMPARATOR = REPO_ROOT / "compare_api.py"


class ExtractLuaApiTests(unittest.TestCase):
    def make_fixture(self, root: pathlib.Path) -> pathlib.Path:
        src = root / "source"
        lua_dir = src / "zombie" / "Lua"
        foo_dir = src / "zombie" / "foo"
        lua_dir.mkdir(parents=True)
        foo_dir.mkdir(parents=True)

        (lua_dir / "LuaManager.java").write_text(
            """package zombie.Lua;
import zombie.foo.Foo;
public class LuaManager {
    public void expose() { this.setExposed(Foo.class); }
    public void setExposed(Class<?> c) {}
    public static class GlobalObject {
        @LuaMethod(global=true, name=\"hello\")
        public static int hello(String value) { return 1; }
    }
}
""",
            encoding="utf-8",
        )
        (foo_dir / "Foo.java").write_text(
            """package zombie.foo;
@UsedFromLua
public class Foo {
    public int value;
    @UsedFromLua
    public String greet(int count) { return \"hi\"; }
}
""",
            encoding="utf-8",
        )
        return src

    def run_extract(self, src: pathlib.Path, output: pathlib.Path, build_id: str):
        return subprocess.run(
            [
                sys.executable,
                str(EXTRACTOR),
                "--src-root",
                str(src),
                "--output",
                str(output),
                "--build-id",
                build_id,
            ],
            cwd=REPO_ROOT,
            text=True,
            capture_output=True,
            check=False,
        )

    def test_explicit_non_windows_paths_identity_and_determinism(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = pathlib.Path(tmp)
            src = self.make_fixture(root)
            out_a = root / "a.json"
            out_b = root / "b.json"

            result_a = self.run_extract(src, out_a, "42.20-test")
            result_b = self.run_extract(src, out_b, "42.20-test")
            self.assertEqual(result_a.returncode, 0, result_a.stderr)
            self.assertEqual(result_b.returncode, 0, result_b.stderr)
            self.assertEqual(out_a.read_bytes(), out_b.read_bytes())

            payload = json.loads(out_a.read_text(encoding="utf-8"))
            self.assertEqual(payload["snapshot"]["schema_version"], 1)
            self.assertEqual(payload["snapshot"]["build_id"], "42.20-test")
            self.assertEqual(
                payload["snapshot"]["extractor"],
                {"name": "PZJavaDocs.extract_lua_api", "version": "1"},
            )
            self.assertIn("zombie.foo.Foo", payload["classes"])
            self.assertEqual(payload["_meta"]["total_classes"], 1)

            round_tripped = json.loads(json.dumps(payload, sort_keys=True))
            self.assertEqual(round_tripped["snapshot"], payload["snapshot"])

    def test_changing_only_build_id_changes_only_snapshot_identity(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = pathlib.Path(tmp)
            src = self.make_fixture(root)
            out_a = root / "a.json"
            out_b = root / "b.json"

            result_a = self.run_extract(src, out_a, "42.20-a")
            result_b = self.run_extract(src, out_b, "42.20-b")
            self.assertEqual(result_a.returncode, 0, result_a.stderr)
            self.assertEqual(result_b.returncode, 0, result_b.stderr)

            payload_a = json.loads(out_a.read_text(encoding="utf-8"))
            payload_b = json.loads(out_b.read_text(encoding="utf-8"))
            self.assertNotEqual(payload_a["snapshot"]["build_id"], payload_b["snapshot"]["build_id"])
            payload_a.pop("snapshot")
            payload_b.pop("snapshot")
            self.assertEqual(payload_a, payload_b)

    def test_generated_snapshot_is_consumable_by_existing_comparator(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = pathlib.Path(tmp)
            src = self.make_fixture(root)
            old_snapshot = root / "old.json"
            new_snapshot = root / "new.json"
            diff_path = root / "diff.json"

            old_result = self.run_extract(src, old_snapshot, "42.20-old")
            new_result = self.run_extract(src, new_snapshot, "42.20-new")
            self.assertEqual(old_result.returncode, 0, old_result.stderr)
            self.assertEqual(new_result.returncode, 0, new_result.stderr)

            compare_result = subprocess.run(
                [
                    sys.executable,
                    str(COMPARATOR),
                    str(old_snapshot),
                    str(new_snapshot),
                    "--old-id",
                    "42.20-old",
                    "--new-id",
                    "42.20-new",
                    "--out",
                    str(diff_path),
                ],
                cwd=REPO_ROOT,
                text=True,
                capture_output=True,
                check=False,
            )
            self.assertEqual(compare_result.returncode, 0, compare_result.stderr)
            diff = json.loads(diff_path.read_text(encoding="utf-8"))
            self.assertEqual(diff["old_snapshot"], "42.20-old")
            self.assertEqual(diff["new_snapshot"], "42.20-new")
            self.assertEqual(diff["changes"], [])

    def test_missing_source_root_fails_closed(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = pathlib.Path(tmp)
            result = self.run_extract(root / "missing", root / "out.json", "42.20")
            self.assertNotEqual(result.returncode, 0)
            self.assertIn("source root does not exist", result.stderr + result.stdout)

    def test_missing_lua_manager_fails_closed(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = pathlib.Path(tmp)
            src = root / "source"
            src.mkdir()
            result = self.run_extract(src, root / "out.json", "42.20")
            self.assertNotEqual(result.returncode, 0)
            self.assertIn("required LuaManager.java not found", result.stderr + result.stdout)

    def test_invalid_build_id_fails_before_extraction(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = pathlib.Path(tmp)
            src = self.make_fixture(root)
            result = self.run_extract(src, root / "out.json", "bad\nbuild")
            self.assertNotEqual(result.returncode, 0)
            self.assertIn("build ID must not contain control characters", result.stderr + result.stdout)

    def test_invalid_output_target_fails_closed(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = pathlib.Path(tmp)
            src = self.make_fixture(root)
            output_dir = root / "output-dir"
            output_dir.mkdir()
            result = self.run_extract(src, output_dir, "42.20")
            self.assertNotEqual(result.returncode, 0)
            self.assertIn("output path is a directory", result.stderr + result.stdout)


if __name__ == "__main__":
    unittest.main()

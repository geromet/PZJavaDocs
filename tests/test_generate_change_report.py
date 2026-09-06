import contextlib
import json
import pathlib
import shutil
import tempfile
import unittest
from unittest import mock

from generate_change_report import LOCK_NAME, OUTPUT_NAMES, _publish, _reserve_output_dir, generate_report


class GenerateChangeReportTests(unittest.TestCase):
    def make_source(self, root: pathlib.Path, *, field_name: str) -> pathlib.Path:
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
}
""",
            encoding="utf-8",
        )
        (foo_dir / "Foo.java").write_text(
            f"""package zombie.foo;
@UsedFromLua
public class Foo {{
    public int {field_name};
}}
""",
            encoding="utf-8",
        )
        return src

    def test_generates_reproducible_artifact_set_and_summary(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = pathlib.Path(tmp)
            old_src = self.make_source(root / "old", field_name="oldValue")
            new_src = self.make_source(root / "new", field_name="newValue")
            output = root / "report"
            output.mkdir()

            summary = generate_report(old_src, " 42.19 ", new_src, "\t42.20\n", output)

            self.assertEqual(
                sorted(path.name for path in output.iterdir() if path.name != LOCK_NAME),
                ["api-diff.json", "new-lua-api.json", "old-lua-api.json", "summary.json"],
            )
            self.assertEqual(summary["old_snapshot"], "42.19")
            self.assertEqual(summary["new_snapshot"], "42.20")
            self.assertEqual(summary["old_counts"], {"classes": 1, "global_functions": 0})
            self.assertEqual(summary["new_counts"], {"classes": 1, "global_functions": 0})
            self.assertEqual(summary["change_counts"]["total"], 2)
            self.assertEqual(summary["change_counts"]["by_change_kind"], {"added": 1, "removed": 1})
            self.assertEqual(summary["change_counts"]["by_entity_kind"], {"class": 2})
            self.assertEqual(json.loads((output / "summary.json").read_text(encoding="utf-8")), summary)
            old_snapshot = json.loads((output / "old-lua-api.json").read_text(encoding="utf-8"))
            new_snapshot = json.loads((output / "new-lua-api.json").read_text(encoding="utf-8"))
            self.assertEqual(old_snapshot["snapshot"]["build_id"], summary["old_snapshot"])
            self.assertEqual(new_snapshot["snapshot"]["build_id"], summary["new_snapshot"])

    def test_resolves_relative_source_roots_before_repository_cwd_change(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = pathlib.Path(tmp)
            self.make_source(root / "old", field_name="value")
            self.make_source(root / "new", field_name="value")
            (root / "report").mkdir()

            with contextlib.chdir(root):
                summary = generate_report(
                    pathlib.Path("old/source"), "42.19",
                    pathlib.Path("new/source"), "42.20",
                    pathlib.Path("report"),
                )
            self.assertEqual(summary["change_counts"]["total"], 0)

    def test_refuses_to_overwrite_any_existing_artifact(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = pathlib.Path(tmp)
            old_src = self.make_source(root / "old", field_name="value")
            new_src = self.make_source(root / "new", field_name="value")
            output = root / "report"
            output.mkdir()
            (output / "api-diff.json").write_text("keep", encoding="utf-8")

            with self.assertRaisesRegex(ValueError, "refusing to overwrite"):
                generate_report(old_src, "42.19", new_src, "42.20", output)
            self.assertEqual((output / "api-diff.json").read_text(encoding="utf-8"), "keep")
            self.assertEqual(sorted(path.name for path in output.iterdir()), [LOCK_NAME, "api-diff.json"])

    def test_failed_extraction_leaves_no_partial_report_artifacts(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = pathlib.Path(tmp)
            old_src = self.make_source(root / "old", field_name="value")
            missing_new_src = root / "missing"
            output = root / "report"
            output.mkdir()

            with self.assertRaises(Exception):
                generate_report(old_src, "42.19", missing_new_src, "42.20", output)
            self.assertEqual([path.name for path in output.iterdir()], [LOCK_NAME])

    def test_active_report_lock_rejects_a_concurrent_writer(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = pathlib.Path(tmp)
            old_src = self.make_source(root / "old", field_name="value")
            new_src = self.make_source(root / "new", field_name="value")
            output = root / "report"
            output.mkdir()
            with _reserve_output_dir(output):
                with self.assertRaisesRegex(ValueError, "another report run owns"):
                    generate_report(old_src, "42.19", new_src, "42.20", output)

            summary = generate_report(old_src, "42.19", new_src, "42.20", output)
            self.assertEqual(summary["change_counts"]["total"], 0)

    def test_stale_lock_file_does_not_block_a_new_run(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = pathlib.Path(tmp)
            old_src = self.make_source(root / "old", field_name="value")
            new_src = self.make_source(root / "new", field_name="value")
            output = root / "report"
            output.mkdir()
            (output / LOCK_NAME).write_text("left by a terminated process", encoding="utf-8")

            summary = generate_report(old_src, "42.19", new_src, "42.20", output)
            self.assertEqual(summary["change_counts"]["total"], 0)

    def test_publish_exposes_only_complete_destination_files(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = pathlib.Path(tmp)
            staging = root / "staging"
            output = root / "output"
            staging.mkdir()
            output.mkdir()
            expected = {}
            for index, name in enumerate(OUTPUT_NAMES):
                payload = ((json.dumps({"index": index}) + "\n") * 1000).encode()
                expected[name] = payload
                (staging / name).write_bytes(payload)

            real_copy = shutil.copyfileobj

            def copy_while_observing(source, target):
                destination = output / pathlib.Path(source.name).name
                target.write(source.read(16))
                target.flush()
                self.assertFalse(destination.exists())
                real_copy(source, target)

            with mock.patch("generate_change_report.shutil.copyfileobj", side_effect=copy_while_observing):
                _publish(staging, output)

            self.assertEqual(
                {path.name: path.read_bytes() for path in output.iterdir()},
                expected,
            )


if __name__ == "__main__":
    unittest.main()

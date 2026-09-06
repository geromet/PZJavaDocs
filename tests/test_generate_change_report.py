import json
import pathlib
import tempfile
import unittest

from generate_change_report import generate_report


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

            summary = generate_report(old_src, "42.19", new_src, "42.20", output)

            self.assertEqual(
                sorted(path.name for path in output.iterdir()),
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
            self.assertEqual([path.name for path in output.iterdir()], ["api-diff.json"])

    def test_failed_extraction_leaves_no_partial_report_artifacts(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = pathlib.Path(tmp)
            old_src = self.make_source(root / "old", field_name="value")
            missing_new_src = root / "missing"
            output = root / "report"
            output.mkdir()

            with self.assertRaises(Exception):
                generate_report(old_src, "42.19", missing_new_src, "42.20", output)
            self.assertEqual(list(output.iterdir()), [])


if __name__ == "__main__":
    unittest.main()

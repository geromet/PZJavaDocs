import json
import tempfile
import unittest
from pathlib import Path

from export_luals import build_metadata, export_api, java_type_to_luacats, render_library


def sample_api():
    return {
        "classes": {
            "zombie.characters.IsoPlayer": {
                "simple_name": "IsoPlayer",
                "extends": "zombie.characters.IsoGameCharacter",
                "implements": ["zombie.util.IHuman"],
                "fields": [
                    {"name": "username", "type": "String"},
                    {"name": "level", "type": "int"},
                ],
                "methods": [
                    {"name": "say", "return_type": "void", "params": [{"name": "text", "type": "String"}]},
                    {"name": "find", "return_type": "String", "params": [{"name": "id", "type": "int"}]},
                    {"name": "find", "return_type": "String", "params": [{"name": "name", "type": "String"}]},
                ],
            },
            "zombie.characters.IsoGameCharacter": {"simple_name": "IsoGameCharacter", "fields": [], "methods": []},
        },
        "global_functions": [
            {"lua_name": "getPlayer", "java_method": "getPlayer", "return_type": "zombie.characters.IsoPlayer", "params": []},
            {"lua_name": "getCell", "java_method": "getCell", "return_type": "Object", "params": []},
        ],
    }


class ExportLuaLSTests(unittest.TestCase):
    def test_type_mapping_covers_primitives_arrays_and_common_generics(self):
        self.assertEqual(java_type_to_luacats("int"), "integer")
        self.assertEqual(java_type_to_luacats("double"), "number")
        self.assertEqual(java_type_to_luacats("String"), "string")
        self.assertEqual(java_type_to_luacats("boolean"), "boolean")
        self.assertEqual(java_type_to_luacats("String[]"), "string[]")
        self.assertEqual(java_type_to_luacats("List<String>"), "string[]")
        self.assertEqual(java_type_to_luacats("Map<String, Integer>"), "table<string, integer>")
        self.assertEqual(java_type_to_luacats("Optional<String>"), "string|nil")

    def test_library_contains_namespaced_classes_inheritance_fields_methods_and_globals(self):
        output = render_library(sample_api(), "42.20.4")
        self.assertIn("---@meta _", output)
        self.assertIn("-- Project Zomboid API build: 42.20.4", output)
        self.assertIn("---@class zombie.characters.IsoPlayer: zombie.characters.IsoGameCharacter, zombie.util.IHuman", output)
        self.assertIn("---@field level integer", output)
        self.assertIn("---@field username string", output)
        self.assertIn("---@param text string", output)
        self.assertIn("function getPlayer() end", output)
        self.assertEqual(output.count(":find("), 2)

    def test_render_is_deterministic_for_differently_ordered_input(self):
        first = sample_api()
        second = {"global_functions": list(reversed(first["global_functions"])), "classes": dict(reversed(list(first["classes"].items())))}
        for entry in second["classes"].values():
            entry["methods"] = list(reversed(entry.get("methods", [])))
            entry["fields"] = list(reversed(entry.get("fields", [])))
        self.assertEqual(render_library(first, "42.20.4"), render_library(second, "42.20.4"))

    def test_class_receivers_are_lexically_scoped_beyond_lua_local_limit(self):
        api = {
            "classes": {
                f"pkg.C{i}": {"simple_name": f"C{i}", "fields": [], "methods": []}
                for i in range(205)
            },
            "global_functions": [],
        }
        output = render_library(api, "test-build")
        self.assertEqual(output.count("\ndo\n---@class pkg.C"), 205)
        self.assertEqual(output.count("\nlocal _pz_"), 205)
        self.assertEqual(output.count("\nend\n"), 205)

    def test_export_writes_stable_library_and_metadata(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            input_path = root / "api.json"
            output_dir = root / "luals"
            input_path.write_text(json.dumps(sample_api()), encoding="utf-8")
            export_api(input_path, output_dir, "42.20.4")
            library = (output_dir / "library.lua").read_text(encoding="utf-8")
            metadata = json.loads((output_dir / "metadata.json").read_text(encoding="utf-8"))
            self.assertIn("---@class zombie.characters.IsoPlayer", library)
            self.assertEqual(metadata, build_metadata(sample_api(), "42.20.4"))
            self.assertEqual(metadata["project_zomboid_build"], "42.20.4")
            self.assertEqual(metadata["class_count"], 2)

    def test_invalid_build_identity_is_rejected(self):
        with self.assertRaises(ValueError):
            render_library(sample_api(), "")
        with self.assertRaises(ValueError):
            render_library(sample_api(), "42.20\nspoofed")

    def test_generated_code_escapes_unusual_names(self):
        api = {
            "classes": {
                "zombie.bad-name.Class": {
                    "fields": [{"name": "end", "type": "String"}],
                    "methods": [{"name": "end", "return_type": "void", "params": [{"name": "end", "type": "String"}]}],
                }
            },
            "global_functions": [{"lua_name": "bad-name", "return_type": "void", "params": []}],
        }
        output = render_library(api, "test-build")
        self.assertIn("---@class zombie.bad_name.Class", output)
        self.assertIn('---@field ["end"] string', output)
        self.assertIn('["end"] = function(_end) end', output)
        self.assertIn('_G["bad-name"] = function() end', output)


if __name__ == "__main__":
    unittest.main()

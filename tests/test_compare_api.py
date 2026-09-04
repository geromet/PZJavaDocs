import json
import unittest

from compare_api import compare_snapshots, serialize_diff


def cls(*, extends=None, implements=(), fields=(), methods=(), set_exposed=True, lua_tagged=False):
    value = {
        "set_exposed": set_exposed,
        "lua_tagged": lua_tagged,
        "fields": list(fields),
        "methods": list(methods),
    }
    if extends is not None:
        value["extends"] = extends
    if implements:
        value["implements"] = list(implements)
    return value


def method(name, params=(), return_type="void", lua_tagged=False):
    return {
        "name": name,
        "params": [{"type": type_name, "name": f"p{i}"} for i, type_name in enumerate(params)],
        "return_type": return_type,
        "lua_tagged": lua_tagged,
    }


class CompareApiTests(unittest.TestCase):
    def compare(self, old, new):
        return compare_snapshots(old, new, "42.19", "42.20")["changes"]

    def test_class_add_remove_and_inheritance_change(self):
        old = {"classes": {
            "z.Old": cls(),
            "z.Keep": cls(extends="z.BaseA", implements=["z.I2", "z.I1"]),
        }}
        new = {"classes": {
            "z.New": cls(),
            "z.Keep": cls(extends="z.BaseB", implements=["z.I1", "z.I2"]),
        }}

        changes = self.compare(old, new)

        self.assertEqual(
            [(c["entity"], c["change_kind"]) for c in changes],
            [("z.Keep", "changed"), ("z.New", "added"), ("z.Old", "removed")],
        )
        self.assertEqual(changes[0]["before"]["extends"], "z.BaseA")
        self.assertEqual(changes[0]["after"]["extends"], "z.BaseB")

    def test_field_type_and_method_return_changes_are_changed(self):
        old = {"classes": {"z.C": cls(
            fields=[{"name": "count", "type": "int", "lua_tagged": False}],
            methods=[method("get", ["String"], "int")],
        )}}
        new = {"classes": {"z.C": cls(
            fields=[{"name": "count", "type": "long", "lua_tagged": False}],
            methods=[method("get", ["String"], "long")],
        )}}

        changes = self.compare(old, new)

        self.assertEqual(
            [(c["member_kind"], c["change_kind"]) for c in changes],
            [("field", "changed"), ("method", "changed")],
        )

    def test_unique_parameter_signature_change_is_reported_as_changed(self):
        old = {"classes": {"z.C": cls(methods=[method("set", ["int"])])}}
        new = {"classes": {"z.C": cls(methods=[method("set", ["long"])])}}

        [change] = self.compare(old, new)

        self.assertEqual(change["member_kind"], "method")
        self.assertEqual(change["change_kind"], "changed")
        self.assertEqual(change["before"]["params"][0]["type"], "int")
        self.assertEqual(change["after"]["params"][0]["type"], "long")

    def test_added_overload_does_not_remove_existing_overload(self):
        old = {"classes": {"z.C": cls(methods=[method("find", ["String"])])}}
        new = {"classes": {"z.C": cls(methods=[
            method("find", ["String"]),
            method("find", ["String", "int"]),
        ])}}

        changes = self.compare(old, new)

        self.assertEqual(len(changes), 1)
        self.assertEqual(changes[0]["change_kind"], "added")
        self.assertEqual(changes[0]["member"], "find(String, int)")

    def test_globals_add_remove_and_change(self):
        old = {"global_functions": [
            {"lua_name": "gone", "java_method": "gone", "return_type": "void", "params": []},
            {"lua_name": "changed", "java_method": "changed", "return_type": "int", "params": []},
        ]}
        new = {"global_functions": [
            {"lua_name": "added", "java_method": "added", "return_type": "void", "params": []},
            {"lua_name": "changed", "java_method": "changed", "return_type": "long", "params": []},
        ]}

        changes = self.compare(old, new)

        self.assertEqual(
            [(c["entity"], c["change_kind"]) for c in changes],
            [("added", "added"), ("changed", "changed"), ("gone", "removed")],
        )

    def test_unchanged_snapshot_is_empty_and_serialization_is_byte_stable(self):
        snapshot = {
            "classes": {"z.C": cls(
                implements=["z.B", "z.A"],
                methods=[method("b"), method("a")],
            )},
            "global_functions": [],
        }

        first = compare_snapshots(snapshot, snapshot, "42.20", "42.20")
        second = compare_snapshots(
            json.loads(json.dumps(snapshot, sort_keys=False)),
            json.loads(json.dumps(snapshot, sort_keys=True)),
            "42.20",
            "42.20",
        )

        self.assertEqual(first["changes"], [])
        self.assertEqual(serialize_diff(first), serialize_diff(second))

    def test_snapshot_identity_is_required(self):
        with self.assertRaises(ValueError):
            compare_snapshots({}, {}, "", "42.20")


if __name__ == "__main__":
    unittest.main()

"""
Adds a 'group' field to each global_function entry in lua_api.json.
Groups were assigned by semantic analysis of the function names in order.
Re-run this after regenerating lua_api.json if the function list changes.
"""
import json, pathlib

VIEWER_DIR = pathlib.Path(__file__).parent
API_FILE   = VIEWER_DIR / "lua_api.json"

# (start_index, end_index_inclusive, group_name)
GROUPS = [
    (0,   5,   "3D Model Loading"),
    (6,   9,   "Audio & Radio"),
    (10,  21,  "Miscellaneous Utilities"),
    (22,  38,  "Multiplayer Connection"),
    (39,  45,  "Animals & Hit Events"),
    (46,  61,  "User & Role Management"),
    (62,  77,  "Animal Hutch & Transport"),
    (78,  85,  "Saves, Items & Logs"),
    (86,  91,  "Coordinate Conversion"),
    (92,  112, "Players, Sound & World"),
    (113, 127, "Server Config & Runtime"),
    (128, 135, "Debug Tools"),
    (136, 146, "Player Data Sync"),
    (147, 158, "Server / Client State"),
    (159, 172, "Server List & Accounts"),
    (173, 185, "Game Config & Saves"),
    (186, 192, "Admin: Bans & Tickets"),
    (193, 203, "Factions & Safehouses"),
    (204, 207, "Zombie & Horde Spawning"),
    (208, 212, "Event System"),
    (213, 226, "Mod & File System"),
    (227, 230, "File Checks & Screenshots"),
    (231, 237, "Item Scripting"),
    (238, 244, "Lua Runtime"),
    (245, 251, "World & Map Dimensions"),
    (252, 263, "Sandbox & File I/O"),
    (264, 267, "Fire & Player Database"),
    (268, 280, "Controller Settings"),
    (281, 317, "Joypad & Gamepad Input"),
    (318, 336, "File I/O"),
    (337, 350, "Keyboard Input & Sound"),
    (351, 365, "Game Config & System Info"),
    (366, 373, "Mod Management"),
    (374, 384, "Lua Stack Introspection"),
    (385, 395, "Reflection & Map Utilities"),
    (396, 413, "Debugger & Lua Inspector"),
    (414, 417, "Game Speed & Pause"),
    (418, 424, "Mouse Input"),
    (425, 431, "Save Info"),
    (432, 436, "World Info Queries"),
    (437, 444, "Textures & Authentication"),
    (445, 460, "Text & Localization"),
    (461, 478, "Item Information"),
    (479, 481, "Sprites"),
    (482, 484, "Mod Data & Controller Type"),
    (485, 500, "Client/Server Commands"),
    (501, 513, "Game Client & Player Management"),
    (514, 527, "Zones, Map Bounds & Timestamps"),
    (528, 554, "Steam & Social"),
    (555, 565, "Trading & String Utilities"),
    (566, 576, "Steam Server Browser"),
    (577, 590, "Debug Rendering & Weather"),
    (591, 610, "Vehicles, Blood & Zombie Spawning"),
    (611, 618, "Input Events & Vehicle Placement"),
    (619, 634, "Debug Editor Views"),
    (635, 643, "Entity & Script Reload"),
    (644, 654, "Chat Processing"),
    (655, 661, "Environment & Climate"),
    (662, 671, "Character Customization"),
    (672, 686, "Sound, XP & Player Stats"),
    (687, 697, "Rendering & Performance"),
    (698, 709, "Item Transactions & Actions"),
    (710, 728, "Anim Events, Sync & Containers"),
    (729, 744, "Teleport, Debug & Timers"),
]

# Rename entire groups after range assignment.
# None = dissolve (functions must be covered by RENAMES below).
GROUP_RENAMES = {
    "Audio & Radio":              "Radio & Broadcast",
    "Players, Sound & World":     "Player Management & World",
    "Keyboard Input & Sound":     "Keyboard Input",
    "Sound, XP & Player Stats":   "XP & Player Stats",
    "Sprites":                    "Sprite Manager",
    "Mod Data & Controller Type": None,   # all 3 functions covered in RENAMES
    "Anim Events, Sync & Containers": "Actions, Sync & Containers",
}

# Override group for specific functions by lua_name.
# Applied after GROUP_RENAMES — takes highest priority.
RENAMES = {
    # Sound functions scattered across multiple groups → unified "Sound & Audio"
    "getSLSoundManager":      "Sound & Audio",
    "getAmbientStreamManager":"Sound & Audio",
    "playServerSound":        "Sound & Audio",
    "getWorldSoundManager":   "Sound & Audio",
    "AddWorldSound":          "Sound & Audio",
    "AddNoiseToken":          "Sound & Audio",
    "pauseSoundAndMusic":     "Sound & Audio",
    "resumeSoundAndMusic":    "Sound & Audio",
    "getBaseSoundBank":       "Sound & Audio",
    "getFMODSoundBank":       "Sound & Audio",
    "isSoundPlaying":         "Sound & Audio",
    "stopSound":              "Sound & Audio",
    "getSoundManager":        "Sound & Audio",
    "testSound":              "Sound & Audio",
    "getFMODEventPathList":   "Sound & Audio",
    "reloadSoundFiles":       "Sound & Audio",
    "addSound":               "Sound & Audio",
    "sendPlaySound":          "Sound & Audio",
    # Zoom belongs with rendering
    "screenZoomIn":           "Rendering & Performance",
    "screenZoomOut":          "Rendering & Performance",
    # Radio belongs with radio
    "getZomboidRadio":        "Radio & Broadcast",
    # File I/O
    "getMyDocumentFolder":    "Sandbox & File I/O",
    # Dissolved "Mod Data & Controller Type" (3 functions)
    "isXBOXController":       "Controller Settings",
    "isPlaystationController":"Controller Settings",
    "getServerModData":       "Mod Management",
    # Misc fixes
    "checkStringPattern":     "Trading & String Utilities",
    "doKeyPress":             "Keyboard Input",
    "detectBadWords":         "Chat Processing",
    "profanityFilterCheck":   "Chat Processing",
    "showDebugInfoInChat":    "Chat Processing",
    "querySteamWorkshopItemDetails": "Steam & Social",
}

def get_group(idx):
    for start, end, name in GROUPS:
        if start <= idx <= end:
            return name
    return "Other"

api = json.loads(API_FILE.read_text(encoding="utf-8"))
fns = api["global_functions"]
print(f"Patching {len(fns)} global functions...")
for i, fn in enumerate(fns):
    fn["group"] = get_group(i)

# Apply group-level renames
for fn in fns:
    new = GROUP_RENAMES.get(fn["group"])
    if new is not None:
        fn["group"] = new
    elif fn["group"] in GROUP_RENAMES:  # dissolved group, leave for RENAMES
        fn["group"] = fn["group"]

# Apply per-function overrides (highest priority)
for fn in fns:
    if fn["lua_name"] in RENAMES:
        fn["group"] = RENAMES[fn["lua_name"]]

# Verify full coverage
ungrouped = [i for i in range(len(fns)) if fns[i]["group"] == "Other"]
if ungrouped:
    print(f"WARNING: {len(ungrouped)} functions have no group: indices {ungrouped[:10]}")
else:
    groups_used = sorted(set(f["group"] for f in fns))
    print(f"OK: {len(groups_used)} groups assigned")

API_FILE.write_text(json.dumps(api, indent=2), encoding="utf-8")
print("Done.")

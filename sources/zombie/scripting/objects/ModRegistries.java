/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

import java.io.File;
import java.util.ArrayList;
import zombie.Lua.LuaManager;
import zombie.ZomboidFileSystem;
import zombie.gameStates.ChooseGameInfo;

public final class ModRegistries {
    public static void init() {
        ArrayList<String> modIds = ZomboidFileSystem.instance.getModIDs();
        for (String modId : modIds) {
            ChooseGameInfo.Mod mod = ChooseGameInfo.getAvailableModDetails(modId);
            if (mod == null) continue;
            File file = new File(mod.getVersionDir() + "/media/registries.lua");
            if (file.exists() && !file.isDirectory()) {
                LuaManager.RunLua(file.getAbsolutePath());
                continue;
            }
            file = new File(mod.getCommonDir() + "/media/registries.lua");
            if (!file.exists() || file.isDirectory()) continue;
            LuaManager.RunLua(file.getAbsolutePath());
        }
    }
}


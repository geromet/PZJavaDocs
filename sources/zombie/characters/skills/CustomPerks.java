/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters.skills;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import se.krka.kahlua.vm.KahluaTable;
import zombie.Lua.LuaManager;
import zombie.ZomboidFileSystem;
import zombie.characters.skills.CustomPerk;
import zombie.characters.skills.PerkFactory;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
import zombie.gameStates.ChooseGameInfo;
import zombie.scripting.ScriptParser;
import zombie.util.StringUtils;

public final class CustomPerks {
    private static final int VERSION1 = 1;
    private static final int VERSION = 1;
    public static final CustomPerks instance = new CustomPerks();
    private final ArrayList<CustomPerk> perks = new ArrayList();

    public void init() {
        PerkFactory.Perk perk;
        ArrayList<String> modIDs = ZomboidFileSystem.instance.getModIDs();
        for (int i = 0; i < modIDs.size(); ++i) {
            String modID = modIDs.get(i);
            ChooseGameInfo.Mod mod = ChooseGameInfo.getAvailableModDetails(modID);
            if (mod == null) continue;
            File file = new File(mod.getVersionDir() + File.separator + "media" + File.separator + "perks.txt");
            if (file.exists()) {
                this.readFile(file.getAbsolutePath());
                continue;
            }
            file = new File(mod.getCommonDir() + File.separator + "media" + File.separator + "perks.txt");
            if (!file.exists()) continue;
            this.readFile(file.getAbsolutePath());
        }
        for (CustomPerk customPerk : this.perks) {
            perk = PerkFactory.Perks.FromString(customPerk.id);
            if (perk != null && perk != PerkFactory.Perks.None && perk != PerkFactory.Perks.MAX) continue;
            perk = new PerkFactory.Perk(customPerk.id);
            perk.setCustom();
        }
        for (CustomPerk customPerk : this.perks) {
            perk = PerkFactory.Perks.FromString(customPerk.id);
            PerkFactory.Perk parent = PerkFactory.Perks.FromString(customPerk.parent);
            if (parent == null || parent == PerkFactory.Perks.None || parent == PerkFactory.Perks.MAX) {
                parent = PerkFactory.Perks.None;
            }
            int[] xp = customPerk.xp;
            PerkFactory.AddPerk(perk, customPerk.translation, parent, xp[0], xp[1], xp[2], xp[3], xp[4], xp[5], xp[6], xp[7], xp[8], xp[9], customPerk.passive);
        }
    }

    public void initLua() {
        KahluaTable perks = (KahluaTable)LuaManager.env.rawget("Perks");
        for (CustomPerk customPerk : this.perks) {
            PerkFactory.Perk perk = PerkFactory.Perks.FromString(customPerk.id);
            perks.rawset(perk.getId(), (Object)perk);
        }
    }

    public static void Reset() {
        CustomPerks.instance.perks.clear();
    }

    /*
     * Enabled aggressive exception aggregation
     */
    private boolean readFile(String path) {
        try (FileReader fr = new FileReader(path);){
            boolean bl;
            try (BufferedReader br = new BufferedReader(fr);){
                StringBuilder stringBuilder = new StringBuilder();
                String str = br.readLine();
                while (str != null) {
                    stringBuilder.append(str);
                    str = br.readLine();
                }
                this.parse(stringBuilder.toString());
                bl = true;
            }
            return bl;
        }
        catch (FileNotFoundException ex) {
            return false;
        }
        catch (Exception ex) {
            ExceptionLogger.logException(ex);
            return false;
        }
    }

    private void parse(String contents) {
        contents = ScriptParser.stripComments(contents);
        ScriptParser.Block block = ScriptParser.parse(contents);
        int version = -1;
        ScriptParser.Value value = block.getValue("VERSION");
        if (value != null) {
            version = PZMath.tryParseInt(value.getValue(), -1);
        }
        if (version < 1 || version > 1) {
            throw new RuntimeException("invalid or missing VERSION");
        }
        for (ScriptParser.Block block1 : block.children) {
            if (!block1.type.equalsIgnoreCase("perk")) {
                throw new RuntimeException("unknown block type \"" + block1.type + "\"");
            }
            CustomPerk option = this.parsePerk(block1);
            if (option == null) {
                DebugLog.General.warn("failed to parse custom perk \"%s\"", block1.id);
                continue;
            }
            this.perks.add(option);
        }
    }

    private CustomPerk parsePerk(ScriptParser.Block block) {
        ScriptParser.Value vPassive;
        ScriptParser.Value vTranslation;
        if (StringUtils.isNullOrWhitespace(block.id)) {
            DebugLog.General.warn("missing or empty perk id");
            return null;
        }
        CustomPerk customPerk = new CustomPerk(block.id);
        ScriptParser.Value vParent = block.getValue("parent");
        if (vParent != null && !StringUtils.isNullOrWhitespace(vParent.getValue())) {
            customPerk.parent = vParent.getValue().trim();
        }
        if ((vTranslation = block.getValue("translation")) != null) {
            customPerk.translation = StringUtils.discardNullOrWhitespace(vTranslation.getValue().trim());
        }
        if (StringUtils.isNullOrWhitespace(customPerk.translation)) {
            customPerk.translation = customPerk.id;
        }
        if ((vPassive = block.getValue("passive")) != null) {
            customPerk.passive = StringUtils.tryParseBoolean(vPassive.getValue().trim());
        }
        for (int i = 1; i <= 10; ++i) {
            int xp;
            ScriptParser.Value vXP = block.getValue("xp" + i);
            if (vXP == null || (xp = PZMath.tryParseInt(vXP.getValue().trim(), -1)) <= 0) continue;
            customPerk.xp[i - 1] = xp;
        }
        return customPerk;
    }
}


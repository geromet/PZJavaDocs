/*
 * Decompiled with CFR 0.152.
 */
package zombie.modding;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.modding.ActiveMods;
import zombie.scripting.ScriptParser;
import zombie.util.StringUtils;

public final class ActiveModsFile {
    private static final int VERSION1 = 1;
    private static final int VERSION = 1;

    public boolean write(String path, ActiveMods activeMods) {
        if (Core.getInstance().isNoSave()) {
            return false;
        }
        File file = new File(path);
        try (FileWriter fr = new FileWriter(file);
             BufferedWriter br = new BufferedWriter(fr);){
            String contents = this.toString(activeMods);
            br.write(contents);
        }
        catch (Exception ex) {
            ExceptionLogger.logException(ex);
            return false;
        }
        return true;
    }

    private String toString(ActiveMods activeMods) {
        ScriptParser.Block block = new ScriptParser.Block();
        block.setValue("VERSION", String.valueOf(1));
        ScriptParser.Block blockMods = block.addBlock("mods", null);
        ArrayList<String> mods = activeMods.getMods();
        for (int i = 0; i < mods.size(); ++i) {
            blockMods.addValue("mod", mods.get(i));
        }
        ScriptParser.Block blockMaps = block.addBlock("maps", null);
        ArrayList<String> maps = activeMods.getMapOrder();
        for (int i = 0; i < maps.size(); ++i) {
            blockMaps.addValue("map", maps.get(i));
        }
        StringBuilder stringBuilder = new StringBuilder();
        String eol = System.lineSeparator();
        block.prettyPrintElements(0, stringBuilder, eol, "    ");
        return stringBuilder.toString();
    }

    public boolean read(String path, ActiveMods activeMods) {
        activeMods.clear();
        try (FileReader fr = new FileReader(path);
             BufferedReader br = new BufferedReader(fr);){
            StringBuilder stringBuilder = new StringBuilder();
            String str = br.readLine();
            while (str != null) {
                stringBuilder.append(str);
                str = br.readLine();
            }
            this.fromString(stringBuilder.toString(), activeMods);
        }
        catch (FileNotFoundException ex) {
            return false;
        }
        catch (Exception ex) {
            ExceptionLogger.logException(ex);
            return false;
        }
        return true;
    }

    private void fromString(String contents, ActiveMods activeMods) {
        ScriptParser.Block blockMaps;
        contents = ScriptParser.stripComments(contents);
        ScriptParser.Block block = ScriptParser.parse(contents);
        int version = -1;
        ScriptParser.Value value = block.getValue("VERSION");
        if (value != null) {
            version = PZMath.tryParseInt(value.getValue(), -1);
        }
        if (version < 1 || version > 1) {
            return;
        }
        ScriptParser.Block blockMods = block.getBlock("mods", null);
        if (blockMods != null) {
            for (ScriptParser.Value value1 : blockMods.values) {
                String modID;
                String key = value1.getKey().trim();
                if (!key.equalsIgnoreCase("mod") || StringUtils.isNullOrWhitespace(modID = value1.getValue().trim().replace("\\", ""))) continue;
                activeMods.getMods().add(modID);
            }
        }
        if ((blockMaps = block.getBlock("maps", null)) != null) {
            for (ScriptParser.Value value1 : blockMaps.values) {
                String map;
                String key = value1.getKey().trim();
                if (!key.equalsIgnoreCase("map") || StringUtils.isNullOrWhitespace(map = value1.getValue().trim())) continue;
                activeMods.getMapOrder().add(map);
            }
        }
    }
}


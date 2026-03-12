/*
 * Decompiled with CFR 0.152.
 */
package zombie.sandbox;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import zombie.SandboxOptions;
import zombie.ZomboidFileSystem;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
import zombie.gameStates.ChooseGameInfo;
import zombie.sandbox.CustomBooleanSandboxOption;
import zombie.sandbox.CustomDoubleSandboxOption;
import zombie.sandbox.CustomEnumSandboxOption;
import zombie.sandbox.CustomIntegerSandboxOption;
import zombie.sandbox.CustomSandboxOption;
import zombie.sandbox.CustomStringSandboxOption;
import zombie.scripting.ScriptParser;
import zombie.util.StringUtils;

public final class CustomSandboxOptions {
    private static final int VERSION1 = 1;
    private static final int VERSION = 1;
    public static final CustomSandboxOptions instance = new CustomSandboxOptions();
    private final ArrayList<CustomSandboxOption> options = new ArrayList();

    public void init() {
        ArrayList<String> modIDs = ZomboidFileSystem.instance.getModIDs();
        for (int i = 0; i < modIDs.size(); ++i) {
            String modID = modIDs.get(i);
            ChooseGameInfo.Mod mod = ChooseGameInfo.getAvailableModDetails(modID);
            if (mod == null) continue;
            File file = new File(mod.getVersionDir() + File.separator + "media" + File.separator + "sandbox-options.txt");
            if (file.exists() && !file.isDirectory()) {
                this.readFile(file.getAbsolutePath());
                continue;
            }
            file = new File(mod.getCommonDir() + File.separator + "media" + File.separator + "sandbox-options.txt");
            if (!file.exists() || file.isDirectory()) continue;
            this.readFile(file.getAbsolutePath());
        }
    }

    public static void Reset() {
        CustomSandboxOptions.instance.options.clear();
    }

    public void initInstance(SandboxOptions options) {
        for (int i = 0; i < this.options.size(); ++i) {
            CustomSandboxOption option = this.options.get(i);
            options.newCustomOption(option);
        }
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
            if (!block1.type.equalsIgnoreCase("option")) {
                throw new RuntimeException("unknown block type \"" + block1.type + "\"");
            }
            CustomSandboxOption option = this.parseOption(block1);
            if (option == null) {
                DebugLog.General.warn("failed to parse custom sandbox option \"%s\"", block1.id);
                continue;
            }
            this.options.add(option);
        }
    }

    private CustomSandboxOption parseOption(ScriptParser.Block block) {
        if (StringUtils.isNullOrWhitespace(block.id)) {
            DebugLog.General.warn("missing or empty option id");
            return null;
        }
        ScriptParser.Value type = block.getValue("type");
        if (type == null || StringUtils.isNullOrWhitespace(type.getValue())) {
            DebugLog.General.warn("missing or empty value \"type\"");
            return null;
        }
        switch (type.getValue().trim()) {
            case "boolean": {
                return CustomBooleanSandboxOption.parse(block);
            }
            case "double": {
                return CustomDoubleSandboxOption.parse(block);
            }
            case "enum": {
                return CustomEnumSandboxOption.parse(block);
            }
            case "integer": {
                return CustomIntegerSandboxOption.parse(block);
            }
            case "string": {
                return CustomStringSandboxOption.parse(block);
            }
        }
        DebugLog.General.warn("unknown option type \"%s\"", type.getValue().trim());
        return null;
    }
}


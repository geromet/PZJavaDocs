/*
 * Decompiled with CFR 0.152.
 */
package zombie.ui;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
import zombie.scripting.ScriptParser;
import zombie.ui.FontsFileFont;
import zombie.util.StringUtils;

public final class FontsFile {
    private static final int VERSION1 = 1;
    private static final int VERSION = 1;

    /*
     * Enabled aggressive exception aggregation
     */
    public boolean read(String path, HashMap<String, FontsFileFont> fonts) {
        try (FileReader fr = new FileReader(path);){
            boolean bl;
            try (BufferedReader br = new BufferedReader(fr);){
                StringBuilder stringBuilder = new StringBuilder();
                String str = br.readLine();
                while (str != null) {
                    stringBuilder.append(str);
                    str = br.readLine();
                }
                this.fromString(stringBuilder.toString(), fonts);
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

    private void fromString(String contents, HashMap<String, FontsFileFont> fonts) {
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
            if (!block1.type.equalsIgnoreCase("font")) {
                throw new RuntimeException("unknown block type \"" + block1.type + "\"");
            }
            if (StringUtils.isNullOrWhitespace(block1.id)) {
                DebugLog.General.warn("missing or empty font id");
                continue;
            }
            ScriptParser.Value fnt = block1.getValue("fnt");
            ScriptParser.Value img = block1.getValue("img");
            ScriptParser.Value type = block1.getValue("type");
            if (fnt == null || StringUtils.isNullOrWhitespace(fnt.getValue())) {
                DebugLog.General.warn("missing or empty value \"fnt\"");
                continue;
            }
            FontsFileFont font = new FontsFileFont();
            font.id = block1.id;
            font.fnt = fnt.getValue().trim();
            if (img != null && !StringUtils.isNullOrWhitespace(img.getValue())) {
                font.img = img.getValue().trim();
            }
            if (type != null) {
                font.type = StringUtils.discardNullOrWhitespace(type.getValue().trim());
            }
            fonts.put(font.id, font);
        }
    }
}


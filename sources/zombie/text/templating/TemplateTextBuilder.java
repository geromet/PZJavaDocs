/*
 * Decompiled with CFR 0.152.
 */
package zombie.text.templating;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import se.krka.kahlua.j2se.KahluaTableImpl;
import zombie.debug.DebugLog;
import zombie.text.templating.IReplace;
import zombie.text.templating.IReplaceProvider;
import zombie.text.templating.ITemplateBuilder;
import zombie.text.templating.ReplaceList;
import zombie.text.templating.ReplaceProviderLua;

public class TemplateTextBuilder
implements ITemplateBuilder {
    private static final String fieldStart = "\\$\\{";
    private static final String fieldEnd = "\\}";
    private static final String regex = "\\$\\{([^}]+)\\}";
    private static final Pattern pattern = Pattern.compile("\\$\\{([^}]+)\\}");
    private final Map<String, IReplace> keys = new HashMap<String, IReplace>();

    protected TemplateTextBuilder() {
    }

    @Override
    public void Reset() {
        this.keys.clear();
    }

    @Override
    public String Build(String input) {
        return this.format(input, null);
    }

    @Override
    public String Build(String input, IReplaceProvider replaceProvider) {
        return this.format(input, replaceProvider);
    }

    @Override
    public String Build(String input, KahluaTableImpl table) {
        ReplaceProviderLua replaceProvider = ReplaceProviderLua.Alloc();
        replaceProvider.fromLuaTable(table);
        String output = this.format(input, replaceProvider);
        replaceProvider.release();
        return output;
    }

    private String format(String format, IReplaceProvider replaceProvider) {
        Matcher m = pattern.matcher(format);
        String result = format;
        while (m.find()) {
            String found = m.group(1).toLowerCase().trim();
            Object replacement = null;
            if (replaceProvider != null && replaceProvider.hasReplacer(found)) {
                replacement = replaceProvider.getReplacer(found).getString();
            } else {
                IReplace replace = this.keys.get(found);
                if (replace != null) {
                    replacement = replace.getString();
                }
            }
            if (replacement == null) {
                replacement = "missing_" + found;
            }
            result = result.replaceFirst(regex, (String)replacement);
        }
        return result;
    }

    @Override
    public void RegisterKey(String key, KahluaTableImpl table) {
        try {
            ArrayList<String> list = new ArrayList<String>();
            for (int i = 1; i < table.len() + 1; ++i) {
                list.add((String)table.rawget(i));
            }
            if (!list.isEmpty()) {
                this.localRegisterKey(key, new ReplaceList(list));
            } else {
                DebugLog.log("TemplateTextBuilder -> key '" + key + "' contains no entries, ignoring.");
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void RegisterKey(String key, IReplace replace) {
        this.localRegisterKey(key, replace);
    }

    private void localRegisterKey(String key, IReplace replace) {
        if (this.keys.containsKey(key.toLowerCase().trim())) {
            DebugLog.log("TemplateTextBuilder -> Warning: key '" + key + "' replaces an existing key.");
        }
        this.keys.put(key.toLowerCase().trim(), replace);
    }

    @Override
    public void CopyFrom(Object other) {
        if (!(other instanceof TemplateTextBuilder)) {
            DebugLog.log("TemplateTextBuilder -> Warning: CopyFrom other not instance of TemplateTextBuilder.");
            return;
        }
        TemplateTextBuilder templateTextBuilder = (TemplateTextBuilder)other;
        this.keys.putAll(templateTextBuilder.keys);
    }
}


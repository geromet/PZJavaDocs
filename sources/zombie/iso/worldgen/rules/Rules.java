/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.worldgen.rules;

import com.google.common.collect.Lists;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import zombie.ZomboidFileSystem;
import zombie.iso.worldgen.rules.Alias;
import zombie.iso.worldgen.rules.Rule;

public class Rules {
    private int version;
    private Map<String, Alias> aliases;
    private Map<String, Rule> rules;
    private Map<String, int[]> colors;
    private Map<String, Integer> colorsInt;

    public static Rules load(String filename) {
        File file = ZomboidFileSystem.instance.getMediaFile(filename);
        if (!file.exists()) {
            return null;
        }
        Rules rules = new Rules();
        rules.aliases = new HashMap<String, Alias>();
        rules.rules = new HashMap<String, Rule>();
        rules.colors = new HashMap<String, int[]>();
        rules.colorsInt = new HashMap<String, Integer>();
        try {
            String line;
            BufferedReader br = new BufferedReader(new FileReader(file));
            while ((line = br.readLine()) != null) {
                if ((line = line.strip()).isEmpty()) continue;
                String[] split = line.split("\\h+");
                switch (split[0]) {
                    case "version": {
                        rules.version = Integer.parseInt(split[2]);
                        break;
                    }
                    case "alias": {
                        Alias alias = Alias.load(br, split);
                        rules.aliases.put(alias.name(), alias);
                        break;
                    }
                    case "rule": {
                        Rule rule = Rule.load(br, split);
                        rules.rules.put(rule.label(), rule);
                    }
                }
            }
            br.close();
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        List<Rule> rulesNoCond = rules.rules.values().stream().filter(k -> k.condition()[0] == -1).toList();
        for (Rule rule : rulesNoCond) {
            for (String tile : rule.tiles()) {
                List<String> subTiles = rules.aliases.containsKey(tile) ? rules.aliases.get(tile).tiles() : Lists.newArrayList(tile);
                for (String subTile : subTiles) {
                    int[] color = rule.color();
                    rules.colors.put(subTile, color);
                    rules.colorsInt.put(subTile, color[0] << 16 | color[1] << 8 | color[2]);
                }
            }
        }
        return rules;
    }

    public int getVersion() {
        return this.version;
    }

    public Map<String, Alias> getAliases() {
        return this.aliases;
    }

    public Map<String, Rule> getRules() {
        return this.rules;
    }

    public Map<String, int[]> getColors() {
        return this.colors;
    }

    public Map<String, Integer> getColorsInt() {
        return this.colorsInt;
    }
}


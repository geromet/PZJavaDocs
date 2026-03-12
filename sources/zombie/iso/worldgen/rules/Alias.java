/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.worldgen.rules;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public record Alias(String name, List<String> tiles) {
    public static Alias load(BufferedReader br, String[] input) throws IOException {
        String line;
        String name = "";
        ArrayList<String> tiles = new ArrayList<String>();
        block8: while ((line = br.readLine()) != null && !line.equals("}")) {
            if ((line = line.strip()).isEmpty() || line.equals("{")) continue;
            String[] split = line.split("\\h+");
            switch (split[0]) {
                case "name": {
                    name = String.join((CharSequence)" ", Arrays.copyOfRange(split, 2, split.length));
                    break;
                }
                case "tiles": {
                    if (split[2].equals("[")) {
                        String l;
                        while ((l = br.readLine()) != null && !l.strip().equals("]")) {
                            tiles.add(l.strip());
                        }
                        continue block8;
                    }
                    tiles.add(split[2]);
                }
            }
        }
        return new Alias(name, tiles);
    }
}


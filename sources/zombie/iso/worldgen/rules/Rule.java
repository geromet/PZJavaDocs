/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.worldgen.rules;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public record Rule(String label, int bitmap, int[] color, List<String> tiles, String layer, int[] condition) {
    public static Rule load(BufferedReader br, String[] input) throws IOException {
        String line;
        String label = "";
        int bitmap = 0;
        int[] color = new int[]{-1, -1, -1};
        ArrayList<String> tiles = new ArrayList<String>();
        String layer = "";
        int[] condition = new int[]{-1, -1, -1};
        block16: while ((line = br.readLine()) != null && !line.equals("}")) {
            if ((line = line.strip()).isEmpty() || line.equals("{")) continue;
            String[] split = line.split("\\h+");
            switch (split[0]) {
                case "label": {
                    label = String.join((CharSequence)" ", Arrays.copyOfRange(split, 2, split.length));
                    break;
                }
                case "bitmap": {
                    bitmap = Integer.parseInt(split[2]);
                    break;
                }
                case "color": {
                    color = new int[]{Integer.parseInt(split[2]), Integer.parseInt(split[3]), Integer.parseInt(split[4])};
                    break;
                }
                case "tiles": {
                    if (split[2].equals("[")) {
                        String l;
                        while ((l = br.readLine()) != null && !l.strip().equals("]")) {
                            tiles.add(l.strip());
                        }
                        continue block16;
                    }
                    tiles.add(split[2]);
                    break;
                }
                case "layer": {
                    layer = split[2];
                    break;
                }
                case "condition": {
                    condition = new int[]{Integer.parseInt(split[2]), Integer.parseInt(split[3]), Integer.parseInt(split[4])};
                }
            }
        }
        return new Rule(label, bitmap, color, tiles, layer, condition);
    }
}


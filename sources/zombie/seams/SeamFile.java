/*
 * Decompiled with CFR 0.152.
 */
package zombie.seams;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import org.joml.Vector2f;
import org.joml.Vector3f;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.scripting.ScriptParser;

public final class SeamFile {
    final ArrayList<Tileset> tilesets = new ArrayList();
    public static final int VERSION1 = 1;
    public static final int VERSION_LATEST = 1;
    int version = 1;

    void read(String fileName) {
        File file = new File(fileName);
        try (FileInputStream fis2 = new FileInputStream(file);
             InputStreamReader isr = new InputStreamReader(fis2);
             BufferedReader br = new BufferedReader(isr);){
            CharBuffer totalFile = CharBuffer.allocate((int)file.length());
            br.read(totalFile);
            totalFile.flip();
            this.parseFile(totalFile.toString());
        }
        catch (FileNotFoundException fis2) {
        }
        catch (Exception e) {
            ExceptionLogger.logException(e);
        }
    }

    void parseFile(String totalFile) throws IOException {
        totalFile = ScriptParser.stripComments(totalFile);
        ScriptParser.Block block = ScriptParser.parse(totalFile);
        block = block.children.get(0);
        ScriptParser.Value value = block.getValue("VERSION");
        if (value == null) {
            throw new IOException("missing VERSION in seams.txt");
        }
        this.version = PZMath.tryParseInt(value.getValue().trim(), -1);
        if (this.version < 1 || this.version > 1) {
            throw new IOException(String.format("unknown seams.txt VERSION \"%s\"", value.getValue().trim()));
        }
        for (ScriptParser.Block child : block.children) {
            Tileset tileset;
            if (!"tileset".equals(child.type) || (tileset = this.parseTileset(child)) == null) continue;
            this.tilesets.add(tileset);
        }
    }

    Tileset parseTileset(ScriptParser.Block block) {
        Tileset tileset = new Tileset();
        ScriptParser.Value value = block.getValue("name");
        tileset.name = value.getValue().trim();
        for (ScriptParser.Block child : block.children) {
            Tile tile;
            if (!"tile".equals(child.type) || (tile = this.parseTile(tileset, child)) == null) continue;
            int index = tileset.tiles.size();
            for (int i = 0; i < tileset.tiles.size(); ++i) {
                Tile tile2 = tileset.tiles.get(i);
                if (tile2.col + tile2.row * 8 <= tile.col + tile.row * 8) continue;
                index = i;
                break;
            }
            tileset.tiles.add(index, tile);
        }
        return tileset;
    }

    Tile parseTile(Tileset tileset, ScriptParser.Block block) {
        Tile tile = new Tile();
        ScriptParser.Value value = block.getValue("xy");
        String[] ss = value.getValue().trim().split("x");
        tile.col = Integer.parseInt(ss[0]);
        tile.row = Integer.parseInt(ss[1]);
        tile.tileName = String.format("%s_%d", tileset.name, tile.col + tile.row * 8);
        for (ScriptParser.Block child : block.children) {
            if ("east".equals(child.type)) {
                tile.joinE = this.parseTileNameList(child);
                continue;
            }
            if ("south".equals(child.type)) {
                tile.joinS = this.parseTileNameList(child);
                continue;
            }
            if ("belowEast".equals(child.type)) {
                tile.joinBelowE = this.parseTileNameList(child);
                continue;
            }
            if ("belowSouth".equals(child.type)) {
                tile.joinBelowS = this.parseTileNameList(child);
                continue;
            }
            if (!"properties".equals(child.type)) continue;
            tile.properties = this.parseTileProperties(child);
        }
        if (tile.isNull()) {
            return null;
        }
        return tile;
    }

    ArrayList<String> parseTileNameList(ScriptParser.Block block) {
        if (block.values.isEmpty()) {
            return null;
        }
        ArrayList<String> tileNames = new ArrayList<String>();
        for (ScriptParser.Value value : block.values) {
            String key = value.getKey().trim();
            if (key.isEmpty()) continue;
            tileNames.add(key);
        }
        return tileNames;
    }

    HashMap<String, String> parseTileProperties(ScriptParser.Block block) {
        if (block.values.isEmpty()) {
            return null;
        }
        HashMap<String, String> result = new HashMap<String, String>();
        for (ScriptParser.Value value : block.values) {
            String key = value.getKey().trim();
            String val = value.getValue().trim();
            if (key.isEmpty()) continue;
            result.put(key, val);
        }
        return result;
    }

    float parseCoord(String str) {
        return PZMath.tryParseFloat(str, 0.0f);
    }

    float parseCoord(ScriptParser.Block block, String valueName, float defaultValue) {
        ScriptParser.Value value = block.getValue(valueName);
        if (value == null) {
            return defaultValue;
        }
        String str = value.getValue().trim();
        return this.parseCoord(str);
    }

    Vector2f parseVector2(String str, Vector2f v) {
        String[] ss = str.trim().split("x");
        v.x = this.parseCoord(ss[0]);
        v.y = this.parseCoord(ss[1]);
        return v;
    }

    Vector3f parseVector3(String str, Vector3f v) {
        String[] ss = str.trim().split("x");
        v.x = this.parseCoord(ss[0]);
        v.y = this.parseCoord(ss[1]);
        v.z = this.parseCoord(ss[2]);
        return v;
    }

    boolean parseVector3(ScriptParser.Block block, String valueName, Vector3f v) {
        ScriptParser.Value value = block.getValue(valueName);
        if (value == null) {
            return false;
        }
        String str = value.getValue().trim();
        this.parseVector3(str, v);
        return true;
    }

    void write(String fileName) {
        ScriptParser.Block blockOG = new ScriptParser.Block();
        blockOG.type = "seams";
        blockOG.setValue("VERSION", String.valueOf(1));
        StringBuilder sb = new StringBuilder();
        ArrayList<String> keys2 = new ArrayList<String>();
        for (Tileset tileset : this.tilesets) {
            ScriptParser.Block blockTS = new ScriptParser.Block();
            blockTS.type = "tileset";
            blockTS.setValue("name", tileset.name);
            for (Tile tile : tileset.tiles) {
                if (tile.isNull()) continue;
                ScriptParser.Block blockT = new ScriptParser.Block();
                blockT.type = "tile";
                blockT.setValue("xy", String.format("%dx%d", tile.col, tile.row));
                this.writeTileNames(blockT, tile.joinE, "east");
                this.writeTileNames(blockT, tile.joinS, "south");
                this.writeTileNames(blockT, tile.joinBelowE, "belowEast");
                this.writeTileNames(blockT, tile.joinBelowS, "belowSouth");
                if (tile.properties != null && !tile.properties.isEmpty()) {
                    ScriptParser.Block blockP = new ScriptParser.Block();
                    blockP.type = "properties";
                    keys2.clear();
                    keys2.addAll(tile.properties.keySet());
                    keys2.sort(Comparator.naturalOrder());
                    for (int i = 0; i < keys2.size(); ++i) {
                        String key = (String)keys2.get(i);
                        blockP.setValue(key, tile.properties.get(key));
                    }
                    blockT.elements.add(blockP);
                    blockT.children.add(blockP);
                }
                blockT.comment = String.format("/* %s_%d */", tileset.name, tile.col + tile.row * 8);
                blockTS.elements.add(blockT);
                blockTS.children.add(blockT);
            }
            blockOG.elements.add(blockTS);
            blockOG.children.add(blockTS);
        }
        sb.setLength(0);
        String eol = System.lineSeparator();
        blockOG.prettyPrint(0, sb, eol);
        this.write(fileName, sb.toString());
    }

    void writeTileNames(ScriptParser.Block blockT, ArrayList<String> tileNames, String type) {
        if (tileNames == null || tileNames.isEmpty()) {
            return;
        }
        ScriptParser.Block blockJ = new ScriptParser.Block();
        blockJ.type = type;
        for (int i = 0; i < tileNames.size(); ++i) {
            String tileName = tileNames.get(i);
            blockJ.setValue(tileName, "");
        }
        blockT.elements.add(blockJ);
        blockT.children.add(blockJ);
    }

    void write(String fileName, String totalFile) {
        File file = new File(fileName);
        try (FileWriter fw = new FileWriter(file);
             BufferedWriter br = new BufferedWriter(fw);){
            br.write(totalFile);
        }
        catch (Throwable t) {
            ExceptionLogger.logException(t);
        }
    }

    void Reset() {
    }

    public static final class Tileset {
        String name;
        final ArrayList<Tile> tiles = new ArrayList();

        Tile getTile(int col, int row) {
            for (Tile tile : this.tiles) {
                if (tile.col != col || tile.row != row) continue;
                return tile;
            }
            return null;
        }

        Tile getOrCreateTile(int col, int row) {
            Tile tile = this.getTile(col, row);
            if (tile != null) {
                return tile;
            }
            tile = new Tile();
            tile.col = col;
            tile.row = row;
            int index = this.tiles.size();
            for (int i = 0; i < this.tiles.size(); ++i) {
                Tile tile2 = this.tiles.get(i);
                if (tile2.col + tile2.row * 8 <= col + row * 8) continue;
                index = i;
                break;
            }
            this.tiles.add(index, tile);
            return tile;
        }
    }

    public static final class Tile {
        public String tileName;
        public int col;
        public int row;
        public ArrayList<String> joinE;
        public ArrayList<String> joinS;
        public ArrayList<String> joinBelowE;
        public ArrayList<String> joinBelowS;
        public HashMap<String, String> properties;

        public boolean isMasterTile() {
            return this.joinE != null && !this.joinE.isEmpty() || this.joinS != null && !this.joinS.isEmpty() || this.joinBelowE != null && !this.joinBelowE.isEmpty() || this.joinBelowS != null && !this.joinBelowS.isEmpty();
        }

        public String getMasterTileName() {
            if (this.properties == null) {
                return null;
            }
            return this.properties.get("master");
        }

        public boolean isNull() {
            if (this.joinE != null && !this.joinE.isEmpty()) {
                return false;
            }
            if (this.joinS != null && !this.joinS.isEmpty()) {
                return false;
            }
            if (this.joinBelowE != null && !this.joinBelowE.isEmpty()) {
                return false;
            }
            if (this.joinBelowS != null && !this.joinBelowS.isEmpty()) {
                return false;
            }
            return this.properties == null || this.properties.isEmpty();
        }
    }
}


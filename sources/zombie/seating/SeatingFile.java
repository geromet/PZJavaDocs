/*
 * Decompiled with CFR 0.152.
 */
package zombie.seating;

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
import java.util.Locale;
import org.joml.Vector2f;
import org.joml.Vector3f;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.scripting.ScriptParser;
import zombie.util.StringUtils;

public final class SeatingFile {
    final ArrayList<Tileset> tilesets = new ArrayList();
    public static final int VERSION1 = 1;
    public static final int VERSION2 = 2;
    public static final int VERSION3 = 3;
    public static final int VERSION_LATEST = 3;
    int version = 3;
    private static final int COORD_MULT = 10000;

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
            throw new IOException("missing VERSION in seating.txt");
        }
        this.version = PZMath.tryParseInt(value.getValue().trim(), -1);
        if (this.version < 1 || this.version > 3) {
            throw new IOException(String.format("unknown seating.txt VERSION \"%s\"", value.getValue().trim()));
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
        String[] ss = value.getValue().trim().split(this.version < 3 ? "x" : "\\s+");
        tile.col = Integer.parseInt(ss[0]);
        tile.row = Integer.parseInt(ss[1]);
        if (this.version == 1) {
            --tile.col;
            --tile.row;
        }
        for (ScriptParser.Block child : block.children) {
            if ("position".equals(child.type)) {
                Position position = this.parsePosition(child);
                if (position == null) continue;
                tile.positions.add(position);
                continue;
            }
            if (!"properties".equals(child.type)) continue;
            this.parseProperties(child, tile.properties);
            if (this.version >= 3) continue;
            this.propertiesToPositions(tile);
        }
        return tile;
    }

    Position parsePosition(ScriptParser.Block block) {
        Position position = new Position();
        ScriptParser.Value value = block.getValue("id");
        if (value == null || StringUtils.isNullOrWhitespace(value.getValue())) {
            return null;
        }
        position.id = value.getValue().trim();
        this.parseVector3(block, "translate", position.translate);
        for (ScriptParser.Block child : block.children) {
            if (!"properties".equals(child.type)) continue;
            this.parseProperties(child, position.properties);
        }
        return position;
    }

    void parseProperties(ScriptParser.Block block, HashMap<String, String> properties) {
        if (block.values.isEmpty()) {
            return;
        }
        for (ScriptParser.Value value : block.values) {
            String key = value.getKey().trim().intern();
            String val = value.getValue().trim().intern();
            if (key.isEmpty()) continue;
            properties.put(key, val);
        }
    }

    float parseCoord(String str) {
        if (this.version < 3) {
            return PZMath.tryParseFloat(str, 0.0f);
        }
        return (float)PZMath.tryParseInt(str, 0) / 10000.0f;
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
        String[] ss = str.trim().split(this.version < 3 ? "x" : "\\s+");
        v.x = this.parseCoord(ss[0]);
        v.y = this.parseCoord(ss[1]);
        return v;
    }

    Vector3f parseVector3(String str, Vector3f v) {
        String[] ss = str.trim().split(this.version < 3 ? "x" : "\\s+");
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

    void propertiesToPositions(Tile tile) {
        if (tile.properties == null) {
            return;
        }
        String translateX = tile.properties.remove("translateX");
        String translateY = tile.properties.remove("translateY");
        String translateZ = tile.properties.remove("translateZ");
        if (translateX != null && translateY != null && translateZ != null) {
            float x = this.parseCoord(translateX);
            float y = this.parseCoord(translateY);
            float z = this.parseCoord(translateZ);
            Position position = new Position();
            position.id = "default";
            position.translate.set(x, y, z);
            tile.positions.add(position);
        }
    }

    void write(String fileName) {
        ScriptParser.Block blockOG = new ScriptParser.Block();
        blockOG.type = "seating";
        blockOG.setValue("VERSION", String.valueOf(3));
        StringBuilder sb = new StringBuilder();
        ArrayList<String> keys2 = new ArrayList<String>();
        for (Tileset tileset : this.tilesets) {
            ScriptParser.Block blockTS = new ScriptParser.Block();
            blockTS.type = "tileset";
            blockTS.setValue("name", tileset.name);
            for (Tile tile : tileset.tiles) {
                if (tile.isEmpty()) continue;
                ScriptParser.Block blockT = new ScriptParser.Block();
                blockT.type = "tile";
                blockT.setValue("xy", String.format("%d %d", tile.col, tile.row));
                for (Position position : tile.positions) {
                    ScriptParser.Block blockP = new ScriptParser.Block();
                    blockP.type = "position";
                    blockP.setValue("id", position.id);
                    blockP.setValue("translate", this.formatVector3(position.translate));
                    if (!position.properties.isEmpty()) {
                        ScriptParser.Block blockProperties = this.propertiesToBlock(position.properties, keys2);
                        blockP.elements.add(blockProperties);
                        blockP.children.add(blockProperties);
                    }
                    blockT.elements.add(blockP);
                    blockT.children.add(blockP);
                }
                if (tile.properties != null && !tile.properties.isEmpty()) {
                    ScriptParser.Block blockP = this.propertiesToBlock(tile.properties, keys2);
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

    ScriptParser.Block propertiesToBlock(HashMap<String, String> properties, ArrayList<String> keys2) {
        ScriptParser.Block blockP = new ScriptParser.Block();
        blockP.type = "properties";
        keys2.clear();
        keys2.addAll(properties.keySet());
        keys2.sort(Comparator.naturalOrder());
        for (int i = 0; i < keys2.size(); ++i) {
            String key = keys2.get(i);
            blockP.setValue(key, properties.get(key));
        }
        return blockP;
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

    int coordInt(float f) {
        return Math.round(f * 10000.0f);
    }

    String formatFloat(float f) {
        return String.format(Locale.US, "%d", this.coordInt(f));
    }

    String formatVector3(float x, float y, float z) {
        return String.format(Locale.US, "%d %d %d", this.coordInt(x), this.coordInt(y), this.coordInt(z));
    }

    String formatVector3(Vector3f v) {
        return this.formatVector3(v.x, v.y, v.z);
    }

    void Reset() {
        this.tilesets.clear();
    }

    static final class Tileset {
        String name;
        final ArrayList<Tile> tiles = new ArrayList();

        Tileset() {
        }

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

        void merge(Tileset other) {
            for (Tile tile2 : other.tiles) {
                Tile tile1 = this.getTile(tile2.col, tile2.row);
                if (tile1 != null && !tile1.isEmpty()) continue;
                tile1 = this.getOrCreateTile(tile2.col, tile2.row);
                tile1.set(tile2);
            }
        }
    }

    static final class Tile {
        int col;
        int row;
        final ArrayList<Position> positions = new ArrayList();
        final HashMap<String, String> properties = new HashMap();

        Tile() {
        }

        boolean isEmpty() {
            return this.properties.isEmpty() && this.positions.isEmpty();
        }

        Position getPositionByIndex(int index) {
            return index >= 0 && index < this.positions.size() ? this.positions.get(index) : null;
        }

        Tile set(Tile other) {
            this.positions.clear();
            for (Position position : other.positions) {
                this.positions.add(new Position().set(position));
            }
            this.properties.clear();
            this.properties.putAll(other.properties);
            return this;
        }
    }

    static final class Position {
        String id;
        final Vector3f translate = new Vector3f();
        final HashMap<String, String> properties = new HashMap();

        Position() {
        }

        Position set(Position other) {
            this.id = other.id;
            this.translate.set(other.translate);
            this.properties.clear();
            this.properties.putAll(other.properties);
            return this;
        }

        String getProperty(String key) {
            return this.properties.get(key);
        }

        void setProperty(String key, String value) {
            if (value == null) {
                this.properties.remove(key);
            } else {
                this.properties.put(key, value);
            }
        }
    }
}


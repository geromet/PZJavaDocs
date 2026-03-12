/*
 * Decompiled with CFR 0.152.
 */
package zombie.tileDepth;

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
import java.util.HashMap;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.scripting.ScriptParser;
import zombie.tileDepth.TileDepthTexture;
import zombie.tileDepth.TileDepthTextureManager;
import zombie.tileDepth.TileGeometryFile;
import zombie.tileDepth.TileGeometryManager;
import zombie.util.StringUtils;

public final class TileDepthTextureAssignments {
    private final HashMap<String, String> assignments = new HashMap();
    private final String mediaAbsPath;
    public static final int VERSION1 = 1;
    public static final int VERSION_LATEST = 1;
    int version = 1;

    public TileDepthTextureAssignments(String mediaAbsPath) {
        this.mediaAbsPath = mediaAbsPath;
    }

    public void assignTileName(String assignTo, String otherTile) {
        if (StringUtils.isNullOrEmpty(assignTo)) {
            return;
        }
        if (StringUtils.isNullOrWhitespace(otherTile)) {
            this.assignments.remove(assignTo);
            return;
        }
        if (assignTo.equals(otherTile)) {
            return;
        }
        this.assignments.put(assignTo, otherTile);
    }

    public void clearAssignedTileName(String assignTo) {
        this.assignments.remove(assignTo);
    }

    public String getAssignedTileName(String tileName) {
        return this.assignments.get(tileName);
    }

    public void load() {
        this.assignments.clear();
        String fileName = this.mediaAbsPath + "/tileDepthTextureAssignments.txt";
        this.read(fileName);
    }

    public void save() {
        String fileName = this.mediaAbsPath + "/tileDepthTextureAssignments.txt";
        this.write(fileName);
    }

    private void read(String fileName) {
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

    private void parseFile(String totalFile) throws IOException {
        ScriptParser.Block block = ScriptParser.parse(totalFile);
        block = block.children.get(0);
        ScriptParser.Value value = block.getValue("VERSION");
        if (value == null) {
            throw new IOException("missing VERSION in tileGeometry.txt");
        }
        this.version = PZMath.tryParseInt(value.getValue().trim(), -1);
        if (this.version < 1 || this.version > 1) {
            throw new IOException(String.format("unknown tileGeometry.txt VERSION \"%s\"", value.getValue().trim()));
        }
        for (ScriptParser.Block child : block.children) {
            if (!"xxx".equals(child.type)) continue;
        }
        for (ScriptParser.Value value2 : block.values) {
            String assignTo = StringUtils.discardNullOrWhitespace(value2.getKey().trim());
            String otherTile = StringUtils.discardNullOrWhitespace(value2.getValue().trim());
            if (assignTo == null || otherTile == null) continue;
            this.assignTileName(assignTo, otherTile);
            int p = otherTile.lastIndexOf(95);
            if (p == -1) continue;
            String tilesetName = otherTile.substring(0, p);
            int tileIndex = PZMath.tryParseInt(otherTile.substring(p + 1), -1);
            TileGeometryFile.Tile tile = TileGeometryManager.getInstance().getTile("game", tilesetName, tileIndex % 8, tileIndex / 8);
            if (tile != null && !tile.geometry.isEmpty()) continue;
            boolean bl = true;
        }
    }

    void write(String fileName) {
        ScriptParser.Block block = new ScriptParser.Block();
        block.type = "tileDepthTextureAssignments";
        block.setValue("VERSION", String.valueOf(1));
        ArrayList<String> tileNames = new ArrayList<String>(this.assignments.keySet());
        tileNames.sort(String::compareTo);
        for (String tileName : tileNames) {
            block.setValue(tileName, this.assignments.get(tileName));
        }
        StringBuilder sb = new StringBuilder();
        sb.setLength(0);
        String eol = System.lineSeparator();
        block.prettyPrint(0, sb, eol);
        this.write(fileName, sb.toString());
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

    public void assignDepthTextureToSprite(String tileName) {
        IsoSprite sprite = IsoSpriteManager.instance.namedMap.get(tileName);
        if (sprite == null) {
            return;
        }
        if (sprite.depthTexture != null && sprite.depthTexture.getName().equals(sprite.name)) {
            return;
        }
        String otherTile = this.getAssignedTileName(tileName);
        if (otherTile == null) {
            sprite.depthTexture = null;
            return;
        }
        TileDepthTexture depthTexture = TileDepthTextureManager.getInstance().getTextureFromTileName(otherTile);
        if (depthTexture == null || depthTexture.isEmpty()) {
            sprite.depthTexture = null;
            return;
        }
        sprite.depthTexture = depthTexture;
    }
}


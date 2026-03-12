/*
 * Decompiled with CFR 0.152.
 */
package zombie.tileDepth;

import gnu.trove.list.array.TFloatArrayList;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.scripting.ScriptParser;
import zombie.tileDepth.TileGeometryUtils;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.Clipper;
import zombie.worldMap.Rasterize;

public final class TileGeometryFile {
    private static Clipper clipper;
    final ArrayList<Tileset> tilesets = new ArrayList();
    private static final int COORD_MULT = 10000;
    public static final int VERSION1 = 1;
    public static final int VERSION2 = 2;
    public static final int VERSION_LATEST = 2;
    int version = 2;

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
            throw new IOException("missing VERSION in tileGeometry.txt");
        }
        this.version = PZMath.tryParseInt(value.getValue().trim(), -1);
        if (this.version < 1 || this.version > 2) {
            throw new IOException(String.format("unknown tileGeometry.txt VERSION \"%s\"", value.getValue().trim()));
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
        for (ScriptParser.Block child : block.children) {
            Polygon polygon;
            Cylinder cylinder;
            Box box;
            if ("box".equals(child.type) && (box = this.parseBox(child)) != null) {
                tile.geometry.add(box);
            }
            if ("cylinder".equals(child.type) && (cylinder = this.parseCylinder(child)) != null) {
                tile.geometry.add(cylinder);
            }
            if ("polygon".equals(child.type) && (polygon = this.parsePolygon(child)) != null) {
                tile.geometry.add(polygon);
            }
            if (!"properties".equals(child.type)) continue;
            tile.properties = this.parseTileProperties(child);
        }
        return tile;
    }

    Box parseBox(ScriptParser.Block block) {
        Box box = new Box();
        if (!this.parseVector3(block, "translate", box.translate)) {
            return null;
        }
        if (!this.parseVector3(block, "rotate", box.rotate)) {
            return null;
        }
        if (!this.parseVector3(block, "min", box.min)) {
            return null;
        }
        if (!this.parseVector3(block, "max", box.max)) {
            return null;
        }
        return box;
    }

    Cylinder parseCylinder(ScriptParser.Block block) {
        Cylinder cylinder = new Cylinder();
        if (!this.parseVector3(block, "translate", cylinder.translate)) {
            return null;
        }
        if (!this.parseVector3(block, "rotate", cylinder.rotate)) {
            return null;
        }
        cylinder.radius1 = this.parseCoord(block, "radius1", -1.0f);
        if (cylinder.radius1 <= 0.0f) {
            return null;
        }
        cylinder.radius2 = this.parseCoord(block, "radius2", -1.0f);
        if (cylinder.radius2 <= 0.0f) {
            return null;
        }
        cylinder.height = this.parseCoord(block, "height", -1.0f);
        if (cylinder.height < 0.0f) {
            return null;
        }
        return cylinder;
    }

    Polygon parsePolygon(ScriptParser.Block block) {
        String[] ss;
        Polygon polygon = new Polygon();
        if (!this.parseVector3(block, "translate", polygon.translate)) {
            return null;
        }
        ScriptParser.Value value = block.getValue("plane");
        polygon.plane = Plane.valueOf(value.getValue().trim());
        if (block.getValue("rotate") == null) {
            switch (polygon.plane.ordinal()) {
                case 0: {
                    polygon.rotate.set(0.0f, 0.0f, 0.0f);
                    break;
                }
                case 1: {
                    polygon.rotate.set(90.0f, 0.0f, 0.0f);
                    break;
                }
                case 2: {
                    polygon.rotate.set(0.0f, 270.0f, 0.0f);
                }
            }
        } else if (!this.parseVector3(block, "rotate", polygon.rotate)) {
            // empty if block
        }
        value = block.getValue("points");
        for (String xy : ss = value.getValue().trim().split(" ")) {
            String[] ss2 = xy.split("x");
            float x = this.parseCoord(ss2[0]);
            float y = this.parseCoord(ss2[1]);
            polygon.points.add(x);
            polygon.points.add(y);
        }
        return polygon;
    }

    HashMap<String, String> parseTileProperties(ScriptParser.Block block) {
        if (block.values.isEmpty()) {
            return null;
        }
        HashMap<String, String> result = new HashMap<String, String>();
        for (ScriptParser.Value value : block.values) {
            String key = value.getKey().trim().intern();
            String val = value.getValue().trim().intern();
            if (key.isEmpty()) continue;
            result.put(key, val);
        }
        return result;
    }

    float parseCoord(String str) {
        if (this.version == 1) {
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

    int coordInt(float f) {
        return Math.round(f * 10000.0f);
    }

    void write(String fileName) {
        ScriptParser.Block blockOG = new ScriptParser.Block();
        blockOG.type = "tileGeometry";
        blockOG.setValue("VERSION", String.valueOf(2));
        StringBuilder sb = new StringBuilder();
        ArrayList<String> keys2 = new ArrayList<String>();
        for (Tileset tileset : this.tilesets) {
            ScriptParser.Block blockTS = new ScriptParser.Block();
            blockTS.type = "tileset";
            blockTS.setValue("name", tileset.name);
            for (Tile tile : tileset.tiles) {
                if (tile.geometry.isEmpty() && (tile.properties == null || tile.properties.isEmpty())) continue;
                ScriptParser.Block blockT = new ScriptParser.Block();
                blockT.type = "tile";
                blockT.setValue("xy", String.format("%dx%d", tile.col, tile.row));
                for (Geometry geometry : tile.geometry) {
                    ScriptParser.Block blockG = geometry.toBlock(sb);
                    if (blockG == null) continue;
                    blockT.elements.add(blockG);
                    blockT.children.add(blockG);
                }
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

        void initSpriteProperties() {
            for (int i = 0; i < this.tiles.size(); ++i) {
                String[] ss;
                int index;
                IsoSprite sprite;
                Tile tile = this.tiles.get(i);
                if (tile.properties == null || tile.properties.isEmpty() || (sprite = IsoSpriteManager.instance.namedMap.get(this.name + "_" + (index = tile.col + tile.row * 8))) == null) continue;
                String value = tile.properties.get("ItemHeight");
                if (value != null) {
                    sprite.getProperties().set("ItemHeight", value, false);
                }
                if ((value = tile.properties.get("Surface")) != null) {
                    sprite.getProperties().set("Surface", value, false);
                }
                if (!StringUtils.isNullOrWhitespace(value = tile.properties.get("CurtainOffset")) && (ss = value.split("\\s+")).length == 3) {
                    float x = PZMath.tryParseFloat(ss[0], 0.0f);
                    float y = PZMath.tryParseFloat(ss[1], 0.0f);
                    float z = PZMath.tryParseFloat(ss[2], 0.0f);
                    sprite.setCurtainOffset(x, y, z);
                }
                if (StringUtils.tryParseBoolean(value = tile.properties.get("OpaquePixelsOnly"))) {
                    sprite.depthFlags |= 4;
                }
                if (StringUtils.tryParseBoolean(value = tile.properties.get("Translucent"))) {
                    sprite.depthFlags |= 2;
                }
                if (!StringUtils.tryParseBoolean(value = tile.properties.get("UseObjectDepthTexture"))) continue;
                sprite.depthFlags |= 1;
            }
        }
    }

    static final class Tile {
        int col;
        int row;
        final ArrayList<Geometry> geometry = new ArrayList();
        HashMap<String, String> properties;

        Tile() {
        }

        void setGeometry(ArrayList<Geometry> geometry) {
            this.geometry.clear();
            this.geometry.addAll(geometry);
        }
    }

    public static final class Box
    extends Geometry {
        public final Vector3f translate = new Vector3f();
        public final Vector3f rotate = new Vector3f();
        public final Vector3f min = new Vector3f();
        public final Vector3f max = new Vector3f();

        @Override
        public Object clone() {
            Box copy = new Box();
            copy.translate.set(this.translate);
            copy.rotate.set(this.rotate);
            copy.min.set(this.min);
            copy.max.set(this.max);
            return copy;
        }

        @Override
        public boolean isBox() {
            return true;
        }

        @Override
        public ScriptParser.Block toBlock(StringBuilder sb) {
            ScriptParser.Block block = new ScriptParser.Block();
            block.type = "box";
            block.setValue("translate", this.formatVector3(this.translate));
            block.setValue("rotate", this.formatVector3(this.rotate));
            block.setValue("min", this.formatVector3(this.min));
            block.setValue("max", this.formatVector3(this.max));
            return block;
        }

        @Override
        public void offset(float dx, float dy) {
            this.translate.add(dx, 0.0f, dy);
        }

        @Override
        public void offset(float dx, float dz, float dy) {
            this.translate.add(dx, dz, dy);
        }

        @Override
        float getNormalizedDepthAt(float tileX, float tileY) {
            return TileGeometryUtils.getNormalizedDepthOnBoxAt(tileX, tileY, this.translate, this.rotate, this.min, this.max);
        }
    }

    public static final class Cylinder
    extends Geometry {
        public final Vector3f translate = new Vector3f();
        public final Vector3f rotate = new Vector3f();
        public float radius1;
        public float radius2;
        public float height;

        @Override
        public Object clone() {
            Cylinder copy = new Cylinder();
            copy.translate.set(this.translate);
            copy.rotate.set(this.rotate);
            copy.radius1 = this.radius1;
            copy.radius2 = this.radius2;
            copy.height = this.height;
            return copy;
        }

        @Override
        public boolean isCylinder() {
            return true;
        }

        @Override
        public ScriptParser.Block toBlock(StringBuilder sb) {
            ScriptParser.Block block = new ScriptParser.Block();
            block.type = "cylinder";
            block.setValue("translate", this.formatVector3(this.translate));
            block.setValue("rotate", this.formatVector3(this.rotate));
            block.setValue("radius1", this.formatFloat(this.radius1));
            block.setValue("radius2", this.formatFloat(this.radius2));
            block.setValue("height", this.formatFloat(this.height));
            return block;
        }

        @Override
        public void offset(float dx, float dy) {
            this.translate.add(dx, 0.0f, dy);
        }

        @Override
        float getNormalizedDepthAt(float tileX, float tileY) {
            return TileGeometryUtils.getNormalizedDepthOnCylinderAt(tileX, tileY, this.translate, this.rotate, this.radius1, this.height);
        }
    }

    public static final class Polygon
    extends Geometry {
        public Plane plane;
        public final Vector3f translate = new Vector3f();
        public final Vector3f rotate = new Vector3f();
        public final TFloatArrayList points = new TFloatArrayList();
        public final TFloatArrayList triangles = new TFloatArrayList();

        @Override
        public Object clone() {
            Polygon copy = new Polygon();
            copy.plane = this.plane;
            copy.translate.set(this.translate);
            copy.rotate.set(this.rotate);
            copy.points.addAll(this.points);
            copy.triangles.addAll(this.triangles);
            return copy;
        }

        @Override
        public ScriptParser.Block toBlock(StringBuilder sb) {
            ScriptParser.Block blockP = new ScriptParser.Block();
            blockP.type = "polygon";
            blockP.setValue("translate", this.formatVector3(this.translate));
            blockP.setValue("rotate", this.formatVector3(this.rotate));
            blockP.setValue("plane", this.plane.name());
            sb.setLength(0);
            for (int i = 0; i < this.points.size(); i += 2) {
                float x = this.points.get(i);
                float y = this.points.get(i + 1);
                sb.append(String.format(Locale.US, "%dx%d ", this.coordInt(x), this.coordInt(y)));
            }
            blockP.setValue("points", sb.toString());
            return blockP;
        }

        @Override
        public void offset(float dx, float dy) {
            this.translate.add(dx, 0.0f, dy);
        }

        @Override
        public boolean isPolygon() {
            return true;
        }

        boolean isClockwise() {
            float sum = 0.0f;
            for (int i = 0; i < this.points.size(); i += 2) {
                float p1x = this.points.get(i);
                float p1y = this.points.get(i + 1);
                float p2x = this.points.get((i + 2) % this.points.size());
                float p2y = this.points.get((i + 3) % this.points.size());
                sum += (p2x - p1x) * (p2y + p1y);
            }
            return (double)sum > 0.0;
        }

        void triangulate() {
            this.triangles.clear();
            if (clipper == null) {
                clipper = new Clipper();
            }
            clipper.clear();
            ByteBuffer bb = ByteBuffer.allocateDirect(8 * (this.points.size() / 2) * 3);
            if (this.isClockwise()) {
                for (i = this.points.size() - 2; i >= 0; i -= 2) {
                    bb.putFloat(this.points.getQuick(i));
                    bb.putFloat(this.points.getQuick(i + 1));
                }
            } else {
                for (i = 0; i < this.points.size(); i += 2) {
                    bb.putFloat(this.points.getQuick(i));
                    bb.putFloat(this.points.getQuick(i + 1));
                }
            }
            clipper.addPath(this.points.size() / 2, bb, false);
            int numPolygons = clipper.generatePolygons();
            if (numPolygons < 1) {
                return;
            }
            bb.clear();
            int numPoints = clipper.triangulate(0, bb);
            Matrix4f m = new Matrix4f();
            m.translation(this.translate);
            m.rotateXYZ(this.rotate.x * ((float)Math.PI / 180), this.rotate.y * ((float)Math.PI / 180), this.rotate.z * ((float)Math.PI / 180));
            Vector3f v = new Vector3f();
            for (int i = 0; i < numPoints; ++i) {
                float x = bb.getFloat();
                float y = bb.getFloat();
                float z = 0.0f;
                v.set(x, y, 0.0f);
                m.transformPosition(v);
                this.triangles.add(v.x);
                this.triangles.add(v.y);
                this.triangles.add(v.z);
            }
        }

        void triangulate2() {
            this.triangles.clear();
            if (clipper == null) {
                clipper = new Clipper();
            }
            clipper.clear();
            ByteBuffer bb = ByteBuffer.allocateDirect(8 * (this.points.size() / 2) * 3);
            if (this.isClockwise()) {
                for (i = this.points.size() - 2; i >= 0; i -= 2) {
                    bb.putFloat(this.points.getQuick(i));
                    bb.putFloat(this.points.getQuick(i + 1));
                }
            } else {
                for (i = 0; i < this.points.size(); i += 2) {
                    bb.putFloat(this.points.getQuick(i));
                    bb.putFloat(this.points.getQuick(i + 1));
                }
            }
            clipper.addPath(this.points.size() / 2, bb, false);
            int numPolygons = clipper.generatePolygons();
            if (numPolygons < 1) {
                return;
            }
            bb.clear();
            int numPoints = clipper.triangulate(0, bb);
            Vector3f v = new Vector3f();
            for (int i = 0; i < numPoints; ++i) {
                float x = bb.getFloat();
                float y = bb.getFloat();
                this.triangles.add(x);
                this.triangles.add(y);
            }
        }

        @Override
        float getNormalizedDepthAt(float tileX, float tileY) {
            Vector3f normal = BaseVehicle.allocVector3f().set(0.0f, 0.0f, 1.0f);
            Matrix4f m = BaseVehicle.allocMatrix4f().rotationXYZ(this.rotate.x * ((float)Math.PI / 180), this.rotate.y * ((float)Math.PI / 180), this.rotate.z * ((float)Math.PI / 180));
            m.transformDirection(normal);
            BaseVehicle.releaseMatrix4f(m);
            float depth = TileGeometryUtils.getNormalizedDepthOnPlaneAt(tileX, tileY, this.translate, normal);
            BaseVehicle.releaseVector3f(normal);
            return depth;
        }

        public void rasterize(Rasterize.ICallback consumer) {
            this.triangulate2();
            this.calcMatrices(L_rasterizeDepth.m_projection, L_rasterizeDepth.m_modelView);
            Vector2f uiPos1 = L_rasterizeDepth.vector2f_1;
            Vector2f uiPos2 = L_rasterizeDepth.vector2f_2;
            Vector2f uiPos3 = L_rasterizeDepth.vector2f_3;
            Vector2f point = L_rasterizeDepth.vector2f_4;
            Vector2f tileXY = L_rasterizeDepth.vector2f_5;
            this.calculateTextureTopLeft(0.0f, 0.0f, 0.0f, tileXY);
            float pixelSize = this.calculatePixelSize();
            for (int i = 0; i < this.triangles.size(); i += 6) {
                float x0 = this.triangles.get(i);
                float y0 = this.triangles.get(i + 1);
                float x1 = this.triangles.get(i + 2);
                float y1 = this.triangles.get(i + 3);
                float x2 = this.triangles.get(i + 4);
                float y2 = this.triangles.get(i + 5);
                this.planeToUI(point.set(x0, y0), uiPos1);
                this.planeToUI(point.set(x1, y1), uiPos2);
                this.planeToUI(point.set(x2, y2), uiPos3);
                this.uiToTile(tileXY, pixelSize, uiPos1, uiPos1);
                this.uiToTile(tileXY, pixelSize, uiPos2, uiPos2);
                this.uiToTile(tileXY, pixelSize, uiPos3, uiPos3);
                L_rasterizeDepth.rasterize.scanTriangle(uiPos1.x, uiPos1.y, uiPos2.x, uiPos2.y, uiPos3.x, uiPos3.y, -1000, 1000, consumer);
            }
            this.triangles.clear();
        }

        float zoomMult() {
            int zoom = 3;
            return (float)Math.exp(0.6f) * 160.0f / Math.max(1.82f, 1.0f);
        }

        private void calcMatrices(Matrix4f projection, Matrix4f modelView) {
            float w = this.screenWidth();
            float scale = 1366.0f / w;
            float h = (float)this.screenHeight() * scale;
            w = 1366.0f;
            projection.setOrtho(-(w /= this.zoomMult()) / 2.0f, w / 2.0f, -(h /= this.zoomMult()) / 2.0f, h / 2.0f, -10.0f, 10.0f);
            float viewX = 0.0f;
            float viewY = 0.0f;
            float zoomedViewX = 0.0f / this.zoomMult() * scale;
            float zoomedViewY = 0.0f / this.zoomMult() * scale;
            projection.translate(-zoomedViewX, zoomedViewY, 0.0f);
            modelView.identity();
            float rotateX = 30.0f;
            float rotateY = 315.0f;
            float rotateZ = 0.0f;
            modelView.rotateXYZ(0.5235988f, 5.497787f, 0.0f);
        }

        float calculatePixelSize() {
            float sx = this.sceneToUIX(0.0f, 0.0f, 0.0f);
            float sy = this.sceneToUIY(0.0f, 0.0f, 0.0f);
            float sx2 = this.sceneToUIX(1.0f, 0.0f, 0.0f);
            float sy2 = this.sceneToUIY(1.0f, 0.0f, 0.0f);
            return (float)(Math.sqrt((sx2 - sx) * (sx2 - sx) + (sy2 - sy) * (sy2 - sy)) / Math.sqrt(5120.0));
        }

        Vector2f calculateTextureTopLeft(float sceneX, float sceneY, float sceneZ, Vector2f topLeft) {
            float sx = this.sceneToUIX(sceneX, sceneY, sceneZ);
            float sy = this.sceneToUIY(sceneX, sceneY, sceneZ);
            float pixelSize = this.calculatePixelSize();
            float tileX = sx - 64.0f * pixelSize;
            float tileY = sy - 224.0f * pixelSize;
            return topLeft.set(tileX, tileY);
        }

        Vector3f planeTo3D(Vector2f pointOnPlane, Vector3f result) {
            Matrix4f m = BaseVehicle.allocMatrix4f();
            m.translation(this.translate);
            m.rotateXYZ(this.rotate.x * ((float)Math.PI / 180), this.rotate.y * ((float)Math.PI / 180), this.rotate.z * ((float)Math.PI / 180));
            m.transformPosition(pointOnPlane.x, pointOnPlane.y, 0.0f, result);
            BaseVehicle.releaseMatrix4f(m);
            return result;
        }

        Vector2f planeToUI(Vector2f pointOnPlane, Vector2f result) {
            Vector3f scenePos = this.planeTo3D(pointOnPlane, BaseVehicle.allocVector3f());
            result.set(this.sceneToUIX(scenePos), this.sceneToUIY(scenePos));
            BaseVehicle.releaseVector3f(scenePos);
            return result;
        }

        public float sceneToUIX(Vector3f scenePos) {
            return this.sceneToUIX(scenePos.x, scenePos.y, scenePos.z);
        }

        public float sceneToUIY(Vector3f scenePos) {
            return this.sceneToUIY(scenePos.x, scenePos.y, scenePos.z);
        }

        public float sceneToUIX(float sceneX, float sceneY, float sceneZ) {
            Matrix4f matrix4f = L_rasterizeDepth.matrix4f_1;
            matrix4f.set(L_rasterizeDepth.m_projection);
            matrix4f.mul(L_rasterizeDepth.m_modelView);
            L_rasterizeDepth.m_viewport[0] = 0;
            L_rasterizeDepth.m_viewport[1] = 0;
            L_rasterizeDepth.m_viewport[2] = this.screenWidth();
            L_rasterizeDepth.m_viewport[3] = this.screenHeight();
            matrix4f.project(sceneX, sceneY, sceneZ, L_rasterizeDepth.m_viewport, L_rasterizeDepth.vector3f_1);
            return L_rasterizeDepth.vector3f_1.x();
        }

        public float sceneToUIY(float sceneX, float sceneY, float sceneZ) {
            Matrix4f matrix4f = L_rasterizeDepth.matrix4f_1;
            matrix4f.set(L_rasterizeDepth.m_projection);
            matrix4f.mul(L_rasterizeDepth.m_modelView);
            L_rasterizeDepth.m_viewport[0] = 0;
            L_rasterizeDepth.m_viewport[1] = 0;
            L_rasterizeDepth.m_viewport[2] = this.screenWidth();
            L_rasterizeDepth.m_viewport[3] = this.screenHeight();
            matrix4f.project(sceneX, sceneY, sceneZ, L_rasterizeDepth.m_viewport, L_rasterizeDepth.vector3f_1);
            return (float)this.screenHeight() - L_rasterizeDepth.vector3f_1.y();
        }

        Vector2f uiToTile(Vector2f tileXY, float pixelSize, Vector2f uiPos, Vector2f tilePos) {
            float x = (uiPos.x - tileXY.x) / pixelSize;
            float y = (uiPos.y - tileXY.y) / pixelSize;
            return tilePos.set(x, y);
        }

        int screenWidth() {
            return 1366;
        }

        int screenHeight() {
            return 768;
        }

        static final class L_rasterizeDepth {
            static final Rasterize rasterize = new Rasterize();
            static final Vector2f vector2f_1 = new Vector2f();
            static final Vector2f vector2f_2 = new Vector2f();
            static final Vector2f vector2f_3 = new Vector2f();
            static final Vector2f vector2f_4 = new Vector2f();
            static final Vector2f vector2f_5 = new Vector2f();
            static final Matrix4f m_projection = new Matrix4f();
            static final Matrix4f m_modelView = new Matrix4f();
            static final Matrix4f matrix4f_1 = new Matrix4f();
            static final int[] m_viewport = new int[4];
            static final Vector3f vector3f_1 = new Vector3f();

            L_rasterizeDepth() {
            }
        }
    }

    public static enum Plane {
        XY,
        XZ,
        YZ;

    }

    public static class Geometry {
        public Object clone() {
            return null;
        }

        public boolean isBox() {
            return false;
        }

        public Box asBox() {
            return Type.tryCastTo(this, Box.class);
        }

        public boolean isCylinder() {
            return false;
        }

        public Cylinder asCylinder() {
            return Type.tryCastTo(this, Cylinder.class);
        }

        public boolean isPolygon() {
            return false;
        }

        public Polygon asPolygon() {
            return Type.tryCastTo(this, Polygon.class);
        }

        public ScriptParser.Block toBlock(StringBuilder sb) {
            return null;
        }

        public void offset(float dx, float dy) {
        }

        public void offset(float dx, float dz, float dy) {
        }

        int coordInt(float f) {
            return Math.round(f * 10000.0f);
        }

        String formatFloat(float f) {
            return String.format(Locale.US, "%d", this.coordInt(f));
        }

        String formatVector3(Vector3f v) {
            return this.formatVector3(v.x, v.y, v.z);
        }

        String formatVector3(float x, float y, float z) {
            return String.format(Locale.US, "%dx%dx%d", this.coordInt(x), this.coordInt(y), this.coordInt(z));
        }

        float getNormalizedDepthAt(float tileX, float tileY) {
            return -1.0f;
        }
    }
}


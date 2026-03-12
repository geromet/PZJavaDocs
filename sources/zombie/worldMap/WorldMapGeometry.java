/*
 * Decompiled with CFR 0.152.
 */
package zombie.worldMap;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import zombie.core.math.PZMath;
import zombie.iso.MapFiles;
import zombie.vehicles.Clipper;
import zombie.worldMap.WorldMapCell;
import zombie.worldMap.WorldMapPoints;

public final class WorldMapGeometry {
    public WorldMapCell cell;
    public Type type;
    public final ArrayList<WorldMapPoints> points = new ArrayList(1);
    public int minX;
    public int minY;
    public int maxX;
    public int maxY;
    public int firstIndex = -1;
    public short indexCount = (short)-1;
    public ArrayList<TrianglesPerZoom> trianglesPerZoom;
    public boolean failedToTriangulate;
    public int vboIndex1 = -1;
    public int vboIndex2 = -1;
    public int vboIndex3 = -1;
    public int vboIndex4 = -1;
    private static Clipper clipper;
    private static ByteBuffer vertices;

    public WorldMapGeometry(WorldMapCell cell) {
        this.cell = cell;
    }

    public void calculateBounds() {
        this.minY = Integer.MAX_VALUE;
        this.minX = Integer.MAX_VALUE;
        this.maxY = Integer.MIN_VALUE;
        this.maxX = Integer.MIN_VALUE;
        for (int i = 0; i < this.points.size(); ++i) {
            WorldMapPoints pts = this.points.get(i);
            pts.calculateBounds();
            this.minX = PZMath.min(this.minX, pts.minX);
            this.minY = PZMath.min(this.minY, pts.minY);
            this.maxX = PZMath.max(this.maxX, pts.maxX);
            this.maxY = PZMath.max(this.maxY, pts.maxY);
        }
    }

    public boolean containsPoint(float x, float y) {
        if (this.type != Type.Polygon || this.points.isEmpty()) {
            return false;
        }
        return this.isPointInPolygon_WindingNumber(x, y, 0) != PolygonHit.Outside;
    }

    public void triangulate(WorldMapCell cell, double[] delta) {
        int i;
        float y2;
        float x2;
        int i2;
        if (clipper == null) {
            clipper = new Clipper();
        }
        clipper.clear();
        WorldMapPoints outer = this.points.get(0);
        if (vertices == null || vertices.capacity() < outer.numPoints() * 2 * 50 * 4) {
            vertices = ByteBuffer.allocateDirect(outer.numPoints() * 2 * 50 * 4);
        }
        vertices.clear();
        if (outer.isClockwise()) {
            for (i2 = outer.numPoints() - 1; i2 >= 0; --i2) {
                vertices.putFloat(outer.getX(i2));
                vertices.putFloat(outer.getY(i2));
            }
        } else {
            for (i2 = 0; i2 < outer.numPoints(); ++i2) {
                vertices.putFloat(outer.getX(i2));
                vertices.putFloat(outer.getY(i2));
            }
        }
        clipper.addPath(outer.numPoints(), vertices, false);
        for (i2 = 1; i2 < this.points.size(); ++i2) {
            vertices.clear();
            WorldMapPoints hole = this.points.get(i2);
            if (hole.isClockwise()) {
                for (j = hole.numPoints() - 1; j >= 0; --j) {
                    vertices.putFloat(hole.getX(j));
                    vertices.putFloat(hole.getY(j));
                }
            } else {
                for (j = 0; j < hole.numPoints(); ++j) {
                    vertices.putFloat(hole.getX(j));
                    vertices.putFloat(hole.getY(j));
                }
            }
            clipper.addPath(hole.numPoints(), vertices, true);
        }
        if (this.minX < 0 || this.minY < 0 || this.maxX > 256 || this.maxY > 256) {
            int pad = 768;
            float x1 = -768.0f;
            float y1 = -768.0f;
            x2 = 1024.0f;
            y2 = -768.0f;
            float x3 = 1024.0f;
            float y3 = 1024.0f;
            float x4 = -768.0f;
            float y4 = 1024.0f;
            float x5 = -768.0f;
            float y5 = 0.0f;
            float x6 = 0.0f;
            float y6 = 0.0f;
            float x7 = 0.0f;
            float y7 = 256.0f;
            float x8 = 256.0f;
            float y8 = 256.0f;
            float x9 = 256.0f;
            float y9 = 0.0f;
            float x10 = -768.0f;
            float y10 = 0.0f;
            vertices.clear();
            vertices.putFloat(-768.0f).putFloat(-768.0f);
            vertices.putFloat(1024.0f).putFloat(-768.0f);
            vertices.putFloat(1024.0f).putFloat(1024.0f);
            vertices.putFloat(-768.0f).putFloat(1024.0f);
            vertices.putFloat(-768.0f).putFloat(0.0f);
            vertices.putFloat(0.0f).putFloat(0.0f);
            vertices.putFloat(0.0f).putFloat(256.0f);
            vertices.putFloat(256.0f).putFloat(256.0f);
            vertices.putFloat(256.0f).putFloat(0.0f);
            vertices.putFloat(-768.0f).putFloat(0.0f);
            clipper.addPath(10, vertices, true);
        }
        if (cell.clipHigherPriorityCells == null) {
            int i3;
            int minCell300X = (int)Math.floor((float)cell.x * 256.0f / 300.0f);
            int minCell300Y = (int)Math.floor((float)cell.y * 256.0f / 300.0f);
            int maxCell300X = (int)Math.floor((float)(cell.x + 1) * 256.0f / 300.0f);
            int maxCell300Y = (int)Math.floor((float)(cell.y + 1) * 256.0f / 300.0f);
            ArrayList<MapFiles> mapFiles = MapFiles.getCurrentMapFiles();
            vertices.clear();
            for (i3 = 0; i3 < cell.priority; ++i3) {
                MapFiles mapFiles1 = mapFiles.get(i3);
                for (int cell300Y = minCell300Y; cell300Y <= maxCell300Y; ++cell300Y) {
                    for (int cell300X = minCell300X; cell300X <= maxCell300X; ++cell300X) {
                        if (!mapFiles1.hasCell300(cell300X, cell300Y)) continue;
                        int x1 = cell300X * 300 - cell.x * 256;
                        int y1 = cell300Y * 300 - cell.y * 256;
                        int x22 = x1 + 300;
                        int y22 = y1 + 300;
                        vertices.putFloat(x1).putFloat(y1);
                        vertices.putFloat(x22).putFloat(y22);
                    }
                }
            }
            cell.clipHigherPriorityCells = new int[vertices.position() / 4];
            i3 = 0;
            int j = 0;
            while (i3 < vertices.position()) {
                float f = vertices.getFloat(i3);
                cell.clipHigherPriorityCells[j] = PZMath.roundToInt(f);
                i3 += 4;
                ++j;
            }
        }
        for (i2 = 0; i2 < cell.clipHigherPriorityCells.length; i2 += 4) {
            float x1 = cell.clipHigherPriorityCells[i2];
            float y1 = cell.clipHigherPriorityCells[i2 + 1];
            x2 = cell.clipHigherPriorityCells[i2 + 2];
            y2 = cell.clipHigherPriorityCells[i2 + 3];
            clipper.clipAABB(x1, y1, x2, y2);
        }
        this.firstIndex = 0;
        this.indexCount = 0;
        int numPolys = clipper.generatePolygons(0.0);
        if (numPolys <= 0) {
            return;
        }
        this.firstIndex = -1;
        for (i = 0; i < numPolys; ++i) {
            vertices.clear();
            int numIndices = clipper.triangulate2(i, vertices);
            if (numIndices < 3) continue;
            int numPoints = vertices.getShort();
            FloatBuffer triangleBuffer = this.cell.getTriangleBuffer(numPoints * 2);
            int firstPoint = triangleBuffer.position() / 2;
            for (int j = 0; j < numPoints; ++j) {
                triangleBuffer.put(vertices.getFloat());
                triangleBuffer.put(vertices.getFloat());
            }
            ShortBuffer indexBuffer = this.cell.getIndexBuffer(numIndices);
            if (this.firstIndex == -1) {
                this.firstIndex = (short)indexBuffer.position();
            }
            this.indexCount = (short)(this.indexCount + (short)numIndices);
            for (int j = 0; j < numIndices; ++j) {
                indexBuffer.put((short)(firstPoint + vertices.getShort()));
            }
        }
        if (delta == null) {
            return;
        }
        for (i = 0; i < delta.length; ++i) {
            double delta2 = delta[i] - (i == 0 ? 0.0 : delta[i - 1]);
            numPolys = clipper.generatePolygons(delta2);
            if (numPolys <= 0) continue;
            TrianglesPerZoom tpz = new TrianglesPerZoom();
            tpz.delta = delta[i];
            for (int j = 0; j < numPolys; ++j) {
                vertices.clear();
                int numIndices = clipper.triangulate2(j, vertices);
                if (numIndices < 3) continue;
                int numPoints = vertices.getShort();
                FloatBuffer triangleBuffer = this.cell.getTriangleBuffer(numPoints * 2);
                int firstPoint = triangleBuffer.position() / 2;
                for (int k = 0; k < numPoints; ++k) {
                    triangleBuffer.put(vertices.getFloat());
                    triangleBuffer.put(vertices.getFloat());
                }
                ShortBuffer indexBuffer = this.cell.getIndexBuffer(numIndices);
                if (tpz.firstIndex == -1) {
                    tpz.firstIndex = indexBuffer.position();
                }
                tpz.indexCount = (short)(tpz.indexCount + (short)numIndices);
                for (int k = 0; k < numIndices; ++k) {
                    indexBuffer.put((short)(firstPoint + vertices.getShort()));
                }
            }
            if (tpz.indexCount < 3) continue;
            if (this.trianglesPerZoom == null) {
                this.trianglesPerZoom = new ArrayList(delta.length);
            }
            this.trianglesPerZoom.add(tpz);
        }
        this.trianglesPerZoom.trimToSize();
    }

    TrianglesPerZoom findTriangles(double delta) {
        if (this.trianglesPerZoom == null) {
            return null;
        }
        for (int i = 0; i < this.trianglesPerZoom.size(); ++i) {
            TrianglesPerZoom tpz = this.trianglesPerZoom.get(i);
            if (tpz.delta != delta) continue;
            return tpz;
        }
        return null;
    }

    public void clearTriangles() {
        this.firstIndex = -1;
        this.indexCount = (short)-1;
        if (this.trianglesPerZoom != null) {
            this.trianglesPerZoom.clear();
            this.trianglesPerZoom = null;
        }
    }

    public void dispose() {
        this.points.clear();
        if (this.trianglesPerZoom != null) {
            this.trianglesPerZoom.clear();
            this.trianglesPerZoom = null;
        }
    }

    float isLeft(float x0, float y0, float x1, float y1, float x2, float y2) {
        return (x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0);
    }

    PolygonHit isPointInPolygon_WindingNumber(float x, float y, int flags) {
        int wn = 0;
        WorldMapPoints outer = this.points.get(0);
        for (int i = 0; i < outer.numPoints(); ++i) {
            int x1 = outer.getX(i);
            int y1 = outer.getY(i);
            int x2 = outer.getX((i + 1) % outer.numPoints());
            int y2 = outer.getY((i + 1) % outer.numPoints());
            if ((float)y1 <= y) {
                if (!((float)y2 > y) || !(this.isLeft(x1, y1, x2, y2, x, y) > 0.0f)) continue;
                ++wn;
                continue;
            }
            if (!((float)y2 <= y) || !(this.isLeft(x1, y1, x2, y2, x, y) < 0.0f)) continue;
            --wn;
        }
        return wn == 0 ? PolygonHit.Outside : PolygonHit.Inside;
    }

    public static enum Type {
        LineString,
        Point,
        Polygon;

    }

    private static enum PolygonHit {
        OnEdge,
        Inside,
        Outside;

    }

    public static final class TrianglesPerZoom {
        public int firstIndex = -1;
        public short indexCount = (short)-1;
        double delta;
    }
}


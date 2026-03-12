/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.worldgen.utils.triangulation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import zombie.iso.worldgen.utils.triangulation.Edge2D;
import zombie.iso.worldgen.utils.triangulation.EdgeDistancePack;
import zombie.iso.worldgen.utils.triangulation.Triangle2D;
import zombie.iso.worldgen.utils.triangulation.Vector2D;

class TriangleSoup {
    private final List<Triangle2D> triangleSoup = new ArrayList<Triangle2D>();

    public void add(Triangle2D triangle) {
        this.triangleSoup.add(triangle);
    }

    public void remove(Triangle2D triangle) {
        this.triangleSoup.remove(triangle);
    }

    public List<Triangle2D> getTriangles() {
        return this.triangleSoup;
    }

    public Triangle2D findContainingTriangle(Vector2D point) {
        for (Triangle2D triangle : this.triangleSoup) {
            if (!triangle.contains(point)) continue;
            return triangle;
        }
        return null;
    }

    public Triangle2D findNeighbour(Triangle2D triangle, Edge2D edge) {
        for (Triangle2D triangleFromSoup : this.triangleSoup) {
            if (!triangleFromSoup.isNeighbour(edge) || triangleFromSoup == triangle) continue;
            return triangleFromSoup;
        }
        return null;
    }

    public Triangle2D findOneTriangleSharing(Edge2D edge) {
        for (Triangle2D triangle : this.triangleSoup) {
            if (!triangle.isNeighbour(edge)) continue;
            return triangle;
        }
        return null;
    }

    public Edge2D findNearestEdge(Vector2D point) {
        ArrayList<EdgeDistancePack> edgeList = new ArrayList<EdgeDistancePack>();
        for (Triangle2D triangle : this.triangleSoup) {
            edgeList.add(triangle.findNearestEdge(point));
        }
        Object[] edgeDistancePacks = new EdgeDistancePack[edgeList.size()];
        edgeList.toArray(edgeDistancePacks);
        Arrays.sort(edgeDistancePacks);
        return ((EdgeDistancePack)edgeDistancePacks[0]).edge;
    }

    public void removeTrianglesUsing(Vector2D vertex) {
        ArrayList<Triangle2D> trianglesToBeRemoved = new ArrayList<Triangle2D>();
        for (Triangle2D triangle : this.triangleSoup) {
            if (!triangle.hasVertex(vertex)) continue;
            trianglesToBeRemoved.add(triangle);
        }
        this.triangleSoup.removeAll(trianglesToBeRemoved);
    }
}


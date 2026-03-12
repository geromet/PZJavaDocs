/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.worldgen.utils.triangulation;

import java.util.Arrays;
import zombie.iso.worldgen.utils.triangulation.Edge2D;
import zombie.iso.worldgen.utils.triangulation.EdgeDistancePack;
import zombie.iso.worldgen.utils.triangulation.Vector2D;

public class Triangle2D {
    public Vector2D a;
    public Vector2D b;
    public Vector2D c;

    public Triangle2D(Vector2D a, Vector2D b, Vector2D c) {
        this.a = a;
        this.b = b;
        this.c = c;
    }

    public boolean contains(Vector2D point) {
        double pbc;
        double pab = point.sub(this.a).cross(this.b.sub(this.a));
        if (!this.hasSameSign(pab, pbc = point.sub(this.b).cross(this.c.sub(this.b)))) {
            return false;
        }
        double pca = point.sub(this.c).cross(this.a.sub(this.c));
        return this.hasSameSign(pab, pca);
    }

    public boolean isPointInCircumcircle(Vector2D point) {
        double a11 = this.a.x - point.x;
        double a21 = this.b.x - point.x;
        double a31 = this.c.x - point.x;
        double a12 = this.a.y - point.y;
        double a22 = this.b.y - point.y;
        double a32 = this.c.y - point.y;
        double a13 = (this.a.x - point.x) * (this.a.x - point.x) + (this.a.y - point.y) * (this.a.y - point.y);
        double a23 = (this.b.x - point.x) * (this.b.x - point.x) + (this.b.y - point.y) * (this.b.y - point.y);
        double a33 = (this.c.x - point.x) * (this.c.x - point.x) + (this.c.y - point.y) * (this.c.y - point.y);
        double det = a11 * a22 * a33 + a12 * a23 * a31 + a13 * a21 * a32 - a13 * a22 * a31 - a12 * a21 * a33 - a11 * a23 * a32;
        if (this.isOrientedCCW()) {
            return det > 0.0;
        }
        return det < 0.0;
    }

    public boolean isOrientedCCW() {
        double a11 = this.a.x - this.c.x;
        double a22 = this.b.y - this.c.y;
        double a12 = this.a.y - this.c.y;
        double a21 = this.b.x - this.c.x;
        double det = a11 * a22 - a12 * a21;
        return det > 0.0;
    }

    public boolean isNeighbour(Edge2D edge) {
        return !(this.a != edge.a && this.b != edge.a && this.c != edge.a || this.a != edge.b && this.b != edge.b && this.c != edge.b);
    }

    public Vector2D getNoneEdgeVertex(Edge2D edge) {
        if (this.a != edge.a && this.a != edge.b) {
            return this.a;
        }
        if (this.b != edge.a && this.b != edge.b) {
            return this.b;
        }
        if (this.c != edge.a && this.c != edge.b) {
            return this.c;
        }
        return null;
    }

    public boolean hasVertex(Vector2D vertex) {
        return this.a == vertex || this.b == vertex || this.c == vertex;
    }

    public EdgeDistancePack findNearestEdge(Vector2D point) {
        Object[] edges = new EdgeDistancePack[]{new EdgeDistancePack(new Edge2D(this.a, this.b), this.computeClosestPoint(new Edge2D(this.a, this.b), point).sub(point).mag()), new EdgeDistancePack(new Edge2D(this.b, this.c), this.computeClosestPoint(new Edge2D(this.b, this.c), point).sub(point).mag()), new EdgeDistancePack(new Edge2D(this.c, this.a), this.computeClosestPoint(new Edge2D(this.c, this.a), point).sub(point).mag())};
        Arrays.sort(edges);
        return edges[0];
    }

    private Vector2D computeClosestPoint(Edge2D edge, Vector2D point) {
        Vector2D ab = edge.b.sub(edge.a);
        double t = point.sub(edge.a).dot(ab) / ab.dot(ab);
        if (t < 0.0) {
            t = 0.0;
        } else if (t > 1.0) {
            t = 1.0;
        }
        return edge.a.add(ab.mult(t));
    }

    private boolean hasSameSign(double a, double b) {
        return Math.signum(a) == Math.signum(b);
    }

    public String toString() {
        return "Triangle2D[" + String.valueOf(this.a) + ", " + String.valueOf(this.b) + ", " + String.valueOf(this.c) + "]";
    }
}


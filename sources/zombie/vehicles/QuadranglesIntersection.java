/*
 * Decompiled with CFR 0.152.
 */
package zombie.vehicles;

import org.joml.Vector4f;
import zombie.iso.Vector2;

public final class QuadranglesIntersection {
    private static final float EPS = 0.001f;

    public static boolean IsQuadranglesAreIntersected(Vector2[] q1, Vector2[] q2) {
        if (q1 == null || q2 == null || q1.length != 4 || q2.length != 4) {
            System.out.println("ERROR: IsQuadranglesAreIntersected");
            return false;
        }
        if (QuadranglesIntersection.lineIntersection(q1[0], q1[1], q2[0], q2[1])) {
            return true;
        }
        if (QuadranglesIntersection.lineIntersection(q1[0], q1[1], q2[1], q2[2])) {
            return true;
        }
        if (QuadranglesIntersection.lineIntersection(q1[0], q1[1], q2[2], q2[3])) {
            return true;
        }
        if (QuadranglesIntersection.lineIntersection(q1[0], q1[1], q2[3], q2[0])) {
            return true;
        }
        if (QuadranglesIntersection.lineIntersection(q1[1], q1[2], q2[0], q2[1])) {
            return true;
        }
        if (QuadranglesIntersection.lineIntersection(q1[1], q1[2], q2[1], q2[2])) {
            return true;
        }
        if (QuadranglesIntersection.lineIntersection(q1[1], q1[2], q2[2], q2[3])) {
            return true;
        }
        if (QuadranglesIntersection.lineIntersection(q1[1], q1[2], q2[3], q2[0])) {
            return true;
        }
        if (QuadranglesIntersection.lineIntersection(q1[2], q1[3], q2[0], q2[1])) {
            return true;
        }
        if (QuadranglesIntersection.lineIntersection(q1[2], q1[3], q2[1], q2[2])) {
            return true;
        }
        if (QuadranglesIntersection.lineIntersection(q1[2], q1[3], q2[2], q2[3])) {
            return true;
        }
        if (QuadranglesIntersection.lineIntersection(q1[2], q1[3], q2[3], q2[0])) {
            return true;
        }
        if (QuadranglesIntersection.lineIntersection(q1[3], q1[0], q2[0], q2[1])) {
            return true;
        }
        if (QuadranglesIntersection.lineIntersection(q1[3], q1[0], q2[1], q2[2])) {
            return true;
        }
        if (QuadranglesIntersection.lineIntersection(q1[3], q1[0], q2[2], q2[3])) {
            return true;
        }
        if (QuadranglesIntersection.lineIntersection(q1[3], q1[0], q2[3], q2[0])) {
            return true;
        }
        if (QuadranglesIntersection.IsPointInTriangle(q1[0], q2[0], q2[1], q2[2]) || QuadranglesIntersection.IsPointInTriangle(q1[0], q2[0], q2[2], q2[3])) {
            return true;
        }
        return QuadranglesIntersection.IsPointInTriangle(q2[0], q1[0], q1[1], q1[2]) || QuadranglesIntersection.IsPointInTriangle(q2[0], q1[0], q1[2], q1[3]);
    }

    public static boolean IsPointInTriangle(Vector2 p, Vector2[] q) {
        return QuadranglesIntersection.IsPointInTriangle(p, q[0], q[1], q[2]) || QuadranglesIntersection.IsPointInTriangle(p, q[0], q[2], q[3]);
    }

    public static float det(float a, float b, float c, float d) {
        return a * d - b * c;
    }

    private static boolean between(float a, float b, double c) {
        return (double)Math.min(a, b) <= c + (double)0.001f && c <= (double)(Math.max(a, b) + 0.001f);
    }

    private static boolean intersect_1(float a1, float b1, float c1, float d1) {
        float c;
        float d;
        float a;
        float b;
        if (a1 > b1) {
            b = a1;
            a = b1;
        } else {
            a = a1;
            b = b1;
        }
        if (c1 > d1) {
            d = c1;
            c = d1;
        } else {
            c = c1;
            d = d1;
        }
        return Math.max(a, c) <= Math.min(b, d);
    }

    public static boolean lineIntersection(Vector2 start1, Vector2 end1, Vector2 start2, Vector2 end2) {
        float a1 = start1.y - end1.y;
        float b1 = end1.x - start1.x;
        float c1 = -a1 * start1.x - b1 * start1.y;
        float a2 = start2.y - end2.y;
        float b2 = end2.x - start2.x;
        float c2 = -a2 * start2.x - b2 * start2.y;
        float zn = QuadranglesIntersection.det(a1, b1, a2, b2);
        if (zn != 0.0f) {
            double x = (double)(-QuadranglesIntersection.det(c1, b1, c2, b2)) * 1.0 / (double)zn;
            double y = (double)(-QuadranglesIntersection.det(a1, c1, a2, c2)) * 1.0 / (double)zn;
            return QuadranglesIntersection.between(start1.x, end1.x, x) && QuadranglesIntersection.between(start1.y, end1.y, y) && QuadranglesIntersection.between(start2.x, end2.x, x) && QuadranglesIntersection.between(start2.y, end2.y, y);
        }
        return QuadranglesIntersection.det(a1, c1, a2, c2) == 0.0f && QuadranglesIntersection.det(b1, c1, b2, c2) == 0.0f && QuadranglesIntersection.intersect_1(start1.x, end1.x, start2.x, end2.x) && QuadranglesIntersection.intersect_1(start1.y, end1.y, start2.y, end2.y);
    }

    public static boolean IsQuadranglesAreTransposed2(Vector4f q1, Vector4f q2) {
        if (QuadranglesIntersection.IsPointInQuadrilateral(new Vector2(q1.x, q1.y), q2.x, q2.z, q2.y, q2.w)) {
            return true;
        }
        if (QuadranglesIntersection.IsPointInQuadrilateral(new Vector2(q1.z, q1.y), q2.x, q2.z, q2.y, q2.w)) {
            return true;
        }
        if (QuadranglesIntersection.IsPointInQuadrilateral(new Vector2(q1.x, q1.w), q2.x, q2.z, q2.y, q2.w)) {
            return true;
        }
        if (QuadranglesIntersection.IsPointInQuadrilateral(new Vector2(q1.z, q1.w), q2.x, q2.z, q2.y, q2.w)) {
            return true;
        }
        if (QuadranglesIntersection.IsPointInQuadrilateral(new Vector2(q2.x, q2.y), q1.x, q1.z, q1.y, q1.w)) {
            return true;
        }
        if (QuadranglesIntersection.IsPointInQuadrilateral(new Vector2(q2.z, q2.y), q1.x, q1.z, q1.y, q1.w)) {
            return true;
        }
        if (QuadranglesIntersection.IsPointInQuadrilateral(new Vector2(q2.x, q2.w), q1.x, q1.z, q1.y, q1.w)) {
            return true;
        }
        return QuadranglesIntersection.IsPointInQuadrilateral(new Vector2(q2.z, q2.w), q1.x, q1.z, q1.y, q1.w);
    }

    private static boolean IsPointInQuadrilateral(Vector2 point, float x1, float x2, float y1, float y2) {
        if (QuadranglesIntersection.IsPointInTriangle(point, new Vector2(x1, y1), new Vector2(x1, y2), new Vector2(x2, y2))) {
            return true;
        }
        return QuadranglesIntersection.IsPointInTriangle(point, new Vector2(x2, y2), new Vector2(x2, y1), new Vector2(x1, y1));
    }

    private static boolean IsPointInTriangle(Vector2 point, Vector2 t1, Vector2 t2, Vector2 t3) {
        float d1 = (t1.x - point.x) * (t2.y - t1.y) - (t2.x - t1.x) * (t1.y - point.y);
        float d2 = (t2.x - point.x) * (t3.y - t2.y) - (t3.x - t2.x) * (t2.y - point.y);
        float d3 = (t3.x - point.x) * (t1.y - t3.y) - (t1.x - t3.x) * (t3.y - point.y);
        return d1 >= 0.0f && d2 >= 0.0f && d3 >= 0.0f || d1 <= 0.0f && d2 <= 0.0f && d3 <= 0.0f;
    }
}


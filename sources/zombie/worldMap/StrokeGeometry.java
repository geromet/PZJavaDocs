/*
 * Decompiled with CFR 0.152.
 */
package zombie.worldMap;

import java.util.ArrayList;

public class StrokeGeometry {
    static Point firstPoint;
    static Point lastPoint;
    static final double EPSILON = 1.0E-4;

    static Point newPoint(double x, double y) {
        if (firstPoint == null) {
            return new Point(x, y);
        }
        Point p = firstPoint;
        firstPoint = StrokeGeometry.firstPoint.next;
        if (lastPoint == p) {
            lastPoint = null;
        }
        p.next = null;
        return p.set(x, y);
    }

    static void release(Point p) {
        if (p.next != null || p == lastPoint) {
            return;
        }
        p.next = firstPoint;
        firstPoint = p;
        if (lastPoint == null) {
            lastPoint = p;
        }
    }

    static void release(ArrayList<Point> points) {
        for (int i = 0; i < points.size(); ++i) {
            StrokeGeometry.release(points.get(i));
        }
    }

    static ArrayList<Point> getStrokeGeometry(Point[] points, Attrs attrs) {
        if (points.length < 2) {
            return null;
        }
        String cap = attrs.cap;
        String join = attrs.join;
        float lineWidth = attrs.width / 2.0f;
        float miterLimit = attrs.miterLimit;
        ArrayList<Point> vertices = new ArrayList<Point>();
        ArrayList<Point> middlePoints = new ArrayList<Point>();
        boolean closed = false;
        if (points.length == 2) {
            join = "bevel";
            StrokeGeometry.createTriangles(points[0], Point.Middle(points[0], points[1]), points[1], vertices, lineWidth, join, miterLimit);
        } else {
            int i;
            for (i = 0; i < points.length - 1; ++i) {
                if (i == 0) {
                    middlePoints.add(points[0]);
                    continue;
                }
                if (i == points.length - 2) {
                    middlePoints.add(points[points.length - 1]);
                    continue;
                }
                middlePoints.add(Point.Middle(points[i], points[i + 1]));
            }
            for (i = 1; i < middlePoints.size(); ++i) {
                StrokeGeometry.createTriangles((Point)middlePoints.get(i - 1), points[i], (Point)middlePoints.get(i), vertices, lineWidth, join, miterLimit);
            }
        }
        if (cap.equals("round")) {
            Point p00 = vertices.get(0);
            Point p01 = vertices.get(1);
            Point p02 = points[1];
            Point p10 = vertices.get(vertices.size() - 1);
            Point p11 = vertices.get(vertices.size() - 3);
            Point p12 = points[points.length - 2];
            StrokeGeometry.createRoundCap(points[0], p00, p01, p02, vertices);
            StrokeGeometry.createRoundCap(points[points.length - 1], p10, p11, p12, vertices);
        } else if (cap.equals("square")) {
            Point p00 = vertices.get(vertices.size() - 1);
            Point p01 = vertices.get(vertices.size() - 3);
            StrokeGeometry.createSquareCap(vertices.get(0), vertices.get(1), Point.Sub(points[0], points[1]).normalize().scalarMult(Point.Sub(points[0], vertices.get(0)).length()), vertices);
            StrokeGeometry.createSquareCap(p00, p01, Point.Sub(points[points.length - 1], points[points.length - 2]).normalize().scalarMult(Point.Sub(p01, points[points.length - 1]).length()), vertices);
        }
        return vertices;
    }

    static void createSquareCap(Point p0, Point p1, Point dir, ArrayList<Point> verts) {
        verts.add(p0);
        verts.add(Point.Add(p0, dir));
        verts.add(Point.Add(p1, dir));
        verts.add(p1);
        verts.add(Point.Add(p1, dir));
        verts.add(p0);
    }

    static void createRoundCap(Point center, Point p0, Point p1, Point nextPointInLine, ArrayList<Point> verts) {
        double angleDiff;
        double radius = Point.Sub(center, p0).length();
        double angle0 = Math.atan2(p1.y - center.y, p1.x - center.x);
        double angle1 = Math.atan2(p0.y - center.y, p0.x - center.x);
        double orgAngle0 = angle0;
        if (angle1 > angle0) {
            if (angle1 - angle0 >= 3.141492653589793) {
                angle1 -= Math.PI * 2;
            }
        } else if (angle0 - angle1 >= 3.141492653589793) {
            angle0 -= Math.PI * 2;
        }
        if (Math.abs(angleDiff = angle1 - angle0) >= 3.141492653589793 && Math.abs(angleDiff) <= 3.1416926535897933) {
            Point r1 = Point.Sub(center, nextPointInLine);
            if (r1.x == 0.0) {
                if (r1.y > 0.0) {
                    angleDiff = -angleDiff;
                }
            } else if (r1.x >= -1.0E-4) {
                angleDiff = -angleDiff;
            }
        }
        int nsegments = (int)(Math.abs(angleDiff * radius) / 7.0);
        double angleInc = angleDiff / (double)(++nsegments);
        for (int i = 0; i < nsegments; ++i) {
            verts.add(StrokeGeometry.newPoint(center.x, center.y));
            verts.add(StrokeGeometry.newPoint(center.x + radius * Math.cos(orgAngle0 + angleInc * (double)i), center.y + radius * Math.sin(orgAngle0 + angleInc * (double)i)));
            verts.add(StrokeGeometry.newPoint(center.x + radius * Math.cos(orgAngle0 + angleInc * (double)(1 + i)), center.y + radius * Math.sin(orgAngle0 + angleInc * (double)(1 + i))));
        }
    }

    static double signedArea(Point p0, Point p1, Point p2) {
        return (p1.x - p0.x) * (p2.y - p0.y) - (p2.x - p0.x) * (p1.y - p0.y);
    }

    static Point lineIntersection(Point p0, Point p1, Point p2, Point p3) {
        double a0 = p1.y - p0.y;
        double b1 = p2.x - p3.x;
        double a1 = p3.y - p2.y;
        double b0 = p0.x - p1.x;
        double det = a0 * b1 - a1 * b0;
        if (det > -1.0E-4 && det < 1.0E-4) {
            return null;
        }
        double c0 = a0 * p0.x + b0 * p0.y;
        double c1 = a1 * p2.x + b1 * p2.y;
        double x = (b1 * c0 - b0 * c1) / det;
        double y = (a0 * c1 - a1 * c0) / det;
        return StrokeGeometry.newPoint(x, y);
    }

    static void createTriangles(Point p0, Point p1, Point p2, ArrayList<Point> verts, float width, String join, float miterLimit) {
        Point t0 = Point.Sub(p1, p0);
        Point t2 = Point.Sub(p2, p1);
        t0.perpendicular();
        t2.perpendicular();
        if (StrokeGeometry.signedArea(p0, p1, p2) > 0.0) {
            t0.invert();
            t2.invert();
        }
        t0.normalize();
        t2.normalize();
        t0.scalarMult(width);
        t2.scalarMult(width);
        Point pintersect = StrokeGeometry.lineIntersection(Point.Add(t0, p0), Point.Add(t0, p1), Point.Add(t2, p2), Point.Add(t2, p1));
        Point anchor = null;
        double anchorLength = Double.MAX_VALUE;
        if (pintersect != null) {
            anchor = Point.Sub(pintersect, p1);
            anchorLength = anchor.length();
        }
        double dd = (int)(anchorLength / (double)width);
        Point p0p1 = Point.Sub(p0, p1);
        double p0p1Length = p0p1.length();
        Point p1p2 = Point.Sub(p1, p2);
        double p1p2Length = p1p2.length();
        if (anchorLength > p0p1Length || anchorLength > p1p2Length) {
            verts.add(Point.Add(p0, t0));
            verts.add(Point.Sub(p0, t0));
            verts.add(Point.Add(p1, t0));
            verts.add(Point.Sub(p0, t0));
            verts.add(Point.Add(p1, t0));
            verts.add(Point.Sub(p1, t0));
            if (join.equals("round")) {
                StrokeGeometry.createRoundCap(p1, Point.Add(p1, t0), Point.Add(p1, t2), p2, verts);
            } else if (join.equals("bevel") || join.equals("miter") && dd >= (double)miterLimit) {
                verts.add(p1);
                verts.add(Point.Add(p1, t0));
                verts.add(Point.Add(p1, t2));
            } else if (join.equals("miter") && dd < (double)miterLimit && pintersect != null) {
                verts.add(Point.Add(p1, t0));
                verts.add(p1);
                verts.add(pintersect);
                verts.add(Point.Add(p1, t2));
                verts.add(p1);
                verts.add(pintersect);
            }
            verts.add(Point.Add(p2, t2));
            verts.add(Point.Sub(p1, t2));
            verts.add(Point.Add(p1, t2));
            verts.add(Point.Add(p2, t2));
            verts.add(Point.Sub(p1, t2));
            verts.add(Point.Sub(p2, t2));
        } else {
            verts.add(Point.Add(p0, t0));
            verts.add(Point.Sub(p0, t0));
            verts.add(Point.Sub(p1, anchor));
            verts.add(Point.Add(p0, t0));
            verts.add(Point.Sub(p1, anchor));
            verts.add(Point.Add(p1, t0));
            if (join.equals("round")) {
                Point newP0 = Point.Add(p1, t0);
                Point newP1 = Point.Add(p1, t2);
                Point newP2 = Point.Sub(p1, anchor);
                verts.add(newP0);
                verts.add(p1);
                verts.add(newP2);
                StrokeGeometry.createRoundCap(p1, newP0, newP1, newP2, verts);
                verts.add(p1);
                verts.add(newP1);
                verts.add(newP2);
            } else {
                if (join.equals("bevel") || join.equals("miter") && dd >= (double)miterLimit) {
                    verts.add(Point.Add(p1, t0));
                    verts.add(Point.Add(p1, t2));
                    verts.add(Point.Sub(p1, anchor));
                }
                if (join.equals("miter") && dd < (double)miterLimit) {
                    verts.add(pintersect);
                    verts.add(Point.Add(p1, t0));
                    verts.add(Point.Add(p1, t2));
                }
            }
            verts.add(Point.Add(p2, t2));
            verts.add(Point.Sub(p1, anchor));
            verts.add(Point.Add(p1, t2));
            verts.add(Point.Add(p2, t2));
            verts.add(Point.Sub(p1, anchor));
            verts.add(Point.Sub(p2, t2));
        }
    }

    public static final class Point {
        double x;
        double y;
        Point next;

        Point() {
            this.x = 0.0;
            this.y = 0.0;
        }

        Point(double x, double y) {
            this.x = x;
            this.y = y;
        }

        Point set(double x, double y) {
            this.x = x;
            this.y = y;
            return this;
        }

        Point scalarMult(double f) {
            this.x *= f;
            this.y *= f;
            return this;
        }

        Point perpendicular() {
            double x = this.x;
            this.x = -this.y;
            this.y = x;
            return this;
        }

        Point invert() {
            this.x = -this.x;
            this.y = -this.y;
            return this;
        }

        double length() {
            return Math.sqrt(this.x * this.x + this.y * this.y);
        }

        Point normalize() {
            double mod = this.length();
            this.x /= mod;
            this.y /= mod;
            return this;
        }

        double angle() {
            return this.y / this.x;
        }

        static double Angle(Point p0, Point p1) {
            return Math.atan2(p1.x - p0.x, p1.y - p0.y);
        }

        static Point Add(Point p0, Point p1) {
            return StrokeGeometry.newPoint(p0.x + p1.x, p0.y + p1.y);
        }

        static Point Sub(Point p1, Point p0) {
            return StrokeGeometry.newPoint(p1.x - p0.x, p1.y - p0.y);
        }

        static Point Middle(Point p0, Point p1) {
            return Point.Add(p0, p1).scalarMult(0.5);
        }
    }

    static class Attrs {
        String cap = "butt";
        String join = "bevel";
        float width = 1.0f;
        float miterLimit = 10.0f;

        Attrs() {
        }
    }
}


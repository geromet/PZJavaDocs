/*
 * Decompiled with CFR 0.152.
 */
package zombie.pathfind;

public final class LiangBarsky {
    private final double[] p = new double[4];
    private final double[] q = new double[4];

    public boolean lineRectIntersect(float x, float y, float dx, float dy, float left, float top, float right, float bottom) {
        return this.lineRectIntersect(x, y, dx, dy, left, top, right, bottom, null);
    }

    public boolean lineRectIntersect(float x, float y, float dx, float dy, float left, float top, float right, float bottom, double[] t1t2) {
        this.p[0] = -dx;
        this.p[1] = dx;
        this.p[2] = -dy;
        this.p[3] = dy;
        this.q[0] = x - left;
        this.q[1] = right - x;
        this.q[2] = y - top;
        this.q[3] = bottom - y;
        double u1 = 0.0;
        double u2 = 1.0;
        for (int i = 0; i < 4; ++i) {
            if (this.p[i] == 0.0) {
                if (!(this.q[i] < 0.0)) continue;
                return false;
            }
            double t = this.q[i] / this.p[i];
            if (this.p[i] < 0.0 && u1 < t) {
                u1 = t;
                continue;
            }
            if (!(this.p[i] > 0.0) || !(u2 > t)) continue;
            u2 = t;
        }
        if (u1 >= u2) {
            return false;
        }
        if (t1t2 != null) {
            t1t2[0] = u1;
            t1t2[1] = u2;
        }
        return true;
    }
}


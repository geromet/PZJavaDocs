/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters.animals.pathfind;

import org.joml.Vector2f;

public final class Envelope {
    public static boolean intersects(Vector2f p1, Vector2f p2, Vector2f q) {
        float f = q.x;
        float f2 = p1.x < p2.x ? p1.x : p2.x;
        if (f >= f2) {
            float f3 = q.x;
            float f4 = p1.x > p2.x ? p1.x : p2.x;
            if (f3 <= f4) {
                float f5 = q.y;
                float f6 = p1.y < p2.y ? p1.y : p2.y;
                if (f5 >= f6) {
                    float f7 = q.y;
                    float f8 = p1.y > p2.y ? p1.y : p2.y;
                    if (f7 <= f8) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean intersects(Vector2f p1, Vector2f p2, Vector2f q1, Vector2f q2) {
        double minq = Math.min(q1.x, q2.x);
        double maxq = Math.max(q1.x, q2.x);
        double minp = Math.min(p1.x, p2.x);
        double maxp = Math.max(p1.x, p2.x);
        if (minp > maxq) {
            return false;
        }
        if (maxp < minq) {
            return false;
        }
        minq = Math.min(q1.y, q2.y);
        maxq = Math.max(q1.y, q2.y);
        minp = Math.min(p1.y, p2.y);
        maxp = Math.max(p1.y, p2.y);
        if (minp > maxq) {
            return false;
        }
        return !(maxp < minq);
    }
}


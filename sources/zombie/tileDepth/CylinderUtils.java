/*
 * Decompiled with CFR 0.152.
 */
package zombie.tileDepth;

import java.util.Arrays;
import org.joml.Vector3f;
import zombie.vehicles.UI3DScene;

public final class CylinderUtils {
    public static boolean intersect(float radius, float height, UI3DScene.Ray rayIn, IntersectionRecord outRecord) {
        double c;
        Vector3f center = new Vector3f(0.0f);
        Vector3f eminusc = rayIn.origin.sub(center, new Vector3f());
        double b = 2.0f * (rayIn.direction.x * eminusc.x + rayIn.direction.y * eminusc.y);
        double a = Math.pow(rayIn.direction.x, 2.0) + Math.pow(rayIn.direction.y, 2.0);
        double discriminant = b * b - 4.0 * a * (c = Math.pow(eminusc.x, 2.0) + Math.pow(eminusc.y, 2.0) - Math.pow(radius, 2.0));
        if (discriminant < 0.0) {
            return false;
        }
        double t1 = Math.min((-b + Math.sqrt(discriminant)) / (2.0 * a), (-b - Math.sqrt(discriminant)) / (2.0 * a));
        double t2 = ((double)height / 2.0 - (double)eminusc.z) / (double)rayIn.direction.z;
        double t3 = ((double)(-height) / 2.0 - (double)eminusc.z) / (double)rayIn.direction.z;
        double[] tarr = new double[]{t1, t2, t3};
        Arrays.sort(tarr);
        Double t = null;
        for (double x : tarr) {
            IntersectionRecord tmp = new IntersectionRecord();
            rayIn.origin.add(CylinderUtils.getScaledVector(rayIn.direction, (float)x), tmp.location);
            if (x == t1) {
                if (!((double)Math.abs(tmp.location.z - center.z) < (double)height / 2.0)) continue;
                outRecord.normal.set(tmp.location.x - center.x, tmp.location.y - center.y, 0.0f);
                outRecord.normal.normalize();
                t = x;
                break;
            }
            if (!(Math.pow(tmp.location.x - center.x, 2.0) + Math.pow(tmp.location.y - center.y, 2.0) - Math.pow(radius, 2.0) <= 0.0)) continue;
            if (x == t2) {
                outRecord.normal.set(0.0f, 0.0f, 1.0f);
            } else if (x == t3) {
                outRecord.normal.set(0.0f, 0.0f, -1.0f);
            }
            t = x;
            break;
        }
        if (t == null) {
            return false;
        }
        rayIn.t = t.floatValue();
        outRecord.t = t;
        rayIn.origin.add(CylinderUtils.getScaledVector(rayIn.direction, t.floatValue()), outRecord.location);
        return true;
    }

    private static Vector3f getScaledVector(Vector3f v, float x) {
        return new Vector3f(v).mul(x);
    }

    public static final class IntersectionRecord {
        public final Vector3f location = new Vector3f();
        public final Vector3f normal = new Vector3f();
        double t;
    }
}


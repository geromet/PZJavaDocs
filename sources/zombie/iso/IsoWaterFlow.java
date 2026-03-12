/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso;

import java.util.ArrayList;
import org.joml.Matrix3f;
import org.joml.Vector2f;
import org.joml.Vector4f;
import zombie.debug.DebugLog;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.SpriteDetails.IsoFlagType;

public final class IsoWaterFlow {
    private static final ArrayList<Vector4f> points = new ArrayList();
    private static final ArrayList<Matrix3f> zones = new ArrayList();

    public static void addFlow(float x, float y, float flow, float speed) {
        int degrees = (360 - (int)flow - 45) % 360;
        if (degrees < 0) {
            degrees += 360;
        }
        flow = (float)Math.toRadians(degrees);
        points.add(new Vector4f(x, y, flow, speed));
    }

    public static void addZone(float x1, float y1, float x2, float y2, float shore, float waterGround) {
        if (x1 > x2 || y1 > y2 || (double)shore > 1.0) {
            DebugLog.log("ERROR IsoWaterFlow: Invalid waterzone (" + x1 + ", " + y1 + ", " + x2 + ", " + y2 + ")");
        }
        zones.add(new Matrix3f(x1, y1, x2, y2, shore, waterGround, 0.0f, 0.0f, 0.0f));
    }

    public static int getShore(int x, int y) {
        for (int i = 0; i < zones.size(); ++i) {
            Matrix3f zone = zones.get(i);
            if (!(zone.m00 <= (float)x) || !(zone.m02 >= (float)x) || !(zone.m01 <= (float)y) || !(zone.m10 >= (float)y)) continue;
            return (int)zone.m11;
        }
        return 1;
    }

    public static Vector2f getFlow(IsoGridSquare square, int ax, int ay, Vector2f out) {
        float flow;
        double d;
        Vector4f point;
        int i;
        Vector4f fpointA = null;
        float fpointAd = Float.MAX_VALUE;
        Vector4f fpointB = null;
        float fpointBd = Float.MAX_VALUE;
        Vector4f fpointC = null;
        float fpointCd = Float.MAX_VALUE;
        if (points.isEmpty()) {
            return out.set(0.0f, 0.0f);
        }
        for (i = 0; i < points.size(); ++i) {
            point = points.get(i);
            d = Math.pow(point.x - (float)(square.x + ax), 2.0) + Math.pow(point.y - (float)(square.y + ay), 2.0);
            if (!(d < (double)fpointAd)) continue;
            fpointAd = (float)d;
            fpointA = point;
        }
        for (i = 0; i < points.size(); ++i) {
            point = points.get(i);
            d = Math.pow(point.x - (float)(square.x + ax), 2.0) + Math.pow(point.y - (float)(square.y + ay), 2.0);
            if (!(d < (double)fpointBd) || point == fpointA) continue;
            fpointBd = (float)d;
            fpointB = point;
        }
        if ((fpointAd = Math.max((float)Math.sqrt(fpointAd), 0.1f)) > (fpointBd = Math.max((float)Math.sqrt(fpointBd), 0.1f)) * 10.0f) {
            flow = fpointA.z;
            speed = fpointA.w;
        } else {
            for (int i2 = 0; i2 < points.size(); ++i2) {
                Vector4f point2 = points.get(i2);
                double d2 = Math.pow(point2.x - (float)(square.x + ax), 2.0) + Math.pow(point2.y - (float)(square.y + ay), 2.0);
                if (!(d2 < (double)fpointCd) || point2 == fpointA || point2 == fpointB) continue;
                fpointCd = (float)d2;
                fpointC = point2;
            }
            fpointCd = Math.max((float)Math.sqrt(fpointCd), 0.1f);
            float fpointBCz = fpointB.z * (1.0f - fpointBd / (fpointBd + fpointCd)) + fpointC.z * (1.0f - fpointCd / (fpointBd + fpointCd));
            float fpointBCw = fpointB.w * (1.0f - fpointBd / (fpointBd + fpointCd)) + fpointC.w * (1.0f - fpointCd / (fpointBd + fpointCd));
            float fpointBCd = fpointBd * (1.0f - fpointBd / (fpointBd + fpointCd)) + fpointCd * (1.0f - fpointCd / (fpointBd + fpointCd));
            flow = fpointA.z * (1.0f - fpointAd / (fpointAd + fpointBCd)) + fpointBCz * (1.0f - fpointBCd / (fpointAd + fpointBCd));
            speed = fpointA.w * (1.0f - fpointAd / (fpointAd + fpointBCd)) + fpointBCw * (1.0f - fpointBCd / (fpointAd + fpointBCd));
        }
        float s = 1.0f;
        IsoCell cell = square.getCell();
        for (int dx = -5; dx < 5; ++dx) {
            for (int dy = -5; dy < 5; ++dy) {
                IsoGridSquare square1 = cell.getGridSquare(square.x + ax + dx, square.y + ay + dy, 0);
                if (square1 != null && square1.getProperties().has(IsoFlagType.water)) continue;
                s = (float)Math.min((double)s, Math.max(0.0, Math.sqrt(dx * dx + dy * dy)) / 4.0);
            }
        }
        return out.set(flow, speed *= s);
    }

    public static void Reset() {
        points.clear();
        zones.clear();
    }
}


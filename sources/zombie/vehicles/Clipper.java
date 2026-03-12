/*
 * Decompiled with CFR 0.152.
 */
package zombie.vehicles;

import java.nio.ByteBuffer;
import zombie.debug.DebugLog;

public class Clipper {
    private long address;
    final ByteBuffer bb = ByteBuffer.allocateDirect(64);
    public static final int ctNoClip = 0;
    public static final int ctIntersection = 1;
    public static final int ctUnion = 2;
    public static final int ctDifference = 3;
    public static final int ctXor = 4;
    public static final int jtSquare = 0;
    public static final int jtBevel = 1;
    public static final int jtRound = 2;
    public static final int jtMiter = 3;

    public static void init() {
        String libSuffix = "";
        if ("1".equals(System.getProperty("zomboid.debuglibs.clipper"))) {
            DebugLog.log("***** Loading debug version of PZClipper");
            libSuffix = "d";
        }
        if (System.getProperty("os.name").contains("OS X")) {
            System.loadLibrary("PZClipper");
        } else {
            System.loadLibrary("PZClipper64" + libSuffix);
        }
        Clipper.n_init();
    }

    public Clipper() {
        this.newInstance();
    }

    private native void newInstance();

    public native void clear();

    public void addPath(int numPoints, ByteBuffer points, boolean bClip) {
        this.addPath(numPoints, points, bClip, true);
    }

    public native void addPath(int var1, ByteBuffer var2, boolean var3, boolean var4);

    public native void addLine(float var1, float var2, float var3, float var4);

    public native void addAABB(float var1, float var2, float var3, float var4);

    public void addAABBBevel(float x1, float y1, float x2, float y2, float radius) {
        this.bb.clear();
        this.bb.putFloat(x1 + radius);
        this.bb.putFloat(y1);
        this.bb.putFloat(x2 - radius);
        this.bb.putFloat(y1);
        this.bb.putFloat(x2);
        this.bb.putFloat(y1 + radius);
        this.bb.putFloat(x2);
        this.bb.putFloat(y2 - radius);
        this.bb.putFloat(x2 - radius);
        this.bb.putFloat(y2);
        this.bb.putFloat(x1 + radius);
        this.bb.putFloat(y2);
        this.bb.putFloat(x1);
        this.bb.putFloat(y2 - radius);
        this.bb.putFloat(x1);
        this.bb.putFloat(y1 + radius);
        this.addPath(this.bb.position() / 4 / 2, this.bb, false);
    }

    public native void addPolygon(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8);

    public native void clipAABB(float var1, float var2, float var3, float var4);

    public int generatePolygons() {
        return this.generatePolygons(0.0);
    }

    public native int generatePolygons(int var1, double var2, int var4);

    public int generatePolygons(double delta, int joinType) {
        return this.generatePolygons(3, delta, joinType);
    }

    public int generatePolygons(double delta) {
        return this.generatePolygons(3, delta, 0);
    }

    public native int getPolygon(int var1, ByteBuffer var2);

    public native int generateTriangulatePolygons(int var1, int var2);

    public native int triangulate(int var1, ByteBuffer var2);

    public native int triangulate2(int var1, ByteBuffer var2);

    public static native void n_init();

    private static void writeToStdErr(String message) {
        System.err.println(message);
    }
}


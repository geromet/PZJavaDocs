/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso;

public final class BuildingID {
    public static long makeID(int cellX, int cellY, int buildingIndex) {
        int hi = cellX | cellY << 16;
        int lo = buildingIndex;
        return (long)hi << 32 | (long)lo;
    }

    public static int getCellX(long id) {
        int hi = (int)(id >> 32);
        return hi & 0xFFFF;
    }

    public static int getCellY(long id) {
        int hi = (int)(id >> 32);
        return hi >> 16 & 0xFFFF;
    }

    public static int getIndex(long id) {
        return (int)(id & 0xFFFFFFFFL);
    }

    public static boolean isSameCell(long id, int cellX, int cellY) {
        return BuildingID.getCellX(id) == cellX && BuildingID.getCellY(id) == cellY;
    }
}


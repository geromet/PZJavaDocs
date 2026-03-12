/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso;

public final class RoomID {
    public static long makeID(int cellX, int cellY, int roomIndex) {
        int hi = cellY << 16 | cellX;
        int lo = roomIndex;
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
        return RoomID.getCellX(id) == cellX && RoomID.getCellY(id) == cellY;
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package zombie.buildingRooms;

import java.util.ArrayList;
import zombie.iso.BuildingDef;
import zombie.iso.RoomDef;

public final class RemovedBuilding {
    public int x;
    public int y;
    public int z;

    public RemovedBuilding setFrom(BuildingDef buildingDef) {
        ArrayList<RoomDef> rooms = buildingDef.getRooms();
        if (rooms.isEmpty()) {
            rooms = buildingDef.getEmptyOutside();
        }
        if (rooms.isEmpty()) {
            throw new IllegalArgumentException("building has no rooms");
        }
        RoomDef roomDef = rooms.get(0);
        if (roomDef.getRects().isEmpty()) {
            throw new IllegalArgumentException("room has no rectangles");
        }
        RoomDef.RoomRect rect = roomDef.getRects().get(0);
        this.x = rect.getX();
        this.y = rect.getY();
        this.z = roomDef.level;
        return this;
    }
}


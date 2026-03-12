/*
 * Decompiled with CFR 0.152.
 */
package zombie.popman;

import java.util.ArrayList;
import zombie.SandboxOptions;
import zombie.iso.BuildingDef;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;

final class PlayerSpawns {
    private final ArrayList<PlayerSpawn> playerSpawns = new ArrayList();

    PlayerSpawns() {
    }

    public void addSpawn(int x, int y, int z) {
        PlayerSpawn ps = new PlayerSpawn(x, y, z);
        if (ps.building != null) {
            this.playerSpawns.add(ps);
        }
    }

    public void update() {
        long ms = System.currentTimeMillis();
        for (int i = 0; i < this.playerSpawns.size(); ++i) {
            PlayerSpawn ps = this.playerSpawns.get(i);
            if (ps.counter == -1L) {
                ps.counter = ms;
            }
            if (ps.counter + 10000L > ms) continue;
            this.playerSpawns.remove(i--);
        }
    }

    public boolean allowZombie(IsoGridSquare sq) {
        for (int i = 0; i < this.playerSpawns.size(); ++i) {
            PlayerSpawn ps = this.playerSpawns.get(i);
            if (ps.allowZombie(sq)) continue;
            return false;
        }
        return true;
    }

    private static class PlayerSpawn {
        public int x;
        public int y;
        public long counter;
        public BuildingDef building;
        public RoomDef room;

        public PlayerSpawn(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.counter = -1L;
            RoomDef roomDef = IsoWorld.instance.getMetaGrid().getRoomAt(x, y, z);
            if (roomDef != null) {
                this.building = roomDef.getBuilding();
                this.room = roomDef;
            }
        }

        public boolean allowZombie(IsoGridSquare sq) {
            switch (SandboxOptions.instance.lore.playerSpawnZombieRemoval.getValue()) {
                case 1: {
                    if (this.building == null) {
                        return true;
                    }
                    if (sq.getBuilding() != null && this.building == sq.getBuilding().getDef()) {
                        return false;
                    }
                    if (sq.getX() < this.building.getX() - 15 || sq.getX() >= this.building.getX2() + 15 || sq.getY() < this.building.getY() - 15 || sq.getY() >= this.building.getY2() + 15) break;
                    return false;
                }
                case 2: {
                    if (this.building == null) {
                        return true;
                    }
                    if (sq.getBuilding() == null || this.building != sq.getBuilding().getDef()) break;
                    return false;
                }
                case 3: {
                    if (this.room == null) {
                        return true;
                    }
                    if (sq.getRoom() == null || this.room != sq.getRoom().getRoomDef()) break;
                    return false;
                }
                case 4: {
                    return true;
                }
            }
            return true;
        }
    }
}


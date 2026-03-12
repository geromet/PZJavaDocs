/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso;

import java.util.ArrayList;
import java.util.Random;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.Lua.LuaManager;
import zombie.characters.IsoGameCharacter;
import zombie.debug.DebugLog;
import zombie.iso.BuildingDef;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;
import zombie.network.GameServer;
import zombie.network.ServerOptions;
import zombie.util.Type;

public final class SpawnPoints {
    public static final SpawnPoints instance = new SpawnPoints();
    private KahluaTable spawnRegions;
    private final ArrayList<IsoGameCharacter.Location> spawnPoints = new ArrayList();
    private final ArrayList<BuildingDef> spawnBuildings = new ArrayList();
    private final IsoGameCharacter.Location tempLocation = new IsoGameCharacter.Location(-1, -1, -1);

    public void init() {
        this.spawnRegions = LuaManager.platform.newTable();
        this.spawnPoints.clear();
        this.spawnBuildings.clear();
    }

    public void initServer1() {
        this.init();
        this.initSpawnRegions();
    }

    public void initServer2(IsoMetaGrid metaGrid) {
        if (this.parseServerSpawnPoint()) {
            return;
        }
        this.parseSpawnRegions(metaGrid);
        this.initSpawnBuildings();
    }

    public void initSinglePlayer(IsoMetaGrid metaGrid) {
        this.init();
        this.initSpawnRegions();
        this.parseSpawnRegions(metaGrid);
        this.initSpawnBuildings();
    }

    private void initSpawnRegions() {
        KahluaTable spawnRegionMgr = (KahluaTable)LuaManager.env.rawget("SpawnRegionMgr");
        if (spawnRegionMgr == null) {
            DebugLog.General.error("SpawnRegionMgr is undefined");
            return;
        }
        Object[] o = LuaManager.caller.pcall(LuaManager.thread, spawnRegionMgr.rawget("getSpawnRegions"), new Object[0]);
        if (o.length > 1 && o[1] instanceof KahluaTable) {
            this.spawnRegions = (KahluaTable)o[1];
        }
    }

    private boolean parseServerSpawnPoint() {
        if (!GameServer.server) {
            return false;
        }
        if (ServerOptions.instance.spawnPoint.getValue().isEmpty()) {
            return false;
        }
        String[] spawnPoint = ServerOptions.instance.spawnPoint.getValue().split(",");
        if (spawnPoint.length == 3) {
            try {
                int x = Integer.parseInt(spawnPoint[0].trim());
                int y = Integer.parseInt(spawnPoint[1].trim());
                int z = Integer.parseInt(spawnPoint[2].trim());
                if (x != 0 || y != 0) {
                    this.spawnPoints.add(new IsoGameCharacter.Location(x, y, z));
                    return true;
                }
            }
            catch (NumberFormatException ex) {
                DebugLog.General.error("SpawnPoint must be x,y,z, got \"" + ServerOptions.instance.spawnPoint.getValue() + "\"");
            }
        } else {
            DebugLog.General.error("SpawnPoint must be x,y,z, got \"" + ServerOptions.instance.spawnPoint.getValue() + "\"");
        }
        return false;
    }

    private void parseSpawnRegions(IsoMetaGrid metaGrid) {
        KahluaTableIterator itRegion = this.spawnRegions.iterator();
        while (itRegion.advance()) {
            Object object = itRegion.getValue();
            if (!(object instanceof KahluaTable)) continue;
            KahluaTable region = (KahluaTable)object;
            this.parseRegion(region, metaGrid);
        }
    }

    private void parseRegion(KahluaTable region, IsoMetaGrid metaGrid) {
        Object object = region.rawget("points");
        if (object instanceof KahluaTable) {
            KahluaTable points = (KahluaTable)object;
            KahluaTableIterator itPoints = points.iterator();
            while (itPoints.advance()) {
                Object object2 = itPoints.getValue();
                if (!(object2 instanceof KahluaTable)) continue;
                KahluaTable profession = (KahluaTable)object2;
                this.parseProfession(profession, metaGrid);
            }
        }
    }

    private void parseProfession(KahluaTable profession, IsoMetaGrid metaGrid) {
        KahluaTableIterator itPoints = profession.iterator();
        while (itPoints.advance()) {
            Object object = itPoints.getValue();
            if (!(object instanceof KahluaTable)) continue;
            KahluaTable point = (KahluaTable)object;
            this.parsePoint(point, metaGrid);
        }
    }

    private void parsePoint(KahluaTable point, IsoMetaGrid metaGrid) {
        Object object = point.rawget("position");
        if (object instanceof String) {
            String position = (String)object;
            switch (position.toLowerCase()) {
                case "center": {
                    Random rnd = new Random();
                    this.tempLocation.x = ((metaGrid.maxX - metaGrid.minX) / 2 + metaGrid.minX) * 256 + rnd.nextInt(32);
                    this.tempLocation.y = ((metaGrid.maxY - metaGrid.minY) / 2 + metaGrid.minY) * 256 + rnd.nextInt(32);
                    this.tempLocation.z = 0;
                    break;
                }
                default: {
                    return;
                }
            }
        } else if (point.rawget("worldX") != null) {
            Double worldX = Type.tryCastTo(point.rawget("worldX"), Double.class);
            Double worldY = Type.tryCastTo(point.rawget("worldY"), Double.class);
            Double posX = Type.tryCastTo(point.rawget("posX"), Double.class);
            Double posY = Type.tryCastTo(point.rawget("posY"), Double.class);
            Double posZ = Type.tryCastTo(point.rawget("posZ"), Double.class);
            if (worldX == null || worldY == null || posX == null || posY == null) {
                return;
            }
            this.tempLocation.x = worldX.intValue() * 300 + posX.intValue();
            this.tempLocation.y = worldY.intValue() * 300 + posY.intValue();
            this.tempLocation.z = posZ == null ? 0 : posZ.intValue();
        } else {
            Double posX = Type.tryCastTo(point.rawget("posX"), Double.class);
            Double posY = Type.tryCastTo(point.rawget("posY"), Double.class);
            Double posZ = Type.tryCastTo(point.rawget("posZ"), Double.class);
            if (posX == null || posY == null) {
                return;
            }
            this.tempLocation.x = posX.intValue();
            this.tempLocation.y = posY.intValue();
            int n = this.tempLocation.z = posZ == null ? 0 : posZ.intValue();
        }
        if (!this.spawnPoints.contains(this.tempLocation)) {
            IsoGameCharacter.Location location = new IsoGameCharacter.Location(this.tempLocation.x, this.tempLocation.y, this.tempLocation.z);
            this.spawnPoints.add(location);
        }
    }

    private void initSpawnBuildings() {
        for (int i = 0; i < this.spawnPoints.size(); ++i) {
            IsoGameCharacter.Location location = this.spawnPoints.get(i);
            RoomDef roomDef = IsoWorld.instance.metaGrid.getRoomAt(location.x, location.y, location.z);
            if (roomDef == null || roomDef.getBuilding() == null) {
                DebugLog.General.warn("initSpawnBuildings: no room or building at %d,%d,%d", location.x, location.y, location.z);
                continue;
            }
            this.spawnBuildings.add(roomDef.getBuilding());
        }
    }

    public boolean isSpawnBuilding(BuildingDef def) {
        return this.spawnBuildings.contains(def);
    }

    public KahluaTable getSpawnRegions() {
        return this.spawnRegions;
    }

    public ArrayList<IsoGameCharacter.Location> getSpawnPoints() {
        return this.spawnPoints;
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package zombie.globalObjects;

import java.util.Objects;
import se.krka.kahlua.vm.KahluaTable;
import zombie.Lua.LuaManager;
import zombie.globalObjects.GlobalObjectSystem;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;

public abstract class GlobalObject {
    protected GlobalObjectSystem system;
    protected int x;
    protected int y;
    protected int z;
    protected final KahluaTable modData;

    GlobalObject(GlobalObjectSystem system, int x, int y, int z) {
        this.system = system;
        this.x = x;
        this.y = y;
        this.z = z;
        this.modData = LuaManager.platform.newTable();
    }

    public GlobalObjectSystem getSystem() {
        return this.system;
    }

    public void setLocation(int x, int y, int z) {
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getZ() {
        return this.z;
    }

    public IsoGridSquare getSquare() {
        return IsoWorld.instance.currentCell.getGridSquare(this.x, this.y, this.z);
    }

    public IsoObject getIsoObject() {
        IsoGridSquare sq = this.getSquare();
        if (sq == null) {
            return null;
        }
        for (int j = 0; j < sq.getObjects().size(); ++j) {
            IsoObject obj = sq.getObjects().get(j);
            if (!this.isValidIsoObject(obj)) continue;
            return obj;
        }
        return null;
    }

    public boolean isValidIsoObject(IsoObject obj) {
        if (obj == null) {
            return false;
        }
        KahluaTable modData = obj.getModData();
        if ("farming".equals(this.system.getName())) {
            return this.getModData().rawget("state") != null && this.getModData().rawget("nbOfGrow") != null && this.getModData().rawget("health") != null;
        }
        return false;
    }

    public KahluaTable getModData() {
        return this.modData;
    }

    public void Reset() {
        this.system = null;
        this.modData.wipe();
    }

    public void destroyThisObject() {
        Object functionObj;
        if ("farming".equals(this.system.getName())) {
            Object functionObj2;
            if (Objects.equals(this.getModData().rawget("state"), "destroyed")) {
                return;
            }
            if (this.getSquare() != null) {
                this.getSquare().playSound("RemovePlant");
            }
            if ((functionObj2 = LuaManager.getFunctionObject("SFarmingSystem.destroyPlant")) != null) {
                LuaManager.caller.pcallvoid(LuaManager.thread, functionObj2, this.getSquare());
            }
        } else if ("campfire".equals(this.system.getName()) && (functionObj = LuaManager.getFunctionObject("SCampfireSystem.putOut")) != null) {
            LuaManager.caller.pcallvoid(LuaManager.thread, functionObj, this.getSquare());
        }
    }
}


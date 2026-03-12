/*
 * Decompiled with CFR 0.152.
 */
package zombie.globalObjects;

import java.util.ArrayDeque;
import java.util.ArrayList;
import se.krka.kahlua.vm.KahluaTable;
import zombie.Lua.LuaManager;
import zombie.globalObjects.GlobalObject;
import zombie.globalObjects.GlobalObjectLookup;
import zombie.iso.IsoGridSquare;

public abstract class GlobalObjectSystem {
    private static final ArrayDeque<ArrayList<GlobalObject>> objectListPool = new ArrayDeque();
    protected final String name;
    protected final KahluaTable modData;
    protected final ArrayList<GlobalObject> objects = new ArrayList();
    protected final GlobalObjectLookup lookup = new GlobalObjectLookup(this);

    GlobalObjectSystem(String name) {
        this.name = name;
        this.modData = LuaManager.platform.newTable();
    }

    public String getName() {
        return this.name;
    }

    public final KahluaTable getModData() {
        return this.modData;
    }

    protected abstract GlobalObject makeObject(int var1, int var2, int var3);

    public final GlobalObject newObject(int x, int y, int z) {
        if (this.getObjectAt(x, y, z) != null) {
            throw new IllegalStateException("already an object at " + x + "," + y + "," + z);
        }
        GlobalObject object = this.makeObject(x, y, z);
        this.objects.add(object);
        this.lookup.addObject(object);
        return object;
    }

    public final void removeObject(GlobalObject object) throws IllegalArgumentException, IllegalStateException {
        if (object == null) {
            throw new NullPointerException("object is null");
        }
        if (object.system != this) {
            throw new IllegalStateException("object not in this system");
        }
        this.objects.remove(object);
        this.lookup.removeObject(object);
        object.Reset();
    }

    public final GlobalObject getObjectAt(int x, int y, int z) {
        return this.lookup.getObjectAt(x, y, z);
    }

    public final GlobalObject getObjectAt(IsoGridSquare sq) {
        return this.lookup.getObjectAt(sq.getX(), sq.getY(), sq.getZ());
    }

    public final boolean hasObjectsInChunk(int wx, int wy) {
        return this.lookup.hasObjectsInChunk(wx, wy);
    }

    public final ArrayList<GlobalObject> getObjectsInChunk(int wx, int wy) {
        return this.lookup.getObjectsInChunk(wx, wy, this.allocList());
    }

    public final ArrayList<GlobalObject> getObjectsAdjacentTo(int x, int y, int z) {
        return this.lookup.getObjectsAdjacentTo(x, y, z, this.allocList());
    }

    public final int getObjectCount() {
        return this.objects.size();
    }

    public final GlobalObject getObjectByIndex(int index) {
        if (index < 0 || index >= this.objects.size()) {
            return null;
        }
        return this.objects.get(index);
    }

    public final ArrayList<GlobalObject> allocList() {
        return objectListPool.isEmpty() ? new ArrayList() : objectListPool.pop();
    }

    public final void finishedWithList(ArrayList<GlobalObject> list) {
        if (list != null && !objectListPool.contains(list)) {
            list.clear();
            objectListPool.add(list);
        }
    }

    public void Reset() {
        for (int i = 0; i < this.objects.size(); ++i) {
            GlobalObject object = this.objects.get(i);
            object.Reset();
        }
        this.objects.clear();
        this.modData.wipe();
    }
}


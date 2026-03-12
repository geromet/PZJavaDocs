/*
 * Decompiled with CFR 0.152.
 */
package zombie.vehicles;

import java.util.ArrayList;
import java.util.Arrays;
import zombie.GameWindow;
import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.network.GameClient;
import zombie.vehicles.BaseVehicle;

public final class VehicleIDMap {
    public static final VehicleIDMap instance = new VehicleIDMap();
    private static final int MAX_IDS = Short.MAX_VALUE;
    private static final int RESIZE_COUNT = 256;
    private int capacity = 256;
    private BaseVehicle[] idToVehicle = new BaseVehicle[this.capacity];
    private short[] freeId = new short[this.capacity];
    private short freeIdSize;
    private final boolean noise = false;
    private int warnCount;

    VehicleIDMap() {
        for (int i = 0; i < this.capacity; ++i) {
            short s = this.freeIdSize;
            this.freeIdSize = (short)(s + 1);
            this.freeId[s] = (short)i;
        }
    }

    public void put(short id, BaseVehicle vehicle) {
        if (Core.debug) {
            // empty if block
        }
        if (GameClient.client && id >= this.capacity) {
            this.resize((id / 256 + 1) * 256);
        }
        if (id < 0 || id >= this.capacity) {
            throw new IllegalArgumentException("invalid vehicle id " + id + " max=" + this.capacity);
        }
        if (this.idToVehicle[id] != null) {
            throw new IllegalArgumentException("duplicate vehicle with id " + id);
        }
        if (vehicle == null) {
            throw new IllegalArgumentException("vehicle is null");
        }
        this.idToVehicle[id] = vehicle;
    }

    public void remove(short id) {
        if (Core.debug) {
            // empty if block
        }
        if (id < 0 || id >= this.capacity) {
            throw new IllegalArgumentException("invalid vehicle id=" + id + " max=" + this.capacity);
        }
        if (this.idToVehicle[id] == null) {
            throw new IllegalArgumentException("no vehicle with id " + id);
        }
        this.idToVehicle[id] = null;
        if (GameClient.client || GameWindow.loadedAsClient) {
            return;
        }
        short s = this.freeIdSize;
        this.freeIdSize = (short)(s + 1);
        this.freeId[s] = id;
    }

    public BaseVehicle get(short id) {
        return id >= 0 && id < this.capacity ? this.idToVehicle[id] : null;
    }

    public boolean containsKey(short id) {
        return id >= 0 && id < this.capacity && this.idToVehicle[id] != null;
    }

    public void toArrayList(ArrayList<BaseVehicle> vehicles) {
        for (int i = 0; i < this.capacity; ++i) {
            if (this.idToVehicle[i] == null) continue;
            vehicles.add(this.idToVehicle[i]);
        }
    }

    public void Reset() {
        Arrays.fill(this.idToVehicle, null);
        this.freeIdSize = (short)this.capacity;
        for (int i = 0; i < this.capacity; i = (int)((short)(i + 1))) {
            this.freeId[i] = i;
        }
    }

    public short allocateID() {
        if (GameClient.client) {
            throw new RuntimeException("client must not call this");
        }
        if (this.freeIdSize > 0) {
            this.freeIdSize = (short)(this.freeIdSize - 1);
            return this.freeId[this.freeIdSize];
        }
        if (this.capacity >= Short.MAX_VALUE) {
            if (this.warnCount < 100) {
                DebugLog.log("warning: ran out of unique vehicle ids");
                ++this.warnCount;
            }
            return -1;
        }
        this.resize(this.capacity + 256);
        return this.allocateID();
    }

    private void resize(int capacity) {
        int oldCapacity = this.capacity;
        this.capacity = Math.min(capacity, Short.MAX_VALUE);
        this.idToVehicle = Arrays.copyOf(this.idToVehicle, this.capacity);
        this.freeId = Arrays.copyOf(this.freeId, this.capacity);
        for (int i = oldCapacity; i < this.capacity; ++i) {
            short s = this.freeIdSize;
            this.freeIdSize = (short)(s + 1);
            this.freeId[s] = (short)i;
        }
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package zombie.popman;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import zombie.GameTime;
import zombie.WorldSoundManager;
import zombie.core.Color;
import zombie.core.Colors;
import zombie.core.network.ByteBufferWriter;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoWorld;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.service.ServerDebugInfo;
import zombie.popman.LoadedAreas;
import zombie.popman.ObjectPool;
import zombie.popman.ZombiePopulationRenderer;

public final class MPDebugInfo {
    public static final MPDebugInfo instance = new MPDebugInfo();
    private static final ConcurrentHashMap<Long, MPSoundDebugInfo> debugSounds = new ConcurrentHashMap();
    public final ArrayList<MPCell> loadedCells = new ArrayList();
    public final ObjectPool<MPCell> cellPool = new ObjectPool<MPCell>(MPCell::new);
    public final LoadedAreas loadedAreas = new LoadedAreas(false);
    public ArrayList<MPRepopEvent> repopEvents = new ArrayList();
    public final ObjectPool<MPRepopEvent> repopEventPool = new ObjectPool<MPRepopEvent>(MPRepopEvent::new);
    public short repopEpoch;
    public long requestTime;
    private boolean requestFlag;
    public boolean requestPacketReceived;
    private final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
    private static final float RESPAWN_EVERY_HOURS = 1.0f;
    private static final float REPOP_DISPLAY_HOURS = 0.5f;

    private static native boolean n_hasData(boolean var0);

    private static native void n_requestData();

    private static native int n_getLoadedCellsCount();

    private static native int n_getLoadedCellsData(int var0, ByteBuffer var1);

    private static native int n_getLoadedAreasCount();

    private static native int n_getLoadedAreasData(int var0, ByteBuffer var1);

    private static native int n_getRepopEventCount();

    private static native int n_getRepopEventData(int var0, ByteBuffer var1);

    private void requestServerInfo() {
        if (!GameClient.client) {
            return;
        }
        long currentTimeMS = System.currentTimeMillis();
        if (this.requestTime + 1000L > currentTimeMS) {
            return;
        }
        this.requestTime = currentTimeMS;
        ServerDebugInfo packet = new ServerDebugInfo();
        packet.setRequestServerInfo();
        ByteBufferWriter bbw = GameClient.connection.startPacket();
        PacketTypes.PacketType.ServerDebugInfo.doPacket(bbw);
        packet.write(bbw);
        PacketTypes.PacketType.ServerDebugInfo.send(GameClient.connection);
    }

    public void request() {
        if (!GameServer.server) {
            return;
        }
        this.requestTime = System.currentTimeMillis();
    }

    private void addRepopEvent(int wx, int wy, float time) {
        float worldAge = (float)GameTime.getInstance().getWorldAgeHours();
        while (!this.repopEvents.isEmpty() && this.repopEvents.get((int)0).worldAge + 0.5f < worldAge) {
            this.repopEventPool.release(this.repopEvents.remove(0));
        }
        this.repopEvents.add(this.repopEventPool.alloc().init(wx, wy, time));
        this.repopEpoch = (short)(this.repopEpoch + 1);
    }

    public void serverUpdate() {
        int i;
        int count;
        int offset;
        int total;
        if (!GameServer.server) {
            return;
        }
        long currentTimeMS = System.currentTimeMillis();
        if (this.requestTime + 10000L < currentTimeMS) {
            this.requestFlag = false;
            this.requestPacketReceived = false;
            return;
        }
        if (this.requestFlag) {
            if (MPDebugInfo.n_hasData(false)) {
                this.requestFlag = false;
                this.cellPool.release((List<MPCell>)this.loadedCells);
                this.loadedCells.clear();
                this.loadedAreas.clear();
                total = MPDebugInfo.n_getLoadedCellsCount();
                offset = 0;
                while (offset < total) {
                    this.byteBuffer.clear();
                    count = MPDebugInfo.n_getLoadedCellsData(offset, this.byteBuffer);
                    offset += count;
                    for (i = 0; i < count; ++i) {
                        MPCell cell = this.cellPool.alloc();
                        cell.cx = this.byteBuffer.getShort();
                        cell.cy = this.byteBuffer.getShort();
                        cell.currentPopulation = this.byteBuffer.getShort();
                        cell.desiredPopulation = this.byteBuffer.getShort();
                        cell.lastRepopTime = this.byteBuffer.getFloat();
                        this.loadedCells.add(cell);
                    }
                }
                total = MPDebugInfo.n_getLoadedAreasCount();
                offset = 0;
                while (offset < total) {
                    this.byteBuffer.clear();
                    count = MPDebugInfo.n_getLoadedAreasData(offset, this.byteBuffer);
                    offset += count;
                    for (i = 0; i < count; ++i) {
                        boolean serverCell = this.byteBuffer.get() == 0;
                        short x = this.byteBuffer.getShort();
                        short y = this.byteBuffer.getShort();
                        short w = this.byteBuffer.getShort();
                        short h = this.byteBuffer.getShort();
                        this.loadedAreas.add(x, y, w, h);
                    }
                }
            }
        } else if (this.requestPacketReceived) {
            MPDebugInfo.n_requestData();
            this.requestFlag = true;
            this.requestPacketReceived = false;
        }
        if (MPDebugInfo.n_hasData(true)) {
            total = MPDebugInfo.n_getRepopEventCount();
            offset = 0;
            while (offset < total) {
                this.byteBuffer.clear();
                count = MPDebugInfo.n_getRepopEventData(offset, this.byteBuffer);
                offset += count;
                for (i = 0; i < count; ++i) {
                    short wx = this.byteBuffer.getShort();
                    short wy = this.byteBuffer.getShort();
                    float worldAge = this.byteBuffer.getFloat();
                    this.addRepopEvent(wx, wy, worldAge);
                }
            }
        }
    }

    boolean isRespawnEnabled() {
        return !IsoWorld.getZombiesDisabled();
    }

    public void render(ZombiePopulationRenderer renderer, float zoom) {
        int i;
        this.requestServerInfo();
        float worldAge = (float)GameTime.getInstance().getWorldAgeHours();
        IsoMetaGrid metaGrid = IsoWorld.instance.metaGrid;
        renderer.outlineRect((float)(metaGrid.minX * 256) * 1.0f, (float)(metaGrid.minY * 256) * 1.0f, (float)((metaGrid.maxX - metaGrid.minX + 1) * 256) * 1.0f, (float)((metaGrid.maxY - metaGrid.minY + 1) * 256) * 1.0f, 1.0f, 1.0f, 1.0f, 0.25f);
        for (i = 0; i < this.loadedCells.size(); ++i) {
            MPCell mpCell = this.loadedCells.get(i);
            renderer.outlineRect(mpCell.cx * 256, mpCell.cy * 256, 256.0f, 256.0f, 1.0f, 1.0f, 1.0f, 0.25f);
            if (!this.isRespawnEnabled()) continue;
            float delta = Math.min(worldAge - mpCell.lastRepopTime, 1.0f) / 1.0f;
            if (mpCell.lastRepopTime > worldAge) {
                delta = 0.0f;
            }
            renderer.outlineRect(mpCell.cx * 256 + 1, mpCell.cy * 256 + 1, 254.0f, 254.0f, 0.0f, 1.0f, 0.0f, delta * delta);
        }
        for (i = 0; i < this.loadedAreas.count; ++i) {
            int n = i * 4;
            int ax = this.loadedAreas.areas[n++];
            int ay = this.loadedAreas.areas[n++];
            int aw = this.loadedAreas.areas[n++];
            int ah = this.loadedAreas.areas[n++];
            renderer.outlineRect(ax * 8, ay * 8, aw * 8, ah * 8, 0.7f, 0.7f, 0.7f, 1.0f);
        }
        for (i = 0; i < this.repopEvents.size(); ++i) {
            MPRepopEvent evt = this.repopEvents.get(i);
            if (evt.worldAge + 0.5f < worldAge) continue;
            float alpha = 1.0f - (worldAge - evt.worldAge) / 0.5f;
            alpha = Math.max(alpha, 0.1f);
            renderer.outlineRect(evt.wx * 8, evt.wy * 8, 40.0f, 40.0f, 0.0f, 0.0f, 1.0f, alpha);
        }
        if (zoom > 0.25f) {
            for (i = 0; i < this.loadedCells.size(); ++i) {
                MPCell cell = this.loadedCells.get(i);
                renderer.renderCellInfo(cell.cx, cell.cy, cell.currentPopulation, cell.desiredPopulation, cell.lastRepopTime + 1.0f - worldAge);
            }
        }
        try {
            debugSounds.entrySet().removeIf(sound -> System.currentTimeMillis() > (Long)sound.getKey() + 1000L);
            for (Map.Entry<Long, MPSoundDebugInfo> entry : debugSounds.entrySet()) {
                Color c = Colors.LightBlue;
                if (entry.getValue().sourceIsZombie) {
                    c = Colors.GreenYellow;
                } else if (entry.getValue().repeating) {
                    c = Colors.Coral;
                }
                float a = 1.0f - Math.max(0.0f, Math.min(1.0f, (float)(System.currentTimeMillis() - entry.getKey()) / 1000.0f));
                renderer.renderCircle(entry.getValue().x, entry.getValue().y, entry.getValue().radius, c.r, c.g, c.b, a);
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public static void AddDebugSound(WorldSoundManager.WorldSound sound) {
        try {
            debugSounds.put(System.currentTimeMillis(), new MPSoundDebugInfo(sound));
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public static final class MPRepopEvent {
        public int wx;
        public int wy;
        public float worldAge;

        public MPRepopEvent init(int wx, int wy, float worldAge) {
            this.wx = wx;
            this.wy = wy;
            this.worldAge = worldAge;
            return this;
        }
    }

    public static final class MPCell {
        public short cx;
        public short cy;
        public short currentPopulation;
        public short desiredPopulation;
        public float lastRepopTime;

        MPCell init(int cx, int cy, int currentPopulation, int desiredPopulation, float lastRepopTime) {
            this.cx = (short)cx;
            this.cy = (short)cy;
            this.currentPopulation = (short)currentPopulation;
            this.desiredPopulation = (short)desiredPopulation;
            this.lastRepopTime = lastRepopTime;
            return this;
        }
    }

    private static class MPSoundDebugInfo {
        int x;
        int y;
        int radius;
        boolean repeating;
        boolean sourceIsZombie;

        MPSoundDebugInfo(WorldSoundManager.WorldSound sound) {
            this.x = sound.x;
            this.y = sound.y;
            this.radius = sound.radius;
            this.repeating = sound.repeating;
            this.sourceIsZombie = sound.sourceIsZombie;
        }
    }
}


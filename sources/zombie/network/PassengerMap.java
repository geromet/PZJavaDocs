/*
 * Decompiled with CFR 0.152.
 */
package zombie.network;

import org.joml.Vector3f;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.network.GameClient;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.vehicles.BaseVehicle;

public final class PassengerMap {
    private static final int CHUNKS = 7;
    private static final int MAX_PASSENGERS = 16;
    private static final PassengerLocal[] perPlayerPngr = new PassengerLocal[4];
    private static final DriverLocal[] perPlayerDriver = new DriverLocal[4];

    public static void updatePassenger(IsoPlayer player) {
        IsoPlayer isoPlayer;
        if (player == null || player.getVehicle() == null || player.getVehicle().isDriver(player)) {
            return;
        }
        IsoGameCharacter driver = player.getVehicle().getDriver();
        if (!(driver instanceof IsoPlayer) || (isoPlayer = (IsoPlayer)driver).isLocalPlayer()) {
            return;
        }
        PassengerLocal pngrLocal = perPlayerPngr[player.playerIndex];
        pngrLocal.chunkMap = IsoWorld.instance.currentCell.chunkMap[player.playerIndex];
        pngrLocal.updateLoaded();
    }

    public static void clientReceivePacket(int pn, int seat, int wx, int wy, long loaded) {
        DriverLocal driverLocal = perPlayerDriver[pn];
        PassengerRemote pngrRemote = driverLocal.passengers[seat];
        if (pngrRemote == null) {
            pngrRemote = driverLocal.passengers[seat] = new PassengerRemote();
        }
        pngrRemote.setLoaded(wx, wy, loaded);
    }

    public static boolean isChunkLoaded(BaseVehicle vehicle, int wx, int wy) {
        IsoPlayer player;
        if (!GameClient.client) {
            return false;
        }
        if (vehicle == null) {
            return false;
        }
        IsoGameCharacter driver = vehicle.getDriver();
        if (!(driver instanceof IsoPlayer) || !(player = (IsoPlayer)driver).isLocalPlayer()) {
            return false;
        }
        int playerIndex = player.playerIndex;
        DriverLocal driverLocal = perPlayerDriver[playerIndex];
        for (int seat = 1; seat < vehicle.getMaxPassengers(); ++seat) {
            IsoPlayer isoPlayer;
            PassengerRemote pngrRemote = driverLocal.passengers[seat];
            if (pngrRemote == null || pngrRemote.wx == -1) continue;
            IsoGameCharacter chr = vehicle.getCharacter(seat);
            if (!(chr instanceof IsoPlayer) || (isoPlayer = (IsoPlayer)chr).isLocalPlayer()) {
                pngrRemote.wx = -1;
                continue;
            }
            int minX = pngrRemote.wx - 3;
            int minY = pngrRemote.wy - 3;
            if (wx < minX || wy < minY || wx >= minX + 7 || wy >= minY + 7 || (pngrRemote.loaded & 1L << wx - minX + (wy - minY) * 7) != 0L) continue;
            return false;
        }
        return true;
    }

    public static void render(int playerIndex) {
        if (!GameClient.client) {
            return;
        }
        IsoPlayer player = IsoPlayer.players[playerIndex];
        if (player == null || player.getVehicle() == null) {
            return;
        }
        BaseVehicle vehicle = player.getVehicle();
        int scale = Core.tileScale;
        int chunksPerWidth = 8;
        float r = 0.1f;
        float g = 0.1f;
        float b = 0.1f;
        float a = 0.75f;
        float z = 0.0f;
        DriverLocal drvrLocal = perPlayerDriver[playerIndex];
        for (int seat = 1; seat < vehicle.getMaxPassengers(); ++seat) {
            IsoPlayer isoPlayer;
            PassengerRemote pngrRemote = drvrLocal.passengers[seat];
            if (pngrRemote == null || pngrRemote.wx == -1) continue;
            IsoGameCharacter chr = vehicle.getCharacter(seat);
            if (!(chr instanceof IsoPlayer) || (isoPlayer = (IsoPlayer)chr).isLocalPlayer()) {
                pngrRemote.wx = -1;
                continue;
            }
            for (int y = 0; y < 7; ++y) {
                for (int x = 0; x < 7; ++x) {
                    boolean bLoaded;
                    boolean bl = bLoaded = (pngrRemote.loaded & 1L << x + y * 7) != 0L;
                    if (bLoaded) continue;
                    float left = (pngrRemote.wx - 3 + x) * 8;
                    float top = (pngrRemote.wy - 3 + y) * 8;
                    float sx = IsoUtils.XToScreenExact(left, top + 8.0f, 0.0f, 0);
                    float sy = IsoUtils.YToScreenExact(left, top + 8.0f, 0.0f, 0);
                    SpriteRenderer.instance.renderPoly((int)sx, (int)sy, (int)(sx + (float)(256 * scale)), (int)(sy - (float)(128 * scale)), (int)(sx + (float)(512 * scale)), (int)sy, (int)(sx + (float)(256 * scale)), (int)(sy + (float)(128 * scale)), 0.1f, 0.1f, 0.1f, 0.75f);
                }
            }
        }
    }

    public static void Reset() {
        for (int pn = 0; pn < 4; ++pn) {
            PassengerLocal pngrLocal = perPlayerPngr[pn];
            pngrLocal.wx = -1;
            DriverLocal drvrLocal = perPlayerDriver[pn];
            for (int seat = 0; seat < 16; ++seat) {
                PassengerRemote pngrRemote = drvrLocal.passengers[seat];
                if (pngrRemote == null) continue;
                pngrRemote.wx = -1;
            }
        }
    }

    static {
        for (int i = 0; i < 4; ++i) {
            PassengerMap.perPlayerPngr[i] = new PassengerLocal(i);
            PassengerMap.perPlayerDriver[i] = new DriverLocal();
        }
    }

    private static final class PassengerLocal {
        final int playerIndex;
        IsoChunkMap chunkMap;
        int wx = -1;
        int wy = -1;
        long loaded;

        PassengerLocal(int playerIndex) {
            this.playerIndex = playerIndex;
        }

        boolean setLoaded() {
            boolean changed;
            boolean moveS;
            int wx = this.chunkMap.worldX;
            int wy = this.chunkMap.worldY;
            Vector3f velocity = IsoPlayer.players[this.playerIndex].getVehicle().jniLinearVelocity;
            float absX = Math.abs(velocity.x);
            float absY = Math.abs(velocity.z);
            boolean moveW = velocity.x < 0.0f && absX > absY;
            boolean moveE = velocity.x > 0.0f && absX > absY;
            boolean moveN = velocity.z < 0.0f && absY > absX;
            boolean bl = moveS = velocity.z > 0.0f && absY > absX;
            if (moveE) {
                ++wx;
            } else if (moveW) {
                --wx;
            } else if (moveN) {
                --wy;
            } else if (moveS) {
                ++wy;
            }
            long loaded = 0L;
            for (int y = 0; y < 7; ++y) {
                for (int x = 0; x < 7; ++x) {
                    IsoChunk chunk = this.chunkMap.getChunk(IsoChunkMap.chunkGridWidth / 2 - 3 + x, IsoChunkMap.chunkGridWidth / 2 - 3 + y);
                    if (chunk == null || !chunk.loaded) continue;
                    loaded |= 1L << x + y * 7;
                }
            }
            boolean bl2 = changed = wx != this.wx || wy != this.wy || loaded != this.loaded;
            if (changed) {
                this.wx = wx;
                this.wy = wy;
                this.loaded = loaded;
            }
            return changed;
        }

        void updateLoaded() {
            if (this.setLoaded()) {
                INetworkPacket.send(PacketTypes.PacketType.VehiclePassengerRequest, this.playerIndex, this.wx, this.wy, this.loaded);
            }
        }
    }

    private static final class DriverLocal {
        final PassengerRemote[] passengers = new PassengerRemote[16];

        private DriverLocal() {
        }
    }

    private static final class PassengerRemote {
        int wx = -1;
        int wy = -1;
        long loaded;

        private PassengerRemote() {
        }

        void setLoaded(int wx, int wy, long loaded) {
            this.wx = wx;
            this.wy = wy;
            this.loaded = loaded;
        }
    }
}


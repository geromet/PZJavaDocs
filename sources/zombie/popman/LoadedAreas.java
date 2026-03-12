/*
 * Decompiled with CFR 0.152.
 */
package zombie.popman;

import zombie.characters.IsoPlayer;
import zombie.core.math.PZMath;
import zombie.core.raknet.UdpConnection;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoWorld;
import zombie.iso.Vector3;
import zombie.network.GameServer;
import zombie.network.ServerMap;

public final class LoadedAreas {
    public static final int MAX_AREAS = 64;
    public int[] areas = new int[256];
    public int count;
    public boolean changed;
    public int[] prevAreas = new int[256];
    public int prevCount;
    private final boolean serverCells;

    public LoadedAreas(boolean serverCells) {
        this.serverCells = serverCells;
    }

    public boolean set() {
        this.setPrev();
        this.clear();
        if (GameServer.server) {
            if (this.serverCells) {
                for (int i = 0; i < ServerMap.instance.loadedCells.size(); ++i) {
                    ServerMap.ServerCell serverCell = ServerMap.instance.loadedCells.get(i);
                    this.add(serverCell.wx * 8, serverCell.wy * 8, 8, 8);
                }
            } else {
                for (int i = 0; i < GameServer.Players.size(); ++i) {
                    IsoPlayer player = GameServer.Players.get(i);
                    int wx = PZMath.fastfloor(player.getX()) / 8;
                    int wy = PZMath.fastfloor(player.getY()) / 8;
                    this.add(wx - player.onlineChunkGridWidth / 2, wy - player.onlineChunkGridWidth / 2, player.onlineChunkGridWidth, player.onlineChunkGridWidth);
                }
                for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
                    UdpConnection c = GameServer.udpEngine.connections.get(n);
                    for (int playerIndex = 0; playerIndex < 4; ++playerIndex) {
                        Vector3 area = c.connectArea[playerIndex];
                        if (area == null) continue;
                        int aw = PZMath.fastfloor(area.z);
                        this.add(PZMath.fastfloor(area.x) - aw / 2, PZMath.fastfloor(area.y) - aw / 2, aw, aw);
                    }
                }
            }
        } else {
            for (int i = 0; i < IsoPlayer.numPlayers; ++i) {
                IsoChunkMap cm = IsoWorld.instance.currentCell.chunkMap[i];
                if (cm.ignore) continue;
                this.add(cm.getWorldXMin(), cm.getWorldYMin(), IsoChunkMap.chunkGridWidth, IsoChunkMap.chunkGridWidth);
            }
        }
        this.changed = this.compareWithPrev();
        return this.changed;
    }

    public void add(int x, int y, int width, int height) {
        if (this.count >= 64) {
            return;
        }
        int n = this.count * 4;
        this.areas[n++] = x;
        this.areas[n++] = y;
        this.areas[n++] = width;
        this.areas[n++] = height;
        ++this.count;
    }

    public void clear() {
        this.count = 0;
        this.changed = false;
    }

    public void copy(LoadedAreas other) {
        this.count = other.count;
        for (int i = 0; i < this.count; ++i) {
            int n;
            this.areas[n = i * 4] = other.areas[n++];
            this.areas[n] = other.areas[n++];
            this.areas[n] = other.areas[n++];
            this.areas[n] = other.areas[n++];
        }
    }

    private void setPrev() {
        this.prevCount = this.count;
        for (int i = 0; i < this.count; ++i) {
            int n;
            this.prevAreas[n = i * 4] = this.areas[n++];
            this.prevAreas[n] = this.areas[n++];
            this.prevAreas[n] = this.areas[n++];
            this.prevAreas[n] = this.areas[n++];
        }
    }

    private boolean compareWithPrev() {
        if (this.prevCount != this.count) {
            return true;
        }
        for (int i = 0; i < this.count; ++i) {
            int n = i * 4;
            if (this.prevAreas[n] != this.areas[n++]) {
                return true;
            }
            if (this.prevAreas[n] != this.areas[n++]) {
                return true;
            }
            if (this.prevAreas[n] != this.areas[n++]) {
                return true;
            }
            if (this.prevAreas[n] == this.areas[n++]) continue;
            return true;
        }
        return false;
    }

    public boolean isOnEdge(int x, int y) {
        if (PZMath.coordmodulo(x, 8) != 0 && PZMath.coordmodulo(x + 1, 8) != 0 && PZMath.coordmodulo(y, 8) != 0 && PZMath.coordmodulo(y + 1, 8) != 0) {
            return false;
        }
        for (int i = 0; i < this.count; ++i) {
            boolean inY;
            int n = i * 4;
            int minX = this.areas[n++] * 8;
            int minY = this.areas[n++] * 8;
            int maxX = minX + this.areas[n++] * 8;
            int maxY = minY + this.areas[n++] * 8;
            boolean inX = x >= minX && x < maxX;
            boolean bl = inY = y >= minY && y < maxY;
            if (inX && (y == minY || y == maxY - 1)) {
                return true;
            }
            if (!inY || x != minX && x != maxX - 1) continue;
            return true;
        }
        return false;
    }
}


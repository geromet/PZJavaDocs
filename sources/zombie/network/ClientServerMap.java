/*
 * Decompiled with CFR 0.152.
 */
package zombie.network;

import java.util.Arrays;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.raknet.UdpConnection;
import zombie.core.textures.Texture;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.PacketTypes;
import zombie.network.ServerMap;
import zombie.network.packets.INetworkPacket;

public final class ClientServerMap {
    private static final int ChunksPerServerCell = 8;
    private static final int SquaresPerServerCell = 64;
    public int playerIndex;
    public int centerX;
    public int centerY;
    int chunkGridWidth;
    public int width;
    public boolean[] loaded;
    private static boolean[] isLoaded;
    private static Texture trafficCone;

    public ClientServerMap(int playerIndex, int squareX, int squareY, int chunkGridWidth) {
        this.playerIndex = playerIndex;
        this.centerX = squareX;
        this.centerY = squareY;
        this.chunkGridWidth = chunkGridWidth;
        this.width = (chunkGridWidth - 1) * 8 / 64;
        if ((chunkGridWidth - 1) * 8 % 64 != 0) {
            ++this.width;
        }
        ++this.width;
        this.loaded = new boolean[this.width * this.width];
    }

    public int getMinX() {
        return PZMath.coorddivision(PZMath.coorddivision(this.centerX, 8) - this.chunkGridWidth / 2, 8);
    }

    public int getMinY() {
        return PZMath.coorddivision(PZMath.coorddivision(this.centerY, 8) - this.chunkGridWidth / 2, 8);
    }

    public int getMaxX() {
        return this.getMinX() + this.width - 1;
    }

    public int getMaxY() {
        return this.getMinY() + this.width - 1;
    }

    public boolean isValidCell(int x, int y) {
        return x >= 0 && y >= 0 && x < this.width && y < this.width;
    }

    public boolean setLoaded() {
        if (!GameServer.server) {
            return false;
        }
        int serverMapMinX = ServerMap.instance.getMinX();
        int serverMapMinY = ServerMap.instance.getMinY();
        int minX = this.getMinX();
        int minY = this.getMinY();
        boolean changed = false;
        for (int y = 0; y < this.width; ++y) {
            for (int x = 0; x < this.width; ++x) {
                ServerMap.ServerCell cell = ServerMap.instance.getCell(minX + x - serverMapMinX, minY + y - serverMapMinY);
                boolean isLoaded = cell == null ? false : cell.isLoaded;
                changed |= this.loaded[x + y * this.width] != isLoaded;
                this.loaded[x + y * this.width] = isLoaded;
            }
        }
        return changed;
    }

    public boolean setPlayerPosition(int squareX, int squareY) {
        if (!GameServer.server) {
            return false;
        }
        int oldMinX = this.getMinX();
        int oldMinY = this.getMinY();
        this.centerX = squareX;
        this.centerY = squareY;
        return this.setLoaded() || oldMinX != this.getMinX() || oldMinY != this.getMinY();
    }

    public static boolean isChunkLoaded(int wx, int wy) {
        if (!GameClient.client) {
            return false;
        }
        for (int pn = 0; pn < IsoPlayer.numPlayers; ++pn) {
            int cy;
            int cx;
            ClientServerMap csm = GameClient.loadedCells[pn];
            if (csm == null || !csm.isValidCell(cx = PZMath.coorddivision(wx, 8) - csm.getMinX(), cy = PZMath.coorddivision(wy, 8) - csm.getMinY()) || !csm.loaded[cx + cy * csm.width]) continue;
            return true;
        }
        return false;
    }

    public static void characterIn(UdpConnection connection, int playerIndex) {
        if (!GameServer.server) {
            return;
        }
        ClientServerMap csm = connection.getLoadedCell(playerIndex);
        if (csm == null) {
            return;
        }
        IsoPlayer player = connection.players[playerIndex];
        if (player == null) {
            return;
        }
        if (csm.setPlayerPosition(PZMath.fastfloor(player.getX()), PZMath.fastfloor(player.getY()))) {
            csm.sendPacket(connection);
        }
    }

    public void sendPacket(IConnection connection) {
        if (!GameServer.server) {
            return;
        }
        INetworkPacket.send(connection, PacketTypes.PacketType.ServerMap, this);
    }

    public static void render(int playerIndex) {
        IsoChunk chunk;
        int x;
        int y;
        if (!GameClient.client) {
            return;
        }
        IsoChunkMap chunkMap = IsoWorld.instance.currentCell.getChunkMap(playerIndex);
        if (chunkMap == null || chunkMap.ignore) {
            return;
        }
        int scale = Core.tileScale;
        int chunksPerWidth = 8;
        float r = 0.1f;
        float g = 0.1f;
        float b = 0.1f;
        float a = 0.75f;
        float z = 0.0f;
        if (trafficCone == null) {
            trafficCone = Texture.getSharedTexture("street_decoration_01_26");
        }
        Texture tex = trafficCone;
        if (isLoaded == null || isLoaded.length < IsoChunkMap.chunkGridWidth * IsoChunkMap.chunkGridWidth) {
            isLoaded = new boolean[IsoChunkMap.chunkGridWidth * IsoChunkMap.chunkGridWidth];
        }
        for (y = 0; y < IsoChunkMap.chunkGridWidth; ++y) {
            for (x = 0; x < IsoChunkMap.chunkGridWidth; ++x) {
                chunk = chunkMap.getChunk(x, y);
                if (chunk == null) continue;
                ClientServerMap.isLoaded[x + y * IsoChunkMap.chunkGridWidth] = ClientServerMap.isChunkLoaded(chunk.wx, chunk.wy);
            }
        }
        for (y = 0; y < IsoChunkMap.chunkGridWidth; ++y) {
            for (x = 0; x < IsoChunkMap.chunkGridWidth; ++x) {
                chunk = chunkMap.getChunk(x, y);
                if (chunk == null) continue;
                boolean loaded = isLoaded[x + y * IsoChunkMap.chunkGridWidth];
                if (loaded && tex != null) {
                    IsoChunk chunkE;
                    IsoChunk chunkW;
                    IsoChunk chunkS;
                    IsoChunk chunkN = chunkMap.getChunk(x, y - 1);
                    if (chunkN != null && !isLoaded[x + (y - 1) * IsoChunkMap.chunkGridWidth]) {
                        for (int xx = 0; xx < 8; ++xx) {
                            float sx = IsoUtils.XToScreenExact(chunk.wx * 8 + xx, chunk.wy * 8, 0.0f, 0);
                            float sy = IsoUtils.YToScreenExact(chunk.wx * 8 + xx, chunk.wy * 8, 0.0f, 0);
                            SpriteRenderer.instance.render(tex, sx - (float)(tex.getWidth() / 2), sy, tex.getWidth(), tex.getHeight(), 1.0f, 1.0f, 1.0f, 1.0f, null);
                        }
                    }
                    if ((chunkS = chunkMap.getChunk(x, y + 1)) != null && !isLoaded[x + (y + 1) * IsoChunkMap.chunkGridWidth]) {
                        for (int xx = 0; xx < 8; ++xx) {
                            float sx = IsoUtils.XToScreenExact(chunk.wx * 8 + xx, chunk.wy * 8 + 8 - 1, 0.0f, 0);
                            float sy = IsoUtils.YToScreenExact(chunk.wx * 8 + xx, chunk.wy * 8 + 8 - 1, 0.0f, 0);
                            SpriteRenderer.instance.render(tex, sx - (float)(tex.getWidth() / 2), sy, tex.getWidth(), tex.getHeight(), 1.0f, 1.0f, 1.0f, 1.0f, null);
                        }
                    }
                    if ((chunkW = chunkMap.getChunk(x - 1, y)) != null && !isLoaded[x - 1 + y * IsoChunkMap.chunkGridWidth]) {
                        for (int yy = 0; yy < 8; ++yy) {
                            float sx = IsoUtils.XToScreenExact(chunk.wx * 8, chunk.wy * 8 + yy, 0.0f, 0);
                            float sy = IsoUtils.YToScreenExact(chunk.wx * 8, chunk.wy * 8 + yy, 0.0f, 0);
                            SpriteRenderer.instance.render(tex, sx - (float)(tex.getWidth() / 2), sy, tex.getWidth(), tex.getHeight(), 1.0f, 1.0f, 1.0f, 1.0f, null);
                        }
                    }
                    if ((chunkE = chunkMap.getChunk(x + 1, y)) != null && !isLoaded[x + 1 + y * IsoChunkMap.chunkGridWidth]) {
                        for (int yy = 0; yy < 8; ++yy) {
                            float sx = IsoUtils.XToScreenExact(chunk.wx * 8 + 8 - 1, chunk.wy * 8 + yy, 0.0f, 0);
                            float sy = IsoUtils.YToScreenExact(chunk.wx * 8 + 8 - 1, chunk.wy * 8 + yy, 0.0f, 0);
                            SpriteRenderer.instance.render(tex, sx - (float)(tex.getWidth() / 2), sy, tex.getWidth(), tex.getHeight(), 1.0f, 1.0f, 1.0f, 1.0f, null);
                        }
                    }
                }
                if (loaded) continue;
                float left = chunk.wx * 8;
                float top = chunk.wy * 8;
                float sx = IsoUtils.XToScreenExact(left, top + 8.0f, 0.0f, 0);
                float sy = IsoUtils.YToScreenExact(left, top + 8.0f, 0.0f, 0);
                SpriteRenderer.instance.renderPoly((int)sx, (int)sy, (int)(sx + (float)(256 * scale)), (int)(sy - (float)(128 * scale)), (int)(sx + (float)(512 * scale)), (int)sy, (int)(sx + (float)(256 * scale)), (int)(sy + (float)(128 * scale)), 0.1f, 0.1f, 0.1f, 0.75f);
            }
        }
    }

    public static void Reset() {
        Arrays.fill(GameClient.loadedCells, null);
        trafficCone = null;
    }
}


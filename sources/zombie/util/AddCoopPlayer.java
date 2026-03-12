/*
 * Decompiled with CFR 0.152.
 */
package zombie.util;

import zombie.Lua.LuaEventManager;
import zombie.SoundManager;
import zombie.characters.IsoPlayer;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferWriter;
import zombie.core.physics.WorldSimulation;
import zombie.debug.DebugLog;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.LightingJNI;
import zombie.iso.LosUtil;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.connection.ConnectCoopPacket;
import zombie.popman.ZombiePopulationManager;
import zombie.ui.UIManager;

public final class AddCoopPlayer {
    private Stage stage;
    private final IsoPlayer player;

    public AddCoopPlayer(IsoPlayer player, boolean newPlayer) {
        this.player = player;
        this.stage = newPlayer ? Stage.SendCoopConnect : Stage.Init;
    }

    public int getPlayerIndex() {
        return this.player == null ? -1 : this.player.playerIndex;
    }

    public IsoPlayer getPlayer() {
        return this.player;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void update() {
        switch (this.stage.ordinal()) {
            case 0: {
                if (GameClient.client) {
                    GameClient.sendCreatePlayer((byte)this.player.playerIndex);
                    this.stage = Stage.ReceiveCoopConnect;
                    break;
                }
                this.stage = Stage.Init;
                break;
            }
            case 2: {
                if (GameClient.client) {
                    ConnectCoopPacket packet = new ConnectCoopPacket();
                    packet.setInit(this.player);
                    ByteBufferWriter b = GameClient.connection.startPacket();
                    PacketTypes.PacketType.ConnectCoop.doPacket(b);
                    packet.write(b);
                    PacketTypes.PacketType.ConnectCoop.send(GameClient.connection);
                    this.stage = Stage.ReceiveClientConnect;
                    break;
                }
                this.stage = Stage.StartMapLoading;
                break;
            }
            case 4: {
                IsoCell cell = IsoWorld.instance.currentCell;
                int playerIndex = this.player.playerIndex;
                IsoChunkMap chunkMap = cell.chunkMap[playerIndex];
                IsoChunkMap.bSettingChunk.lock();
                try {
                    chunkMap.Unload();
                    chunkMap.ignore = false;
                    int wx = (int)(this.player.getX() / 8.0f);
                    int wy = (int)(this.player.getY() / 8.0f);
                    try {
                        if (LightingJNI.init) {
                            LightingJNI.teleport(playerIndex, wx - IsoChunkMap.chunkGridWidth / 2, wy - IsoChunkMap.chunkGridWidth / 2);
                        }
                    }
                    catch (Exception exception) {
                        // empty catch block
                    }
                    if (!GameServer.server && !GameClient.client) {
                        ZombiePopulationManager.instance.playerSpawnedAt(PZMath.fastfloor(this.player.getX()), PZMath.fastfloor(this.player.getY()), PZMath.fastfloor(this.player.getZ()));
                    }
                    chunkMap.worldX = wx;
                    chunkMap.worldY = wy;
                    if (!GameServer.server) {
                        WorldSimulation.instance.activateChunkMap(playerIndex);
                    }
                    int minwx = wx - IsoChunkMap.chunkGridWidth / 2;
                    int minwy = wy - IsoChunkMap.chunkGridWidth / 2;
                    int maxwx = wx + IsoChunkMap.chunkGridWidth / 2 + 1;
                    int maxwy = wy + IsoChunkMap.chunkGridWidth / 2 + 1;
                    for (int x = minwx; x < maxwx; ++x) {
                        for (int y = minwy; y < maxwy; ++y) {
                            IsoChunk chunk;
                            if (!IsoWorld.instance.getMetaGrid().isValidChunk(x, y) || (chunk = chunkMap.LoadChunkForLater(x, y, x - minwx, y - minwy)) == null || !chunk.loaded) continue;
                            cell.setCacheChunk(chunk, playerIndex);
                        }
                    }
                    chunkMap.SwapChunkBuffers();
                }
                finally {
                    IsoChunkMap.bSettingChunk.unlock();
                }
                this.stage = Stage.CheckMapLoading;
                break;
            }
            case 5: {
                IsoCell cell = IsoWorld.instance.currentCell;
                IsoChunkMap chunkMap = cell.chunkMap[this.player.playerIndex];
                chunkMap.update();
                for (int y = 0; y < IsoChunkMap.chunkGridWidth; ++y) {
                    for (int x = 0; x < IsoChunkMap.chunkGridWidth; ++x) {
                        if (!IsoWorld.instance.getMetaGrid().isValidChunk(chunkMap.getWorldXMin() + x, chunkMap.getWorldYMin() + y) || chunkMap.getChunk(x, y) != null) continue;
                        return;
                    }
                }
                chunkMap.calculateZExtentsForChunkMap();
                IsoGridSquare sq = cell.getGridSquare(PZMath.fastfloor(this.player.getX()), PZMath.fastfloor(this.player.getY()), PZMath.fastfloor(this.player.getZ()));
                if (sq != null && sq.getRoom() != null) {
                    sq.getRoom().def.setExplored(true);
                    sq.getRoom().building.setAllExplored(true);
                }
                this.stage = GameClient.client ? Stage.SendPlayerConnect : Stage.AddToWorld;
                break;
            }
            case 6: {
                GameClient.connection.setUserName(this.player.username);
                ConnectCoopPacket packet = new ConnectCoopPacket();
                packet.setPlayerConnect(this.player);
                ByteBufferWriter b = GameClient.connection.startPacket();
                PacketTypes.PacketType.ConnectCoop.doPacket(b);
                packet.write(b);
                PacketTypes.PacketType.ConnectCoop.send(GameClient.connection);
                this.stage = Stage.ReceivePlayerConnect;
                break;
            }
            case 8: {
                IsoPlayer.players[this.player.playerIndex] = this.player;
                LosUtil.cachecleared[this.player.playerIndex] = true;
                this.player.updateLightInfo();
                IsoCell cell = IsoWorld.instance.currentCell;
                this.player.setCurrentSquareFromPosition();
                this.player.updateUsername();
                this.player.setSceneCulled(false);
                if (cell.isSafeToAdd()) {
                    cell.getObjectList().add(this.player);
                } else {
                    cell.getAddList().add(this.player);
                }
                this.player.getInventory().addItemsToProcessItems();
                LuaEventManager.triggerEvent("OnCreatePlayer", this.player.playerIndex, this.player);
                if (this.player.isAsleep()) {
                    UIManager.setFadeBeforeUI(this.player.playerIndex, true);
                    UIManager.FadeOut(this.player.playerIndex, 2.0);
                    UIManager.setFadeTime(this.player.playerIndex, 0.0);
                }
                this.stage = Stage.Finished;
                SoundManager.instance.stopMusic("PlayerDied");
                break;
            }
        }
    }

    public boolean isFinished() {
        return this.stage == Stage.Finished;
    }

    public void playerCreated(int playerIndex) {
        if (this.player.playerIndex == playerIndex) {
            DebugLog.Multiplayer.debugln("created player=%d", playerIndex, (short)4);
            this.stage = Stage.Init;
        }
    }

    public void accessGranted(int playerIndex) {
        if (this.player.playerIndex == playerIndex) {
            DebugLog.log("coop player=" + (playerIndex + 1) + "/4 access granted");
            this.stage = Stage.StartMapLoading;
        }
    }

    public void accessDenied(int playerIndex, String reason) {
        if (this.player.playerIndex == playerIndex) {
            DebugLog.log("coop player=" + (playerIndex + 1) + "/4 access denied: " + reason);
            IsoCell cell = IsoWorld.instance.currentCell;
            int n = this.player.playerIndex;
            IsoChunkMap chunkMap = cell.chunkMap[n];
            chunkMap.Unload();
            chunkMap.ignore = true;
            this.stage = Stage.Finished;
            LuaEventManager.triggerEvent("OnCoopJoinFailed", playerIndex);
        }
    }

    public void receivePlayerConnect(int playerIndex) {
        if (this.player.playerIndex == playerIndex) {
            this.stage = Stage.AddToWorld;
            this.update();
        }
    }

    public boolean isLoadingThisSquare(int x, int y) {
        int wx = (int)(this.player.getX() / 8.0f);
        int wy = (int)(this.player.getY() / 8.0f);
        int minX = wx - IsoChunkMap.chunkGridWidth / 2;
        int minY = wy - IsoChunkMap.chunkGridWidth / 2;
        int maxX = minX + IsoChunkMap.chunkGridWidth;
        int maxY = minY + IsoChunkMap.chunkGridWidth;
        return (x /= 8) >= minX && x < maxX && (y /= 8) >= minY && y < maxY;
    }

    public static enum Stage {
        SendCoopConnect,
        ReceiveCoopConnect,
        Init,
        ReceiveClientConnect,
        StartMapLoading,
        CheckMapLoading,
        SendPlayerConnect,
        ReceivePlayerConnect,
        AddToWorld,
        Finished;

    }
}


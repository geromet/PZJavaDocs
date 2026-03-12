/*
 * Decompiled with CFR 0.152.
 */
package zombie.spnetwork;

import java.nio.ByteBuffer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import zombie.characters.IsoPlayer;
import zombie.characters.Role;
import zombie.core.network.ByteBufferWriter;
import zombie.iso.Vector3;
import zombie.network.ClientServerMap;
import zombie.network.IConnection;
import zombie.network.PacketsCache;
import zombie.network.PlayerDownloadServer;
import zombie.network.anticheats.PacketValidator;
import zombie.spnetwork.UdpEngine;

public final class UdpConnection
extends PacketsCache
implements IConnection {
    public final IsoPlayer[] players = IsoPlayer.players;
    final UdpEngine engine;
    private final Lock bufferLock = new ReentrantLock();
    private final ByteBuffer bb = ByteBuffer.allocate(1000000);
    private final ByteBufferWriter bbw = new ByteBufferWriter(this.bb);

    public UdpConnection(UdpEngine engine) {
        this.engine = engine;
    }

    public boolean ReleventTo(float x, float y) {
        return false;
    }

    @Override
    public IsoPlayer getPlayerAt(int playerIndex) {
        return this.players[playerIndex];
    }

    @Override
    public void setPlayerAt(int playerIndex, IsoPlayer player) {
        this.players[playerIndex] = player;
    }

    @Override
    public ByteBufferWriter startPacket() {
        this.bufferLock.lock();
        this.bb.clear();
        return this.bbw;
    }

    @Override
    public void endPacket(int packetPriority, int packetReliability, byte orderingChannel) {
    }

    @Override
    public String getUserName() {
        return "";
    }

    @Override
    public boolean hasPlayer(String userName) {
        return false;
    }

    @Override
    public Role getRole() {
        return null;
    }

    @Override
    public void setRole(Role role) {
    }

    @Override
    public boolean isRelevantTo(float x, float y) {
        return false;
    }

    @Override
    public long getConnectedGUID() {
        return 0L;
    }

    @Override
    public String getIDStr() {
        return null;
    }

    @Override
    public boolean wasInLoadingQueue() {
        return false;
    }

    @Override
    public PlayerDownloadServer getPlayerDownloadServer() {
        return null;
    }

    public void endPacketImmediate() {
        this.bb.flip();
        this.engine.Send(this.bb);
        this.bufferLock.unlock();
    }

    @Override
    public void cancelPacket() {
        this.bufferLock.unlock();
    }

    @Override
    public long getLastConnection() {
        return 0L;
    }

    @Override
    public int getMTUSize() {
        return 0;
    }

    @Override
    public void setConnectionTimestamp(long connectionReadyInterval) {
    }

    @Override
    public PacketValidator getValidator() {
        return null;
    }

    @Override
    public int getChunkGridWidth() {
        return 0;
    }

    @Override
    public void setChunkGridWidth(int range) {
    }

    @Override
    public void setUserName(int playerIndex, String userName) {
    }

    @Override
    public void setRelevantPos(int playerIndex, Vector3 relevantPos) {
    }

    @Override
    public void setConnectArea(int playerIndex, Vector3 connectArea) {
    }

    @Override
    public void setPlayerId(int playerIndex, short playerID) {
    }

    @Override
    public String getUserName(int playerIndex) {
        return "";
    }

    @Override
    public void setRelevantRange(byte relevantRange) {
    }

    @Override
    public void forceDisconnect(String description) {
    }

    @Override
    public void setFullyConnected() {
    }

    @Override
    public long getSteamId() {
        return 0L;
    }

    @Override
    public String getIP() {
        return null;
    }

    @Override
    public short getPlayerId(int playerIndex) {
        return 0;
    }

    @Override
    public void setLoadedCells(int playerIndex, ClientServerMap clientServerMap) {
    }

    @Override
    public ClientServerMap getLoadedCell(int playerIndex) {
        return null;
    }

    @Override
    public Vector3 getRelevantPos(int playerIndex) {
        return null;
    }
}


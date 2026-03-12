/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.fields.character;

import java.nio.ByteBuffer;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.fields.IDShort;
import zombie.network.fields.INetworkPacketField;
import zombie.network.fields.IPositional;

public class PlayerID
extends IDShort
implements INetworkPacketField,
IPositional {
    protected IsoPlayer player;
    @JSONField
    protected byte playerIndex = (byte)-1;

    public void set(IsoPlayer player) {
        this.setID(player.onlineId);
        this.playerIndex = (byte)(player.isLocal() ? (int)player.getPlayerNum() : -1);
        this.player = player;
    }

    public void clear() {
        this.setID((short)-1);
        this.playerIndex = (byte)-1;
        this.player = null;
    }

    private void parsePlayer(IConnection connection) {
        if (GameServer.server) {
            this.player = connection != null && this.playerIndex != -1 ? GameServer.getPlayerFromConnection(connection, this.playerIndex) : GameServer.IDToPlayerMap.get(this.getID());
        } else if (GameClient.client) {
            this.player = GameClient.IDToPlayerMap.get(this.getID());
        }
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        super.parse(b, connection);
        this.playerIndex = b.getByte();
        this.parsePlayer(connection);
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
        b.putByte(this.playerIndex);
    }

    @Override
    public void write(ByteBuffer bb) {
        super.write(bb);
        bb.put(this.playerIndex);
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        return super.isConsistent(connection) && this.getPlayer() != null;
    }

    public String toString() {
        return this.player == null ? "?" : "(" + this.player.getOnlineID() + ")";
    }

    public IsoPlayer getPlayer() {
        return this.player;
    }

    public void copy(PlayerID other) {
        this.setID(other.getID());
        this.player = other.player;
        this.playerIndex = other.playerIndex;
    }

    @Override
    public float getX() {
        if (this.player != null) {
            return this.player.getX();
        }
        return 0.0f;
    }

    @Override
    public float getY() {
        if (this.player != null) {
            return this.player.getY();
        }
        return 0.0f;
    }

    @Override
    public float getZ() {
        if (this.player != null) {
            return this.player.getZ();
        }
        return 0.0f;
    }

    public byte getPlayerIndex() {
        return this.playerIndex;
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.characters.BodyDamage.BodyDamage;
import zombie.characters.BodyDamage.BodyPart;
import zombie.characters.Capability;
import zombie.characters.CharacterStat;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.network.BodyDamageSync;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.character.PlayerID;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering=5, priority=1, reliability=2, requiredCapability=Capability.LoginOnServer, handlingType=3)
public class BodyDamageUpdatePacket
implements INetworkPacket {
    @JSONField
    private Type packetType = Type.START_UPDATING;
    @JSONField
    private final PlayerID currentPlayer = new PlayerID();
    @JSONField
    private final PlayerID remotePlayer = new PlayerID();
    private ByteBuffer data;
    private ByteBufferReader dataReader;

    public void setStart(IsoPlayer remotePlayer) {
        this.packetType = Type.START_UPDATING;
        this.currentPlayer.set(IsoPlayer.players[0]);
        this.remotePlayer.set(remotePlayer);
        DebugLog.Multiplayer.noise("start receiving updates from " + this.remotePlayer.getDescription() + " to " + this.currentPlayer.getDescription());
    }

    public void setStop(IsoPlayer remotePlayer) {
        this.packetType = Type.STOP_UPDATING;
        this.currentPlayer.set(IsoPlayer.players[0]);
        this.remotePlayer.set(remotePlayer);
        DebugLog.Multiplayer.noise("stop receiving updates from " + this.remotePlayer.getDescription() + " to " + this.currentPlayer.getDescription());
    }

    public void setUpdate(IsoPlayer remotePlayer, IsoPlayer requester, ByteBuffer inputData) {
        this.packetType = Type.UPDATE;
        this.currentPlayer.set(requester);
        this.remotePlayer.set(remotePlayer);
        this.data = ByteBuffer.allocate(inputData.position());
        this.dataReader = new ByteBufferReader(this.data);
        this.data.put(inputData.array(), 0, inputData.position());
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putEnum(this.packetType);
        this.currentPlayer.write(b);
        this.remotePlayer.write(b);
        if (this.packetType == Type.UPDATE) {
            this.data.position(0);
            b.put(this.data);
        }
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.packetType = b.getEnum(Type.class);
        this.currentPlayer.parse(b, connection);
        this.remotePlayer.parse(b, connection);
        if (this.packetType == Type.UPDATE) {
            this.data = ByteBuffer.allocate(b.limit() - b.position());
            this.dataReader = new ByteBufferReader(this.data);
            this.data.position(0);
            this.data.put(b.bb);
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        if (this.packetType == Type.START_UPDATING) {
            return;
        }
        if (this.packetType == Type.STOP_UPDATING) {
            return;
        }
        if (this.packetType == Type.UPDATE) {
            this.data.position(0);
            BodyDamage bodyDamage = this.remotePlayer.getPlayer().getBodyDamageRemote();
            byte bd = this.data.get();
            if (bd == 50) {
                bodyDamage.setOverallBodyHealth(this.data.getFloat());
                bodyDamage.setRemotePainLevel(this.data.get());
                bodyDamage.isFakeInfected = this.data.get() != 0;
                this.remotePlayer.getPlayer().getStats().set(CharacterStat.ZOMBIE_INFECTION, this.data.getFloat());
                bd = this.data.get();
            }
            while (bd == 64) {
                byte partIndex = this.data.get();
                BodyPart part = bodyDamage.getBodyParts().get(partIndex);
                byte id = this.data.get();
                while (id != 65) {
                    part.sync(this.dataReader, id);
                    id = this.data.get();
                }
                bd = this.data.get();
            }
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        if (this.packetType == Type.START_UPDATING) {
            BodyDamageSync.instance.startSendingUpdates(this.remotePlayer.getID(), this.currentPlayer.getID());
            return;
        }
        if (this.packetType == Type.STOP_UPDATING) {
            BodyDamageSync.instance.stopSendingUpdates(this.remotePlayer.getID(), this.currentPlayer.getID());
            return;
        }
        if (this.packetType == Type.UPDATE) {
            return;
        }
    }

    private static enum Type {
        START_UPDATING,
        STOP_UPDATING,
        UPDATE;

    }
}


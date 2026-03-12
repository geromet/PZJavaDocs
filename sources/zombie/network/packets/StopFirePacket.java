/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets;

import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.properties.IsoObjectChange;
import zombie.core.raknet.UdpConnection;
import zombie.iso.IsoGridSquare;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.Square;
import zombie.network.fields.character.PlayerID;
import zombie.network.fields.hit.Zombie;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering=0, priority=1, reliability=2, requiredCapability=Capability.LoginOnServer, handlingType=3)
public class StopFirePacket
implements INetworkPacket {
    @JSONField
    Type type = Type.GridSquare;
    @JSONField
    PlayerID playerId;
    @JSONField
    Zombie zombieId;
    @JSONField
    Square squareId;

    @Override
    public void setData(Object ... values2) {
        if (values2[0] instanceof IsoPlayer) {
            this.type = Type.Player;
            this.playerId = new PlayerID();
            this.playerId.set((IsoPlayer)values2[0]);
        }
        if (values2[0] instanceof IsoZombie) {
            this.type = Type.Zombie;
            this.zombieId = new Zombie();
            this.zombieId.set((IsoZombie)values2[0]);
        }
        if (values2[0] instanceof IsoGridSquare) {
            this.type = Type.GridSquare;
            this.squareId = new Square();
            this.squareId.set((IsoGridSquare)values2[0]);
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        if (this.type == Type.GridSquare) {
            this.squareId.getSquare().stopFire();
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        if (this.type == Type.Player) {
            this.playerId.getPlayer().sendObjectChange(IsoObjectChange.STOP_BURNING);
        }
        if (this.type == Type.Zombie) {
            this.zombieId.getCharacter().StopBurning();
        }
        if (this.type == Type.GridSquare) {
            this.squareId.getSquare().stopFire();
            this.sendToRelativeClients(PacketTypes.PacketType.StopFire, connection, this.squareId.getX(), this.squareId.getY());
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putEnum(this.type);
        if (this.type == Type.Player) {
            this.playerId.write(b);
        }
        if (this.type == Type.Zombie) {
            this.zombieId.write(b);
        }
        if (this.type == Type.GridSquare) {
            this.squareId.write(b);
        }
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.type = b.getEnum(Type.class);
        if (this.type == Type.Player) {
            this.playerId = new PlayerID();
            this.playerId.parse(b, connection);
        }
        if (this.type == Type.Zombie) {
            this.zombieId = new Zombie();
            this.zombieId.parse(b, connection);
        }
        if (this.type == Type.GridSquare) {
            this.squareId = new Square();
            this.squareId.parse(b, connection);
        }
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        if (this.type == Type.Player) {
            return this.playerId.getPlayer() != null;
        }
        if (this.type == Type.Zombie) {
            return this.zombieId.getCharacter() != null;
        }
        if (this.type == Type.GridSquare) {
            return this.squareId.getSquare() != null;
        }
        return false;
    }

    static enum Type {
        GridSquare,
        Player,
        Zombie;

    }
}


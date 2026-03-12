/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets;

import java.util.HashSet;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.character.PlayerID;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering=0, priority=1, reliability=3, requiredCapability=Capability.LoginOnServer, handlingType=7)
public class VariableSyncPacket
implements INetworkPacket {
    public PlayerID player = new PlayerID();
    public String key;
    public String value;
    public boolean boolValue;
    public float floatValue;
    public VariableType varType;
    public static HashSet<String> syncedVariables = new HashSet();

    @Override
    public void write(ByteBufferWriter b) {
        this.player.write(b);
        b.putUTF(this.key);
        b.putEnum(this.varType);
        switch (this.varType.ordinal()) {
            case 0: {
                b.putUTF(this.value);
                break;
            }
            case 1: {
                b.putBoolean(this.boolValue);
                break;
            }
            case 2: {
                b.putFloat(this.floatValue);
            }
        }
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.player.parse(b, connection);
        this.key = b.getUTF();
        this.varType = b.getEnum(VariableType.class);
        switch (this.varType.ordinal()) {
            case 0: {
                this.value = b.getUTF();
                break;
            }
            case 1: {
                this.boolValue = b.getBoolean();
                break;
            }
            case 2: {
                this.floatValue = b.getFloat();
            }
        }
    }

    @Override
    public void setData(Object ... values2) {
        this.player.set((IsoPlayer)values2[0]);
        this.key = (String)values2[1];
        if (values2[2] instanceof String) {
            this.varType = VariableType.String;
            this.value = (String)values2[2];
        }
        if (values2[2] instanceof Boolean) {
            this.varType = VariableType.Boolean;
            this.boolValue = (Boolean)values2[2];
        }
        if (values2[2] instanceof Float) {
            this.varType = VariableType.Float;
            this.floatValue = ((Float)values2[2]).floatValue();
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        switch (this.varType.ordinal()) {
            case 0: {
                this.player.getPlayer().setVariable(this.key, this.value);
                break;
            }
            case 1: {
                this.player.getPlayer().setVariable(this.key, this.boolValue);
                break;
            }
            case 2: {
                this.player.getPlayer().setVariable(this.key, this.floatValue);
            }
        }
        if (!syncedVariables.contains(this.key)) {
            syncedVariables.add(this.key);
        }
        for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            if (c.getConnectedGUID() == connection.getConnectedGUID() || !c.isRelevantTo(this.player.getPlayer().getX(), this.player.getPlayer().getY())) continue;
            ByteBufferWriter b2 = c.startPacket();
            PacketTypes.PacketType.VariableSync.doPacket(b2);
            this.write(b2);
            PacketTypes.PacketType.VariableSync.send(c);
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        switch (this.varType.ordinal()) {
            case 0: {
                this.player.getPlayer().setVariable(this.key, this.value);
                break;
            }
            case 1: {
                this.player.getPlayer().setVariable(this.key, this.boolValue);
                break;
            }
            case 2: {
                this.player.getPlayer().setVariable(this.key, this.floatValue);
            }
        }
    }

    @Override
    public void processClientLoading(UdpConnection connection) {
        switch (this.varType.ordinal()) {
            case 0: {
                this.player.getPlayer().setVariable(this.key, this.value);
                break;
            }
            case 1: {
                this.player.getPlayer().setVariable(this.key, this.boolValue);
                break;
            }
            case 2: {
                this.player.getPlayer().setVariable(this.key, this.floatValue);
            }
        }
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        return this.player.isConsistent(connection) && !this.key.isEmpty();
    }

    public static enum VariableType {
        String,
        Boolean,
        Float;

    }
}


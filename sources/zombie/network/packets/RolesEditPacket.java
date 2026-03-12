/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets;

import java.util.ArrayList;
import zombie.characters.Capability;
import zombie.characters.Roles;
import zombie.core.Color;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering=0, priority=2, reliability=2, requiredCapability=Capability.RolesWrite, handlingType=1)
public class RolesEditPacket
implements INetworkPacket {
    @JSONField
    public Command command;
    @JSONField
    public String name;
    @JSONField
    String description;
    @JSONField
    Color color;
    @JSONField
    ArrayList<Capability> capabilities;
    @JSONField
    String defaultId;
    @JSONField
    byte movingDirection;

    @Override
    public void setData(Object ... values2) {
        this.command = (Command)((Object)values2[0]);
        if (this.command == Command.AddRole || this.command == Command.DeleteRole) {
            this.name = (String)values2[1];
        }
        if (this.command == Command.SetupRole) {
            this.name = (String)values2[1];
            this.description = (String)values2[2];
            this.color = (Color)values2[3];
            this.capabilities = (ArrayList)values2[4];
        }
        if (this.command == Command.SetDefaultRole) {
            this.defaultId = (String)values2[1];
            this.name = (String)values2[2];
        }
        if (this.command == Command.MoveRole) {
            this.movingDirection = (Byte)values2[1];
            this.name = (String)values2[2];
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putEnum(this.command);
        if (this.command == Command.AddRole || this.command == Command.DeleteRole) {
            b.putUTF(this.name);
        }
        if (this.command == Command.SetupRole) {
            b.putUTF(this.name);
            b.putUTF(this.description);
            b.putFloat(this.color.r);
            b.putFloat(this.color.g);
            b.putFloat(this.color.b);
            b.putFloat(this.color.a);
            b.putShort(this.capabilities.size());
            for (Capability c : this.capabilities) {
                b.putUTF(c.name());
            }
        }
        if (this.command == Command.SetDefaultRole) {
            b.putUTF(this.defaultId);
            b.putUTF(this.name);
        }
        if (this.command == Command.MoveRole) {
            b.putByte(this.movingDirection);
            b.putUTF(this.name);
        }
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.command = b.getEnum(Command.class);
        if (this.command == Command.AddRole || this.command == Command.DeleteRole) {
            this.name = b.getUTF();
        }
        if (this.command == Command.SetupRole) {
            this.name = b.getUTF();
            this.description = b.getUTF();
            float colorR = b.getFloat();
            float colorG = b.getFloat();
            float colorB = b.getFloat();
            float colorA = b.getFloat();
            this.color = new Color(colorR, colorG, colorB, colorA);
            int capabilityCount = b.getShort();
            this.capabilities = new ArrayList();
            for (int i = 0; i < capabilityCount; ++i) {
                this.capabilities.add(Capability.valueOf(b.getUTF()));
            }
        }
        if (this.command == Command.SetDefaultRole) {
            this.defaultId = b.getUTF();
            this.name = b.getUTF();
        }
        if (this.command == Command.MoveRole) {
            this.movingDirection = b.getByte();
            this.name = b.getUTF();
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        if (this.command == Command.AddRole) {
            Roles.addRole(this.name);
        }
        if (this.command == Command.DeleteRole) {
            Roles.deleteRole(this.name, connection.getUserName());
        }
        if (this.command == Command.SetupRole) {
            Roles.setupRole(this.name, this.description, this.color, this.capabilities);
        }
        if (this.command == Command.SetDefaultRole) {
            Roles.setDefaultRoleFor(this.defaultId, this.name);
        }
        if (this.command == Command.MoveRole) {
            Roles.moveRole(this.movingDirection, this.name);
        }
    }

    public static enum Command {
        AddRole,
        DeleteRole,
        SetupRole,
        SetDefaultRole,
        MoveRole;

    }
}


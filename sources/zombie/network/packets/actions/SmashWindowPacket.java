/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets.actions;

import zombie.characters.Capability;
import zombie.core.Core;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.iso.IsoObject;
import zombie.iso.objects.IsoWindow;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.NetObject;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering=0, priority=1, reliability=2, requiredCapability=Capability.LoginOnServer, handlingType=3)
public class SmashWindowPacket
implements INetworkPacket {
    @JSONField
    NetObject window = new NetObject();
    @JSONField
    Action action;

    public void setSmashWindow(IsoObject window) {
        this.window.setObject(window);
        this.action = Action.smashWindow;
    }

    public void setRemoveBrokenGlass(IsoObject window) {
        this.window.setObject(window);
        this.action = Action.removeBrokenGlass;
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.window.write(b);
        b.putEnum(this.action);
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.window.parse(b, connection);
        this.action = b.getEnum(Action.class);
    }

    @Override
    public void processClient(UdpConnection connection) {
        IsoObject o = this.window.getObject();
        if (o instanceof IsoWindow) {
            IsoWindow isoWindow = (IsoWindow)o;
            if (this.action == Action.smashWindow) {
                isoWindow.smashWindow(true);
            } else if (this.action == Action.removeBrokenGlass) {
                isoWindow.setGlassRemoved(true);
            }
        } else if (Core.debug) {
            DebugLog.log("SmashWindow not a window!");
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        IsoObject o = this.window.getObject();
        if (o != null && o instanceof IsoWindow) {
            IsoWindow isoWindow = (IsoWindow)o;
            if (this.action == Action.smashWindow) {
                isoWindow.smashWindow(true);
            } else if (this.action == Action.removeBrokenGlass) {
                isoWindow.setGlassRemoved(true);
            }
            this.sendToRelativeClients(PacketTypes.PacketType.SmashWindow, null, o.getX(), o.getY());
        }
    }

    static enum Action {
        smashWindow,
        removeBrokenGlass;

    }
}


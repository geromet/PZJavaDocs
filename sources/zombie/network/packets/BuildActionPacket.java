/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets;

import se.krka.kahlua.vm.KahluaTable;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.ActionManager;
import zombie.core.BuildAction;
import zombie.core.Transaction;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering=1, priority=1, reliability=3, requiredCapability=Capability.LoginOnServer, handlingType=3)
public class BuildActionPacket
extends BuildAction
implements INetworkPacket {
    @Override
    public void setData(Object ... values2) {
        if (values2.length != 7) {
            DebugLog.Multiplayer.error(this.getClass().getSimpleName() + ".set get invalid arguments");
            return;
        }
        this.set((IsoPlayer)values2[0], ((Float)values2[1]).floatValue(), ((Float)values2[2]).floatValue(), ((Float)values2[3]).floatValue(), (Boolean)values2[4], (String)values2[5], (KahluaTable)values2[6]);
    }

    @Override
    public void processClient(UdpConnection connection) {
        ActionManager.getInstance().setStateFromPacket(this);
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        if (this.state == Transaction.TransactionState.Request) {
            if (this.isConsistent(connection) && this.item != null) {
                DebugLog.Action.trace("BuildAction accepted %s", this.getDescription());
                ActionManager.start(this);
                this.state = Transaction.TransactionState.Accept;
                ByteBufferWriter bbw = connection.startPacket();
                PacketTypes.PacketType.BuildAction.doPacket(bbw);
                this.write(bbw);
                PacketTypes.PacketType.BuildAction.send(connection);
            } else {
                DebugLog.Action.trace("BuildAction rejected %s", this.getDescription());
                this.state = Transaction.TransactionState.Reject;
                ByteBufferWriter bbw = connection.startPacket();
                PacketTypes.PacketType.BuildAction.doPacket(bbw);
                this.write(bbw);
                PacketTypes.PacketType.BuildAction.send(connection);
            }
        } else if (Transaction.TransactionState.Reject == this.state) {
            DebugLog.Action.trace("BuildAction reject %s", this.getDescription());
            ActionManager.stop(this);
        }
    }
}


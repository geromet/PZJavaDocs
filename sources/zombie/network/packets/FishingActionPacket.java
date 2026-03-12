/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets;

import se.krka.kahlua.vm.KahluaTable;
import zombie.Lua.LuaEventManager;
import zombie.characters.Capability;
import zombie.core.ActionManager;
import zombie.core.FishingAction;
import zombie.core.Transaction;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering=1, priority=1, reliability=3, requiredCapability=Capability.LoginOnServer, handlingType=3)
public class FishingActionPacket
extends FishingAction
implements INetworkPacket {
    @Override
    public void setData(Object ... values2) {
        DebugLog.Objects.warn("The FishingActionPacket.setData function isn't implemented");
        DebugLog.Objects.printStackTrace();
    }

    @Override
    public void processClient(UdpConnection connection) {
        ActionManager.getInstance().setStateFromPacket(this);
        if (this.state == Transaction.TransactionState.Reject || (this.contentFlag & flagStopFishing) != 0) {
            // empty if block
        }
        if ((this.contentFlag & (flagUpdateFish | flagDestroyBobber | flagCreateBobber)) != 0) {
            KahluaTable data = this.getLuaTable();
            if (data != null) {
                LuaEventManager.triggerEvent("OnFishingActionMPUpdate", this.getLuaTable());
            } else {
                DebugLog.Objects.warn("FishingActionPacket.processClient: no data found");
            }
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        if (this.state == Transaction.TransactionState.Request) {
            if (this.isConsistent(connection)) {
                DebugLog.Action.trace("FishingAction accepted %s", this.getDescription());
                ActionManager.start(this);
                this.state = Transaction.TransactionState.Accept;
                bbw = connection.startPacket();
                PacketTypes.PacketType.FishingAction.doPacket(bbw);
                this.write(bbw);
                PacketTypes.PacketType.FishingAction.send(connection);
            } else {
                DebugLog.Action.trace("FishingAction rejected %s", this.getDescription());
                this.state = Transaction.TransactionState.Reject;
                bbw = connection.startPacket();
                PacketTypes.PacketType.FishingAction.doPacket(bbw);
                this.write(bbw);
                PacketTypes.PacketType.FishingAction.send(connection);
            }
        } else if (Transaction.TransactionState.Reject == this.state) {
            DebugLog.Action.trace("FishingAction reject %s", this.getDescription());
            ActionManager.stop(this);
        }
        if ((this.contentFlag & flagUpdateBobberParameters) != 0) {
            KahluaTable data = this.getLuaTable();
            if (data != null) {
                LuaEventManager.triggerEvent("OnFishingActionMPUpdate", this.getLuaTable());
            } else {
                DebugLog.Objects.warn("FishingActionPacket.processServer: no data found");
            }
        }
    }
}


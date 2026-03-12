/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets.service;

import java.io.IOException;
import se.krka.kahlua.vm.KahluaTable;
import zombie.Lua.LuaEventManager;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.inventory.ItemContainer;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.ContainerID;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering=0, priority=1, reliability=2, requiredCapability=Capability.LoginOnServer, handlingType=3)
public class ReceiveContainerModDataPacket
implements INetworkPacket {
    @JSONField
    private final ContainerID containerId = new ContainerID();
    @JSONField
    private KahluaTable table;

    @Override
    public void setData(Object ... values2) {
        ItemContainer _container = (ItemContainer)values2[0];
        this.containerId.set(_container);
        this.table = _container.getParent().getModData();
    }

    public void set(ItemContainer _container) {
        this.containerId.set(_container);
        this.table = _container.getParent().getModData();
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.containerId.parse(b, connection);
        this.table = this.containerId.getContainer().getParent().getModData();
        try {
            this.table.load(b.bb, 244);
        }
        catch (IOException e) {
            DebugType.Network.printException(e, "Exception thrown parsing ReceiveContainerModDataPacket.", LogSeverity.Error);
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.containerId.write(b);
        try {
            this.table.save(b.bb);
        }
        catch (IOException e) {
            DebugType.Network.printException(e, "Exception thrown writing ReceiveContainerModDataPacket.", LogSeverity.Error);
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        LuaEventManager.triggerEvent("OnContainerUpdate");
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        this.sendToClients(PacketTypes.PacketType.ReceiveContainerModData, connection);
    }
}


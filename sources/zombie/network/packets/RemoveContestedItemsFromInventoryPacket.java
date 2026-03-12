/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets;

import java.util.ArrayList;
import java.util.Collection;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.IConnection;
import zombie.network.PacketSetting;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering=0, priority=1, reliability=2, requiredCapability=Capability.LoginOnServer, handlingType=2)
public class RemoveContestedItemsFromInventoryPacket
implements INetworkPacket {
    ArrayList<Integer> ids = new ArrayList();

    @Override
    public void setData(Object ... values2) {
        this.ids.clear();
        this.ids.addAll((Collection)values2[0]);
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putShort(this.ids.size());
        for (int n = 0; n < this.ids.size(); ++n) {
            b.putInt(this.ids.get(n));
        }
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.ids.clear();
        int count = b.getShort();
        for (int n = 0; n < count; ++n) {
            int id = b.getInt();
            this.ids.add(id);
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        for (Integer id : this.ids) {
            for (int pn = 0; pn < IsoPlayer.numPlayers; ++pn) {
                IsoPlayer player = IsoPlayer.players[pn];
                if (player == null || player.isDead()) continue;
                player.getInventory().removeItemWithIDRecurse(id);
            }
        }
    }
}


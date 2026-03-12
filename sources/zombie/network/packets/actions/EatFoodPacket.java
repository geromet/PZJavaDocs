/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets.actions;

import java.io.IOException;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.Food;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.character.PlayerID;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering=0, priority=1, reliability=2, requiredCapability=Capability.LoginOnServer, handlingType=3)
public class EatFoodPacket
implements INetworkPacket {
    @JSONField
    PlayerID player = new PlayerID();
    @JSONField
    float percentage;
    @JSONField
    Food food;

    @Override
    public void setData(Object ... values2) {
        if (values2.length == 3 && values2[0] instanceof IsoPlayer) {
            this.set((IsoPlayer)values2[0], (Food)values2[1], ((Float)values2[2]).floatValue());
        } else {
            DebugLog.Multiplayer.warn(this.getClass().getSimpleName() + ".set get invalid arguments");
        }
    }

    public void set(IsoPlayer player, Food food, float percentage) {
        this.player.set(player);
        this.percentage = percentage;
        this.food = food;
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.player.parse(b, connection);
        this.percentage = b.getFloat();
        this.player.getPlayer().getNutrition().load(b.bb);
        try {
            this.food = (Food)InventoryItem.loadItem(b.bb, 244);
        }
        catch (Exception ex) {
            ex.printStackTrace();
            this.food = null;
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        try {
            this.player.write(b);
            b.putFloat(this.percentage);
            this.player.getPlayer().getNutrition().save(b.bb);
            this.food.saveWithSize(b.bb, false);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        this.player.getPlayer().EatOnClient(this.food, this.percentage);
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        if (this.isConsistent(connection)) {
            this.processClient(connection);
        }
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        return this.player.isConsistent(connection) && this.food != null && this.percentage >= 0.0f && this.percentage < 100.0f;
    }
}


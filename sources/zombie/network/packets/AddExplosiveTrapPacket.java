/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets;

import java.io.IOException;
import zombie.characters.Capability;
import zombie.core.logger.LoggerManager;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.HandWeapon;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoTrap;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering=0, priority=1, reliability=2, requiredCapability=Capability.LoginOnServer, handlingType=3)
public class AddExplosiveTrapPacket
implements INetworkPacket {
    @JSONField
    int x;
    @JSONField
    int y;
    @JSONField
    byte z;
    @JSONField
    InventoryItem item;
    @JSONField
    boolean isNewItem;

    public void set(HandWeapon weapon, IsoGridSquare sq) {
        this.isNewItem = weapon != null;
        this.item = weapon;
        this.x = sq.x;
        this.y = sq.y;
        this.z = (byte)sq.z;
    }

    @Override
    public void setData(Object ... values2) {
        Object object;
        if (values2.length == 2 && (object = values2[1]) instanceof IsoGridSquare) {
            IsoGridSquare sq = (IsoGridSquare)object;
            Object object2 = values2[0];
            if (object2 instanceof HandWeapon) {
                HandWeapon weapon = (HandWeapon)object2;
                this.isNewItem = true;
                this.item = weapon;
            } else {
                this.isNewItem = false;
                this.item = null;
            }
            this.x = sq.x;
            this.y = sq.y;
            this.z = (byte)sq.z;
        } else {
            DebugLog.Multiplayer.warn(this.getClass().getSimpleName() + ".set get invalid arguments");
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putInt(this.x);
        b.putInt(this.y);
        b.putByte(this.z);
        if (b.putBoolean(this.isNewItem)) {
            try {
                this.item.saveWithSize(b.bb, false);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.x = b.getInt();
        this.y = b.getInt();
        this.z = b.getByte();
        this.isNewItem = b.getBoolean();
        if (this.isNewItem) {
            try {
                this.item = InventoryItem.loadItem(b.bb, 244);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(this.x, this.y, this.z);
        if (sq != null && this.isNewItem) {
            HandWeapon weapon = (HandWeapon)this.item;
            IsoTrap trap = new IsoTrap(weapon, sq.getCell(), sq);
            sq.AddTileObject(trap);
            if (trap.isInstantExplosion()) {
                trap.triggerExplosion();
            }
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(this.x, this.y, this.z);
        if (sq != null && this.isNewItem) {
            if (this.item == null) {
                return;
            }
            HandWeapon weapon = (HandWeapon)this.item;
            DebugLog.log("trap: user \"" + connection.getUserName() + "\" added " + this.item.getFullType() + " at " + this.x + "," + this.y + "," + this.z);
            LoggerManager.getLogger("map").write(connection.getIDStr() + " \"" + connection.getUserName() + "\" added " + this.item.getFullType() + " at " + this.x + "," + this.y + "," + this.z);
            IsoTrap trap = new IsoTrap(weapon, sq.getCell(), sq);
            sq.AddTileObject(trap);
            if (weapon.isInstantExplosion()) {
                for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
                    UdpConnection c = GameServer.udpEngine.connections.get(n);
                    ByteBufferWriter b = c.startPacket();
                    PacketTypes.PacketType.AddExplosiveTrap.doPacket(b);
                    this.write(b);
                    PacketTypes.PacketType.AddExplosiveTrap.send(c);
                }
                trap.triggerExplosion();
            } else {
                trap.transmitCompleteItemToClients();
            }
        }
    }
}


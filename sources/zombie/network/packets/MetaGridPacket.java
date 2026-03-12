/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets;

import zombie.characters.Capability;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.iso.IsoMetaCell;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;
import zombie.iso.areas.IsoRoom;
import zombie.network.IConnection;
import zombie.network.PacketSetting;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering=0, priority=1, reliability=2, requiredCapability=Capability.LoginOnServer, handlingType=2)
public class MetaGridPacket
implements INetworkPacket {
    short cellX = (short)-1;
    short cellY = (short)-1;
    short roomIndex = (short)-1;
    boolean lightsActive;

    public boolean set(int cellX, int cellY, int roomIndex) {
        IsoMetaGrid mg = IsoWorld.instance.metaGrid;
        if (cellX < mg.getMinX() || cellX > mg.getMaxX() || cellY < mg.getMinY() || cellY > mg.getMaxY()) {
            return false;
        }
        IsoMetaCell cell = mg.getCellData(cellX, cellY);
        if (cell.info == null || roomIndex < 0 || roomIndex >= cell.roomList.size()) {
            return false;
        }
        this.cellX = (short)cellX;
        this.cellY = (short)cellY;
        this.roomIndex = (short)roomIndex;
        this.lightsActive = cell.roomList.get((int)roomIndex).lightsActive;
        return true;
    }

    @Override
    public void processClient(UdpConnection connection) {
        IsoMetaGrid mg = IsoWorld.instance.metaGrid;
        if (this.cellX < mg.getMinX() || this.cellX > mg.getMaxX() || this.cellY < mg.getMinY() || this.cellY > mg.getMaxY()) {
            return;
        }
        IsoMetaCell cell = mg.getCellData(this.cellX, this.cellY);
        if (cell.info == null || this.roomIndex < 0 || this.roomIndex >= cell.roomList.size()) {
            return;
        }
        RoomDef roomDef = cell.roomList.get(this.roomIndex);
        roomDef.lightsActive = this.lightsActive;
        IsoRoom room = mg.getRoomByID(roomDef.getID());
        if (room == null) {
            return;
        }
        if (!room.lightSwitches.isEmpty()) {
            room.lightSwitches.get(0).switchLight(roomDef.lightsActive);
        }
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.cellX = b.getShort();
        this.cellY = b.getShort();
        this.roomIndex = b.getShort();
        this.lightsActive = b.getBoolean();
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putShort(this.cellX);
        b.putShort(this.cellY);
        b.putShort(this.roomIndex);
        b.putBoolean(this.lightsActive);
    }
}


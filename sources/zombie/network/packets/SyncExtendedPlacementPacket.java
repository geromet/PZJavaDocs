/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets;

import zombie.characters.Capability;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.ServerMap;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering=0, priority=1, reliability=2, requiredCapability=Capability.LoginOnServer, handlingType=3)
public class SyncExtendedPlacementPacket
implements INetworkPacket {
    int x = -1;
    int y = -1;
    byte z = (byte)-1;
    int itemId = -1;
    float offX;
    float offY;
    float offZ;
    float rotX;
    float rotY;
    float rotZ;
    IsoGridSquare gridSquare;
    IsoWorldInventoryObject object;

    @Override
    public void setData(Object ... values2) {
        this.object = (IsoWorldInventoryObject)values2[0];
        this.x = this.object.getSquare().getX();
        this.y = this.object.getSquare().getY();
        this.z = (byte)this.object.getSquare().getZ();
        this.itemId = this.object.getItem().getID();
        this.offX = this.object.getOffX();
        this.offY = this.object.getOffY();
        this.offZ = this.object.getOffZ();
        this.rotX = this.object.getItem().getWorldXRotation();
        this.rotY = this.object.getItem().getWorldYRotation();
        this.rotZ = this.object.getItem().getWorldZRotation();
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putInt(this.x);
        b.putInt(this.y);
        b.putByte(this.z);
        b.putInt(this.itemId);
        b.putFloat(this.offX);
        b.putFloat(this.offY);
        b.putFloat(this.offZ);
        b.putFloat(this.rotX);
        b.putFloat(this.rotY);
        b.putFloat(this.rotZ);
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.x = b.getInt();
        this.y = b.getInt();
        this.z = b.getByte();
        this.itemId = b.getInt();
        this.offX = PZMath.clamp(b.getFloat(), 0.0f, 1.0f);
        this.offY = PZMath.clamp(b.getFloat(), 0.0f, 1.0f);
        this.offZ = PZMath.clamp(b.getFloat(), 0.0f, 1.0f);
        this.rotX = PZMath.clamp(b.getFloat(), 0.0f, 360.0f);
        this.rotY = PZMath.clamp(b.getFloat(), 0.0f, 360.0f);
        this.rotZ = PZMath.clamp(b.getFloat(), 0.0f, 360.0f);
        this.findObject();
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        return this.gridSquare != null && this.object != null;
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        if (!this.isRelevant(connection)) {
            return;
        }
        this.updateObject(this.object);
        this.sendToRelativeClients(PacketTypes.PacketType.SyncExtendedPlacement, connection, this.x, this.y);
    }

    @Override
    public void processClient(UdpConnection connection) {
        this.updateObject(this.object);
    }

    public boolean isRelevant(UdpConnection connection) {
        if (this.gridSquare != null && !connection.isRelevantTo(this.gridSquare.x, this.gridSquare.y)) {
            DebugLog.Multiplayer.noise("[SyncExtendedPlacementPacket] not relevant gridSquare[x,y,z]=" + this.gridSquare.x + "," + this.gridSquare.y + "," + this.gridSquare.z);
            return false;
        }
        return true;
    }

    private void updateObject(IsoWorldInventoryObject object) {
        if (object == null) {
            DebugLog.Multiplayer.noise("[SyncExtendedPlacementPacket] skipping update of null IsoWorldInventoryObject");
            return;
        }
        object.setOffX(this.offX);
        object.setOffY(this.offY);
        object.setOffZ(this.offZ);
        object.getItem().setWorldXRotation(this.rotX);
        object.getItem().setWorldYRotation(this.rotY);
        object.getItem().setWorldZRotation(this.rotZ);
    }

    private IsoWorldInventoryObject getIsoWorldInventoryObjectWithID() {
        for (int i = 0; i < this.gridSquare.getWorldObjects().size(); ++i) {
            IsoWorldInventoryObject wo = this.gridSquare.getWorldObjects().get(i);
            if (wo == null || wo.getItem().getID() != this.itemId) continue;
            return wo;
        }
        return null;
    }

    private void findObject() {
        if (GameServer.server) {
            this.gridSquare = ServerMap.instance.getGridSquare(this.x, this.y, this.z);
        } else if (GameClient.client) {
            this.gridSquare = IsoWorld.instance.currentCell.getGridSquare(this.x, this.y, this.z);
        }
        if (this.gridSquare == null) {
            DebugLog.Multiplayer.warn("[SyncExtendedPlacementPacket] parse error: gridSquare is null x,y,z=" + this.x + "," + this.y + "," + this.z);
            return;
        }
        this.object = this.getIsoWorldInventoryObjectWithID();
        if (this.object == null) {
            DebugLog.Multiplayer.noise("[SyncExtendedPlacementPacket] unable to find object for gridSquare x,y,z=" + this.x + "," + this.y + "," + this.z);
        }
    }
}


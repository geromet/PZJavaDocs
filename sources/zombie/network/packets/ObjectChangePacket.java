/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets;

import java.util.IllegalFormatException;
import se.krka.kahlua.vm.KahluaTable;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.properties.IsoObjectChange;
import zombie.debug.DebugType;
import zombie.iso.IsoObject;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.network.GameClient;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.fields.Square;
import zombie.network.fields.character.PlayerID;
import zombie.network.fields.vehicle.VehicleID;
import zombie.network.packets.INetworkPacket;
import zombie.vehicles.BaseVehicle;

@PacketSetting(ordering=0, priority=1, reliability=3, requiredCapability=Capability.LoginOnServer, handlingType=2)
public class ObjectChangePacket
implements INetworkPacket {
    @JSONField
    IsoObject o;
    @JSONField
    IsoObjectChange change;
    @JSONField
    KahluaTable tbl;

    @Override
    public void setData(Object ... values2) {
        this.set((IsoObject)values2[0], IsoObjectChange.lookup(values2[1]), (KahluaTable)values2[2]);
    }

    private void set(IsoObject o, IsoObjectChange change, KahluaTable tbl) {
        this.o = o;
        this.change = change;
        this.tbl = tbl;
    }

    @Override
    public void write(ByteBufferWriter b) {
        IsoObject isoObject = this.o;
        if (isoObject instanceof IsoPlayer) {
            IsoPlayer isoPlayer = (IsoPlayer)isoObject;
            b.putByte(1);
            player = new PlayerID();
            ((PlayerID)player).set(isoPlayer);
            ((PlayerID)player).write(b);
        } else {
            player = this.o;
            if (player instanceof BaseVehicle) {
                BaseVehicle baseVehicle = (BaseVehicle)player;
                b.putByte(2);
                vehicleID = new VehicleID();
                ((VehicleID)vehicleID).set(baseVehicle);
                ((VehicleID)vehicleID).write(b);
            } else {
                vehicleID = this.o;
                if (vehicleID instanceof IsoWorldInventoryObject) {
                    IsoWorldInventoryObject isoWorldInventoryObject = (IsoWorldInventoryObject)vehicleID;
                    b.putByte(3);
                    square = new Square();
                    square.set(this.o.getSquare());
                    square.write(b);
                    b.putInt(isoWorldInventoryObject.getItem().getID());
                } else if (this.o instanceof IsoDeadBody) {
                    b.putByte(4);
                    square = new Square();
                    square.set(this.o.getSquare());
                    square.write(b);
                    b.putInt(this.o.getStaticMovingObjectIndex());
                } else {
                    b.putByte(0);
                    square = new Square();
                    square.set(this.o.getSquare());
                    square.write(b);
                    b.putInt(this.o.getSquare().getObjects().indexOf(this.o));
                }
            }
        }
        b.putEnum(this.change);
        this.o.saveChange(this.change, this.tbl, b);
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        try {
            this.o = ObjectChangePacket.parseObjectChange(this.o, b, connection);
        }
        catch (IsoObjectChangeTargetNotFoundException otnfe) {
            DebugType.Network.error("%s %s", otnfe.getMessage(), this.getDescription());
        }
    }

    public static IsoObject parseObjectChange(IsoObject o, ByteBufferReader b, IConnection connection) throws IsoObjectChangeTargetNotFoundException {
        byte type = b.getByte();
        if (type == 1) {
            PlayerID player = new PlayerID();
            player.parse(b, connection);
            o = player.getPlayer();
            IsoObjectChange change = b.getEnum(IsoObjectChange.class);
            DebugType.Network.debugln("receiveObjectChange. Type: %d Change: %s", new Object[]{type, change});
            if (player.isConsistent(connection)) {
                player.getPlayer().loadChange(change, b);
                return o;
            }
            throw new IsoObjectChangeTargetNotFoundException("Player can't be found. Connection not consistent.", new Object[0]);
        }
        if (type == 2) {
            VehicleID vehicleID = new VehicleID();
            vehicleID.parse(b, connection);
            o = vehicleID.getVehicle();
            IsoObjectChange change = b.getEnum(IsoObjectChange.class);
            DebugType.Network.debugln("receiveObjectChange. Type: %d Change: %s", new Object[]{type, change});
            if (vehicleID.isConsistent(connection)) {
                vehicleID.getVehicle().loadChange(change, b);
                return o;
            }
            throw new IsoObjectChangeTargetNotFoundException("Vehicle can't be found. Connection not consistent.", new Object[0]);
        }
        if (type == 3) {
            Square square = new Square();
            square.parse(b, connection);
            int itemID = b.getInt();
            IsoObjectChange change = b.getEnum(IsoObjectChange.class);
            DebugType.Network.debugln("receiveObjectChange. Type: %d Change: %s", new Object[]{type, change});
            if (!square.isConsistent(connection)) {
                GameClient.instance.delayPacket(PZMath.fastfloor(square.getX()), PZMath.fastfloor(square.getY()), PZMath.fastfloor(square.getZ()));
                return o;
            }
            for (int i = 0; i < square.getSquare().getWorldObjects().size(); ++i) {
                IsoWorldInventoryObject worldItem = square.getSquare().getWorldObjects().get(i);
                if (worldItem.getItem() == null || worldItem.getItem().getID() != itemID) continue;
                worldItem.loadChange(change, b);
                return o;
            }
            throw new IsoObjectChangeTargetNotFoundException("Object can't be found (square=%s, itemID=%s)", square.getDescription(), itemID);
        }
        if (type == 4) {
            Square square = new Square();
            square.parse(b, connection);
            int index = b.getInt();
            IsoObjectChange change = b.getEnum(IsoObjectChange.class);
            DebugType.Network.debugln("receiveObjectChange. Type: %d Change: %s", new Object[]{type, change});
            if (!square.isConsistent(connection)) {
                GameClient.instance.delayPacket(PZMath.fastfloor(square.getX()), PZMath.fastfloor(square.getY()), PZMath.fastfloor(square.getZ()));
                return o;
            }
            if (index >= 0 && index < square.getSquare().getStaticMovingObjects().size()) {
                o = square.getSquare().getStaticMovingObjects().get(index);
                o.loadChange(change, b);
                return o;
            }
            throw new IsoObjectChangeTargetNotFoundException("receiveObjectChange: object can't be found (square=%s, index=%d)", square.getDescription(), index);
        }
        Square square = new Square();
        square.parse(b, connection);
        int index = b.getInt();
        IsoObjectChange change = b.getEnum(IsoObjectChange.class);
        DebugType.Network.debugln("receiveObjectChange. Type: %d Change: %s", new Object[]{type, change});
        if (!square.isConsistent(connection)) {
            GameClient.instance.delayPacket(PZMath.fastfloor(square.getX()), PZMath.fastfloor(square.getY()), PZMath.fastfloor(square.getZ()));
            return o;
        }
        if (index >= 0 && index < square.getSquare().getObjects().size()) {
            o = square.getSquare().getObjects().get(index);
            o.loadChange(change, b);
            return o;
        }
        throw new IsoObjectChangeTargetNotFoundException("receiveObjectChange: object can't be found (square=%s, index=%d)", square.getDescription(), index);
    }

    public static class IsoObjectChangeTargetNotFoundException
    extends Exception {
        private final Object[] params;

        public IsoObjectChangeTargetNotFoundException(String message, Object ... params) {
            super(message);
            this.params = params;
        }

        @Override
        public String getMessage() {
            try {
                return String.format(super.getMessage(), this.params);
            }
            catch (IllegalFormatException ex) {
                return super.getMessage();
            }
        }
    }
}

